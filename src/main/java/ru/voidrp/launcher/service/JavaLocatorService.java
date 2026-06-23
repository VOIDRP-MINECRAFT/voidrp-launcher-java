package ru.voidrp.launcher.service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JavaLocatorService {
    private static final Pattern VERSION_PATTERN =
        Pattern.compile("version \"(\\d+)(?:\\.(\\d+))?");

    private final LauncherPaths paths;

    public JavaLocatorService(LauncherPaths paths) {
        this.paths = paths;
    }

    /**
     * Finds a Java executable satisfying the required major version.
     * Search order: bundled → JAVA_HOME → PATH → well-known install dirs.
     * On Windows returns javaw.exe (no console window) when available.
     */
    public String locate(int requiredMajor) {
        if (requiredMajor <= 0) requiredMajor = 21;
        var wrongVersion = new ArrayList<String>();

        for (var binDir : binDirectories()) {
            var javaExe = binDir.resolve(isWindows() ? "java.exe" : "java");
            if (!Files.isExecutable(javaExe)) continue;

            int ver = detectVersion(javaExe.toString());
            if (ver < 0) continue;
            if (ver >= requiredMajor) {
                if (isWindows()) {
                    var javaw = binDir.resolve("javaw.exe");
                    if (Files.exists(javaw)) return javaw.toString();
                }
                return javaExe.toString();
            }
            wrongVersion.add(javaExe + " (Java " + ver + ")");
        }

        throw new RuntimeException(buildError(requiredMajor, wrongVersion));
    }

    private List<Path> binDirectories() {
        var result = new LinkedHashSet<Path>();

        // 1. Bundled Java (paths.java() / bin  or  paths.java()/<jdk-folder>/bin)
        var bundledBin = paths.java().resolve("bin");
        if (Files.isDirectory(bundledBin)) result.add(bundledBin);
        // Scan up to two levels deep: runtime seed unpacks to java/java/<jdk>/...
        try {
            Files.list(paths.java())
                .filter(Files::isDirectory)
                .sorted(Comparator.reverseOrder())
                .forEach(sub -> {
                    addJdkBinDirs(result, sub);
                    // Two levels: java/<extra-dir>/<jdk>/
                    try {
                        Files.list(sub)
                            .filter(Files::isDirectory)
                            .sorted(Comparator.reverseOrder())
                            .forEach(sub2 -> addJdkBinDirs(result, sub2));
                    } catch (Exception ignored) {}
                });
        } catch (Exception ignored) {}

        // 2. JAVA_HOME
        var javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            var bin = Path.of(javaHome, "bin");
            if (Files.isDirectory(bin)) result.add(bin);
        }

        // 3. macOS: /usr/libexec/java_home (most reliable on Mac)
        if (isMac()) {
            var home = runJavaHome(0); // any version — we'll verify ourselves
            if (home != null) {
                var bin = Path.of(home, "bin");
                if (Files.isDirectory(bin)) result.add(bin);
            }
        }

        // 4. PATH — locate java, then take parent (the bin dir)
        var fromPath = findInPath(isWindows() ? "java.exe" : "java");
        if (fromPath != null) {
            result.add(fromPath.getParent());
            try { result.add(fromPath.toRealPath().getParent()); } catch (Exception ignored) {}
        }

        // 4. Well-known install directories per OS
        if (isWindows()) {
            for (var root : List.of(
                Path.of("C:/Program Files/Java"),
                Path.of("C:/Program Files/Eclipse Adoptium"),
                Path.of("C:/Program Files/Microsoft"),
                Path.of("C:/Program Files/BellSoft"),
                Path.of("C:/Program Files/Amazon Corretto"),
                Path.of("C:/Program Files/Zulu")
            )) {
                addSubdirBins(result, root);
            }
            var local = System.getenv("LOCALAPPDATA");
            if (local != null) addSubdirBins(result, Path.of(local, "Programs", "Eclipse Adoptium"));
        } else if (isMac()) {
            // /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin
            var jvmRoot = Path.of("/Library/Java/JavaVirtualMachines");
            if (Files.isDirectory(jvmRoot)) {
                try {
                    Files.list(jvmRoot)
                        .filter(Files::isDirectory)
                        .sorted(Comparator.reverseOrder())
                        .forEach(sub -> {
                            var bin = sub.resolve("Contents").resolve("Home").resolve("bin");
                            if (Files.isDirectory(bin)) result.add(bin);
                        });
                } catch (Exception ignored) {}
            }
            for (var brew : List.of(
                Path.of("/usr/local/opt"),
                Path.of("/opt/homebrew/opt")
            )) addSubdirBins(result, brew);
        } else {
            // Linux
            for (var jvmRoot : List.of(
                Path.of("/usr/lib/jvm"),
                Path.of("/usr/local/lib/jvm"),
                Path.of("/opt/java"),
                Path.of("/opt/jdk")
            )) addSubdirBins(result, jvmRoot);
        }

        return new ArrayList<>(result);
    }

    private static void addJdkBinDirs(Set<Path> result, Path dir) {
        var bin = dir.resolve("bin");
        if (Files.isDirectory(bin)) result.add(bin);
        var macBin = dir.resolve("Contents").resolve("Home").resolve("bin");
        if (Files.isDirectory(macBin)) result.add(macBin);
    }

    private void addSubdirBins(Set<Path> set, Path dir) {
        if (!Files.isDirectory(dir)) return;
        try {
            Files.list(dir)
                .filter(Files::isDirectory)
                .sorted(Comparator.reverseOrder())
                .forEach(sub -> {
                    var bin = sub.resolve("bin");
                    if (Files.isDirectory(bin)) set.add(bin);
                });
        } catch (Exception ignored) {}
    }

    /** Calls /usr/libexec/java_home on macOS. Pass 0 for "any version". */
    private String runJavaHome(int minVersion) {
        try {
            var cmd = minVersion > 0
                ? new String[]{"/usr/libexec/java_home", "-v", minVersion + "+"}
                : new String[]{"/usr/libexec/java_home"};
            var proc = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String out;
            try (var r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                out = r.lines().collect(Collectors.joining()).trim();
            }
            proc.waitFor(5, TimeUnit.SECONDS);
            return out.isBlank() || out.startsWith("Unable") ? null : out;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Path findInPath(String name) {
        var pathEnv = System.getenv("PATH");
        if (pathEnv == null) return null;
        for (var dir : pathEnv.split(File.pathSeparator)) {
            var p = Path.of(dir.trim(), name);
            if (Files.exists(p)) return p;
        }
        return null;
    }

    int detectVersion(String javaExe) {
        try {
            var proc = new ProcessBuilder(javaExe, "-version")
                .redirectErrorStream(true)
                .start();
            String output;
            try (var r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                output = r.lines().collect(Collectors.joining("\n"));
            }
            proc.waitFor(5, TimeUnit.SECONDS);
            // Formats: 'java version "21.0.1"', 'openjdk version "21.0.1"', 'java version "1.8.0_321"'
            var m = VERSION_PATTERN.matcher(output);
            if (m.find()) {
                int major = Integer.parseInt(m.group(1));
                if (major == 1 && m.group(2) != null) major = Integer.parseInt(m.group(2)); // 1.8 → 8
                return major;
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private String buildError(int required, List<String> wrong) {
        var sb = new StringBuilder("Java " + required + "+ не найдена.");
        if (!wrong.isEmpty()) sb.append(" Найдена: ").append(wrong.get(0)).append(".");
        if (isWindows()) {
            sb.append(" Скачайте с https://adoptium.net (Eclipse Temurin " + required + ").");
        } else if (isMac()) {
            sb.append(" Установите: brew install --cask temurin@" + required
                + "  или  скачайте с https://adoptium.net.");
        } else {
            sb.append(" Установите: sudo apt install openjdk-" + required + "-jre"
                + "  или  скачайте с https://adoptium.net.");
        }
        return sb.toString();
    }

    private static boolean isWindows() { return System.getProperty("os.name", "").toLowerCase().contains("win"); }
    private static boolean isMac() { return System.getProperty("os.name", "").toLowerCase().contains("mac"); }
}
