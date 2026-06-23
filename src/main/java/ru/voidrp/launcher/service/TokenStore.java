package ru.voidrp.launcher.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

public class TokenStore {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Path file;

    public TokenStore(LauncherPaths paths) {
        this.file = paths.state().resolve("launcher-auth.json");
    }

    public void save(String refreshToken) {
        try {
            Files.createDirectories(file.getParent());
            JSON.writeValue(file.toFile(), Map.of("RefreshToken", refreshToken));
            tryRestrictPermissions();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save token: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public String load() {
        try {
            if (!Files.exists(file)) return null;
            var map = JSON.readValue(file.toFile(), Map.class);
            var token = (String) map.get("RefreshToken");
            return (token != null && !token.isBlank()) ? token : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void clear() {
        try { Files.deleteIfExists(file); } catch (Exception ignored) {}
    }

    private void tryRestrictPermissions() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (!os.contains("win")) {
                Files.setPosixFilePermissions(file, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
                ));
            }
        } catch (Exception ignored) {}
    }
}
