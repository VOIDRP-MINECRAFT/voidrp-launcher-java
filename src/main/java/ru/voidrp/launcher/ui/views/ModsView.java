package ru.voidrp.launcher.ui.views;

import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.voidrp.launcher.App;
import ru.voidrp.launcher.config.AppConfig;
import ru.voidrp.launcher.model.LauncherManifest;
import ru.voidrp.launcher.model.LauncherSettings;
import ru.voidrp.launcher.service.ModService;
import ru.voidrp.launcher.ui.MainWindow;

import java.util.List;

public class ModsView extends VBox {
    private final MainWindow window;
    private VBox modList;
    private Label statusLbl;
    private LauncherManifest manifest;
    private LauncherSettings settings;

    public ModsView(MainWindow window) {
        this.window = window;
        setStyle("-fx-background-color: #0f1117;");
        setPadding(new Insets(28));
        setSpacing(16);
        build();
        loadMods();
    }

    private void build() {
        var title = new Label("Моды");
        title.setStyle("-fx-text-fill: #e5e7eb; -fx-font-size: 22px; -fx-font-weight: bold;");

        var desc = new Label("Управление опциональными модами. Изменения вступят в силу при следующем запуске игры.");
        desc.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px; -fx-wrap-text: true;");

        statusLbl = new Label("Загружаем список модов...");
        statusLbl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");

        modList = new VBox(8);

        var scroll = new ScrollPane(modList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(title, desc, statusLbl, scroll);
    }

    private void loadMods() {
        Thread.ofVirtual().start(() -> {
            try {
                manifest = App.manifestService.load(AppConfig.PACK_MANIFEST_URL);
                settings = App.settingsService.load();
                var mods = App.modService.list(manifest, settings);
                Platform.runLater(() -> showMods(mods));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLbl.setText("Ошибка загрузки: " + ex.getMessage()));
            }
        });
    }

    private void showMods(List<ModService.ModInfo> mods) {
        modList.getChildren().clear();
        if (mods.isEmpty()) {
            statusLbl.setText("Нет опциональных модов.");
            return;
        }
        statusLbl.setText(mods.size() + " опциональных модов");

        for (var mod : mods) {
            modList.getChildren().add(buildModRow(mod));
        }
    }

    private HBox buildModRow(ModService.ModInfo mod) {
        var toggle = new CheckBox();
        toggle.setSelected(mod.enabled());
        toggle.setDisable(mod.required());
        toggle.setStyle("-fx-accent: #7c3aed;");

        var nameLabel = new Label(mod.displayName());
        nameLabel.setStyle("-fx-text-fill: " + (mod.enabled() ? "#e5e7eb" : "#6b7280") + "; -fx-font-size: 14px;");

        var badgeBox = new HBox(6);
        if (mod.required()) {
            var required = new Label("обязательный");
            required.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: #60a5fa; -fx-font-size: 10px; " +
                "-fx-background-radius: 4; -fx-padding: 2 6;");
            badgeBox.getChildren().add(required);
        } else {
            var opt = new Label("опциональный");
            opt.setStyle("-fx-background-color: #1a2e1a; -fx-text-fill: #4ade80; -fx-font-size: 10px; " +
                "-fx-background-radius: 4; -fx-padding: 2 6;");
            badgeBox.getChildren().add(opt);
        }

        String desc = mod.description() != null && !mod.description().isBlank() ? mod.description() : mod.path();
        var descLabel = new Label(desc);
        descLabel.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 11px; -fx-wrap-text: true;");

        var textBox = new VBox(2, new HBox(8, nameLabel, badgeBox), descLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        var row = new HBox(12, toggle, textBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));
        row.setStyle("-fx-background-color: #16191f; -fx-background-radius: 8; " +
            "-fx-border-color: #1f2937; -fx-border-radius: 8; -fx-border-width: 1;");

        toggle.setOnAction(e -> {
            if (manifest == null || settings == null) return;
            var enabled = toggle.isSelected();
            if (App.modService.toggle(mod.path(), enabled, manifest, settings)) {
                App.settingsService.save(settings);
                nameLabel.setStyle("-fx-text-fill: " + (enabled ? "#e5e7eb" : "#6b7280") + "; -fx-font-size: 14px;");
                // Sync to server
                Thread.ofVirtual().start(() -> {
                    try { App.authService.saveModPrefs(settings.getDisabledMods()); } catch (Exception ignored) {}
                });
            } else {
                toggle.setSelected(mod.enabled()); // revert
            }
        });

        return row;
    }
}
