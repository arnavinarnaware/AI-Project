from fastapi import FastAPI
from pydantic import BaseModel
from typing import Optional

import csv, math

app = FastAPI()

# Load data from CSV file
POIS = []
CURRENT_PREFS_LIKE: set[str] = set()

with open("pois_boston_seed.csv") as f:
    for row in csv.DictReader(f):
        row["lat"] = float(row["lat"])
        row["lon"] = float(row["lon"])
        row["avg_dwell_min"] = int(row["avg_dwell_min"])
        row["admission_cost"] = float(row["admission_cost"])
        POIS.append(row)

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

    # NEW FIELDS that match your Android UI
    days: Optional[int] = None
    has_car: Optional[bool] = None
    max_distance_miles: Optional[int] = None

def minutes(t):
    h, m = map(int, t.split(":"))
    return h * 60 + m

def haversine_mins(a, b, speed_kmh=5.0):
    R = 6371
    dlat = math.radians(b["lat"] - a["lat"])
    dlon = math.radians(b["lon"] - a["lon"])
    la1, la2 = math.radians(a["lat"]), math.radians(b["lat"])
    x = math.sin(dlat / 2) ** 2 + math.cos(la1) * math.cos(la2) * math.sin(dlon / 2) ** 2
    km = 2 * R * math.asin(math.sqrt(x))
    hours = km / max(speed_kmh, 1e-6)
    return int(hours * 60)

def price_norm(p): return {"$":0.2,"$$":0.5,"$$$":0.9}.get(p,0.5)

def score(poi, strategy):
    base = 0.0
    if strategy == "static_budget":
        base += -(price_norm(poi["price_tier"])) * 1.5
        base += 0.6 if poi["category"] in {"history","outdoors"} else 0
    elif strategy == "static_explorer":
        base += -price_norm(poi["price_tier"]) * 0.4
        base += 0.8 if poi["category"] in {"museums","food","history"} else 0
    else:
        base += -price_norm(poi["price_tier"]) * 0.4
        base += 0.8 if poi["category"] in {"museums","food","history"} else 0
    
    # NEW: preference bump (we'll pass a set of liked categories)
    likes = CURRENT_PREFS_LIKE  # we’ll define this global shortly
    if likes and poi["category"] in likes:
        base += 1.0
    
    return base

@app.get("/pois")
def get_pois():
    return {"pois": POIS}

@app.post("/plan")
def plan(req: PlanReq):
    start = minutes(req.start_time)
    end = minutes(req.end_time)
    t = start
    budget = req.budget_total
    # Choose travel speed based on mobility / car access
    if req.has_car:
        speed_kmh = 25.0   # approximate city driving
    else:
        speed_kmh = {
            "walk": 5.0,
            "mbta": 15.0,
            "rideshare": 25.0,
        }.get(req.mobility.lower(), 5.0)
    
    global CURRENT_PREFS_LIKE
    # expect preferences like {"like": ["museums", "food", ...]}
    like_list = req.preferences.get("like", []) if isinstance(req.preferences, dict) else []
    CURRENT_PREFS_LIKE = {x.lower() for x in like_list}

    route = []
    legs = []
    cur = {"lat":42.3601,"lon":-71.0589,"name":"Start"}  # Boston approx

    visited = set()
    while True:
        cands = []
        for p in POIS:
            if p["id"] in visited:
                continue
            travel = haversine_mins(cur, p, speed_kmh=speed_kmh)
            arrive = t + travel
            open_from = minutes(p["open_from"])
            open_to = minutes(p["open_to"])
            if not (open_from <= arrive <= open_to - p["avg_dwell_min"]):
                continue
            if budget - (p["admission_cost"]) < 0:
                continue
            if arrive + p["avg_dwell_min"] > end:
                continue
            cands.append((score(p, req.strategy), p, travel))
        if not cands:
            break
        cands.sort(key=lambda x: x[0], reverse=True)
        s, p, travel = cands[0]
        legs.append({"from": cur.get("name", "Start"), "to": p["name"], "mode": req.mobility, "eta_min": travel})
        t += travel
        route.append({
            "poi_id": p["id"],
            "name": p["name"],
            "start": f"{t//60:02d}:{t%60:02d}",
            "end": f"{(t+p['avg_dwell_min'])//60:02d}:{(t+p['avg_dwell_min'])%60:02d}",
            "dwell_min": p["avg_dwell_min"],
            "admission_est": p["admission_cost"]
        })
        t += p["avg_dwell_min"]
        budget -= p["admission_cost"]
        cur = p
        visited.add(p["id"])

    return {
        "itinerary_id": f"bos-{req.date}-001",
        "stops": route,
        "legs": legs,
        "cost_summary": {
            "admissions": sum(s["admission_est"] for s in route),
            "transport": 0,
            "total": sum(s["admission_est"] for s in route)
        }
    }

class Feedback(BaseModel):
    itinerary_id: str
    poi_id: str
    rating: int

@app.post("/feedback")
def feedback(fb: Feedback):
    return {"ok": True}
