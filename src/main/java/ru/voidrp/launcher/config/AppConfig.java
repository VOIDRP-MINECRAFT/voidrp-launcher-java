package ru.voidrp.launcher.config;

public final class AppConfig {
    public static final String VERSION = "1.0.0";
    public static final String DEVICE_NAME = "VoidRP Launcher Java";

    public static final String API_BASE_URL = "https://api.void-rp.ru/api/v1";
    public static final String PACK_MANIFEST_URL = "https://void-rp.ru/launcher/manifests/manifest.json";
    public static final String RUNTIME_SEED_URL = "https://void-rp.ru/launcher/runtime-seed";
    public static final String RUNTIME_MANIFEST_BASE_URL = "https://void-rp.ru/launcher/manifests";

    public static final String REGISTER_URL = "https://void-rp.ru/register";
    public static final String FORGOT_PASSWORD_URL = "https://void-rp.ru/forgot-password";
    public static final String VERIFY_EMAIL_URL = "https://void-rp.ru/verify-email";
    public static final String MAP_URL = "https://map.void-rp.ru";

    public static final String LAUNCHER_HMAC_SECRET =
        "86b03b8adbf76bc0dd0269651e2c0cc0bf523fb1b71b8b806c06394d3c5b9e67";

    private AppConfig() {}
}
