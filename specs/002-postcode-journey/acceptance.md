# Validation — 2026-10-27

## Fare source decision — 2026-10-28

- CURRENT POLICY: production uses only the route-level fare returned by TfL Journey Planner (`journeys[].fare.totalCost`) and its returned charge level. FareTo is absent from the public Unified API Swagger, so the application no longer calls it or resolves station IDs for fare lookup. Missing/invalid Journey Planner totals remain unavailable; leg fares are never invented or summed.
- The FareTo experiments recorded below are historical investigation only. They do not describe current production behavior.

## Journey Planner `routeBetweenEntrances` observation — 2026-10-28

- USER-PROVIDED live response for `940GZZLULVT` (Liverpool Street Underground) → `940GZZLUMGT` (Moorgate Underground), `mode=tube`, departing 2026-10-28 09:00. `journeyVector.uri` and time-adjustment URIs confirm `routebetweenentrances=true` was used.
- TfL returned Tube options on the Metropolitan and Circle lines with separate walking legs at both ends: about 98 m / 2 min from the Liverpool Street station point to its Underground stop point, and 37 m / 3 min from the Moorgate Underground stop point to the Moorgate station point. The walks include turn-by-turn steps and station obstacle data (stairs / walkway).
- This confirms the parameter can cause the response to include access/transfer walking details around the selected Underground stops. The payload does not explicitly label these points as ticket gates or say whether either walk crosses the paid-area boundary; it cannot by itself establish whether passengers need to exit/re-enter or how fare validation works.

## Same-station interchange example — 2026-10-28

- LIVE TfL Journey Planner response for Westminster Underground (`940GZZLUWSM`) → Maida Vale Underground (`940GZZLUMVL`), `mode=tube`, `routeBetweenEntrances=true`, departing 2026-10-28 09:00.
- The first returned journey changes at Baker Street (`940GZZLUBST`): Jubilee line Westminster → Baker Street (arrives 09:15), then Bakerloo line Baker Street → Maida Vale (departs 09:19). The Jubilee leg reports an obstacle `{type: WALKWAY, stopId: 1000011, position: AFTER}`; the 4-minute gap is the interchange allowance. This is TfL's concrete same-station line-change example.
- Caveat: this response represents the interchange with the walkway obstacle and schedule gap, not a separate named walking leg/path. It supports that TfL models a walkway connection at Baker Street; it does not expose an explicit paid-area/gate-crossing field, so it alone cannot prove the route stays within the paid zone.
- Request: `https://api.tfl.gov.uk/Journey/JourneyResults/940GZZLUWSM/to/940GZZLUMVL?date=20261028&time=0900&timeIs=Departing&mode=tube&routeBetweenEntrances=true`
- Repeated live in the user's TfL Swagger UI on 2026-10-28 with the same origin/destination, date, time, mode, and `routeBetweenEntrances=true`; Swagger showed HTTP 200 and the same Baker Street interchange sequence/`WALKWAY` marker.
- Fare interpretation: the returned £3.60 Peak single adult PAYG quote has only the route endpoint taps (Westminster and Maida Vale), with no Baker Street tap. This is consistent with the planner pricing the itinerary as one continuous rail journey, but the quote is not evidence of the physical paid-area boundary or actual passenger taps. A separate Baker Street Stop Structure fetch was attempted in this environment but DNS/network access failed; do not claim its footpath topology or gate status was verified here.

- PASS: frontend npm run build (TypeScript + Vite).
- PASS: backend Maven verify, 22 tests, no failures/errors. Used offline mode with `-Daether.syncContext.named.factory=noop` because the sandbox cannot create lock files in ~/.m2.
- PASS: default application context creates the new journey service without a DataSource bean or datasource override.
- PASS: new endpoint accepts only two postcodes and returns two-leg route with total GBP 4.85; invalid inputs return bilingual field errors before provider calls.
- PASS: service normalizes postcodes; uses fixed London-local departure date/time; keeps missing/invalid total unknown; preserves provider zero; distinguishes empty vs malformed; returns location candidates.
- PASS: browser on existing Vite port 5174 shows the simplified page, two postcode fields, swap and search controls, disclosure and TfL attribution; visual layout checked.
- BLOCKED: starting new backend on 8080 fails with SocketException Operation not permitted; starting Vite on 5173 fails with EPERM. Existing backend 8081 was not restarted and does not prove new endpoint availability. Live TfL end-to-end result rendering and 5 representative actual queries remain unverified.

Current specification checklist passes. No extension hooks registered before/after specify (`hooks: {}`).

## Live TfL parameter regression — 2026-10-27

Root cause reproduced through the browser: the earlier `mode=public-bus,...,train,...` request returned HTTP 400 `ApiArgumentException`: modes public-bus and train were not recognised. The Swagger parameter examples use those older names; the live `/Journey/Meta/Modes` response instead reports bus and national-rail.

Corrected the client and URI regression expectations to `bus,overground,national-rail,elizabeth-line,tube,dlr,tram,walking`.
Live upstream verification: SW1A 1AA → W1D 3QF, 2026-10-27 15:30, departing, calcOneDirection=true. Corrected request returned 4 journeys: 30 min GBP 3.00, 31 min GBP 3.00, 29 min bus GBP 1.75, and a 35 min walking option without fare. These are provider observations, not guarantees of final payment.
The running backend must be restarted to load the corrected JAR. The launcher now refuses to silently reuse a backend already running on 8080, and selects the installed Java 21 instead of accepting an inherited Java 8 JAVA_HOME.

## Route stop-count follow-up — 2026-10-27

- PASS (source mapping): `JourneyLookupService` reads each TfL leg's `path.stopPoints` array and returns its item count as `legs[].stops`; the route card appends that value after the segment endpoints and omits it for walking or when TfL provides no list.
- PASS (archived provider evidence): manually inspected 18 saved raw TfL Journey Planner response files under `tools/tfl-fare-api-demo/runs/`; 99 legs contained a non-empty `path.stopPoints` array. These examples confirm the provider field and its varying presence, but are not the requested live-page acceptance.
- PASS (backend packaging): `JAVA_HOME=<installed JDK 21> ./mvnw -o -Daether.syncContext.named.factory=noop --batch-mode --no-transfer-progress -DskipTests package` completed successfully; tests were skipped for this packaging check.
- BLOCKED at that earlier check (live-page acceptance): shell requests to localhost and starting another local listener were unavailable. The follow-up below uses the already-running app in Chrome and supersedes this status.

## Live route verification — 2026-10-27

- PASS: used the already-running MVP in Chrome to submit five distinct Leave-time searches for the same weekday itinerary. Each returned three route options; the page showed scheduled segment times and durations, complete route fares, TfL charge levels, and the Elizabeth line segment's stop count.
- PASS: for the displayed `Abbey Wood → Tottenham Court Road` Elizabeth line leg, the app shows `(7 站)` immediately after the endpoints. The same count appeared consistently across all three options in the live responses. Example segment presentation: `16:45 — 17:01 (16 分钟)` for the preceding walk, followed by `Abbey Wood → Tottenham Court Road (7 站)` and its own time/duration.
- PASS: provider-returned fare labels appeared as both Peak and Off Peak across the sampled times; labels were rendered in Chinese and remained alongside the whole-route total.
- Privacy: this acceptance note records no submitted postcode pair or journey search history. The browser was left on the user's requested example result; its on-screen inputs are not written to project files.
- SC-002 live UI check is complete for this sample set. The code-level archived-response inspection remains supporting evidence only; the confirmation above came from the active application and live query flow.

## TfL autocomplete implementation — 2026-10-27

- PASS (build): frontend TypeScript/Vite production build completed; backend Java 21 compile completed with tests skipped.
- IMPLEMENTED: `/api/locations` combines StopPoint Search results with Place Search fallback; the frontend displays a 500ms debounced dropdown, skips postcode-like input, binds selected locations to short-lived server tokens, and triggers Journey Planner after a selection when both endpoints are populated.
- PASS (live TfL contract): `StopPoint/Search/King's Cross` returned `HUBKGX` with `icsId=1000129`; `StopPoint/Search/Abbey Wood` returned `HUBABW` with `icsId=1001001`. A direct Journey Planner request using those ICS IDs returned valid journeys, including a 31-minute option.
- FIXED: the selected suggestion token now maps a HUB result to its `icsId`; ordinary StopPoint results continue using their NaPTAN `id`. The previous code passed `HUBKGX`/`HUBABW` directly, which Journey Planner could not resolve.
- PASS (live UI): after the user restarted the local service, both station candidate lists loaded in the running app and the paired selection submitted a Journey Planner request. The follow-up below records one intermittent failure and successful retry.

## Intermittent selected-location query — 2026-10-27

- PASS (live UI reproduction): after the user restarted the service, the browser returned TfL suggestions for Abbey Wood and King's Cross. Selecting both started a Journey Planner request that showed the transient `provider_unavailable` message; pressing Search again with the same selected locations returned three routes.
- PASS (upstream comparison): a direct TfL Journey Planner request for ICS IDs `1000129` → `1001001` at 17:52 London time returned valid journeys. This confirms the selected location IDs work; the failed app request was intermittent and the exact upstream response for that failed attempt was not captured.
- FIXED (resilience): the frontend retries `provider_unavailable`, `network_error`, and `request_timeout` once after 350ms using the same selected candidate tokens. It does not retry `rate_limited` responses.
- UI observation: prior route results remain visible after a failed new search by design, so the error is shown alongside the previous result until a later request succeeds.
- PASS (verification after retry change): `npm run build` completed successfully; live browser selection of both TfL candidates then auto-loaded three routes. The retry branch itself was not forced during this final successful run.
- PASS (immediate repeat request): on 2026-10-27, reselected `Abbey Wood` and `King's Cross & St Pancras International` in the running app and triggered a fresh Now query. The app returned three routes; the fastest shown departed 18:00 and arrived 18:28 (28 minutes, one interchange). TfL did not provide a whole-route fare for these results, so the page correctly displayed “票价暂不可用”.

## Re-selecting a location after a successful query — 2026-10-27

- FAIL (live UI reproduction): a successful Abbey Wood → King's Cross query was followed by changing the destination to Liverpool Street and selecting its TfL suggestion; the page reported that TfL could not recognize the location. Changing the destination back to King's Cross and selecting the same TfL suggestion also failed.
- ROOT CAUSE: `JourneyLookupService` removed selected candidate tokens after a successful journey, and `App.tsx` cleared both token IDs from state. The input still displayed the selected candidate names, so the next query sent the unchanged endpoint as free text. This made later queries depend on TfL's inconsistent free-text disambiguation.
- FIXED: journey lookup no longer consumes selected-location tokens on success, and the frontend retains them. Editing an endpoint still invalidates only that endpoint's token; stored tokens expire after 10 minutes. Assessment candidate tokens retain their existing one-use cleanup.
- PASS (live recheck after restart): with the rebuilt backend running, Abbey Wood → King's Cross returned three routes; changing the destination to a TfL-selected Liverpool Street returned routes; changing it back to a TfL-selected King's Cross returned routes again. The unchanged Abbey Wood selection remained valid through both subsequent lookups.
- OPERATIONAL CHECK — 2026-10-27 18:02 UTC: the page still showed `TfL 无法识别该地点` with an older route beneath it. `127.0.0.1:8080` did not respond; the new packaged JAR was built, but starting it in this execution environment failed with `SocketException: Operation not permitted` while binding 8080. Therefore this page error is currently also caused by the local backend being unavailable, and the patched end-to-end flow cannot be verified from this environment.
- RESOLVED — 2026-10-27: after the user restarted the backend, the live edit-away/edit-back check above passed. The failed 18:02 launch was an environment limitation at that moment, not a remaining issue in the restarted service.

## Time-change query drops selected locations — 2026-10-27

- ROOT CAUSE: `scheduleTimeSearch` passed `{}` as the selected-candidate argument to `search`, so switching to Leave/Arrive or changing date/time discarded both TfL selection tokens. The displayed location names then fell back to TfL free-text parsing and could be unresolved.
- FIXED: scheduled searches now preserve the current origin and destination candidate tokens.
- PASS (unit tests): `npm test` verifies token preservation when switching Now → Leave, changing the date, and changing the time (3 passed, 0 failed).
- PASS (build): `npm run build` completed successfully.
- This regression is covered by the automated unit tests; no repeated manual page steps are required to verify the token arguments.

## Expanded journey switching coverage — 2026-10-27

- PASS (frontend unit tests): 9 tests cover Now/Leave/Arrive and repeated date/time changes while retaining both candidate tokens; swapping A → B moves each token with its location, swapping twice restores the original state, and a partially selected journey preserves its one selected token.
- PASS (backend unit tests): `JourneyLookupServiceTest` confirms returned legs preserve bus, Overground, National Rail, Elizabeth line, Tube, DLR, Tram, and walking modes, while walking is not counted as a transit change. `TflClientTest` verifies the Journey Planner request includes all supported transit modes.
- PASS: frontend `npm test` (9 passed), frontend `npm run build`, and focused backend tests `JourneyLookupServiceTest,TflClientTest` (11 passed).

## Mixed Tube / National Rail fare investigation — 2026-10-27

- CONFIRMED (archived TfL response): the saved `rail-transfer-journey` sample queried London Bridge National Rail Station → stop `1000138` with National Rail, Tube, and Elizabeth line modes. A returned itinerary contained a National Rail Thameslink leg (St Pancras International → Farringdon) and an Elizabeth line leg (Farringdon → Liverpool Street); the itinerary's `journeys[].fare` was null, and neither leg object contained a fare field. Therefore the missing price is not attributable to one leg: TfL omitted the fare for the whole itinerary and did not provide leg fares.
- CONFIRMED (implementation): `JourneyLookupService` maps only `journeys[].fare.totalCost` and `journeys[].fare.fares[]`; the frontend displays the route-level fare. It does not discard a leg-level price because TfL Journey Planner does not return one in this schema/sample.
- API research: TfL's `StopPoint/{from}/FareTo/{to}` is documented as Tube-only. National Rail RTJP supports station-to-station/multi-leg planning and fares under a formal licence with cost-recovery charges; its previous request route is closed while migration to Rail Data Marketplace is expected in November 2026. BR Fares Easy Fares is another paid option for rail fares between two locations, but it is not a source of the actual fare for each TfL mixed-journey leg and does not check live quota-controlled ticket availability.
- Product rule: do not infer a mixed-journey total by adding separate Tube and rail prices. Keep the current unavailable-fare state unless the provider supplies a compatible whole-itinerary price. References and details are in `docs/tfl-unified-api-reference.md`.

## Abbey Wood → Seven Kings live fare check — 2026-10-27

- PASS (live app): selected TfL suggestions for `Abbey Wood` and `Seven Kings Rail Station`; the lookup returned three options. Each option showed the same TfL whole-route quote: **£2.50, Peak, Adult PAYG**.
- The displayed itinerary legs were Elizabeth line, Abbey Wood → Whitechapel (4 stops), then Elizabeth line, Whitechapel → Seven Kings Rail Station (6 stops). Neither displayed leg has an individual fare; the available £2.50 is the route total, so it must not be assigned as the price of either segment.

## Historical FareTo fallback experiment for Abbey Wood → King's Cross Underground — 2026-10-27 (not current behavior)

- Resolve concrete station IDs from Journey Planner endpoint IDs, TfL StopPoint Search results, or a selected Hub's `children`; prioritize children whose mode matches the route's first/last transit leg.
- Match the FareTo response by endpoint ATCO codes, default route, adult passenger, PAYG ticket, and the departure-time band. Never use the alternative route price for the default Journey Planner route.
- Empty arrays, request errors, unsupported ticket periods, or non-matching response rows leave the fare unavailable; none represent £0.
- Do not hand-maintain station IDs, add FareTo values to Journey Planner fare items, or infer per-leg costs.

## Historical mixed bus route FareTo investigation — 2026-10-27 (not current behavior)

- USER-OBSERVED in the local app: option 3 is Walk (Abbey Wood Station → Harrow Manorway / Abbey Wood Station), N1 (to Tottenham Court Road), then N73 (to King's Cross & St Pancras International). Its fare remains unavailable while the Elizabeth line + Tube options have fares.
- At the time, the fallback used the first and last transit endpoints, so this route resolved the bus stop at Harrow Manorway / Abbey Wood and the final King's Cross endpoint, then requested FareTo for that pair. TfL documents FareTo as Tube-only, so an empty or unsupported result is plausible; this does not prove a wrong ID.
- VERIFIED (live backend log, PID 83771): at 2026-10-27 23:37:32.757Z the fallback selected origin mode `bus`, candidate `[490G000957]`, and destination mode `bus`, candidate `[940GZZLUKSX]`. The next log entry records `GET /StopPoint/490G000957/FareTo/940GZZLUKSX` → HTTP 200, raw response `[]`. The same route was queried again at 23:38:14.190Z and returned HTTP 200 with `[]` again.
- The displayed first non-walking leg was N1 from Harrow Manorway / Abbey Wood Station; the last was N73 to King’s Cross St. Pancras Underground Station. The candidate IDs above are what the backend passed to FareTo. The new log does not separately record the raw Journey Planner `point.id`, so it cannot distinguish whether `490G000957` came directly from that field or from StopPoint Search.
- Because the response body is an empty array, there are no returned ATCO codes, fare rows, or Adult PAYG tickets to filter. The fare remains unavailable due to TfL returning no fare record for this request, not because an ATCO/Default Route/ticket filter rejected a non-empty response.

## Bus route fares returned after selecting Abbey Wood Station — 2026-10-28

- USER-OBSERVED in the running app: the origin input is now `Abbey Wood Station` (previously `Abbey Wood`). The Walk + N1 + N73 route displays **£1.75**; two other bus-only options display **£1.75** and **£3.50**.
- The 2026-10-28 00:00 backend log shows FareTo calls only for Elizabeth line → Tube options. It shows no FareTo calls for the bus-only options. In `JourneyLookupService`, FareTo runs only when Journey Planner's `fare.totalCost` is absent; therefore the displayed bus totals came through Journey Planner's route-level fare field, not FareTo and not summed leg prices.
- The raw Journey Planner fare response is not logged, so the amount's exact provider payload was not captured. The changed origin selection (`Abbey Wood Station` versus `Abbey Wood`) and new query time coincide with the change; the logs do not establish which change caused TfL to include fares.
- This is a live observation for the app's current query time and TfL quote, not a per-leg fare breakdown or guaranteed final charge. No local files or code were changed by the query itself.

## Standalone Tube leg fare versus mixed-route leg — 2026-10-27

- USER-PROVIDED live observation: the mixed Abbey Wood → King's Cross & St Pancras option was 18:19–18:47 and had no complete route fare. Its Tube leg was Barbican Underground Station → King's Cross St. Pancras Underground Station, scheduled 18:42–18:47 on Hammersmith & City.
- The user then queried Barbican → King's Cross & St Pancras as a separate Now journey at 18:24. TfL returned a 4-minute, 2-stop Metropolitan-line option and a **whole-route standalone quote of £3.10 Peak Adult PAYG**.
- This confirms Journey Planner can return a fare for the same Tube station pair queried by itself. It does not establish that £3.10 is the fare attributable to that Tube leg in the earlier multi-leg itinerary: query time, selected route, actual tap sequence, and any linked-journey/Oyster/Contactless rules may affect the charged amount. Do not silently add this standalone quote to the first Elizabeth line segment as a guaranteed total.
- USER-PROVIDED follow-up live observation: Abbey Wood → Barbican Underground Station returned an Elizabeth line + Walk option (18:27–18:55) with a whole-route TfL quote of **£3.60 Off Peak Adult PAYG**. This prices that entire Abbey Wood → Barbican query, not the Elizabeth line segment in isolation.
- Validation boundary: TfL station/status information refers to a step-free connection between the westbound Barbican platform and the Elizabeth line at Farringdon. Therefore the planner's `Walk` leg does not prove that the passenger taps out/in at Barbican. Adding the separate Abbey Wood → Barbican and Barbican → King's Cross quotes could price a different tap sequence; £6.70 is not established as the actual fare for the original journey. Keep the complete-fare-unavailable state unless a full-route quote is returned or the UI labels such a sum as an explicitly unverified estimate.

## TfL Single Fare Finder — Abbey Wood → King's Cross — 2026-10-27

- PASS (live official TfL fare finder): selected `Abbey Wood` (Elizabeth line) → `King's Cross St. Pancras Underground Station`, Adult. The official single-fare result lists Oyster/Contactless PAYG **Peak £4.80** (Monday–Friday morning peak) and **Off Peak £3.60** (other times, including public holidays). Cash single is £7.00 at any time.
- Fare-rule clarification: TfL Staff Guide says peak is generally 06:30–09:29 and 16:00–18:59 weekdays, with fare type determined by the start touch-in. Exception: when touching in outside Zone 1 during the evening window and touching out in Zone 1, the journey is charged Off Peak. Thus the user's 18:36 Abbey Wood (Zone 4) → King's Cross (Zone 1) example is **£3.60 Off Peak**, not £4.80 Peak; arrival after 19:00 does not change that classification.
- The same result lists a separate alternative fare when changing between National Rail and London Underground at named stations (Peak £7.30 / Off Peak £5.90). This is route-dependent and should not be confused with the main fare for the Abbey Wood Elizabeth line → King's Cross Tube itinerary.
- This provides an authoritative whole-pair PAYG fare reference for the reported 18:19–18:47 journey: at that evening time the listed main Adult PAYG fare is **£3.60 Off Peak**. TfL's fare finder states that route, direction, time and weekday can affect fares; check the relevant route and passenger/ticket type before generalizing.

## FareTo hub ID versus specific stop IDs — 2026-10-27

- USER-PROVIDED live observations: `/StopPoint/Search/Abbey Wood` returned hub `HUBABW`; `/StopPoint/HUBABW` listed children `910GABWD` (Abbey Wood (London) Rail Station) and `910GABWDXR` (Abbey Wood, Elizabeth line). King’s Cross / St Pancras IDs confirmed from TfL station data: `940GZZLUKSX` (Tube), `910GSTPX` (National Rail).
- Production resolves concrete IDs dynamically from Journey Planner endpoint IDs or StopPoint Search/Hub details, prioritizing each endpoint's actual transit mode. The observed IDs are examples, not a maintained mapping table.
- FareTo requests returned HTTP 200 but different bodies: `HUBABW → HUBKGX` gave `[]`; `910GABWD → HUBKGX` also gave `[]`; `910GABWD → 940GZZLUKSX` returned Adult PAYG Peak £4.80 / Off Peak £3.60 plus an alternative route Peak £7.30 / Off Peak £5.90; `910GABWD → 910GSTPX` returned a non-empty fare response (amount not recorded here).
- Interpretation: HTTP 200 + `[]` means no usable fare was returned, not £0 and not proof that no fare exists. Hub IDs may need expansion to actual stop IDs; use Journey Planner legs and point/mode IDs to choose route-relevant candidates. A FareTo station-pair result is not a leg fare; do not sum pair quotes or claim it matches a route without matching the actual itinerary.
- Historical implementation (removed 2026-10-28): the service resolved first/last transit stop IDs, expanded Hub children, and queried FareTo when Journey Planner omitted `fare.totalCost`. The application no longer contains this flow because FareTo is not listed in the public Unified API Swagger.
