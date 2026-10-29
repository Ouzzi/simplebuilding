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
 * The menu of the Strong, Mighty, Evil and Mighty Evil beacons: Vanilla's {@link BeaconMenu} (payment slot, inventory, three data values) under its own menu
 * type, so the client opens the two-column screen. Vanilla's {@code ServerboundSetBeaconPacket} reaches
 * {@link #updateEffects}, which checks the choice against {@link StrongBeaconLogic} instead of Vanilla's fixed list.
 */
public class StrongBeaconMenu extends BeaconMenu {
    /**
     * Data values beyond Vanilla's three (levels, primary, secondary): the server's max pyramid, range numbers and the
     * switches of the ten extra effects (bit i = {@code BeaconsConfig.EFFECT_IDS[i]}). They ride the normal menu data
     * sync, so a client on a server with other settings shows the right limits. Then the beacon kind ({@link BeaconKind}
     * ordinal) and the base amplifier of the chosen primary power (a "II" placeholder = 1).
     */
    public static final int DATA_MAX_LEVELS = 3, DATA_RANGE_BASE = 4, DATA_RANGE_PER_LEVEL = 5, DATA_EFFECTS_ON = 6, DATA_KIND = 7,
            DATA_PRIMARY_AMP = 8, DATA_COUNT = 9;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    /** Server side: the amplifier of the power the player clicked last (button id {@link #POWER_BUTTON} + amplifier), see {@link #clickMenuButton}. */
    private int pickedAmp;
    public static final int POWER_BUTTON = 10;

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

    /** Which beacon this menu belongs to (Strong before the first sync or without synced data). */
    public BeaconKind kind() {
        return synced() ? BeaconKind.byOrdinal(this.data.get(DATA_KIND)) : BeaconKind.STRONG;
    }

    /** Base amplifier of the primary power the beacon has chosen (what the screen highlights). */
    public int primaryAmp() {
        return synced() ? this.data.get(DATA_PRIMARY_AMP) : 0;
    }

    /** Highest pyramid the server allows for this beacon (what the client screen shows). */
    public int maxLevels() {
        return synced() ? Math.max(1, Math.min(StrongBeaconLogic.MAX_LEVELS, this.data.get(DATA_MAX_LEVELS))) : StrongBeaconLogic.maxLevels(kind());
    }

    /** Effect range at {@code levels} with the server's numbers. */
    public int range(int levels) {
        return synced() ? this.data.get(DATA_RANGE_BASE) + this.data.get(DATA_RANGE_PER_LEVEL) * levels : StrongBeaconLogic.range(levels);
    }

    /** Whether the server lets players choose {@code effect} (the ten extra effects can be switched off; Vanilla's six always). */
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
        return this.access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof StrongBeaconBlock
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }

    /** The client tells which button (amplifier) it pressed before it confirms: Vanilla's packet only carries the effect. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < POWER_BUTTON || id > POWER_BUTTON + 1) return false;
        this.pickedAmp = id - POWER_BUTTON;
        return true;
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
        BeaconKind kind = kind();
        int amp = primaryEffect != null && kind.primaryLevelOf(primaryEffect, this.pickedAmp) > 0 ? this.pickedAmp : Math.max(0, kind.baseAmplifier(primaryEffect));
        if (!StrongBeaconLogic.validate(kind, primaryEffect, amp, secondaryEffect, this.getLevels())) return true;
        if (synced()) this.data.set(DATA_PRIMARY_AMP, amp);
        this.data.set(1, encodeEffect(primaryEffect));
        this.data.set(2, encodeEffect(secondaryEffect));
        this.slots.get(0).remove(1);
        this.access.execute(Level::blockEntityChanged);
        return true;
    }
}
