# Tasks

- [x] T001 完成 specs/005-location-selection/ 规格、计划、契约、研究与验收步骤。
- [x] T002 [US1] 添加 backend/src/main/java/uk/co/commutefit/location/PhotonClient.java 和限频缓存配置。
- [x] T003 [US1] 添加 LocationSearchService.java 与 PlaceSearchController.java，统一候选及令牌。
- [x] T004 [US1] 添加 frontend/src/PlacePicker.tsx 并集成 WorkCommutePage.tsx。
- [x] T005 [US2] 修复 TflJourneyService.java 的 parameterValue 消歧与显示名绑定。
- [x] T006 [US2] 在 backend/src/test/java/uk/co/commutefit/ 下回归五类端点、GeoJSON、令牌与消歧重查。
- [x] T007 [US3] 添加 scripts/check_tfl_location_types.py，记录真实观察与环境限制。
- [x] T008 [US3] 完成自动化验证、README.md 和 specs/005-location-selection/CHANGELOG.md；明确记录本地页面闭环尚未实测。
- [x] T009 [US3] 修正 start-local.sh，运行独立 JAR 副本，避免开发期构建覆盖正在运行的文件；语法检查通过，实际启动记录端口绑定受限。
- [x] T010 [US1/US3] 用户重启后，通过真实页面验证中文双周六班次、地点确认及十二个方向；验证编辑清除选择和班次保留选择，修正旧空状态和自定义班次文案，记录完整名称需手动细化的实际限制。

Dependencies: T001 → T002 → T003 → T004；T005/T006 可独立于提供方接入验证；最后 T007/T008。默认不批量调用 demo 服务。

T001–T009 是重启前的实现与验证记录；T010 为用户重启后的实际页面验收。本次使用手动细化名称的公开样例，不等于所有门店自动匹配或招聘 URL 均通过。复现步骤见 quickstart.md。

## US4 多源区域共识

- [x] T011 写明区域共识规格、计划、接口及验收。
- [x] T012 实现跨来源完全图、保守文本匹配、去重、medoid 和距离边界测试。
- [x] T013 实现三源并发和隔离失败的 consensus API。
- [x] T014 Demo 默认三源评估，展示共同区域、来源状态、筛选原因与原始候选。
- [x] T015 定向测试、前端构建、打包及更新验收记录。

## US5 多品牌实测

- [x] T016 收集 50 个不同门店、7 个品牌的招聘来源与独立地址参考。
- [x] T017 添加四规则基准页，执行 200 条观察并导出完整响应。
- [x] T018 使用独立参考做 0.6 英里区域核验、输出 CSV 与可离线复算脚本。
- [x] T019 记录错误／漏筛／方法限制，展示中文结果摘要并验证构建。

## US6 安全的分店歧义处理

- [x] T020 根据 50 门店基准复核长街多分店误选，增加远处同品牌候选阻止自动选点的规则；不会要求用户挑区域。
- [x] T021 修正 postcode 严格相等、英式撇号及常见道路缩写规范化，并添加定向回归用例。
- [x] T022 更新地点搜索 Demo：有歧义时明确说明最强共识候选未被采用，避免误以为后续路线会使用该坐标。
- [x] T023 仅在地点搜索 Demo 增加无共识文字回退：品牌和街道/区域文本匹配且匹配坐标集中时展示低置信度候选；远处同街道多店仍标歧义。
- [x] T024 为唯一文字候选与远处分店冲突增加后端回归用例；更新契约及用户可见 Demo 文案。
