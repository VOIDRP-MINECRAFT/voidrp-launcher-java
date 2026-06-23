package ru.voidrp.launcher.ui.views;

import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import ru.voidrp.launcher.App;
import ru.voidrp.launcher.model.AuthSession;
import ru.voidrp.launcher.model.api.SkinResponse;
import ru.voidrp.launcher.ui.MainWindow;

import java.io.File;
import java.nio.file.Path;

public class AccountView extends VBox {
    private final MainWindow window;
    private Label skinStatusLbl;
    private Label messageLabel;
    private ImageView skinPreview;
    private Button uploadBtn;
    private Button deleteBtn;
    private ToggleGroup modelGroup;

    public AccountView(MainWindow window) {
        this.window = window;
        setStyle("-fx-background-color: #0f1117;");
        setPadding(new Insets(28));
        setSpacing(16);
        build();
        loadSkin();
    }

    private void build() {
        var title = new Label("Аккаунт");
        title.setStyle("-fx-text-fill: #e5e7eb; -fx-font-size: 22px; -fx-font-weight: bold;");

        var session = App.authService.getSession();

        // Profile info section
        var infoSection = new VBox(8);
        infoSection.setStyle("-fx-background-color: #16191f; -fx-background-radius: 8; -fx-padding: 16; " +
            "-fx-border-color: #1f2937; -fx-border-radius: 8; -fx-border-width: 1;");
        var infoTitle = new Label("Профиль");
        infoTitle.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");
        infoSection.getChildren().add(infoTitle);

        if (session != null) {
            if (session.getPlayerAccount() != null && !session.getPlayerAccount().getMinecraftNickname().isBlank())
                infoSection.getChildren().add(infoRow("Ник в Minecraft:", session.getPlayerAccount().getMinecraftNickname()));
            if (session.getUser() != null) {
                if (!session.getUser().getSiteLogin().isBlank())
                    infoSection.getChildren().add(infoRow("Логин:", session.getUser().getSiteLogin()));
                if (!session.getUser().getEmail().isBlank())
                    infoSection.getChildren().add(infoRow("Email:", session.getUser().getEmail()));
                infoSection.getChildren().add(infoRow("Email подтверждён:", session.getUser().isEmailVerified() ? "✓ Да" : "✗ Нет"));
            }
            if (session.getSecurity() != null) {
                infoSection.getChildren().add(infoRow("Активных сессий:", String.valueOf(session.getSecurity().getActiveRefreshSessions())));
            }
        }

        // Security actions
        var revokeBtn = new Button("Завершить другие сессии");
        revokeBtn.setStyle("-fx-background-color: #374151; -fx-text-fill: #9ca3af; -fx-font-size: 12px; " +
            "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 14;");
        revokeBtn.setOnAction(e -> {
            revokeBtn.setDisable(true);
            Thread.ofVirtual().start(() -> {
                try {
                    App.authService.revokeOtherSessions();
                    Platform.runLater(() -> showMessage("Другие сессии завершены."));
                } catch (Exception ex) {
                    Platform.runLater(() -> showMessage("Ошибка: " + ex.getMessage()));
                } finally {
                    Platform.runLater(() -> revokeBtn.setDisable(false));
                }
            });
        });
        infoSection.getChildren().add(revokeBtn);

        // Skin section
        var skinSection = new VBox(10);
        skinSection.setStyle("-fx-background-color: #16191f; -fx-background-radius: 8; -fx-padding: 16; " +
            "-fx-border-color: #1f2937; -fx-border-radius: 8; -fx-border-width: 1;");

        var skinTitle = new Label("Скин");
        skinTitle.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");

        skinStatusLbl = new Label("Загружаем...");
        skinStatusLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        skinPreview = new ImageView();
        skinPreview.setFitWidth(64);
        skinPreview.setFitHeight(64);
        skinPreview.setPreserveRatio(true);
        skinPreview.setVisible(false);

        var classic = new RadioButton("Classic");
        classic.setStyle("-fx-text-fill: #9ca3af;");
        var slim = new RadioButton("Slim (Alex)");
        slim.setStyle("-fx-text-fill: #9ca3af;");
        modelGroup = new ToggleGroup();
        classic.setToggleGroup(modelGroup);
        slim.setToggleGroup(modelGroup);
        classic.setSelected(true);
        var modelRow = new HBox(12, new Label("Модель:") {{ setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;"); }}, classic, slim);
        modelRow.setAlignment(Pos.CENTER_LEFT);

        uploadBtn = new Button("Загрузить скин");
        uploadBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 12px; " +
            "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 16;");
        uploadBtn.setOnAction(e -> chooseSkin());

        deleteBtn = new Button("Удалить скин");
        deleteBtn.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: #fca5a5; -fx-font-size: 12px; " +
            "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 16;");
        deleteBtn.setOnAction(e -> deleteSkin());

        var btnRow = new HBox(10, uploadBtn, deleteBtn);
        skinSection.getChildren().addAll(skinTitle, skinStatusLbl, skinPreview, modelRow, btnRow);

        messageLabel = new Label("");
        messageLabel.setStyle("-fx-text-fill: #34d399; -fx-font-size: 12px;");
        messageLabel.setVisible(false);

        getChildren().addAll(title, infoSection, skinSection, messageLabel);
    }

    private HBox infoRow(String key, String value) {
        var kl = new Label(key);
        kl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px; -fx-min-width: 160;");
        var vl = new Label(value);
        vl.setStyle("-fx-text-fill: #e5e7eb; -fx-font-size: 12px;");
        return new HBox(8, kl, vl);
    }

    private void loadSkin() {
        Thread.ofVirtual().start(() -> {
            try {
                var skin = App.authService.getSkin();
                Platform.runLater(() -> applySkin(skin));
            } catch (Exception ex) {
                Platform.runLater(() -> skinStatusLbl.setText("Скин не загружен"));
            }
        });
    }

    private void applySkin(SkinResponse skin) {
        if (skin == null || !skin.isHasSkin()) {
            skinStatusLbl.setText("Скин не установлен");
            skinPreview.setVisible(false);
            deleteBtn.setDisable(true);
            return;
        }
        skinStatusLbl.setText("Установлен • " + skin.getModelVariant() + " • " + skin.getWidth() + "×" + skin.getHeight());
        deleteBtn.setDisable(false);
        if ("slim".equals(skin.getModelVariant())) {
            for (var t : modelGroup.getToggles()) {
                if (t instanceof RadioButton rb && "Slim (Alex)".equals(rb.getText())) rb.setSelected(true);
            }
        }
        if (!skin.getHeadPreviewUrl().isBlank()) {
            Thread.ofVirtual().start(() -> {
                try {
                    var img = new Image(skin.getHeadPreviewUrl(), true);
                    Platform.runLater(() -> {
                        skinPreview.setImage(img);
                        skinPreview.setVisible(true);
                    });
                } catch (Exception ignored) {}
            });
        }
    }

    private void chooseSkin() {
        var fc = new FileChooser();
        fc.setTitle("Выберите PNG скин (64×64)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG изображения", "*.png"));
        File f = null;
        try { f = fc.showOpenDialog(null); } catch (Exception ignored) {}
        if (f != null) uploadSkin(f.toPath());
    }

    private void uploadSkin(Path file) {
        var model = modelGroup.getSelectedToggle() instanceof RadioButton rb && rb.getText().contains("Slim")
            ? "slim" : "classic";
        uploadBtn.setDisable(true);
        Thread.ofVirtual().start(() -> {
            try {
                var skin = App.authService.uploadSkin(file, model);
                Platform.runLater(() -> {
                    applySkin(skin);
                    showMessage("Скин сохранён.");
                    uploadBtn.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showMessage("Ошибка: " + ex.getMessage());
                    uploadBtn.setDisable(false);
                });
            }
        });
    }

    private void deleteSkin() {
        deleteBtn.setDisable(true);
        Thread.ofVirtual().start(() -> {
            try {
                var skin = App.authService.deleteSkin();
                Platform.runLater(() -> {
                    applySkin(skin);
                    showMessage("Скин удалён.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showMessage("Ошибка: " + ex.getMessage());
                    deleteBtn.setDisable(false);
                });
            }
        });
    }

    private void showMessage(String msg) {
        messageLabel.setText(msg);
        messageLabel.setVisible(true);
    }
}
