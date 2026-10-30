# Shift template proposal

Status: product proposal, not implemented. Times below are editable examples, not claims about an employer's rota. The supplied research includes individual accounts and unsupported generalisations; it does not establish prevalence. Validate with dated UK job advertisements before describing any template as common or representative.

## 2026-10-29 revision — four scenarios per daily card

Implementation sequencing: first add explicit-date scenario queries, including overnight return dates and independent direction errors; then build the editable presets and seven date cards with incremental loading; finally integrate confirmed job shifts and optional availability, and verify against real TfL routes. The current assessment endpoint derives the next occurrence of each weekday and can move a past time to the following week, so it cannot be used unchanged to claim results for seven specific calendar dates.

This revision supersedes the selection approach below. Keep the original industry examples as a research inventory, not a mandatory menu users must work through.

- Keep seven horizontally scrollable date cards, each initially containing four editable reference scenarios. Each scenario gives a scoped conclusion first and expands into outbound and return details.
- Default to a general set when industry is unknown. Offer an optional industry preset switch, or suggest a detected industry for confirmation. Do not require brand, role or industry selection.

| Preset | Scenario 1 | Scenario 2 | Scenario 3 | Scenario 4 |
| --- | --- | --- | --- | --- |
| General | Early 06:00–14:00 | Day 09:00–17:00 | Late 14:00–22:00 | Overnight 22:00–06:00 next day |
| Supermarket | Online picking 04:00–08:00 | Day 09:00–17:00 | Late 14:00–22:00 | Overnight replenishment 22:00–07:00 next day |
| Café / bakery | Opening 05:00–13:00 | Day 08:00–16:00 | Lunch support 11:00–15:00 | Closing 13:00–21:00 |
| Restaurant / pub | Breakfast / prep 06:00–14:00 | Lunch 11:00–16:00 | Evening / closing 17:30–23:30 | Overnight cleaning 23:00–07:00 next day |

These four selections deliberately sample different work scenarios; they do not cover every shift or establish an employer's available shifts. Allow replacement/customisation, preserve user edits across dates, and require an explicit action before a preset change replaces edited scenarios. A particular successful reference time does not establish that the whole morning/evening window is commutable.

Known actual job shifts take priority for route queries and appear as actual shifts; reference scenarios remain optional exploration. Never silently move an actual shift to fit availability. Availability is optional and means working hours, not hours away from home. Do not treat omitted availability as unrestricted availability. If preference flexibility is unknown, report a conflict as needing confirmation rather than a categorical rejection; explicit hard constraints can establish incompatibility. Overnight availability must cover both calendar days.

Report commute feasibility separately from availability compatibility. Label reference results as conditional on the displayed times; actual job suitability remains uncertain while required job times or days are unknown. If an advertisement lists multiple shifts, ask whether they are alternatives or mandatory rotation before treating one conflicting shift as disqualifying the whole job.

Start with the visible day's requests and prefetch subsequent days with bounded concurrency; show independent progress/retry states. Use actual dates, support next-day returns, and invalidate relevant results when locations, dates, times, limits or buffers change. No route returned is inconclusive, not proof that travel is impossible. No fare is used.

Keep arrival/departure buffers optional and zero by default. A user may explicitly compare a delayed departure (for example +30 minutes) to the scheduled end; do not infer a universal closing delay or silently overwrite the scheduled shift.

## Research inventory from the initial proposal

| Category | Scenario | Example |
| --- | --- | --- |
| Supermarket | Online picking | 04:00–08:00 |
| Supermarket | Opening / replenishment | 06:00–14:00 |
| Supermarket | Customer service | 09:00–17:00 |
| Supermarket | Late / closing | 14:00–22:00 |
| Supermarket | Overnight replenishment | 22:00–07:00 next day |
| Café / bakery | Opening | 05:00–13:00 |
| Café / bakery | Daytime | 08:00–16:00 |
| Café / bakery | Lunch support | 11:00–15:00 |
| Café / bakery | Closing | 13:00–21:00 |
| Restaurant / pub | Breakfast / preparation | 06:00–14:00 |
| Restaurant / pub | Lunch | 11:00–16:00 |
| Restaurant / pub | Evening / closing | 17:30–23:30 |
| Restaurant / pub | Overnight cleaning | 23:00–07:00 next day |

Selection: let the user choose an optional industry and one or more examples, or enter a custom shift. No brand or role is mandatory. If an actual shift is known, give it priority and retain examples for exploration. Do not infer the employer's schedule, workdays or availability from a template.

For each selected shift and date, query arrival by the start and departure after the end, advancing the return date when the shift crosses midnight. Separately report work availability compatibility and each commute direction. Unknown availability is unknown; missing routes are inconclusive. A template result does not establish whether the actual job is suitable.

Optional user-entered arrival and departure buffers default to zero; explain that a job's stated start may already include opening preparation and its end may include closing. Split shifts require an explicit choice of staying nearby or going home and are deferred from the first template release. Display actual dates and allow gradual loading to manage request volume.

Research leads provided by user: [Tesco](https://www.reddit.com/r/tesco/comments/1dp7bl0/night_shift_role/), [Sainsbury's](https://www.reddit.com/r/SainsburysWorkers/comments/1wc1nie/4am8am_shift/), [Lidl](https://www.reddit.com/r/lidl/comments/1wfjjfa/uk_shifts/), [Costa](https://www.reddit.com/r/Costa/comments/1x8yi2e/opening_shifts/), [Wetherspoons](https://www.reddit.com/r/Wetherspoons/comments/1rpctfo/wetherspoons_night_kitchen_staff/), [Nando's](https://www.reddit.com/r/nandos/comments/1ofcnxl). These are unverified leads, not evidence of industry-wide frequency.
