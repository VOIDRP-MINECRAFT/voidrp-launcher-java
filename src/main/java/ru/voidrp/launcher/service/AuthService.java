package ru.voidrp.launcher.service;

import ru.voidrp.launcher.config.AppConfig;
import ru.voidrp.launcher.model.AuthSession;
import ru.voidrp.launcher.model.api.*;

import java.io.IOException;
import java.util.*;

public class AuthService {
    private final ApiClient api;
    private final TokenStore tokenStore;
    private AuthSession session;

    public AuthService(ApiClient api, TokenStore tokenStore) {
        this.api = api;
        this.tokenStore = tokenStore;
    }

    public AuthSession getSession() { return session; }
    public boolean isAuthenticated() { return session != null && session.isAuthenticated(); }

    public AuthSession tryRestore() {
        var refreshToken = tokenStore.load();
        if (refreshToken == null) { session = null; return null; }
        try {
            var resp = api.post("auth/refresh",
                Map.of("refresh_token", refreshToken, "device_name", AppConfig.DEVICE_NAME),
                null, TokenPairResponse.class);
            session = fromTokenPair(resp);
            tokenStore.save(session.getRefreshToken());
            return session;
        } catch (ApiClient.AuthException ex) {
            if (ex.isUnauthorized()) { tokenStore.clear(); }
            session = null;
            return null;
        } catch (Exception e) {
            session = null;
            return null;
        }
    }

    public AuthSession login(String login, String password) throws IOException {
        var resp = api.post("auth/login",
            Map.of("login", login.trim(), "password", password, "device_name", AppConfig.DEVICE_NAME),
            null, TokenPairResponse.class);
        session = fromTokenPair(resp);
        tokenStore.save(session.getRefreshToken());
        return session;
    }

    public void reloadMe() throws IOException {
        if (session == null) throw new IllegalStateException("Not authenticated");
        var me = api.get("me", session.getAccessToken(), MeResponse.class);
        session.setUser(me.getUser());
        session.setPlayerAccount(me.getPlayerAccount());
        session.setSecurity(me.getSecurity() != null ? me.getSecurity() : new AccountSecurityDto());
    }

    public void logout() {
        try {
            if (session != null && session.getRefreshToken() != null) {
                api.post("auth/logout",
                    Map.of("refresh_token", session.getRefreshToken()),
                    null, Object.class);
            }
        } catch (Exception ignored) {}
        session = null;
        tokenStore.clear();
    }

    public PlayTicketResponse requestPlayTicket() throws IOException {
        requireAuth();
        return api.post("launcher/play-ticket",
            Map.of("launcher_version", AppConfig.VERSION, "launcher_platform", new LauncherPaths().platformRid()),
            session.getAccessToken(), PlayTicketResponse.class);
    }

    public DashboardResponse getDashboard() throws IOException {
        requireAuth();
        return api.get("launcher/me/dashboard", session.getAccessToken(), DashboardResponse.class);
    }

    public SkinResponse getSkin() throws IOException {
        requireAuth();
        return api.get("account/skin", session.getAccessToken(), SkinResponse.class);
    }

    public SkinResponse uploadSkin(java.nio.file.Path file, String modelVariant) throws IOException {
        requireAuth();
        var resp = api.postMultipart("account/skin", file, "file", modelVariant,
            session.getAccessToken(), SkinOperationResponse.class);
        return resp.getSkin();
    }

    public SkinResponse deleteSkin() throws IOException {
        requireAuth();
        var resp = api.delete("account/skin", session.getAccessToken(), SkinOperationResponse.class);
        return resp.getSkin();
    }

    public PreferencesResponse getPreferences() throws IOException {
        requireAuth();
        return api.get("launcher/me/prefs", session.getAccessToken(), PreferencesResponse.class);
    }

    public void saveModPrefs(List<String> disabledMods) throws IOException {
        requireAuth();
        api.put("launcher/me/prefs/mods",
            Map.of("disabled_mods", disabledMods),
            session.getAccessToken(), Object.class);
    }

    public void revokeOtherSessions() throws IOException {
        requireAuth();
        api.post("account/revoke-other-sessions",
            Map.of("refresh_token", session.getRefreshToken()),
            session.getAccessToken(), Object.class);
    }

    private void requireAuth() {
        if (!isAuthenticated()) throw new IllegalStateException("Not authenticated");
    }

    private AuthSession fromTokenPair(TokenPairResponse resp) {
        var s = new AuthSession();
        s.setAccessToken(resp.getAccessToken());
        s.setRefreshToken(resp.getRefreshToken());
        s.setUser(resp.getUser() != null ? resp.getUser() : new UserDto());
        s.setPlayerAccount(resp.getPlayerAccount() != null ? resp.getPlayerAccount() : new PlayerAccountDto());
        s.setSecurity(new AccountSecurityDto());
        return s;
    }

    // Inner DTO for skin operation responses
    private static class SkinOperationResponse {
        private String message = "";
        private SkinResponse skin = new SkinResponse();
        public String getMessage() { return message; }
        public void setMessage(String v) { message = v; }
        public SkinResponse getSkin() { return skin; }
        public void setSkin(SkinResponse v) { skin = v; }
    }
}
