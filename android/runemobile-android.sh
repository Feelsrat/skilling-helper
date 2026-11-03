#!/data/data/com.termux/files/usr/bin/bash
# Installs or updates "RuneMobile SH": RuneMobile (github.com/audaro/RuneMobile) built with the Skilling Helper
# plugin, from the rolling "runemobile" prerelease of github.com/Feelsrat/skilling-helper. Run it in Termux:
#
#   curl -fsSL https://github.com/Feelsrat/skilling-helper/releases/download/runemobile/runemobile-android.sh | bash
#
# The app keeps the plugin itself up to date at every start. Run this again (or the "Update RuneMobile SH" widget
# shortcut) after an OSRS weekly update, when the app needs a newer build.
#
# RuneMobile is unofficial and not on Jagex's approved client list: use it at your own risk, ideally on a spare
# account.
set -euo pipefail

BASE="https://github.com/Feelsrat/skilling-helper/releases/download/runemobile"
STATE="$HOME/.config/skilling-helper-android"
SELF="${PREFIX:-$HOME/.local}/bin/runemobile-sh"
PKG="dev.audaro.runemobile.skillinghelper"

say() { printf '\n\033[1;35m%s\033[0m\n' "$*"; }

for t in curl; do
	command -v "$t" >/dev/null || { say "Installing $t"; pkg install -y "$t"; }
done
mkdir -p "$STATE" "$(dirname "$SELF")"

# Keep a copy of this script as a command and a widget shortcut.
curl -fsSL "$BASE/runemobile-android.sh" -o "$SELF.tmp" && chmod 755 "$SELF.tmp" && mv "$SELF.tmp" "$SELF" || rm -f "$SELF.tmp"
mkdir -p "$HOME/.shortcuts"
printf '#!/data/data/com.termux/files/usr/bin/bash\nexec %s\n' "$SELF" > "$HOME/.shortcuts/Update RuneMobile SH"
chmod 755 "$HOME/.shortcuts/Update RuneMobile SH"

latest=$(curl -fsSL "$BASE/runemobile-build.txt" | head -1)
[ -n "$latest" ] || { say "Could not reach GitHub. Check your connection and try again."; exit 1; }
have=$(cat "$STATE/runemobile-build.txt" 2>/dev/null || true)
installed=no
if pm path "$PKG" >/dev/null 2>&1; then installed=yes; fi

if [ "$installed" = yes ] && [ "$latest" = "$have" ]; then
	say "RuneMobile SH is up to date ($latest)."
	exit 0
fi

say "Downloading RuneMobile SH ($latest)"
# termux-open needs the APK in shared storage for the Android installer to read it.
if [ ! -d "$HOME/storage/downloads" ]; then
	termux-setup-storage || true
	sleep 2 2>/dev/null || true
fi
APK="$HOME/storage/downloads/RuneMobile-SH.apk"
[ -d "$HOME/storage/downloads" ] || APK="/storage/emulated/0/Download/RuneMobile-SH.apk"
curl -fL --retry 3 "$BASE/RuneMobile-SH.apk" -o "$APK"
echo "$latest" > "$STATE/runemobile-build.txt"

say "Opening the installer. Tap Install (allow Termux to install apps if Android asks)."
# Android only lets Termux pop up the installer when Termux may "Display over other apps"
# (Settings > Apps > Termux). Without it nothing appears, so also say where the APK is.
termux-open --content-type application/vnd.android.package-archive "$APK" 2>/dev/null || true
cat <<EOF2
If no installer appeared: open Download/RuneMobile-SH.apk in your Files app and tap it, or allow
Termux to "Display over other apps" (Settings > Apps > Termux) and run runemobile-sh again.
EOF2
cat <<'EOF'

After it installs:
  1. Open "RuneMobile SH" and log in with your Jagex account.
  2. Skilling Helper and PvM Progression are loaded automatically. Find them in the plugin list.
  3. You can delete Download/RuneMobile-SH.apk afterwards.

To update later, run "runemobile-sh" in Termux or tap the "Update RuneMobile SH" widget.
EOF
