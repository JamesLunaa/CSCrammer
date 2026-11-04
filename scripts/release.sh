#!/usr/bin/env bash
# Builds the signed release APK locally and publishes it as a GitHub release.
# The signing key never leaves this machine.
#
# Usage: scripts/release.sh [--dry-run] <version>      e.g. scripts/release.sh 1.2
#
# Before running: bump versionCode/versionName in app/build.gradle.kts, add a
# "## [<version>]" section to CHANGELOG.md, merge to master, and check out master.
set -euo pipefail

dry_run=false
if [[ "${1:-}" == "--dry-run" ]]; then dry_run=true; shift; fi
version="${1:-}"
[[ -n "$version" ]] || { echo "Usage: scripts/release.sh [--dry-run] <version>" >&2; exit 1; }

cd "$(git rev-parse --show-toplevel)"
fail() { echo "error: $*" >&2; exit 1; }
warn() { echo "warning: $*" >&2; }

# Repo state: clean master, in sync with GitHub.
[[ "$(git branch --show-current)" == "master" ]] || fail "check out master first"
[[ -z "$(git status --porcelain)" ]] || fail "working tree has uncommitted changes"
git fetch -q origin master --tags
[[ "$(git rev-parse HEAD)" == "$(git rev-parse origin/master)" ]] || fail "master differs from origin/master; push or pull first"
if git rev-parse -q --verify "refs/tags/$version" >/dev/null; then
  $dry_run && warn "tag $version already exists" || fail "tag $version already exists"
fi

# Version and changelog must match.
app_version=$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts)
[[ "$app_version" == "$version" ]] || fail "versionName in app/build.gradle.kts is $app_version, not $version"
grep -q "^## \[$version\]" CHANGELOG.md || fail "CHANGELOG.md has no '## [$version]' section"
[[ -f keystore.properties ]] || fail "keystore.properties is missing, so the APK can't be signed"

# Build tools from the Android SDK.
sdk="${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' local.properties 2>/dev/null)}"
build_tools=$(ls -d "$sdk"/build-tools/* 2>/dev/null | sort -V | tail -1)
[[ -n "$build_tools" ]] || fail "Android SDK build-tools not found (set ANDROID_HOME)"

echo "==> Testing and building $version"
./gradlew -q test assembleRelease

apk_out=app/build/outputs/apk/release/app-release.apk
[[ -f "$apk_out" ]] || fail "no signed APK at $apk_out (is keystore.properties correct?)"
"$build_tools/apksigner" verify "$apk_out" || fail "APK signature check failed"
built_version=$(sed -n "s/.*versionName='\([^']*\)'.*/\1/p" <("$build_tools/aapt2" dump badging "$apk_out" | head -1))
[[ "$built_version" == "$version" ]] || fail "built APK reports version $built_version"

# A dry run leaves dist/ alone, so it always matches what was published.
if $dry_run; then out_dir=$(mktemp -d); else out_dir=dist; mkdir -p dist; fi
apk="$out_dir/CSCrammer-v$version.apk"
cp "$apk_out" "$apk"
sha=$(sha256sum "$apk" | cut -d' ' -f1)

# Release notes: this version's changelog section, plus install info.
notes=$(mktemp)
trap 'rm -f "$notes"' EXIT
# $(...) drops trailing blank lines, so exactly one blank line precedes "### Install".
printf '%s\n' "$(awk -v v="$version" '
  $0 ~ "^## \\[" v "\\]" { on = 1; next }
  on && /^## \[/ { exit }
  on && /^\[[^]]+\]: / { next }
  on { print }
' CHANGELOG.md | sed -e '/./,$!d')" > "$notes"
cat >> "$notes" <<NOTES

### Install

Download \`CSCrammer-v$version.apk\` below and open it on your phone. It installs over earlier versions and keeps your progress.

SHA-256: \`$sha\`
NOTES

echo
echo "==> Ready: $apk"
echo "    SHA-256 $sha"
echo "    Release notes:"
sed 's/^/    | /' "$notes"
echo

if $dry_run; then
  echo "Dry run: nothing published."
  exit 0
fi

read -r -p "Publish GitHub release $version from master? [y/N] " answer
[[ "$answer" == [yY] ]] || { echo "Cancelled."; exit 0; }
gh release create "$version" "$apk" --target master --title "CSCrammer v$version" --notes-file "$notes"
