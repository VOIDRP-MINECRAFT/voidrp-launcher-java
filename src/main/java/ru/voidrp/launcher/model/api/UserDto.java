package ru.voidrp.launcher.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDto {
    private String id = "";
    @JsonProperty("site_login") private String siteLogin = "";
    private String email = "";
    @JsonProperty("email_verified") private boolean emailVerified;
    @JsonProperty("is_active") private boolean isActive;

    public String getId() { return id; }
    public void setId(String v) { id = v; }
    public String getSiteLogin() { return siteLogin; }
    public void setSiteLogin(String v) { siteLogin = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean v) { emailVerified = v; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean v) { isActive = v; }
}
