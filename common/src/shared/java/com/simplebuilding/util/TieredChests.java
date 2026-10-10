package com.simplebuilding.util;

import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.blocks.custom.TieredChestBlock;
import com.simplebuilding.blocks.entity.custom.TieredChestBlockEntity;
import com.simplebuilding.items.custom.BackpackItem;
import com.simplebuilding.mixin.CompoundContainerAccessor;
import com.simplebuilding.screen.TieredChestMenu;
import com.simplebuilding.screen.TieredChestOpenData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Was die Mod-Truhen ausserhalb ihrer Block- und Menueklassen brauchen: das Oeffnen samt
 * Oeffnungsdaten, die Stapelgrenze fuer Trichter, das Komparatorsignal und die Aufwertung in der
 * Welt, die den Inhalt behaelt (Kupfertruhe -&gt; Verstaerkt -&gt; Netherit -&gt; Enderit, siehe
 * {@link SledgehammerUpgrades}).
 */
public final class TieredChests {

    private TieredChests() {
    }

    // =====================================================================================
    // OEFFNEN
    // =====================================================================================

    /** Ein Menue-Anbieter mit den Daten, die der Client fuer den Aufbau des Menues braucht. */
    public record Opening(MenuProvider provider, TieredChestOpenData data) {
    }

    /**
     * Wie Vanillas {@code MENU_PROVIDER_COMBINER}: eine einzelne Truhe ist selbst der Anbieter, eine
     * Doppeltruhe bekommt einen, der beide Haelften als {@code CompoundContainer} zeigt - erst die
     * rechte ("erste") Haelfte, dann die linke, genau wie Vanilla - und nur oeffnet, wenn beide
     * Schloesser es erlauben. Null, wenn die Truhe blockiert ist (Block oder Katze auf dem Deckel).
     */
    public static @Nullable Opening opening(TieredChestBlock block, BlockState state, Level level, BlockPos pos) {
        ChestTier tier = block.tier();
        return block.combine(state, level, pos, false).apply(new DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<Opening>>() {
            @Override
            public Optional<Opening> acceptDouble(ChestBlockEntity first, ChestBlockEntity second) {
                Container container = new CompoundContainer(first, second);
                MenuProvider provider = new MenuProvider() {
                    @Override
                    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                        if (first.canOpen(player) && second.canOpen(player)) {
                            first.unpackLootTable(inventory.player);
                            second.unpackLootTable(inventory.player);
                            return TieredChestMenu.server(containerId, inventory, container, tier, true);
                        }
                        Vec3 center = Vec3.atCenterOf(first.getBlockPos()).add(Vec3.atCenterOf(second.getBlockPos())).scale(0.5);
                        BaseContainerBlockEntity.sendChestLockedNotifications(center, player, getDisplayName());
                        return null;
                    }

                    @Override
                    public Component getDisplayName() {
                        if (first.hasCustomName()) {
                            return first.getDisplayName();
                        }
                        if (second.hasCustomName()) {
                            return second.getDisplayName();
                        }
                        return Component.translatable("container.simplebuilding.double_chest", block.getContainerName());
                    }
                };
                return Optional.of(new Opening(provider, TieredChestOpenData.of(tier, true)));
            }

            @Override
            public Optional<Opening> acceptSingle(ChestBlockEntity single) {
                return Optional.of(new Opening(single, TieredChestOpenData.of(tier, false)));
            }

            @Override
            public Optional<Opening> acceptNone() {
                return Optional.empty();
            }
        }).orElse(null);
    }

    /** Die Tooltip-Zeilen einer Mod-Truhe (Plaetze, ab Netherit der Stapelfaktor), sonst leer. */
    public static List<Component> tooltip(ItemStack stack) {
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem item) || !(item.getBlock() instanceof TieredChestBlock chest)) {
            return List.of();
        }
        ChestTier tier = chest.tier();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.simplebuilding.tiered_chest.slots", tier.slots(), tier.slots() * 2)
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        if (tier.stackMultiplier() > 1) {
            lines.add(Component.translatable("tooltip.simplebuilding.tiered_chest.stacks", tier.stackMultiplier())
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        return lines;
    }

    // =====================================================================================
    // STAPELGRENZEN (Trichter) UND KOMPARATOR
    // =====================================================================================

    /** Die Mod-Truhe hinter {@code container} (auch die erste Haelfte einer Doppeltruhe), sonst null. */
    public static @Nullable TieredChestBlockEntity chestBehind(@Nullable Container container) {
        if (container instanceof TieredChestBlockEntity chest) {
            return chest;
        }
        if (container instanceof CompoundContainer compound
                && ((CompoundContainerAccessor) compound).simplebuilding$first() instanceof TieredChestBlockEntity chest) {
            return chest;
        }
        return null;
    }

    /**
     * Wie viele {@code stack} ein Platz von {@code container} fasst: bei einer Mod-Truhe (allein
     * oder doppelt) die Grenze ihrer Stufe (x2/x4), sonst {@code vanilla}. Vanillas Trichter
     * rechnen mit {@code stack.getMaxStackSize()}, ein {@code CompoundContainer} mit 64 - beides
     * waere fuer Netherit und Enderit zu klein.
     */
    public static int maxStackSize(@Nullable Container container, ItemStack stack, int vanilla) {
        Container storage = oversizedStorage(container);
        return storage == null ? vanilla : storage.getMaxStackSize(stack);
    }

    /**
     * Das Lager mit Stufen-Stapelgrenze hinter {@code container}: eine Mod-Truhe (auch als erste
     * Haelfte einer Doppeltruhe), eine gestufte Shulkerkiste oder ein gestuftes Kisten-Fahrzeug, sonst null.
     */
    public static @Nullable Container oversizedStorage(@Nullable Container container) {
        if (container instanceof com.simplebuilding.blocks.entity.custom.TieredShulkerBoxBlockEntity box) {
            return box;
        }
        if (container instanceof com.simplebuilding.entity.vehicle.TieredStorageVehicle) {
            return container; // gestufte Kistenlore / Kistenboot (Queue N19)
        }
        return chestBehind(container);
    }

    /** Vanillas Komparatorformel, gegen die Stapelgrenze der Stufe gerechnet. */
    public static int analogSignal(@Nullable Container container, ChestTier tier) {
        if (container == null || container.getContainerSize() == 0) {
            return 0;
        }
        float total = 0.0F;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                total += (float) stack.getCount() / BackpackItem.maxStackSizeIn(stack, tier.stackMultiplier());
            }
        }
        total /= container.getContainerSize();
        return Mth.lerpDiscrete(total, 0, 15);
    }

    // =====================================================================================
    // AUFWERTUNG IN DER WELT
    // =====================================================================================

    /**
     * Die Truhen, die eine Aufwertung von {@code pos} nach {@code to} umbaut: die Truhe selbst und,
     * bei einer Doppeltruhe, ihre Haelfte - wenn die zur selben Stufe fuehrt, dieselbe Blickrichtung
     * hat und zurueck auf {@code pos} zeigt.
     */
    public static List<BlockPos> halves(Level level, BlockPos pos, BlockState state, Block to) {
        List<BlockPos> halves = new ArrayList<>();
        halves.add(pos.immutable());
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = ChestBlock.getConnectedBlockPos(pos, state);
            BlockState otherState = level.getBlockState(other);
            SledgehammerUpgrades.Upgrade otherUpgrade = SledgehammerUpgrades.upgradeOf(otherState.getBlock());
            if (otherUpgrade != null && otherUpgrade.to() == to
                    && otherState.hasProperty(ChestBlock.TYPE)
                    && otherState.getValue(ChestBlock.TYPE) == state.getValue(ChestBlock.TYPE).getOpposite()
                    && otherState.getValue(ChestBlock.FACING) == state.getValue(ChestBlock.FACING)
                    && ChestBlock.getConnectedBlockPos(other, otherState).equals(pos)) {
                halves.add(other.immutable());
            }
        }
        return halves;
    }

    /** Was von einer Truhenhaelfte in die neue Stufe mitgeht. */
    private record Carried(BlockPos pos, BlockState oldState, List<ItemStack> items, DataComponentMap components) {
    }

    /**
     * Baut die Truhe (und ihre Haelfte) zu {@code to} um und behaelt dabei Inhalt (Platz fuer
     * Platz, die neue Stufe hat mehr Plaetze und groessere Stapel), Namen, Schloss, Blickrichtung,
     * Doppeltruhen-Seite und Wasser. Liefert die Zahl der umgebauten Bloecke (1 oder 2).
     *
     * <p>Die alte Block-Entity wird vorher geleert: Vanilla wirft beim Blockwechsel den Inhalt
     * aus. Beide Haelften werden ohne Formupdates gesetzt ({@code UPDATE_KNOWN_SHAPE}), sonst
     * sieht die zuerst umgebaute Haelfte eine fremde Nachbarin und wird einzeln - und die zweite
     * danach auch. Erst danach bekommen die Nachbarn (Komparatoren, Trichter) Bescheid.
     */
    public static int upgradeInPlace(ServerLevel level, BlockPos pos, Block to) {
        BlockState state = level.getBlockState(pos);
        List<Carried> carried = new ArrayList<>();
        for (BlockPos half : halves(level, pos, state, to)) {
            BlockState halfState = level.getBlockState(half);
            List<ItemStack> items = new ArrayList<>();
            DataComponentMap.Builder kept = DataComponentMap.builder();
            BlockEntity entity = level.getBlockEntity(half);
            if (entity instanceof BaseContainerBlockEntity container) {
                DataComponentMap components = container.collectComponents();
                kept.set(DataComponents.CUSTOM_NAME, components.get(DataComponents.CUSTOM_NAME));
                if (components.has(DataComponents.LOCK)) {
                    kept.set(DataComponents.LOCK, components.get(DataComponents.LOCK));
                }
                for (int i = 0; i < container.getContainerSize(); i++) {
                    items.add(container.getItem(i));
                }
                container.clearContent();
            }
            carried.add(new Carried(half, halfState, items, kept.build()));
        }
        for (Carried half : carried) {
            level.setBlock(half.pos(), to.withPropertiesOf(half.oldState()), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        for (Carried half : carried) {
            if (level.getBlockEntity(half.pos()) instanceof TieredChestBlockEntity chest) {
                chest.applyComponents(half.components(), DataComponentPatch.EMPTY);
                chest.receiveUpgradedContents(half.items());
            } else {
                // Sollte nie passieren (jede Stufe hat die Truhen-Block-Entity) - dann wenigstens nichts verlieren.
                for (ItemStack stack : half.items()) {
                    Block.popResource(level, half.pos(), stack);
                }
            }
        }
        for (Carried half : carried) {
            level.updateNeighborsAt(half.pos(), to);
            level.updateNeighbourForOutputSignal(half.pos(), to);
            SledgehammerProgress.clear(level, half.pos());
        }
        return carried.size();
    }
}
