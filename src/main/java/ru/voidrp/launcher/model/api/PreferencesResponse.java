package ru.voidrp.launcher.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PreferencesResponse {
    @JsonProperty("disabled_mods") private List<String> disabledMods = new ArrayList<>();

    public List<String> getDisabledMods() { return disabledMods; }
    public void setDisabledMods(List<String> v) { disabledMods = v != null ? v : new ArrayList<>(); }
}
