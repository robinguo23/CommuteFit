"""Fetch Moorgate Stop Structure data using the local TfL app key."""

from pathlib import Path
import ssl
import sys
import xml.etree.ElementTree as ET
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

import certifi


PROJECT_ROOT = Path(__file__).resolve().parent.parent
ENV_FILE = PROJECT_ROOT / ".env"
OUTPUT_DIR = PROJECT_ROOT / "docs"
ENDPOINT = "https://api.tfl.gov.uk/jp_public/api10/XML_STOPSTRUCTURE_REQUEST"
USER_AGENT = "CommuteFit/1.0"
DEFAULT_STOP_ID = "940GZZLUMGT"


def read_app_key() -> str:
    if not ENV_FILE.is_file():
        raise SystemExit(f"找不到项目配置文件：{ENV_FILE}")

    for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
        if line.startswith("TFL_APP_KEY="):
            return line.split("=", 1)[1].strip().strip("\"'")

    raise SystemExit("没找到 TFL_APP_KEY，请检查项目根目录的 .env")


def main() -> None:
    args = [arg for arg in sys.argv[1:] if arg not in ("--minimal", "--all-footpaths")]
    minimal = "--minimal" in sys.argv[1:]
    all_footpaths = "--all-footpaths" in sys.argv[1:]
    stop_id = args[0] if args else DEFAULT_STOP_ID
    if len(args) > 1 or (minimal and all_footpaths):
        raise SystemExit(
            "用法：python3 scripts/fetch_tfl_stop_structure.py [ATCO_CODE_OR_ICS_ID] [--minimal | --all-footpaths]"
        )
    params = {
        "sSStopNr": stop_id,
        "coordOutputFormat": "WGS84[DD.DDDDD]",
        "app_key": read_app_key(),
    }
    if not minimal:
        params.update({
            "sSESM": "0",
            "sSFEA": "1",
            "sSInclSP": "1",
            "sSInclSL": "1",
            "sSOnlyDF": "0",
            "onlyDefaultFootpaths": "0",
        })
    if all_footpaths:
        params = {
            "sSStopNr": stop_id,
            "sSOnlyDF": "0",
            "coordOutputFormat": "WGS84[DD.DDDDD]",
            "app_key": read_app_key(),
        }
    url = f"{ENDPOINT}?{urlencode(params)}"
    ssl_context = ssl.create_default_context(cafile=certifi.where())

    try:
        request = Request(
            url,
            headers={
                "User-Agent": USER_AGENT,
                "Accept": "application/xml, text/xml;q=0.9, */*;q=0.8",
            },
        )
        with urlopen(request, timeout=30, context=ssl_context) as response:
            data = response.read()
            status = response.status
    except HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace").strip()
        message = f"TfL 返回 HTTP {error.code}：{error.reason}"
        if detail:
            message += f"\nTfL 错误详情：{detail}"
        cf_ray = error.headers.get("cf-ray")
        if cf_ray:
            message += f"\ncf-ray：{cf_ray}"
        if error.code == 403 and "1010" in detail:
            message += (
                f"\n本次请求使用 User-Agent: {USER_AGENT}。"
                "判断：上游访问防护仍拒绝了请求；这不等同于 subscription key 无效。"
                "请记录请求时间和 cf-ray（若有），通过 TfL API Portal 支持渠道反馈。"
            )
        raise SystemExit(message) from error
    except URLError as error:
        raise SystemExit(f"无法连接 TfL：{error.reason}") from error

    OUTPUT_FILE = OUTPUT_DIR / f"stop-structure-{stop_id.lower()}.xml"
    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    ERROR_FILE = OUTPUT_DIR / f"stop-structure-{stop_id.lower()}-error.xml"
    try:
        root = ET.fromstring(data)
    except ET.ParseError as error:
        ERROR_FILE.write_bytes(data)
        raise SystemExit(
            f"HTTP {status}，响应不是有效 XML；原始响应已保存到 "
            f"{ERROR_FILE.relative_to(PROJECT_ROOT)}：{error}"
        ) from error

    api_errors = [
        (node.get("code", ""), " ".join("".join(node.itertext()).split()))
        for node in root.iter("itdMessage")
        if node.get("type") == "error"
    ]
    if api_errors:
        ERROR_FILE.write_bytes(data)
        summary = "; ".join(
            f"code {code}: {message}" if code else message
            for code, message in api_errors
        )
        raise SystemExit(
            f"HTTP {status}，但 TfL XML 内含应用错误（{summary}）；"
            f"错误响应已单独保存到 {ERROR_FILE.relative_to(PROJECT_ROOT)}，不会覆盖成功数据"
        )

    OUTPUT_FILE.write_bytes(data)
    print(f"HTTP {status}，已保存 {OUTPUT_FILE.relative_to(PROJECT_ROOT)}（{len(data)} bytes）")


if __name__ == "__main__":
    main()
