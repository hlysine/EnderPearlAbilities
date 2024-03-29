package io.github.henry_yslin.enderpearlabilities.abilities.cryptoultimate;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.*;
import io.github.henry_yslin.enderpearlabilities.abilities.cryptotactical.CryptoTacticalAbility;
import io.github.henry_yslin.enderpearlabilities.abilities.cryptotactical.CryptoTacticalAbilityInfo;
import io.github.henry_yslin.enderpearlabilities.events.AbilityActivateEvent;
import io.github.henry_yslin.enderpearlabilities.events.EventListener;
import io.github.henry_yslin.enderpearlabilities.managers.interactionlock.InteractionLockManager;
import io.github.henry_yslin.enderpearlabilities.utils.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class CryptoUltimateAbility extends Ability<CryptoUltimateAbilityInfo> {

    static final double EMP_RADIUS = 20;
    final String TACTICAL_ABILITY_NAME;

    public CryptoUltimateAbility(Plugin plugin, CryptoUltimateAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);

        TACTICAL_ABILITY_NAME = new CryptoTacticalAbilityInfo(plugin).getCodeName();
    }

    @Override
    protected AbilityCooldown createCooldown() {
        return new SingleUseCooldown(this, player);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);

    @Override
    public boolean isActive() {
        return abilityActive.get();
    }

    @Override
    public boolean isChargingUp() {
        return chargingUp.get();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        super.onPlayerJoin(event);
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            setUpPlayer(player);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (player != null) {
            setUpPlayer(player);
        }
    }

    private void setUpPlayer(Player player) {
        chargingUp.set(false);
        abilityActive.set(false);
        cooldown.setCooldown(info.getCooldown());
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager.hasMetadata("emp")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Entity shooter = (Entity) event.getEntity().getShooter();
        if (shooter != null && shooter.hasMetadata("emp")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        Entity entity = event.getEntity();
        if (entity.hasMetadata("emp")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (event.getEntityType() == EntityType.ENDERMAN) {
            Entity entity = event.getEntity();
            if (entity.hasMetadata("silence")) {
                event.setCancelled(true);
            }
        }
    }

    private void activateAbility(LivingEntity abilityTarget) {
        List<Entity> entities = new ArrayList<>();

        new FunctionChain(
                next -> {
                    if (info.getChargeUp() <= 0) {
                        next.run();
                        return;
                    }
                    chargingUp.set(true);
                    new AbilityRunnable() {
                        BossBar bossbar;

                        @Override
                        protected synchronized void start() {
                            bossbar = Bukkit.createBossBar("Charging up", BarColor.WHITE, BarStyle.SOLID);
                            bossbar.addPlayer(player);
                        }

                        @Override
                        protected synchronized void tick() {
                            if (!abilityTarget.isValid() || !player.isValid()) {
                                cancel();
                                return;
                            }
                            bossbar.setProgress(count / (double) info.getChargeUp());
                            player.getWorld().spawnParticle(Particle.WHITE_ASH, player.getLocation(), 5, 0.5, 0.5, 0.5, 0.02);
                            WorldUtils.spawnParticleSphere(abilityTarget.getLocation(), EMP_RADIUS, Particle.END_ROD, 100, true);
                        }

                        @Override
                        protected synchronized void end() {
                            bossbar.removeAll();
                            chargingUp.set(false);
                            if (this.hasCompleted())
                                next.run();
                        }
                    }.runTaskRepeated(this, 0, 1, info.getChargeUp());
                },
                next -> {
                    abilityActive.set(true);
                    abilityTarget.getWorld().playSound(abilityTarget.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1, 0.5f);
                    abilityTarget.getWorld().spawnParticle(Particle.END_ROD, abilityTarget.getLocation(), 2000, 1, 1, 1, 1, null, true);
                    entities.addAll(abilityTarget.getWorld().getNearbyEntities(abilityTarget.getLocation(), EMP_RADIUS, EMP_RADIUS, EMP_RADIUS, entity -> entity instanceof LivingEntity && entity.getLocation().distance(abilityTarget.getLocation()) < EMP_RADIUS));
                    entities.removeIf(entity -> {
                        if (entity instanceof Player p) {
                            return p.getGameMode() == GameMode.SPECTATOR;
                        }
                        if (entity instanceof Mob mob) {
                            return !mob.hasAI();
                        }
                        return false;
                    });
                    for (int i = entities.size() - 1; i >= 0; i--) {
                        Entity entity = entities.get(i);
                        if (entity.getType() == EntityType.PLAYER) {
                            if (entity.hasMetadata("NPC")) {
                                Optional<Object> metadata = EntityUtils.getMetadata(entity, "ability");
                                if (metadata.isPresent()) {
                                    if (metadata.get() instanceof AbilityCouple couple) {
                                        if (couple.ability().equals(TACTICAL_ABILITY_NAME))
                                            entities.add(plugin.getServer().getPlayer(couple.player()));
                                    }
                                }
                            }
                        }
                    }
                    if (entities.isEmpty()) {
                        abilityActive.set(false);
                        cooldown.setCooldown(info.getCooldown());
                        return;
                    }
                    for (Entity entity : entities) {
                        if (entity instanceof LivingEntity livingEntity) {
                            livingEntity.damage(4, abilityTarget);
                            livingEntity.addPotionEffect(PotionEffectType.SLOW.createEffect(30, 3));
                            livingEntity.setMetadata("emp", new FixedMetadataValue(plugin, info.getCodeName()));
                        }
                        if (entity instanceof Player p) {
                            p.sendTitle(" ", ChatColor.RED + "EMP detected", 5, 30, 30);
                            InteractionLockManager.getInstance().lockInteraction(p);
                        }
                    }
                    next.run();
                },
                next -> new AbilityRunnable() {
                    BossBar bossbar;
                    BossBar hitBossbar;

                    @Override
                    protected synchronized void start() {
                        bossbar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + info.getName(), BarColor.PURPLE, BarStyle.SOLID);
                        bossbar.addPlayer(player);
                        hitBossbar = Bukkit.createBossBar(ChatColor.RED + info.getName(), BarColor.RED, BarStyle.SOLID);
                        for (Entity entity : entities) {
                            if (entity instanceof Player p) {
                                hitBossbar.addPlayer(p);
                            }
                        }
                    }

                    @Override
                    protected synchronized void tick() {
                        if (!abilityActive.get()) {
                            cancel();
                        }
                        bossbar.setProgress(count / (double) info.getDuration() * 10);
                        hitBossbar.setProgress(count / (double) info.getDuration() * 10);
                    }

                    @Override
                    protected synchronized void end() {
                        bossbar.removeAll();
                        hitBossbar.removeAll();
                        for (Entity entity : entities) {
                            entity.removeMetadata("emp", plugin);
                            if (entity instanceof Player p) {
                                InteractionLockManager.getInstance().unlockInteraction(p);
                            }
                        }
                        entities.clear();
                        abilityActive.set(false);
                        cooldown.setCooldown(info.getCooldown());
                        next.run();
                    }
                }.runTaskRepeated(this, 0, 10, info.getDuration() / 10)
        ).execute();
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.getName().equals(ownerName)) return;

        Optional<Ability<?>> droneAbility = EnderPearlAbilities.getInstance().getAbilities().stream()
                .filter(ability -> ability instanceof CryptoTacticalAbility && ability.getOwnerName().equals(ownerName))
                .findFirst();

        LivingEntity targetEntity;

        if (droneAbility.isEmpty()) {
            if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;
            targetEntity = player;
        } else {
            CryptoTacticalAbility tacticalAbility = (CryptoTacticalAbility) droneAbility.get();
            LivingEntity droneEntity = tacticalAbility.getDroneEntity();
            if (droneEntity != null && droneEntity.isValid()) {
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK)
                        return;
                    if (!PlayerUtils.inventoryContains(player, Material.ENDER_PEARL, 1)) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Not enough ender pearls"));
                        return;
                    }
                } else {
                    if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;
                }
                targetEntity = droneEntity;
            } else {
                if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Drone required"));
                event.setCancelled(true);
                return;
            }
        }

        event.setCancelled(true);

        if (!cooldown.isAbilityUsable()) return;
        if (chargingUp.get()) return;
        if (abilityActive.get()) return;

        AbilityUtils.consumeEnderPearl(this, player);
        EnderPearlAbilities.getInstance().emitEvent(
                EventListener.class,
                new AbilityActivateEvent(this),
                EventListener::onAbilityActivate
        );

        activateAbility(targetEntity);
    }
}
