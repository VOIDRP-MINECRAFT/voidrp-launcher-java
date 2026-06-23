package ru.voidrp.launcher.service;

import ru.voidrp.launcher.model.LauncherManifest;
import ru.voidrp.launcher.model.LauncherSettings;

import java.io.File;
import java.util.*;

public class ModService {
    public record ModInfo(String path, String displayName, String description, boolean optional, boolean required, boolean enabled) {}

    public List<ModInfo> list(LauncherManifest manifest, LauncherSettings settings) {
        var disabled = new HashSet<>(settings.getDisabledMods());
        return manifest.getFiles().stream()
            .filter(LauncherManifest.ManifestFile::isOptional)
            .map(f -> {
                var rel = normPath(f.getPath());
                var name = f.getDisplayName() != null && !f.getDisplayName().isBlank()
                    ? f.getDisplayName()
                    : new File(f.getPath()).getName().replaceAll("\\.jar$", "");
                var enabled = f.isRequired() || !disabled.contains(rel);
                return new ModInfo(rel, name, f.getDescription(), f.isOptional(), f.isRequired(), enabled);
            })
            .sorted(Comparator.comparing(m -> m.displayName().toLowerCase()))
            .toList();
    }

    public boolean toggle(String path, boolean enable, LauncherManifest manifest, LauncherSettings settings) {
        var rel = normPath(path);
        if (rel.isBlank()) return false;

        // Check if required
        if (manifest != null) {
            var entry = manifest.getFiles().stream()
                .filter(f -> normPath(f.getPath()).equalsIgnoreCase(rel))
                .findFirst().orElse(null);
            if (entry != null && entry.isRequired()) return false;
        }

        var disabled = new ArrayList<>(settings.getDisabledMods());
        if (enable) {
            disabled.removeIf(m -> m.equalsIgnoreCase(rel));
        } else {
            if (disabled.stream().noneMatch(m -> m.equalsIgnoreCase(rel))) {
                disabled.add(rel);
            }
        }
        settings.setDisabledMods(disabled);
        return true;
    }

    private static String normPath(String path) {
        return path.replace('\\', '/').stripLeading().replaceFirst("^/+", "");
    }
}
