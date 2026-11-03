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
  7. In landscape the open sidebar takes its width from the game, which resizes like desktop instead of being covered.
  8. Dropdowns (JComboBox) in plugin panels open as a list drawn into the panel image (ComboHook.java.txt).
  9. A desktop-style icon bar always on the right edge in landscape (IconStrip.kt.txt); the game sits left of it.
  10. In landscape the icon bar's ≡ replaces the floating "≡ Plugins" button at the bottom centre.
  11. Landscape only: portrait and auto-rotate are gone, and so is their setting.
  12. A separate app id and name, so it installs next to the official RuneMobile instead of replacing it.
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

# 7. Sidebar shrinks the game instead of covering it (landscape). The window width the stage fit and the canvas
# resize use loses the sidebar's width while it is open, and the stage docks left; the engine then re-lays out at the
# narrower size, like desktop RuneLite when its sidebar opens.
ma = "android/app/src/main/java/dev/audaro/runemobile/MainActivity.kt"
patch(ma,
	"    private var gameStage: FrameLayout? = null\n",
	"    private var gameStage: FrameLayout? = null\n"
	"    private var shSidebarPx = 0 // Skilling Helper build: width the open plugin sidebar takes from the game\n")
patch(ma,
	"""        val (w, h) = stageSize(root.width, root.height, canvasW, canvasH)
        val lp = stage.layoutParams as FrameLayout.LayoutParams
        if (lp.width != w || lp.height != h) {
            lp.width = w
            lp.height = h
""",
	"""        val reserve = shReservePx()
        val availW = (root.width - reserve).coerceAtLeast(1)
        var (w, h) = stageSize(availW, root.height, canvasW, canvasH)
        if (reserve > 0 && w == FrameLayout.LayoutParams.MATCH_PARENT) w = availW
        val grav = if (reserve > 0) Gravity.START or Gravity.CENTER_VERTICAL else Gravity.CENTER
        val lp = stage.layoutParams as FrameLayout.LayoutParams
        if (lp.width != w || lp.height != h || lp.gravity != grav) {
            lp.width = w
            lp.height = h
            lp.gravity = grav
""")
patch(ma,
	"""            val (w, h) = if (root == null) 0 to 0 else canvasFor(root.width, root.height, resources.displayMetrics.density)
            val strip = if (root == null) 0 else stripFor(root.width, root.height, resources.displayMetrics.density)""",
	"""            val gameW = if (root == null) 0 else (root.width - shReservePx()).coerceAtLeast(1)
            val (w, h) = if (root == null) 0 to 0 else canvasFor(gameW, root.height, resources.displayMetrics.density)
            val strip = if (root == null) 0 else stripFor(gameW, root.height, resources.displayMetrics.density)""")
patch(ma,
	"    private fun fitGameStage() {\n",
	"""    /** Skilling Helper build: reserve (or give back) the plugin sidebar's width beside the game. Landscape only: in
     *  portrait the sidebar would leave the game too narrow, so it overlays as before. Matches SidePanel's width
     *  (42% of the window, 280-300dp). */
    private fun shReservePx(): Int {
        val root = rootContainer ?: return 0
        if (root.width <= root.height) return 0
        return Math.round(ShUi.STRIP_DP * resources.displayMetrics.density) + shSidebarPx
    }

    private fun shApplySidebar(open: Boolean) {
        val root = rootContainer ?: return
        val d = resources.displayMetrics.density
        val rw = root.width
        shSidebarPx =
            if (open && rw > root.height) {
                val panel = (rw * 0.42f).coerceIn(minOf(280 * d, rw.toFloat()), minOf(300 * d, rw.toFloat()))
                Math.round(panel)
            } else {
                0
            }
        fitGameStage()
        root.removeCallbacks(resizeCanvasToWindow)
        root.postDelayed(resizeCanvasToWindow, RESIZE_SETTLE_MS)
    }

    private fun fitGameStage() {
""")
t = (root / ma).read_text()
if t.count("pluginPanel.visibility = View.GONE\n") != 2 or t.count("pluginPanel.visibility = View.VISIBLE\n") != 1:
	sys.exit("patch.py: pluginPanel visibility anchors changed")
t = t.replace("pluginPanel.visibility = View.GONE\n", "pluginPanel.visibility = View.GONE\n shApplySidebar(false)\n")
t = t.replace("pluginPanel.visibility = View.VISIBLE\n", "pluginPanel.visibility = View.VISIBLE\n shApplySidebar(true)\n")
(root / ma).write_text(t)
print("patched MainActivity.kt (sidebar open/close)")

# 8. Dropdowns in panels.
mb = "runelite-client/src/main/java/net/runelite/client/headless/MobileUiBridge.java"
patch(mb,
	"	private static volatile String panelTextValue = \"\";\n",
	"	private static volatile String panelTextValue = \"\";\n" + (pathlib.Path(__file__).parent / "ComboHook.java.txt").read_text())
patch(mb,
	"		final int logicalH = Math.max(1, Math.min(PANEL_MAX_H, pp.getPreferredSize().height + 12));\n",
	"		final int logicalH = shComboExtend(Math.max(1, Math.min(PANEL_MAX_H, pp.getPreferredSize().height + 12)));\n")
patch(mb,
	"		wrapped.printAll(g); // wrapped is PANEL_W x PANEL_MAX_H showing the content from the top\n",
	"		wrapped.printAll(g); // wrapped is PANEL_W x PANEL_MAX_H showing the content from the top\n"
	"		shPaintCombo(g); // Skilling Helper build: an open dropdown's list\n")
patch(mb,
	"""			final int ly = y / s;
			Component target = SwingUtilities.getDeepestComponentAt(wrapped, lx, ly);""",
	"""			final int ly = y / s;
			if (shComboTap(wrapped, lx, ly))
			{
				renderActiveOnEdt();
				return;
			}
			Component target = SwingUtilities.getDeepestComponentAt(wrapped, lx, ly);""")
patch(mb,
	"	public static String openPanel(int index)\n	{\n",
	"	public static String openPanel(int index)\n	{\n		shCombo = null; // Skilling Helper build: no dropdown carries over to another panel\n")

# 9. Icon bar.
pp3 = "android/app/src/main/java/dev/audaro/runemobile/PluginPanel.kt"
t = (root / pp3).read_text()
t = t + (pathlib.Path(__file__).parent / "IconStrip.kt.txt").read_text()
(root / pp3).write_text(t)
patch(pp3,
	"""    onEnterOverlayEdit: () -> Unit = {}, // overlay layout is now edited ON the live game (host drag mode)
) {""",
	"""    onEnterOverlayEdit: () -> Unit = {}, // overlay layout is now edited ON the live game (host drag mode)
    startPanel: Pair<Int, String>? = null, // Skilling Helper build: open straight on this panel (icon bar)
) {""")
patch(pp3,
	"var panelOpen by remember { mutableStateOf<Pair<Int, String>?>(null) }",
	"var panelOpen by remember { mutableStateOf<Pair<Int, String>?>(startPanel) }")
# The sidebar sits left of the icon bar in landscape.
patch(pp3,
	"""                    .align(Alignment.CenterEnd)
                    .width(panelW)""",
	"""                    .align(Alignment.CenterEnd)
                    .padding(end = if (maxWidth > maxHeight) ShUi.STRIP_DP.dp else 0.dp)
                    .width(panelW)""")
patch(ma,
	"        fun openLauncher() {\n            pluginPanel.setContent {\n                PluginPanel(\n",
	"        fun openLauncher(start: Pair<Int, String>? = null) {\n            ShUi.active.value = start?.first ?: -1\n"
	"            pluginPanel.setContent {\n                PluginPanel(\n                    startPanel = start,\n")
t = (root / ma).read_text()
t = t.replace(" shApplySidebar(false)\n", " shApplySidebar(false)\n ShUi.active.value = null\n")
(root / ma).write_text(t)
patch(ma,
	"""                            bottomMargin = handleBaseBottom
                        },
                )
""",
	"""                            bottomMargin = handleBaseBottom
                        },
                )
                // Skilling Helper build: the desktop-style icon bar on the right edge (landscape; empty in portrait).
                addView(
                    ComposeView(this@MainActivity).apply {
                        elevation = 210f
                        setContent {
                            ShIconStrip(onOpen = { sel ->
                                val idx = sel?.first ?: -1
                                if (pluginPanel.visibility == View.VISIBLE && ShUi.active.value == idx) {
                                    pluginPanel.visibility = View.GONE
                                    pluginPanel.disposeComposition()
                                    JvmLauncher.onBridge { JvmLauncher.flushConfig() }
                                    shApplySidebar(false)
                                    ShUi.active.value = null
                                } else {
                                    openLauncher(sel)
                                }
                            })
                        }
                    },
                    FrameLayout.LayoutParams(
                        Math.round(ShUi.STRIP_DP * resources.displayMetrics.density),
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        Gravity.END,
                    ),
                )
""")
# A narrower game area is worth a canvas resize even under the 10% letterbox slack (the strip alone is ~4%).
patch(ma,
	"val worth = abs(w.toFloat() / oldW - 1f) > STAGE_ASPECT_SLACK || abs(h.toFloat() / oldH - 1f) > STAGE_ASPECT_SLACK",
	"val worth = abs(w.toFloat() / oldW - 1f) > 0.01f || abs(h.toFloat() / oldH - 1f) > 0.01f // Skilling Helper build")

# 10. The bottom-centre "≡ Plugins" button only in portrait; landscape has the icon bar.
patch(ma,
	"""        if (v == null || lp == null || host == null) return
        var margin = handleBaseBottom + handleInsetBottom""",
	"""        if (v == null || lp == null || host == null) return
        val shVis = if (host.width > host.height) View.GONE else View.VISIBLE // Skilling Helper build
        if (v.visibility != shVis) v.visibility = shVis
        var margin = handleBaseBottom + handleInsetBottom""")

# 11. Landscape only, like a desktop window.
ss = "android/app/src/main/java/dev/audaro/runemobile/ScreenSetting.kt"
patch(ss,
	"fun portrait(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_PORTRAIT, false)",
	"fun portrait(ctx: Context): Boolean = false // Skilling Helper build: landscape only")
patch(ss,
	"fun autoRotate(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_AUTO, false)",
	"fun autoRotate(ctx: Context): Boolean = false // Skilling Helper build: landscape only")
patch(ss,
	"internal fun GsScreenSection() {\n",
	"internal fun GsScreenSection() {\n    if (ScreenSetting.portrait(LocalContext.current) == false) return // Skilling Helper build: no Screen choice\n")
