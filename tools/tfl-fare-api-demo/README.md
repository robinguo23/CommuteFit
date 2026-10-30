# TfL Fare API Demo
# TfL 票价 API 演示

This standalone demo probes TfL Journey Planner and the StopPoint `FareTo` endpoint for Charing Cross → Baker Street. It saves the exact response body and HTTP headers for every completed request, records request parameters and UTC timestamps, and prints fare-related JSON paths without assuming the response shape or currency unit.

此独立演示会测试 Charing Cross → Baker Street 的 TfL Journey Planner 和 StopPoint `FareTo` 接口。每次收到响应后，它会保存原始响应正文和 HTTP headers，记录请求参数及 UTC 时间，并打印票价相关 JSON 路径；不会预设 JSON 结构或金额单位。

## Run / 运行

Requires Python 3.10+ and network access to `api.tfl.gov.uk`. No third-party packages are used.

需要 Python 3.10+，并且运行环境可以访问 `api.tfl.gov.uk`。不需要第三方 Python 包。

```sh
python3 tools/tfl-fare-api-demo/probe.py
```

Optional API credentials are read from `TFL_APP_ID` and `TFL_APP_KEY`, and are never written into the manifest or printed. Passing `--no-journey` or `--no-fareto` skips an endpoint. `--date` uses TfL's live Journey Planner format `YYYYMMDD` (default: today); `--time` accepts `HH:mm` or `HHmm` and is sent in TfL's required `HHmm` format. `--time-is` configures the query. TLS certificate verification is always enabled. On python.org macOS Python, the script automatically uses the installed `certifi` bundle if available. If the trust error persists, provide a PEM bundle with `--ca-bundle /path/to/ca.pem` or `SSL_CERT_FILE=/path/to/ca.pem`. Do not disable verification. The chosen CA bundle path is recorded in the manifest; credentials are not.

可选凭证从 `TFL_APP_ID` 和 `TFL_APP_KEY` 读取，不会写入记录文件或打印。`--no-journey` 或 `--no-fareto` 可跳过接口。`--date` 使用 TfL 实际 Journey Planner 接口格式 `YYYYMMDD`（默认今天）；`--time` 可输入 `HH:mm` 或 `HHmm`，脚本会按接口要求转换为 `HHmm` 发送。`--time-is` 用于设置查询。TLS 证书校验始终开启。对于 python.org 安装的 macOS Python，脚本会在可用时自动使用已安装的 `certifi` 证书包。如果仍有证书错误，可通过 `--ca-bundle /path/to/ca.pem` 或 `SSL_CERT_FILE=/path/to/ca.pem` 指定 PEM 证书包。不要关闭证书校验。记录文件会包含所用 CA bundle 路径，但不会包含 API 凭证。

Each run creates a timestamped directory under `runs/`. A network/DNS failure is recorded as an error with no fabricated HTTP status or response body. FareTo output is exploratory: it does not assume `data[0].cost`, pence units, or equivalence with a Journey Planner fare. The manifest explicitly classifies an empty FareTo array as `no-usable-fare-empty-array`; the console says no usable fare was returned, not £0.

每次运行会在 `runs/` 下创建带时间戳的目录。网络/DNS 失败会记录为错误，不会伪造 HTTP 状态或响应正文。FareTo 输出仅供检查：不假设存在 `data[0].cost`、金额单位为便士，或其结果与 Journey Planner 票价等价。manifest 会将 FareTo 空数组明确标记为 `no-usable-fare-empty-array`；终端提示“未返回可用票价”，而不是 £0。

## Scenario batch / 批量场景

Copy `scenarios.template.json` to `scenarios.json`, then run:

```sh
python3 tools/tfl-fare-api-demo/probe.py --scenarios-file scenarios.json
```

The template proposes candidate Tube transfer, bus+Tube, consecutive bus/Hopper, National Rail transfer, Hammersmith OSI, separately queried journeys via Oxford Circus, a Zone 1-avoiding alternative, and weekday peak-boundary cases. These are candidates, not guaranteed itineraries: confirm from the returned Journey Planner JSON that each intended mode/transfer actually occurred, and change an endpoint pair if needed. `via` requests guide route search but do not simulate taps or prove the charged fare. The separate-journey case compares independent journey quotes; it does not emulate exiting/re-entering with one card. The API cannot model card identity, same-card continuity, actual interchange validation or whether a passenger touched a pink reader. Verify those conditions against official TfL policy. Add `fareTo` pairs only for explicit station-to-station reference queries. These are whole-pair lookups and must not be labelled as per-leg prices. Every request records its scenario and purpose in `manifest.json`.

将 `scenarios.template.json` 复制为 `scenarios.json` 后运行。模板提供候选地铁换乘、公交+地铁、连续公交/Hopper、National Rail 换乘、Hammersmith OSI、经 Oxford Circus 分开查询的旅程、绕开 Zone 1 的路线候选，以及工作日高峰边界场景。这些只是候选路线，不保证实际规划结果符合预期。`via` 只引导路线搜索，不能模拟实际刷卡，也不能证明最终扣款。分开查询只比较不同旅程的报价，不模拟同一张卡的真实出站/重进。接口不能模拟卡片身份、同卡连续性、实际换乘验证或乘客是否刷了粉色读卡器；这些条件须对照 TfL 官方政策核实。检查原始 JSON 确认预期交通方式/换乘；不符合时更换起终点。`fareTo` 只用于明确的站到站参考查询，不能当作逐 leg 票价。每个请求会在 `manifest.json` 中记录场景和用途。

### Deep API batch / 深度 API 批次

Run the prepared 30-request batch from this project root:

```sh
python3 tools/tfl-fare-api-demo/probe.py --scenarios-file tools/tfl-fare-api-demo/scenarios-deep.json
```

It covers matching Journey Planner/FareTo Tube quotes, multi-leg Tube and OSI routes, bus+Tube, a resolved multi-bus/Hopper route, Zone 1 avoidance via Highbury & Islington, National Rail fare coverage, weekday fare boundaries, Saturday/Sunday arrive-by and depart-at checks around 02:30, late returns, after-midnight date handling and the 2026 UK clock-change overlap. The scenario dates are 27 October, 31 October and 1 November 2026; update them before reusing the batch for another period.

The command writes one raw `.body` and `.headers` file per HTTP response plus `manifest.json` under `runs/<run-id>/`. Share the printed run path (or leave the directory in place) so the response bodies can be inspected against the manifest. HTTP failures and pre-HTTP network errors remain distinct; an empty result or missing fare is recorded as unknown, never as £0.

从项目根目录运行已准备好的 30 请求批次：

```sh
python3 tools/tfl-fare-api-demo/probe.py --scenarios-file tools/tfl-fare-api-demo/scenarios-deep.json
```

覆盖 Journey Planner 与 FareTo 的地铁票价对照、多 leg 地铁和 OSI 路线、公交+地铁、已解析站点的多段公交/Hopper、经 Highbury & Islington 避开 Zone 1、National Rail 票价覆盖、工作日票价边界、周六/周日凌晨 02:30 的到达/出发查询、晚班返程、跨午夜日期处理，以及 2026 年英国夏令时回拨时重复的本地时间。场景日期为 2026 年 10 月 27 日、10 月 31 日和 11 月 1 日；以后复用时请更新日期。

命令会在 `runs/<run-id>/` 下为每个收到 HTTP 响应的请求保存原始 `.body` 和 `.headers`，并保存 `manifest.json`。请保留并提供终端打印的 run 路径，以便将原始响应与 manifest 对照分析。HTTP 错误和 HTTP 响应前的网络错误分别记录；空结果或缺失票价保持为“未知”，不能按 £0 处理。

**Latest archived analysis / 最近一次归档分析**: The 30-request deep batch completed with HTTP 200 for every request. See the [bilingual findings report](deep-results-20261026T234104Z-b18d1676.md) and its archived [manifest](runs/20261026T234104Z-b18d1676/manifest.json). The report separates direct API observations, TfL policy facts and unresolved fare edge cases.

30 请求深度批次已完成，所有请求均返回 HTTP 200。请查看[中英双语发现报告](deep-results-20261026T234104Z-b18d1676.md)及其归档 [manifest](runs/20261026T234104Z-b18d1676/manifest.json)。报告区分 API 实测现象、TfL 政策事实及尚未解决的票价边界情况。

## Evidence boundaries / 证据边界

The demo distinguishes raw observations from schema inspection. It does not sum vehicle legs into fares or claim that matching endpoint values prove the fare policy is correct. To test transfer cases, provide stop IDs explicitly using `--from-stop` and `--to-stop`; each pair remains a distinct endpoint query and must be compared with a matching Journey Planner query and fare basis.

演示会区分真实响应与 schema 检查结果。它不会把车辆 leg 相加当成票价，也不会声称端点金额一致就证明票价政策正确。测试换乘场景时，可用 `--from-stop` 和 `--to-stop` 显式提供站点 ID；每一对仍是独立接口查询，必须与条件一致的 Journey Planner 查询及票价基础进行比较。
