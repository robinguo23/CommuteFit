# Work Commute Check — Change Log

## 2026-10-29 — Live scenario acceptance

- After restarting the backend, checked DA18 4DE → E1 6AN with a 60-minute limit in the browser. The 29 October 06:00–14:00 example returned 80 minutes outbound and 50 minutes return; 22:00–06:00 next day returned 47 minutes outbound and 49 minutes return. Expanded route legs and transfer counts were visible.
- Confirmed successive dated cards are progressively populated with actual TfL results. Not every card and preset was manually checked.
- Localised scenario leg labels and removed redundant line names. The frontend build passed after this change.

## 2026-10-29 — Scenario recovery and contract checks

- Stop queued scenario requests on a missing endpoint, local connection failure or unresolved location; surface the error and support retrying a single shift. Display over-limit directions even if the other direction is unknown.
- Overnight availability checks both calendar days. Added HTTP contract coverage for the explicit date, next-day return and fare-free response.
- Frontend 19 tests and production build pass. Backend full suite passed 40 tests before the additional contract test; the updated work commute suite passes 14 tests. Backend package builds successfully.
- Browser acceptance using DA18 4DE to E1 6AN reached the running old backend, whose scenario endpoint is missing. The sandbox cannot connect to or restart that existing process, and computer-use access to the terminal is denied; live TfL acceptance therefore remains outstanding.

## 2026-10-29 — Scenario interface iteration

- Added editable bilingual four-scenario presets and seven horizontally scrolling dated cards, with bounded background requests and expandable route details.
- Added optional availability comparison; work-schedule conflicts now produce a pending overall verdict. Explicit-date overnight and partial-failure backend tests were added.
- Verification: backend 39 tests pass, frontend 19 tests pass and frontend build succeeds. Browser inspection confirms cards and controls render. The running backend has not been restarted and live TfL scenario queries have not been verified; UI state and interactive acceptance remain outstanding.

## 2026-10-29 — Explicit-date scenario groundwork

- Added an initial `/api/work-commute/scenario` endpoint that queries a specified date and moves an overnight return to the next day. It is not yet connected to the UI and does not yet isolate provider errors between directions.
- Backend tests could not start: Maven could not acquire its local repository lock in the sandbox. The endpoint remains unverified.
- Follow-up: copied the existing Maven cache to a writable temporary directory and ran the backend suite offline with Java 21: 37 existing tests passed. These tests do not specifically exercise the new endpoint.
- Updated the 003 requirements, plan and task list; scenario lookups now preserve the other direction when TfL is unavailable or rate limited. The existing backend suite again passed 37 tests; specific coverage and UI remain outstanding.

## 2026-10-29 — Availability default

- Start with availability days and times empty so the form does not invent a user's preferences. The four-scenario weekly cards and revised compatibility decisions are still outstanding; see shift-template-proposal.md.
- Frontend: 16 existing tests passed.

## 2026-10-28 — MVP implementation

- Added `/api/work-commute/extract` and `/api/work-commute/assess`.
- Reused TfL Journey Planner for outbound arrival-by and return leave-at queries.
- Added a weekly availability check, a one-way duration limit, route details and three outcome states.
- Excluded fares from work commute results and decision logic.
- Added a new bilingual default interface; existing standalone route lookup remains at `/journey`.
- Added backend tests for extraction and assessment behavior.
- Fixed reverse-query candidate-token mapping and routes with missing durations so they remain uncertain instead of being treated as confirmed over-limit.
- Kept the existing route page available at `/journey`; the new commute check is the default page.
- Static webpage extraction remains heuristic and may require manual review.

## Verification

- Backend: 37 tests passed during the MVP implementation.
- Frontend: 16 tests pass; `npm run build` succeeds.
- Live browser acceptance: DA18 4DE → E1 6AN, Monday 09:00–17:00 returned a suitable result, with 47-minute outbound and 49-minute return routes. No browser rendering errors remained.

## 2026-10-28 — Live acceptance fixes

- Read availability days and shift times from submitted form controls, then sync them back into component state so native time inputs are preserved after submit.
- Fixed Chrome's `Intl.DateTimeFormat` error by specifying the weekday and calendar date fields explicitly.
- Added a render error boundary so unexpected UI errors show a recoverable message instead of a blank page.
- Added frontend tests for submitted schedule values and weekday date labels.
