package ru.voidrp.launcher.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayTicketResponse {
    private String ticket = "";
    @JsonProperty("expires_at") private String expiresAt = "";
    @JsonProperty("minecraft_nickname") private String minecraftNickname = "";
    @JsonProperty("ttl_seconds") private int ttlSeconds;

    public String getTicket() { return ticket; }
    public void setTicket(String v) { ticket = v; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String v) { expiresAt = v; }
    public String getMinecraftNickname() { return minecraftNickname; }
    public void setMinecraftNickname(String v) { minecraftNickname = v; }
    public int getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(int v) { ttlSeconds = v; }
}
