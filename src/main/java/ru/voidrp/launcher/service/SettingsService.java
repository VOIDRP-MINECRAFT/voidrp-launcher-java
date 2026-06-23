package ru.voidrp.launcher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.voidrp.launcher.model.LauncherSettings;

import java.nio.file.Files;

public class SettingsService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final LauncherPaths paths;

    public SettingsService(LauncherPaths paths) {
        this.paths = paths;
    }

    public LauncherSettings load() {
        try {
            if (!Files.exists(paths.settings())) {
                var defaults = new LauncherSettings();
                defaults.setMaxRamMb(detectDefaultRam());
                save(defaults);
                return defaults;
            }
            var s = JSON.readValue(paths.settings().toFile(), LauncherSettings.class);
            s.setMaxRamMb(normalizeRam(s.getMaxRamMb()));
            return s;
        } catch (Exception e) {
            return new LauncherSettings();
        }
    }

    public void save(LauncherSettings settings) {
        try {
            paths.ensureDirs();
            settings.setMaxRamMb(normalizeRam(settings.getMaxRamMb()));
            JSON.writerWithDefaultPrettyPrinter().writeValue(paths.settings().toFile(), settings);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save settings: " + e.getMessage(), e);
        }
    }

    public int normalizeRam(int mb) {
        if (mb <= 0) mb = 4096;
        mb = (int) (Math.round(mb / 512.0) * 512);
        if (mb < 2048) mb = 2048;
        if (mb > 16384) mb = 16384;
        return mb;
    }

    private int detectDefaultRam() {
        try {
            var os = System.getProperty("os.name", "").toLowerCase();
            long totalBytes;
            if (os.contains("linux")) {
                totalBytes = Files.lines(java.nio.file.Path.of("/proc/meminfo"))
                    .filter(l -> l.startsWith("MemTotal:"))
                    .findFirst()
                    .map(l -> {
                        var parts = l.split("\\s+");
                        return parts.length >= 2 ? Long.parseLong(parts[1]) * 1024L : 0L;
                    })
                    .orElse(0L);
            } else {
                totalBytes = Runtime.getRuntime().maxMemory();
            }
            long gb = totalBytes / (1024L * 1024 * 1024);
            return gb >= 16 ? 6144 : 4096;
        } catch (Exception ignored) {
            return 4096;
        }
    }
}
