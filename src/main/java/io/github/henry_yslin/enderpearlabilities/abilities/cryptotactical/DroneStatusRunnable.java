package io.github.henry_yslin.enderpearlabilities.abilities.cryptotactical;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class DroneStatusRunnable extends AbilityRunnable {

    final Ability ability;
    final Player player;
    final AtomicReference<LivingEntity> drone;
    final AtomicBoolean chargingUp;
    final AtomicBoolean abilityActive;

    public DroneStatusRunnable(Ability ability, Player player, AtomicReference<LivingEntity> drone, AtomicBoolean chargingUp, AtomicBoolean abilityActive) {
        this.ability = ability;
        this.player = player;
        this.drone = drone;
        this.chargingUp = chargingUp;
        this.abilityActive = abilityActive;
    }

    @Override
    public void tick() {
        if (!ability.getCooldown().isAbilityUsable()) return;
        if (chargingUp.get()) return;
        if (abilityActive.get()) return;
        boolean mainHandPearl = player.getInventory().getItemInMainHand().getType() == Material.ENDER_PEARL;
        boolean offHandPearl = player.getInventory().getItemInOffHand().getType() == Material.ENDER_PEARL;
        if (ability.getInfo().getActivation() == ActivationHand.MainHand && mainHandPearl ||
                ability.getInfo().getActivation() == ActivationHand.OffHand && offHandPearl) {
            if (drone.get() != null && drone.get().isValid()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.LIGHT_PURPLE + "Enter existing drone, sneak to recall"));
            } else {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.LIGHT_PURPLE + "Deploy new drone"));
            }
        }
    }
}
