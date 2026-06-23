package ru.voidrp.launcher.ui.views;

import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.voidrp.launcher.App;
import ru.voidrp.launcher.config.AppConfig;
import ru.voidrp.launcher.model.LauncherSettings;
import ru.voidrp.launcher.ui.MainWindow;

public class SettingsView extends VBox {
    private final MainWindow window;
    private Slider ramSlider;
    private Label ramLabel;
    private Label messageLabel;

    public SettingsView(MainWindow window) {
        this.window = window;
        setStyle("-fx-background-color: #0f1117;");
        setPadding(new Insets(28));
        setSpacing(20);
        build();
    }

    private void build() {
        var title = new Label("Настройки");
        title.setStyle("-fx-text-fill: #e5e7eb; -fx-font-size: 22px; -fx-font-weight: bold;");

        var settings = App.settingsService.load();

        // RAM section
        var ramSection = new VBox(10);
        ramSection.setStyle("-fx-background-color: #16191f; -fx-background-radius: 8; -fx-padding: 16; -fx-border-color: #1f2937; -fx-border-radius: 8; -fx-border-width: 1;");

        var ramTitle = new Label("Оперативная память (RAM)");
        ramTitle.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");

        ramLabel = new Label(formatRam(settings.getMaxRamMb()));
        ramLabel.setStyle("-fx-text-fill: #e5e7eb; -fx-font-size: 18px; -fx-font-weight: bold;");

        ramSlider = new Slider(2048, 16384, settings.getMaxRamMb());
        ramSlider.setMajorTickUnit(1024);
        ramSlider.setMinorTickCount(1);
        ramSlider.setSnapToTicks(true);
        ramSlider.setShowTickMarks(true);
        ramSlider.setStyle("-fx-accent: #7c3aed;");
        ramSlider.valueProperty().addListener((obs, old, nv) -> {
            int v = (int) (Math.round(nv.doubleValue() / 512.0) * 512);
            if (v < 2048) v = 2048;
            if (v > 16384) v = 16384;
            ramSlider.setValue(v);
            ramLabel.setText(formatRam(v));
        });

        var saveBtn = new Button("Сохранить");
        saveBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 13px; " +
            "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 20;");
        saveBtn.setOnAction(e -> {
            var s = App.settingsService.load();
            s.setMaxRamMb((int) ramSlider.getValue());
            App.settingsService.save(s);
            showMessage("Настройки сохранены.");
        });

        var resetBtn = new Button("Сбросить");
        resetBtn.setStyle("-fx-background-color: #374151; -fx-text-fill: #9ca3af; -fx-font-size: 13px; " +
            "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 20;");
        resetBtn.setOnAction(e -> {
            var s = App.settingsService.load();
            s.setMaxRamMb(4096);
            App.settingsService.save(s);
            ramSlider.setValue(4096);
            showMessage("Настройки сброшены.");
        });

        var btnRow = new HBox(10, saveBtn, resetBtn);
        ramSection.getChildren().addAll(ramTitle, ramLabel, ramSlider, btnRow);

        // Repair section
        var repairSection = new VBox(10);
        repairSection.setStyle("-fx-background-color: #16191f; -fx-background-radius: 8; -fx-padding: 16; -fx-border-color: #1f2937; -fx-border-radius: 8; -fx-border-width: 1;");

        var repairTitle = new Label("Восстановление клиента");
        repairTitle.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");
        var repairDesc = new Label("Удаляет все managed-файлы и state. При следующем запуске будет выполнена полная синхронизация.");
        repairDesc.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px; -fx-wrap-text: true;");

        var repairBtn = new Button("Восстановить клиент");
        repairBtn.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: #fca5a5; -fx-font-size: 13px; " +
            "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 20;");
        repairBtn.setOnAction(e -> doRepair(repairBtn));
        repairSection.getChildren().addAll(repairTitle, repairDesc, repairBtn);

        // Paths info section
        var pathSection = new VBox(8);
        pathSection.setStyle("-fx-background-color: #16191f; -fx-background-radius: 8; -fx-padding: 16; -fx-border-color: #1f2937; -fx-border-radius: 8; -fx-border-width: 1;");
        var pathTitle = new Label("Директории");
        pathTitle.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");
        pathSection.getChildren().add(pathTitle);
        pathSection.getChildren().add(pathRow("Данные:", App.paths.base().toString()));
        pathSection.getChildren().add(pathRow("Игра:", App.paths.game().toString()));
        pathSection.getChildren().add(pathRow("Версия лаунчера:", AppConfig.VERSION));

        messageLabel = new Label("");
        messageLabel.setStyle("-fx-text-fill: #34d399; -fx-font-size: 12px;");
        messageLabel.setVisible(false);

        getChildren().addAll(title, ramSection, repairSection, pathSection, messageLabel);
    }

    private HBox pathRow(String key, String value) {
        var kl = new Label(key);
        kl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px; -fx-min-width: 120;");
        var vl = new Label(value);
        vl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px; -fx-wrap-text: true;");
        vl.setMaxWidth(400);
        return new HBox(8, kl, vl);
    }

    private void doRepair(Button btn) {
        btn.setDisable(true);
        btn.setText("Восстанавливаем...");
        Thread.ofVirtual().start(() -> {
            try {
                repair();
                Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("Восстановить клиент");
                    showMessage("Ремонт завершён. Следующий запуск выполнит полную синхронизацию.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("Восстановить клиент");
                    showMessage("Ошибка: " + ex.getMessage());
                });
            }
        });
    }

    private void repair() throws Exception {
        var stateDir = App.paths.state();
        if (java.nio.file.Files.isDirectory(stateDir)) {
            try (var stream = java.nio.file.Files.list(stateDir)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".json")
                        && !p.getFileName().toString().equals("launcher-auth.json")
                        && !p.getFileName().toString().equals("hash-cache.json")
                        && !p.getFileName().toString().equals("runtime.stamp"))
                    .forEach(p -> { try { java.nio.file.Files.delete(p); } catch (Exception ignored) {} });
            }
        }
        // Delete .download temp files
        if (java.nio.file.Files.isDirectory(App.paths.game())) {
            try (var walk = java.nio.file.Files.walk(App.paths.game())) {
                walk.filter(p -> p.getFileName().toString().endsWith(".download"))
                    .forEach(p -> { try { java.nio.file.Files.delete(p); } catch (Exception ignored) {} });
            }
        }
    }

    private void showMessage(String msg) {
        messageLabel.setText(msg);
        messageLabel.setVisible(true);
    }

    private static String formatRam(int mb) {
        if (mb >= 1024) return String.format("%.1f GB (%d MB)", mb / 1024.0, mb);
        return mb + " MB";
    }
}
