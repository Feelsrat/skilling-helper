#!/data/data/com.termux/files/usr/bin/bash
# Skilling Helper for Android, run from Termux. Plays the RuneLite dev client with the plugins built in
# through GameNative (which runs Windows programs on Android).
#
#   bash skilling-helper-android.sh      set everything up (installs GameNative if needed)
#   skilling-helper-android              build the game folder again, e.g. after a new Jagex login
#   skilling-helper-android --play       start the game in GameNative
#
# It builds Download/SkillingHelper, which you import into GameNative once (Add Custom Game). GameNative copies
# it into its own storage; GameNative isn't allowed to read files other apps saved, so the game carries
# everything itself:
#   SkillingHelper.exe   the only .exe GameNative looks at, so it picks it as the program on the first Play.
#                        It updates client.jar from the newest GitHub release on every start, then runs Java.
#   jre\                 a Windows Java 21 (one folder deeper than GameNative looks for programs)
#   client.jar           the RuneLite dev client with the plugins built in, version.txt says which release
#   home\.runelite\      RuneLite's settings and your Jagex login (from the Steam Deck's phone kit)
# Updates need nothing from here: the launcher fetches them. Only a new login means building and importing again.
set -euo pipefail

REPO="${SKILLING_HELPER_REPO:-Feelsrat/skilling-helper}"
API="https://api.github.com"
STORAGE="${SKILLING_HELPER_STORAGE:-/storage/emulated/0}"
GAME="$STORAGE/Download/SkillingHelper"
# Left over from earlier versions of this script
OLD_DATA="$STORAGE/Download/skilling-helper"
OLD_GAME="$STORAGE/GameNative/CustomGames/Skilling Helper"
GN_PKG=app.gamenative
GN_REPO=utkarshdalal/GameNative
GN_FALLBACK_APK="https://downloads.gamenative.app/releases/1.3.0/gamenative-v1.3.0.apk"
# Fixed custom-game id, written to the game folder's .gamenative file, so the Play button can start it
APP_ID=742013
STATE="$HOME/.config/skilling-helper-android"
TOKEN_FILE="$STATE/token"
# Termux puts $PREFIX/bin on PATH (not ~/.local/bin)
SELF="${PREFIX:-$HOME/.local}/bin/skilling-helper-android"
LOGIN="$STATE/credentials.properties"
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

# Earlier versions put the game in GameNative/CustomGames (GameNative can't delete Termux's files there, so it
# kept coming back) and the jar in Download/skilling-helper (GameNative can't read it there). Clean both up,
# keeping the Jagex login.
clean_old() {
	mkdir -p "$STATE"
	if [ -f "$OLD_DATA/home/.runelite/credentials.properties" ] && [ ! -f "$LOGIN" ]; then
		install -m 600 "$OLD_DATA/home/.runelite/credentials.properties" "$LOGIN"
	fi
	if [ -d "$OLD_DATA" ]; then
		rm -rf "$OLD_DATA"
		say "Cleaned up Download/skilling-helper from the earlier setup."
	fi
	if [ -d "$OLD_GAME" ]; then
		rm -rf "$OLD_GAME"
		say "Removed the old \"Skilling Helper\" folder from GameNative/CustomGames."
	fi
	rm -f "$STATE"/java-installed*
}

# The Steam Deck updater's --phone-kit makes skilling-helper-phone.zip with the Jagex login in it.
# Pick it up from Download (zipped, or already unzipped), keep the login in Termux and delete the copy.
import_phone_kit() {
	local kit dir
	mkdir -p "$STATE"
	for kit in "$STORAGE"/Download/skilling-helper-phone*.zip; do
		[ -f "$kit" ] || continue
		local tmp; tmp="$(mktemp -d)"
		if unzip -q -o "$kit" home/.runelite/credentials.properties -d "$tmp" 2>/dev/null; then
			install -m 600 "$tmp/home/.runelite/credentials.properties" "$LOGIN"
			rm -f "$kit"
			say "Imported your Jagex login from $(basename "$kit") (and deleted the zip)."
		fi
		rm -rf "$tmp"
	done
	for dir in "$STORAGE"/Download/skilling-helper-phone*/; do
		[ -f "$dir/home/.runelite/credentials.properties" ] || continue
		install -m 600 "$dir/home/.runelite/credentials.properties" "$LOGIN"
		rm -rf "$dir"
		say "Imported your Jagex login from $(basename "$dir") (and deleted the folder)."
	done
	return 0
}

# release_asset JSON NAME OUT: download one asset of the release described in JSON
release_asset() {
	local id
	id="$(jq -r --arg n "$2" '.assets[] | select(.name == $n) | .id' "$1" | head -1)"
	[ -n "$id" ] || return 1
	gh_get "$API/repos/$REPO/releases/assets/$id" "$3" "application/octet-stream"
}

build_game() {
	local tmp; tmp="$(mktemp -d)"
	gh_get "$API/repos/$REPO/releases/latest" "$tmp/release.json" || fail "couldn't reach GitHub; try again"
	local tag; tag="$(jq -r .tag_name "$tmp/release.json")"
	release_asset "$tmp/release.json" SkillingHelper.exe "$tmp/SkillingHelper.exe" \
		|| fail "the newest release has no SkillingHelper.exe yet; try again in a few minutes"
	say "Downloading Skilling Helper $tag..."
	release_asset "$tmp/release.json" android-client.jar "$tmp/client.jar" \
		|| fail "the newest release has no android-client.jar yet; try again in a few minutes"
	say "Downloading Windows Java 21 (about 50 MB)..."
	curl -fsSL --retry 3 -o "$tmp/info.json" "$JRE_API"
	local url sum
	url="$(jq -r '.[0].binary.package.link' "$tmp/info.json")"
	sum="$(jq -r '.[0].binary.package.checksum' "$tmp/info.json")"
	curl -fSL --retry 3 -o "$tmp/jre.zip" "$url"
	echo "$sum  $tmp/jre.zip" | sha256sum -c --quiet - || fail "Java download is corrupt; try again"
	unzip -q "$tmp/jre.zip" -d "$tmp/x"

	rm -rf "$GAME"
	mkdir -p "$GAME/home/.runelite"
	mv "$tmp"/x/* "$GAME/jre"
	install -m 644 "$tmp/SkillingHelper.exe" "$GAME/SkillingHelper.exe"
	install -m 644 "$tmp/client.jar" "$GAME/client.jar"
	echo "$tag" > "$GAME/version.txt"
	[ -f "$LOGIN" ] && install -m 600 "$LOGIN" "$GAME/home/.runelite/credentials.properties"
	printf '{"appId":%s}\n' "$APP_ID" > "$GAME/.gamenative"
	# Keep this script current too
	release_asset "$tmp/release.json" skilling-helper-android.sh "$tmp/self.sh" && bash -n "$tmp/self.sh" \
		&& { mkdir -p "$(dirname "$SELF")"; install -m 755 "$tmp/self.sh" "$SELF"; } || true
	rm -rf "$tmp"
}

install_shortcuts() {
	mkdir -p "$(dirname "$SELF")" "$HOME/.shortcuts"
	if [ -f "$0" ] && [ "$(readlink -f "$0")" != "$(readlink -f "$SELF")" ]; then
		install -m 755 "$0" "$SELF"
	fi
	printf '#!/data/data/com.termux/files/usr/bin/bash\nexec "%s" --play\n' "$SELF" > "$HOME/.shortcuts/Play Skilling Helper"
	chmod 755 "$HOME/.shortcuts/Play Skilling Helper"
	rm -f "$HOME/.shortcuts/Update Skilling Helper" "$HOME/.local/bin/skilling-helper-android"
}

play() {
	say "Starting Skilling Helper in GameNative..."
	am start -n "$GN_PKG/.MainActivity" -a "$GN_PKG.LAUNCH_GAME" \
		--ei app_id "$APP_ID" --es game_source CUSTOM_GAME >/dev/null \
		|| fail "couldn't start GameNative. Open it and start SkillingHelper from its library."
}

MODE="${1:-}"
case "$MODE" in
	--play) play; exit 0 ;;
	--no-daily) command -v termux-job-scheduler >/dev/null && termux-job-scheduler --cancel --job-id 4207 || true; exit 0 ;;
	""|--daily|--reinstall-java) ;;
	*) echo "Unknown option $MODE"; exit 1 ;;
esac
# Older versions scheduled a daily update; the game updates itself now
command -v termux-job-scheduler >/dev/null 2>&1 && termux-job-scheduler --cancel --job-id 4207 >/dev/null 2>&1 || true

need_tools
need_storage
install_gamenative
install_shortcuts
clean_old
import_phone_kit
build_game
say ""
if [ ! -f "$LOGIN" ]; then
	say "Note: no Jagex login yet. On the Steam Deck double-click \"Skilling Helper phone kit\", copy"
	say "skilling-helper-phone.zip into this phone's Download folder and run skilling-helper-android again"
	say "(then import again). Without it the game asks you to log in and can't."
	say ""
fi
say "Now in GameNative:"
say "  1. Delete any older \"Skilling Helper\" or \"SkillingHelper\" game."
say "  2. Library > Add Custom Game > Continue > pick Download/SkillingHelper > Use this folder."
say "  3. Press Play. The first start picks SkillingHelper.exe by itself; turn on Touchscreen Mode in the"
say "     quick menu once. Updates download by themselves each time you start it."
