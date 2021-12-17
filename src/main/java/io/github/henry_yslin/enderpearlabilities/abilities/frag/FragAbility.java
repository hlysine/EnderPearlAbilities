package io.github.henry_yslin.enderpearlabilities.abilities.frag;

import io.github.henry_yslin.enderpearlabilities.abilities.*;
import io.github.henry_yslin.enderpearlabilities.utils.*;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class FragAbility extends Ability {

    static final double PROJECTILE_SPEED = 10;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        super.setConfigDefaults(config);
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 60);
        config.addDefault("cooldown", 400);
    }

    public FragAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("frag")
                .name("Frag Grenade")
                .origin("Apex")
                .description("Throw frag grenades with accurate guides.")
                .usage("Hold an ender pearl to see the guide. Right click to throw.")
                .activation(ActivationHand.OffHand);

        if (config != null)
            builder
                    .chargeUp(config.getInt("charge-up"))
                    .duration(config.getInt("duration"))
                    .cooldown(config.getInt("cooldown"));

        info = builder.build();
    }

    @Override
    public AbilityInfo getInfo() {
        return info;
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    FragPredictionRunnable fragPredictionRunnable;

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
        cooldown.setCooldown(info.cooldown);

        if (fragPredictionRunnable != null && !fragPredictionRunnable.isCancelled())
            fragPredictionRunnable.cancel();
        fragPredictionRunnable = new FragPredictionRunnable(this, player);
        fragPredictionRunnable.runTaskTimer(this, 0, 5);
    }

    private Location getFirePosition(Player player) {
        return player.getEyeLocation().add(new Vector(0, 1, 0).crossProduct(player.getEyeLocation().getDirection()).multiply(0.4));
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.isCoolingDown()) return;
        if (chargingUp.get()) return;
        if (abilityActive.get()) return;

        abilityActive.set(true);

        AtomicReference<Projectile> grenade = new AtomicReference<>();

        PlayerUtils.consumeEnderPearl(player);
        new FunctionChain(
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, chargingUp, next),
                next -> {
                    Projectile projectile = AbilityUtils.fireProjectile(this, player, null, info.duration + 10, PROJECTILE_SPEED, true);
                    if (projectile != null) {
                        projectile.teleport(getFirePosition(player));
                        grenade.set(projectile);
                        projectile.setMetadata("grenade", new FixedMetadataValue(plugin, grenade));
                    }
                    next.run();
                },
                next -> new AbilityRunnable() {
                    Location lastLocation;

                    @Override
                    protected void start() {
                        Projectile snowball = grenade.get();
                        if (snowball == null) return;
                        lastLocation = snowball.getLocation();
                    }

                    @Override
                    protected void tick() {
                        Projectile snowball = grenade.get();
                        if (snowball == null) {
                            cancel();
                            return;
                        }
                        snowball.setVelocity(snowball.getVelocity().multiply(0.9).add(new Vector(0, -0.1, 0)));
                        WorldUtils.spawnParticleLine(lastLocation, snowball.getLocation(), Particle.END_ROD, 1, true);
                        lastLocation = snowball.getLocation();
                    }

                    @Override
                    protected void end() {
                        Projectile snowball = grenade.get();
                        if (snowball != null && snowball.isValid()) {
                            World world = snowball.getWorld();
//                            Location snowballLocation = snowball.getLocation();
//                            // Flash bang
//                            world.spawnParticle(Particle.FLASH, snowballLocation, 3, 1, 1, 1, 1);
//                            world.spawnParticle(Particle.FIREWORKS_SPARK, snowballLocation, 10, 0.05, 0.05, 0.05, 0.3);
//                            world.getNearbyEntities(snowballLocation, 20, 20, 20, entity -> entity instanceof LivingEntity).forEach(entity -> {
//                                LivingEntity livingEntity = (LivingEntity) entity;
//                                double distance = snowballLocation.distance(livingEntity.getEyeLocation());
//                                RayTraceResult result = world.rayTraceBlocks(snowballLocation, livingEntity.getEyeLocation().toVector().subtract(pearlLocation.toVector()), distance, FluidCollisionMode.NEVER, true);
//                                if (result != null) {
//                                    if (result.getHitBlock() != null) return;
//                                }
//                                int duration = (int) Math.min(120, 1000 / distance);
//                                livingEntity.addPotionEffect(PotionEffectType.BLINDNESS.createEffect(duration, 1));
//                                livingEntity.addPotionEffect(PotionEffectType.CONFUSION.createEffect(duration * 2, 1));
//                            });
                            if (hasCompleted())
                                world.createExplosion(snowball.getLocation().add(0, 0.1, 0), 5, false, false);
                            snowball.remove();
                        }
                        abilityActive.set(false);
                        cooldown.setCooldown(info.cooldown);
                    }
                }.runTaskRepeated(this, 0, 1, info.duration)
        ).execute();
    }

    @SuppressWarnings("unchecked")
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();

        if (!(shooter instanceof Player player)) return;
        if (!AbilityUtils.verifyAbilityCouple(this, projectile)) return;
        if (!player.getName().equals(ownerName)) return;

        if (!(projectile instanceof Snowball)) return;

        event.setCancelled(true);

        projectile.getWorld().spawnParticle(Particle.SMOKE_NORMAL, projectile.getLocation(), 2, 0.1, 0.1, 0.1, 0.02);

        Location hitPosition = ProjectileUtils.correctProjectileHitLocation(projectile);
        Vector newVelocity;
        if (event.getHitBlockFace() != null) {
            double hitMagnitude = Math.abs(projectile.getVelocity().dot(event.getHitBlockFace().getDirection()));
            newVelocity = projectile.getVelocity().add(event.getHitBlockFace().getDirection().multiply(hitMagnitude)).multiply(0.3).add(event.getHitBlockFace().getDirection().multiply(Math.min(1, hitMagnitude * 0.5)));

        } else {
            newVelocity = projectile.getVelocity().setX(0).setZ(0);
        }
        Snowball snowball = projectile.getWorld().spawn(hitPosition, Snowball.class, entity -> {
            entity.setShooter(player);
            entity.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)));
            entity.setVelocity(newVelocity);
        });
        Optional<Object> ref = EntityUtils.getMetadata(projectile, "grenade");
        if (ref.isPresent()) {
            AtomicReference<Projectile> grenade = (AtomicReference<Projectile>) ref.get();
            grenade.set(snowball);
            snowball.setMetadata("grenade", new FixedMetadataValue(plugin, grenade));
        }
        projectile.remove();
    }
}
