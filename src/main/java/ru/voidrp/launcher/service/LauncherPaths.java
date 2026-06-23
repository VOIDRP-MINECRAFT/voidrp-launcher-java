package ru.voidrp.launcher.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class LauncherPaths {
    private final Path base;

    public LauncherPaths() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String localApp = System.getenv("LOCALAPPDATA");
            if (localApp == null) localApp = System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Local";
            base = Path.of(localApp, "VoidRpLauncher");
        } else if (os.contains("mac")) {
            base = Path.of(System.getProperty("user.home"), "Library", "Application Support", "VoidRpLauncher");
        } else {
            String xdg = System.getenv("XDG_DATA_HOME");
            if (xdg != null && !xdg.isBlank()) {
                base = Path.of(xdg, "VoidRpLauncher");
            } else {
                base = Path.of(System.getProperty("user.home"), ".local", "share", "VoidRpLauncher");
            }
        }
    }

    public Path base() { return base; }
    public Path game() { return base.resolve("game"); }
    public Path versions() { return game().resolve("versions"); }
    public Path java() { return game().resolve("java"); }
    public Path logs() { return base.resolve("logs"); }
    public Path state() { return base.resolve("state"); }
    public Path temp() { return base.resolve("temp"); }
    public Path settings() { return base.resolve("launcher_settings.json"); }

    public void ensureDirs() {
        try {
            Files.createDirectories(base());
            Files.createDirectories(game());
            Files.createDirectories(versions());
            Files.createDirectories(java());
            Files.createDirectories(logs());
            Files.createDirectories(state());
            Files.createDirectories(temp());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create launcher directories: " + e.getMessage(), e);
        }
    }

    public String resolveJavaExe() {
        String os = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = os.contains("win");
        // Try direct bin path first
        Path javaBin = java().resolve("bin").resolve(isWindows ? "javaw.exe" : "java");
        if (Files.exists(javaBin)) return javaBin.toString();
        Path javaConsoleBin = java().resolve("bin").resolve(isWindows ? "java.exe" : "java");
        if (Files.exists(javaConsoleBin)) return javaConsoleBin.toString();
        // Search recursively
        try {
            String exeName = isWindows ? "javaw.exe" : "java";
            var found = Files.walk(java())
                .filter(p -> p.getFileName() != null && p.getFileName().toString().equals(exeName))
                .filter(Files::isExecutable)
                .findFirst();
            if (found.isPresent()) return found.get().toString();
            if (isWindows) {
                var found2 = Files.walk(java())
                    .filter(p -> p.getFileName() != null && p.getFileName().toString().equals("java.exe"))
                    .findFirst();
                if (found2.isPresent()) return found2.get().toString();
            }
        } catch (Exception ignored) {}
        throw new RuntimeException("Java executable not found in: " + java());
    }

    public String platformRid() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();
        boolean arm = arch.contains("aarch64") || arch.contains("arm64");
        if (os.contains("win")) return "win-x64";
        if (os.contains("mac")) return arm ? "osx-arm64" : "osx-x64";
        return arm ? "linux-arm64" : "linux-x64";
    }

    public String runtimeManifestFileName() {
        return "runtime-manifest." + platformRid() + ".json";
    }
}
