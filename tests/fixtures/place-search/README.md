# Place Search Regression Baseline v1

First cross-brand regression snapshot for CommuteFit `/place-demo`. Use it as a
benchmark: after any change to `GeoapifyClient`, `AreaConsensusService.rejection()`,
or `AreaConsensusService.assess()`, re-run the same 10 queries and diff the output.

## What is recorded

- `brand-regression-v1.csv` — one row per query. `actual_status` / `provider_count` /
  `max_distance_m` are the system's real output; `ground_truth_status` / `evaluation`
  are our independent judgment. These two columns MUST stay separate.
- `raw-snapshot-v1.json` — raw consensus API responses captured from the running backend,
  with metadata (capture time, provider family, threshold, build info).

## Reproduce

Backend on `127.0.0.1:8080`, then for each query:

```
curl "http://127.0.0.1:8080/api/place-search/consensus?query=<urlencoded>"
```

Provider family at baseline: `geoapify`, `arcgis`, `tomtom` (independent data families;
LocationIQ kept for direct lookups only). Cluster diameter limit = 0.6 mile = 965.6 m.

## Key correctness principle proven by this run

Multi-source agreement proves the sources agree on coordinates; it does NOT prove the
brand, area, or postcode are right. B&M Woolwich is the live example: ArcGIS + TomTom
agreed within 12 m, but the returned postcode (SE7 7RJ) differs from the official store
listing (SE7 8LU). Always verify ground truth independently.

## Flagged for the next investigation round (do NOT count as PASS)

1. **B&M postcode mismatch** — returned SE7 7RJ vs official SE7 8LU. Two sources agreed
   on a possibly-wrong branch. Investigate whether the selected store is the intended one.
2. **Co-op name-variant instability** — `Co-op Greenwich` -> ambiguous, but `Coop Greenwich`
   -> needs-confirmation. No brand collision, but normalization should give stable confidence.
3. **B&Q Charlton failure** — 24 candidates, zero cluster. Determine whether the correct
   B&Q (official: B&Q Greenwich, 59 Bugsby Way, SE10 0QJ) was recalled, wrongly excluded by
   `rejection()`, or recalled but failed geometric clustering.
4. **Sainsbury's wide cluster** — 566–575 m diameter may merge the Calderwood St store with
   the Woolwich Arsenal Local. Acceptable for commute screening but recorded as a risk.

## Expected stable behavior (do not regress)

- `&` and `and` forms must return the same status and coordinates for the same brand.
- Dropping apostrophes / hyphens must not cause cross-brand collision (B&M must never match
  H&M or B&Q — verified in v1).
- Multiple-branch areas (H&M Oxford St, Co-op Greenwich) SHOULD return ambiguous /
  needs-confirmation, not a random pick.
