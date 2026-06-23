package ru.voidrp.launcher.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SkinResponse {
    @JsonProperty("has_skin") private boolean hasSkin;
    @JsonProperty("model_variant") private String modelVariant = "classic";
    @JsonProperty("skin_url") private String skinUrl = "";
    @JsonProperty("head_preview_url") private String headPreviewUrl = "";
    @JsonProperty("body_preview_url") private String bodyPreviewUrl = "";
    private int width;
    private int height;
    private String sha256 = "";
    @JsonProperty("updated_at") private String updatedAt;

    public boolean isHasSkin() { return hasSkin; }
    public void setHasSkin(boolean v) { hasSkin = v; }
    public String getModelVariant() { return modelVariant; }
    public void setModelVariant(String v) { modelVariant = v; }
    public String getSkinUrl() { return skinUrl; }
    public void setSkinUrl(String v) { skinUrl = v; }
    public String getHeadPreviewUrl() { return headPreviewUrl; }
    public void setHeadPreviewUrl(String v) { headPreviewUrl = v; }
    public String getBodyPreviewUrl() { return bodyPreviewUrl; }
    public void setBodyPreviewUrl(String v) { bodyPreviewUrl = v; }
    public int getWidth() { return width; }
    public void setWidth(int v) { width = v; }
    public int getHeight() { return height; }
    public void setHeight(int v) { height = v; }
    public String getSha256() { return sha256; }
    public void setSha256(String v) { sha256 = v; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String v) { updatedAt = v; }
}
