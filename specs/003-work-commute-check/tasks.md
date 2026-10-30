# Tasks: Work Commute Check

## Seven-day reference shift iteration (2026-10-29)

- [x] T101 Record requirements in specs/003-work-commute-check/spec.md and the design in specs/003-work-commute-check/plan.md.
- [x] T102 [US1] Complete explicit date and independent direction error handling in backend/src/main/java/uk/co/commutefit/workcommute/WorkCommuteService.java.
- [x] T103 [US1] Test explicit date, midnight and partial provider errors in backend/src/test/java/uk/co/commutefit/workcommute/WorkCommuteServiceTest.java.
- [x] T104 [US1] Add scenario request and result types in frontend/src/api/client.ts.
- [x] T105 [US1] Implement editable bilingual presets and seven day cards in frontend/src/ShiftScenarios.tsx and frontend/src/shiftScenarios.css.
- [x] T106 [US1] Implement bounded loading, retry and stale result handling in frontend/src/ShiftScenarios.tsx.
- [x] T107 [US2] Integrate confirmed job shifts and optional soft availability in backend/src/main/java/uk/co/commutefit/workcommute/WorkCommuteService.java and frontend/src/WorkCommutePage.tsx.
- [ ] T108 [US1] Test scenario presentation and state changes under frontend/tests/ (availability unit tests pass; interactive state tests outstanding).
- [ ] T109 Run backend and frontend tests, production build, and live acceptance when available; record results in specs/003-work-commute-check/CHANGELOG.md.

- [x] Inspect existing journey lookup, location suggestions, frontend structure and specifications.
- [x] Add reviewable URL/text/postcode extraction endpoint with bounded public URL fetching.
- [x] Add work commute endpoint reusing Journey Planner and excluding fare data.
- [x] Implement the three-valued verdict and weekly schedule compatibility.
- [x] Build applicant form, source selector, editable details, location suggestions and route results.
- [x] Keep the standalone journey lookup available at `/journey`.
- [x] Add tests for extraction, ambiguity, route timing, schedule fit, unknown routes and overnight shifts.
- [x] Run backend tests and fix regressions.
- [x] Run frontend tests and production build.
- [ ] Confirm local app startup and conduct a live TfL query.
- [x] Update root README and feature changelog.
