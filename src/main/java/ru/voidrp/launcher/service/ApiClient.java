package ru.voidrp.launcher.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    public static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    public static final ObjectMapper JSON = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final OkHttpClient http;
    private final String baseUrl;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.http = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    public <T> T post(String path, Object body, String token, Class<T> responseType) throws IOException {
        var req = new Request.Builder()
            .url(baseUrl + path)
            .post(jsonBody(body));
        if (token != null) req.header("Authorization", "Bearer " + token);
        return execute(req.build(), responseType);
    }

    public <T> T get(String path, String token, Class<T> responseType) throws IOException {
        var req = new Request.Builder().url(baseUrl + path).get();
        if (token != null) req.header("Authorization", "Bearer " + token);
        return execute(req.build(), responseType);
    }

    public <T> T put(String path, Object body, String token, Class<T> responseType) throws IOException {
        var req = new Request.Builder()
            .url(baseUrl + path)
            .put(jsonBody(body));
        if (token != null) req.header("Authorization", "Bearer " + token);
        return execute(req.build(), responseType);
    }

    public <T> T delete(String path, String token, Class<T> responseType) throws IOException {
        var req = new Request.Builder().url(baseUrl + path).delete();
        if (token != null) req.header("Authorization", "Bearer " + token);
        return execute(req.build(), responseType);
    }

    public <T> T postMultipart(String path, Path filePath, String fieldName, String modelVariant, String token, Class<T> responseType) throws IOException {
        var body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(fieldName, filePath.getFileName().toString(),
                RequestBody.create(filePath.toFile(), MediaType.get("image/png")))
            .addFormDataPart("model_variant", modelVariant)
            .build();
        var req = new Request.Builder().url(baseUrl + path).post(body);
        if (token != null) req.header("Authorization", "Bearer " + token);
        return execute(req.build(), responseType);
    }

    public void downloadFile(String url, Path dest, DownloadProgress progress) throws IOException {
        var req = new Request.Builder().url(url).get().build();
        try (var resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) throw new IOException("Download failed: HTTP " + resp.code());
            var body = resp.body();
            if (body == null) throw new IOException("Empty response for: " + url);
            long total = body.contentLength();
            var tempFile = dest.resolveSibling(dest.getFileName() + ".download");
            try {
                try (var in = body.byteStream();
                     var out = new FileOutputStream(tempFile.toFile())) {
                    byte[] buf = new byte[65536];
                    long downloaded = 0;
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                        downloaded += n;
                        if (progress != null && total > 0) {
                            progress.onProgress(downloaded, total);
                        }
                    }
                    out.flush();
                }
                if (dest.toFile().exists()) dest.toFile().delete();
                tempFile.toFile().renameTo(dest.toFile());
            } catch (Exception e) {
                tempFile.toFile().delete();
                throw e;
            }
        }
    }

    public OkHttpClient rawClient() { return http; }

    private <T> T execute(Request request, Class<T> responseType) throws IOException {
        try (var resp = http.newCall(request).execute()) {
            var body = resp.body();
            var bodyStr = body != null ? body.string() : "";
            if (!resp.isSuccessful()) {
                throw new AuthException(resp.code(), extractUserMessage(bodyStr, resp.code()));
            }
            if (responseType == Void.class || responseType == void.class) return null;
            if (bodyStr.isBlank()) throw new IOException("Empty response body");
            return JSON.readValue(bodyStr, responseType);
        }
    }

    private static RequestBody jsonBody(Object body) throws IOException {
        return RequestBody.create(JSON.writeValueAsString(body), JSON_TYPE);
    }

    private static String extractUserMessage(String body, int code) {
        if (body == null || body.isBlank()) return "Ошибка сервера (" + code + ")";
        try {
            var node = JSON.readTree(body);
            for (var key : new String[]{"detail", "message", "Message", "error", "title"}) {
                var v = node.get(key);
                if (v != null && v.isTextual() && !v.asText().isBlank()) return v.asText();
            }
        } catch (Exception ignored) {}
        return body.length() > 200 ? "Ошибка сервера (" + code + ")" : body;
    }

    public static class AuthException extends IOException {
        private final int statusCode;
        public AuthException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
        public boolean isUnauthorized() { return statusCode == 401 || statusCode == 403; }
    }

    @FunctionalInterface
    public interface DownloadProgress {
        void onProgress(long downloaded, long total);
    }
}
