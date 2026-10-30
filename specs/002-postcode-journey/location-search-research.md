# 模糊地点输入与候选选择调研

- **日期**：2026-10-27
- **范围**：伦敦通勤路线查询的出发地和目的地输入
- **结论状态**：建议先复用 TfL，不引入新的地理编码供应商

## 当前实现

- 简版 `JourneyLookupRequest` 接受最多 120 字符的出发地/目的地文本，并保留 postcode 的大写与分隔空格规范化。
- 输入停顿 500ms 后，页面请求本地 `/api/locations`；后端先调用 StopPoint Search，候选少于 5 项时用 Place Search 补充，最多返回 8 项。postcode-like 输入跳过联想。
- 候选含名称、地点类型及 10 分钟有效的 token。StopPoint 一般映射到 NaPTAN `id`；TfL 返回 `HUB...` 枢纽时映射到同项的 `icsId`（HUB key 本身不能用于路线查询）；Place token 映射到 Place 坐标。路线查询成功后仍保留 token，使后续只编辑另一端的查询继续使用原选地点；用户编辑该端输入时，该端 token 作废。
- 普通地址可能不在 Place 目录中。自由文本最终仍交给 Journey Planner，并保留其 `fromLocationDisambiguation` / `toLocationDisambiguation` 处理。

## 供应商比较

| 选项 | 适配度 | 费用/限制（官方资料，查询日期 2026-10-27） |
|---|---|---|
| **TfL Unified API / Journey Planner + Place** | Journey Planner 接受坐标、postcode、NaPTAN/ICS stop ID 和自由文本，并能返回路线所需的地点消歧。`Place/Search` 是另外一个按名称搜索 TfL Place 目录的 endpoint，可给站点/登记地点候选；它不是通用地址地理编码。 | TfL API 门户称匿名调用限额为每分钟 50 次；订阅可申请每分钟 500 次。适用 TfL 数据条款与署名要求。 [TfL API 门户](https://api-portal.tfl.gov.uk/products) [TfL Swagger Place/Journey](https://api.tfl.gov.uk/swagger/docs/v1) |
| **Mapbox Geocoding** | 可找地址和地点；如果以后需要 TfL 以外的地址覆盖或地点自动补全，可评估。它返回坐标，仍需要把坐标转成 TfL 可规划的地点或 stop point。 | 官方价格页当前列出 Temporary Geocoding 每月前 100,000 次免费，超出按量收费；temporary 结果不能缓存。启用 autocomplete 时每次键入都会计一次请求。 [价格](https://www.mapbox.com/pricing) [API 文档](https://docs.mapbox.com/api/search/geocoding/) |
| **Google Maps Geocoding / Places** | 搜索体验成熟，但会引入计费配置、API key 管理及 Google Maps Platform 使用条款；仍需将匹配结果接到 TfL 路线规划。 | 当前官方价目列 Geocoding 每月 10,000 次免费额度；启用 API 仍要求绑定 billing。Places autocomplete 的请求和 session 按 SKU 计费。 [价格表](https://developers.google.com/maps/billing-and-pricing/pricing) [计费说明](https://developers.google.com/maps/documentation/places/web-service/usage-and-billing) |
| **OSM 公共 Nominatim** | 不建议作为正式产品的自动补全后端。可探索，但覆盖、容量和政策不适合作为无条件依赖。 | 官方政策限制最多每秒 1 次，要求识别应用、署名、可随时切换服务；明确禁止用公共服务实现 autocomplete，并提醒服务容量有限。 [使用政策](https://operations.osmfoundation.org/policies/nominatim/) |

## 建议的交互

1. 输入框接受 postcode、站名、街道/地址或地点名称。名称输入停顿后搜索站点及 TfL Place；postcode 直接走路线查询。
2. 候选项显示 TfL 返回的地点名与地点类型，用户选择后才调用路线查询；两端均有值时选择即触发查询。候选通过短期服务端 token 绑定到显示名称与上游目标。
3. Place 结果需要坐标才能用作 Journey Planner 路线点；StopPoint 使用 NaPTAN 或 ICS stop ID，不能直接使用 `HUB...` 搜索键。候选最终都由 Journey Planner 验证。
4. 如果输入过于宽泛或没有 TfL 候选，允许用户继续提交自由文本，让 Journey Planner 做后备消歧；仍无法识别时提示输入更具体地点。

## 决策建议

当前版本使用 StopPoint Search 的站点匹配，并在匹配不足时用 Place Search 补充候选；Journey Planner 仍负责最终路线规划与自由文本消歧。只有真实使用显示 TfL 对普通地址覆盖不足时，再比较 Mapbox 与 Google；第三方 geocoder 结果也必须让用户确认并通过 TfL Journey Planner 验证。

## 依据与限制

- TfL 的 API 门户介绍其 Places 能力可用于按名称、postcode 等查找和匹配地点；本项目已读取 Journey Planner 消歧响应，实际仍应通过样本查询确认不同模糊输入的覆盖质量。
- Swagger 区分了 `/StopPoint/Search`（按 common name/公交 stop code 搜站点）、`/Place/Search`（按 name 搜 TfL Place 目录）与 Journey Planner 自由文本解析/消歧。实现已用前两者做联想候选，后者作为地址和空候选的后备。 [TfL Swagger](https://api.tfl.gov.uk/swagger/docs/v1)
- 2026-10-27 实测：`King's Cross` 首选结果为 `id=HUBKGX, icsId=1000129`，`Abbey Wood` 首选结果为 `id=HUBABW, icsId=1001001`。直接以两个 ICS ID 调用 Journey Planner 成功返回路线（31 分钟方案含 Northern line、步行及 Elizabeth line）。因此候选服务需将 HUB id 转成其 ICS ID；这是已复现的“选了 TfL 地点仍无法识别”原因。
- 免费额度、限额与服务政策可能调整；以上额度是 2026-10-27 查阅官方页面时的资料，不构成未来账单承诺。
- 公共 Nominatim 的政策适用于 `nominatim.openstreetmap.org`，不自动适用于自托管实例或其他服务商。
