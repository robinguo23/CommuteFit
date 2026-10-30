# Implementation Plan: Work Commute Check

## 2026-10-29 seven-day scenarios iteration

Extend the existing feature in place. Use `/api/work-commute/scenario` with an explicit London calendar date and shift, reusing the existing stop resolution and fare-free route projection. Return outbound and return independently when the provider fails; retain disambiguation errors for user correction. Handle end-before-start on the following calendar day. Do not move an explicit date to a different week. The browser requests the visible card first with bounded concurrency and records loading, error, empty and complete results separately. Prevent stale responses after form or scenario edits. Presets and UI copy are bilingual and editable; identify all presets as illustrative. Availability is optional and soft unless the user explicitly indicates a hard restriction. Preserve known job schedules as actual rather than replacing them with examples. Add meaningful backend and frontend tests for date, midnight, state changes and partial errors. Build and run existing suites and attempt live acceptance where environment permits.

## Approach

Build the applicant-facing check as a new default React page and keep the postcode journey lookup available at `/journey`. Reuse `/api/locations` candidate search and `JourneyLookupService` for TfL resolution and route planning. Add a separate work-commute API projection that deliberately excludes fare properties.

## Backend

1. `POST /api/work-commute/extract` reads pasted text or a bounded public URL and returns reviewable heuristic suggestions.
2. `POST /api/work-commute/assess` validates the time limit and inputs, checks weekly schedule fit, then queries each listed job weekday.
3. Outbound uses `arrive` at shift start; return uses `leave` at shift end, with an overnight end date advanced one day.
4. Route projection exposes route times, duration, transfers and legs. No fare lookup or fare projection is permitted.
5. Preserve semantic uncertainty: absent routes/durations or absent schedule data yields `needs-review`, not `unsuitable`.

## Frontend

1. Default page collects start location, one-way time limit, user availability, and one of the three job input sources.
2. Extracted and manually supplied workplace/days/times remain editable; location suggestions and Planner disambiguation are selectable.
3. Render the three decision states with bilingual labels and per-day direction cards.
4. Keep the previous route lookup under `/journey`.

## Verification

- Frontend unit suite and production TypeScript/Vite build.
- Backend tests for route time semantics, direction token swap, threshold and schedule outcomes, no-route uncertainty, overnight return date and extraction ambiguity.
- Full local TfL live acceptance is separate and may require a running backend with outbound network access.
