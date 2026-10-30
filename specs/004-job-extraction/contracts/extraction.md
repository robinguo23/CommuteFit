# HTTP contract

POST /api/job-extractions，JSON {sourceType, content}。响应含 status/title/jobStatus/source/locations/schedules/warnings/missing 及兼容字段 workplaceLocation/workDays/startTime/endTime/notes。
GET/POST 其他业务仍走原接口。POST /api/work-commute/extract 委托同一模块，响应扩展字段向后兼容。非法来源/超长输入为 400；合法 URL 获取失败为 200 + status=unavailable + warnings；部分字段缺失为 partial。所有值待确认。嵌入 HTML 证据仅纯文本显示。

Schedule.cycleWeek: nullable integer（1 或 2）。前端多选实际班次时调用现有 /api/work-commute/scenario，按每个明确日期和时段分别查询往返。存在实际班次时不提供 availability-window 给上层作为候选班次。未知星期覆盖七天的查询由前端展开，不篡改证据 days 字段。description.store 仅为地图检索提示词；不提供虚构 postcode 或地点 token。
