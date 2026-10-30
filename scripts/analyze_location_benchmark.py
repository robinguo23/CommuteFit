#!/usr/bin/env python3
"""Score saved public place-search observations against independent area references.
No network requests or credentials. Run from the project root.
"""
import collections
import csv
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'docs/location-benchmark'
LIMIT = 0.6 * 1609.344

def distance(candidate, reference):
    lat1, lat2 = map(math.radians, (candidate['lat'], reference['latitude']))
    dlat = lat2 - lat1
    dlon = math.radians(reference['longitude'] - candidate['lon'])
    a = math.sin(dlat / 2)**2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon / 2)**2
    return 2 * 6371000 * math.asin(min(1, math.sqrt(a)))

def main():
    payload = json.loads((BASE / 'results-2026-10-29.json').read_text())
    samples = {s['id']: s for s in payload['dataset']['samples']}
    references = {r['id']: r for r in json.loads((BASE / 'reference-coordinates.json').read_text())['references']}
    rows = []
    for run in payload['runs']:
        sample = samples[run['id']]
        reference = references[run['id']]
        data = run.get('data', {})
        areas = data.get('areas', [])
        chosen = areas[0]['representative'] if areas else None
        metres = distance(chosen, reference['result']) if chosen else None
        status = 'request-error' if run.get('error') else 'no-consensus' if not chosen else 'within-area' if metres <= LIMIT else 'outside-area'
        row = {'id': run['id'], 'brand': sample['brand'], 'store': sample['store'], 'rule': run['rule'], 'query': run['query'], 'status': status,
               'distanceMetres': round(metres, 1) if metres is not None else '', 'selectedName': chosen['name'] if chosen else '',
               'selectedAddress': chosen['address'] if chosen else '', 'selectedPostcode': chosen.get('postcode', '') if chosen else '',
               'referencePostcode': reference['postcode'], 'referenceSource': reference.get('coordinateSource', reference['source']),
               'areaCount': len(areas), 'elapsedMs': run['elapsedMs']}
        for provider in data.get('providers', []):
            candidates = provider['result'].get('candidates', [])
            distances = [distance(c, reference['result']) for c in candidates]
            row[provider['provider'] + 'AnyWithinArea'] = any(d <= LIMIT for d in distances)
            row[provider['provider'] + 'FirstWithinArea'] = bool(distances and distances[0] <= LIMIT)
        rows.append(row)
    rules = list(dict.fromkeys(r['rule'] for r in rows))
    stats = {}
    for rule in rules:
        group = [r for r in rows if r['rule'] == rule]
        stats[rule] = {'executed': len(group), **dict(collections.Counter(r['status'] for r in group))}
        stats[rule]['perBrand'] = {brand: dict(collections.Counter(r['status'] for r in group if r['brand'] == brand)) for brand in dict.fromkeys(s['brand'] for s in samples.values())}
        stats[rule]['providers'] = {p: {'anyWithinArea': sum(r.get(p + 'AnyWithinArea', False) for r in group), 'firstWithinArea': sum(r.get(p + 'FirstWithinArea', False) for r in group)} for p in ['locationiq', 'arcgis', 'tomtom']}
    fields = list(dict.fromkeys(k for r in rows for k in r))
    with (BASE / 'scored-results.csv').open('w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)
    summary = {'toleranceMetres': LIMIT, 'referenceMethod': 'Official job/store postcode centroid, historical postcode where labelled; J36 terminal-level Google Maps reference replaces unsuitable airport postcode centroid. Measures vicinity, not branch identity or access.', 'rules': stats}
    (BASE / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n')
    for rule, stat in stats.items():
        print(rule, {k: v for k, v in stat.items() if k not in ['perBrand', 'providers']})
    print('Saved scored-results.csv and summary.json')

if __name__ == '__main__':
    main()
