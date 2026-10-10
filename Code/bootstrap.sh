#!/usr/bin/env bash
# SepareWorldItems v3 bootstrap (Linux / macOS): prepares Java 8/17/21/25 (existing installs or portable
# Azul Zulu JDKs in Code/.jdks/), builds every platform and collects everything in Code/dist/.
#
#   ./bootstrap.sh                   everything
#   ./bootstrap.sh bukkit fabric     some platforms (bukkit fabric legacy neoforge forge)
#   SELFTEST=1 ./bootstrap.sh        also run the in-game self tests (slow)
#   NO_DOWNLOAD=1 ./bootstrap.sh     never download JDKs
set -u
CODE="$(cd "$(dirname "$0")" && pwd)"
JDKS="$CODE/.jdks"
VERSION="$(grep '^version=' "$CODE/gradle.properties" | cut -d= -f2)"
DIST="$CODE/dist/SepareWorldItems-$VERSION"
ONLY="${*:-bukkit fabric legacy neoforge forge}"
REPORT=()
case "$(uname -s)" in Darwin) OS=macos ;; *) OS=linux ;; esac
case "$(uname -m)" in arm64|aarch64) ARCH=aarch64 ;; *) ARCH=x64 ;; esac

has() { [[ " $ONLY " == *" $1 "* ]]; }

java_major() {
  [ -x "$1/bin/java" ] || return 1
  "$1/bin/java" -version 2>&1 | head -1 | sed -E 's/.*version "(1\.)?([0-9]+).*/\2/'
}

find_jdk() {
  local major=$1 c
  for c in "$JDKS/zulu-$major" /usr/lib/jvm/zulu-$major /usr/lib/jvm/java-$major-openjdk* /usr/lib/jvm/*$major* \
           /Library/Java/JavaVirtualMachines/*$major*/Contents/Home "${JAVA_HOME:-}"; do
    [ -n "$c" ] && [ "$(java_major "$c" 2>/dev/null)" = "$major" ] && { echo "$c"; return 0; }
  done
  return 1
}

install_jdk() {
  local major=$1 json url name tmp
  json=$(curl -fsSL "https://api.azul.com/metadata/v1/zulu/packages/?java_version=$major&os=$OS&arch=$ARCH&archive_type=tar.gz&java_package_type=jdk&javafx_bundled=false&crac_supported=false&latest=true&release_status=ga&availability_types=CA&page_size=1") || return 1
  url=$(echo "$json" | sed -E 's/.*"download_url":"([^"]+)".*/\1/')
  name=$(basename "$url")
  echo "   downloading $name ..." >&2
  mkdir -p "$JDKS" && tmp="$JDKS/tmp-$major" && rm -rf "$tmp" && mkdir -p "$tmp"
  curl -fsSL "$url" | tar -xz -C "$tmp" || return 1
  local inner; inner=$(find "$tmp" -mindepth 1 -maxdepth 1 -type d | head -1)
  [ -d "$inner/Contents/Home" ] && inner="$inner/Contents/Home"
  rm -rf "$JDKS/zulu-$major" && mv "$inner" "$JDKS/zulu-$major" && rm -rf "$tmp"
  echo "$JDKS/zulu-$major"
}

echo "== SepareWorldItems $VERSION bootstrap =="
echo; echo "[1/3] Java"
declare -A JDK
for major in 8 17 21 25; do
  path=$(find_jdk $major) || { [ -z "${NO_DOWNLOAD:-}" ] && path=$(install_jdk $major); } || path=""
  if [ -n "$path" ]; then JDK[$major]="$path"; echo "   JDK $major $path"; else echo "   JDK $major missing"; REPORT+=("MISSING  JDK $major"); fi
done
[ -n "${JDK[25]:-}" ] || { echo "Gradle needs JDK 25+"; exit 1; }
export JAVA_HOME="${JDK[25]}"
PATHS=$(IFS=,; echo "${JDK[*]}")
ARGS=(--console=plain "-Porg.gradle.java.installations.paths=$PATHS" -Porg.gradle.java.installations.auto-download=false)

gradle_step() {
  local name=$1 dir=$2; shift 2
  local log="$CODE/build/bootstrap-${name//[^a-zA-Z0-9]/-}.log"
  mkdir -p "$CODE/build"
  echo "   $name ..."
  if (cd "$dir" && ./gradlew "$@" "${ARGS[@]}" >"$log" 2>&1); then REPORT+=("OK       $name"); echo "   OK       $name"
  else REPORT+=("FAILED   $name -> $log"); echo "   FAILED   $name (log: $log)"; fi
}

echo; echo "[2/3] Build"
gradle_step "api + common" "$CODE" :api:build :api:javadocJar :api:sourcesJar :common:test
has bukkit   && gradle_step "plugin (java8 / java17 / java21)" "$CODE" :bukkit:build
has fabric   && gradle_step "fabric" "$CODE" build -x :bukkit:build
has legacy   && gradle_step "fabric 1.12.2 (Legacy Fabric)" "$CODE/fabric-legacy" build
has neoforge && gradle_step "neoforge" "$CODE/neoforge" build
has forge    && gradle_step "forge" "$CODE/forge" build

if [ -n "${SELFTEST:-}" ]; then
  run_test() {
    local name=$1 dir=$2 task=$3 log="$CODE/build/selftest-${1//[^a-zA-Z0-9.]/-}.log" line
    (cd "$dir" && ./gradlew "$task" -PswiSelftest "${ARGS[@]}" >"$log" 2>&1)
    line=$(grep -o '\[SelfTest\] \(PASSED\|FAILED\).*' "$log" | tail -1); line=${line:-"did not run (see log)"}
    [[ "$line" == *PASSED* ]] && REPORT+=("OK       selftest $name: $line") || REPORT+=("FAILED   selftest $name: $line")
    echo "   selftest $name: $line"
  }
  has fabric   && for d in "$CODE"/fabric/versions/*/;   do v=$(basename "$d"); run_test "fabric $v" "$CODE" ":fabric:$v:runServer"; done
  has legacy   && run_test "fabric 1.12.2" "$CODE/fabric-legacy" runServer
  has neoforge && for d in "$CODE"/neoforge/versions/*/; do v=$(basename "$d"); run_test "neoforge $v" "$CODE/neoforge" ":$v:runServer"; done
  has forge    && for d in "$CODE"/forge/versions/*/;    do v=$(basename "$d"); run_test "forge $v" "$CODE/forge" ":$v:runServer"; done
fi

echo; echo "[3/3] Collect -> $DIST"
rm -rf "$DIST"; mkdir -p "$DIST"/{plugin,fabric,neoforge,forge,api,configs/plugin,configs/mods}
for p in bukkit:plugin fabric:fabric neoforge:neoforge forge:forge; do
  # Exactly this version (ignores older -SNAPSHOT or previous builds still in build/libs).
  for j in "$CODE/build/libs/${p%%:*}/"*-"$VERSION".jar "$CODE/build/libs/${p%%:*}/"*-"$VERSION"-java*.jar; do
    [ -f "$j" ] && cp "$j" "$DIST/${p##*:}/"
  done
done
for j in "$CODE"/api/build/libs/separeworlditems-api-"$VERSION".jar "$CODE"/api/build/libs/separeworlditems-api-"$VERSION"-{sources,javadoc}.jar; do [ -f "$j" ] && cp "$j" "$DIST/api/"; done
cp "$CODE/bukkit/src/main/resources/config.yml" "$DIST/configs/plugin/config.yml"
cp "$CODE/fabric/src/main/resources/separeworlditems/config.yml" "$DIST/configs/mods/config.yml"

check() { ls "$DIST/$1/"*"-$2"[-.]* >/dev/null 2>&1 && REPORT+=("  OK       $1 $2") || REPORT+=("  MISSING  $1 $2"); }
REPORT+=("" "Jars:")
for v in java8 java17 java21; do check plugin $v; done
for d in "$CODE"/fabric/versions/*/ 1.12.2; do check fabric "$(basename "$d")"; done
for d in "$CODE"/neoforge/versions/*/; do check neoforge "$(basename "$d")"; done
for d in "$CODE"/forge/versions/*/; do check forge "$(basename "$d")"; done

{
  echo "SepareWorldItems $VERSION - build report ($(date '+%Y-%m-%d %H:%M'))"
  echo
  echo "plugin/    Spigot/Paper/Purpur/Folia: -java8 = MC 1.8.8-1.16.5, -java17 = 1.17-1.20.4, -java21 = 1.20.5-26.x"
  echo "fabric/    Fabric (needs Fabric API; 1.12.2 needs Legacy Fabric API)"
  echo "neoforge/  NeoForge      forge/  MinecraftForge"
  echo "api/       separeworlditems-api (compile against it, do not install it on a server)"
  echo "configs/   plugin/config.yml -> plugins/SepareWorldItems/   mods/config.yml -> config/separeworlditems/"
  echo
  printf '%s\n' "${REPORT[@]}"
} > "$DIST/BUILD-REPORT.txt"

echo; printf '%s\n' "${REPORT[@]}" | grep -E 'MISSING|FAILED' && echo "Done with problems." || echo "Done: everything built."
echo "Output: $DIST"
