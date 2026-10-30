#!/usr/bin/env bash
set -euo pipefail

# Resolve paths from this file, independent of the caller's current directory.
project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$project_dir"

if [[ -x '/Users/jdm/Library/Java/JavaVirtualMachines/jdk-21.0.13+9/Contents/Home/bin/java' ]]; then
  JAVA_HOME='/Users/jdm/Library/Java/JavaVirtualMachines/jdk-21.0.13+9/Contents/Home'
else
  JAVA_HOME="$(/usr/libexec/java_home -v 21)"
fi
java_bin="$JAVA_HOME/bin/java"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

# Load an optional TfL app key for provider requests; it is not required for local startup.
# Read only this variable from the project .env; never print it or write it to logs.
if [[ -z "${TFL_APP_KEY:-}" && -f "$project_dir/.env" ]]; then
  tfl_app_key="$(awk -F= '$1 ~ /^[[:space:]]*TFL_APP_KEY[[:space:]]*$/ {sub(/^[^=]*=[[:space:]]*/, ""); print; exit}' "$project_dir/.env")"
  tfl_app_key="${tfl_app_key%$'\r'}"
  if [[ "${tfl_app_key:0:1}" == '"' && "${tfl_app_key: -1}" == '"' ]]; then
    tfl_app_key="${tfl_app_key:1:${#tfl_app_key}-2}"
  elif [[ "${tfl_app_key:0:1}" == "'" && "${tfl_app_key: -1}" == "'" ]]; then
    tfl_app_key="${tfl_app_key:1:${#tfl_app_key}-2}"
  fi
  if [[ -n "$tfl_app_key" ]]; then export TFL_APP_KEY="$tfl_app_key"; fi
  unset tfl_app_key
fi

# Optional key for the one-provider-at-a-time location demo. Keep it local and
# out of logs/source control, just like the TfL key.
if [[ -z "${GEOAPIFY_API_KEY:-}" && -f "$project_dir/.env" ]]; then
  geoapify_api_key="$(awk -F= '$1 ~ /^[[:space:]]*GEOAPIFY_API_KEY[[:space:]]*$/ {sub(/^[^=]*=[[:space:]]*/, ""); print; exit}' "$project_dir/.env")"
  geoapify_api_key="${geoapify_api_key%$'\r'}"
  if [[ "${geoapify_api_key:0:1}" == '"' && "${geoapify_api_key: -1}" == '"' ]]; then
    geoapify_api_key="${geoapify_api_key:1:${#geoapify_api_key}-2}"
  elif [[ "${geoapify_api_key:0:1}" == "'" && "${geoapify_api_key: -1}" == "'" ]]; then
    geoapify_api_key="${geoapify_api_key:1:${#geoapify_api_key}-2}"
  fi
  if [[ -n "$geoapify_api_key" ]]; then export GEOAPIFY_API_KEY="$geoapify_api_key"; fi
  unset geoapify_api_key
fi

# Optional MapTiler key for sequential provider comparison.
if [[ -z "${MAPTILER_API_KEY:-}" && -f "$project_dir/.env" ]]; then
  maptiler_api_key="$(awk -F= '$1 ~ /^[[:space:]]*MAPTILER_API_KEY[[:space:]]*$/ {sub(/^[^=]*=[[:space:]]*/, ""); print; exit}' "$project_dir/.env")"
  maptiler_api_key="${maptiler_api_key%$'\r'}"
  if [[ "${maptiler_api_key:0:1}" == '"' && "${maptiler_api_key: -1}" == '"' ]]; then
    maptiler_api_key="${maptiler_api_key:1:${#maptiler_api_key}-2}"
  elif [[ "${maptiler_api_key:0:1}" == "'" && "${maptiler_api_key: -1}" == "'" ]]; then
    maptiler_api_key="${maptiler_api_key:1:${#maptiler_api_key}-2}"
  fi
  if [[ -n "$maptiler_api_key" ]]; then export MAPTILER_API_KEY="$maptiler_api_key"; fi
  unset maptiler_api_key
fi

# Optional LocationIQ key for sequential provider comparison.
if [[ -z "${LOCATIONIQ_API_KEY:-}" && -f "$project_dir/.env" ]]; then
  locationiq_api_key="$(awk -F= '$1 ~ /^[[:space:]]*LOCATIONIQ_API_KEY[[:space:]]*$/ {sub(/^[^=]*=[[:space:]]*/, ""); print; exit}' "$project_dir/.env")"
  locationiq_api_key="${locationiq_api_key%$'\r'}"
  if [[ "${locationiq_api_key:0:1}" == '"' && "${locationiq_api_key: -1}" == '"' ]]; then
    locationiq_api_key="${locationiq_api_key:1:${#locationiq_api_key}-2}"
  elif [[ "${locationiq_api_key:0:1}" == "'" && "${locationiq_api_key: -1}" == "'" ]]; then
    locationiq_api_key="${locationiq_api_key:1:${#locationiq_api_key}-2}"
  fi
  if [[ -n "$locationiq_api_key" ]]; then export LOCATIONIQ_API_KEY="$locationiq_api_key"; fi
  unset locationiq_api_key
fi

# Optional OpenCage key for sequential provider comparison.
if [[ -z "${OPENCAGE_API_KEY:-}" && -f "$project_dir/.env" ]]; then
  opencage_api_key="$(awk -F= '$1 ~ /^[[:space:]]*OPENCAGE_API_KEY[[:space:]]*$/ {sub(/^[^=]*=[[:space:]]*/, ""); print; exit}' "$project_dir/.env")"
  opencage_api_key="${opencage_api_key%$'\r'}"
  if [[ "${opencage_api_key:0:1}" == '"' && "${opencage_api_key: -1}" == '"' ]]; then
    opencage_api_key="${opencage_api_key:1:${#opencage_api_key}-2}"
  elif [[ "${opencage_api_key:0:1}" == "'" && "${opencage_api_key: -1}" == "'" ]]; then
    opencage_api_key="${opencage_api_key:1:${#opencage_api_key}-2}"
  fi
  if [[ -n "$opencage_api_key" ]]; then export OPENCAGE_API_KEY="$opencage_api_key"; fi
  unset opencage_api_key
fi

# Optional TomTom key for one-provider-at-a-time location comparison.
if [[ -z "${TOMTOM_API_KEY:-}" && -f "$project_dir/.env" ]]; then
  tomtom_api_key="$(awk -F= '$1 ~ /^[[:space:]]*TOMTOM_API_KEY[[:space:]]*$/ {sub(/^[^=]*=[[:space:]]*/, ""); print; exit}' "$project_dir/.env")"
  tomtom_api_key="${tomtom_api_key%$'\r'}"
  if [[ "${tomtom_api_key:0:1}" == '"' && "${tomtom_api_key: -1}" == '"' ]]; then
    tomtom_api_key="${tomtom_api_key:1:${#tomtom_api_key}-2}"
  elif [[ "${tomtom_api_key:0:1}" == "'" && "${tomtom_api_key: -1}" == "'" ]]; then
    tomtom_api_key="${tomtom_api_key:1:${#tomtom_api_key}-2}"
  fi
  if [[ -n "$tomtom_api_key" ]]; then export TOMTOM_API_KEY="$tomtom_api_key"; fi
  unset tomtom_api_key
fi

# Optional Foursquare key for one-provider-at-a-time location comparison.
if [[ -z "${FOURSQUARE_API_KEY:-}" && -f "$project_dir/.env" ]]; then
  foursquare_api_key="$(awk -F= '$1 ~ /^[[:space:]]*FOURSQUARE_API_KEY[[:space:]]*$/ {sub(/^[^=]*=[[:space:]]*/, ""); print; exit}' "$project_dir/.env")"
  foursquare_api_key="${foursquare_api_key%$'\r'}"
  if [[ "${foursquare_api_key:0:1}" == '"' && "${foursquare_api_key: -1}" == '"' ]]; then
    foursquare_api_key="${foursquare_api_key:1:${#foursquare_api_key}-2}"
  elif [[ "${foursquare_api_key:0:1}" == "'" && "${foursquare_api_key: -1}" == "'" ]]; then
    foursquare_api_key="${foursquare_api_key:1:${#foursquare_api_key}-2}"
  fi
  if [[ -n "$foursquare_api_key" ]]; then export FOURSQUARE_API_KEY="$foursquare_api_key"; fi
  unset foursquare_api_key
fi

backend_pid=''
runtime_dir=''
cleanup() {
  if [[ -n "$backend_pid" ]]; then
    kill "$backend_pid" 2>/dev/null || true
    wait "$backend_pid" 2>/dev/null || true
  fi
  if [[ -n "$runtime_dir" ]]; then
    rm -f "$runtime_dir/commute-fit.jar"
    rmdir "$runtime_dir" 2>/dev/null || true
  fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

mkdir -p .local
backend_log="$project_dir/.local/journey-backend.log"
jar_file="$project_dir/backend/target/commute-fit-0.1.0-SNAPSHOT.jar"

# GET returns 405 on the POST-only endpoint when the current API is available.
api_code="$(curl --max-time 2 -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/journeys || true)"
if [[ "$api_code" == '405' ]]; then
  echo 'A backend is already running on 8080. Stop it with Ctrl+C in its terminal before restarting.'
  echo '8080 上已有后端运行。请先在原启动终端按 Ctrl+C，再运行本脚本，确保加载最新版本。'
  exit 1
fi
if [[ "$api_code" != '405' ]]; then
  echo 'Building the backend / 正在构建后端…'
  (cd backend && ./mvnw -o -Daether.syncContext.named.factory=noop --batch-mode --no-transfer-progress -DskipTests package)
  # A running Boot loader may still read classes lazily. Never let a later
  # Maven build overwrite the archive used by the active backend process.
  runtime_dir="$(mktemp -d "$project_dir/.local/commute-fit-run.XXXXXX")"
  cp "$jar_file" "$runtime_dir/commute-fit.jar"
  echo 'Starting the backend on 8080 / 正在启动后端…'
  "$java_bin" -jar "$runtime_dir/commute-fit.jar" --server.address=127.0.0.1 --server.port=8080 --spring.profiles.active=default >"$backend_log" 2>&1 &
  backend_pid=$!
  backend_ready=false
  for ((attempt=0; attempt<40; attempt++)); do
    if ! kill -0 "$backend_pid" 2>/dev/null; then
      echo 'Backend exited. Startup log / 后端已退出，启动日志：'
      tail -n 60 "$backend_log"
      exit 1
    fi
    api_code="$(curl --max-time 1 -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/journeys || true)"
    if [[ "$api_code" == '405' ]]; then backend_ready=true; break; fi
    sleep 1
  done
  if [[ "$backend_ready" != true ]]; then
    echo 'Backend did not become ready / 后端未能就绪：'
    tail -n 60 "$backend_log"
    exit 1
  fi
fi

echo 'Backend ready. Open the URL printed below. / 后端已就绪，请打开下方 Vite 输出的网址。'
echo 'Keep this terminal open; Ctrl+C stops services started by this script. / 保持本终端开启，Ctrl+C 停止本脚本启动的服务。'
cd "$project_dir/frontend"
if [[ ! -d node_modules ]]; then npm ci; fi
VITE_API_PROXY_TARGET=http://127.0.0.1:8080 npm run dev
