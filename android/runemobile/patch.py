#!/usr/bin/env python3
"""Turn a RuneMobile checkout (github.com/audaro/RuneMobile) into the Skilling Helper build.

Usage: patch.py <RuneMobile checkout>

Changes, each one checked so an upstream change fails the build loudly instead of shipping a half-patched app:
  1. RuneLite loads ~/.runelite/sideloaded-plugins without --developer-mode (RuneMobile never passes it).
  2. Before RuneLite starts, the boot class downloads the latest Skilling Helper plugin jar from this repo's
     GitHub release into that folder (keeps the old jar when offline).
  3. The plugin panel is a desktop-style sidebar on the right edge (SidePanel.kt.txt) instead of a bottom sheet.
  4. Plugin Hub search works like desktop: ranked results, tags, install counts (HubSearch.kt.txt).
  5. Taps in rendered plugin panels reach the component that listens for them, like desktop AWT does.
  6. Panels: hover events before a tap, faster and cheaper refresh, a narrower desktop-sized panel; compact hub rows.
  7. A separate app id and name, so it installs next to the official RuneMobile instead of replacing it.
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

# 4. Plugin Hub search: the bridge also sends tags and install counts; the screen ranks results like desktop.
patch("runelite-client/src/main/java/net/runelite/client/headless/MobileUiBridge.java",
	"	private static volatile List<PluginHubManifest.DisplayData> hubDisplay;\n",
	"	private static volatile List<PluginHubManifest.DisplayData> hubDisplay;\n"
	"	private static volatile java.util.Map<String, Integer> hubCounts = java.util.Collections.emptyMap();\n")
patch("runelite-client/src/main/java/net/runelite/client/headless/MobileUiBridge.java",
	"hubDisplay = extPluginClient.downloadManifestFull().getDisplay();",
	"final List<PluginHubManifest.DisplayData> fetched = extPluginClient.downloadManifestFull().getDisplay();\n"
	"						try\n"
	"						{\n"
	"							hubCounts = extPluginClient.getPluginCounts();\n"
	"						}\n"
	"						catch (Throwable cex)\n"
	"						{\n"
	"							log.warn(\"[mobileui] hub install counts failed\", cex);\n"
	"						}\n"
	"						hubDisplay = fetched;")
patch("runelite-client/src/main/java/net/runelite/client/headless/MobileUiBridge.java",
	"""				.kb("installed", installed.contains(d.getInternalName()))
				.end();""",
	"""				.kb("installed", installed.contains(d.getInternalName()))
				.ki("installs", hubCounts.getOrDefault(d.getInternalName(), 0))
				.key("tags").arr();
			if (d.getTags() != null)
			{
				for (String tag : d.getTags())
				{
					out.val(tag);
				}
			}
			out.end().end();""")
hub = "android/app/src/main/java/dev/audaro/runemobile/PluginHubScreen.kt"
patch(hub,
	"""    val installed: Boolean,
    val installing: Boolean = false,
)
""",
	"""    val installed: Boolean,
    val installing: Boolean = false,
    val tags: List<String> = emptyList(),
    val installs: Int = 0,
)
""" + (pathlib.Path(__file__).parent / "HubSearch.kt.txt").read_text())
patch(hub,
	"""                val shown =
                    rows.filter {
                        // Match name, author, and description (parity with iOS's three-field hub search) — a
                        // user often recalls a plugin by its author.
                        filter.isBlank() || it.displayName.contains(filter, ignoreCase = true) ||
                            (it.author?.contains(filter, ignoreCase = true) == true) ||
                            (it.description?.contains(filter, ignoreCase = true) == true)
                    }
""",
	"""                val shown = remember(rows, filter) { hubSearch(rows, filter) }
""")
patch(hub,
	"""                if (!p.version.isNullOrBlank()) {
                    Text("  v${p.version}", color = Color(0xFF9A9A9A), fontSize = 12.sp)
                }
""",
	"""                if (!p.version.isNullOrBlank()) {
                    Text("  v${p.version}", color = Color(0xFF9A9A9A), fontSize = 12.sp)
                }
                if (p.installs > 0) {
                    Text("  ${formatInstalls(p.installs)} installs", color = Color(0xFF9A9A9A), fontSize = 12.sp)
                }
""")
patch(hub,
	"""                        installed = o.optBoolean("installed"),
                    )""",
	"""                        installed = o.optBoolean("installed"),
                        tags =
                            o.optJSONArray("tags")?.let { a -> (0 until a.length()).map { a.optString(it) } }
                                ?: emptyList(),
                        installs = o.optInt("installs", 0),
                    )""")

# 5. Plugin panel taps. Desktop AWT sends a click to the deepest component that has mouse listeners, so a click on a
# label inside a clickable panel reaches the panel. The bridge sent it to the deepest component of any kind, where a
# plain JLabel just drops it, so most custom plugin rows and buttons did nothing.
patch("runelite-client/src/main/java/net/runelite/client/headless/MobileUiBridge.java",
	"""			final Component target = SwingUtilities.getDeepestComponentAt(wrapped, lx, ly);
			if (target == null)
			{
				return;
			}
""",
	"""			Component target = SwingUtilities.getDeepestComponentAt(wrapped, lx, ly);
			if (target == null)
			{
				return;
			}
			// Skilling Helper build: retarget like AWT's LightweightDispatcher, to the nearest component that
			// listens for mouse events (a label inside a clickable row hands the click to the row).
			// A listener that is only Swing's ToolTipManager (any component with a tooltip has one) does not count,
			// else a label with a tooltip inside a clickable row would swallow the row's click.
			for (Component c = target; c != null; c = c.getParent())
			{
				boolean listens = false;
				for (java.awt.event.MouseListener ml : c.getMouseListeners())
				{
					if (!(ml instanceof javax.swing.ToolTipManager))
					{
						listens = true;
						break;
					}
				}
				if (listens)
				{
					target = c;
					break;
				}
				if (c == wrapped)
				{
					break;
				}
			}
""")
# The tap offset from a pointerInput placed after verticalScroll is already in scrolled content coordinates, so
# adding the scroll offset again sent every tap on a scrolled panel too far down.
patch("android/app/src/main/java/dev/audaro/runemobile/PanelView.kt",
	"    val cy = originY + ((off.y + scrollY) - originY - pan.y) / scale\n",
	"    val cy = originY + (off.y - originY - pan.y) / scale // Skilling Helper build: off.y already includes the scroll\n")

# 6a. Send MOUSE_ENTERED before the press (some panels only act on a row the mouse is over) and MOUSE_EXITED after.
patch("runelite-client/src/main/java/net/runelite/client/headless/MobileUiBridge.java",
	"""			target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_PRESSED, when, mods, p.x, p.y, 1, false, MouseEvent.BUTTON1));""",
	"""			target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_ENTERED, when, 0, p.x, p.y, 0, false, MouseEvent.NOBUTTON));
			target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_MOVED, when, 0, p.x, p.y, 0, false, MouseEvent.NOBUTTON));
			target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_PRESSED, when, mods, p.x, p.y, 1, false, MouseEvent.BUTTON1));""")
patch("runelite-client/src/main/java/net/runelite/client/headless/MobileUiBridge.java",
	"""			target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_CLICKED, when + 2, 0, p.x, p.y, 1, false, MouseEvent.BUTTON1));""",
	"""			target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_CLICKED, when + 2, 0, p.x, p.y, 1, false, MouseEvent.BUTTON1));
			target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_EXITED, when + 3, 0, p.x, p.y, 0, false, MouseEvent.NOBUTTON));""")

pv = "android/app/src/main/java/dev/audaro/runemobile/PanelView.kt"
# 6b. Narrower panel column: ~1.15 dp per Swing px instead of ~1.4, so more of a panel fits on screen.
patch(pv, "private val PANEL_MAX_DP = 340.dp", "private val PANEL_MAX_DP = 280.dp // Skilling Helper build (was 340)")
# 6c. Poll the frame counter and copy the (large) pixel buffer only when it changed, and poll faster so a tap shows.
patch(pv,
	"""            val s =
                withContext(Dispatchers.IO) {
                    JvmLauncher
                        .onBridge {
                            PanelSnap(
                                JvmLauncher.panelWidth(),
                                JvmLauncher.panelHeight(),
                                JvmLauncher.panelFrameSeq(),
                                JvmLauncher.panelPixels(),
                            )
                        }.get()
                }""",
	"""            val s =
                withContext(Dispatchers.IO) {
                    JvmLauncher
                        .onBridge {
                            // Skilling Helper build: skip the pixel copy when nothing was redrawn.
                            val seqNow = JvmLauncher.panelFrameSeq()
                            PanelSnap(
                                JvmLauncher.panelWidth(),
                                JvmLauncher.panelHeight(),
                                seqNow,
                                if (seqNow != lastSeq) JvmLauncher.panelPixels() else null,
                            )
                        }.get()
                }""")
patch(pv, """            delay(400)
        }""", """            delay(150) // Skilling Helper build (was 400)
        }""")

# 6d. Sidebar width to match the narrower panel column.
pp2 = root / "android/app/src/main/java/dev/audaro/runemobile/PluginPanel.kt"
t = pp2.read_text()
old_w = "val maxW = with(density) { 380.dp.toPx() }"
if old_w not in t:
	sys.exit("patch.py: sidebar width anchor missing")
pp2.write_text(t.replace(old_w, "val maxW = with(density) { 300.dp.toPx() }").replace(
	"val minW = with(density) { 280.dp.toPx() }", "val minW = with(density) { 280.dp.toPx() }"))
print("patched PluginPanel.kt (sidebar width)")

# 6e. Compact hub rows: name on one line, then author / version / installs on one line, description 2 lines.
patch(hub,
	"""            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.displayName, color = if (unavailable) Color(0xFF9E9E9E) else Color.White, fontSize = 15.sp)
                if (!p.author.isNullOrBlank()) {
                    Text("  by ${p.author}", color = Color(0xFF9A9A9A), fontSize = 12.sp)
                }
                if (!p.version.isNullOrBlank()) {
                    Text("  v${p.version}", color = Color(0xFF9A9A9A), fontSize = 12.sp)
                }
                if (p.installs > 0) {
                    Text("  ${formatInstalls(p.installs)} installs", color = Color(0xFF9A9A9A), fontSize = 12.sp)
                }
            }
""",
	"""            // Skilling Helper build: one line each, so a long name or author never stacks the row up word by word.
            Text(
                p.displayName,
                color = if (unavailable) Color(0xFF9E9E9E) else Color.White,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta =
                listOfNotNull(
                    p.author?.takeIf { it.isNotBlank() },
                    p.version?.takeIf { it.isNotBlank() }?.let { "v$it" },
                    p.installs.takeIf { it > 0 }?.let { "${formatInstalls(it)} installs" },
                ).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(meta, color = Color(0xFF9A9A9A), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
""")
