# Tasks

- [x] 更新当前规格指针和章程范围。
- [x] 实现不依赖档案的 POST /api/journeys 接口及回归测试。
- [x] 默认不依赖 PostgreSQL，保留旧数据及 legacy 后端。
- [x] 实现两个 postcode 表单、路线与全程价格、候选和错误状态。
- [x] 更新 README；前端构建和后端 22 项测试通过。
- [x] 浏览器核对新首页：两个必填 postcode、查询按钮、语言、说明与布局。
- [x] 增加 Google Maps 风格的 NOW / Leave / Arrive 时间控件；Leave/Arrive 使用日期、24 小时制时分和伦敦时区，含服务回归测试与浏览器检查。
- [x] 时间选项按中英文显示“现在/出发/到达”与“Now/Leave/Arrive”；切换模式或修改日期、时间后自动查询，加载期间保留旧结果，并精简重复说明。
- [x] 参考 Apple 地图网页的组合日期/时间选择器体验，加入带月份导航的日历弹层及内嵌时分控件；日期或时刻选择后立即自动查询。
- [x] 实际操作 Google 地图路线时间控件；保留原有三种时间模式并排选项，仅指定时间的时间/日期控件采用紧凑布局、微调按钮及浮层月历。
- [x] 保留 TfL 路线票价项的 `chargeLevel`，并在全程总价金额右侧按界面语言显示高峰/非高峰标签。
- [x] 完成路线结果细节展示：路线和分段时间隐藏日期并显示时长、合并重复的线路/交通方式名称、英文步行显示为 Walk，并在 TfL 返回 `path.stopPoints` 时显示 stops 数量。
- [x] 将 postcode 原生校验提示按界面语言显示，并在用户编辑时清除旧提示。
- [x] 通过当前 Chrome 中已运行的 MVP 完成新服务实时联调；确认路线结果由本地服务返回并显示 TfL 的 stops。
- [x] 按 SC-002 在新流程核对至少 5 次真实指定时间查询及返回路线的交通段、耗时、总价和高峰/非高峰标签；确认 TfL 返回的 stops 显示在 Elizabeth line 站点段后。
- [x] 将简版路线查询扩展为 postcode、站名、地址和地点名输入；复用 TfL Journey Planner 消歧候选 token。
- [x] 加入输入停顿后的 TfL StopPoint/Place 候选下拉框；选择候选后使用短期服务端 token 交给 Journey Planner；postcode 跳过联想请求。
