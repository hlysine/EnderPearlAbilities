package io.github.henry_yslin.enderpearlabilities;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link Listener} with onEnable and onDisable methods and runnables/sub-listeners management.
 */
@SuppressWarnings("rawtypes")
public abstract class ExtendedListener<TRunnable extends ExtendedRunnable, TListener extends ExtendedListener> implements Listener {

    protected final Plugin plugin;
    protected final List<TRunnable> runnables = Collections.synchronizedList(new ArrayList<>());
    protected final List<TListener> subListeners = Collections.synchronizedList(new ArrayList<>());

    public Plugin getPlugin() {
        return plugin;
    }

    public List<TRunnable> getRunnables() {
        return runnables;
    }

    public List<TListener> getSubListeners() {
        return subListeners;
    }

    /**
     * Create an {@link ExtendedListener} instance.
     *
     * @param plugin The owning plugin.
     */
    public ExtendedListener(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        for (TListener subListener : subListeners) {
            plugin.getServer().getPluginManager().registerEvents(subListener, plugin);
            subListener.onEnable();
        }
    }

    public void onDisable() {
        for (TListener subListener : subListeners) {
            subListener.onDisable();
            HandlerList.unregisterAll(subListener);
        }
        synchronized (runnables) {
            for (int i = runnables.size() - 1; runnables.size() > 0; i = runnables.size() - 1) {
                runnables.get(i).cancel();
            }
        }
    }
}
