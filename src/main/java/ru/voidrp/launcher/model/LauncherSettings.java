package ru.voidrp.launcher.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LauncherSettings {
    private int maxRamMb = 4096;
    private List<String> disabledMods = new ArrayList<>();

    public int getMaxRamMb() { return maxRamMb; }
    public void setMaxRamMb(int v) { maxRamMb = v; }

    public List<String> getDisabledMods() { return disabledMods; }
    public void setDisabledMods(List<String> v) { disabledMods = v != null ? v : new ArrayList<>(); }
}
