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

/** The running challenge of a server: preset, state, price engine and what the unlocked area offers (the easy-tier price pool). */
public final class Active implements View {
    public final MinecraftServer server;
    public final Preset preset;
    public final UnlockMode mode;
    public final ChallengeState state;
    public final PriceEngine engine;
    private final Map<String, WeightedItem> available = new LinkedHashMap<>();

    Active(MinecraftServer server, Preset preset, ChallengeState state) {
        this.server = server;
        this.preset = preset;
        this.mode = preset.unlock();
        this.state = state;
        this.engine = new PriceEngine(state.seed, mode);
        state.bind(mode);
    }

    @Override
    public UnlockGrid grid(String dimension) {
        return state.grids.get(dimension);
    }

    @Override
    public boolean exemptCreative() {
        return ChallengeServerConfig.exemptCreative;
    }

    public Iterable<String> dimensions() {
        return mode.dimensions();
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
