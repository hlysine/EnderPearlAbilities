package io.github.henry_yslin.enderpearlabilities.abilities.valkyrietactical;

import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.utils.MathUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class VTOLJetsRunnable extends AbilityRunnable {

    static final double MAX_FUEL = 140;
    static final int REGEN_DELAY = 160;
    static final double REGEN_SPEED = 140d / 200d;
    static final float FLY_SPEED = 0.04f;

    final Player player;
    BossBar bossbar;
    boolean velocityStable = false;
    boolean wasFlying;
    double previousY;
    double fuel;
    int regenTicks;

    public VTOLJetsRunnable(Player player) {
        this.player = player;
    }

    @Override
    protected void start() {
        super.start();
        bossbar = Bukkit.createBossBar(ChatColor.GREEN + "VTOL Jets", BarColor.GREEN, BarStyle.SOLID);
        bossbar.addPlayer(player);
        wasFlying = player.isFlying();
        previousY = player.getLocation().getY();
        regenTicks = REGEN_DELAY;
        fuel = 0;
    }

    @Override
    public void tick() {
        ItemStack chestplate = player.getInventory().getChestplate();
        if ((chestplate != null && chestplate.getType() == Material.ELYTRA)) {
            player.setAllowFlight(false);
            bossbar.setVisible(false);
            return;
        } else if (player.getFallDistance() >= 3) {
            player.setAllowFlight(false);
            bossbar.setProgress(fuel / MAX_FUEL);
            bossbar.setTitle(ChatColor.RED + "VTOL Jets");
            bossbar.setColor(BarColor.RED);
            bossbar.setVisible(true);
            return;
        } else {
            if (MathUtils.almostEqual(fuel, MAX_FUEL)) {
                bossbar.setVisible(false);
            } else {
                bossbar.setProgress(fuel / MAX_FUEL);
                bossbar.setTitle(ChatColor.GREEN + "VTOL Jets");
                bossbar.setColor(BarColor.GREEN);
                bossbar.setVisible(true);
            }
            player.setAllowFlight(!MathUtils.almostSmaller(fuel, 0));
        }

        if (player.isFlying()) {
            fuel = Math.max(0, fuel - 1);
            regenTicks = REGEN_DELAY;
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.05f, 0.3f);
            player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 2, 0.1, 0.1, 0.1, 0.05);
        } else {
            if (regenTicks > 0)
                regenTicks--;
            else
                fuel = Math.min(MAX_FUEL, fuel + REGEN_SPEED);
        }

        if (player.isFlying()) {
            if (!wasFlying) {
                velocityStable = false;
            }
            if (!velocityStable) {
                if (MathUtils.almostEqual(player.getVelocity().getY(), 0))
                    velocityStable = true;
            } else if (!MathUtils.almostEqual(player.getVelocity().getY(), 0) || MathUtils.almostSmaller(player.getLocation().getY(), previousY)) {
                player.setFlying(false);
            }
            player.setFlySpeed(FLY_SPEED);
        }
        previousY = player.getLocation().getY();
        wasFlying = player.isFlying();
    }

    @Override
    protected void end() {
        super.end();
        bossbar.removeAll();
    }
}
