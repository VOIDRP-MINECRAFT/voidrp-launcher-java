package ru.voidrp.launcher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import ru.voidrp.launcher.model.LauncherManifest;
import ru.voidrp.launcher.model.LauncherManifest.ManifestFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class FileSyncService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final OkHttpClient http = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build();

    private final LauncherPaths paths;
    private final HashService hashService;

    public FileSyncService(LauncherPaths paths, HashService hashService) {
        this.paths = paths;
        this.hashService = hashService;
    }

    public void sync(LauncherManifest manifest, Set<String> disabledMods, SyncProgress progress) throws Exception {
        paths.ensureDirs();
        var key = sanitize(manifest.getPackName());
        var stateFile = paths.state().resolve(key + ".json");
        var prevState = loadState(stateFile);
        var currentPaths = new HashSet<String>();

        var ordered = manifest.getFiles().stream()
            .sorted(Comparator.comparing(f -> normPath(f.getPath())))
            .toList();

        for (int i = 0; i < ordered.size(); i++) {
            var entry = ordered.get(i);
            var rel = normPath(entry.getPath());
            if (rel.isBlank()) continue;
            currentPaths.add(rel);

            if (progress != null) progress.onFile(rel, i, ordered.size(), 0);

            if (entry.isOptional() && !entry.isRequired() && disabledMods != null && disabledMods.contains(rel)) {
                var local = paths.game().resolve(rel.replace('/', File.separatorChar));
                if (Files.exists(local)) Files.delete(local);
                continue;
            }

            var local = paths.game().resolve(rel.replace('/', File.separatorChar));
            Files.createDirectories(local.getParent());

            if (needsDownload(entry, local)) {
                downloadFile(entry, rel, local, i, ordered.size(), progress);
            }
        }

        // Remove stale files
        for (var stale : prevState) {
            if (!currentPaths.contains(stale) && !isProtected(stale)) {
                var full = paths.game().resolve(stale.replace('/', File.separatorChar));
                try { Files.deleteIfExists(full); } catch (Exception ignored) {}
            }
        }

        // Remove rogue mods
        var modsDir = paths.game().resolve("mods");
        if (Files.isDirectory(modsDir)) {
            try (var walk = Files.walk(modsDir)) {
                walk.filter(Files::isRegularFile).forEach(p -> {
                    try {
                        var rel2 = normPath(paths.game().relativize(p).toString());
                        if (!currentPaths.contains(rel2)) {
                            Files.deleteIfExists(p);
                        }
                    } catch (Exception ignored) {}
                });
            }
        }

        saveState(stateFile, currentPaths.stream().filter(r -> !isPlayerWritable(r)).sorted().toList());
        hashService.flush();
        if (progress != null) progress.onFile("", ordered.size(), ordered.size(), 100);
    }

    private boolean needsDownload(ManifestFile entry, Path local) {
        if (!Files.exists(local)) return true;
        if (entry.isAlwaysOverwrite()) return true;
        if (isPlayerWritable(normPath(entry.getPath()))) return false;
        try {
            if (Files.size(local) != entry.getSize()) return true;
            return !hashService.sha256(local).equalsIgnoreCase(entry.getSha256());
        } catch (Exception e) {
            return true;
        }
    }

    private void downloadFile(ManifestFile entry, String rel, Path local, int idx, int total, SyncProgress progress) throws Exception {
        var temp = local.resolveSibling(local.getFileName() + ".download");
        Files.deleteIfExists(temp);
        try {
            var req = new Request.Builder().url(entry.getUrl()).build();
            try (var resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) throw new IOException("HTTP " + resp.code() + " for " + entry.getUrl());
                var body = resp.body();
                if (body == null) throw new IOException("Empty response");
                long totalBytes = body.contentLength() > 0 ? body.contentLength() : entry.getSize();
                try (var in = body.byteStream(); var out = new FileOutputStream(temp.toFile())) {
                    byte[] buf = new byte[65536];
                    long done = 0;
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                        done += n;
                        if (progress != null && totalBytes > 0) {
                            double pct = (idx + (double) done / totalBytes) / total * 85.0;
                            progress.onFile(rel, idx, total, pct);
                        }
                    }
                    out.flush();
                }
            }
            if (!entry.getSha256().isBlank()) {
                var got = hashService.sha256(temp);
                if (!got.equalsIgnoreCase(entry.getSha256()))
                    throw new IOException("SHA-256 mismatch for " + rel + ": expected " + entry.getSha256() + " got " + got);
            }
            Files.deleteIfExists(local);
            Files.move(temp, local);
        } catch (Exception e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    private static boolean isPlayerWritable(String rel) {
        return rel.equalsIgnoreCase("options.txt") || rel.equalsIgnoreCase("servers.dat")
            || rel.startsWith("resourcepacks/") || rel.startsWith("shaderpacks/")
            || rel.startsWith("config/");
    }

    private static boolean isProtected(String rel) {
        return isPlayerWritable(rel) || rel.startsWith("saves/") || rel.startsWith("screenshots/")
            || rel.startsWith("logs/") || rel.startsWith("crash-reports/");
    }

    private static String normPath(String path) {
        return path.replace('\\', '/').stripLeading().replaceFirst("^/+", "");
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    @SuppressWarnings("unchecked")
    private List<String> loadState(Path file) {
        try {
            if (!Files.exists(file)) return List.of();
            var map = JSON.readValue(file.toFile(), Map.class);
            var files = (List<?>) map.get("Files");
            if (files == null) return List.of();
            return files.stream().map(Object::toString).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private void saveState(Path file, List<String> files) {
        try {
            JSON.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), Map.of("Files", files));
        } catch (Exception ignored) {}
    }

    @FunctionalInterface
    public interface SyncProgress {
        void onFile(String rel, int processed, int total, double percent);
    }
}
