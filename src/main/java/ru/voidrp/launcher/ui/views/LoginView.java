package ru.voidrp.launcher.ui.views;

import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import ru.voidrp.launcher.App;
import ru.voidrp.launcher.config.AppConfig;
import ru.voidrp.launcher.ui.MainWindow;

public class LoginView extends StackPane {
    private final MainWindow window;
    private TextField loginField;
    private PasswordField passField;
    private Button loginBtn;
    private Label errorLabel;

    public LoginView(MainWindow window) {
        this.window = window;
        setStyle("-fx-background-color: #0f1117;");
        build();
        tryAutoRestore();
    }

    private void build() {
        var card = new VBox(16);
        card.setMaxWidth(380);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setStyle("-fx-background-color: #16191f; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 4);");

        var title = new Label("VoidRP");
        title.setStyle("-fx-text-fill: #a855f7; -fx-font-size: 28px; -fx-font-weight: bold;");
        var subtitle = new Label("Войдите в аккаунт");
        subtitle.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px;");

        loginField = styledField("Логин");
        passField = styledPass("Пароль");

        loginBtn = new Button("Войти");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 12; -fx-background-radius: 8; -fx-cursor: hand;");
        loginBtn.setOnAction(e -> doLogin());

        errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px; -fx-wrap-text: true;");
        errorLabel.setTextAlignment(TextAlignment.CENTER);
        errorLabel.setVisible(false);

        var registerLink = new Hyperlink("Зарегистрироваться");
        registerLink.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px; -fx-border-color: transparent;");
        registerLink.setOnAction(e -> openUrl(AppConfig.REGISTER_URL));

        var forgotLink = new Hyperlink("Забыли пароль?");
        forgotLink.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px; -fx-border-color: transparent;");
        forgotLink.setOnAction(e -> openUrl(AppConfig.FORGOT_PASSWORD_URL));

        var linksBox = new HBox(16, registerLink, forgotLink);
        linksBox.setAlignment(Pos.CENTER);

        passField.setOnAction(e -> doLogin());
        loginField.setOnAction(e -> passField.requestFocus());

        card.getChildren().addAll(title, subtitle, loginField, passField, loginBtn, errorLabel, linksBox);
        getChildren().add(card);
    }

    private void tryAutoRestore() {
        loginBtn.setDisable(true);
        loginBtn.setText("Восстанавливаем сессию...");
        Thread.ofVirtual().start(() -> {
            try {
                var session = App.authService.tryRestore();
                if (session != null) {
                    try { App.authService.reloadMe(); } catch (Exception ignored) {}
                    Platform.runLater(() -> window.showHome());
                    return;
                }
            } catch (Exception ignored) {}
            Platform.runLater(() -> {
                loginBtn.setDisable(false);
                loginBtn.setText("Войти");
            });
        });
    }

    private void doLogin() {
        var login = loginField.getText().trim();
        var pass = passField.getText();
        if (login.isBlank() || pass.isBlank()) {
            showError("Введите логин и пароль");
            return;
        }
        setLoading(true);
        Thread.ofVirtual().start(() -> {
            try {
                App.authService.login(login, pass);
                try { App.authService.reloadMe(); } catch (Exception ignored) {}
                Platform.runLater(() -> window.showHome());
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError(ex.getMessage());
                    setLoading(false);
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        loginBtn.setDisable(loading);
        loginBtn.setText(loading ? "Входим..." : "Войти");
        if (loading) hideError();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
    }

    private TextField styledField(String prompt) {
        var f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: #0f1117; -fx-text-fill: #e5e7eb; -fx-prompt-text-fill: #4b5563; " +
            "-fx-border-color: #374151; -fx-border-radius: 6; -fx-background-radius: 6; " +
            "-fx-padding: 10 12; -fx-font-size: 14px;");
        return f;
    }

    private PasswordField styledPass(String prompt) {
        var f = new PasswordField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: #0f1117; -fx-text-fill: #e5e7eb; -fx-prompt-text-fill: #4b5563; " +
            "-fx-border-color: #374151; -fx-border-radius: 6; -fx-background-radius: 6; " +
            "-fx-padding: 10 12; -fx-font-size: 14px;");
        return f;
    }

    private void openUrl(String url) {
        try {
            var rt = Runtime.getRuntime();
            var os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) rt.exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            else if (os.contains("mac")) rt.exec(new String[]{"open", url});
            else rt.exec(new String[]{"xdg-open", url});
        } catch (Exception ignored) {}
    }
}
