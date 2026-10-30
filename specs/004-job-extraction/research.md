# Research decisions

完整证据见 ../../.specify/assessments/job-url-extraction/research.md。

Decision: 以 Java 模块实现抓取/解析/结果契约，保留旧接口兼容入口。先用 JDK HTML 解析器与现有 Jackson，避免增加当前环境无法下载的 jsoup；HTML 处理封装，未来可替换。复杂 HTML5 的保真需另行测试。
Decision: 使用缓存中已有 Apache HttpClient 5 的 DNS resolver，验证并固定实际连接地址；不携带 cookies，不绕过证书验证，严格控制重定向和下载大小。
Decision: 不采用 PageSift 为必需依赖。它可改善页面获取，不能代替岗位语义解析；授权和目标站点效果需单独评估。
Decision: 无精确班次时返回时间类型和原文，沿用参考班次流程。只显示城市时要求用户补充，不猜门店。
Alternatives: jsoup + Playwright Java；PageSift 自托管；Python Trafilatura/extruct。官方资料及局限已记录在调研文档。
