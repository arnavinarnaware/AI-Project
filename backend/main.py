from fastapi import FastAPI
from pydantic import BaseModel
from typing import Optional, List, Dict
import csv, math, os, time
from dataclasses import dataclass, field
from heapq import heappush, heappop
from collections import defaultdict
from datetime import datetime

app = FastAPI()

# --------- LOAD POIS FROM CSV ---------
BASE_DIR = os.path.dirname(__file__)
CSV_PATH = os.path.join(BASE_DIR, "pois_boston_seed.csv")

POIS: List[Dict] = []
with open(CSV_PATH) as f:
    for row in csv.DictReader(f):
        row["lat"] = float(row["lat"])
        row["lon"] = float(row["lon"])
        row["avg_dwell_min"] = int(row["avg_dwell_min"])
        row["admission_cost"] = float(row["admission_cost"])
        POIS.append(row)

# will be set from each /plan request
CURRENT_PREFS_LIKE = set()

# category -> list of ratings (for simple preference learning)
CATEGORY_RATINGS: Dict[str, List[int]] = defaultdict(list)


class PlanReq(BaseModel):
    city: str = "Boston"
    date: str
    start_time: str
    end_time: str
    budget_total: float
    mobility: str = "walk"
    preferences: dict = {}
    strategy: str = "static_budget"   # "static_budget", "static_explorer", "astar_budget", ...
    must_see: list[str] = []

    # fields coming from your Android UI
    days: Optional[int] = None
    has_car: Optional[bool] = None
    max_distance_miles: Optional[int] = None

    # new: toggle simulated "real-time" constraints
    use_live_constraints: bool = False


class Feedback(BaseModel):
    itinerary_id: str
    poi_id: str
    rating: int  # assume 1–5


# --------- UTILS ---------
def minutes(t: str) -> int:
    """Convert 'HH:MM' to minutes after midnight."""
    h, m = map(int, t.split(":"))
    return h * 60 + m


def parse_day_of_week(date_str: str) -> Optional[int]:
    """
    Parse YYYY-MM-DD -> weekday index (0=Monday,...,6=Sunday).
    If parsing fails, return None.
    """
    try:
        d = datetime.strptime(date_str, "%Y-%m-%d").date()
        return d.weekday()
    except Exception:
        return None


def haversine_mins(a, b, speed_kmh: float = 5.0) -> int:
    R = 6371
    dlat = math.radians(b["lat"] - a["lat"])
    dlon = math.radians(b["lon"] - a["lon"])
    la1, la2 = math.radians(a["lat"]), math.radians(b["lat"])
    x = math.sin(dlat / 2) ** 2 + math.cos(la1) * math.cos(la2) * math.sin(dlon / 2) ** 2
    km = 2 * R * math.asin(math.sqrt(x))
    hours = km / max(speed_kmh, 1e-6)
    return int(hours * 60)


def adjust_travel_mins(base_mins: int, current_time_min: int, use_live: bool) -> int:
    """
    Simulate "live" travel times:
    - Rush hours (8–10, 17–19) are slower.
    """
    if not use_live:
        return base_mins

    hour = (current_time_min // 60) % 24
    factor = 1.0
    if 8 <= hour <= 10 or 17 <= hour <= 19:
        factor = 1.5  # heavier traffic

    return max(1, int(math.ceil(base_mins * factor)))


def price_norm(p):
    return {"$": 0.2, "$$": 0.5, "$$$": 0.9}.get(p, 0.5)


def category_preference_bonus(cat: str) -> float:
    """
    Simple "learning" from ratings in CATEGORY_RATINGS.
    If avg rating is 1–5, map to [-0.5, 0.5] bonus.
    """
    scores = CATEGORY_RATINGS.get(cat, [])
    if not scores:
        return 0.0
    avg = sum(scores) / len(scores)  # 1..5
    return (avg - 3.0) * 0.25  # center at 3, scale down


def score(poi, strategy: str) -> float:
    """
    Base scoring for each strategy, plus:
      - bump for liked categories (CURRENT_PREFS_LIKE)
      - bump/penalty from learned category ratings
    """
    base = 0.0
    cat = poi["category"].lower()

    if strategy == "static_budget" or strategy == "astar_budget":
        # strongly penalize expensive places, favor history/outdoors
        base += -(price_norm(poi["price_tier"])) * 1.5
        if cat in {"history", "outdoors"}:
            base += 0.6
    elif strategy == "static_explorer" or strategy == "astar_explorer":
        # mild cost penalty, favor museums / food / history
        base += -price_norm(poi["price_tier"]) * 0.4
        if cat in {"museums", "food", "history"}:
            base += 0.8
    else:
        # fallback similar to explorer
        base += -price_norm(poi["price_tier"]) * 0.4
        if cat in {"museums", "food", "history"}:
            base += 0.8

    # extra bump if this category is in the user's explicit "like" list
    if CURRENT_PREFS_LIKE and cat in CURRENT_PREFS_LIKE:
        base += 1.0

    # learned preference from feedback
    base += category_preference_bonus(cat)

    return base


def get_speed_kmh(req: PlanReq) -> float:
    """Convert has_car/mobility into an approximate travel speed."""
    if req.has_car:
        return 25.0  # simple city driving
    mob = (req.mobility or "walk").lower()
    if mob == "mbta":
        return 15.0
    if mob == "rideshare":
        return 25.0
    return 5.0  # walk by default


def is_poi_open_today(poi, dow: Optional[int], use_live: bool) -> bool:
    """
    Simulate real-time constraints: e.g. museums closed on Sunday.
    """
    if not use_live or dow is None:
        return True

    cat = poi["category"].lower()
    # 6 = Sunday
    if dow == 6 and cat == "museums":
        return False

    return True


# --------- A* SEARCH STRUCT ---------
@dataclass(order=True)
class AStarNode:
    f: float
    g: float = field(compare=False)        # negative total score so far (we minimize)
    time: int = field(compare=False)
    budget: float = field(compare=False)
    cur_idx: int = field(compare=False)    # index into candidates, -1 for "start"
    visited_mask: int = field(compare=False)
    route: list = field(compare=False, default_factory=list)
    legs: list = field(compare=False, default_factory=list)


# --------- SINGLE-DAY GREEDY PLANNER ---------
def greedy_plan_one_day(
    req: PlanReq,
    day: int,
    used_pois: set,
    speed_kmh: float,
    dow: Optional[int],
):
    start = minutes(req.start_time)
    end = minutes(req.end_time)
    days = req.days or 1
    budget_per_day = req.budget_total / days

    t = start
    cur = {"lat": 42.3601, "lon": -71.0589, "name": "Start"}  # Boston center approx
    budget = budget_per_day

    route_day = []
    legs_day = []

    evals = 0
    while True:
        cands = []
        for p in POIS:
            evals += 1
            if p["id"] in used_pois:
                continue

            if not is_poi_open_today(p, dow, req.use_live_constraints):
                continue

            # travel time with chosen speed (adjusted for rush hour if enabled)
            base_travel = haversine_mins(cur, p, speed_kmh=speed_kmh)
            travel = adjust_travel_mins(base_travel, t, req.use_live_constraints)
            arrive = t + travel

            open_from = minutes(p["open_from"])
            open_to = minutes(p["open_to"])

            # must be open and have time for dwell
            if not (open_from <= arrive <= open_to - p["avg_dwell_min"]):
                continue

            # respect daily budget
            if budget - p["admission_cost"] < 0:
                continue

            # must fit in this day's time window
            if arrive + p["avg_dwell_min"] > end:
                continue

            cands.append((score(p, req.strategy), p, travel))

        if not cands:
            break

        cands.sort(key=lambda x: x[0], reverse=True)
        _, p, travel = cands[0]

        # leg (movement between points)
        legs_day.append({
            "from": cur.get("name", "Start"),
            "to": p["name"],
            "mode": req.mobility,
            "eta_min": travel,
            "day": day,
        })

        t += travel
        start_str = f"{t // 60:02d}:{t % 60:02d}"
        end_t = t + p["avg_dwell_min"]
        end_str = f"{end_t // 60:02d}:{end_t % 60:02d}"

        # record the stop; include day
        route_day.append({
            "poi_id": p["id"],
            "name": p["name"],
            "start": start_str,
            "end": end_str,
            "dwell_min": p["avg_dwell_min"],
            "admission_est": p["admission_cost"],
            "day": day,
        })

        t += p["avg_dwell_min"]
        budget -= p["admission_cost"]
        cur = p
        used_pois.add(p["id"])

    return route_day, legs_day, evals


# --------- VERY LIGHT CSP/BACKTRACK FALLBACK ---------
def csp_fill_day_by_backtracking(
    req: PlanReq,
    day: int,
    used_pois: set,
    dow: Optional[int],
):
    """
    Tiny CSP-ish layer:
    - choose a subset of POIs that fits time and budget
    - uses backtracking over a small candidate set (max 8)
    - ignores travel time to keep it simple, but respects open hours + dwell + budget
    """

    start = minutes(req.start_time)
    end = minutes(req.end_time)
    days = req.days or 1
    budget_per_day = req.budget_total / days

    # small candidate pool: top K by score that are open today
    candidates = []
    for p in POIS:
        if p["id"] in used_pois:
            continue
        if not is_poi_open_today(p, dow, req.use_live_constraints):
            continue

        candidates.append(p)

    if not candidates:
        return [], []

    # sort by "earliest closing" as MRV-like heuristic
    candidates.sort(key=lambda p: minutes(p["open_to"]))
    candidates = candidates[:8]  # keep the search tiny

    best_route: list = []
    best_score: float = float("-inf")

    def backtrack(idx: int, current_time: int, current_budget: float, chosen: list, score_sum: float):
        nonlocal best_route, best_score

        if idx == len(candidates):
            if score_sum > best_score:
                best_score = score_sum
                best_route = list(chosen)
            return

        # Option 1: skip this POI
        backtrack(idx + 1, current_time, current_budget, chosen, score_sum)

        # Option 2: try to include this POI
        p = candidates[idx]
        open_from = minutes(p["open_from"])
        open_to = minutes(p["open_to"])
        dwell = p["avg_dwell_min"]

        # ensure we respect time & budget (ignoring travel here)
        arrive = max(current_time, open_from)

        if arrive + dwell > end:
            return

        if current_budget - p["admission_cost"] < 0:
            return

        if not (open_from <= arrive <= open_to - dwell):
            return

        chosen.append(p)
        backtrack(
            idx + 1,
            arrive + dwell,
            current_budget - p["admission_cost"],
            chosen,
            score_sum + score(p, req.strategy),
        )
        chosen.pop()

    backtrack(0, start, budget_per_day, [], 0.0)

    if not best_route:
        return [], []

    # Convert best_route into route/legs without travel (legs eta=0)
    route_day = []
    legs_day = []
    t = start
    cur_name = "Start"
    for p in best_route:
        open_from = minutes(p["open_from"])
        t = max(t, open_from)
        start_str = f"{t // 60:02d}:{t % 60:02d}"
        end_t = t + p["avg_dwell_min"]
        end_str = f"{end_t // 60:02d}:{end_t % 60:02d}"

        legs_day.append({
            "from": cur_name,
            "to": p["name"],
            "mode": req.mobility,
            "eta_min": 0,
            "day": day,
        })
        cur_name = p["name"]

        route_day.append({
            "poi_id": p["id"],
            "name": p["name"],
            "start": start_str,
            "end": end_str,
            "dwell_min": p["avg_dwell_min"],
            "admission_est": p["admission_cost"],
            "day": day,
        })

        t = end_t
        used_pois.add(p["id"])

    return route_day, legs_day


# --------- SINGLE-DAY A* PLANNER ---------
def astar_plan_one_day(
    req: PlanReq,
    day: int,
    used_pois: set,
    speed_kmh: float,
    dow: Optional[int],
):
    """
    A* over a small candidate set.
    - State: (time, budget, visited_mask, last_idx)
    - Cost g = -sum(score(p)) so far (we want to maximize score)
    - Heuristic h = 0 (still valid A*; just less pruned)
    """

    start = minutes(req.start_time)
    end = minutes(req.end_time)
    days = req.days or 1
    budget_per_day = req.budget_total / days

    # Candidate pool: open today & not previously used
    cand_list = []
    for p in POIS:
        if p["id"] in used_pois:
            continue
        if not is_poi_open_today(p, dow, req.use_live_constraints):
            continue
        cand_list.append(p)

    # limit to top K by static score to keep state small
    cand_list.sort(key=lambda p: score(p, req.strategy), reverse=True)
    cand_list = cand_list[:8]

    if not cand_list:
        return [], [], 0

    # Build A* frontier
    start_node = AStarNode(
        f=0.0,
        g=0.0,
        time=start,
        budget=budget_per_day,
        cur_idx=-1,
        visited_mask=0,
        route=[],
        legs=[],
    )
    frontier: List[AStarNode] = []
    heappush(frontier, start_node)

    best_node = start_node
    visited_states = {}

    expansions = 0
    while frontier:
        node = heappop(frontier)
        expansions += 1

        # If this node is better than the best we've seen, keep it
        if node.g < best_node.g:  # remember g is negative total score
            best_node = node

        # expand
        for i, p in enumerate(cand_list):
            mask = 1 << i
            if node.visited_mask & mask:
                continue

            # check time/budget constraints
            cur_loc = {"lat": 42.3601, "lon": -71.0589}
            if node.cur_idx >= 0:
                cur_loc = cand_list[node.cur_idx]

            base_travel = haversine_mins(cur_loc, p, speed_kmh=speed_kmh)
            travel = adjust_travel_mins(base_travel, node.time, req.use_live_constraints)
            arrive = node.time + travel

            open_from = minutes(p["open_from"])
            open_to = minutes(p["open_to"])
            dwell = p["avg_dwell_min"]

            if not (open_from <= arrive <= open_to - dwell):
                continue

            if arrive + dwell > end:
                continue

            if node.budget - p["admission_cost"] < 0:
                continue

            # cost / score
            poi_score = score(p, req.strategy)
            new_g = node.g - poi_score  # negative sum of scores
            new_time = arrive + dwell
            new_budget = node.budget - p["admission_cost"]
            new_mask = node.visited_mask | mask

            # record route + legs
            new_route = list(node.route)
            new_legs = list(node.legs)

            leg_from = "Start" if node.cur_idx < 0 else cand_list[node.cur_idx]["name"]
            leg = {
                "from": leg_from,
                "to": p["name"],
                "mode": req.mobility,
                "eta_min": travel,
                "day": day,
            }
            new_legs.append(leg)

            start_str = f"{arrive // 60:02d}:{arrive % 60:02d}"
            end_str = f"{new_time // 60:02d}:{new_time % 60:02d}"
            new_route.append({
                "poi_id": p["id"],
                "name": p["name"],
                "start": start_str,
                "end": end_str,
                "dwell_min": dwell,
                "admission_est": p["admission_cost"],
                "day": day,
            })

            # Heuristic: 0 (we could add optimistic future score, but not needed)
            h = 0.0
            f = new_g + h

            state_key = (new_mask, i)
            if state_key in visited_states and visited_states[state_key] <= new_g:
                continue
            visited_states[state_key] = new_g

            new_node = AStarNode(
                f=f,
                g=new_g,
                time=new_time,
                budget=new_budget,
                cur_idx=i,
                visited_mask=new_mask,
                route=new_route,
                legs=new_legs,
            )
            heappush(frontier, new_node)

    # best_node has the highest score (lowest negative g) found
    return best_node.route, best_node.legs, expansions


# --------- MULTI-DAY WRAPPERS ---------
def plan_multi_day_greedy(req: PlanReq, days: int, speed_kmh: float, dow: Optional[int]):
    used_pois = set()
    all_stops = []
    all_legs = []
    total_evals = 0

    for day in range(1, days + 1):
        route_day, legs_day, day_evals = greedy_plan_one_day(req, day, used_pois, speed_kmh, dow)
        total_evals += day_evals

        # if greedy fails to place anything, try CSP/backtracking as a fallback
        if not route_day:
            route_day, legs_day = csp_fill_day_by_backtracking(req, day, used_pois, dow)

        all_stops.extend(route_day)
        all_legs.extend(legs_day)

    cost_summary = {
        "admissions": sum(s["admission_est"] for s in all_stops),
        "transport": 0,
        "total": sum(s["admission_est"] for s in all_stops),
    }
    return all_stops, all_legs, cost_summary, total_evals


def plan_multi_day_astar(req: PlanReq, days: int, speed_kmh: float, dow: Optional[int]):
    used_pois = set()
    all_stops = []
    all_legs = []
    total_expansions = 0

    for day in range(1, days + 1):
        route_day, legs_day, day_exp = astar_plan_one_day(req, day, used_pois, speed_kmh, dow)
        total_expansions += day_exp

        # if A* fails to place anything, fall back to CSP, then greedy
        if not route_day:
            route_day, legs_day = csp_fill_day_by_backtracking(req, day, used_pois, dow)

        if not route_day:
            route_day, legs_day, _ = greedy_plan_one_day(req, day, used_pois, speed_kmh, dow)

        for stop in route_day:
            used_pois.add(stop["poi_id"])

        all_stops.extend(route_day)
        all_legs.extend(legs_day)

    cost_summary = {
        "admissions": sum(s["admission_est"] for s in all_stops),
        "transport": 0,
        "total": sum(s["admission_est"] for s in all_stops),
    }
    return all_stops, all_legs, cost_summary, total_expansions


# --------- ENDPOINTS ---------
@app.get("/pois")
def get_pois():
    return {"pois": POIS}


@app.post("/plan")
def plan(req: PlanReq):
    """
    High-level planner:
    - Normalizes preferences.like into CURRENT_PREFS_LIKE
    - Chooses travel speed from mobility/has_car
    - Strategy:
        * "astar_*" -> A* planner
        * otherwise -> greedy + CSP fallback
    - Adds multi-day structure and cost summary
    - Returns extra metrics for evaluation
    """

    global CURRENT_PREFS_LIKE

    # 1) preferences.like -> CURRENT_PREFS_LIKE (normalized to match CSV categories)
    raw_like = []
    if isinstance(req.preferences, dict):
        raw_like = req.preferences.get("like", []) or []

    CATEGORY_MAP = {
        "museums": "museums",
        "museum": "museums",
        "restaurants": "food",
        "restaurants + cafes": "food",
        "cafes": "food",
        "coffee": "food",
        "seafood": "seafood",
        "history": "history",
        "outdoors": "outdoors",
        "parks": "outdoors",
        "park": "outdoors",
        "nightlife": "nightlife",
        "shopping": "shopping",
    }

    normalized = set()
    for x in raw_like:
        key = str(x).strip().lower()
        normalized.add(CATEGORY_MAP.get(key, key))

    CURRENT_PREFS_LIKE = normalized

    # 2) travel speed and date info
    speed_kmh = get_speed_kmh(req)
    days = req.days or 1
    dow = parse_day_of_week(req.date)

    # 3) choose planner + measure runtime
    t0 = time.perf_counter()
    if req.strategy.startswith("astar"):
        stops, legs, summary, search_effort = plan_multi_day_astar(req, days, speed_kmh, dow)
        planner_name = "astar"
    else:
        stops, legs, summary, search_effort = plan_multi_day_greedy(req, days, speed_kmh, dow)
        planner_name = "greedy"
    t1 = time.perf_counter()
    runtime_ms = (t1 - t0) * 1000.0

    # Total score for this itinerary
    total_score = 0.0
    for s in stops:
        poi = next((p for p in POIS if p["id"] == s["poi_id"]), None)
        if poi:
            total_score += score(poi, req.strategy)

    total_travel_min = sum(l["eta_min"] for l in legs)

    metrics = {
        "planner": planner_name,
        "runtime_ms": runtime_ms,
        "total_stops": len(stops),
        "total_travel_min": total_travel_min,
        "total_score": total_score,
        "search_effort": search_effort,
    }

    return {
        "itinerary_id": f"bos-{req.date}-001",
        "stops": stops,
        "legs": legs,
        "cost_summary": summary,
        "metrics": metrics,
    }


@app.post("/feedback")
def feedback(fb: Feedback):
    """
    Simple preference learning:
    - Look up POI by id
    - Add rating to that category's history
    - score() uses CATEGORY_RATINGS to bias scores
    """
    poi = next((p for p in POIS if p["id"] == fb.poi_id), None)
    if poi:
        cat = poi["category"].lower()
        CATEGORY_RATINGS[cat].append(fb.rating)
    return {"ok": True}
