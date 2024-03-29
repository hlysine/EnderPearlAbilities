package io.github.henry_yslin.enderpearlabilities.abilities.fraggrenade;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.abilities.MultipleChargeAbilityWithDurationInfo;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class FragGrenadeAbilityInfo extends MultipleChargeAbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 3 * 20);
        config.addDefault("cooldown", 30 * 20);
        config.addDefault("charge", 2);
    }

    @Override
    public String getCodeName() {
        return "frag-grenade";
    }

    @Override
    public String getName() {
        return "Frag Grenade";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends";
    }

    @Override
    public String getDescription() {
        return "Throw frag grenades with accurate guides.";
    }

    @Override
    public String getUsage() {
        return "Hold an ender pearl to see the guide. Sneak to zoom in. Right click to throw.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public FragGrenadeAbility createInstance(String ownerName) {
        return new FragGrenadeAbility(plugin, this, ownerName);
    }

    public FragGrenadeAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
