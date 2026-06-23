package ru.voidrp.launcher.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountSecurityDto {
    @JsonProperty("active_refresh_sessions") private int activeRefreshSessions;
    @JsonProperty("must_use_launcher") private boolean mustUseLauncher;
    @JsonProperty("legacy_hash_present") private boolean legacyHashPresent;
    @JsonProperty("legacy_ready") private boolean legacyReady;

    public int getActiveRefreshSessions() { return activeRefreshSessions; }
    public void setActiveRefreshSessions(int v) { activeRefreshSessions = v; }
    public boolean isMustUseLauncher() { return mustUseLauncher; }
    public void setMustUseLauncher(boolean v) { mustUseLauncher = v; }
    public boolean isLegacyHashPresent() { return legacyHashPresent; }
    public void setLegacyHashPresent(boolean v) { legacyHashPresent = v; }
    public boolean isLegacyReady() { return legacyReady; }
    public void setLegacyReady(boolean v) { legacyReady = v; }
}
