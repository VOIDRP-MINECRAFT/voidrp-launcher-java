package ru.voidrp.launcher.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RuntimeBootstrapService {
    private static final ObjectMapper JSON = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final OkHttpClient http = new OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build();

    private final LauncherPaths paths;
    private final HashService hashService;
    private final String seedUrl;
    private final String manifestBaseUrl;

    public RuntimeBootstrapService(LauncherPaths paths, HashService hashService,
                                   String seedUrl, String manifestBaseUrl) {
        this.paths = paths;
        this.hashService = hashService;
        this.seedUrl = seedUrl;
        this.manifestBaseUrl = manifestBaseUrl;
    }

    public void ensureRuntime(RuntimeProgress progress) throws Exception {
        paths.ensureDirs();
        progress.report("Проверяем Java runtime...", 0);

        boolean hasJava = hasJava();
        progress.report("Получаем runtime manifest...", 6);
        var manifestUrl = resolveManifestUrl();
        progress.report("Загружаем runtime manifest...", 12);

        var manifest = loadManifest(manifestUrl);
        if (manifest.files.isEmpty()) throw new IOException("Runtime manifest пуст");

        if (hasJava) {
            var stampFile = paths.state().resolve("runtime.stamp");
            var fingerprint = computeFingerprint(manifest);
            try {
                if (Files.exists(stampFile) && Files.readString(stampFile).trim().equals(fingerprint)) {
                    progress.report("Java runtime готов.", 100);
                    return;
                }
            } catch (Exception ignored) {}
        }

        syncFiles(manifest, progress);

        if (!hasJava()) throw new IOException("Java runtime синхронизирован, но java не найден");

        var fingerprint = computeFingerprint(manifest);
        try { Files.writeString(paths.state().resolve("runtime.stamp"), fingerprint); } catch (Exception ignored) {}

        progress.report("Java runtime готов.", 100);
    }

    private boolean hasJava() {
        try { paths.resolveJavaExe(); return true; } catch (Exception e) { return false; }
    }

    private String resolveManifestUrl() {
        var fallback = manifestBaseUrl.stripTrailing().replaceAll("/$", "") + "/" + paths.runtimeManifestFileName();
        if (seedUrl == null || seedUrl.isBlank()) return fallback;
        try {
            var req = new Request.Builder().url(addCacheBuster(seedUrl)).get().build();
            try (var resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) return fallback;
                var body = resp.body();
                if (body == null) return fallback;
                var node = JSON.readTree(body.string());
                if (node.isTextual() && !node.asText().isBlank()) return node.asText();
                var rid = paths.platformRid();
                var artifacts = node.get("artifacts");
                if (artifacts != null && artifacts.has(rid)) {
                    var plat = artifacts.get(rid);
                    for (var key : new String[]{"manifestUrl", "url"}) {
                        var v = plat.get(key);
                        if (v != null && v.isTextual() && !v.asText().isBlank()) return v.asText();
                    }
                }
                for (var key : new String[]{"manifestUrl", "runtimeManifestUrl", "url"}) {
                    var v = node.get(key);
                    if (v != null && v.isTextual() && !v.asText().isBlank()) return v.asText();
                }
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    private RuntimeManifest loadManifest(String url) throws IOException {
        var req = new Request.Builder().url(addCacheBuster(url)).get().build();
        try (var resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) throw new IOException("Runtime manifest: HTTP " + resp.code());
            var body = resp.body();
            if (body == null) throw new IOException("Empty runtime manifest response");
            var m = JSON.readValue(body.string(), RuntimeManifest.class);
            if (m == null || m.files == null) throw new IOException("Null runtime manifest");
            m.files.removeIf(f -> f.path == null || f.url == null || f.path.isBlank() || f.url.isBlank());
            return m;
        }
    }

    private void syncFiles(RuntimeManifest manifest, RuntimeProgress progress) throws Exception {
        int total = manifest.files.size();
        for (int i = 0; i < total; i++) {
            var entry = manifest.files.get(i);
            var rel = normPath(entry.path);
            var targetDir = resolveTargetDir(rel);
            var local = targetDir.resolve(rel.replace('/', File.separatorChar));
            Files.createDirectories(local.getParent());

            progress.report("Проверяем runtime: " + rel, calcPercent(i, total, 0));

            if (!needsDownload(entry, local)) {
                progress.report("Runtime готов: " + rel, calcPercent(i + 1, total, 0));
                continue;
            }

            downloadRuntimeFile(entry, rel, local, i, total, progress);
        }
        hashService.flush();
    }

    private Path resolveTargetDir(String rel) {
        for (var prefix : new String[]{"assets/", "libraries/", "versions/", "mods/", "config/",
            "defaultconfigs/", "resourcepacks/", "shaderpacks/", "kubejs/", "scripts/"}) {
            if (rel.startsWith(prefix)) return paths.game();
        }
        return paths.java();
    }

    private boolean needsDownload(RuntimeFile entry, Path local) {
        if (!Files.exists(local)) return true;
        if (entry.size > 0) {
            try { if (Files.size(local) != entry.size) return true; } catch (Exception e) { return true; }
        }
        if (entry.sha256 == null || entry.sha256.isBlank()) return false;
        return !hashService.sha256(local).equalsIgnoreCase(entry.sha256);
    }

    private void downloadRuntimeFile(RuntimeFile entry, String rel, Path local, int idx, int total, RuntimeProgress progress) throws Exception {
        var temp = local.resolveSibling(local.getFileName() + ".download");
        Files.deleteIfExists(temp);
        try {
            var req = new Request.Builder().url(entry.url).build();
            try (var resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) throw new IOException("HTTP " + resp.code());
                var body = resp.body();
                if (body == null) throw new IOException("Empty");
                long totalBytes = body.contentLength() > 0 ? body.contentLength() : entry.size;
                try (var in = body.byteStream(); var out = new FileOutputStream(temp.toFile())) {
                    byte[] buf = new byte[65536];
                    long done = 0;
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                        done += n;
                        if (totalBytes > 0) progress.report("Скачиваем: " + rel, calcPercent(idx, total, (double) done / totalBytes));
                    }
                    out.flush();
                }
            }
            if (entry.sha256 != null && !entry.sha256.isBlank()) {
                var got = hashService.sha256(temp);
                if (!got.equalsIgnoreCase(entry.sha256)) throw new IOException("SHA-256 mismatch for " + rel);
            }
            Files.deleteIfExists(local);
            Files.move(temp, local);
            // Make java executable on Linux/Mac
            if (local.getFileName().toString().equals("java") || local.getFileName().toString().equals("java.exe")) {
                try { local.toFile().setExecutable(true); } catch (Exception ignored) {}
            }
            try {
                if (!System.getProperty("os.name", "").toLowerCase().contains("win") && Files.isRegularFile(local)) {
                    var name = local.getFileName().toString();
                    if (!name.contains(".") || name.endsWith(".sh")) {
                        local.toFile().setExecutable(true);
                    }
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            Files.deleteIfExists(temp);
            throw e;
        }
        progress.report("Runtime готов: " + rel, calcPercent(idx + 1, total, 0));
    }

    private static double calcPercent(int idx, int total, double ratio) {
        if (total <= 0) return 100;
        ratio = Math.max(0, Math.min(1, ratio));
        return Math.max(0, Math.min(100, ((double) idx / total + ratio / total) * 100));
    }

    private static String computeFingerprint(RuntimeManifest m) {
        return (m.neoForgeVersion != null ? m.neoForgeVersion : "") + "|"
            + (m.buildDateUtc != null ? m.buildDateUtc : "") + "|" + m.files.size();
    }

    private static String addCacheBuster(String url) {
        return url + (url.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();
    }

    private static String normPath(String path) {
        return path.replace('\\', '/').stripLeading().replaceFirst("^/+", "");
    }

    @FunctionalInterface
    public interface RuntimeProgress {
        void report(String msg, double percent);
    }

    private static class RuntimeManifest {
        public String neoForgeVersion;
        public String buildDateUtc;
        public List<RuntimeFile> files = new ArrayList<>();
    }

    private static class RuntimeFile {
        public String path;
        public long size;
        public String sha256;
        public String url;
        public boolean alwaysOverwrite;
    }
}
