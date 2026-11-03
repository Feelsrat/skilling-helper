#!/data/data/com.termux/files/usr/bin/bash
# Skilling Helper for Android, run from Termux. Plays the RuneLite dev client with the plugins built in
# through GameNative (which runs Windows programs on Android).
#
#   bash skilling-helper-android.sh            install everything, or update to the newest release
#   skilling-helper-android --play             update, then start the game in GameNative
#   skilling-helper-android --daily            also check for updates once a day in the background
#   skilling-helper-android --no-daily         stop the daily check
#   skilling-helper-android --reinstall-java   set the game up in GameNative again (after removing it there)
#
# What goes where:
#   GameNative/CustomGames/Skilling Helper   a Windows Java 21. GameNative finds it there by itself, lists it as a
#                                             game and moves it into its own storage on first start.
#   Download/skilling-helper                  skilling-helper-all.jar and RuneLite's settings (home/.runelite).
#                                             GameNative shows the Download folder to Windows as drive D:.
# It also adds "Play Skilling Helper" and "Update Skilling Helper" buttons for the Termux:Widget app.
set -euo pipefail

REPO="${SKILLING_HELPER_REPO:-Feelsrat/skilling-helper}"
API="https://api.github.com"
STORAGE="${SKILLING_HELPER_STORAGE:-/storage/emulated/0}"
DATA="$STORAGE/Download/skilling-helper"
GAME="$STORAGE/GameNative/CustomGames/Skilling Helper"
GN_PKG=app.gamenative
GN_REPO=utkarshdalal/GameNative
GN_FALLBACK_APK="https://downloads.gamenative.app/releases/1.3.0/gamenative-v1.3.0.apk"
# Fixed custom-game id, written to the game folder's .gamenative file, so the Play button can start it
APP_ID=742011
JAVA_ARGS='-ea -Xmx1g -Duser.home=D:\skilling-helper\home -jar D:\skilling-helper\skilling-helper-all.jar --developer-mode'
STATE="$HOME/.config/skilling-helper-android"
TOKEN_FILE="$STATE/token"
SELF="$HOME/.local/bin/skilling-helper-android"
JRE_API="https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jre&os=windows&vendor=eclipse"
QUIET=0

say() { [ "$QUIET" = 1 ] || echo "$@"; }
notify() { command -v termux-notification >/dev/null 2>&1 && termux-notification --title "$1" --content "$2" >/dev/null 2>&1 || true; }
fail() { echo "Error: $*" >&2; notify "Skilling Helper" "$*"; exit 1; }

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
	if [ ! -w "$STORAGE/Download" ]; then
		[ "$QUIET" = 1 ] && fail "no storage access; run the script once by hand"
		say "Termux needs storage access. Allow it in the prompt."
		termux-setup-storage
		sleep 3
		[ -w "$STORAGE/Download" ] || fail "still no storage access. Run termux-setup-storage and try again."
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

gamenative_installed() {
	[ -n "${SKILLING_HELPER_ASSUME_GAMENATIVE:-}" ] && return 0
	pm path "$GN_PKG" 2>/dev/null | grep -q package: || cmd package path "$GN_PKG" 2>/dev/null | grep -q package:
}

install_gamenative() {
	gamenative_installed && return 0
	[ "$QUIET" = 1 ] && fail "GameNative isn't installed; run the script once by hand"
	say "GameNative isn't installed. Downloading it..."
	local tmp url apk
	tmp="$(mktemp -d)"
	url=""
	if curl -fsSL --retry 2 -H "User-Agent: skilling-helper-android" -o "$tmp/gn.json" "$API/repos/$GN_REPO/releases/latest"; then
		url="$(jq -r '[.assets[] | select(.name | test("^gamenative-v[0-9.]+\\.apk$"))][0].browser_download_url // empty' "$tmp/gn.json")"
	fi
	[ -n "$url" ] || url="$GN_FALLBACK_APK"
	apk="$STORAGE/Download/$(basename "$url")"
	curl -fSL --retry 3 -o "$apk" "$url" || fail "couldn't download GameNative"
	rm -rf "$tmp"
	say ""
	say "Opening the installer. If Android asks, allow Termux to install apps, then tap Install."
	termux-open "$apk" || say "Couldn't open it. Install $apk with your file manager."
	say ""
	say "After it's installed: open GameNative once and finish its first-start setup."
	say "Then run this script again to finish."
	exit 0
}

install_java() {
	mkdir -p "$STATE"
	# GameNative moves the folder into its own storage on first start, so remember that it was done
	if [ -f "$STATE/java-installed" ] && [ "${1:-}" != force ]; then return 0; fi
	say "Downloading Windows Java 21 (about 50 MB)..."
	local tmp; tmp="$(mktemp -d)"
	curl -fsSL --retry 3 -o "$tmp/info.json" "$JRE_API"
	local url sum
	url="$(jq -r '.[0].binary.package.link' "$tmp/info.json")"
	sum="$(jq -r '.[0].binary.package.checksum' "$tmp/info.json")"
	curl -fSL --retry 3 -o "$tmp/jre.zip" "$url"
	echo "$sum  $tmp/jre.zip" | sha256sum -c --quiet - || fail "Java download is corrupt; try again"
	unzip -q "$tmp/jre.zip" -d "$tmp/x"
	rm -rf "$GAME"
	mkdir -p "$(dirname "$GAME")"
	mv "$tmp"/x/* "$GAME"
	rm -rf "$tmp"
	# GameNative picks the program to run by itself only when the folder holds exactly one .exe
	find "$GAME/bin" -maxdepth 1 -iname '*.exe' ! -iname 'java.exe' -delete
	printf '{"appId":%s}\n' "$APP_ID" > "$GAME/.gamenative"
	date > "$STATE/java-installed"
	say "Added \"Skilling Helper\" to GameNative's library."
}

update_jar() {
	local tmp; tmp="$(mktemp -d)"
	local tries=0
	until gh_get "$API/repos/$REPO/releases/latest" "$tmp/release.json"; do
		tries=$((tries + 1))
		[ $tries -le 2 ] || fail "couldn't reach GitHub; check the internet connection and try again"
		sleep 5
	done
	local tag have id size
	tag="$(jq -r '.tag_name' "$tmp/release.json")"
	have="$(cat "$DATA/.version" 2>/dev/null || true)"
	if [ "$tag" = "$have" ] && [ -f "$DATA/skilling-helper-all.jar" ]; then
		say "Already on the newest release ($tag)."
	else
		id="$(jq -r '.assets[] | select(.name | endswith("-all.jar")) | .id' "$tmp/release.json" | head -1)"
		size="$(jq -r '.assets[] | select(.name | endswith("-all.jar")) | .size' "$tmp/release.json" | head -1)"
		[ -n "$id" ] || fail "release $tag has no -all.jar"
		say "Downloading Skilling Helper $tag..."
		gh_get "$API/repos/$REPO/releases/assets/$id" "$tmp/new.jar" "application/octet-stream" || fail "download failed"
		[ "$(stat -c %s "$tmp/new.jar")" = "$size" ] || fail "download was cut off; try again"
		[ -f "$DATA/skilling-helper-all.jar" ] && cp "$DATA/skilling-helper-all.jar" "$DATA/skilling-helper-all.jar.bak"
		mv "$tmp/new.jar" "$DATA/skilling-helper-all.jar"
		echo "$tag" > "$DATA/.version"
		say "Updated to $tag."
		[ -n "$have" ] && notify "Skilling Helper updated" "Now on $tag."
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
	cat > "$DATA/README.txt" <<EOF
Skilling Helper on Android (GameNative)
=======================================
This folder holds the RuneLite dev client with the plugins built in (skilling-helper-all.jar) and its
settings (home/.runelite). The Termux script skilling-helper-android keeps it up to date.

Login: the dev client reuses a saved Jagex login. On a PC or Steam Deck follow
https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts (run RuneLite from the Jagex Launcher with
--insecure-write-credentials), then copy .runelite/credentials.properties from that machine into
home/.runelite/ here. Keep that file private: it is your login.

Play: tap "Play Skilling Helper" on the Termux widget (or run: skilling-helper-android --play).
Starting it from GameNative's library instead? Set its arguments (game settings) to:
  $JAVA_ARGS
EOF
}

install_shortcuts() {
	mkdir -p "$(dirname "$SELF")" "$HOME/.shortcuts"
	if [ "$(readlink -f "$0")" != "$(readlink -f "$SELF")" ]; then
		install -m 755 "$0" "$SELF"
	fi
	printf '#!/data/data/com.termux/files/usr/bin/bash\nexec "%s" --play\n' "$SELF" > "$HOME/.shortcuts/Play Skilling Helper"
	printf '#!/data/data/com.termux/files/usr/bin/bash\nexec "%s"\n' "$SELF" > "$HOME/.shortcuts/Update Skilling Helper"
	chmod 755 "$HOME/.shortcuts/Play Skilling Helper" "$HOME/.shortcuts/Update Skilling Helper"
}

schedule() {
	command -v termux-job-scheduler >/dev/null 2>&1 || pkg install -y termux-api
	say "The daily check needs the Termux:API app too (from the same place you got Termux)."
	termux-job-scheduler --job-id 4207 --script "$SELF" --period-ms 86400000 --network any --persisted true >/dev/null \
		&& say "Will check for updates once a day." || fail "couldn't schedule; is the Termux:API app installed?"
}

# The Steam Deck updater's --phone-kit makes skilling-helper-phone.zip with the Jagex login in it.
# Pick it up from Download (zipped, or already unzipped), put the login in place and delete the copy.
import_phone_kit() {
	local dest="$DATA/home/.runelite/credentials.properties" kit dir
	for kit in "$STORAGE"/Download/skilling-helper-phone*.zip; do
		[ -f "$kit" ] || continue
		local tmp; tmp="$(mktemp -d)"
		if unzip -q -o "$kit" home/.runelite/credentials.properties -d "$tmp" 2>/dev/null; then
			install -m 600 "$tmp/home/.runelite/credentials.properties" "$dest"
			rm -f "$kit"
			say "Imported your Jagex login from $(basename "$kit") (and deleted the zip)."
		fi
		rm -rf "$tmp"
	done
	for dir in "$STORAGE"/Download/skilling-helper-phone*/; do
		[ -f "$dir/home/.runelite/credentials.properties" ] || continue
		install -m 600 "$dir/home/.runelite/credentials.properties" "$dest"
		rm -rf "$dir"
		say "Imported your Jagex login from $(basename "$dir") (and deleted the folder)."
	done
	return 0
}

play() {
	if [ ! -f "$DATA/home/.runelite/credentials.properties" ]; then
		say "Heads up: no Jagex login file yet, so the game will ask for one. See $DATA/README.txt"
	fi
	say "Starting Skilling Helper in GameNative..."
	am start -n "$GN_PKG/.MainActivity" -a "$GN_PKG.LAUNCH_GAME" \
		--ei app_id "$APP_ID" --es game_source CUSTOM_GAME --es exec_args "$JAVA_ARGS" >/dev/null \
		|| fail "couldn't start GameNative. Open it and start \"Skilling Helper\" from its library."
}

MODE="${1:-}"
case "$MODE" in
	--no-daily) termux-job-scheduler --cancel --job-id 4207; echo "Daily check stopped."; exit 0 ;;
	""|--play|--daily|--reinstall-java) ;;
	*) echo "Unknown option $MODE"; exit 1 ;;
esac
# No terminal (scheduled job): keep quiet, report through a notification
[ -t 1 ] || QUIET=1

need_tools
need_storage
install_gamenative
mkdir -p "$DATA/home/.runelite"
install_shortcuts
import_phone_kit
if [ "$MODE" = --reinstall-java ]; then install_java force; else install_java; fi
if [ "$MODE" = --play ]; then
	# Don't block playing on a failed update check
	( update_jar ) || say "Update check failed; starting the version you have."
	play
	exit 0
fi
update_jar
write_readme
[ "$MODE" = --daily ] && schedule
say ""
if [ ! -f "$DATA/home/.runelite/credentials.properties" ]; then
	say "Still needed: your Jagex login. On the Steam Deck double-click \"Skilling Helper phone kit\","
	say "copy skilling-helper-phone.zip into this phone's Download folder and run this again."
fi
say "All set. Add the Termux:Widget widget to your home screen and tap \"Play Skilling Helper\"."
