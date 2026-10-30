# Data model

ExtractionRequest: sourceType=url|description|work-postcode，content 最多 20000 字符。
Source: method=static-http|text|manual，url（不回显凭证参数）、fetchedAt；不落库。
Result: status=partial|ready|unavailable，title 可空，jobStatus=expired|unknown，locations[]、schedules[]、warnings[]、missing[]。
Location: value（完整邮编或地址）、evidence、sourcePath，需用户确认。
Schedule: kind、days[]（枚举星期），start/end 可空，endsNextDay，evidence、sourcePath、cycleWeek（1、2 或 null）。days 只记录原文明确星期；空数组意味着查询范围展开七天，不声称一周上班七天。00:00 同时开始结束不默认解释成 24 小时班次。
Location 可为 postcode、完整地址或门店名称+城市。description.store 标识尚待地图确认的门店候选，不等于已解析坐标。
查询选择：前端保留多个已选 Schedule，优先实际班次，次选窗口。cycleWeek 存在时，需用户指定第一周周一日期，生成十四天中对应星期/周次的查询；无 cycleWeek 时生成连续七天中的对应日。
兼容投影: workplaceLocation/startTime/endTime 仅在单一无歧义候选时提供；workDays 与该实际班次绑定。notes 用于旧客户端。
