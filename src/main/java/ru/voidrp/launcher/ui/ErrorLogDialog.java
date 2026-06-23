package ru.voidrp.launcher.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;

public class ErrorLogDialog {

    public static void show(String title, Path logFile) {
        String content;
        try {
            content = Files.readString(logFile);
        } catch (Exception e) {
            content = "(не удалось прочитать лог: " + e.getMessage() + ")";
        }
        showText(title, content, logFile);
    }

    public static void showText(String title, String text, Path logFile) {
        final String finalText = text;
        Platform.runLater(() -> {
            var stage = new Stage();
            stage.setTitle(title);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setWidth(820);
            stage.setHeight(560);
            stage.setMinWidth(500);
            stage.setMinHeight(300);

            // Log area
            var logArea = new TextArea(finalText);
            logArea.setEditable(false);
            logArea.setWrapText(false);
            logArea.setStyle(
                "-fx-control-inner-background: #0d1117; " +
                "-fx-text-fill: #e5e7eb; " +
                "-fx-font-family: 'Consolas', 'Monospaced'; " +
                "-fx-font-size: 12px; " +
                "-fx-border-color: #1f2937; " +
                "-fx-border-width: 1;"
            );
            VBox.setVgrow(logArea, Priority.ALWAYS);

            // Scroll to bottom so the error is visible first
            logArea.setScrollTop(Double.MAX_VALUE);

            // Buttons
            var copyBtn = new Button("Копировать лог");
            copyBtn.setStyle(
                "-fx-background-color: #374151; -fx-text-fill: #e5e7eb; " +
                "-fx-font-size: 13px; -fx-background-radius: 6; -fx-cursor: hand; " +
                "-fx-padding: 8 18 8 18;"
            );
            copyBtn.setOnAction(e -> {
                var cc = new ClipboardContent();
                cc.putString(finalText);
                Clipboard.getSystemClipboard().setContent(cc);
                copyBtn.setText("Скопировано ✓");
                copyBtn.setDisable(true);
            });

            var openFolderBtn = new Button("Открыть папку логов");
            openFolderBtn.setStyle(
                "-fx-background-color: #1f2937; -fx-text-fill: #9ca3af; " +
                "-fx-font-size: 13px; -fx-background-radius: 6; -fx-cursor: hand; " +
                "-fx-padding: 8 18 8 18;"
            );
            openFolderBtn.setOnAction(e -> {
                try {
                    var dir = logFile != null ? logFile.getParent() : null;
                    if (dir != null) java.awt.Desktop.getDesktop().open(dir.toFile());
                } catch (Exception ignored) {}
            });
            if (logFile == null) openFolderBtn.setVisible(false);

            var closeBtn = new Button("Закрыть");
            closeBtn.setStyle(
                "-fx-background-color: #7c3aed; -fx-text-fill: white; " +
                "-fx-font-size: 13px; -fx-background-radius: 6; -fx-cursor: hand; " +
                "-fx-padding: 8 18 8 18;"
            );
            closeBtn.setOnAction(e -> stage.close());

            var btnBar = new HBox(10, copyBtn, openFolderBtn, new Region(), closeBtn);
            HBox.setHgrow(btnBar.getChildren().get(2), Priority.ALWAYS);
            btnBar.setAlignment(Pos.CENTER_LEFT);
            btnBar.setPadding(new Insets(10, 0, 0, 0));

            var root = new VBox(10, logArea, btnBar);
            root.setPadding(new Insets(16));
            root.setStyle("-fx-background-color: #0f1117;");

            stage.setScene(new Scene(root));
            stage.getScene().getStylesheets(); // reset default stylesheet
            stage.show();
        });
    }
}
