# Source this file in cloud tasks; Android Studio manages its own local tools.
export JAVA_HOME=/workspace/toolchains/jdk-17.0.17+10
export ANDROID_HOME=/workspace/toolchains/android-sdk
export ANDROID_USER_HOME=/workspace/toolchains/android-user
export GRADLE_USER_HOME=/workspace/toolchains/gradle-home
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
# Java does not automatically use the cloud's HTTPS_PROXY environment variable.
if [ -n "${HTTPS_PROXY:-}" ]; then
    quiet_proxy_options=$(python3 - <<'PY'
import os, urllib.parse
p=urllib.parse.urlsplit(os.environ['HTTPS_PROXY'])
if p.username or p.password:
    raise SystemExit('Authenticated proxy needs platform-supported Java configuration')
print(f'-Dhttps.proxyHost={p.hostname} -Dhttps.proxyPort={p.port or 80} -Dhttp.proxyHost={p.hostname} -Dhttp.proxyPort={p.port or 80}')
PY
    )
    export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} $quiet_proxy_options -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
    unset quiet_proxy_options
fi
