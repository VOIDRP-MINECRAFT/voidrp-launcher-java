package ru.voidrp.launcher.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

public class HashService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Path cacheFile;
    private final Map<String, CacheEntry> cache;
    private boolean dirty;

    public HashService(LauncherPaths paths) {
        this.cacheFile = paths.state().resolve("hash-cache.json");
        this.cache = loadCache();
    }

    public String sha256(Path file) {
        try {
            var info = file.toFile();
            var key = file.toString();
            var cached = cache.get(key);
            if (cached != null && cached.size == info.length() && cached.mtime == info.lastModified()) {
                return cached.hash;
            }
        } catch (Exception ignored) {}

        var hash = computeSha256(file);
        try {
            var info = file.toFile();
            cache.put(file.toString(), new CacheEntry(info.length(), info.lastModified(), hash));
            dirty = true;
        } catch (Exception ignored) {}
        return hash;
    }

    public void flush() {
        if (!dirty) return;
        try {
            Files.createDirectories(cacheFile.getParent());
            JSON.writeValue(cacheFile.toFile(), cache);
            dirty = false;
        } catch (Exception ignored) {}
    }

    private String computeSha256(Path file) {
        try (var is = new FileInputStream(file.toFile())) {
            var md = MessageDigest.getInstance("SHA-256");
            var buf = new byte[65536];
            int n;
            while ((n = is.read(buf)) != -1) md.update(buf, 0, n);
            return HEX.formatHex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed for: " + file, e);
        }
    }

    private Map<String, CacheEntry> loadCache() {
        try {
            if (!Files.exists(cacheFile)) return new HashMap<>();
            return JSON.readValue(cacheFile.toFile(), new TypeReference<>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private static final HexFormat HEX = HexFormat.of().withLowerCase();

    record CacheEntry(long size, long mtime, String hash) {}
}
