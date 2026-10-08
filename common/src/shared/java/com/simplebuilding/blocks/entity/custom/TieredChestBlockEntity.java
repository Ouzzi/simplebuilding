package com.simplebuilding.blocks.entity.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.blocks.custom.TieredChestBlock;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.items.custom.BackpackItem;
import com.simplebuilding.screen.TieredChestMenu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Die Block-Entity der Mod-Truhen (alle drei Stufen teilen einen Typ). Eine Vanilla-Truhe mit
 * mehr Plaetzen und - ab Netherit - uebergrossen Stapeln.
 *
 * <p><b>Plaetze.</b> Die Liste gehoert weiter {@link ChestBlockEntity} (ueber {@code setItems}),
 * nur ihre Groesse kommt aus der Stufe des Blocks. So laden, speichern, droppen und
 * vergleichen alle Vanilla-Pfade dieselbe Liste.
 *
 * <p><b>Uebergrosse Stapel.</b> Vanillas Item-Codec speichert hoechstens 99 Stueck. Beim
 * Speichern (und beim Sammeln der Komponenten, etwa fuer das Kreativ-Blockwaehlen) sieht Vanilla
 * deshalb eine Kopie mit hoechstens 99 je Platz; die echte Anzahl steht zusaetzlich unter
 * {@value #EXTRA_COUNTS} und wird nach dem Laden wieder eingesetzt. Ein Werkzeug, das nur Vanillas
 * {@code Items} liest, sieht also 99 statt 256 - verloren geht nichts.
 *
 * <p><b>Oeffnen.</b> Das Menue ist kein {@code ChestMenu} (es ist breiter als neun Spalten);
 * Vanillas Oeffner-Zaehler erkennt aber nur {@code ChestMenu}s und wuerde den Deckel beim
 * naechsten Nachzaehlen schliessen. Deshalb zaehlt diese Klasse selbst ({@link #openers}) und
 * meldet den Stand wie Vanilla ueber das Block-Ereignis 1 an den Deckel-Regler weiter; Klang,
 * Spielereignis und Nachzaehlen sind dieselben.
 */
public class TieredChestBlockEntity extends ChestBlockEntity {
    public static final String EXTRA_COUNTS = "simplebuilding:counts";
    /** Hoechste Anzahl, die Vanillas Item-Codec speichert. */
    public static final int CODEC_MAX_COUNT = 99;

    private record ExtraCount(int slot, int count) {
        static final Codec<ExtraCount> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("Slot").forGetter(ExtraCount::slot),
                Codec.INT.fieldOf("Count").forGetter(ExtraCount::count)
        ).apply(i, ExtraCount::new));
    }

    private final ChestTier tier;

    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            if (state.getBlock() instanceof ChestBlock chest) {
                playSound(level, pos, state, chest.getOpenChestSound());
            }
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            if (state.getBlock() instanceof ChestBlock chest) {
                playSound(level, pos, state, chest.getCloseChestSound());
            }
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int current) {
            TieredChestBlockEntity.this.signalOpenCount(level, pos, state, previous, current);
        }

        @Override
        public boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof TieredChestMenu menu && menu.shows(TieredChestBlockEntity.this);
        }
    };

    public TieredChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIERED_CHEST_BE, pos, state);
        this.tier = state.getBlock() instanceof TieredChestBlock chest ? chest.tier() : ChestTier.REINFORCED;
        this.setItems(NonNullList.withSize(this.tier.slots(), ItemStack.EMPTY));
    }

    public ChestTier tier() {
        return this.tier;
    }

    @Override
    public int getContainerSize() {
        return this.tier.slots();
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock() instanceof TieredChestBlock chest
                ? chest.getContainerName()
                : Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    // --- Beute ---

    /**
     * Eine Stufen-Truhe mit Loot-Tabelle (eine bessere Struktur-Truhe, {@code BetterChests}) wuerfelt ihre Tabelle
     * zweimal: doppelte Beute. Der zweite Wurf hat einen abgeleiteten Seed (0 bleibt "zufaellig" wie bei Vanilla).
     */
    @Override
    public void unpackLootTable(@org.jetbrains.annotations.Nullable Player player) {
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> key = getLootTable();
        long seed = getLootTableSeed();
        super.unpackLootTable(player);
        if (key == null || !com.simplebuilding.version.McVersion.RARE_STRUCTURE_FINDS
                || !(this.level instanceof net.minecraft.server.level.ServerLevel server)) {
            return;
        }
        net.minecraft.world.level.storage.loot.LootTable table = server.getServer().reloadableRegistries().getLootTable(key);
        net.minecraft.world.level.storage.loot.LootParams.Builder params = new net.minecraft.world.level.storage.loot.LootParams.Builder(server)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                        net.minecraft.world.phys.Vec3.atCenterOf(this.worldPosition));
        if (player != null) {
            params.withLuck(player.getLuck()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, player);
        }
        table.fill(this, params.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST),
                seed == 0L ? 0L : seed * 31L + SECOND_ROLL_OFFSET);
    }

    /** Abstand des zweiten Beute-Seeds. */
    private static final long SECOND_ROLL_OFFSET = 0x2545_F491_4F6C_DD1DL;

    // --- Stapelgrenzen ---

    /** Vanillas Container-Obergrenze (99) mal Stufenfaktor, wie beim Rucksack. */
    @Override
    public int getMaxStackSize() {
        return 99 * this.tier.stackMultiplier();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return BackpackItem.maxStackSizeIn(stack, this.tier.stackMultiplier());
    }

    // --- Speichern ---

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        NonNullList<ItemStack> items = this.getItems();
        for (ExtraCount extra : input.listOrEmpty(EXTRA_COUNTS, ExtraCount.CODEC)) {
            if (extra.slot() >= 0 && extra.slot() < items.size() && !items.get(extra.slot()).isEmpty()) {
                items.get(extra.slot()).setCount(extra.count());
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        NonNullList<ItemStack> real = this.getItems();
        this.setItems(codecSafeCopy(real));
        try {
            super.saveAdditional(output);
        } finally {
            this.setItems(real);
        }
        ValueOutput.TypedOutputList<ExtraCount> extras = output.list(EXTRA_COUNTS, ExtraCount.CODEC);
        for (int i = 0; i < real.size(); i++) {
            if (real.get(i).getCount() > CODEC_MAX_COUNT) {
                extras.add(new ExtraCount(i, real.get(i).getCount()));
            }
        }
        if (extras.isEmpty()) {
            output.discard(EXTRA_COUNTS);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        NonNullList<ItemStack> real = this.getItems();
        this.setItems(codecSafeCopy(real));
        try {
            super.collectImplicitComponents(components);
        } finally {
            this.setItems(real);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard(EXTRA_COUNTS);
    }

    /** Die Liste, mit jedem Stapel ueber 99 als Kopie mit 99 Stueck (sonst dieselben Stapel). */
    private static NonNullList<ItemStack> codecSafeCopy(NonNullList<ItemStack> items) {
        NonNullList<ItemStack> copy = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            copy.set(i, stack.getCount() > CODEC_MAX_COUNT ? stack.copyWithCount(CODEC_MAX_COUNT) : stack);
        }
        return copy;
    }

    /** Alle Stapel (fuer die Aufwertung und die Tests). */
    public List<ItemStack> itemsView() {
        return java.util.Collections.unmodifiableList(this.getItems());
    }

    /**
     * Die Aufwertung: der Inhalt der alten Stufe, Platz fuer Platz. Die neue Stufe hat mindestens
     * so viele Plaetze und so grosse Stapel; was trotzdem nicht passt, faellt heraus statt zu
     * verschwinden.
     */
    public void receiveUpgradedContents(List<ItemStack> stacks) {
        NonNullList<ItemStack> items = this.getItems();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (i < items.size() && items.get(i).isEmpty() && stack.getCount() <= getMaxStackSize(stack)) {
                items.set(i, stack);
            } else if (this.level != null) {
                net.minecraft.world.level.block.Block.popResource(this.level, this.worldPosition, stack);
            }
        }
        this.setChanged();
    }

    // --- Menue und Oeffner ---

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return TieredChestMenu.server(containerId, inventory, this, this.tier, false);
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator()) {
            this.openers.incrementOpeners(user.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState(),
                    user.getContainerInteractionRange());
        }
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator()) {
            this.openers.decrementOpeners(user.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    @Override
    public List<ContainerUser> getEntitiesWithContainerOpen() {
        return this.openers.getEntitiesWithContainerOpen(this.getLevel(), this.getBlockPos());
    }

    @Override
    public void recheckOpen() {
        if (!this.remove) {
            this.openers.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    public int openerCount() {
        return this.openers.getOpenerCount();
    }

    @Override
    protected void signalOpenCount(Level level, BlockPos pos, BlockState state, int previous, int current) {
        super.signalOpenCount(level, pos, state, previous, current);
        if (previous != current && state.getBlock() instanceof TieredChestBlock chest && chest.isTrapped()) {
            var orientation = net.minecraft.world.level.redstone.ExperimentalRedstoneUtils.initialOrientation(
                    level, state.getValue(ChestBlock.FACING).getOpposite(), Direction.UP);
            level.updateNeighborsAt(pos, chest, orientation);
            level.updateNeighborsAt(pos.below(), chest, orientation);
        }
    }

    /** Wie Vanillas {@code ChestBlockEntity#playSound}: eine Doppeltruhe klingt einmal, aus ihrer Mitte. */
    private static void playSound(Level level, BlockPos pos, BlockState state, SoundEvent event) {
        ChestType type = state.getValue(ChestBlock.TYPE);
        if (type == ChestType.LEFT) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        if (type == ChestType.RIGHT) {
            Direction direction = ChestBlock.getConnectedDirection(state);
            x += direction.getStepX() * 0.5;
            z += direction.getStepZ() * 0.5;
        }
        level.playSound(null, x, y, z, event, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
    }
}
