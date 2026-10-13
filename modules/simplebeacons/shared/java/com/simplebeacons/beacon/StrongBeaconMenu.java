package com.simplebeacons.beacon;

import com.simplebeacons.BeaconsRegistry;
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
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client side (menu type factory). */
    public StrongBeaconMenu(int containerId, Container inventory) {
        this(containerId, inventory, new SimpleContainerData(3), ContainerLevelAccess.NULL);
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
