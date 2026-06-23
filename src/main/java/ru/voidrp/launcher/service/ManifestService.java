package ru.voidrp.launcher.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import ru.voidrp.launcher.model.LauncherManifest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ManifestService {
    private static final ObjectMapper JSON = JsonMapper.builder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .build();

    private final OkHttpClient http = new OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build();

    public LauncherManifest load(String manifestUrl) throws IOException {
        var url = addCacheBuster(manifestUrl);
        var req = new Request.Builder().url(url).get().build();
        try (var resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) throw new IOException("Manifest fetch failed: HTTP " + resp.code());
            var body = resp.body();
            if (body == null) throw new IOException("Empty manifest response");
            var manifest = JSON.readValue(body.string(), LauncherManifest.class);
            if (manifest == null) throw new IOException("Manifest is null after parsing");
            return manifest;
        }
    }

    private static String addCacheBuster(String url) {
        var sep = url.contains("?") ? "&" : "?";
        return url + sep + "t=" + System.currentTimeMillis();
    }
}
