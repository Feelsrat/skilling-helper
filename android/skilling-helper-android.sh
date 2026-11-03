#!/data/data/com.termux/files/usr/bin/bash
# Skilling Helper for Android (GameNative custom game), run from Termux.
#
#   bash skilling-helper-android.sh            set up the folder, or update it to the newest release
#   bash skilling-helper-android.sh --daily    also check for updates once a day in the background
#   bash skilling-helper-android.sh --no-daily stop the daily check
#
# It keeps /sdcard/Download/skilling-helper-gamenative up to date: a Windows Java 21 (jre\) and
# skilling-helper-all.jar from the newest GitHub release. GameNative runs that folder as a custom game
# (see the README in it). It also adds an "Update Skilling Helper" button for the Termux:Widget app.
set -euo pipefail

REPO="${SKILLING_HELPER_REPO:-Feelsrat/skilling-helper}"
API="https://api.github.com"
DEST="${SKILLING_HELPER_DIR:-/sdcard/Download/skilling-helper-gamenative}"
STATE="$HOME/.config/skilling-helper-android"
TOKEN_FILE="$STATE/token"
SELF="$HOME/.local/bin/skilling-helper-android"
JRE_API="https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jre&os=windows&vendor=eclipse"
QUIET=0

say() { [ "$QUIET" = 1 ] || echo "$@"; }
fail() { echo "Error: $*" >&2; notify "Skilling Helper update failed" "$*"; exit 1; }
notify() { command -v termux-notification >/dev/null 2>&1 && termux-notification --title "$1" --content "$2" >/dev/null 2>&1 || true; }

need_tools() {
	local missing=()
	for t in curl jq unzip; do command -v "$t" >/dev/null 2>&1 || missing+=("$t"); done
	if [ ${#missing[@]} -gt 0 ]; then
		[ "$QUIET" = 1 ] && fail "missing ${missing[*]}; run the script once by hand"
		say "Installing ${missing[*]}..."
		pkg install -y "${missing[@]}"
	fi
}

need_storage() {
	local parent; parent="$(dirname "$DEST")"
	if [ ! -w "$parent" ]; then
		[ "$QUIET" = 1 ] && fail "no storage access; run the script once by hand"
		say "Termux needs storage access to write to Download/. Allow it in the prompt."
		termux-setup-storage
		sleep 3
		[ -w "$parent" ] || fail "still no storage access. Run termux-setup-storage and try again."
	fi
}

# Optional: a token in this file is only needed if the repo goes private again
token() { [ -f "$TOKEN_FILE" ] && tr -d '[:space:]' < "$TOKEN_FILE" || true; }

# gh_get URL OUT [ACCEPT]: curl drops the Authorization header when a download redirects to another host.
gh_get() {
	local auth=()
	local t; t="$(token)"
	[ -n "$t" ] && auth=(-H "Authorization: Bearer $t")
	curl -fsSL --retry 3 -H "Accept: ${3:-application/vnd.github+json}" -H "User-Agent: skilling-helper-android" \
		"${auth[@]}" -o "$2" "$1"
}

latest_release() {
	local tries=0
	until gh_get "$API/repos/$REPO/releases/latest" "$1"; do
		tries=$((tries + 1))
		[ $tries -le 2 ] || fail "couldn't reach GitHub; check the internet connection and try again"
		sleep 5
	done
}

install_jre() {
	[ -f "$DEST/jre/bin/java.exe" ] && return 0
	say "Downloading Windows Java 21 (about 50 MB)..."
	local tmp; tmp="$(mktemp -d)"
	curl -fsSL --retry 3 -o "$tmp/info.json" "$JRE_API"
	local url sum
	url="$(jq -r '.[0].binary.package.link' "$tmp/info.json")"
	sum="$(jq -r '.[0].binary.package.checksum' "$tmp/info.json")"
	curl -fSL --retry 3 -o "$tmp/jre.zip" "$url"
	echo "$sum  $tmp/jre.zip" | sha256sum -c --quiet - || fail "Java download is corrupt; try again"
	unzip -q "$tmp/jre.zip" -d "$tmp/x"
	rm -rf "$DEST/jre"
	mv "$tmp"/x/* "$DEST/jre"
	rm -rf "$tmp"
}

update_jar() {
	local tmp; tmp="$(mktemp -d)"
	latest_release "$tmp/release.json"
	local tag have id size
	tag="$(jq -r '.tag_name' "$tmp/release.json")"
	have="$(cat "$DEST/.version" 2>/dev/null || true)"
	if [ "$tag" = "$have" ] && [ -f "$DEST/skilling-helper-all.jar" ]; then
		say "Already on the newest release ($tag)."
	else
		id="$(jq -r '.assets[] | select(.name | endswith("-all.jar")) | .id' "$tmp/release.json" | head -1)"
		size="$(jq -r '.assets[] | select(.name | endswith("-all.jar")) | .size' "$tmp/release.json" | head -1)"
		[ -n "$id" ] || fail "release $tag has no -all.jar"
		say "Downloading $tag..."
		gh_get "$API/repos/$REPO/releases/assets/$id" "$tmp/new.jar" "application/octet-stream" || fail "download failed"
		[ "$(stat -c %s "$tmp/new.jar")" = "$size" ] || fail "download was cut off; try again"
		[ -f "$DEST/skilling-helper-all.jar" ] && cp "$DEST/skilling-helper-all.jar" "$DEST/skilling-helper-all.jar.bak"
		mv "$tmp/new.jar" "$DEST/skilling-helper-all.jar"
		echo "$tag" > "$DEST/.version"
		say "Updated to $tag. Restart the game in GameNative to use it."
		notify "Skilling Helper updated" "Now on $tag. Restart it in GameNative."
	fi
	# Keep this script current too
	id="$(jq -r '.assets[] | select(.name == "skilling-helper-android.sh") | .id' "$tmp/release.json" | head -1)"
	if [ -n "$id" ] && gh_get "$API/repos/$REPO/releases/assets/$id" "$tmp/self.sh" "application/octet-stream" \
		&& bash -n "$tmp/self.sh" && ! cmp -s "$tmp/self.sh" "$SELF"; then
		mkdir -p "$(dirname "$SELF")"
		install -m 755 "$tmp/self.sh" "$SELF"
	fi
	rm -rf "$tmp"
}

write_readme() {
	cat > "$DEST/README.txt" <<'EOF'
Skilling Helper + PvM Progression in GameNative
================================================
1. Log in once on a PC or Steam Deck: follow
   https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts
   (run RuneLite from the Jagex Launcher with --insecure-write-credentials), then copy
   .runelite/credentials.properties from that machine into home/.runelite/ in this folder.
   Keep that file private: it is your login.
2. In GameNative: add a custom game, pick Download/skilling-helper-gamenative, and in its settings set
     Executable:  jre\bin\java.exe
     Arguments:   -ea -Xmx1g -Duser.home=A:\home -jar A:\skilling-helper-all.jar --developer-mode
3. Press Play. Updates: tap "Update Skilling Helper" on the Termux widget, or turn on the daily check with
   skilling-helper-android --daily
EOF
}

install_self() {
	mkdir -p "$(dirname "$SELF")" "$HOME/.shortcuts"
	if [ "$(readlink -f "$0")" != "$(readlink -f "$SELF")" ]; then
		install -m 755 "$0" "$SELF"
	fi
	printf '#!/data/data/com.termux/files/usr/bin/bash\nexec "%s" "$@"\n' "$SELF" > "$HOME/.shortcuts/Update Skilling Helper"
	chmod 755 "$HOME/.shortcuts/Update Skilling Helper"
}

schedule() {
	command -v termux-job-scheduler >/dev/null 2>&1 || pkg install -y termux-api
	say "Daily check needs the Termux:API app too (from the same place you got Termux)."
	termux-job-scheduler --job-id 4207 --script "$SELF" --period-ms 86400000 --network any --persisted true >/dev/null \
		&& say "Will check for updates once a day." || fail "couldn't schedule; is the Termux:API app installed?"
}

case "${1:-}" in
	--no-daily) termux-job-scheduler --cancel --job-id 4207; say "Daily check stopped."; exit 0 ;;
esac
# No terminal (widget in background or scheduled job): keep quiet, report through a notification
[ -t 1 ] || QUIET=1

need_tools
need_storage
mkdir -p "$DEST/home/.runelite"
install_self
install_jre
update_jar
write_readme
[ "${1:-}" = "--daily" ] && schedule
if [ ! -f "$DEST/home/.runelite/credentials.properties" ]; then
	say ""
	say "Still needed: your Jagex login file. See step 1 of $DEST/README.txt"
fi
say "Folder ready: $DEST"
