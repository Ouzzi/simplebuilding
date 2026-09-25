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

    /**
     * Der Scan-Zustand eines offenen Kartentischs (ein Feld im Mixin). {@link #inputsChanged} startet
     * einen Scan oder verwirft ihn, {@link #tick} fuehrt ihn Tick fuer Tick fort (aus
     * {@code broadcastChanges}, das der Server fuer das offene Menue jeden Tick ruft) und meldet den
     * Fortschritt in der Aktionsleiste. Liegt etwas anderes im Tisch, wird der Scan abgebrochen.
     */
    public static final class TableScan {
        private BlueprintScanner.Job job;
        private long lastTick = Long.MIN_VALUE;
        private int budget = BlueprintScanner.DEFAULT_BUDGET_PER_TICK;

        public boolean running() {
            return job != null;
        }

        /** Die Eingaben haben sich geaendert: neu starten, weiterlaufen lassen oder ablehnen. */
        public void inputsChanged(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos table,
                                  ItemStack map, ItemStack additional, Player player, java.util.function.Consumer<ItemStack> result) {
            if (job != null && job.matches(map, additional)) {
                return;
            }
            job = null;
            if (!BlueprintScanner.isOctant(map) || !BlueprintScanner.isWritableBlueprint(additional)) {
                if (player != null && BlueprintScanner.isOctant(map) && additional.getItem() instanceof com.simplebuilding.items.custom.BlueprintItem) {
                    player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("simplebuilding.blueprint.scan.signed").withStyle(ChatFormatting.RED));
                }
                result.accept(ItemStack.EMPTY);
                return;
            }
            Object started = BlueprintScanner.start(level, table, map, additional);
            if (started instanceof BlueprintScanner.Outcome outcome) {
                finish(outcome, player, result);
                return;
            }
            BlueprintScanner.Job next = (BlueprintScanner.Job) started;
            result.accept(ItemStack.EMPTY);
            budget = BlueprintScanner.budgetFor(player);
            if (next.total() <= budget) {
                next.step(level, budget);
                finish(next.outcome(), player, result);
                return;
            }
            job = next;
            lastTick = level.getGameTime();
            next.step(level, budget);
            progress(player);
        }

        /** Ein Tick des laufenden Scans (hoechstens einmal je Spieltick). */
        public void tick(net.minecraft.world.level.Level level, ItemStack map, ItemStack additional, Player player,
                         java.util.function.Consumer<ItemStack> result) {
            if (job == null || level.getGameTime() == lastTick) {
                return;
            }
            if (!job.matches(map, additional)) {
                job = null;
                return;
            }
            lastTick = level.getGameTime();
            if (job.step(level, budget)) {
                BlueprintScanner.Outcome outcome = job.outcome();
                job = null;
                finish(outcome, player, result);
            } else {
                progress(player);
            }
        }

        private void progress(Player player) {
            if (player != null && job != null) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("simplebuilding.blueprint.scan.progress", job.percent())
                        .withStyle(ChatFormatting.AQUA));
            }
        }

        private static void finish(BlueprintScanner.Outcome outcome, Player player, java.util.function.Consumer<ItemStack> result) {
            if (outcome.error() != null) {
                if (player != null) {
                    player.sendOverlayMessage(outcome.error().copy().withStyle(ChatFormatting.RED));
                }
                result.accept(ItemStack.EMPTY);
                return;
            }
            if (player != null) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("simplebuilding.blueprint.scan.done").withStyle(ChatFormatting.GREEN));
            }
            result.accept(outcome.result());
        }
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

        @Override
        public ItemStack safeClone(Player player) {
            return original.safeClone(player);
        }
    }
}
