package io.github.henryyslin.enderpearlabilities.utils;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.Optional;

public class PlayerUtils {
    public static Entity getPlayerTargetEntity(Player player) {
        World world = player.getWorld();
        RayTraceResult result = world.rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 48, entity -> {
            if (entity instanceof Player p) {
                return !p.getName().equals(player.getName());
            }
            return true;
        });
        if (result == null) return null;
        return result.getHitEntity();
    }

    public static LivingEntity getPlayerTargetLivingEntity(Player player) {
        World world = player.getWorld();
        RayTraceResult result = world.rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 48, entity -> {
            if (!(entity instanceof LivingEntity)) return false;
            if (entity instanceof Player p) {
                return !p.getName().equals(player.getName());
            }
            return true;
        });
        if (result == null) return null;
        return (LivingEntity) result.getHitEntity();
    }

    public static Optional<Integer> getMainHandToolDurability(Player player) {
        return ItemUtils.getToolDurability(player.getInventory().getItemInMainHand());
    }

    public static boolean canMainHandBreakBlock(Player player, Block block) {
        return !block.getDrops(player.getInventory().getItemInMainHand(), player).isEmpty();
    }
}
