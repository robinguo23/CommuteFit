#!/usr/bin/env python3
"""Opt-in, low-volume TfL location format observation. Never logs app_key."""
import argparse
import json
import os
import ssl
import time
from datetime import datetime, timedelta
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import quote, urlencode
from urllib.request import Request, urlopen
from zoneinfo import ZoneInfo

CASES = [
    ("coordinate", "51.5178,-0.0823"),
    ("postcode", "E1 6AN"),
    ("naptan", "940GZZLULVT"),
    ("ics", "1000138"),
    ("free-text", "Liverpool Street"),
]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--live", action="store_true", help="Make five real TfL requests")
    parser.add_argument("--date", default=(datetime.now(ZoneInfo("Europe/London")) + timedelta(days=1)).strftime("%Y%m%d"))
    parser.add_argument("--time", default="0900")
    parser.add_argument("--output", type=Path, default=Path("docs/tfl-location-types-live.json"))
    args = parser.parse_args()
    datetime.strptime(args.date + args.time, "%Y%m%d%H%M")
    if not args.live:
        print(json.dumps({"mode": "dry-run", "cases": CASES, "note": "Use --live for five real requests. These points are not identical and durations need not match."}, indent=2))
        return
    key = os.environ.get("TFL_APP_KEY", "")
    env_file = Path(__file__).resolve().parents[1] / ".env"
    if not key and env_file.exists():
        for line in env_file.read_text().splitlines():
            if line.strip().startswith("TFL_APP_KEY="):
                key = line.split("=", 1)[1].strip().strip("\"'")
                break
    context = ssl.create_default_context()
    try:
        import certifi
        context = ssl.create_default_context(cafile=certifi.where())
    except ImportError:
        pass
    rows = []
    for kind, origin in CASES:
        destination = "940GZZLUMGT"
        params = {"date": args.date, "time": args.time, "timeIs": "Departing", "calcOneDirection": "true"}
        if key:
            params["app_key"] = key
        path = "/Journey/JourneyResults/" + quote(origin, safe="") + "/to/" + quote(destination, safe="")
        url = "https://api.tfl.gov.uk" + path + "?" + urlencode(params)
        row = {"kind": kind, "from": origin, "to": destination, "date": args.date, "time": args.time}
        try:
            req = Request(url, headers={"User-Agent": "CommuteFit/1.0 (five location format checks)", "Accept": "application/json"})
            try:
                response = urlopen(req, timeout=20, context=context)
            except HTTPError as error:
                response = error  # 300 disambiguation JSON is still useful.
            with response:
                row["http_status"] = response.code
                body = response.read(2_000_001)
            if len(body) > 2_000_000:
                row["outcome"] = "oversized-response"
            else:
                try:
                    data = json.loads(body)
                    row["journeys"] = len(data.get("journeys", []))
                    row["from_candidates"] = len(data.get("fromLocationDisambiguation", {}).get("disambiguationOptions", []))
                    row["to_candidates"] = len(data.get("toLocationDisambiguation", {}).get("disambiguationOptions", []))
                    row["outcome"] = "routes" if row["journeys"] else "disambiguation" if row["from_candidates"] or row["to_candidates"] else "no-routes-or-provider-error"
                except (ValueError, AttributeError):
                    row["outcome"] = "non-json-response"
        except (URLError, TimeoutError, OSError):
            row["outcome"] = "network-unavailable"
        rows.append(row)
        print(json.dumps(row, ensure_ascii=False))
        time.sleep(1)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps({"observed_at": datetime.now(ZoneInfo("Europe/London")).isoformat(),
        "note": "Live observation, not a guarantee of coverage or equivalent endpoints.", "cases": rows}, ensure_ascii=False, indent=2))
    print("Saved report:", args.output)


if __name__ == "__main__":
    main()
