# TfL fare API deep-test preflight
# TfL 票价 API 深度测试预检记录

**Observed:** 2026-10-26, direct GET requests opened in Chrome against `api.tfl.gov.uk`.
**Status:** These are the preliminary Chrome observations that preceded the archived batch. The full 30-request batch is now archived at [`runs/20261026T234104Z-b18d1676/`](runs/20261026T234104Z-b18d1676/); see the [bilingual results report](deep-results-20261026T234104Z-b18d1676.md) for its analysis.

**观察时间：** 2026-10-26，通过 Chrome 直接 GET `api.tfl.gov.uk`。
**状态：** 以下是归档批次之前的 Chrome 初步观察。完整 30 请求批次已归档于 [`runs/20261026T234104Z-b18d1676/`](runs/20261026T234104Z-b18d1676/)；详细分析见[中英双语结果报告](deep-results-20261026T234104Z-b18d1676.md)。

## Live preflight observations / 在线预检观察

| Case | Request | Observed response | Interpretation limit |
|---|---|---|---|
| Consecutive buses / 连续公交 | Journey Planner: `490G000803` → `490000038F`, 2026-10-27 09:00, `mode=bus` | Itinerary includes bus 15 then bus 135. A returned fare had `totalCost=175`; Fare entries were 175 and 0, with the second marked `isHopperFare=true`. Tap times were 09:09 and 09:50. Another returned itinerary had taps at 09:20 and 10:20, with the latter also returned as Hopper. | Confirms API output can expose fare components and Hopper flags across bus legs. It does not emulate card payment or independently prove the back-office charge. Inspect all raw journeys after the local run. |
| Saturday early bus / 周六凌晨公交 | Journey Planner: same pair, 2026-10-31 02:30, departing, `mode=bus` | First option walked to Charing Cross, then used N550 from 02:51 to 03:27; fare total 175. Another option used N21, N15 and N277, with fare components 175, 0 Hopper, 0 Hopper. | A route returned for a 02:30 departure can start later; this query is depart-at, not arrive-by. |
| Sunday early bus / 周日凌晨公交 | Journey Planner: same pair, 2026-11-01 02:30, departing, `mode=bus` | One option used N21, N15 and N277, arriving 03:23, with fare components 175, 0 Hopper, 0 Hopper. A separate option used N550. | Confirms scheduled bus options were returned for this stop pair and time. It says nothing about an arbitrary home postcode or job location. |
| Arrive by 02:30 / 凌晨 02:30 前到达 | Same pair, Sat 2026-10-31 and Sun 2026-11-01, 02:30, `timeIs=Arriving` | Saturday options included N550 01:21–02:02 and another itinerary arriving 02:27. Sunday options included N550 01:21–02:03 and a multi-bus option arriving 02:25. | These are planner results for one central stop pair, not a guarantee of arrival. Arrival buffer is expressible through `timeIs=Arriving`. |
| Clock-change overlap / 夏令时回拨重叠时间 | Same pair, 2026-11-01 01:30, departing, `mode=bus` | The API returned a first option starting 01:35; fare tap timestamps carried `+00:00`. Journey start/leg display times had no explicit offset. | The query does not let us choose between the two local 01:30 occurrences. Need inspect local batch response and define an app policy for ambiguous DST times. |
| Hammersmith OSI / Hammersmith 站外换乘 | Journey Planner: Goldhawk Road → Earl's Court, `via=1000097`, 2026-10-27 09:00, `mode=tube` | HTTP 200; returned Tube, walking interchange from Hammersmith H&C to Hammersmith District/Piccadilly, then Tube; `fare.totalCost=230` with one fare object. | Confirms route/fare shape for this example. It does not model whether a card was validated during the OSI. |
| Zone 1 avoidance / 避开 Zone 1 | Journey Planner: Finsbury Park → Stratford via `940GZZLUHAI`, 2026-10-27 09:00 | HTTP 200; first route used Victoria line to Highbury & Islington then Mildmay line to Stratford Rail Station; `totalCost=230`, fare taps included Finsbury Park, Highbury & Islington and Stratford. Another returned route used Tube through Zone 1 and cost 360. | `via` can return the intended alternative route. It cannot prove a pink-reader tap. Compare its actual route/fare with FareTo rows and official validation conditions. |
| National Rail coverage / National Rail 覆盖 | Journey Planner: St Pancras International Rail Station `910GSTPX` → Liverpool Street Rail Station `910GLIVST`; FareTo queried for same pair | Journey Planner returned National Rail + Elizabeth line route options without a `fare` object. FareTo returned an empty array `[]`. | Missing fare/empty rows means unknown. The official example API request document labels FareTo Tube-only. |
| FareTo bus probe / FareTo 公交站点探测 | FareTo: bus stop `490G000803` → `490000038F` | Returned `[]`. | Do not display this as £0; use Journey Planner's bus fare/Hopper fields where present. |

## Execution blocker / 执行阻塞

Earlier, the shell in this agent environment failed before HTTP with DNS error `nodename nor servname provided` when resolving `api.tfl.gov.uk`. The user then ran the prepared batch locally and archived all 30 responses successfully. The earlier failure was environment-specific and was not evidence that TfL rejected the requests.

此前，当前 agent 环境中的 shell 在 HTTP 响应前发生 DNS 错误 `nodename nor servname provided`。之后用户在本机成功运行批次，并完整归档了 30 个响应。此前错误属于执行环境差异，并不表示 TfL 拒绝请求。
