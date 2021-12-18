package io.github.henry_yslin.enderpearlabilities.abilities.necromancer;

import io.github.henry_yslin.enderpearlabilities.Instantiable;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityWithDurationInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

@Instantiable
public class SkeletonArmyAbilityInfo extends AbilityWithDurationInfo {

    @Override
    public void writeConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 60);
        config.addDefault("duration", 100);
        config.addDefault("cooldown", 1800);
    }

    @Override
    public String getCodeName() {
        return "skeleton-army";
    }

    @Override
    public String getName() {
        return "Skeleton Army";
    }

    @Override
    public String getOrigin() {
        return "Clash Royale";
    }

    @Override
    public String getDescription() {
        return "Summon skeletons to fight whatever the player is looking at. Summoned skeletons obey commands until death.\nPassive ability: No skeletons will ever actively attack the player.";
    }

    @Override
    public String getUsage() {
        return "Right click to summon skeletons. They target the entity that is currently under the player's crosshair. If the skeletons are summoned during daytime, they have a higher chance of wearing helmets.";
    }

    @Override
    public ActivationHand getActivation() {
        return ActivationHand.MainHand;
    }

    @Override
    public SkeletonArmyAbility createInstance(String ownerName) {
        return new SkeletonArmyAbility(plugin, this, ownerName);
    }

    public SkeletonArmyAbilityInfo(Plugin plugin) {
        super(plugin);
    }
}