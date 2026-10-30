# Postcode Journey Changelog

## 2026-10-28

- Journey Planner requests now include all nine TfL Go modes (Bus, Tube, National Rail, DLR, London Overground, Elizabeth line, River Bus, Tram, Cable Car), plus walking; River Bus and Cable Car results have localized labels.
- Pure walking routes show a whole-route fare of £0.00 and omit the TfL quote label and unavailable-fare note; transit and mixed-mode routes keep the existing fare behavior.
- Added frontend unit coverage for walking-only, mixed, transit, and empty-leg routes, including GBP zero formatting.

## TfL location matching — 2026-10-27

- Journey inputs accept a UK postcode, station, address, or place name, up to 120 characters.
- Added a 500ms debounced `/api/locations` proxy for TfL StopPoint Search; when it returns fewer than five candidates, TfL Place Search supplements the dropdown. Results are limited to eight, and postcode-like text skips autocomplete.
- Added one automatic retry for transient Journey Planner provider/network/timeout failures after reproducing an intermittent failure that succeeded on manual retry with the same selected station tokens. Rate-limit responses are not retried.
- Kept selected location tokens reusable for the 10-minute token lifetime after route lookup. This prevents the unchanged endpoint from reverting to ambiguous free text when the user edits and reselects the other endpoint.
- Scheduled Now/Leave/Arrive searches now retain the selected location tokens. Swapping origin and destination also moves each selected token with its location.
- Added automated frontend cases for time-mode/date/time changes and swapping journeys in both directions, plus backend coverage for all supported TfL travel modes and walking transfer counts.
- Selecting a suggestion fills its canonical name and keeps its short-lived server token. When both endpoints are populated, selection immediately triggers route planning; the backend sends the selected NaPTAN ID or TfL Place coordinates to Journey Planner.
- Journey Planner disambiguation remains as fallback for free-text addresses and terms outside TfL's Place coverage. No third-party geocoder was added.
- See `location-search-research.md` for the provider comparison, limits, and interaction rationale.

## 2026-10-27

- Postcode fields normalize case and add the UK postcode separator automatically while typing (for example, `da184de` becomes `DA18 4DE`).
- Journey requests time out after 25 seconds, including while reading the response body, so the loading state can recover with a retryable message.
- Time mode labels are localized: Chinese uses “现在 / 出发 / 到达”; English uses “Now / Leave / Arrive”.
- Existing journey results remain visible while the user changes postcodes, time mode, date, or time. Switching time mode or changing the date/time automatically starts a new lookup; the new response replaces the old result.
- Removed the repeated explanatory sentence from the time selector; the selector keeps only its heading, mode choices, and relevant date/time fields.
- Used Apple Maps on the computer and opened its departure-time picker. Its web UI combines departure/arrival selection, month navigation, a calendar, and a time field in one popover. The MVP now presents date and time together in a localized calendar popover; selecting either automatically starts a lookup, following the product's immediate-query behavior.
- Used Google Maps directions on the computer and selected its departure-time mode. The route card uses a compact inline row with time first, then a date control that includes the weekday, without explanatory labels. The MVP date/time controls now follow this layout while retaining the clickable calendar and automatic lookup.
- Tightened the date button to the width of its date/weekday text and changed the calendar to an overlay that sits above page content instead of expanding the layout.
- The calendar overlay closes when the user clicks outside the time/date controls or presses Escape.
- Reworked the specified-time controls to follow the Google Maps route panel: one compact time/date row, adjacent time and day navigation controls, and an anchored month calendar. The original three-option time-mode selector remains.
- Kept the original mode selector aligned above the date/time row and made the date controls wrap cleanly on narrow screens.
- Added a 15-minute time dropdown and moved the earlier/later arrows inside its frame; increased spacing around the time-mode controls.
- Moved the previous/next day arrows inside the date frame; opening the time list now keeps the page scroll position and scrolls only within the list.
- Removed the date icon, tightened spacing between time and its arrows, increased separation between the journey-time heading and mode choices, and reduced the calendar popover size.
- Localized native postcode validation messages to the selected interface language and clear stale custom messages as the user edits a field.
- Removed the year from the date field label and close the calendar immediately after a date is selected.
- Preserve each route fare item's TfL `chargeLevel` and show its peak/off-peak label beside the whole-route total, localized for the current interface language.
- Place the peak/off-peak label inline immediately to the right of the whole-route fare amount.
- Increase the inline peak/off-peak label size for easier reading.
- Remove calendar dates from route and leg time displays; show each route/leg time range with its duration, avoid repeating a route name when it duplicates the mode label, use “Walk” in English, and show TfL-provided stop counts after transit endpoints when available.
- Center the selected time in its control and bring the 15-minute adjustment arrows closer together at the right edge.
- Narrow the time control and center the time plus adjustment arrows together to reduce unused space.
- Align the specification and implementation plan with the current date-control design, optional Leave/Arrive API fields, and existing route time/name/stops presentation; keep live TfL acceptance tracked separately.
- Live-check Abbey Wood → Seven Kings Rail Station: three options returned a TfL whole-route quote of £2.50 Peak Adult PAYG; both Elizabeth line legs had no individual fare values. Recorded the observation in `acceptance.md`.
- 2026-10-28: Remove the FareTo fallback and its station-ID resolution/logging from production. FareTo is not listed in TfL's public Unified API Swagger; fares come only from Journey Planner `journey.fare.totalCost`, and missing totals remain unavailable.
