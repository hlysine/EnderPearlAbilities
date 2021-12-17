package io.github.henry_yslin.enderpearlabilities;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.commands.ability.AbilityCommand;
import io.github.henry_yslin.enderpearlabilities.commands.ability.AbilityTabCompleter;
import io.github.henry_yslin.enderpearlabilities.managers.Manager;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.reflections.scanners.Scanners.SubTypes;

public final class EnderPearlAbilities extends JavaPlugin {

    final FileConfiguration config = getConfig();

    final List<Ability> internalTemplateAbilities = new ArrayList<>();
    final List<Ability> templateAbilities = Collections.unmodifiableList(internalTemplateAbilities);
    final List<Ability> abilities = Collections.synchronizedList(new ArrayList<>());

    final List<Manager> internalManagers = new ArrayList<>();
    final List<Manager> managers = Collections.unmodifiableList(internalManagers);

    static EnderPearlAbilities instance;
    static ProtocolManager protocolManager;

    public static EnderPearlAbilities getInstance() {
        return instance;
    }

    public static ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    public List<Ability> getTemplateAbilities() {
        return templateAbilities;
    }

    public List<Ability> getAbilities() {
        return abilities;
    }

    public List<Manager> getManagers() {
        return managers;
    }

    public FileConfiguration getLoadedConfig() {
        return config;
    }

    @Override
    public void onEnable() {
        instance = this;
        protocolManager = ProtocolLibrary.getProtocolManager();
        getLogger().info(protocolManager.toString());

        // Plugin startup logic
        Reflections reflections = new Reflections("io.github.henry_yslin.enderpearlabilities");
        List<Class<?>> managerSubTypes =
                reflections.get(SubTypes.of(Manager.class).asClass()).stream().filter(c -> c.isAnnotationPresent(Instantiable.class)).sorted(Comparator.comparing(Class::getName)).toList();
        for (Class<?> subType : managerSubTypes) {
            try {
                Manager manager = (Manager) subType.getDeclaredConstructor(Plugin.class).newInstance(this);
                String name = manager.getCodeName();
                getLogger().info("Setting config defaults for manager: " + name);
                ConfigurationSection section = config.getConfigurationSection(name);
                if (section == null) {
                    section = config.createSection(name);
                }
                manager.writeConfigDefaults(section);
                manager.loadConfig(section);
                internalManagers.add(manager);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<Class<?>> abilitySubTypes =
                reflections.get(SubTypes.of(Ability.class).asClass()).stream().sorted(Comparator.comparing(Class::getName)).toList();
        for (Class<?> subType : abilitySubTypes) {
            try {
                Ability ability = (Ability) subType.getDeclaredConstructor(Plugin.class, String.class, ConfigurationSection.class).newInstance(this, null, null);
                String codeName = ability.getInfo().codeName;
                getLogger().info("Setting config defaults for " + codeName + " ability");
                ConfigurationSection section = config.getConfigurationSection(codeName);
                if (section == null) section = config.createSection(codeName);
                ability.setConfigDefaults(section);
                section.addDefault("players", new ArrayList<String>());
                Ability template = ability.getClass().getDeclaredConstructor(Plugin.class, String.class, ConfigurationSection.class).newInstance(this, null, section);
                internalTemplateAbilities.add(template);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        internalTemplateAbilities.sort(Comparator.comparing(ability -> ability.getInfo().codeName));

        getLogger().info("Saving config");

        config.addDefault("no-cooldown", false);
        config.addDefault("dynamic", false);
        config.options().copyDefaults(true);
        saveConfig();

        getLogger().info("Setting up managers");

        for (Manager manager : managers) {
            try {
                getServer().getPluginManager().registerEvents(manager, this);
                getLogger().info("Setting up \"" + manager.getCodeName() + "\"");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        getLogger().info("Setting up abilities");

        PluginCommand command = this.getCommand("ability");
        if (command != null) {
            command.setExecutor(new AbilityCommand());
            command.setTabCompleter(new AbilityTabCompleter());
        } else {
            getServer().broadcastMessage("Failed to register /ability command. Please check plugin description file.");
        }

        if (config.getBoolean("no-cooldown")) {
            getLogger().info("No cooldown mode is ON. All ability cooldowns are set to 1s.");
        }

        for (Ability templateAbility : templateAbilities) {
            try {
                String codeName = templateAbility.getInfo().codeName;
                ConfigurationSection configSection = config.getConfigurationSection(codeName);
                if (configSection == null) configSection = config.createSection(codeName);
                List<String> players = configSection.getStringList("players");
                for (String player : players) {
                    Ability instance = templateAbility.getClass().getDeclaredConstructor(Plugin.class, String.class, ConfigurationSection.class).newInstance(this, player, configSection);
                    abilities.add(instance);
                    getServer().getPluginManager().registerEvents(instance, this);
                    getLogger().info("Setting up \"" + codeName + "\" for " + player);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                synchronized (abilities) {
                    for (Manager manager : managers) {
                        getLogger().info("Calling onEnable for \"" + manager.getCodeName() + "\"");
                        manager.onEnable();
                    }
                    for (Ability ability : abilities) {
                        getLogger().info("Calling onEnable for \"" + ability.getInfo().codeName + "\" for " + ability.ownerName);
                        ability.onEnable();
                    }
                }
            }
        }.runTaskLater(this, 1);

        getLogger().info("Plugin onEnable completed");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        synchronized (abilities) {
            for (Ability ability : abilities) {
                getLogger().info("Calling onDisable for \"" + ability.getInfo().codeName + "\" for " + ability.ownerName);
                ability.onDisable();
            }
        }

        synchronized (managers) {
            for (Manager manager : managers) {
                getLogger().info("Calling onDisable for \"" + manager.getCodeName() + "\"");
                manager.onDisable();
            }
        }
        getLogger().info("Plugin onDisable completed");
    }

    public void removeAbility(Ability ability) {
        synchronized (abilities) {
            if (!abilities.contains(ability)) {
                return;
            }
            abilities.remove(ability);
            HandlerList.unregisterAll(ability);
            ability.onDisable();
            getLogger().info("Calling onDisable for \"" + ability.getInfo().codeName + "\"");
            ConfigurationSection section = config.getConfigurationSection(ability.getInfo().codeName);
            if (section != null) {
                List<String> players = section.getStringList("players");
                players.remove(ability.ownerName);
                section.set("players", players);
                saveConfig();
            }
        }
    }

    public Ability addAbility(Ability templateAbility, String ownerName) {
        try {
            String codeName = templateAbility.getInfo().codeName;
            ConfigurationSection configSection = config.getConfigurationSection(codeName);
            if (configSection == null) {
                configSection = config.createSection(codeName);
                templateAbility.setConfigDefaults(configSection);
            }
            List<String> players = configSection.getStringList("players");
            if (!players.contains(ownerName)) {
                players.add(ownerName);
                configSection.set("players", players);
                saveConfig();
            }
            Ability instance = templateAbility.getClass().getDeclaredConstructor(Plugin.class, String.class, ConfigurationSection.class).newInstance(this, ownerName, configSection);
            abilities.add(instance);
            getServer().getPluginManager().registerEvents(instance, this);
            getLogger().info("Setting up \"" + codeName + "\" for " + ownerName);
            instance.onEnable();
            return instance;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
