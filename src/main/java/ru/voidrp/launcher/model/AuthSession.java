package ru.voidrp.launcher.model;

import ru.voidrp.launcher.model.api.*;

public class AuthSession {
    private String accessToken = "";
    private String refreshToken = "";
    private UserDto user = new UserDto();
    private PlayerAccountDto playerAccount = new PlayerAccountDto();
    private AccountSecurityDto security = new AccountSecurityDto();

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String v) { accessToken = v; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String v) { refreshToken = v; }

    public UserDto getUser() { return user; }
    public void setUser(UserDto v) { user = v != null ? v : new UserDto(); }

    public PlayerAccountDto getPlayerAccount() { return playerAccount; }
    public void setPlayerAccount(PlayerAccountDto v) { playerAccount = v != null ? v : new PlayerAccountDto(); }

    public AccountSecurityDto getSecurity() { return security; }
    public void setSecurity(AccountSecurityDto v) { security = v != null ? v : new AccountSecurityDto(); }

    public boolean isAuthenticated() {
        return accessToken != null && !accessToken.isBlank()
            && refreshToken != null && !refreshToken.isBlank()
            && playerAccount != null && playerAccount.getMinecraftNickname() != null
            && !playerAccount.getMinecraftNickname().isBlank();
    }

    public String requireNickname() {
        if (!isAuthenticated()) throw new IllegalStateException("Not authenticated");
        return playerAccount.getMinecraftNickname();
    }
}
