from fastapi import FastAPI
from pydantic import BaseModel
from typing import Optional
import csv, math

app = FastAPI()

# --------- LOAD POIS ---------
POIS = []
with open("pois_boston_seed.csv") as f:
    for row in csv.DictReader(f):
        row["lat"] = float(row["lat"])
        row["lon"] = float(row["lon"])
        row["avg_dwell_min"] = int(row["avg_dwell_min"])
        row["admission_cost"] = float(row["admission_cost"])
        POIS.append(row)

# will be set from each /plan request
CURRENT_PREFS_LIKE = set()


class PlanReq(BaseModel):
    city: str = "Boston"
    date: str
    start_time: str
    end_time: str
    budget_total: float
    mobility: str = "walk"
    preferences: dict = {}
    strategy: str = "static_budget"
    must_see: list[str] = []

    # fields coming from your Android UI
    days: Optional[int] = None
    has_car: Optional[bool] = None
    max_distance_miles: Optional[int] = None


class Feedback(BaseModel):
    itinerary_id: str
    poi_id: str
    rating: int


# --------- UTILS ---------
def minutes(t: str) -> int:
    h, m = map(int, t.split(":"))
    return h * 60 + m


def haversine_mins(a, b, speed_kmh: float = 5.0) -> int:
    R = 6371
    dlat = math.radians(b["lat"] - a["lat"])
    dlon = math.radians(b["lon"] - a["lon"])
    la1, la2 = math.radians(a["lat"]), math.radians(b["lat"])
    x = math.sin(dlat / 2) ** 2 + math.cos(la1) * math.cos(la2) * math.sin(dlon / 2) ** 2
    km = 2 * R * math.asin(math.sqrt(x))
    hours = km / max(speed_kmh, 1e-6)
    return int(hours * 60)


def price_norm(p):
    return {"$": 0.2, "$$": 0.5, "$$$": 0.9}.get(p, 0.5)


def score(poi, strategy: str) -> float:
    """
    Base scoring for each strategy, plus extra bump for liked categories.
    """
    base = 0.0
    cat = poi["category"].lower()

    if strategy == "static_budget":
        # strongly penalize expensive places, favor history/outdoors
        base += -(price_norm(poi["price_tier"])) * 1.5
        if cat in {"history", "outdoors"}:
            base += 0.6
    elif strategy == "static_explorer":
        # mild cost penalty, favor museums / food / history
        base += -price_norm(poi["price_tier"]) * 0.4
        if cat in {"museums", "food", "history"}:
            base += 0.8
    else:
        # fallback similar to explorer
        base += -price_norm(poi["price_tier"]) * 0.4
        if cat in {"museums", "food", "history"}:
            base += 0.8

    # extra bump if this category is in the user's "like" list
    if CURRENT_PREFS_LIKE and cat in CURRENT_PREFS_LIKE:
        base += 1.0

    return base


# --------- ENDPOINTS ---------
@app.get("/pois")
def get_pois():
    return {"pois": POIS}


@app.post("/plan")
def plan(req: PlanReq):
    """
    Greedy planner:
    - Uses has_car / mobility to pick travel speed
    - Uses preferences.like to bias categories
    - Uses days to generate a multi-day itinerary
      (each stop has a 'day' field in the response)
    """

    global CURRENT_PREFS_LIKE

    # 1) preferences.like -> CURRENT_PREFS_LIKE
    like_list = []
    if isinstance(req.preferences, dict):
        like_list = req.preferences.get("like", [])
    CURRENT_PREFS_LIKE = {x.lower() for x in like_list}

    # 2) travel speed based on has_car / mobility
    if req.has_car:
        speed_kmh = 25.0  # simple city driving
    else:
        mob = (req.mobility or "walk").lower()
        if mob == "mbta":
            speed_kmh = 15.0
        elif mob == "rideshare":
            speed_kmh = 25.0
        else:
            speed_kmh = 5.0  # walk

    start = minutes(req.start_time)
    end = minutes(req.end_time)
    total_budget = req.budget_total
    days = req.days or 1

    # We'll plan across all days; stops include a "day" field
    route = []
    legs = []
    visited = set()

    for day in range(1, days + 1):
        t = start
        cur = {"lat": 42.3601, "lon": -71.0589, "name": "Start"}  # Boston center

        while True:
            cands = []
            for p in POIS:
                if p["id"] in visited:
                    continue

                # travel time with chosen speed
                travel = haversine_mins(cur, p, speed_kmh=speed_kmh)
                arrive = t + travel

                open_from = minutes(p["open_from"])
                open_to = minutes(p["open_to"])

                # must be open and have time for dwell
                if not (open_from <= arrive <= open_to - p["avg_dwell_min"]):
                    continue

                # respect total budget
                if total_budget - p["admission_cost"] < 0:
                    continue

                # must fit in this day's time window
                if arrive + p["avg_dwell_min"] > end:
                    continue

                cands.append((score(p, req.strategy), p, travel))

            if not cands:
                break

            cands.sort(key=lambda x: x[0], reverse=True)
            s, p, travel = cands[0]

            # leg (movement between points)
            legs.append({
                "from": cur.get("name", "Start"),
                "to": p["name"],
                "mode": req.mobility,
                "eta_min": travel,
                "day": day
            })

            t += travel
            start_str = f"{t // 60:02d}:{t % 60:02d}"
            end_t = t + p["avg_dwell_min"]
            end_str = f"{end_t // 60:02d}:{end_t % 60:02d}"

            # record the stop; include day
            route.append({
                "poi_id": p["id"],
                "name": p["name"],
                "start": start_str,
                "end": end_str,
                "dwell_min": p["avg_dwell_min"],
                "admission_est": p["admission_cost"],
                "day": day
            })

            t += p["avg_dwell_min"]
            total_budget -= p["admission_cost"]
            cur = p
            visited.add(p["id"])

    return {
        "itinerary_id": f"bos-{req.date}-001",
        "stops": route,
        "legs": legs,
        "cost_summary": {
            "admissions": sum(s["admission_est"] for s in route),
            "transport": 0,
            "total": sum(s["admission_est"] for s in route),
        },
    }


@app.post("/feedback")
def feedback(fb: Feedback):
    # placeholder; could log ratings for adaptive learning
    return {"ok": True}