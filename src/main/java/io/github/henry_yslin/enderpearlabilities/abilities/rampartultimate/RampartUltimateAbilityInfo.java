package io.github.henry_yslin.enderpearlabilities.abilities.rampartultimate;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithMagazineInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class RampartUltimateAbilityInfo extends AbilityWithMagazineInfo {

    protected int spinUp;

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 20);
        config.addDefault("spin-up", 20);
        config.addDefault("magazine-size", 200);
        config.addDefault("base-cooldown", 5 * 20);
        config.addDefault("cooldown-per-shot", 11);
    }

    @Override
    public String getCodeName() {
        return "rampart-ultimate";
    }

    @Override
    public String getName() {
        return "Mobile Minigun \"Sheila\"";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Rampart";
    }

    @Override
    public String getDescription() {
        return "Wield a mobile minigun with a single high capacity magazine.";
    }

    @Override
    public String getUsage() {
        return "Right click to bring up Sheila. Sneak to spin up and fire, switch away from holding ender pearls to holster the minigun. Right click with the ender pearl again to start cooldown. Cooldown is increased by the number of shots fired.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    public int getSpinUp() {
        return spinUp;
    }

    @Override
    protected void readFromConfig(ConfigurationSection config) {
        super.readFromConfig(config);
        spinUp = config.getInt("spin-up");
    }

    @Override
    public RampartUltimateAbility createInstance(String ownerName) {
        return new RampartUltimateAbility(plugin, this, ownerName);
    }

    public RampartUltimateAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
