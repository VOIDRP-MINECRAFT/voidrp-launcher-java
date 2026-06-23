package ru.voidrp.launcher.ui.views;

import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import ru.voidrp.launcher.App;
import ru.voidrp.launcher.config.AppConfig;
import ru.voidrp.launcher.model.LauncherManifest;
import ru.voidrp.launcher.model.api.DashboardResponse;
import ru.voidrp.launcher.ui.ErrorLogDialog;
import ru.voidrp.launcher.ui.MainWindow;

import java.util.concurrent.atomic.AtomicBoolean;

public class HomeView extends BorderPane {
    private final MainWindow window;
    private Button playBtn;
    private Label statusLbl;
    private ProgressBar playProgress;
    private Label progressDetailLbl;
    private Label nicknameLbl;
    private Label balanceLbl;
    private Label playtimeLbl;
    private Label pvpLbl;
    private Label nationLbl;
    private final AtomicBoolean busy = new AtomicBoolean(false);

    public HomeView(MainWindow window) {
        this.window = window;
        setStyle("-fx-background-color: #0f1117;");
        build();
        loadDashboard();
    }

    private void build() {
        var session = App.authService.getSession();
        var nickname = session != null && session.getPlayerAccount() != null
            ? session.getPlayerAccount().getMinecraftNickname() : "Игрок";

        // Header
        nicknameLbl = new Label(nickname);
        nicknameLbl.setStyle("-fx-text-fill: #e5e7eb; -fx-font-size: 22px; -fx-font-weight: bold;");
        var header = new HBox(12, nicknameLbl);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(28, 28, 12, 28));

        // Stats grid
        balanceLbl = statLabel("—");
        playtimeLbl = statLabel("—");
        pvpLbl = statLabel("—");
        nationLbl = statLabel("—");

        var statsGrid = new GridPane();
        statsGrid.setHgap(12);
        statsGrid.setVgap(12);
        statsGrid.setPadding(new Insets(0, 28, 20, 28));
        statsGrid.add(statCard("💰 Баланс", balanceLbl), 0, 0);
        statsGrid.add(statCard("⏱ Онлайн", playtimeLbl), 1, 0);
        statsGrid.add(statCard("⚔️ PvP убийства", pvpLbl), 0, 1);
        statsGrid.add(statCard("🏛 Нация", nationLbl), 1, 1);
        for (int i = 0; i < 2; i++) {
            var cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            statsGrid.getColumnConstraints().add(cc);
        }

        // Play section
        playBtn = new Button("▶  ЗАПУСТИТЬ ИГРУ");
        playBtn.setPrefWidth(300);
        playBtn.setPrefHeight(56);
        playBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 17px; " +
            "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(124,58,237,0.4), 12, 0, 0, 3);");
        playBtn.setOnMouseEntered(e -> playBtn.setStyle(playBtn.getStyle().replace("#7c3aed", "#6d28d9")));
        playBtn.setOnMouseExited(e -> playBtn.setStyle(playBtn.getStyle().replace("#6d28d9", "#7c3aed")));
        playBtn.setOnAction(e -> doPlay());

        statusLbl = new Label("Готов к запуску");
        statusLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        playProgress = new ProgressBar(0);
        playProgress.setPrefWidth(300);
        playProgress.setVisible(false);
        playProgress.setStyle("-fx-accent: #7c3aed;");

        progressDetailLbl = new Label("");
        progressDetailLbl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px; -fx-wrap-text: true;");
        progressDetailLbl.setMaxWidth(300);

        var logoutLink = new Hyperlink("Выйти из аккаунта");
        logoutLink.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 11px; -fx-border-color: transparent;");
        logoutLink.setOnAction(e -> doLogout());

        var playSection = new VBox(10, playBtn, playProgress, progressDetailLbl, statusLbl, logoutLink);
        playSection.setAlignment(Pos.CENTER);
        playSection.setPadding(new Insets(20, 28, 28, 28));

        var center = new VBox(header, statsGrid, new Separator(), playSection);
        setCenter(center);
    }

    private VBox statCard(String title, Label valueLabel) {
        var titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
        var card = new VBox(4, titleLbl, valueLabel);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: #16191f; -fx-background-radius: 8; -fx-border-color: #1f2937; -fx-border-radius: 8; -fx-border-width: 1;");
        return card;
    }

    private Label statLabel(String text) {
        var l = new Label(text);
        l.setStyle("-fx-text-fill: #e5e7eb; -fx-font-size: 18px; -fx-font-weight: bold;");
        return l;
    }

    private void loadDashboard() {
        Thread.ofVirtual().start(() -> {
            try {
                var dash = App.authService.getDashboard();
                Platform.runLater(() -> applyDashboard(dash));
            } catch (Exception ignored) {}
        });
    }

    private void applyDashboard(DashboardResponse d) {
        if (d == null) return;
        if (d.getPlayerStats() != null) {
            var s = d.getPlayerStats();
            balanceLbl.setText(String.format("%.0f", s.getCurrentBalance()));
            var mins = s.getTotalPlaytimeMinutes();
            playtimeLbl.setText(mins >= 60 ? (mins / 60) + "ч " + (mins % 60) + "м" : mins + "м");
            pvpLbl.setText(String.valueOf(s.getPvpKills()));
        }
        if (d.getNation() != null && d.getNation().getTitle() != null) {
            nationLbl.setText(d.getNation().getTitle());
        }
        if (d.getWalletBalance() > 0 && (d.getPlayerStats() == null || d.getPlayerStats().getCurrentBalance() == 0)) {
            balanceLbl.setText(String.format("%.0f", d.getWalletBalance()));
        }
    }

    private void doPlay() {
        if (!busy.compareAndSet(false, true)) return;
        setPlayEnabled(false, "Подготовка...");
        LauncherManifest[] manifestHolder = {null};

        Thread.ofVirtual().start(() -> {
            try {
                updateProgress("Java runtime", "Проверяем Java runtime...", 0);
                App.runtimeBootstrapService.ensureRuntime((msg, pct) ->
                    updateProgress("Java runtime", msg, pct));

                updateProgress("Манифест", "Загружаем manifest...", 5);
                var manifest = App.manifestService.load(AppConfig.PACK_MANIFEST_URL);
                manifestHolder[0] = manifest;

                var settings = App.settingsService.load();
                var disabled = new java.util.HashSet<>(settings.getDisabledMods());

                App.fileSyncService.sync(manifest, disabled, (rel, done, total, pct) -> {
                    var msg = rel.isBlank() ? "Синхронизация завершена" : rel;
                    updateProgress("Синхронизация", msg, pct);
                });

                updateProgress("Запуск", "Запрашиваем игровой ticket...", 95);
                String nickname;
                try {
                    var ticket = App.authService.requestPlayTicket();
                    nickname = ticket.getMinecraftNickname();
                    if (nickname == null || nickname.isBlank())
                        nickname = App.authService.getSession().requireNickname();
                    // Write ticket file so the NeoForge auth-bridge mod can read it.
                    // PlayTicketFile record requires: ticket, minecraftNickname, expiresAtUtc.
                    if (ticket.getTicket() != null && !ticket.getTicket().isBlank()) {
                        var stateFile = App.paths.state().resolve("play-ticket.json");
                        var nick4file = ticket.getMinecraftNickname() != null
                                ? ticket.getMinecraftNickname() : nickname;
                        var expiresAt = ticket.getExpiresAt() != null && !ticket.getExpiresAt().isBlank()
                                ? ticket.getExpiresAt()
                                : java.time.Instant.now().plusSeconds(ticket.getTtlSeconds() > 0
                                        ? ticket.getTtlSeconds() : 120).toString();
                        var proof = computeHmacSha256Hex(
                                AppConfig.LAUNCHER_HMAC_SECRET, ticket.getTicket());
                        var ticketMap = new java.util.LinkedHashMap<String, String>();
                        ticketMap.put("ticket", ticket.getTicket());
                        ticketMap.put("minecraftNickname", nick4file);
                        ticketMap.put("expiresAtUtc", expiresAt);
                        ticketMap.put("source", "java_launcher");
                        if (proof != null) ticketMap.put("launcherProof", proof);
                        var json = new com.fasterxml.jackson.databind.ObjectMapper()
                                .writeValueAsString(ticketMap);
                        java.nio.file.Files.writeString(stateFile, json);
                    }
                } catch (Exception ex) {
                    nickname = App.authService.getSession().requireNickname();
                }

                final var nick = nickname;
                final var man = manifest;
                final var maxRam = settings.getMaxRamMb();

                updateProgress("Запуск", "Запускаем Minecraft...", 99);
                Platform.runLater(() -> {
                    try {
                        var result = App.gameLaunchService.launch(nick, man, maxRam);
                        setPlayEnabled(true, "Minecraft запущен");
                        hideProgress();
                        busy.set(false);
                        monitorForCrash(result);
                    } catch (Exception ex) {
                        setPlayEnabled(true, "Ошибка: " + ex.getMessage());
                        hideProgress();
                        busy.set(false);
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setPlayEnabled(true, "Ошибка: " + ex.getMessage());
                    hideProgress();
                    busy.set(false);
                    ErrorLogDialog.showText("Ошибка запуска", ex.getMessage(), null);
                });
            }
        });
    }

    private void monitorForCrash(ru.voidrp.launcher.service.GameLaunchService.LaunchResult result) {
        Thread.ofVirtual().start(() -> {
            try {
                // Give the process a chance to start fully
                Thread.sleep(10_000);
                if (result.process().isAlive()) return;

                int code = result.process().exitValue();
                if (code == 0) return;

                // Process crashed — read the log and show dialog
                String log;
                try {
                    log = java.nio.file.Files.readString(result.logFile());
                } catch (Exception e) {
                    log = "Не удалось прочитать лог: " + e.getMessage();
                }
                final String logText = log;
                Platform.runLater(() ->
                    ErrorLogDialog.showText(
                        "Minecraft упал (код " + code + ")",
                        logText,
                        result.logFile()
                    )
                );
            } catch (InterruptedException ignored) {}
        });
    }

    private void updateProgress(String stage, String detail, double pct) {
        Platform.runLater(() -> {
            playProgress.setVisible(true);
            playProgress.setProgress(pct / 100.0);
            progressDetailLbl.setText(detail);
            statusLbl.setText(stage);
            window.setStatus(stage + ": " + detail);
            window.setProgress(pct, detail);
        });
    }

    private void hideProgress() {
        Platform.runLater(() -> {
            playProgress.setVisible(false);
            progressDetailLbl.setText("");
            window.hideProgress();
        });
    }

    private void setPlayEnabled(boolean enabled, String statusText) {
        Platform.runLater(() -> {
            playBtn.setDisable(!enabled);
            playBtn.setText(enabled ? "▶  ЗАПУСТИТЬ ИГРУ" : "⏳  " + statusText);
            statusLbl.setText(statusText);
        });
    }

    private static String computeHmacSha256Hex(String secret, String data) {
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            var bytes = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void doLogout() {
        Thread.ofVirtual().start(() -> {
            App.authService.logout();
            Platform.runLater(() -> window.showLogin());
        });
    }
}
