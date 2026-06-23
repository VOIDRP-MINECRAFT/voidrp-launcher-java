package ru.voidrp.launcher.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MeResponse {
    private UserDto user = new UserDto();
    @JsonProperty("player_account") private PlayerAccountDto playerAccount = new PlayerAccountDto();
    private AccountSecurityDto security = new AccountSecurityDto();

    public UserDto getUser() { return user; }
    public void setUser(UserDto v) { user = v; }
    public PlayerAccountDto getPlayerAccount() { return playerAccount; }
    public void setPlayerAccount(PlayerAccountDto v) { playerAccount = v; }
    public AccountSecurityDto getSecurity() { return security; }
    public void setSecurity(AccountSecurityDto v) { security = v; }
}
