#!/usr/bin/env bash
# Skilling Helper updater for Steam Deck (and any Linux Steam install).
#
# Finds the Steam shortcut that starts your RuneLite dev instance, works out which
# Skilling Helper jar it uses, and swaps in the newest release from GitHub.
#
#   skilling-helper-update.sh                 update now (asks which shortcut if there are several)
#   skilling-helper-update.sh --install       copy this updater to ~/.local/bin and add a
#                                             "Update Skilling Helper" launcher to the Desktop and menu
#   skilling-helper-update.sh --phone-kit     put skilling-helper-phone.zip on the Desktop: your Jagex login and the
#                                             Android installer, to copy to the phone (Download folder)
#   skilling-helper-update.sh --auto %command% <the shortcut's usual launch options>
#                                             for Steam launch options: update (with a small window saying
#                                             what happened), then start the game
#
# Run by hand, it also offers (once) to put "--auto %command%" into the shortcut's launch options for you, so the
# game updates itself on every start; --enable-auto does that without asking.
# Other options: --shortcut NAME (pick a shortcut by name), --list, --force (reinstall even if up to date).
# It only reads Steam's shortcut list and replaces jar files; it never touches the game itself.
# Old jars are kept next to the new one as *.bak.

set -u

REPO="${SKILLING_HELPER_REPO:-Feelsrat/skilling-helper}"
INSTALL_DIR="$HOME/.local/bin"
INSTALL_PATH="$INSTALL_DIR/skilling-helper-update"
SELF="$(readlink -f "${BASH_SOURCE[0]}")"

if ! command -v python3 >/dev/null 2>&1; then
	echo "python3 is needed (it ships with SteamOS)." >&2
	[ "${1:-}" = "--auto" ] && { shift; exec "$@"; }
	exit 1
fi

UPDATER=$(cat <<'PY'
import glob
import hashlib
import json
import os
import re
import shlex
import shutil
import struct
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import zipfile

REPO = os.environ.get("SKILLING_HELPER_REPO", "Feelsrat/skilling-helper")
API = os.environ.get("SKILLING_HELPER_API", "https://api.github.com")
HOME = os.path.expanduser("~")
STATE_DIR = os.path.join(HOME, ".config", "skilling-helper-updater")
STATE_FILE = os.path.join(STATE_DIR, "installed.json")
TOKEN_FILE = os.path.join(STATE_DIR, "token")
PLUGIN_CLASS = "com/skillinghelper/SkillingHelperPlugin.class"
SCRIPT_ASSET = "skilling-helper-update.sh"
RUNELITE_DIRS = [
    os.path.join(HOME, ".runelite"),
    os.path.join(HOME, ".var", "app", "net.runelite.RuneLite", ".runelite"),
    os.path.join(HOME, ".var", "app", "net.runelite.RuneLite", "data", ".runelite"),
]

args = sys.argv[1:]
auto = bool(args) and args[0] == "--auto"
if auto:
    args = args[1:]
force = "--force" in args
list_only = "--list" in args
enable_auto = "--enable-auto" in args
wanted = None
if "--shortcut" in args:
    i = args.index("--shortcut")
    wanted = args[i + 1] if i + 1 < len(args) else ""


def say(msg=""):
    if auto:
        msg = time.strftime("%Y-%m-%d %H:%M:%S ") + msg
    print(msg, flush=True)


def fail(msg):
    say(msg)
    sys.exit(1)


# --- Steam's binary shortcuts.vdf -------------------------------------------------

def read_vdf(data):
    pos = 0

    def cstr():
        nonlocal pos
        end = data.index(b"\x00", pos)
        s = data[pos:end].decode("utf-8", "replace")
        pos = end + 1
        return s

    def read_map():
        nonlocal pos
        out = {}
        while pos < len(data):
            kind = data[pos]
            pos += 1
            if kind == 0x08:
                return out
            key = cstr().lower()
            if kind == 0x00:
                out[key] = read_map()
            elif kind == 0x01:
                out[key] = cstr()
            elif kind == 0x02:
                out[key] = struct.unpack_from("<i", data, pos)[0]
                pos += 4
            elif kind == 0x07:
                out[key] = struct.unpack_from("<Q", data, pos)[0]
                pos += 8
            else:
                raise ValueError("unknown VDF field type %d" % kind)
        return out

    root = read_map()
    shortcuts = root.get("shortcuts", {})
    return [v for v in shortcuts.values() if isinstance(v, dict)]


# Lossless copy of the same format, for writing launch options back: [(type, key bytes, value)]
def read_vdf_raw(data):
    pos = 0

    def cstr():
        nonlocal pos
        end = data.index(b"\x00", pos)
        s = data[pos:end]
        pos = end + 1
        return s

    def read_list():
        nonlocal pos
        out = []
        while pos < len(data):
            kind = data[pos]
            pos += 1
            if kind == 0x08:
                return out
            key = cstr()
            if kind == 0x00:
                out.append((kind, key, read_list()))
            elif kind == 0x01:
                out.append((kind, key, cstr()))
            elif kind == 0x02:
                out.append((kind, key, data[pos:pos + 4]))
                pos += 4
            elif kind == 0x07:
                out.append((kind, key, data[pos:pos + 8]))
                pos += 8
            else:
                raise ValueError("unknown VDF field type %d" % kind)
        return out

    return read_list()


def write_vdf_raw(items):
    out = bytearray()
    for kind, key, value in items:
        out.append(kind)
        out += key + b"\x00"
        if kind == 0x00:
            out += write_vdf_raw(value)
        elif kind == 0x01:
            out += value + b"\x00"
        else:
            out += value
    out.append(0x08)
    return bytes(out)


def steam_roots():
    roots = [
        os.path.join(HOME, ".local", "share", "Steam"),
        os.path.join(HOME, ".steam", "steam"),
        os.path.join(HOME, ".steam", "root"),
        os.path.join(HOME, ".var", "app", "com.valvesoftware.Steam", ".local", "share", "Steam"),
    ]
    seen, out = set(), []
    for r in roots:
        real = os.path.realpath(r)
        if os.path.isdir(real) and real not in seen:
            seen.add(real)
            out.append(real)
    return out


def load_shortcuts():
    found = []
    for root in steam_roots():
        for path in glob.glob(os.path.join(root, "userdata", "*", "config", "shortcuts.vdf")):
            try:
                with open(path, "rb") as f:
                    entries = read_vdf(f.read())
            except (OSError, ValueError, IndexError, struct.error) as e:
                say("Couldn't read %s: %s" % (path, e))
                continue
            for e in entries:
                found.append({
                    "file": path,
                    "name": e.get("appname", "") or "(no name)",
                    "exe": e.get("exe", ""),
                    "startdir": e.get("startdir", "").strip('"'),
                    "options": e.get("launchoptions", ""),
                })
    return found


# --- Working out which jar a shortcut uses ----------------------------------------

def words(text):
    try:
        parts = shlex.split(text)
    except ValueError:
        parts = text.split()
    out = []
    for p in parts:
        # "-cp a.jar:b.jar", "--foo=/x/y.jar" and quoted exes all end up as separate paths
        for piece in re.split(r"[:=]", p):
            piece = piece.strip("\"'")
            if piece:
                out.append(piece)
    return out


def expand(path, startdir):
    path = os.path.expandvars(os.path.expanduser(path))
    if not os.path.isabs(path) and startdir:
        path = os.path.join(os.path.expanduser(startdir), path)
    return os.path.normpath(path)


def script_text(path):
    # A shortcut that runs a launch script: look inside it for the jar too
    try:
        if os.path.isfile(path) and os.path.getsize(path) < 256 * 1024 and not zipfile.is_zipfile(path):
            with open(path, "r", errors="replace") as f:
                text = f.read()
            if "\x00" not in text:
                return text
    except OSError:
        pass
    return ""


def describe(sc):
    """Everything the shortcut runs, as (paths, all text) for matching."""
    text = " ".join([sc["exe"], sc["options"]])
    paths = [expand(w, sc["startdir"]) for w in words(text)]
    extra = []
    for p in paths:
        # the updater itself (put in the launch options by --auto) is long and never names the jar
        if os.path.basename(p).startswith("skilling-helper-update"):
            continue
        body = script_text(p)
        if body:
            extra.append(body)
            for line in body.splitlines():
                if line.lstrip().startswith("#"):
                    continue
                paths.extend(expand(w, os.path.dirname(p)) for w in words(line))
    return paths, " ".join([sc["name"], text] + extra)


def is_ours(name):
    return name.lower().startswith("skilling-helper") and name.lower().endswith(".jar")


def find_targets(paths, text):
    """[(jar path, 'all' | 'plugin')] that this launch uses."""
    targets = []
    for p in paths:
        name = os.path.basename(p)
        if is_ours(name) and p not in [t[0] for t in targets]:
            targets.append((p, "all" if name.lower().endswith("-all.jar") else "plugin"))
    if targets:
        return targets
    # Normal RuneLite (AppImage, flatpak or launcher jar) in developer mode: the plugin is sideloaded
    if "runelite" in text.lower():
        for d in RUNELITE_DIRS:
            for jar in sorted(glob.glob(os.path.join(d, "sideloaded-plugins", "*.jar"))):
                if is_ours(os.path.basename(jar)):
                    targets.append((jar, "plugin"))
    return targets


def candidates(shortcuts):
    out = []
    for sc in shortcuts:
        paths, text = describe(sc)
        targets = find_targets(paths, text)
        lowered = text.lower()
        if targets or "skilling-helper" in lowered or "runelite" in lowered:
            out.append((sc, targets))
    return out


# --- GitHub release ---------------------------------------------------------------

entered_token = None


def token():
    if entered_token:
        return entered_token
    t = os.environ.get("GITHUB_TOKEN", "").strip()
    if not t and os.path.isfile(TOKEN_FILE):
        with open(TOKEN_FILE) as f:
            t = f.read().strip()
    return t


class DropAuthOnRedirect(urllib.request.HTTPRedirectHandler):
    # Release downloads redirect to a storage host that rejects the GitHub token
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        new = super().redirect_request(req, fp, code, msg, headers, newurl)
        if new is not None and urllib.parse.urlparse(newurl).hostname != urllib.parse.urlparse(req.full_url).hostname:
            new.remove_header("Authorization")
        return new


opener = urllib.request.build_opener(DropAuthOnRedirect)


def request(url, accept="application/vnd.github+json"):
    headers = {"Accept": accept, "User-Agent": "skilling-helper-updater"}
    if token():
        headers["Authorization"] = "Bearer " + token()
    return opener.open(urllib.request.Request(url, headers=headers), timeout=30)


def ask_for_token():
    global entered_token
    say("The skilling-helper repo is private, so GitHub needs a token to let this download releases.")
    say("Make one at https://github.com/settings/personal-access-tokens/new :")
    say("  Repository access: Only select repositories > skilling-helper")
    say("  Permissions: Contents > Read-only")
    t = input("Paste the token here (or just press Enter to cancel): ").strip()
    if not t:
        sys.exit(1)
    os.makedirs(STATE_DIR, exist_ok=True)
    with open(os.open(TOKEN_FILE, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600), "w") as f:
        f.write(t + "\n")
    entered_token = t
    say("Saved to %s" % TOKEN_FILE)


def latest_release():
    while True:
        try:
            with request("%s/repos/%s/releases/latest" % (API, REPO)) as r:
                return json.load(r)
        except urllib.error.HTTPError as e:
            if e.code in (401, 403, 404) and not auto:
                if token():
                    say("GitHub refused the saved token (%s %s)." % (e.code, e.reason))
                ask_for_token()
                continue
            if e.code in (401, 403, 404):
                fail("GitHub refused the request (%s). Run the updater once by hand to set up a token." % e.code)
            fail("GitHub said %s %s." % (e.code, e.reason))
        except (urllib.error.URLError, OSError) as e:
            fail("Couldn't reach GitHub (%s). Are you online?" % e)


def asset_for(release, kind):
    for a in release.get("assets", []):
        name = a["name"]
        if not is_ours(name):
            continue
        if (kind == "all") == name.endswith("-all.jar"):
            return a
    return None


def download(asset, dest):
    with request(asset["url"], accept="application/octet-stream") as r, open(dest, "wb") as f:
        shutil.copyfileobj(r, f)


# --- Swapping the jar -------------------------------------------------------------

def load_state():
    try:
        with open(STATE_FILE) as f:
            return json.load(f)
    except (OSError, ValueError):
        return {}


def save_state(state):
    os.makedirs(STATE_DIR, exist_ok=True)
    with open(STATE_FILE, "w") as f:
        json.dump(state, f, indent=2)


def jar_in_use(path):
    real = os.path.realpath(path)
    sideloaded = "sideloaded-plugins" in real
    me = {str(os.getpid()), str(os.getppid())}
    for cmd in glob.glob("/proc/[0-9]*/cmdline"):
        if cmd.split("/")[2] in me:
            continue
        try:
            with open(cmd, "rb") as f:
                parts = [p.decode("utf-8", "replace") for p in f.read().split(b"\x00")]
        except OSError:
            continue
        for p in parts:
            if p.endswith(".jar") and os.path.basename(p) == os.path.basename(real):
                return True
            # a running RuneLite holds its sideloaded plugins open
            if sideloaded and ("net.runelite.client" in p or "net.runelite.launcher" in p
                               or p.endswith(("RuneLite.jar", "RuneLite.AppImage"))):
                return True
    return False


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def update_jar(path, kind, release, state):
    tag = release["tag_name"]
    asset = asset_for(release, kind)
    if asset is None:
        say("  The %s release has no %s jar, skipping %s" % (tag, "standalone" if kind == "all" else "plugin", path))
        return False
    known = state.get(path, {})
    if not force and known.get("tag") == tag and os.path.isfile(path) and known.get("sha256") == sha256(path):
        say("  %s is already %s" % (path, tag))
        return False

    # with --auto, Steam runs us before the game starts
    while not auto and jar_in_use(path):
        input("  RuneLite is still running. Close it, then press Enter (Ctrl+C to cancel)... ")

    tmp = path + ".download"
    say("  Downloading %s ..." % asset["name"])
    try:
        download(asset, tmp)
        with zipfile.ZipFile(tmp) as z:
            if PLUGIN_CLASS not in z.namelist():
                raise ValueError("the download doesn't contain the plugin")
    except Exception as e:
        if os.path.exists(tmp):
            os.remove(tmp)
        fail("  Download failed (%s). Nothing was changed." % e)

    if os.path.isfile(path):
        shutil.copy2(path, path + ".bak")
    os.replace(tmp, path)
    state[path] = {"tag": tag, "sha256": sha256(path), "asset": asset["name"], "updated": time.strftime("%Y-%m-%d %H:%M")}
    say("  Updated %s to %s (old one kept as %s.bak)" % (path, tag, os.path.basename(path)))
    return True


def update_self(release):
    # Keep the installed copy of this updater current too
    installed = os.environ.get("SKILLING_HELPER_INSTALLED", "")
    if not installed or os.environ.get("SKILLING_HELPER_SELF") != installed:
        return
    asset = next((a for a in release.get("assets", []) if a["name"] == SCRIPT_ASSET), None)
    if asset is None:
        return
    tmp = installed + ".download"
    try:
        download(asset, tmp)
        with open(tmp, "rb") as f:
            new = f.read()
        if not new.startswith(b"#!") or new == open(installed, "rb").read():
            os.remove(tmp)
            return
        os.chmod(tmp, 0o755)
        os.replace(tmp, installed)
        say("Updated the updater itself.")
    except Exception:
        if os.path.exists(tmp):
            os.remove(tmp)


# --- Updating on every launch (Steam launch options) -----------------------------

def has_auto(sc):
    return "skilling-helper-update" in sc.get("options", "")


def with_auto(options, installed):
    hook = shlex.quote(installed) + " --auto %command%"
    if "%command%" in options:
        return options.replace("%command%", hook, 1)
    # Without %command%, Steam adds the options after the game's command, so they stay after it here too
    return (hook + " " + options).strip()


def steam_pids():
    pids = []
    for comm in glob.glob("/proc/[0-9]*/comm"):
        try:
            with open(comm) as f:
                if f.read().strip() == "steam":
                    pids.append(comm.split("/")[2])
        except OSError:
            pass
    return pids


def set_launch_options(sc, installed):
    """Rewrite this shortcut's launch options in shortcuts.vdf. Steam must be closed (it rewrites the file on exit)."""
    path = sc["file"]
    with open(path, "rb") as f:
        data = f.read()
    root = read_vdf_raw(data)
    done = False
    for kind, key, value in root:
        if kind != 0x00 or key.lower() != b"shortcuts":
            continue
        for k2, _, entry in value:
            if k2 != 0x00:
                continue
            fields = {k.lower(): (i, v) for i, (t, k, v) in enumerate(entry) if t == 0x01}
            name = fields.get(b"appname", (None, b""))[1].decode("utf-8", "replace")
            exe = fields.get(b"exe", (None, b""))[1].decode("utf-8", "replace")
            if name != sc["name"] or exe != sc["exe"]:
                continue
            old = fields.get(b"launchoptions", (None, b""))[1].decode("utf-8", "replace")
            if "skilling-helper-update" in old:
                return True
            new = with_auto(old, installed).encode("utf-8")
            if b"launchoptions" in fields:
                i = fields[b"launchoptions"][0]
                entry[i] = (0x01, entry[i][1], new)
            else:
                entry.append((0x01, b"LaunchOptions", new))
            done = True
    if not done:
        return False
    shutil.copy2(path, path + ".bak")
    tmp = path + ".tmp"
    with open(tmp, "wb") as f:
        f.write(write_vdf_raw(root))
    os.replace(tmp, path)
    return True


def turn_on_auto(chosen, ask):
    installed = os.environ.get("SKILLING_HELPER_INSTALLED", "")
    if not installed or not os.path.isfile(installed):
        return
    state = load_state()
    declined = state.get("auto_declined", [])
    todo = [sc for sc, _ in chosen if sc.get("file") and not has_auto(sc) and (not ask or sc["name"] not in declined)]
    if not todo:
        if not ask:
            say("Updating on every start is already on.")
        return
    names = ", ".join(sc["name"] for sc in todo)
    if ask:
        say()
        say("Update automatically every time you start \"%s\" from Steam (Game Mode too)?" % names)
        say("This adds the updater to the shortcut's launch options. Steam closes for a few seconds to save it.")
        if input("[Y/n] ").strip().lower() in ("n", "no"):
            state["auto_declined"] = sorted(set(declined) | {sc["name"] for sc in todo})
            save_state(state)
            say("OK. Run skilling-helper-update --enable-auto if you change your mind.")
            return
    was_running = bool(steam_pids())
    if was_running:
        say("Closing Steam for a moment...")
        try:
            subprocess.run(["steam", "-shutdown"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=30)
        except (OSError, subprocess.TimeoutExpired):
            pass
        for _ in range(60):
            if not steam_pids():
                break
            time.sleep(0.5)
        if steam_pids():
            say("Steam didn't close, so nothing was changed. Close Steam and run skilling-helper-update --enable-auto.")
            return
    for sc in todo:
        try:
            if set_launch_options(sc, installed):
                say("\"%s\" now updates Skilling Helper every time it starts." % sc["name"])
            else:
                say("Couldn't find \"%s\" in %s any more." % (sc["name"], sc["file"]))
        except (OSError, ValueError, IndexError) as e:
            say("Couldn't change \"%s\" (%s). Its launch options can be set by hand to: %s"
                % (sc["name"], e, with_auto(sc["options"], installed)))
    if was_running:
        say("Starting Steam again.")
        try:
            subprocess.Popen(["steam"], stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL,
                             stderr=subprocess.DEVNULL, start_new_session=True)
        except OSError:
            say("Couldn't start Steam; open it yourself.")


def pick(cands):
    if wanted is not None:
        matches = [c for c in cands if wanted.lower() in c[0]["name"].lower()]
        if not matches:
            fail("No Steam shortcut called \"%s\". Run with --list to see them." % wanted)
        cands = matches
    usable = [c for c in cands if c[1]]
    if len(usable) == 1 or (usable and auto):
        return usable if auto else usable[:1]
    if not usable:
        return []
    say("More than one Steam shortcut uses Skilling Helper:")
    for i, (sc, targets) in enumerate(usable, 1):
        say("  %d) %s  ->  %s" % (i, sc["name"], ", ".join(t[0] for t in targets)))
    say("  a) all of them")
    while True:
        choice = input("Which one? ").strip().lower()
        if choice == "a":
            return usable
        if choice.isdigit() and 1 <= int(choice) <= len(usable):
            return [usable[int(choice) - 1]]


def main():
    if auto:
        # Steam hands us the shortcut's own command line
        paths, text = describe({"name": "", "exe": shlex.join(args), "startdir": os.getcwd(), "options": ""})
        chosen = [({"name": "this Steam shortcut"}, find_targets(paths, text))]
        if not chosen[0][1]:
            say("No Skilling Helper jar in: %s" % shlex.join(args))
            return
    else:
        shortcuts = load_shortcuts()
        if not steam_roots():
            fail("Couldn't find Steam in your home folder.")
        if list_only:
            for sc in shortcuts:
                say("%s\n    exe: %s\n    start in: %s\n    launch options: %s"
                    % (sc["name"], sc["exe"], sc["startdir"], sc["options"]))
            return
        cands = candidates(shortcuts)
        chosen = pick(cands)
        if not chosen:
            say("Couldn't find a Steam shortcut that starts Skilling Helper.")
            if cands:
                say("These look like RuneLite, but no skilling-helper jar was found for them:")
                for sc, _ in cands:
                    say("  %s: %s %s" % (sc["name"], sc["exe"], sc["options"]))
                say("If you sideload the plugin, put the jar in ~/.runelite/sideloaded-plugins once and run this again.")
            else:
                say("Run with --list to see your non-Steam shortcuts.")
            sys.exit(1)

    if enable_auto:
        turn_on_auto(chosen, ask=False)
        return

    release = latest_release()
    say("Latest release: %s" % release["tag_name"])
    state = load_state()
    changed = False
    for sc, targets in chosen:
        say("%s:" % sc["name"])
        for path, kind in targets:
            changed = update_jar(path, kind, release, state) or changed
    save_state(state)
    update_self(release)
    if changed:
        say("Done. Start the game from Steam as usual.")
    else:
        say("Nothing to update.")
    if not auto:
        turn_on_auto(chosen, ask=True)


try:
    main()
except (KeyboardInterrupt, EOFError):
    print()
    sys.exit(130)
PY
)

desktop_terminal() {
	if command -v konsole >/dev/null 2>&1; then echo "konsole --hold -e"; else echo "xterm -hold -e"; fi
}

install_phone_kit_launcher() {
	mkdir -p "$HOME/.local/share/applications"
	local entry
	entry="[Desktop Entry]
Type=Application
Name=Skilling Helper phone kit
Comment=Zip your Jagex login and the Android installer to copy to your phone
Exec=$(desktop_terminal) \"$INSTALL_PATH\" --phone-kit
Icon=smartphone
Terminal=false
Categories=Game;"
	printf '%s\n' "$entry" > "$HOME/.local/share/applications/skilling-helper-phone-kit.desktop"
	if [ -d "$HOME/Desktop" ]; then
		printf '%s\n' "$entry" > "$HOME/Desktop/skilling-helper-phone-kit.desktop"
		chmod +x "$HOME/Desktop/skilling-helper-phone-kit.desktop"
	fi
}

# Builds ~/Desktop/skilling-helper-phone.zip: the Jagex login file the dev client needs, plus the Android
# installer. The Android installer picks the zip up from the phone's Download folder by itself.
make_phone_kit() {
	local creds="$HOME/.runelite/credentials.properties"
	if [ ! -f "$creds" ]; then
		echo "No saved Jagex login found at $creds."
		echo "Make one first: https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts"
		echo "(start RuneLite once from the Jagex Launcher with --insecure-write-credentials), then run this again."
		return 1
	fi
	local out_dir="$HOME/Desktop"
	[ -d "$out_dir" ] || out_dir="$HOME"
	local out="$out_dir/skilling-helper-phone.zip"
	local tmp; tmp="$(mktemp -d)"
	mkdir -p "$tmp/home/.runelite"
	cp "$creds" "$tmp/home/.runelite/"
	if ! curl -fsSL --retry 2 -o "$tmp/skilling-helper-android.sh" \
		"https://github.com/$REPO/releases/latest/download/skilling-helper-android.sh"; then
		echo "Couldn't download the Android installer; the zip only has your login. Is the Deck online?"
		rm -f "$tmp/skilling-helper-android.sh"
	fi
	cat > "$tmp/README.txt" <<'EOF'
Skilling Helper phone kit
=========================
Copy skilling-helper-phone.zip (don't unzip it) into the phone's Download folder, then in Termux run:

  curl -fLo sh-android.sh https://github.com/Feelsrat/skilling-helper/releases/latest/download/skilling-helper-android.sh && bash sh-android.sh

(or, if you unzipped it: bash /sdcard/Download/skilling-helper-phone/skilling-helper-android.sh)

It finds this zip, puts your Jagex login in place and deletes the zip.
The zip holds your Jagex login: don't share it, and delete any other copies once it's on the phone.
EOF
	rm -f "$out"
	(cd "$tmp" && umask 077 && python3 -c 'import sys,zipfile,os
z=zipfile.ZipFile(sys.argv[1],"w",zipfile.ZIP_DEFLATED)
for root,dirs,files in os.walk("."):
    for f in files:
        p=os.path.join(root,f)
        z.write(p,os.path.relpath(p,"."))
z.close()' "$out")
	rm -rf "$tmp"
	echo "Made $out"
	echo
	echo "Copy it into the phone's Download folder (USB cable, KDE Connect, or adb push \"$out\" /sdcard/Download/)."
	echo "Then run the Android installer in Termux; it uses the zip by itself. The zip holds your login,"
	echo "so delete this copy once it's on the phone."
	if command -v adb >/dev/null 2>&1 && adb get-state >/dev/null 2>&1; then
		echo
		read -r -p "A phone is connected over adb. Copy it there now? [Y/n] " a
		if [ "${a:-y}" != n ] && [ "${a:-y}" != N ]; then
			adb push "$out" /sdcard/Download/ && echo "Copied to the phone's Download folder."
		fi
	fi
}

install_launcher() {
	mkdir -p "$INSTALL_DIR" "$HOME/.local/share/applications"
	if [ "$SELF" != "$INSTALL_PATH" ]; then
		cp "$SELF" "$INSTALL_PATH"
	fi
	chmod +x "$INSTALL_PATH"

	local terminal="konsole --hold -e"
	command -v konsole >/dev/null 2>&1 || terminal="xterm -hold -e"
	local entry
	entry="[Desktop Entry]
Type=Application
Name=Update Skilling Helper
Comment=Download the newest Skilling Helper into your RuneLite Steam shortcut
Exec=$terminal \"$INSTALL_PATH\"
Icon=system-software-update
Terminal=false
Categories=Game;"
	printf '%s\n' "$entry" > "$HOME/.local/share/applications/skilling-helper-update.desktop"
	if [ -d "$HOME/Desktop" ]; then
		printf '%s\n' "$entry" > "$HOME/Desktop/skilling-helper-update.desktop"
		chmod +x "$HOME/Desktop/skilling-helper-update.desktop"
	fi
	install_phone_kit_launcher
	echo "Installed to $INSTALL_PATH"
	echo "Double-click \"Update Skilling Helper\" on the Desktop (or find it in the app menu) to update by hand."
	echo
	# Make the Steam shortcut update on every start (asks before closing Steam to save it)
	SKILLING_HELPER_SELF="$INSTALL_PATH" SKILLING_HELPER_INSTALLED="$INSTALL_PATH" python3 -c "$UPDATER" || true
}

if [ "${1:-}" = "--install" ]; then
	install_launcher
	exit 0
fi

if [ "${1:-}" = "--phone-kit" ]; then
	make_phone_kit
	exit $?
fi

# Installed copies made before the phone kit existed: add its Desktop icon on the next run
if [ "$SELF" = "$INSTALL_PATH" ] && [ ! -f "$HOME/.local/share/applications/skilling-helper-phone-kit.desktop" ]; then
	install_phone_kit_launcher
	echo "Added a \"Skilling Helper phone kit\" icon to the Desktop."
fi

# --- Feedback for --auto: a small window while it checks, then what happened -------------------
# Shown with zenity (part of SteamOS; in Game Mode it appears over the game like any window the game opens),
# else a desktop notification. SKILLING_HELPER_QUIET=1 in the launch options turns it off.

feedback_ui() {
	[ "${SKILLING_HELPER_QUIET:-0}" = "1" ] && return 1
	[ -n "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ] || return 1
	return 0
}

# Steam's overlay library is preloaded into the game's processes; keep it out of the dialogs
ui() { env -u LD_PRELOAD "$@" >/dev/null 2>&1; }

auto_progress() {
	if feedback_ui && command -v zenity >/dev/null 2>&1; then
		env -u LD_PRELOAD zenity --progress --pulsate --auto-close --no-cancel --width=360 \
			--title="Skilling Helper" --text="Checking for Skilling Helper updates..." >/dev/null 2>&1
		cat >/dev/null # if zenity couldn't open, still let the update finish
	else
		cat >/dev/null
	fi
}

auto_result() {
	local rc="$1" out="$2" tag msg kind="info" secs=4
	tag="$(sed -n 's/^.*Latest release: //p' "$out" | tail -1)"
	if [ "$rc" = "124" ]; then
		kind="warning"; secs=8
		msg="Update check timed out (GitHub slow or offline). Starting the version you have."
	elif [ "$rc" != "0" ]; then
		kind="warning"; secs=8
		msg="Update failed: $(grep -v '^[[:space:]]*$' "$out" | tail -1 | sed 's/^[0-9-]* [0-9:]* *//')
Starting the version you have."
	elif grep -q " Updated .* to v" "$out"; then
		msg="Updated Skilling Helper to ${tag:-the newest version}."
	elif grep -q "No Skilling Helper jar in" "$out"; then
		kind="warning"; secs=8
		msg="Couldn't find a Skilling Helper jar in this shortcut, so nothing was updated."
	else
		msg="Skilling Helper is up to date${tag:+ ($tag)}."
	fi
	feedback_ui || return 0
	if command -v zenity >/dev/null 2>&1; then
		# runs until closed or the timeout (zenity exits 5 then); the game starts right after
		ui zenity "--$kind" --no-wrap --timeout="$secs" --title="Skilling Helper" --text="$msg"
		return 0
	fi
	command -v notify-send >/dev/null 2>&1 && ui notify-send -a "Skilling Helper" "Skilling Helper" "$msg"
	return 0
}

if [ "${1:-}" = "--auto" ]; then
	shift
	[ $# -gt 0 ] || { echo "--auto needs the game command after it (use --auto %command% in Steam)." >&2; exit 1; }
	# Never stop the game from starting: a failed or slow update just launches what's there.
	mkdir -p "$HOME/.cache"
	out="$(mktemp)"
	{
		SKILLING_HELPER_SELF="$SELF" SKILLING_HELPER_INSTALLED="$INSTALL_PATH" \
			timeout 60 python3 -c "$UPDATER" --auto "$@" </dev/null >"$out" 2>&1
		echo $? >"$out.rc"
		echo 100
	} | auto_progress
	cat "$out" >>"$HOME/.cache/skilling-helper-update.log"
	auto_result "$(cat "$out.rc" 2>/dev/null || echo 1)" "$out"
	rm -f "$out" "$out.rc"
	exec "$@"
fi

SKILLING_HELPER_SELF="$SELF" SKILLING_HELPER_INSTALLED="$INSTALL_PATH" python3 -c "$UPDATER" "$@"
