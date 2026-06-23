package ru.voidrp.launcher.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerAccountDto {
    private String id = "";
    @JsonProperty("minecraft_nickname") private String minecraftNickname = "";
    @JsonProperty("nickname_locked") private boolean nicknameLocked;
    @JsonProperty("legacy_auth_enabled") private boolean legacyAuthEnabled;

    public String getId() { return id; }
    public void setId(String v) { id = v; }
    public String getMinecraftNickname() { return minecraftNickname; }
    public void setMinecraftNickname(String v) { minecraftNickname = v; }
    public boolean isNicknameLocked() { return nicknameLocked; }
    public void setNicknameLocked(boolean v) { nicknameLocked = v; }
    public boolean isLegacyAuthEnabled() { return legacyAuthEnabled; }
    public void setLegacyAuthEnabled(boolean v) { legacyAuthEnabled = v; }
}
