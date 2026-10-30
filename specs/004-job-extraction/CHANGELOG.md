# 004 Job extraction — 2026-10-29

## 用户验收修正：多选、中文、分周与查询范围

- 按用户规则：实际班次优先，存在实际班次时不显示/查询雇主窗口；没有实际班次才使用窗口边界参考。
- 班次多选默认全选；未知星期或七天选五天含周末等描述覆盖七日查询，不把查询范围当作实际工作日承诺。
- 支持中文星期、第一周/第二周及门店名称+城市。用户提供的 M&S Woolwich 相关正文保存为回归样例，六条安排保留各自周次、星期和时间。双周实际日期需用户明确第一周周一基准后再查。
- 查询矩阵测试：两个未限定日期的班次生成14个班次日期组合、28个方向；M&S 样例在指定周一基准后只生成6个对应日期组合、12个方向。
- 后端完整 verify：61项通过并已打包。前端26项通过，生产构建通过。此轮后端解析变更需重启加载，未声称新行为已在旧运行服务中实测。
- 位置解析候选尚未等于精确地点：后续 005 已接入 Photon 地点搜索、地址/地图确认与令牌传参。用户重启后已通过一条需要手动细化门店名称的双周班次页面闭环；具体结果与限制见 ../005-location-selection/CHANGELOG.md 和 quickstart.md。

## Live acceptance after user restart

- In the running browser, pasted a complete weekday shift: E1 6AN, Monday–Friday 09:00–17:00; location, five weekdays and both times filled correctly with evidence.
- Replaced it with two workplace postcodes and employer availability windows. Both locations were selectable, prior days/times cleared, window times were not prefilled, and 15:30–24:00 was shown as next-day 00:00. Selecting a location populated its field.
- Fetched the real public Tesco URL record=1013709 through the application. It returned the actual title, expiry warning, two rotating shifts and seven employer windows. No complete workplace postcode was extracted, so the workplace remains manual. This is one expired sample, not a site-wide accuracy claim.
- Live sample exposed a weekday inference bug: “5 days from 7 (including weekends)” was being reduced to weekends. Added conservative server parsing and immediate client selection guard; backend extraction tests now 17 passed, frontend 23 passed and both builds/packages succeed. The server correction is packaged and loads on the next backend restart; the client guard is already hot-loaded.
- Re-fetched the same live URL after the client update and selected the 14:00–22:00 shift. The fields filled correctly while all workday checkboxes remained unselected, verifying the client guard against the running backend response. Left the browser on this real extraction result.

## Delivered

- Created specification, research decisions, plan, data model, API contract, requirements checklist, task breakdown and quickstart. Requirements checklist reviewed before implementation: 6/6 satisfied. Extension hooks configuration contains no registered hooks.
- Added an independent jobextraction module with PublicPageFetcher, JobDocumentParser, JobExtractionService and JobExtractionController. Existing work-commute extraction delegates to it.
- Added field evidence, postcode normalization, JSON-LD arrays/@graph, multiple-job ambiguity, separate employer windows, per-day tables, 12/24-hour shifts, overnight/24:00 handling and expired/remote warnings.
- UI shows candidate workplace and shift evidence, allows explicit selection, keeps manual correction, and clears old job fields before a new extraction. Added stale source-response suppression.
- Bounded static fetching verifies DNS addresses used by the connector, rejects private destinations, limits redirects and response size, and does not carry browser login state. Source URLs returned to clients omit query credentials.
- No paid APIs, model calls, browser crawler or persistent job storage added. Deferred browser rendering and ATS adapters remain outside this first-release spec.

## Verification

- Java 21, offline writable Maven cache: ./mvnw -o -Dmaven.repo.local=/tmp/commute-fit-m2 verify — 57 tests passed, including 16 new extraction tests; backend artifact packaged.
- Frontend npm test — 22 tests passed, including 3 extraction field tests.
- Frontend npm run build — passed.
- New HTTP extraction contract verified with MockMvc and injected page fixtures. Fixtures are authored boundary examples, not a measured corpus of live websites.
- Real external URL retrieval and latest backend UI flow were not live-tested: this environment cannot resolve external hosts via shell or control the existing backend process. No real-site accuracy percentage is claimed. Restart local services using quickstart.md before independent UI acceptance.

## Limitations

- Uses JDK HTML parser rather than jsoup because dependencies cannot be downloaded here. Tested basic tables and snippets; complex/malformed HTML5 structure may require an adapter.
- Static public pages only; JavaScript rendering, login, CAPTCHA and general ATS integrations are not implemented.
- Rule parsing cannot recover unpublished rota, infer missing meridiem or resolve ambiguous workdays. Single exact suggestions remain subject to user review.
- Standalone tests verify parsing and fetch target policy; hostile remote redirect/DNS integration and a real-site coverage corpus remain future validation work.
