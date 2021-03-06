package io.github.henry_yslin.enderpearlabilities.abilities.bloodhoundtactical;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class BloodhoundTacticalAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 10);
        config.addDefault("duration", 10 * 20);
        config.addDefault("cooldown", 20 * 20);
    }

    @Override
    public String getCodeName() {
        return "bloodhound-tactical";
    }

    @Override
    public String getName() {
        return "Eye of the Allfather";
    }

    @Override
    public String getOrigin() {
        return "Apex Legends - Bloodhound";
    }

    @Override
    public String getDescription() {
        return "Briefly reveal entities through all structures in front of you.\nPassive ability: See trails of entities moving nearby.";
    }

    @Override
    public String getUsage() {
        return "Right click to activate. Living entities are marked red while others are marked white. Scanned players are warned through a pop-up message.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.OffHand;
    }

    @Override
    public BloodhoundTacticalAbility createInstance(String ownerName) {
        return new BloodhoundTacticalAbility(plugin, this, ownerName);
    }

    public BloodhoundTacticalAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}
