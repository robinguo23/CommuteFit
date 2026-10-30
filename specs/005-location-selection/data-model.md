# Model

Search query: trimmed 3–120 字符。
Candidate: id（短期 token）、name（填框的准确显示文本）、address、postcode?、lat?、lon?、precision=venue|address|street|locality|postcode|coordinate|station、source、sourceId?、selectable。
Response: status=matches|no-matches|unavailable、candidates[]、message、attribution。
第三方 candidate 的 routeValue 只在服务端存储为 lat,lon；TfL 站点保留站点 ID，postcode 保留规范化邮编。ID 输入只验证格式，未知站名不伪造。
UI: edited → searching → preview → confirmed；编辑后立即回到 edited。精度不足候选不可确认；postcode 允许用户确认但明确为邮区。令牌最多十分钟，不保存历史。
