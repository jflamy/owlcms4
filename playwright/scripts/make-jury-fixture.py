#!/usr/bin/env python3
"""
make-jury-fixture.py - build the database fixture of the jury scenarios from an owlcms JSON export.

The export must have its lifts cleared and contain sessions M1 and M2. The fixture keeps M1 as is and prepares
single-athlete sessions for the scenarios that need a lift that ends something. A scenario uses up its session, so
there is one session per scenario:

  M2, M4  one athlete with two snatches done: the next lift is the last snatch of the session, which starts the
          break before clean and jerk
  M3, M5  one athlete with three snatches and two clean and jerks done: the next lift is the last lift of the
          session

The athletes are the M2 athletes with the lowest snatch declarations. The other M2 athletes lose their session.

Usage (from the repository root):
    python3 playwright/scripts/make-jury-fixture.py <export.json> [playwright/fixtures/jury-scenarios.json]
"""
import json
import sys
from datetime import datetime, timedelta
from pathlib import Path

DEFAULT_OUT = Path(__file__).resolve().parent.parent / "fixtures" / "jury-scenarios.json"


def good_lifts(athlete, lift, first_weight, count, start):
    """
    Record `count` good lifts from the declared first weight, each 1 kg above the previous one and one minute apart
    from `start`. Returns the best lift.
    """
    weight = first_weight
    for n in range(1, 4):
        prefix = f"{lift}{n}"
        athlete[prefix + "AutomaticProgression"] = "-" if n == 1 else str(weight)
        if n <= count:
            athlete[prefix + "ActualLift"] = str(weight)
            athlete[prefix + "Decisions"] = [True, True, True]
            athlete[prefix + "LiftTime"] = (start + timedelta(minutes=n)).isoformat()
            weight += 1
        elif n > count + 1:
            athlete[prefix + "AutomaticProgression"] = "0"
    return weight - 1


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    source = Path(sys.argv[1])
    out = Path(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_OUT
    data = json.loads(source.read_text(encoding="utf-8"))

    groups = {g["name"]: g for g in data["groups"]}
    sessions = [("M2", "snatch"), ("M3", "lift"), ("M4", "snatch"), ("M5", "lift")]
    if "M1" not in groups or "M2" not in groups or any(name in groups for name, _ in sessions[1:]):
        sys.exit("the export must contain sessions M1 and M2, and no session M3, M4 or M5")
    lifted = [a for a in data["athletes"] if any(
        a.get(f"{lift}{n}ActualLift") for lift in ("snatch", "cleanJerk") for n in (1, 2, 3))]
    if lifted:
        sys.exit(f"the export must have its lifts cleared: {len(lifted)} athlete(s) have lifts")

    m2 = groups["M2"]
    members = sorted((a for a in data["athletes"] if a.get("group") == m2["id"]),
                     key=lambda a: (int(a["snatch1Declaration"]), a["lotNumber"]))
    if len(members) < len(sessions):
        sys.exit(f"session M2 needs at least {len(sessions)} athletes")
    for athlete in members[len(sessions):]:
        athlete["group"] = None
        athlete["startNumber"] = None

    # the lifts already done are dated from the start of the session
    start = datetime.fromisoformat(m2["competitionTime"])
    next_id = max(g["id"] for g in data["groups"]) + 1
    for (name, kind), athlete in zip(sessions, members):
        if name == "M2":
            group = m2
        else:
            group = dict(m2)
            group["id"] = next_id
            group["name"] = name
            next_id += 1
            data["groups"].append(group)
        athlete["group"] = group["id"]
        athlete["startNumber"] = 1
        group["nbAthletes"] = 1
        if kind == "snatch":
            # the next lift is the third snatch
            good_lifts(athlete, "snatch", int(athlete["snatch1Declaration"]), 2, start)
            group["nbAttemptedLifts"] = 2
        else:
            # the next lift is the third clean and jerk
            best_snatch = good_lifts(athlete, "snatch", int(athlete["snatch1Declaration"]), 3, start)
            best_clean_jerk = good_lifts(athlete, "cleanJerk", int(athlete["cleanJerk1Declaration"]), 2,
                                         start + timedelta(minutes=10))
            athlete["total"] = best_snatch + best_clean_jerk
            group["nbAttemptedLifts"] = 5
        print(f"{name}: {athlete['lastName']}, {athlete['firstName']} (next: third "
              f"{'snatch' if kind == 'snatch' else 'clean and jerk'})")

    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"{len(members) - len(sessions)} athlete(s) of M2 no longer have a session")
    print(f"wrote {out}")


if __name__ == "__main__":
    main()
