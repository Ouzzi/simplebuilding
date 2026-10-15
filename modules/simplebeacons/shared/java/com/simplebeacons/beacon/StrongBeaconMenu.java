package com.simplebeacons.beacon;

import com.simplebeacons.BeaconsRegistry;
import com.simplebeacons.config.BeaconsConfig;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;

/**
 * The Strong Beacon menu: Vanilla's {@link BeaconMenu} (payment slot, inventory, three data values) under its own menu
 * type, so the client opens the two-column screen. Vanilla's {@code ServerboundSetBeaconPacket} reaches
 * {@link #updateEffects}, which checks the choice against {@link StrongBeaconLogic} instead of Vanilla's fixed list.
 */
public class StrongBeaconMenu extends BeaconMenu {
    /**
     * Data values beyond Vanilla's three (levels, primary, secondary): the server's max pyramid, range numbers and the
     * switches of the six extra effects (bit i = {@code BeaconsConfig.EFFECT_IDS[i]}). They ride the normal menu data
     * sync, so a client on a server with other settings shows the right limits.
     */
    public static final int DATA_MAX_LEVELS = 3, DATA_RANGE_BASE = 4, DATA_RANGE_PER_LEVEL = 5, DATA_EFFECTS_ON = 6, DATA_COUNT = 7;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client side (menu type factory). */
    public StrongBeaconMenu(int containerId, Container inventory) {
        this(containerId, inventory, clientData(), ContainerLevelAccess.NULL);
    }

    /** Client data before the server's first sync: this side's own settings, replaced as soon as the values arrive. */
    private static ContainerData clientData() {
        SimpleContainerData data = new SimpleContainerData(DATA_COUNT);
        data.set(DATA_MAX_LEVELS, StrongBeaconLogic.maxLevels());
        data.set(DATA_RANGE_BASE, BeaconsConfig.strongBeaconRangeBase);
        data.set(DATA_RANGE_PER_LEVEL, BeaconsConfig.strongBeaconRangePerLevel);
        data.set(DATA_EFFECTS_ON, effectsMask());
        return data;
    }

    /** Bit mask of the switched-on extra effects (server side source of {@link #DATA_EFFECTS_ON}). */
    public static int effectsMask() {
        int mask = 0;
        for (int i = 0; i < BeaconsConfig.EFFECT_IDS.size(); i++) {
            if (BeaconsConfig.effectOn(BeaconsConfig.EFFECT_IDS.get(i))) mask |= 1 << i;
        }
        return mask;
    }

    /** Whether this menu carries the server settings (a hand-made 3-value data array does not: then the local config counts). */
    private boolean synced() {
        return this.data.getCount() >= DATA_COUNT;
    }

    /** Highest pyramid the server allows (what the client screen shows). */
    public int maxLevels() {
        return synced() ? Math.max(1, Math.min(StrongBeaconLogic.MAX_LEVELS, this.data.get(DATA_MAX_LEVELS))) : StrongBeaconLogic.maxLevels();
    }

    /** Effect range at {@code levels} with the server's numbers. */
    public int range(int levels) {
        return synced() ? this.data.get(DATA_RANGE_BASE) + this.data.get(DATA_RANGE_PER_LEVEL) * levels : StrongBeaconLogic.range(levels);
    }

    /** Whether the server lets players choose {@code effect} (the six extra effects can be switched off; Vanilla's four always). */
    public boolean effectEnabled(Holder<MobEffect> effect) {
        String path = StrongBeaconLogic.path(effect);
        int index = BeaconsConfig.EFFECT_IDS.indexOf(path);
        if (index < 0) return true;
        return synced() ? (this.data.get(DATA_EFFECTS_ON) & (1 << index)) != 0 : BeaconsConfig.effectOn(path);
    }

    public StrongBeaconMenu(int containerId, Container inventory, ContainerData data, ContainerLevelAccess access) {
        super(containerId, inventory, data, access);
        this.data = data;
        this.access = access;
    }

    @Override
    public MenuType<?> getType() {
        return BeaconsRegistry.STRONG_BEACON_MENU;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, BeaconsRegistry.STRONG_BEACON);
    }

    /**
     * Applies a valid choice and takes the payment. An invalid choice (foreign effect, level too low, a switched off
     * effect, Absorption II) is refused without taking the payment; unlike Vanilla it does not disconnect the player,
     * because a config change on the server may have made a client's buttons stale.
     */
    @Override
    public boolean updateEffects(Optional<Holder<MobEffect>> primary, Optional<Holder<MobEffect>> secondary) {
        if (!this.hasPayment()) return false;
        Holder<MobEffect> primaryEffect = primary.orElse(null);
        Holder<MobEffect> secondaryEffect = secondary.orElse(null);
        if (!StrongBeaconLogic.validate(primaryEffect, secondaryEffect, this.getLevels())) return true;
        this.data.set(1, encodeEffect(primaryEffect));
        this.data.set(2, encodeEffect(secondaryEffect));
        this.slots.get(0).remove(1);
        this.access.execute(Level::blockEntityChanged);
        return true;
    }
}
