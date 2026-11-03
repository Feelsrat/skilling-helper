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
  12. Sharp panels: rendered at the integer scale that matches ~280dp on this screen and shown 1:1, unfiltered.
  13. A separate app id and name, so it installs next to the official RuneMobile instead of replacing it.
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
	"androidx.compose.foundation.gestures.detectHorizontalDragGestures",
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
	"    private var shSidebarPx = 0 // Skilling Helper build: width the open plugin sidebar takes from the game\n"
	"    private var shImeDismissed = false // Skilling Helper build: the user closed the keyboard over an open game field\n")
patch(ma,
	"""        val (w, h) = stageSize(root.width, root.height, canvasW, canvasH)
        val lp = stage.layoutParams as FrameLayout.LayoutParams
        if (lp.width != w || lp.height != h) {
            lp.width = w
            lp.height = h
""",
	"""        val reserve = shReservePx()
        val availW = (root.width - reserve).coerceAtLeast(1)
        val availH = root.height
        var (w, h) = stageSize(availW, root.height, canvasW, canvasH)
        if (reserve > 0 && canvasW > 0 && canvasH > 0) {
            // Fit the canvas's own shape into the space left of the bar (never stretch it, e.g. at the login screen
            // before a resize applies); fill it exactly once the canvas matches.
            val ca = canvasW.toFloat() / canvasH
            val wa = availW.toFloat() / availH
            when {
                abs(ca / wa - 1f) <= 0.01f -> { w = availW; h = availH }
                ca > wa -> { w = availW; h = Math.round(availW / ca) }
                else -> { w = Math.round(availH * ca); h = availH }
            }
        } else if (reserve > 0 && w == FrameLayout.LayoutParams.MATCH_PARENT) {
            w = availW
        }
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
     *  (ShUi: the dragged width, else the panels' exact width). */
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
                Math.round(ShUi.clampWidth(ShUi.widthPx.value ?: ShUi.defaultWidthPx(d), rw.toFloat(), d))
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
	"            // Skilling Helper build: a fresh composition, so a different icon really opens its own panel.\n"
	"            pluginPanel.disposeComposition()\n"
	"            val shPrefs = getSharedPreferences(\"sh_ui\", MODE_PRIVATE)\n"
	"            if (ShUi.widthPx.value == null && shPrefs.getFloat(\"sidebar_w\", -1f) > 0f) ShUi.widthPx.value = shPrefs.getFloat(\"sidebar_w\", -1f)\n"
	"            ShUi.onWidthCommitted = { px -> shPrefs.edit().putFloat(\"sidebar_w\", px).apply(); shApplySidebar(true) }\n"
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
                            }, tools = listOf(
                                ShTool("⌨", "Keyboard", { manualKeyboard }, { kbButton.performClick() }),
                                ShTool("⇧", "Shift", { shiftHeld }, { shiftButton.performClick() }),
                                ShTool("⌃", "Ctrl", { ctrlHeld }, { ctrlButton.performClick() }),
                            ))
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

# 12. Sharp plugin panels, like desktop at an integer UI scale. The JVM learns the device's panel scale at boot
# (sh.panelScale = round(280dp in px / 242)); the panel buffer is drawn at that scale and shown pixel for pixel.
patch(ma,
	"""                "-Xmx${MAX_HEAP_MB}m",
                // Fail fast""",
	"""                "-Xmx${MAX_HEAP_MB}m",
                "-Dsh.panelScale=${Math.round(280f * resources.displayMetrics.density / 242f).coerceIn(1, 6)}", // Skilling Helper build
                // Fail fast""")
patch(mb,
	"	private static final int RENDER_SCALE = 4;",
	"	private static final int RENDER_SCALE = Math.max(1, Math.min(6, Integer.getInteger(\"sh.panelScale\", 4))); // Skilling Helper build")
patch(mb,
	"	private static final long MAX_BUFFER_PX = 3_000_000L;",
	"	private static final long MAX_BUFFER_PX = 4_500_000L; // Skilling Helper build (was 3M): tall panels keep full scale")
patch(pv,
	"                    val colW = if (maxWidth < PANEL_MAX_DP) maxWidth else PANEL_MAX_DP\n",
	"""                    // Skilling Helper build: show the buffer pixel for pixel when it fits (sharp, like desktop).
                    val exactW = with(androidx.compose.ui.platform.LocalDensity.current) { pw.toDp() }
                    val oneToOne = pw > 0 && kotlin.math.abs((exactW - maxWidth).value) < 1f
                    val colW = maxWidth // the panel scales with the sidebar width (sharp at the default width)
""")
patch(pv,
	"                            contentScale = ContentScale.FillWidth,\n",
	"                            contentScale = ContentScale.FillWidth,\n"
	"                            filterQuality = if (oneToOne) androidx.compose.ui.graphics.FilterQuality.None else androidx.compose.ui.graphics.FilterQuality.High,\n")

# 13. Live panels. The stock repaint loop is a thread that quits as soon as no panel is active, and openPanel only
# starts a new one when the old one has already quit. Switching or reopening a panel (close + open, with the
# active panel briefly null on the EDT) leaves no loop running, so the panel only redraws on a tap (XP Tracker
# numbers froze until an entry was collapsed). One Swing timer that runs for the whole session instead.
patch(mb,
	"""		if (panelRepainterRunning)
		{
			return;
		}
		panelRepainterRunning = true;
		final Thread t = new Thread(() ->""",
	"""		if (panelRepainterRunning)
		{
			return;
		}
		panelRepainterRunning = true;
		// Skilling Helper build: one EDT timer for the whole session (see patch.py step 13)
		final javax.swing.Timer shTimer = new javax.swing.Timer(500, ev ->
		{
			if (activePanel != null)
			{
				renderActiveOnEdt();
			}
		});
		shTimer.setCoalesce(true);
		shTimer.start();
		if (shTimer.isRunning()) // always: skips the stock thread below
		{
			return;
		}
		final Thread t = new Thread(() ->""")

# 14. Typing into the game with the sidebar open. The stock poll only hands the keyboard to the game's hidden input
# view when it raises the keyboard; with the sidebar open during play another view can hold focus while the
# keyboard is already up, so typed text could go nowhere. Take focus back whenever the game wants the keyboard.
patch(ma,
	"""                        if (want != imeShown) {
                            if (want != keyboardShown) {""",
	"""                        // Skilling Helper build: the game's input view must hold focus whenever the game wants the keyboard
                        if (want && imeShown && !keyboardView.isFocused && keyboardView.requestFocus()) {
                            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).restartInput(keyboardView)
                            Log.i(TAG, "sh: game input view took focus back")
                        }
                        if (want != imeShown) {
                            if (want != keyboardShown) {""")
# 15. Closing the keyboard. The poll re-raised it on the next frame while a game field stayed open, so it could not
# be closed (e.g. to see the whole fairy ring dial). A close by the user now holds until that field closes; the ⌨
# button brings the keyboard back.
patch(ma,
	"""                        imeWasShown = imeShown
                        val want = manualKeyboard || mapSearchKeyboard || JvmLauncher.isTextInputOpen()""",
	"""                        // Skilling Helper build: respect a close while a game field is open (see patch.py step 15)
                        val shField = JvmLauncher.isTextInputOpen()
                        if (imeWasShown && !imeShown && keyboardShown && shField && !manualKeyboard && !mapSearchKeyboard) {
                            shImeDismissed = true
                            Log.i(TAG, "sh: user closed the keyboard over a game field -> stay closed until it closes")
                        }
                        if (!shField || manualKeyboard) shImeDismissed = false
                        imeWasShown = imeShown
                        val want = manualKeyboard || mapSearchKeyboard || (shField && !shImeDismissed)""")

# 16. Keyboards that compose. The game's input view only forwarded committed text, but Samsung Keyboard (and Gboard
# at times) types into a composing region first and never commits each letter, so most typing was lost. Ask for a
# plain no-suggestion field (visible-password variation, which keyboards treat as "no composing"), and forward
# composing text too, as the letters it adds or removes.
patch(ma,
	"                    EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS\n",
	"                    EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS or\n"
	"                        EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD // Skilling Helper build\n")
patch(ma,
	"""            return object : BaseInputConnection(this, false) {
                override fun commitText(
                    text: CharSequence?,
                    newCursorPosition: Int,
                ): Boolean {
                    val s = text?.toString().orEmpty()
                    if (s.isNotEmpty()) {
                        JvmLauncher.text(s)
                    }
                    return true
                }
""",
	"""            return object : BaseInputConnection(this, false) {
                // Skilling Helper build: what the keyboard is composing, already sent to the game letter by letter
                private var shComposing = ""

                private fun shSendDiff(next: String) {
                    var common = 0
                    while (common < shComposing.length && common < next.length && shComposing[common] == next[common]) common++
                    repeat(shComposing.length - common) { JvmLauncher.keyStroke(VK_BACK_SPACE) }
                    if (common < next.length) JvmLauncher.text(next.substring(common))
                    shComposing = next
                }

                override fun setComposingText(
                    text: CharSequence?,
                    newCursorPosition: Int,
                ): Boolean {
                    shSendDiff(text?.toString().orEmpty())
                    return true
                }

                override fun finishComposingText(): Boolean {
                    shComposing = ""
                    return true
                }

                override fun commitText(
                    text: CharSequence?,
                    newCursorPosition: Int,
                ): Boolean {
                    val s = text?.toString().orEmpty()
                    if (shComposing.isNotEmpty()) {
                        shSendDiff(s)
                        shComposing = ""
                    } else if (s.isNotEmpty()) {
                        JvmLauncher.text(s)
                    }
                    return true
                }
""")

# 17. The game keeps keyboard focus. Keys go straight to the game canvas's listeners, and the game ignores them while
# it thinks it lost focus. The stock bridge tells the game it has focus only once per session, but our sidebar keeps
# a plugin panel window showing during play, which can take focus from the canvas; after that every key was ignored.
# Tell the game it has focus before each burst of typing.
patch("runelite-client/src/main/java/net/runelite/client/headless/JavaBridge.java",
	"""		if (focused && canvas == focusedCanvas)
		{
			return;
		}
		focused = true;""",
	"""		// Skilling Helper build: always re-assert focus (see patch.py step 17)
		focused = true;""")

# 18. The chat stuck in its dialogue look. The bridge treats the chat as busy with a dialogue (opaque box, can't be
# collapsed, its left side under the button column) while any dialogue widget merely exists or anything is mounted
# on the chatbox, even when the game has hidden it. Count only what is actually showing.
hv = "runelite-client/src/main/java/net/runelite/client/headless/HudVeneerBridge.java"
patch(hv,
	"""			return c.getWidget(id) != null;
		}
		catch (RuntimeException ex)
		{
			return false;
		}""",
	"""			final Widget w = c.getWidget(id);
			return w != null && !w.isHidden(); // Skilling Helper build: shown, not just loaded
		}
		catch (RuntimeException ex)
		{
			return false;
		}""")
patch(hv,
	"""				if (n != null && (((int) n.getHash()) >>> 16) == InterfaceID.CHATBOX)
				{
					return n.getId();
				}""",
	"""				if (n != null && (((int) n.getHash()) >>> 16) == InterfaceID.CHATBOX)
				{
					// Skilling Helper build: skip a box mounted on a chatbox component the game has hidden
					final Widget host = c.getWidget((int) n.getHash());
					if (host != null && host.isHidden())
					{
						continue;
					}
					return n.getId();
				}""")

# 19. Desktop HUD. RuneMobile redraws the game's HUD in a mobile layout (chat moved to the top-left with a button
# column, native tab grid, minimap moved). Yuri wants it to look like RuneLite on desktop, so start with the game's
# own HUD where the game puts it: chat bottom-left, tab stones bottom-right, minimap top-right.
patch("android/app/src/main/java/dev/audaro/runemobile/ChromeOverlayView.kt",
	"    private var shown = true // A/B debug toggle (▦ native grid vs ▤ raw engine stones)",
	"    private var shown = false // Skilling Helper build: the game's own (desktop) HUD; was true (native grid)")

# 20. The touch helpers (keyboard, sticky Shift/Ctrl) move from the bottom-left, where they sat over the game's chat
# in the desktop HUD, to the bottom of the icon bar. Their buttons stay as the state holders, just not shown.
patch(ma,
	"""                addView(
                    devBar,""",
	"""                if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                    devBar.visibility = View.GONE // Skilling Helper build: shown in the icon bar instead
                }
                addView(
                    devBar,""")

# 21. S Pen (and mouse) hover, like a desktop mouse: a pen held over the game moves the game's cursor without
# clicking, so hover highlights, the top-left action text and plugin tooltips show. Leaving the screen parks it.
# Read at the activity, before any view: the overlays stacked over the game view would otherwise take the hover.
patch(ma,
	"""    private fun toGameX(viewX: Float): Int {""",
	"""    // Skilling Helper build: stylus/mouse hover moves the game cursor (see patch.py step 21)
    private val shHoverLoc = IntArray(2)
    private var shHovering = false

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        val tool = ev.getToolType(0)
        if ((tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_MOUSE) && ::surfaceView.isInitialized) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                    surfaceView.getLocationInWindow(shHoverLoc)
                    val x = ev.x - shHoverLoc[0]
                    val y = ev.y - shHoverLoc[1]
                    if (x >= 0 && y >= 0 && x < surfaceView.width && y < surfaceView.height) {
                        JvmLauncher.move(toGameX(x), toGameY(y))
                        shHovering = true
                    } else if (shHovering) {
                        JvmLauncher.clearHover()
                        shHovering = false
                    }
                }
                MotionEvent.ACTION_HOVER_EXIT -> if (shHovering) {
                    // A pen touching the screen also ends its hover, just before the touch. Parking the cursor then
                    // would land after the tap started and spoil it, so only park once the pen has really left.
                    shHovering = false
                    val exitAt = SystemClock.uptimeMillis()
                    surfaceView.postDelayed({
                        if (!shHovering && shLastDownAt < exitAt) JvmLauncher.clearHover()
                    }, 250)
                }
            }
        }
        return super.dispatchGenericMotionEvent(ev)
    }

    private var shLastDownAt = 0L

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) shLastDownAt = SystemClock.uptimeMillis()
        return super.dispatchTouchEvent(ev)
    }

    private fun toGameX(viewX: Float): Int {""")
patch(ma,
	"""                "-Dsh.panelScale=""",
	"""                "-Drunelite.hover.text=true", // Skilling Helper build: the desktop's top-left action text
                "-Dsh.panelScale=""")

# 22. Sidebar clean-up. Screen titles were 18-22sp in the RuneScape font next to text buttons, so in the narrow
# sidebar the title got squeezed to a letter per line ("Plu / gin / s"). Titles are now 16sp on one line (cut with
# "..."), back buttons are a plain "‹", and the launcher drops its "Close" (the sidebar's ✕ does that). SidePanel
# also draws its content at 90% density.
import re
kt_dir = root / "android/app/src/main/java/dev/audaro/runemobile"
one_line = "maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, "
title_hits = 0
for f in sorted(kt_dir.glob("*.kt")):
	s = f.read_text()
	n0 = s
	s, a = re.subn(r"fontSize = (?:1[89]|2[0-9])\.sp, modifier = Modifier\.weight\(1f\)",
		"fontSize = 16.sp, " + one_line + "modifier = Modifier.weight(1f)", s)
	s, b = re.subn(r"fontSize = (?:1[89]|2[0-9])\.sp,(\n\s*)modifier = Modifier\.weight\(1f\),",
		lambda m: "fontSize = 16.sp," + m.group(1) + one_line.rstrip() + m.group(1) + "modifier = Modifier.weight(1f),", s)
	s = s.replace('{ Text("‹ Back") }', '{ Text("‹", fontSize = 18.sp) }')
	title_hits += a + b
	if s != n0:
		f.write_text(s)
		print(f"patched {f.relative_to(root)} ({a + b} titles)")
if title_hits < 8:
	sys.exit(f"patch.py: step 22 found only {title_hits} screen titles")
pp = "android/app/src/main/java/dev/audaro/runemobile/PluginPanel.kt"
patch(pp, '}) { Text("‹ Grid") }', '}) { Text("‹", fontSize = 18.sp) }')
patch(pp,
	"""                            TextButton(onClick = { hubOpen = true }) { Text("Hub") }
                            TextButton(onClick = onClose) { Text("Close") }""",
	"""                            TextButton(onClick = { hubOpen = true }) { Text("Hub") }""")

# 23. S Pen side button = right-click: touching the screen with the pen's button held opens the menu at once, the
# same menu a long-press opens, instead of after the long-press wait.
patch(ma,
	"""                    touchPastSlop = false
                    surfaceView.removeCallbacks(holdRunnable)
                    surfaceView.postDelayed(holdRunnable, GameSettings.longPressMs(this))""",
	"""                    touchPastSlop = false
                    surfaceView.removeCallbacks(holdRunnable)
                    // Skilling Helper build: the S Pen's side button held = right-click (see patch.py step 23)
                    val shPenButton = event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS &&
                        (event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
                    surfaceView.postDelayed(holdRunnable, if (shPenButton) 0L else GameSettings.longPressMs(this).toLong())""")

# 24. Double-tap in plugin panels. Some panels act only on a double-click (RuneLite's World Switcher hops on a
# double-click of a world), and every tap was sent as a lone single click. A second tap on the same component soon
# after the first now arrives as a double-click.
mbp = "runelite-client/src/main/java/net/runelite/client/headless/MobileUiBridge.java"
patch(mbp,
	"	private static final AtomicInteger panelSeq = new AtomicInteger();\n",
	"	private static final AtomicInteger panelSeq = new AtomicInteger();\n"
	"	private static java.awt.Component shLastTapTarget; // Skilling Helper build: double-tap detection (EDT only)\n"
	"	private static long shLastTapAt;\n")
patch(mbp,
	"""			target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_ENTERED, when, 0, p.x, p.y, 0, false, MouseEvent.NOBUTTON));
			target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_MOVED, when, 0, p.x, p.y, 0, false, MouseEvent.NOBUTTON));
			target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_PRESSED, when, mods, p.x, p.y, 1, false, MouseEvent.BUTTON1));""",
	"""			final long shNow = System.currentTimeMillis();
			final int shCount = target == shLastTapTarget && shNow - shLastTapAt < 500 ? 2 : 1;
			shLastTapTarget = shCount == 2 ? null : target;
			shLastTapAt = shNow;
			target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_ENTERED, when, 0, p.x, p.y, 0, false, MouseEvent.NOBUTTON));
			target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_MOVED, when, 0, p.x, p.y, 0, false, MouseEvent.NOBUTTON));
			target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_PRESSED, when, mods, p.x, p.y, shCount, false, MouseEvent.BUTTON1));""")
patch(mbp,
	"""			target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_RELEASED, when + 1, 0, p.x, p.y, 1, false, MouseEvent.BUTTON1));
			target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_CLICKED, when + 2, 0, p.x, p.y, 1, false, MouseEvent.BUTTON1));""",
	"""			target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_RELEASED, when + 1, 0, p.x, p.y, shCount, false, MouseEvent.BUTTON1));
			target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_CLICKED, when + 2, 0, p.x, p.y, shCount, false, MouseEvent.BUTTON1));""")

# 25. The game's own right-click menu, as on desktop. RuneMobile hid the engine menu and drew its own list over it.
# Now a long-press right-clicks at the finger and the game draws its normal "Choose Option" menu, which stays open
# like a desktop right-click: tap an option to pick it, or tap elsewhere to close it.
patch("runelite-client/src/main/java/net/runelite/client/headless/JavaBridge.java",
	"	private static volatile boolean menuRenderSuppressed = true;",
	"	private static volatile boolean menuRenderSuppressed = false; // Skilling Helper build: the game draws its menu")
patch("runelite-client/src/main/java/net/runelite/client/headless/JavaBridge.java",
	"""	public static void setMenuRenderSuppressed(boolean suppressed)
	{
		menuRenderSuppressed = suppressed;""",
	"""	public static void setMenuRenderSuppressed(boolean suppressed)
	{
		menuRenderSuppressed = false; // Skilling Helper build: never hide the game's own menu""")
patch(ma,
	"""        dropTapArm()
        val gx = toGameX(viewX)
        val gy = toGameY(viewY)
        surfaceToOverlay(viewX, viewY)""",
	"""        dropTapArm()
        // Skilling Helper build: right-click and let the game show its own menu (see patch.py step 25)
        if (true) {
            JvmLauncher.click(toGameX(viewX), toGameY(viewY), 3)
            haptic(GameSettings.Haptic.MENU_OPEN)
            shMenuDrag = true
            shMenuDragMoved = false
            return
        }
        val gx = toGameX(viewX)
        val gy = toGameY(viewY)
        surfaceToOverlay(viewX, viewY)""")

# 26. Swiping a game interface (bank, quest list, chat, ...) scrolled the wrong way: a finger moving up scrolled the
# list up. It now drags the content with the finger, like the plugin sidebar and the rest of the phone.
patch(ma,
	"                ifaceScrollAccum += -distanceY / IFACE_SCROLL_PT_PER_NOTCH\n",
	"                ifaceScrollAccum += distanceY / IFACE_SCROLL_PT_PER_NOTCH // Skilling Helper build: was -distanceY\n")

# 27. Hold, drag to an option, let go: with the game's own menu (step 25) the rest of the long-press drove nothing,
# so the menu stayed open and the release did nothing. While the finger stays down after the menu opens, the cursor
# follows it (highlighting the option under it); letting go after a drag picks that option. Letting go without a
# drag leaves the menu open for a tap, as before.
patch(ma,
	"""            if (menuActive) {
                // The native menu is hit-tested in the overlay's space""",
	"""            // Skilling Helper build: drag-to-select on the game's own menu (see patch.py step 27)
            if (shMenuDrag) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        if (hypot(event.x - touchOriginX, event.y - touchOriginY) > sceneDragSlopPx) shMenuDragMoved = true
                        if (shMenuDragMoved) JvmLauncher.move(toGameX(event.x), toGameY(event.y))
                    }
                    MotionEvent.ACTION_UP -> {
                        shMenuDrag = false
                        // Only while the menu is still up: dragging off it closes it, and a click then would walk
                        val open = runCatching { JSONObject(JvmLauncher.menuSnapshot()).optBoolean("menuOpen", false) }
                            .getOrDefault(false)
                        if (shMenuDragMoved && open) JvmLauncher.click(toGameX(event.x), toGameY(event.y), 1)
                    }
                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> shMenuDrag = false
                }
                return@setOnTouchListener true
            }
            if (menuActive) {
                // The native menu is hit-tested in the overlay's space""")
patch(ma,
	"    private var shImeDismissed = false // Skilling Helper build: the user closed the keyboard over an open game field\n",
	"    private var shImeDismissed = false // Skilling Helper build: the user closed the keyboard over an open game field\n"
	"    private var shMenuDrag = false // Skilling Helper build: the long-press that opened the game menu is still down\n"
	"    private var shMenuDragMoved = false\n")
