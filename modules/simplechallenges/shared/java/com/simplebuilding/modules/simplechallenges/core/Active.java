package com.simplebuilding.modules.simplechallenges.core;

import com.simplebuilding.modules.simplechallenges.config.ChallengeServerConfig;
import com.simplebuilding.modules.simplechallenges.preset.Preset;
import com.simplebuilding.modules.simplechallenges.preset.UnlockMode;
import com.simplebuilding.modules.simplechallenges.price.PriceEngine;
import com.simplebuilding.modules.simplechallenges.price.WeightedItem;
import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * The running challenge of a server: preset, state, price engine and what the unlocked area offers (the easy-tier price pool).
 * {@code mode} (the Unlock mode) is null for a preset that has only a world mode (pure Skyblock): then there are no grids, no
 * prices and no borders, and every unlock hook finds nothing to do.
 */
public final class Active implements View {
    public final MinecraftServer server;
    public final Preset preset;
    public final UnlockMode mode;
    public final ChallengeState state;
    public final PriceEngine engine;
    /** The Lucky odds (null without a lucky / randomdrops mode) and the Skyline plans (null without a skyline mode); tests may replace the plans. */
    public final com.simplebuilding.modules.simplechallenges.lucky.LuckyTable lucky;
    public volatile com.simplebuilding.modules.simplechallenges.skyline.SkylinePlans skyline;
    private final Map<String, WeightedItem> available = new LinkedHashMap<>();

    Active(MinecraftServer server, Preset preset, ChallengeState state) {
        this.server = server;
        this.preset = preset;
        this.mode = preset.unlock();
        this.state = state;
        this.engine = mode == null ? null : new PriceEngine(state.seed, mode);
        if (mode != null) state.bind(mode);
        this.lucky = preset.lucky() == null ? null : com.simplebuilding.modules.simplechallenges.lucky.LuckyTable.build(preset.lucky().alpha());
        this.skyline = preset.skyline() == null ? null : com.simplebuilding.modules.simplechallenges.skyline.SkylinePlans.build(preset.skyline());
    }

    @Override
    public UnlockGrid grid(String dimension) {
        return mode == null ? null : state.grids.get(dimension);
    }

    @Override
    public boolean exemptCreative() {
        return ChallengeServerConfig.exemptCreative;
    }

    public Iterable<String> dimensions() {
        return mode == null ? java.util.List.of() : mode.dimensions();
    }

    public ServerLevel level(String dimension) {
        try {
            return server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimension)));
        } catch (RuntimeException e) {
            return null;
        }
    }

    public ServerLevel overworld() {
        return server.getLevel(Level.OVERWORLD);
    }

    /** What the unlocked area offers by hand (summed weights); the pool of the easy tier and the check for deadlocks. */
    public List<WeightedItem> region() {
        return new ArrayList<>(available.values());
    }

    public void addAvailable(List<WeightedItem> items) {
        for (WeightedItem w : items) available.merge(w.id(), w, (a, b) -> new WeightedItem(a.id(), a.weight() + b.weight(), a.maxStack()));
    }

    /** True when the item can be had from the unlocked area: found on its surface, or in the hard ladder (mined and smelted in the pillars). */
    public boolean obtainable(String item) {
        if (available.containsKey(item)) return true;
        for (PriceEngine.Rung r : PriceEngine.LADDER) if (r.item().equals(item)) return true;
        return false;
    }
}
