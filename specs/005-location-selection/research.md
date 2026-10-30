# 精确位置选择：调研与接口草案

2026-10-29，用户提出独立位置模块。下方保留接入前调研和技术决策；随后已实现 Photon 客户端和地点确认模块，未部署自托管地理库。实现和验证结果见 CHANGELOG.md。

## 已有能力

LocationSuggestionService 目前整合 TfL StopPoint/Search 与 Place/Search，候选通过 CandidateTokenStore 保存 providerId/坐标字符串并绑定显示文本；令牌十分钟有效。这个机制可以复用，但返回名称+类型缺少地址、精度和地图确认。门店搜索不能用邻近车站替代目的地。

## 开源选择与证据

- Photon：https://github.com/komoot/photon ，Apache-2.0，支持多语言、搜索即输入、错字容忍、位置偏向、边界框，返回 OSM 数据。适合评估门店/地址下拉框。公开 demo 仅允许合理规模且不保证可用性；自托管需要数据和 OpenSearch/Java 资源，不是无限免费的托管 API。全球库资源较大，评估英国/伦敦数据范围后再部署。官方说明不能证明本案例的 M&S 门店一定存在。
- Pelias：https://github.com/pelias/pelias ，MIT，地点/地址/场馆自动补全，多数据源可扩展；相较当前项目部署和数据维护更复杂（工程判断），适合未来需要多源数据时评估。
- Nominatim：https://operations.osmfoundation.org/policies/nominatim/ ，该政策只针对 OSMF 官方公共服务器：全应用最多每秒一次，必须署名和标识客户端，明确禁止自动补全。不将该公共接口接入逐字搜索；自托管或其他供应商条件另行核实。

接入前只核对官方资料。接入后的单条真实观察：Chrome 直接访问 Photon，搜索 M&S Woolwich 返回两条 Woolwich 铁路点和 M&S Foodhall（4 Victory Parade、SE18 6FL）。原始公开数据已保存为 locations/photon-woolwich.json 回归样例；不据此推断该点一定是招聘门店或员工入口，也不声称测得整体匹配准确率。来源及五类 TfL 实际响应摘要见 docs/location-browser-observations-2026-10-29.json。

## 建议产品流程

岗位提取给出原文门店+城市 → 位置模块检索 → 展示多个具体候选（门店、完整地址、postcode 若有、地图点）→ 用户明确选中 → 服务端保存确认的坐标/来源 ID → 通勤查询使用该坐标。编辑显示文本立即使原选择失效。

只匹配到城市、街道、邮区中心时显示其精度，不当作已确认门店。支持无匹配时补充地址、完整 postcode 或手动输入坐标后预览地图；不包含地图点选/拖拽。坐标落在建筑中心也不保证等于员工入口。中文别名需数据支持；不得通过模型猜翻译后假定位置正确。M&S Woolwich Simply Food 和同区域其他 M&S 分店必须分别展示，不静默选首项。

## 底层契约（已由 005 spec 明确）

保留 /api/locations 兼容；新模块返回 id（已生成候选 token）、name、address、postcode?、lat/lon、precision、source、sourceId、selectable；没有 bbox 字段。令牌沿用原短期机制，绑定最终显示名；确认时选用已有令牌，不另建持久记录。Journey Planner 使用 lat,lon，不能把 GeoJSON lon,lat 反着传。城市/街道候选不可确认；无匹配/提供方故障明确区分，不能把 unknown 当作通勤不符合。

建议先用公开门店信息组成少量样本，对比 Photon 与现有 TfL 候选；衡量门店是否正确、地址/坐标精度、同品牌分店混淆及中文检索覆盖。这个比较完成后再决定生产提供方，不能因开源就承诺数据覆盖。

## 用户进一步明确的 TfL 端点关联

最终用户确认的候选必须既是界面地点，也是 TfL 路线端点。第三方地理编码的 sourceId 不等于 TfL StopPoint ID。第三方门店/地址候选应在服务端验证坐标后，以纬度,经度传入 Journey Planner；TfL 车站候选使用其支持的站点 ID。GeoJSON 返回经度,纬度，需显式转换顺序。

2026-10-29 直接核对 TfL 官方 OpenAPI：https://api.tfl.gov.uk/swagger/docs/v1 ，GET /Journey/JourneyResults/{from}/to/{to}。from、to 均为必填字符串，各自可用 WGS84 的 lat,long、UK postcode、NaPTAN StopPoint ID、ICS StopId 或自由文本；via 是可选中途点，也支持同样五种表示。自由文本需要消歧，除非恰好匹配兴趣点名称。fromName/toName/viaName 是附加结果名称，不能代替真正的坐标/ID 定位。此结论是文档核对，不是对所有输入格式逐一做线上请求实测。

复用现有令牌结构：前端只发送显示名和短期 selection token，后端根据 token 查出经过验证的 routeValue。令牌绑定确切候选和显示文本；编辑输入后失效。去程使用 home → workplace，回程使用 workplace → home，并交换各自的选择令牌，不能再次只按门店名称自由搜索。候选坐标精度不足时仍需确认，不把附近公交站或地铁站自动替换成门店入口。API 返回的第三方 ID、地址与坐标必须来自同一个候选。
