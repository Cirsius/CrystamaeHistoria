package io.github.sefiraat.crystamaehistoria.managers;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.runnables.ParticleDisplayRunnable;
import io.github.sefiraat.crystamaehistoria.runnables.SaveConfigRunnable;
import io.github.sefiraat.crystamaehistoria.runnables.TemporaryEffectsRunnable;
import lombok.Getter;

public class RunnableManager {

    @Getter
    public final TemporaryEffectsRunnable temporaryEffectsRunnable;
    @Getter
    public final SaveConfigRunnable saveConfigRunnable;
    @Getter
    public final ParticleDisplayRunnable particleDisplayRunnable;

    public RunnableManager() {
        CrystamaeHistoria plugin = CrystamaeHistoria.getInstance();

        this.temporaryEffectsRunnable = new TemporaryEffectsRunnable();
        this.temporaryEffectsRunnable.runTaskTimer(plugin, 1, 20);

        this.saveConfigRunnable = new SaveConfigRunnable();
        final long saveInterval = Math.max(20, plugin.getConfig().getInt("saving.interval-ticks", 12000));
        this.saveConfigRunnable.runTaskTimer(plugin, 1, saveInterval);

        this.particleDisplayRunnable = new ParticleDisplayRunnable();
        this.particleDisplayRunnable.runTaskTimer(plugin, 1, 80);
    }
}
