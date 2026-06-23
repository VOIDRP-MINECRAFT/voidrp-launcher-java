package ru.voidrp.launcher.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DashboardResponse {
    private NationDto nation;
    @JsonProperty("nation_stats") private NationStatsDto nationStats;
    @JsonProperty("player_stats") private PlayerStatsDto playerStats;
    @JsonProperty("recent_activity") private List<ActivityDto> recentActivity = new ArrayList<>();
    @JsonProperty("wallet_balance") private double walletBalance;

    public NationDto getNation() { return nation; }
    public void setNation(NationDto v) { nation = v; }
    public NationStatsDto getNationStats() { return nationStats; }
    public void setNationStats(NationStatsDto v) { nationStats = v; }
    public PlayerStatsDto getPlayerStats() { return playerStats; }
    public void setPlayerStats(PlayerStatsDto v) { playerStats = v; }
    public List<ActivityDto> getRecentActivity() { return recentActivity; }
    public void setRecentActivity(List<ActivityDto> v) { recentActivity = v != null ? v : new ArrayList<>(); }
    public double getWalletBalance() { return walletBalance; }
    public void setWalletBalance(double v) { walletBalance = v; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NationDto {
        private String id = "";
        private String slug = "";
        private String title = "";
        private String tag = "";
        @JsonProperty("accent_color") private String accentColor = "";
        private String role = "";
        @JsonProperty("icon_url") private String iconUrl = "";
        @JsonProperty("alliance_title") private String allianceTitle = "";
        @JsonProperty("alliance_tag") private String allianceTag = "";

        public String getId() { return id; }
        public void setId(String v) { id = v; }
        public String getSlug() { return slug; }
        public void setSlug(String v) { slug = v; }
        public String getTitle() { return title; }
        public void setTitle(String v) { title = v; }
        public String getTag() { return tag; }
        public void setTag(String v) { tag = v; }
        public String getAccentColor() { return accentColor; }
        public void setAccentColor(String v) { accentColor = v; }
        public String getRole() { return role; }
        public void setRole(String v) { role = v; }
        public String getIconUrl() { return iconUrl; }
        public void setIconUrl(String v) { iconUrl = v; }
        public String getAllianceTitle() { return allianceTitle; }
        public void setAllianceTitle(String v) { allianceTitle = v; }
        public String getAllianceTag() { return allianceTag; }
        public void setAllianceTag(String v) { allianceTag = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NationStatsDto {
        @JsonProperty("treasury_balance") private double treasuryBalance;
        @JsonProperty("pvp_kills") private int pvpKills;
        @JsonProperty("deaths") private int deaths;
        @JsonProperty("total_playtime_minutes") private int totalPlaytimeMinutes;
        @JsonProperty("territory_points") private int territoryPoints;

        public double getTreasuryBalance() { return treasuryBalance; }
        public void setTreasuryBalance(double v) { treasuryBalance = v; }
        public int getPvpKills() { return pvpKills; }
        public void setPvpKills(int v) { pvpKills = v; }
        public int getDeaths() { return deaths; }
        public void setDeaths(int v) { deaths = v; }
        public int getTotalPlaytimeMinutes() { return totalPlaytimeMinutes; }
        public void setTotalPlaytimeMinutes(int v) { totalPlaytimeMinutes = v; }
        public int getTerritoryPoints() { return territoryPoints; }
        public void setTerritoryPoints(int v) { territoryPoints = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlayerStatsDto {
        @JsonProperty("minecraft_nickname") private String minecraftNickname = "";
        @JsonProperty("total_playtime_minutes") private int totalPlaytimeMinutes;
        @JsonProperty("pvp_kills") private int pvpKills;
        @JsonProperty("mob_kills") private int mobKills;
        @JsonProperty("deaths") private int deaths;
        @JsonProperty("blocks_placed") private long blocksPlaced;
        @JsonProperty("blocks_broken") private long blocksBroken;
        @JsonProperty("current_balance") private double currentBalance;
        @JsonProperty("completed_quests") private int completedQuests;

        public String getMinecraftNickname() { return minecraftNickname; }
        public void setMinecraftNickname(String v) { minecraftNickname = v; }
        public int getTotalPlaytimeMinutes() { return totalPlaytimeMinutes; }
        public void setTotalPlaytimeMinutes(int v) { totalPlaytimeMinutes = v; }
        public int getPvpKills() { return pvpKills; }
        public void setPvpKills(int v) { pvpKills = v; }
        public int getMobKills() { return mobKills; }
        public void setMobKills(int v) { mobKills = v; }
        public int getDeaths() { return deaths; }
        public void setDeaths(int v) { deaths = v; }
        public long getBlocksPlaced() { return blocksPlaced; }
        public void setBlocksPlaced(long v) { blocksPlaced = v; }
        public long getBlocksBroken() { return blocksBroken; }
        public void setBlocksBroken(long v) { blocksBroken = v; }
        public double getCurrentBalance() { return currentBalance; }
        public void setCurrentBalance(double v) { currentBalance = v; }
        public int getCompletedQuests() { return completedQuests; }
        public void setCompletedQuests(int v) { completedQuests = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActivityDto {
        @JsonProperty("event_type") private String eventType = "";
        private String message = "";
        @JsonProperty("created_at") private String createdAt;

        public String getEventType() { return eventType; }
        public void setEventType(String v) { eventType = v; }
        public String getMessage() { return message; }
        public void setMessage(String v) { message = v; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String v) { createdAt = v; }
    }
}
