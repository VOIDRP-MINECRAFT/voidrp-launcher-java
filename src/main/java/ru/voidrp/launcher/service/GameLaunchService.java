package ru.voidrp.launcher.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import ru.voidrp.launcher.config.AppConfig;
import ru.voidrp.launcher.model.LauncherManifest;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GameLaunchService {
    private static final ObjectMapper JSON = JsonMapper.builder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .build();

    private final LauncherPaths paths;

    public GameLaunchService(LauncherPaths paths) {
        this.paths = paths;
    }

    public record LaunchResult(Process process, Path logFile) {}

    public LaunchResult launch(String nickname, LauncherManifest manifest, int maxRamMb) throws Exception {
        if (nickname == null || nickname.isBlank()) throw new IllegalArgumentException("Nickname is empty");
        var profileId = manifest.getLauncherProfileId();
        if (profileId == null || profileId.isBlank())
            throw new IllegalArgumentException("LauncherProfileId missing in manifest. Run file sync first.");

        paths.ensureDirs();
        var requiredJava = manifest.getJavaVersion() > 0 ? manifest.getJavaVersion() : 21;
        var javaExe = new JavaLocatorService(paths).locate(requiredJava);
        var versionJson = paths.versions().resolve(profileId).resolve(profileId + ".json");

        if (!Files.exists(versionJson))
            throw new FileNotFoundException(
                "Version JSON not found: " + versionJson + "\nRun file sync first (нажми Play — sync выполнится автоматически).");

        var logFile = paths.logs().resolve("minecraft-" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".log");

        var cmd = buildCommand(nickname.trim(), profileId, versionJson, javaExe, maxRamMb, manifest);

        // Write launch command to log for debugging
        try (var w = Files.newBufferedWriter(logFile)) {
            w.write("[" + LocalDateTime.now() + "] Launch command:\n");
            w.write(String.join(" \\\n  ", cmd));
            w.write("\n\n[STDOUT/STDERR follows]\n");
        } catch (Exception ignored) {}

        var pb = new ProcessBuilder(cmd);
        pb.directory(paths.game().toFile());
        pb.redirectErrorStream(true);
        var proc = pb.start();

        // Stream output to log file
        Thread.ofVirtual().start(() -> {
            try (var in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                 var out = Files.newBufferedWriter(logFile, StandardOpenOption.APPEND)) {
                String line;
                while ((line = in.readLine()) != null) {
                    out.write(line);
                    out.newLine();
                }
            } catch (Exception ignored) {}
        });

        return new LaunchResult(proc, logFile);
    }

    private List<String> buildCommand(String nickname, String profileId, Path versionJson,
                                      String javaExe, int maxRamMb, LauncherManifest manifest) throws Exception {
        var version = JSON.readTree(versionJson.toFile());
        var merged = mergeInheritance(version);

        var mainClass = merged.has("mainClass") ? merged.get("mainClass").asText() : "";
        if (mainClass.isBlank()) throw new IOException("mainClass missing in version JSON");

        var gameDir = paths.game().toAbsolutePath().toString();
        var assetsDir = paths.game().resolve("assets").toAbsolutePath().toString();
        var nativesDir = paths.versions().resolve(profileId).resolve("natives").toAbsolutePath().toString();
        var libDir = paths.game().resolve("libraries").toAbsolutePath().toString();
        Files.createDirectories(Path.of(nativesDir));

        var sep = isWindows() ? ";" : ":";

        // Split classpath into module-path JARs (bootstraplauncher, securejarhandler,
        // JarJarFileSystems) and regular classpath JARs. NeoForge BootstrapLauncher 2.x
        // requires these three as named Java modules, not unnamed-classpath entries.
        var allJars = buildClasspathAsList(merged, profileId);
        var moduleJars = allJars.stream()
            .filter(GameLaunchService::isNeoForgeModuleJar)
            .toList();
        var cpJars = allJars.stream()
            .filter(j -> !isNeoForgeModuleJar(j))
            .toList();

        var fullCpStr = String.join(sep, allJars);   // all JARs — for -DlegacyClassPath
        var cpStr     = String.join(sep, cpJars);    // non-module JARs — for -cp
        var mpStr     = String.join(sep, moduleJars); // module JARs — for -p

        var assetIndexId = getAssetIndex(merged);
        var vars = buildVarMap(nickname, profileId, gameDir, assetsDir, nativesDir,
            assetIndexId, cpStr, libDir);
        vars.put("modules", mpStr); // satisfies ${modules} in NeoForge version JSON

        var cmd = new ArrayList<String>();
        cmd.add(javaExe);

        // Memory
        cmd.add("-Xmx" + maxRamMb + "m");
        cmd.add("-Xms" + Math.max(512, maxRamMb / 2) + "m");

        // Standard JVM flags
        cmd.add("-XX:+UseG1GC");
        cmd.add("-XX:+UnlockExperimentalVMOptions");
        cmd.add("-XX:G1NewSizePercent=20");
        cmd.add("-XX:G1ReservePercent=20");
        cmd.add("-XX:MaxGCPauseMillis=50");
        cmd.add("-XX:G1HeapRegionSize=32M");

        // NeoForge/Forge compat flags
        cmd.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
        cmd.add("-Dfml.ignorePatchDiscrepancies=true");

        // Module path for NeoForge BootstrapLauncher — must come before -cp
        if (!moduleJars.isEmpty()) {
            cmd.add("-p");
            cmd.add(mpStr);
            cmd.add("--add-modules");
            cmd.add("ALL-MODULE-PATH");
            // Open to the named module (on module path) AND to ALL-UNNAMED for code
            // that gets loaded via the legacy classloader
            for (var pkg : List.of("java.lang.invoke", "java.util.jar",
                                   "sun.security.util", "java.net")) {
                cmd.add("--add-opens=java.base/" + pkg + "=cpw.mods.securejarhandler");
                cmd.add("--add-opens=java.base/" + pkg + "=ALL-UNNAMED");
            }
        } else {
            // Fallback: no modular JARs found — open everything to ALL-UNNAMED
            for (var pkg : List.of("java.lang.invoke", "java.util.jar",
                                   "sun.security.util", "java.net")) {
                cmd.add("--add-opens=java.base/" + pkg + "=ALL-UNNAMED");
            }
        }

        // Native library paths (LWJGL, JNA) — must be set before Minecraft initialises
        cmd.add("-Djava.library.path=" + nativesDir);
        cmd.add("-Djna.tmpdir=" + nativesDir);
        cmd.add("-Dorg.lwjgl.system.SharedLibraryExtractPath=" + nativesDir);
        cmd.add("-Dio.netty.native.workdir=" + nativesDir);
        cmd.add("-Dminecraft.launcher.brand=" + AppConfig.DEVICE_NAME);
        cmd.add("-Dminecraft.launcher.version=" + AppConfig.VERSION);

        // BootstrapLauncher reads legacyClassPath to build the legacy class loader
        cmd.add("-DlegacyClassPath=" + fullCpStr);
        cmd.add("-DlibraryDirectory=" + libDir);

        // JVM args from version JSON
        var argsNode = merged.get("arguments");
        if (argsNode != null && argsNode.has("jvm")) {
            for (var arg : argsNode.get("jvm")) {
                collectArg(arg, cmd, vars);
            }
        }

        // Ensure classpath is set (version JSON may already have added it via ${classpath})
        if (cmd.stream().noneMatch(a -> a.equals("-cp") || a.equals("--class-path"))) {
            cmd.add("-cp");
            cmd.add(cpStr);
        }

        cmd.add(mainClass);

        // Game args from version JSON
        if (argsNode != null && argsNode.has("game")) {
            for (var arg : argsNode.get("game")) {
                collectArg(arg, cmd, vars);
            }
        }

        // Legacy minecraftArguments
        if (merged.has("minecraftArguments")) {
            for (var part : merged.get("minecraftArguments").asText().split(" ")) {
                if (!part.isBlank()) cmd.add(substitute(part, vars));
            }
        }

        // Fallback game args — added if the version JSON didn't provide them.
        // NeoForge needs --launchTarget to not NPE in ImmediateWindowHandler;
        // vanilla Minecraft needs the identity/path args to actually start.
        if (cmd.stream().noneMatch("--launchTarget"::equals)) {
            cmd.add("--launchTarget");
            cmd.add("forgeclient");
        }
        // NeoForge FML requires these four options; add them if the version JSON omitted them.
        // Primary source: manifest fields; fallbacks: profileId parsing and classpath scanning.
        if (cmd.stream().noneMatch("--fml.mcVersion"::equals)) {
            var mcVer = manifest.getMinecraftVersion();
            if (mcVer == null || mcVer.isBlank())
                mcVer = merged.has("inheritsFrom") ? merged.get("inheritsFrom").asText() : "";

            var nfVer = manifest.getNeoForgeVersion();
            if (nfVer == null || nfVer.isBlank()) nfVer = extractNeoForgeVersion(profileId);

            var fmlVer = manifest.getFmlVersion();
            if (fmlVer == null || fmlVer.isBlank())
                fmlVer = findLibraryVersion(allJars, "/fancymodloader/loader/");

            var nfFormVer = manifest.getNeoFormVersion();
            if (nfFormVer == null || nfFormVer.isBlank())
                nfFormVer = findLibraryVersion(allJars, "/net/neoforged/neoform/");
            // Ensure neoFormVersion has the MC prefix (e.g. "1.21.1-20240808.144430")
            if (nfFormVer != null && !nfFormVer.isBlank() && !nfFormVer.startsWith(mcVer + "-"))
                nfFormVer = mcVer + "-" + nfFormVer;

            if (!mcVer.isBlank())       { cmd.add("--fml.mcVersion");       cmd.add(mcVer); }
            if (nfVer != null && !nfVer.isBlank())       { cmd.add("--fml.neoForgeVersion");  cmd.add(nfVer); }
            if (nfFormVer != null && !nfFormVer.isBlank()){ cmd.add("--fml.neoFormVersion");   cmd.add(nfFormVer); }
            if (fmlVer != null && !fmlVer.isBlank())     { cmd.add("--fml.fmlVersion");       cmd.add(fmlVer); }
        }
        if (cmd.stream().noneMatch("--username"::equals)) {
            cmd.add("--username");    cmd.add(nickname);
            cmd.add("--version");     cmd.add(profileId);
            cmd.add("--gameDir");     cmd.add(gameDir);
            cmd.add("--assetsDir");   cmd.add(assetsDir);
            cmd.add("--assetIndex");  cmd.add(assetIndexId);
            cmd.add("--uuid");        cmd.add(offlineUuid(nickname));
            cmd.add("--accessToken"); cmd.add("0");
            cmd.add("--userType");    cmd.add("msa");
            cmd.add("--versionType"); cmd.add("release");
        }

        return cmd;
    }

    private JsonNode mergeInheritance(JsonNode version) throws Exception {
        var inheritsFrom = version.has("inheritsFrom") ? version.get("inheritsFrom").asText() : null;
        if (inheritsFrom == null || inheritsFrom.isBlank()) return version;

        Path parentJson = null;
        for (var candidate : List.of(
            paths.versions().resolve(inheritsFrom).resolve(inheritsFrom + ".json"),
            paths.game().resolve("versions").resolve(inheritsFrom).resolve(inheritsFrom + ".json")
        )) {
            if (Files.exists(candidate)) { parentJson = candidate; break; }
        }
        if (parentJson == null) return version;

        var parent = JSON.readTree(parentJson.toFile());
        var merged = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.createObjectNode();

        // Start with parent
        parent.fields().forEachRemaining(e -> merged.set(e.getKey(), e.getValue()));

        // Override with child (except libraries + arguments)
        version.fields().forEachRemaining(e -> {
            if (!e.getKey().equals("libraries") && !e.getKey().equals("arguments")) {
                merged.set(e.getKey(), e.getValue());
            }
        });

        // Merge libraries: parent first, then child
        var libs = JSON.createArrayNode();
        if (parent.has("libraries")) parent.get("libraries").forEach(libs::add);
        if (version.has("libraries")) version.get("libraries").forEach(libs::add);
        merged.set("libraries", libs);

        // Merge arguments
        var parentArgs = parent.has("arguments") ? parent.get("arguments") : null;
        var childArgs = version.has("arguments") ? version.get("arguments") : null;
        if (parentArgs != null || childArgs != null) {
            var mergedArgs = JSON.createObjectNode();
            for (var type : new String[]{"jvm", "game"}) {
                var arr = JSON.createArrayNode();
                if (parentArgs != null && parentArgs.has(type)) parentArgs.get(type).forEach(arr::add);
                if (childArgs != null && childArgs.has(type)) childArgs.get(type).forEach(arr::add);
                mergedArgs.set(type, arr);
            }
            merged.set("arguments", mergedArgs);
        }
        return merged;
    }

    private List<String> buildClasspathAsList(JsonNode version, String profileId) {
        var cp = new LinkedHashSet<String>();
        var gameDir = paths.game().toAbsolutePath();

        if (version.has("libraries")) {
            for (var lib : version.get("libraries")) {
                if (!isLibraryAllowed(lib)) continue;

                var downloads = lib.get("downloads");
                if (downloads != null) {
                    var artifact = downloads.get("artifact");
                    if (artifact != null && artifact.has("path")) {
                        var p = gameDir.resolve("libraries")
                            .resolve(artifact.get("path").asText().replace('/', File.separatorChar));
                        if (Files.exists(p)) cp.add(p.toString());
                    }
                } else if (lib.has("name")) {
                    var path = mavenToPath(lib.get("name").asText());
                    if (path != null) {
                        var p = gameDir.resolve("libraries").resolve(path.replace('/', File.separatorChar));
                        if (Files.exists(p)) cp.add(p.toString());
                    }
                }
            }
        }

        var versionJar = paths.versions().resolve(profileId).resolve(profileId + ".jar");
        if (Files.exists(versionJar)) cp.add(versionJar.toString());

        return new ArrayList<>(cp);
    }

    /**
     * JARs that NeoForge BootstrapLauncher 2.x requires on the module path.
     * securejarhandler requires org.objectweb.asm.* as named modules, so all
     * JARs under org/ow2/asm/ must also be on the module path.
     * Receives the full absolute path to the JAR.
     */
    private static boolean isNeoForgeModuleJar(String jarPath) {
        var path = jarPath.replace('\\', '/');
        var name = path.substring(path.lastIndexOf('/') + 1).toLowerCase();
        return name.startsWith("bootstraplauncher-")
            || name.startsWith("securejarhandler-")
            || name.startsWith("jarjarfilesystems-")
            || path.contains("/org/ow2/asm/");   // asm, asm-tree, asm-commons, asm-util, asm-analysis
    }

    /** Parses neoForgeVersion from profileId like "neoforge-21.1.232" → "21.1.232". */
    private static String extractNeoForgeVersion(String profileId) {
        var idx = profileId.lastIndexOf('-');
        if (idx >= 0) {
            var ver = profileId.substring(idx + 1);
            if (ver.matches("\\d+\\..*")) return ver;
        }
        return null;
    }

    /**
     * Scans JAR paths for a directory segment like "/net/neoforged/neoform/" and returns
     * the version component that immediately follows it.
     * E.g. ".../libraries/net/neoforged/neoform/1.21.1-20240808.144430/neoform-....jar"
     *      → "1.21.1-20240808.144430"
     */
    private static String findLibraryVersion(List<String> jars, String pathSegment) {
        var segFwd  = pathSegment;
        var segBack = pathSegment.replace('/', '\\');
        for (var jar : jars) {
            var norm = jar.replace('\\', '/');
            var idx = norm.indexOf(segFwd);
            if (idx < 0) continue;
            var after = norm.substring(idx + segFwd.length());
            var slash = after.indexOf('/');
            return slash >= 0 ? after.substring(0, slash) : after;
        }
        return null;
    }

    private boolean isLibraryAllowed(JsonNode lib) {
        var rules = lib.get("rules");
        if (rules == null) return true;
        boolean result = false;
        for (var rule : rules) {
            var action = rule.has("action") ? rule.get("action").asText() : "allow";
            var os = rule.get("os");
            boolean matches = (os == null) || matchesOs(os.has("name") ? os.get("name").asText() : "");
            if (matches) result = "allow".equals(action);
        }
        return result;
    }

    private boolean matchesOs(String osName) {
        return switch (osName) {
            case "windows" -> isWindows();
            case "osx" -> isMac();
            case "linux" -> !isWindows() && !isMac();
            default -> true;
        };
    }

    private static String mavenToPath(String name) {
        var parts = name.split(":");
        if (parts.length < 3) return null;
        // Handle classifier: group:artifact:version:classifier
        var group = parts[0].replace('.', '/');
        var artifact = parts[1];
        var version = parts[2];
        String classifier = parts.length > 3 ? parts[3] : null;
        var jar = artifact + "-" + version + (classifier != null ? "-" + classifier : "") + ".jar";
        return group + "/" + artifact + "/" + version + "/" + jar;
    }

    private static String getAssetIndex(JsonNode version) {
        var ai = version.get("assetIndex");
        if (ai != null && ai.has("id")) return ai.get("id").asText();
        var av = version.get("assets");
        if (av != null) return av.asText();
        return "1.21";
    }

    private Map<String, String> buildVarMap(String nickname, String profileId,
                                            String gameDir, String assetsDir, String nativesDir,
                                            String assetIndex, String classpath, String libDir) {
        var uuid = offlineUuid(nickname);
        var sep = isWindows() ? ";" : ":";
        var map = new HashMap<String, String>();

        // Standard Minecraft vars
        map.put("auth_player_name", nickname);
        map.put("auth_uuid", uuid);
        map.put("auth_access_token", "0");
        map.put("auth_session", "token:0:" + uuid);
        map.put("auth_xuid", "");
        map.put("clientid", "");
        map.put("user_type", "msa");
        map.put("user_properties", "{}");
        map.put("version_name", profileId);
        map.put("version_type", "release");
        map.put("game_directory", gameDir);
        map.put("assets_root", assetsDir);
        map.put("assets_index_name", assetIndex);
        map.put("game_assets", assetsDir);
        map.put("natives_directory", nativesDir);
        map.put("launcher_name", AppConfig.DEVICE_NAME);
        map.put("launcher_version", AppConfig.VERSION);

        // Classpath vars
        map.put("classpath", classpath);
        map.put("classpath_separator", sep);

        // NeoForge / Forge specific vars
        map.put("classpath_all_jar_modules", classpath);  // same as classpath for module-path
        map.put("library_directory", libDir);
        map.put("local_branch", "");
        map.put("forge_version", "");
        map.put("fml_version", "");

        return map;
    }

    private void collectArg(JsonNode arg, List<String> out, Map<String, String> vars) {
        if (arg.isTextual()) {
            var s = substitute(arg.asText(), vars);
            if (!s.isBlank()) out.add(s);
        } else if (arg.isObject()) {
            var rules = arg.get("rules");
            if (rules != null && !rulesAllow(rules)) return;
            // Version JSONs use both "value" (old format) and "values" (new format)
            var value = arg.has("value") ? arg.get("value") : arg.get("values");
            if (value == null) return;
            if (value.isTextual()) {
                var s = substitute(value.asText(), vars);
                if (!s.isBlank()) out.add(s);
            } else if (value.isArray()) {
                value.forEach(v -> {
                    var s = substitute(v.asText(), vars);
                    if (!s.isBlank()) out.add(s);
                });
            }
        }
    }

    private boolean rulesAllow(JsonNode rules) {
        boolean result = false;
        for (var rule : rules) {
            var action = rule.has("action") ? rule.get("action").asText() : "allow";
            var os = rule.get("os");
            var features = rule.get("features");
            // Skip feature-gated args (demo, custom resolution) — never enable
            if (features != null) continue;
            boolean matches = (os == null) || matchesOs(os.has("name") ? os.get("name").asText() : "");
            if (matches) result = "allow".equals(action);
        }
        return result;
    }

    private static String substitute(String tmpl, Map<String, String> vars) {
        for (var e : vars.entrySet()) {
            tmpl = tmpl.replace("${" + e.getKey() + "}", e.getValue() != null ? e.getValue() : "");
        }
        return tmpl;
    }

    private static String offlineUuid(String nickname) {
        try {
            var md5 = java.security.MessageDigest.getInstance("MD5");
            byte[] h = md5.digest(("OfflinePlayer:" + nickname).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            h[6] = (byte) ((h[6] & 0x0f) | 0x30);
            h[8] = (byte) ((h[8] & 0x3f) | 0x80);
            return String.format("%08x-%04x-%04x-%04x-%012x",
                toInt(h, 0), toShort(h, 4), toShort(h, 6), toShort(h, 8), toLong6(h, 10));
        } catch (Exception e) {
            return UUID.nameUUIDFromBytes(nickname.getBytes()).toString();
        }
    }

    private static int toInt(byte[] b, int o) {
        return ((b[o]&0xff)<<24)|((b[o+1]&0xff)<<16)|((b[o+2]&0xff)<<8)|(b[o+3]&0xff);
    }
    private static int toShort(byte[] b, int o) { return ((b[o]&0xff)<<8)|(b[o+1]&0xff); }
    private static long toLong6(byte[] b, int o) {
        long v=0; for(int i=0;i<6;i++) v=(v<<8)|(b[o+i]&0xff); return v;
    }
    private static boolean isWindows() { return System.getProperty("os.name","").toLowerCase().contains("win"); }
    private static boolean isMac() { return System.getProperty("os.name","").toLowerCase().contains("mac"); }
}
