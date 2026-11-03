
    // ---- Skilling Helper build (inserted by skilling-helper android/runemobile/patch.py) ----
    // Keeps the Skilling Helper plugin in ~/.runelite/sideloaded-plugins up to date from the latest GitHub
    // release of github.com/Feelsrat/skilling-helper. Any failure (offline, GitHub down) keeps the jar already
    // there, so the game still starts.
    private static final String SKILLING_HELPER_RELEASE =
        "https://github.com/Feelsrat/skilling-helper/releases/latest/download/";

    private static void skillingHelperSideload() {
        try {
            java.io.File dir = new java.io.File(System.getProperty("user.home"), ".runelite/sideloaded-plugins");
            if (!dir.isDirectory() && !dir.mkdirs()) {
                throw new java.io.IOException("cannot create " + dir);
            }
            java.io.File jar = new java.io.File(dir, "skilling-helper.jar");
            java.io.File mark = new java.io.File(dir, "skilling-helper.version");
            String latest = new String(skillingHelperFetch(SKILLING_HELPER_RELEASE + "android-version.txt"),
                java.nio.charset.StandardCharsets.UTF_8).trim();
            String have = mark.isFile() ? new String(java.nio.file.Files.readAllBytes(mark.toPath()),
                java.nio.charset.StandardCharsets.UTF_8).trim() : "";
            if (jar.isFile() && latest.equals(have)) {
                System.out.println("[skilling-helper] plugin " + have + " is current");
                return;
            }
            byte[] data = skillingHelperFetch(SKILLING_HELPER_RELEASE + "runemobile-plugin.jar");
            if (data.length < 100_000) {
                throw new java.io.IOException("plugin jar too small: " + data.length + " bytes");
            }
            java.io.File tmp = new java.io.File(dir, "skilling-helper.jar.part");
            java.nio.file.Files.write(tmp.toPath(), data);
            java.nio.file.Files.move(tmp.toPath(), jar.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            java.nio.file.Files.write(mark.toPath(), latest.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            System.out.println("[skilling-helper] plugin updated to " + latest);
        } catch (Exception e) {
            System.out.println("[skilling-helper] plugin update failed, keeping the current one: " + e);
        }
    }

    private static byte[] skillingHelperFetch(String url) throws java.io.IOException {
        java.net.HttpURLConnection c = (java.net.HttpURLConnection) java.net.URI.create(url).toURL().openConnection();
        c.setConnectTimeout(10_000);
        c.setReadTimeout(30_000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "skilling-helper-runemobile");
        try {
            int code = c.getResponseCode();
            if (code != 200) {
                throw new java.io.IOException("HTTP " + code + " for " + url);
            }
            // Java 8 (the boot class is compiled with --release 8): no InputStream.readAllBytes.
            try (java.io.InputStream in = c.getInputStream()) {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[65536];
                for (int n; (n = in.read(buf)) > 0; ) {
                    out.write(buf, 0, n);
                }
                return out.toByteArray();
            }
        } finally {
            c.disconnect();
        }
    }
    // ---- end Skilling Helper build ----

