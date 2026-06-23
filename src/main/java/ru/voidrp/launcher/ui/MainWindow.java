package ru.voidrp.launcher.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import ru.voidrp.launcher.App;
import ru.voidrp.launcher.config.AppConfig;
import ru.voidrp.launcher.ui.views.*;

public class MainWindow {
    private final Stage stage;
    private BorderPane root;
    private StackPane contentArea;
    private VBox sidebar;
    private Label statusLabel;
    private ProgressBar progressBar;
    private Label progressLabel;

    private String currentView = "";

    public MainWindow(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #0f1117;");

        sidebar = buildSidebar();
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #0f1117;");

        var statusBar = buildStatusBar();

        root.setLeft(sidebar);
        root.setCenter(contentArea);
        root.setBottom(statusBar);

        var scene = new Scene(root, 1000, 640);
        try {
            var css = getClass().getResource("/css/launcher.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}

        stage.setScene(scene);
        stage.setTitle("VoidRP Launcher");
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setOnCloseRequest(e -> Platform.exit());
        stage.show();

        showLogin();
    }

    private VBox buildSidebar() {
        var nav = new VBox(4);
        nav.setPadding(new Insets(0, 0, 0, 0));
        nav.setStyle("-fx-background-color: #0a0b10; -fx-pref-width: 190;");
        nav.setAlignment(Pos.TOP_LEFT);

        var brand = new Label("VoidRP");
        brand.setStyle("-fx-text-fill: #a855f7; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 20 16 10 16;");
        var sub = new Label("Лаунчер v" + AppConfig.VERSION);
        sub.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 11px; -fx-padding: 0 16 16 16;");

        nav.getChildren().addAll(brand, sub, new Separator());

        var spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        nav.getChildren().addAll(
            navBtn("🏠  Главная", "home"),
            navBtn("🎮  Моды", "mods"),
            navBtn("👤  Аккаунт", "account"),
            navBtn("⚙️  Настройки", "settings"),
            spacer
        );

        return nav;
    }

    private Button navBtn(String text, String viewId) {
        var btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPadding(new Insets(12, 16, 12, 16));
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 0;");
        btn.setOnMouseEntered(e -> {
            if (!viewId.equals(currentView))
                btn.setStyle("-fx-background-color: #1f2937; -fx-text-fill: #e5e7eb; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 0;");
        });
        btn.setOnMouseExited(e -> {
            if (!viewId.equals(currentView))
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 0;");
        });
        btn.setOnAction(e -> navigateTo(viewId));
        btn.setUserData(viewId);
        return btn;
    }

    private HBox buildStatusBar() {
        statusLabel = new Label("Инициализация...");
        statusLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(180);
        progressBar.setVisible(false);
        progressBar.setStyle("-fx-accent: #7c3aed;");

        progressLabel = new Label("");
        progressLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");

        var bar = new HBox(10, statusLabel, new Region(), progressBar, progressLabel);
        HBox.setHgrow(bar.getChildren().get(1), Priority.ALWAYS);
        bar.setPadding(new Insets(6, 12, 6, 12));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #060709; -fx-border-color: #1f2937; -fx-border-width: 1 0 0 0;");
        return bar;
    }

    public void showLogin() {
        setContent(new LoginView(this));
        sidebar.setVisible(false);
        sidebar.setManaged(false);
        currentView = "login";
    }

    public void showHome() {
        sidebar.setVisible(true);
        sidebar.setManaged(true);
        navigateTo("home");
    }

    public void navigateTo(String viewId) {
        if (!App.authService.isAuthenticated() && !viewId.equals("login")) return;
        highlightNav(viewId);
        currentView = viewId;
        switch (viewId) {
            case "home" -> setContent(new HomeView(this));
            case "mods" -> setContent(new ModsView(this));
            case "account" -> setContent(new AccountView(this));
            case "settings" -> setContent(new SettingsView(this));
        }
    }

    private void highlightNav(String viewId) {
        for (var node : sidebar.getChildren()) {
            if (node instanceof Button btn && viewId.equals(btn.getUserData())) {
                btn.setStyle("-fx-background-color: #1e1040; -fx-text-fill: #a855f7; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 0;");
            } else if (node instanceof Button btn) {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9ca3af; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 0;");
            }
        }
    }

    private void setContent(javafx.scene.Node node) {
        contentArea.getChildren().setAll(node);
    }

    public void setStatus(String msg) {
        Platform.runLater(() -> statusLabel.setText(msg));
    }

    public void setProgress(double percent, String detail) {
        Platform.runLater(() -> {
            progressBar.setVisible(percent >= 0 && percent < 100);
            progressBar.setProgress(percent / 100.0);
            progressLabel.setText(percent >= 0 && percent < 100 ? String.format("%.0f%%", percent) : "");
        });
    }

    public void hideProgress() {
        Platform.runLater(() -> {
            progressBar.setVisible(false);
            progressLabel.setText("");
        });
    }
}
