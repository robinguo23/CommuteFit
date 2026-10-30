# Implementation plan

新增 POST /api/journeys，以 originPostcode、destinationPostcode 为仅有的必填字段；支持临时候选令牌，以及可选的 timeMode、travelDate、travelTime。默认 NOW，指定时间用伦敦当地日期与时分，并映射为 TfL `timeIs=Departing/Arriving`。
复用 TfL 客户端与候选存储，独立映射路线和 `fare.totalCost`、`fare.fares[].chargeLevel`，以及 leg 的 `path.stopPoints[]` 数量；不依赖档案或优惠引擎。
默认关闭旧档案/班次组件与数据库自动配置；legacy profile 可保留旧功能，绝不清除数据库。
前端以出发地/目的地输入框与路线卡替换原主页面；输入支持 postcode、站名、地址和地点名，歧义时展示 TfL 候选名称与类型供用户确认；首版仅在提交后查询，不做逐字自动补全。保留语言选择、提供方说明及错误处理。NOW/Leave/Arrive 的指定时间控件支持 15 分钟选择、日期浮层和更改后自动查询；路线时间只显示时分及耗时，线路标签去重，Walk 与 stops 按交通方式及 TfL 返回值显示。
验证：固定时钟+HTTP stub 服务测试，含有效路线、缺价、空数组、畸形响应和候选；前端构建；再按 SC-002 对至少 5 个真实 TfL 路线响应进行端到端核对。
