#!/usr/bin/env python3
"""Probe TfL Journey Planner and FareTo; preserve evidence without guessing."""

from __future__ import annotations

import argparse
import datetime as dt
import json
import json
import os
import pathlib
import re
import ssl
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid


BASE = "https://api.tfl.gov.uk"
DEFAULT_FROM = "940GZZLUCHX"
DEFAULT_TO = "940GZZLUBST"
FARE_KEYS = {
    "fare", "fares", "totalcost", "cost", "amount", "price", "currency",
    "peak", "offpeak", "ishopperfare", "taps", "ticketsavailable", "rows",
    "tickettype", "passengertype", "paymentmethod", "chargelevel",
    "chargeprofilename", "lowzone", "highzone", "caveats",
}
TOKEN_PATTERN = re.compile(r"(?i)(app_key|app_id)=([^&\s]+)")


def utc_now() -> dt.datetime:
    return dt.datetime.now(dt.timezone.utc)


def safe_url(url: str) -> str:
    return TOKEN_PATTERN.sub(r"\1=<redacted>", url)


def write_json(path: pathlib.Path, value: object) -> None:
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def fare_paths(value: object, path: str = "$", out: list[dict[str, object]] | None = None) -> list[dict[str, object]]:
    if out is None:
        out = []
    if isinstance(value, dict):
        for key, child in value.items():
            child_path = f"{path}.{key}"
            if key.lower() in FARE_KEYS:
                out.append({"path": child_path, "value": child})
            fare_paths(child, child_path, out)
    elif isinstance(value, list):
        for index, child in enumerate(value):
            fare_paths(child, f"{path}[{index}]", out)
    return out


def add_credentials(params: dict[str, str]) -> dict[str, str]:
    result = dict(params)
    for env_name, query_name in (("TFL_APP_ID", "app_id"), ("TFL_APP_KEY", "app_key")):
        value = os.environ.get(env_name)
        if value:
            result[query_name] = value
    return result


def build_requests(args: argparse.Namespace) -> list[dict[str, object]]:
    requests: list[dict[str, object]] = []
    pairs = list(zip(args.from_stop, args.to_stop))
    if not pairs:
        pairs = [(DEFAULT_FROM, DEFAULT_TO)]
    if not args.no_journey:
        query = add_credentials({
            "date": args.date,
            "time": args.time.replace(":", ""),
            "timeIs": args.time_is,
            "mode": args.mode,
        })
        for from_stop, to_stop in pairs:
            path = f"/Journey/JourneyResults/{urllib.parse.quote(from_stop, safe='')}/to/{urllib.parse.quote(to_stop, safe='')}"
            requests.append({"name": f"journey-{from_stop}-to-{to_stop}", "path": path, "params": query})
    if not args.no_fareto:
        query = add_credentials({})
        for from_stop, to_stop in pairs:
            path = f"/StopPoint/{urllib.parse.quote(from_stop, safe='')}/FareTo/{urllib.parse.quote(to_stop, safe='')}"
            requests.append({"name": f"fareto-{from_stop}-to-{to_stop}", "path": path, "params": query})
    return requests


def build_scenario_requests(path: pathlib.Path, args: argparse.Namespace) -> list[dict[str, object]]:
    """Load explicitly selected cases; each journey and FareTo pair is independent."""
    config = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(config, dict) or not isinstance(config.get("scenarios"), list):
        raise ValueError("scenario file must be an object with a scenarios array")
    requests: list[dict[str, object]] = []
    for scenario in config["scenarios"]:
        if not isinstance(scenario, dict) or not scenario.get("name"):
            raise ValueError("every scenario needs a non-empty name")
        label = re.sub(r"[^A-Za-z0-9_-]+", "-", str(scenario["name"])).strip("-")
        journey = scenario.get("journey")
        journey_items = scenario.get("journeys")
        if journey is not None and journey_items is not None:
            raise ValueError(f"scenario {label}: use either journey or journeys, not both")
        if journey is not None:
            journey_items = [journey]
        if journey_items is not None and not isinstance(journey_items, list):
            raise ValueError(f"scenario {label}: journeys must be an array")
        if journey_items is not None and not args.no_journey:
            for journey_index, journey in enumerate(journey_items, start=1):
                if not isinstance(journey, dict) or not journey.get("from") or not journey.get("to"):
                    raise ValueError(f"scenario {label}: every journey needs from and to")
                modes = journey.get("modes", ["tube"])
                if not isinstance(modes, list) or not modes:
                    raise ValueError(f"scenario {label}: journey.modes must be a non-empty array")
                params = add_credentials({
                    "date": str(journey.get("date", config.get("date", dt.date.today().strftime("%Y%m%d")))),
                    "time": str(journey.get("time", config.get("time", "0900"))).replace(":", ""),
                    "timeIs": str(journey.get("timeIs", "Departing")),
                    "mode": ",".join(str(mode) for mode in modes),
                })
                if journey.get("via"):
                    params["via"] = str(journey["via"])
                encoded_from = urllib.parse.quote(str(journey["from"]), safe="")
                encoded_to = urllib.parse.quote(str(journey["to"]), safe="")
                suffix = "" if len(journey_items) == 1 else f"-{journey_index}"
                requests.append({
                    "name": f"{label}-journey{suffix}",
                    "scenario": label,
                    "purpose": journey.get("purpose", scenario.get("purpose", "whole-journey-route-and-fare")),
                    "path": f"/Journey/JourneyResults/{encoded_from}/to/{encoded_to}",
                    "params": params,
                })
        for index, fare in enumerate(scenario.get("fareTo", []) if not args.no_fareto else [], start=1):
            if not isinstance(fare, dict) or not fare.get("from") or not fare.get("to"):
                raise ValueError(f"scenario {label}: each fareTo item needs from and to stop IDs")
            params = add_credentials({})
            fare_from = urllib.parse.quote(str(fare["from"]), safe="")
            fare_to = urllib.parse.quote(str(fare["to"]), safe="")
            requests.append({
                "name": f"{label}-fareto-{index}",
                "scenario": label,
                "purpose": fare.get("purpose", "explicit-station-pair-cross-check"),
                "path": f"/StopPoint/{fare_from}/FareTo/{fare_to}",
                "params": params,
            })
    return requests


def fetch(request_spec: dict[str, object], output_dir: pathlib.Path, timeout: float) -> dict[str, object]:
    name = str(request_spec["name"])
    params = request_spec["params"]
    assert isinstance(params, dict)
    path = str(request_spec["path"])
    query = urllib.parse.urlencode(params)
    url = f"{BASE}{path}" + (f"?{query}" if query else "")
    request_time = utc_now()
    req = urllib.request.Request(
        url,
        headers={"Accept": "application/json", "User-Agent": "TfL-Fare-API-Demo/1.0"},
        method="GET",
    )
    # Honour standard CA bundle variables and common macOS Python installations.
    # Certificate verification remains enabled; users can point SSL_CERT_FILE at
    # a trusted corporate/system bundle when Python's bundled roots are stale.
    cafile = os.environ.get("SSL_CERT_FILE") or os.environ.get("REQUESTS_CA_BUNDLE")
    context = ssl.create_default_context(cafile=cafile) if cafile else ssl.create_default_context()
    result: dict[str, object] = {
        "name": name,
        "scenario": request_spec.get("scenario"),
        "purpose": request_spec.get("purpose"),
        "requestedAtUtc": request_time.isoformat(),
        "request": {"method": "GET", "url": safe_url(url), "parameters": {k: ("<redacted>" if k in {"app_id", "app_key"} else v) for k, v in params.items()}},
    }
    body_path = output_dir / f"{name}.body"
    headers_path = output_dir / f"{name}.headers"
    try:
        with urllib.request.urlopen(req, timeout=timeout, context=context) as response:
            raw = response.read()
            headers = response.headers
            status = response.status
            result.update({"httpStatus": status, "responseReceivedAtUtc": utc_now().isoformat(), "contentType": headers.get("Content-Type")})
            headers_path.write_text("".join(f"{k}: {v}\n" for k, v in headers.items()), encoding="utf-8")
    except urllib.error.HTTPError as error:
        raw = error.read()
        headers = error.headers
        status = error.code
        result.update({"httpStatus": status, "responseReceivedAtUtc": utc_now().isoformat(), "contentType": headers.get("Content-Type")})
        headers_path.write_text("".join(f"{k}: {v}\n" for k, v in headers.items()), encoding="utf-8")
    except Exception as error:  # Keep the failure explicit; there is no HTTP status/body.
        result.update({"httpStatus": None, "error": {"type": type(error).__name__, "message": str(error)}, "responseReceivedAtUtc": utc_now().isoformat()})
        print(f"{name}: request failed before an HTTP response ({type(error).__name__}: {error})", file=sys.stderr)
        return result

    body_path.write_bytes(raw)
    result["bodyFile"] = body_path.name
    result["headersFile"] = headers_path.name
    try:
        parsed = json.loads(raw.decode("utf-8"))
        result["json"] = parsed
        result["fareFieldPaths"] = fare_paths(parsed)
        if "/FareTo/" in path:
            if isinstance(parsed, list) and not parsed:
                result["fareResponseStatus"] = "no-usable-fare-empty-array"
                result["fareResponseNote"] = "TfL returned []; this is not a zero fare."
            elif isinstance(parsed, list):
                result["fareResponseStatus"] = "fare-records-returned"
            else:
                result["fareResponseStatus"] = "unexpected-response-shape"
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        result["jsonParseError"] = str(error)
        if "/FareTo/" in path:
            result["fareResponseStatus"] = "unparseable-response"
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--date", default=dt.date.today().strftime("%Y%m%d"), help="Journey date, YYYYMMDD (TfL Journey Planner format)")
    parser.add_argument("--time", default="09:00", help="Journey time, HH:mm")
    parser.add_argument("--time-is", choices=("Departing", "Arriving"), default="Departing")
    parser.add_argument("--mode", default="tube", help="Journey Planner mode filter (default: tube)")
    parser.add_argument("--from-stop", action="append", default=[], help="Origin stop ID; repeat alongside --to-stop for scenarios")
    parser.add_argument("--to-stop", action="append", default=[], help="Destination stop ID; repeat alongside --from-stop")
    parser.add_argument("--timeout", type=float, default=25.0)
    parser.add_argument("--ca-bundle", help="Trusted PEM CA bundle (also accepts SSL_CERT_FILE / REQUESTS_CA_BUNDLE)")
    parser.add_argument("--scenarios-file", type=pathlib.Path, help="JSON scenario batch; when used, replaces the default single-pair requests")
    parser.add_argument("--no-journey", action="store_true")
    parser.add_argument("--no-fareto", action="store_true")
    args = parser.parse_args()
    if args.ca_bundle:
        os.environ["SSL_CERT_FILE"] = args.ca_bundle
    elif not os.environ.get("SSL_CERT_FILE") and not os.environ.get("REQUESTS_CA_BUNDLE"):
        # The python.org macOS installer uses its own OpenSSL trust store. Its
        # bundled Install Certificates.command typically installs certifi here.
        try:
            import certifi  # type: ignore[import-not-found]

            os.environ["SSL_CERT_FILE"] = certifi.where()
        except ImportError:
            pass
    if len(args.from_stop) != len(args.to_stop):
        parser.error("--from-stop and --to-stop must be supplied in matching pairs")
    try:
        if not re.fullmatch(r"\d{8}", args.date):
            raise ValueError("expected YYYYMMDD")
        dt.datetime.strptime(args.date, "%Y%m%d")
    except ValueError:
        parser.error("--date must be YYYYMMDD, as required by the live TfL Journey Planner endpoint")
    if not re.fullmatch(r"(?:\d{2}:\d{2}|\d{4})", args.time):
        parser.error("--time must use HH:mm or HHmm")
    normalized_time = args.time.replace(":", "")
    if int(normalized_time[:2]) > 23 or int(normalized_time[2:]) > 59:
        parser.error("--time must be a valid 24-hour time")

    run_id = utc_now().strftime("%Y%m%dT%H%M%SZ") + "-" + uuid.uuid4().hex[:8]
    output_dir = pathlib.Path(__file__).resolve().parent / "runs" / run_id
    output_dir.mkdir(parents=True, exist_ok=False)
    request_specs = build_scenario_requests(args.scenarios_file, args) if args.scenarios_file else build_requests(args)
    records = [fetch(spec, output_dir, args.timeout) for spec in request_specs]
    effective_ca = os.environ.get("SSL_CERT_FILE") or os.environ.get("REQUESTS_CA_BUNDLE")
    manifest = {
        "runId": run_id,
        "baseUrl": BASE,
        "tlsVerification": "enabled",
        "caBundle": effective_ca or "Python/OpenSSL default trust store",
        "startedAtUtc": records[0]["requestedAtUtc"] if records else utc_now().isoformat(),
        "finishedAtUtc": utc_now().isoformat(),
        "notes": [
            "Raw responses are saved only when an HTTP response is received.",
            "FareTo [] means no usable fare returned, not a zero fare.",
            "No assumption is made about non-empty FareTo response currency units or journey equivalence.",
            "Journey legs and fare-bearing intervals are distinct concepts; do not sum legs as fares.",
        ],
        "requests": records,
    }
    write_json(output_dir / "manifest.json", manifest)
    print(f"Run evidence: {output_dir}")
    for record in records:
        print(f"{record['name']}: HTTP {record.get('httpStatus') if record.get('httpStatus') is not None else 'none'}")
        if record.get("fareResponseStatus") == "no-usable-fare-empty-array":
            print("  FareTo returned an empty array: no usable fare returned (not £0).")
        elif record.get("fareResponseStatus"):
            print(f"  FareTo response: {record['fareResponseStatus']}")
        for field in record.get("fareFieldPaths", []):
            print(f"  {field['path']} = {json.dumps(field['value'], ensure_ascii=False)}")
    return 0 if all(record.get("httpStatus") is not None and 200 <= int(record["httpStatus"]) < 300 for record in records) else 2


if __name__ == "__main__":
    raise SystemExit(main())
