#!/usr/bin/env python3
"""Turn a RuneMobile checkout (github.com/audaro/RuneMobile) into the Skilling Helper build.

Usage: patch.py <RuneMobile checkout>

Changes, each one checked so an upstream change fails the build loudly instead of shipping a half-patched app:
  1. RuneLite loads ~/.runelite/sideloaded-plugins without --developer-mode (RuneMobile never passes it).
  2. Before RuneLite starts, the boot class downloads the latest Skilling Helper plugin jar from this repo's
     GitHub release into that folder (keeps the old jar when offline).
  3. A separate app id and name, so it installs next to the official RuneMobile instead of replacing it.
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
