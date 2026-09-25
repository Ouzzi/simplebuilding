package com.simplebuilding.blueprint;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Der Kartentisch als Scanner (siehe {@link BlueprintScanner}): die Slots des Vanilla-Menues
 * werden durch Huellen ersetzt, die zusaetzlich Oktant (oben) und Blaupause (unten) annehmen,
 * und der Ergebnis-Slot verbraucht beim Scan nur die Blaupause - der Oktant bleibt liegen.
 * Das Mixin {@code CartographyTableMenuMixin} haengt das in den Konstruktor und in
 * {@code setupResultSlot}.
 */
public final class BlueprintCartography {
    public static final int MAP_SLOT = 0;
    public static final int ADDITIONAL_SLOT = 1;
    public static final int RESULT_SLOT = 2;

    private BlueprintCartography() {
    }

    /** Ersetzt die drei Tisch-Slots durch die Huellen (gleicher Container, Index und Platz). */
    public static void wrapSlots(AbstractContainerMenu menu, List<Slot> slots, ContainerLevelAccess access) {
        slots.set(MAP_SLOT, new InputSlot(slots.get(MAP_SLOT), BlueprintScanner::isOctant));
        slots.set(ADDITIONAL_SLOT, new InputSlot(slots.get(ADDITIONAL_SLOT), s -> s.getItem() instanceof com.simplebuilding.items.custom.BlueprintItem));
        slots.set(RESULT_SLOT, new ResultSlot(slots.get(RESULT_SLOT), menu, access));
    }

    /** Liegt ein Scan-Paar im Tisch (Oktant oben oder Blaupause unten)? Dann ist es nicht Vanillas Sache. */
    public static boolean isScanSetup(ItemStack map, ItemStack additional) {
        return BlueprintScanner.isOctant(map) || additional.getItem() instanceof com.simplebuilding.items.custom.BlueprintItem;
    }

    /** Das Scan-Ergebnis fuer den Tisch; meldet einen Ablehnungsgrund in der Aktionsleiste. */
    public static ItemStack result(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos table,
                                   ItemStack map, ItemStack additional, Player player) {
        if (!BlueprintScanner.isOctant(map) || !BlueprintScanner.isWritableBlueprint(additional)) {
            if (player != null && BlueprintScanner.isOctant(map) && additional.getItem() instanceof com.simplebuilding.items.custom.BlueprintItem) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("simplebuilding.blueprint.scan.signed").withStyle(ChatFormatting.RED), true);
            }
            return ItemStack.EMPTY;
        }
        BlueprintScanner.Outcome outcome = BlueprintScanner.scanAtTable(level, table, map, additional);
        if (outcome.error() != null && player != null) {
            player.displayClientMessage(outcome.error().copy().withStyle(ChatFormatting.RED), true);
        }
        return outcome.result();
    }

    static final class InputSlot extends Slot {
        private final Slot original;
        private final Predicate<ItemStack> extra;

        InputSlot(Slot original, Predicate<ItemStack> extra) {
            super(original.container, original.getContainerSlot(), original.x, original.y);
            this.index = original.index;
            this.original = original;
            this.extra = extra;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return original.mayPlace(stack) || extra.test(stack);
        }
    }

    static final class ResultSlot extends Slot {
        private final Slot original;
        private final AbstractContainerMenu menu;
        private final ContainerLevelAccess access;

        ResultSlot(Slot original, AbstractContainerMenu menu, ContainerLevelAccess access) {
            super(original.container, original.getContainerSlot(), original.x, original.y);
            this.index = original.index;
            this.original = original;
            this.menu = menu;
            this.access = access;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack carried) {
            if (BlueprintScanner.isOctant(menu.getSlot(MAP_SLOT).getItem())) {
                // Scan: nur die Blaupause wird verbraucht, der Oktant bleibt im Tisch.
                menu.getSlot(ADDITIONAL_SLOT).remove(1);
                access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F));
                this.setChanged();
                return;
            }
            original.onTake(player, carried);
        }
    }
}
