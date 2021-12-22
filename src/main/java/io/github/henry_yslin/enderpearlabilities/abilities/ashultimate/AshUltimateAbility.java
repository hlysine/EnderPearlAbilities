package io.github.henry_yslin.enderpearlabilities.abilities.ashultimate;

import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityRunnable;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.managers.voidspace.VoidSpaceManager;
import io.github.henry_yslin.enderpearlabilities.utils.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class AshUltimateAbility extends Ability<AshUltimateAbilityInfo> {

    static final int TARGET_RANGE = 80;
    static final double ANGLE_ALLOWANCE = Math.toRadians(10);
    static final double PHASE_VELOCITY = 2;

    public AshUltimateAbility(Plugin plugin, AshUltimateAbilityInfo info, String ownerName) {
        super(plugin, info, ownerName);
    }

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    AbilityRunnable portal;

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
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (!abilityActive.get()) return;
        event.setCancelled(true);
    }

    private Vector rotateVectorDownwards(Vector vector, double angle) {
        double horizontalDist = vector.clone().setY(0).length();
        vector.setY(Math.max(vector.getY() - vector.length(), horizontalDist * Math.tan(Math.max(Math.toRadians(-89.99999), Math.atan2(vector.getY(), horizontalDist) - angle))));
        return vector;
    }

    private Optional<Location> getTargetLocation(Player player) {
        World world = player.getWorld();
        Vector direction = player.getEyeLocation().getDirection();
        Location eyeLocation = player.getEyeLocation();

        if (MathUtils.almostEqual(direction.getX(), 0) && MathUtils.almostEqual(direction.getZ(), 0) && direction.getY() > 0)
            return Optional.empty();

        RayTraceResult eyeRay = world.rayTraceBlocks(eyeLocation, direction, TARGET_RANGE, FluidCollisionMode.NEVER, true);

        Vector eyeHitPosition;
        if (eyeRay != null) {
            if (eyeRay.getHitBlockFace() == BlockFace.UP)
                return Optional.of(eyeRay.getHitPosition().toLocation(player.getWorld()));
            eyeHitPosition = eyeRay.getHitPosition().add(direction.clone().multiply(-0.1));
        } else {
            if (MathUtils.almostEqual(direction.getX(), 0) && MathUtils.almostEqual(direction.getZ(), 0) && direction.getY() < 0)
                return Optional.empty();
            eyeHitPosition = player.getEyeLocation().add(direction.clone().multiply(TARGET_RANGE)).toVector();
        }

        Vector eyeLocationVector = eyeLocation.toVector();
        for (Vector offset = direction.clone().multiply(-0.5); eyeHitPosition.distanceSquared(eyeLocationVector) > 1; eyeHitPosition.add(offset)) {
            Vector relative = eyeHitPosition.clone().subtract(eyeLocationVector);
            Vector downward = rotateVectorDownwards(relative.clone(), ANGLE_ALLOWANCE);
            RayTraceResult result = world.rayTraceBlocks(eyeHitPosition.toLocation(world), new Vector(0, -1, 0), relative.getY() - downward.getY(), FluidCollisionMode.NEVER, true);
            if (result == null) continue;
            Location targetEye = result.getHitPosition().toLocation(world).add(0, player.getEyeHeight(), 0);
            Vector targetDirection = eyeLocation.toVector().subtract(targetEye.toVector());
            RayTraceResult check = world.rayTraceBlocks(targetEye, targetDirection, targetDirection.length(), FluidCollisionMode.NEVER, true);
            if (check == null) return Optional.of(result.getHitPosition().toLocation(world));
        }
        return Optional.empty();
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.getActivation())) return;

        event.setCancelled(true);

        if (cooldown.isCoolingDown()) return;
        if (abilityActive.get()) return;

        if (chargingUp.get()) {
            chargingUp.set(false);
            abilityActive.set(true);
        } else {
            final Location[] lastLocation = new Location[1];
            final Location[] startLocation = new Location[1];

            Ability<?> ability = this;
            new FunctionChain(
                    next -> new AbilityRunnable() {
                        @Override
                        protected void start() {
                            chargingUp.set(true);
                        }

                        @Override
                        protected void tick() {
                            if (!chargingUp.get()) {
                                cancel();
                                return;
                            }
                            boolean mainHandPearl = player.getInventory().getItemInMainHand().getType() == Material.ENDER_PEARL;
                            boolean offHandPearl = player.getInventory().getItemInOffHand().getType() == Material.ENDER_PEARL;
                            boolean shouldContinue = ability.getInfo().getActivation() == ActivationHand.MainHand && mainHandPearl ||
                                    ability.getInfo().getActivation() == ActivationHand.OffHand && offHandPearl;
                            if (!player.isValid()) shouldContinue = false;
                            if (shouldContinue) {
                                Optional<Location> targetLocation = getTargetLocation(player);
                                lastLocation[0] = targetLocation.orElse(null);
                                targetLocation.ifPresentOrElse(location -> {
                                    player.spawnParticle(Particle.ELECTRIC_SPARK, location.clone().add(0, 1, 0), 20, 0.05, 1, 0.05, 0, null);
                                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.LIGHT_PURPLE + String.format("%.0fm", location.distance(player.getLocation()))));
                                }, () -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No target")));
                            } else {
                                chargingUp.set(false);
                                abilityActive.set(false);
                                cancel();
                            }
                        }

                        @Override
                        protected void end() {
                            if (lastLocation[0] != null && lastLocation[0].distance(player.getLocation()) < 1)
                                lastLocation[0] = null;

                            if (abilityActive.get()) {
                                if (lastLocation[0] == null)
                                    abilityActive.set(false);
                                else
                                    next.run();
                            }
                            if (!abilityActive.get())
                                cooldown.setCooldown(20);
                        }
                    }.runTaskTimer(this, 0, 1),
                    next -> new AbilityRunnable() {
                        Location location;

                        @Override
                        protected void start() {
                            PlayerUtils.consumeEnderPearl(player);
                            location = player.getLocation();
                            Vector direction = player.getLocation().getDirection();
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.5f);
                            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getEyeLocation().add(direction), 10, 0, 0, 0, 0, null, true);
                        }

                        @Override
                        protected void tick() {
                            if (!player.isValid()) {
                                cancel();
                                return;
                            }
                            player.teleport(location);
                            player.setVelocity(new Vector());
                            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 10, 0.5, 1, 0.5, 0.02);
                        }

                        @Override
                        protected void end() {
                            if (hasCompleted())
                                next.run();
                            else
                                abilityActive.set(false);
                        }
                    }.runTaskRepeated(this, 0, 1, 10),
                    next -> new AbilityRunnable() {
                        Location currentLocation;
                        Vector velocity;

                        @Override
                        protected void start() {
                            startLocation[0] = player.getLocation();
                            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.3f, 2);
                            abilityActive.set(true);

                            VoidSpaceManager.getInstance().enterVoid(player);

                            currentLocation = player.getLocation();
                            velocity = lastLocation[0].clone().subtract(player.getLocation()).toVector().normalize().multiply(PHASE_VELOCITY);
                            currentLocation.setDirection(velocity);
                        }

                        @Override
                        protected void tick() {
                            if (!player.isValid()) {
                                cancel();
                                return;
                            }
                            currentLocation.add(velocity);
                            player.teleport(currentLocation);
                            player.setVelocity(velocity);
                            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 10, 0.5, 1, 0.5, 0.02, null, true);
                            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, lastLocation[0].clone().add(0, 1, 0), 20, 0.05, 1, 0.05, 0, null, true);

                            if (currentLocation.distance(lastLocation[0]) < PHASE_VELOCITY * 1.5)
                                cancel();
                        }

                        @Override
                        protected void end() {
                            boolean completed = currentLocation.distance(lastLocation[0]) < PHASE_VELOCITY * 1.5;
                            if (completed)
                                player.teleport(lastLocation[0].setDirection(player.getLocation().getDirection()));

                            abilityActive.set(false);
                            player.setVelocity(new Vector());
                            player.setFallDistance(0);

                            VoidSpaceManager.getInstance().exitVoid(player);

                            cooldown.setCooldown(info.getCooldown());
                            if (completed)
                                next.run();
                        }
                    }.runTaskTimer(this, 0, 1),
                    next -> {
                        if (portal != null && !portal.isCancelled())
                            portal.cancel();
                        (portal = new AbilityRunnable() {
                            Location from;
                            Location to;

                            @Override
                            protected void start() {
                                from = startLocation[0].clone().add(0, 1, 0);
                                to = lastLocation[0].clone().add(0, 1, 0);
                            }

                            @Override
                            protected void tick() {
                                World world = Objects.requireNonNull(from.getWorld());
                                world.spawnParticle(Particle.DRAGON_BREATH, from, 10, 0.1, 1, 0.1, 0.005, null, true);
                                world.spawnParticle(Particle.ELECTRIC_SPARK, to, 10, 0.05, 1, 0.05, 0.01, null, true);

                                for (Entity entity : world.getNearbyEntities(from, 0.5, 1, 0.5, entity -> entity instanceof LivingEntity)) {
                                    from.getWorld().playSound(from, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.5f);
                                    from.getWorld().playSound(to, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.5f);
                                    entity.teleport(to.clone().subtract(0, 0.99, 0));
                                    entity.setFallDistance(0);
                                    entity.setVelocity(new Vector());
                                    WorldUtils.spawnParticleLine(from, to, Particle.DRAGON_BREATH, 2, true);
                                }
                            }

                            @Override
                            protected void end() {
                                super.end();
                            }
                        }).runTaskRepeated(this, 0, 1, info.getDuration());
                    }
            ).execute();
        }
    }
}