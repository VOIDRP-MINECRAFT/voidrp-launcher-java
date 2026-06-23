package ru.voidrp.launcher.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenPairResponse {
    @JsonProperty("access_token") private String accessToken = "";
    @JsonProperty("refresh_token") private String refreshToken = "";
    private UserDto user = new UserDto();
    @JsonProperty("player_account") private PlayerAccountDto playerAccount = new PlayerAccountDto();

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String v) { accessToken = v; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String v) { refreshToken = v; }
    public UserDto getUser() { return user; }
    public void setUser(UserDto v) { user = v; }
    public PlayerAccountDto getPlayerAccount() { return playerAccount; }
    public void setPlayerAccount(PlayerAccountDto v) { playerAccount = v; }
}
