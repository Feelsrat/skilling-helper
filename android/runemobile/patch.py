#!/usr/bin/env python3
"""Turn a RuneMobile checkout (github.com/audaro/RuneMobile) into the Skilling Helper build.

Usage: patch.py <RuneMobile checkout>

Changes, each one checked so an upstream change fails the build loudly instead of shipping a half-patched app:
  1. RuneLite loads ~/.runelite/sideloaded-plugins without --developer-mode (RuneMobile never passes it).
  2. Before RuneLite starts, the boot class downloads the latest Skilling Helper plugin jar from this repo's
     GitHub release into that folder (keeps the old jar when offline).
  3. The plugin panel is a desktop-style sidebar on the right edge (SidePanel.kt.txt) instead of a bottom sheet.
  4. A separate app id and name, so it installs next to the official RuneMobile instead of replacing it.
"""
import pathlib
import sys

root = pathlib.Path(sys.argv[1])


def patch(rel, old, new):
	p = root / rel
	s = p.read_text()
	if s.count(old) != 1:
		sys.exit(f"patch.py: expected exactly one match in {rel} for:\n{old}")
	p.write_text(s.replace(old, new))
	print(f"patched {rel}")


patch("runelite-client/src/main/java/net/runelite/client/plugins/PluginManager.java",
	"""	public void loadSideLoadPlugins()
	{
		if (!developerMode)
		{
			return;
		}
""",
	"""	public void loadSideLoadPlugins()
	{
		// Skilling Helper build: always load sideloaded plugins (RuneMobile never starts in developer mode).
""")

hook = (pathlib.Path(__file__).parent / "SideloadHook.java").read_text()
patch("ios/dev-tools/awtsmoke/DeviceHeadlessBoot.java",
	"public class DeviceHeadlessBoot {\n",
	"public class DeviceHeadlessBoot {\n" + hook)
patch("ios/dev-tools/awtsmoke/DeviceHeadlessBoot.java",
	"        net.runelite.client.headless.RuneLiteHeadless.main(rlArgs);",
	"        skillingHelperSideload();\n        net.runelite.client.headless.RuneLiteHeadless.main(rlArgs);")

patch("android/app/build.gradle.kts",
	'applicationId = "dev.audaro.runemobile"',
	'applicationId = "dev.audaro.runemobile.skillinghelper"')
patch("android/app/src/main/res/values/strings.xml",
	'<string name="app_name">RuneMobile</string>',
	'<string name="app_name">RuneMobile SH</string>')

# 3. Right-hand sidebar instead of the bottom sheet: replace the whole SheetScaffold function.
pp = root / "android/app/src/main/java/dev/audaro/runemobile/PluginPanel.kt"
s = pp.read_text()
start = s.find("@Composable\nprivate fun SheetScaffold(")
end = s.find("/**\n * §4 increment 2", start)
if start < 0 or end < 0:
	sys.exit("patch.py: SheetScaffold not found in PluginPanel.kt")
s = s[:start] + (pathlib.Path(__file__).parent / "SidePanel.kt.txt").read_text() + s[end:]
imports = [
	"androidx.compose.foundation.layout.fillMaxHeight",
	"androidx.compose.foundation.layout.offset",
	"androidx.compose.foundation.layout.statusBars",
	"androidx.compose.foundation.layout.width",
	"androidx.compose.ui.unit.IntOffset",
	"kotlin.math.roundToInt",
]
anchor = "import androidx.compose.foundation.layout.Row\n"
if anchor not in s:
	sys.exit("patch.py: import anchor not found in PluginPanel.kt")
extra = "".join(f"import {i}\n" for i in imports if f"import {i}\n" not in s)
s = s.replace(anchor, anchor + extra, 1)
pp.write_text(s)
print("patched PluginPanel.kt (right-hand sidebar)")
