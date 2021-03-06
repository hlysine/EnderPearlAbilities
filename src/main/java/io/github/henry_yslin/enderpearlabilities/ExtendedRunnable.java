package io.github.henry_yslin.enderpearlabilities;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("rawtypes")
public abstract class ExtendedRunnable<TListener extends ExtendedListener> extends BukkitRunnable {

    protected long count = Long.MIN_VALUE;
    protected TListener executor;

    /**
     * Whether this runnable has completed all its iterations as scheduled by {@code runTaskRepeated}.
     */
    public boolean hasCompleted() {
        return count < 0;
    }

    @SuppressWarnings("unchecked")
    private void internalStart(TListener executor) {
        this.executor = executor;
        executor.runnables.add(this);
        start();
    }

    private void internalEnd() {
        executor.runnables.remove(this);
        end();
    }

    /**
     * Preparation work to be executed immediately in a {@code runTask*} call.
     */
    protected void start() {
    }

    /**
     * To be executed in each iteration of the runnable, similar to {@code run} of a {@link BukkitRunnable}.
     * If this runnable is scheduled by {@code runTaskRepeated}, access {@code count} for the number of remaining iterations.
     */
    protected void tick() {
    }

    /**
     * Clean-up work to be executed after the last iteration, or before this runnable is cancelled.
     */
    protected void end() {
    }

    @Override
    public final synchronized void run() {
        tick();

        if (count == Long.MIN_VALUE) return;
        count--;
        if (hasCompleted()) {
            cancel();
        }
    }

    @Override
    @Deprecated
    public final synchronized @NotNull BukkitTask runTaskLater(@NotNull Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
        return super.runTaskLater(plugin, delay);
    }

    @Override
    @Deprecated
    public final synchronized @NotNull BukkitTask runTaskTimer(@NotNull Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        return super.runTaskTimer(plugin, delay, period);
    }

    public final synchronized BukkitTask runTaskRepeated(TListener executor, long delay, long period, long repeat) throws IllegalArgumentException, IllegalStateException {
        count = repeat - 1;
        return runTaskTimer(executor, delay, period);
    }

    public final synchronized BukkitTask runTaskTimer(TListener executor, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        internalStart(executor);
        return super.runTaskTimer(executor.plugin, delay, period);
    }

    public final synchronized BukkitTask runTaskLater(TListener executor, long delay) throws IllegalArgumentException, IllegalStateException {
        count = 0;
        internalStart(executor);
        return super.runTaskLater(executor.plugin, delay);
    }

    @Override
    public final synchronized void cancel() throws IllegalStateException {
        internalEnd();
        if (!isCancelled())
            super.cancel();
    }
}
