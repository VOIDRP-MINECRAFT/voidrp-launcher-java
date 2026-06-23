package ru.voidrp.launcher;

import javafx.application.Application;
import javafx.stage.Stage;
import ru.voidrp.launcher.config.AppConfig;
import ru.voidrp.launcher.service.*;
import ru.voidrp.launcher.ui.MainWindow;

public class App extends Application {
    // Shared services (singleton-per-app)
    public static LauncherPaths paths;
    public static HashService hash;
    public static TokenStore tokenStore;
    public static SettingsService settingsService;
    public static ApiClient apiClient;
    public static AuthService authService;
    public static ManifestService manifestService;
    public static FileSyncService fileSyncService;
    public static RuntimeBootstrapService runtimeBootstrapService;
    public static GameLaunchService gameLaunchService;
    public static ModService modService;

    @Override
    public void init() {
        paths = new LauncherPaths();
        hash = new HashService(paths);
        tokenStore = new TokenStore(paths);
        settingsService = new SettingsService(paths);
        apiClient = new ApiClient(AppConfig.API_BASE_URL);
        authService = new AuthService(apiClient, tokenStore);
        manifestService = new ManifestService();
        fileSyncService = new FileSyncService(paths, hash);
        runtimeBootstrapService = new RuntimeBootstrapService(paths, hash,
            AppConfig.RUNTIME_SEED_URL, AppConfig.RUNTIME_MANIFEST_BASE_URL);
        gameLaunchService = new GameLaunchService(paths);
        modService = new ModService();
    }

    @Override
    public void start(Stage stage) {
        new MainWindow(stage).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
