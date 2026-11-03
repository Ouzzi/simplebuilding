package com.simpleenchanting.menu;

import com.simpleenchanting.EnchanterStage;
import com.simpleenchanting.EnchantingRegistry;
import com.simpleenchanting.EnchantingRules;
import com.simpleenchanting.block.EnchanterBlock;
import com.simpleenchanting.block.EnchanterBlockEntity;
import java.util.List;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Menu of the Dark and the Astral Enchanter (owner 2026-10-09, queue N27; stages ENCH01 03.11.). Built like Vanilla's enchanting menu: the item slot
 * belongs to the menu (it goes back to the player on close), lapis and blaze powder are the table's own slots
 * ({@link EnchanterBlockEntity}, they stay in the block). Instead of three fixed offers there are three
 * random enchantments that fit the item, each with a slider 0..max; the sliders share the table's budget in levels
 * ({@link EnchantingRules}: value per enchantment, a tax for combining, at most 50; budget up to the stage's top tier). Everything reaches the server as Vanilla menu button clicks: {@code row * 16 + level} sets
 * a slider, {@link #BUTTON_ENCHANT} enchants. The server checks every click against the budget and the stock again.
 */
public class EnchanterMenu extends AbstractContainerMenu {
    public static final int ITEM_SLOT = 0;
    public static final int LAPIS_SLOT = 1;
    public static final int BLAZE_SLOT = 2;
    public static final int PLAYER_START = 3;
    public static final int PLAYER_END = PLAYER_START + 36;
    public static final int LEVELS_PER_ROW = 16;
    public static final int BUTTON_ENCHANT = 100;
    /** Slot positions (also read by the screen). */
    /** The table's own slots (item, lapis, blaze) come first in the menu. */
    public static final int TABLE_SLOTS = 3;
    public static final int ITEM_X = 17, ITEM_Y = 20, LAPIS_X = 8, BLAZE_X = 26, STOCK_Y = 44;
    /** Owner N31: a taller, airier screen - the player inventory 36 px lower than Vanilla's 84. */
    public static final int INVENTORY_Y = 120, IMAGE_HEIGHT = INVENTORY_Y + 82;
    private static final Identifier EMPTY_LAPIS = Identifier.withDefaultNamespace("container/slot/lapis_lazuli");
    private static final Identifier EMPTY_BLAZE = Identifier.withDefaultNamespace("container/slot/brewing_fuel");

    private final Container itemSlot = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            EnchanterMenu.this.slotsChanged(this);
        }
    };
    private final Container stock;
    private final ContainerLevelAccess access;
    private final Player player;
    private final DataSlot tier = DataSlot.standalone();
    /** Shelf points * 4 + the floor kind (for the tooltip: what the next stage lacks). */
    private final DataSlot setup = DataSlot.standalone();
    /** The table's stage ({@link EnchanterStage#ordinal}). */
    private final DataSlot stage = DataSlot.standalone();
    /**
     * Per row: enchantment id (holder id map, -1 none), top level of the slider (Vanilla maximum, or the endgame level at the top tier,
     * {@link EnchantingRules#levelCap}), chosen level, value at the Vanilla maximum, and that Vanilla maximum.
     */
    private final int[] enchant = {-1, -1, -1};
    private final int[] maxLevel = new int[3];
    private final int[] baseMax = new int[3];
    private final int[] chosen = new int[3];
    private final int[] value = new int[3];

    /** Client. */
    public EnchanterMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(EnchanterBlockEntity.SIZE), ContainerLevelAccess.NULL);
    }

    public EnchanterMenu(int containerId, Inventory inventory, Container stock, ContainerLevelAccess access) {
        super(EnchantingRegistry.ENCHANTER_MENU, containerId);
        checkContainerSize(stock, EnchanterBlockEntity.SIZE);
        this.stock = stock;
        this.access = access;
        this.player = inventory.player;
        stock.startOpen(inventory.player);
        this.addSlot(new Slot(this.itemSlot, 0, ITEM_X, ITEM_Y) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addSlot(new Slot(stock, EnchanterBlockEntity.LAPIS, LAPIS_X, STOCK_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EnchanterBlockEntity.fits(EnchanterBlockEntity.LAPIS, stack);
            }

            @Override
            public Identifier getNoItemIcon() {
                return EMPTY_LAPIS;
            }
        });
        this.addSlot(new Slot(stock, EnchanterBlockEntity.BLAZE, BLAZE_X, STOCK_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EnchanterBlockEntity.fits(EnchanterBlockEntity.BLAZE, stack);
            }

            @Override
            public Identifier getNoItemIcon() {
                return EMPTY_BLAZE;
            }
        });
        this.addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        this.addDataSlot(this.tier);
        this.addDataSlot(this.setup);
        this.addDataSlot(this.stage);
        for (int[] array : new int[][] {this.enchant, this.maxLevel, this.chosen, this.value, this.baseMax}) {
            for (int i = 0; i < 3; i++) this.addDataSlot(DataSlot.shared(array, i));
        }
        refresh();
    }

    // ------------------------------------------------------------------ state

    /** Server: reads the table's tier and draws the three enchantments for the item from the player's seed. */
    private void refresh() {
        this.access.execute((level, pos) -> {
            int currentTier = readTier(level, pos);
            this.tier.set(currentTier);
            ItemStack stack = this.itemSlot.getItem(0);
            List<Holder<Enchantment>> choices = EnchantingRules.choices(level.registryAccess(), stack, this.player.getEnchantmentSeed());
            IdMap<Holder<Enchantment>> ids = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
            for (int i = 0; i < 3; i++) {
                Holder<Enchantment> holder = i < choices.size() ? choices.get(i) : null;
                this.enchant[i] = holder == null ? -1 : ids.getId(holder);
                this.baseMax[i] = holder == null ? 0 : holder.value().getMaxLevel();
                this.maxLevel[i] = holder == null ? 0 : EnchantingRules.levelCap(holder, currentTier);
                this.value[i] = holder == null ? 0 : EnchantingRules.value(holder.value());
                this.chosen[i] = 0;
            }
            this.broadcastChanges();
        });
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == this.itemSlot) refresh();
    }

    private int readTier(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        EnchanterStage at = level.getBlockState(pos).getBlock() instanceof EnchanterBlock block ? block.stage() : EnchanterStage.DARK;
        this.stage.set(at.ordinal());
        int points = EnchantingRules.shelfPoints(level, pos);
        int floor = EnchantingRules.floorKind(level, pos);
        this.setup.set(points * 4 + floor);
        int tier = EnchantingRules.tier(at, points, floor);
        this.tier.set(tier);
        return tier;
    }

    public EnchanterStage stage() {
        return EnchanterStage.byId(this.stage.get());
    }

    public int shelfPoints() {
        return this.setup.get() >> 2;
    }

    public boolean hasFloor() {
        return floorKind() != EnchantingRules.FLOOR_NONE;
    }

    /** {@link EnchantingRules#FLOOR_NONE}, {@code FLOOR_BLAZING} or {@code FLOOR_NIHIL}. */
    public int floorKind() {
        return this.setup.get() & 3;
    }

    /** {@link EnchantingRules#nextStep} for this table. */
    public int nextStep() {
        return EnchantingRules.nextStep(stage(), shelfPoints(), floorKind());
    }

    public int tier() {
        return this.tier.get();
    }

    public int budget() {
        return EnchantingRules.budget(tier());
    }

    public int enchantId(int row) {
        return this.enchant[row];
    }

    public int maxLevel(int row) {
        return this.enchant[row] < 0 ? 0 : this.maxLevel[row];
    }

    /** The Vanilla maximum of the row's enchantment: the slider levels above it are endgame levels (dearer, {@link EnchantingRules#ENDGAME_SURCHARGE} each). */
    public int baseMax(int row) {
        return this.enchant[row] < 0 ? 0 : this.baseMax[row];
    }

    public int chosen(int row) {
        return this.chosen[row];
    }

    /** Value of the row's enchantment at its maximum level (before the tax). */
    public int value(int row) {
        return this.value[row];
    }

    /** Levels the row's slider would cost alone at {@code level} (before the tax). */
    public int levelPrice(int row, int level) {
        return EnchantingRules.levelPrice(level, maxLevel(row), baseMax(row), this.value[row]);
    }

    public int[] chosenLevels() {
        return this.chosen.clone();
    }

    private int[] maxLevels() {
        return new int[] {maxLevel(0), maxLevel(1), maxLevel(2)};
    }

    private int[] baseMaxes() {
        return new int[] {baseMax(0), baseMax(1), baseMax(2)};
    }

    /** Sum of the slider prices before the tax. */
    public int basePrice() {
        return EnchantingRules.basePrice(this.chosen, maxLevels(), baseMaxes(), this.value);
    }

    /** Whether the item slot holds a golden apple (BS25-22): the table then offers the Enchanted Golden Apple instead of sliders. */
    public boolean isApple() {
        return this.itemSlot.getItem(0).is(Items.GOLDEN_APPLE);
    }

    /** The price in levels (tax included, at most 50): used up on enchanting, and the player needs it. A golden apple: {@link EnchantingRules#APPLE_LEVELS}. */
    public int price() {
        return isApple() ? EnchantingRules.APPLE_LEVELS : EnchantingRules.price(this.chosen, maxLevels(), baseMaxes(), this.value);
    }

    /** Levels the click takes off the player: the apple's whole price, else 1-5 like Vanilla's table. */
    public int levelsTaken() {
        return isApple() ? EnchantingRules.APPLE_LEVELS : EnchantingRules.levelCost(price());
    }

    public int lapisNeeded() {
        return isApple() ? EnchantingRules.APPLE_LAPIS : EnchantingRules.lapisCost(price());
    }

    public int blazeNeeded() {
        return isApple() ? EnchantingRules.APPLE_BLAZE : EnchantingRules.blazePowderCost(price());
    }

    /** Highest level the slider in {@code row} may reach now (the rest greyed out). */
    public int affordable(int row) {
        return EnchantingRules.maxAffordable(row, this.chosen, maxLevels(), baseMaxes(), this.value, budget());
    }

    public int lapis() {
        return this.stock.getItem(EnchanterBlockEntity.LAPIS).getCount();
    }

    public int blazePowder() {
        return this.stock.getItem(EnchanterBlockEntity.BLAZE).getCount();
    }

    /** Whether {@code player} could enchant now (client: to light the button; server checks again). */
    public boolean canEnchant(Player player) {
        int price = price();
        boolean apple = isApple();
        if (this.itemSlot.getItem(0).isEmpty() || price <= 0 || (!apple && price > budget())) return false;
        if (player.hasInfiniteMaterials()) return true;
        return player.experienceLevel >= price && lapis() >= lapisNeeded() && blazePowder() >= blazeNeeded();
    }

    /** Enchantment of a row on this side, or null. */
    public @Nullable Holder<Enchantment> enchantment(Level level, int row) {
        int id = this.enchant[row];
        if (id < 0) return null;
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap().byId(id);
    }

    // ------------------------------------------------------------------ clicks

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == BUTTON_ENCHANT) return enchant(player);
        int row = buttonId / LEVELS_PER_ROW;
        int level = buttonId % LEVELS_PER_ROW;
        if (buttonId < 0 || row >= 3 || this.enchant[row] < 0) return false;
        this.chosen[row] = Math.min(level, affordable(row));
        return true;
    }

    /** BS25-22: the golden apple becomes an Enchanted Golden Apple for 40 levels, 3 lapis lazuli and 3 blaze powder. */
    private boolean enchantApple(Player player) {
        return this.access.evaluate((level, pos) -> {
            if (!player.hasInfiniteMaterials()) {
                player.giveExperienceLevels(-EnchantingRules.APPLE_LEVELS);
                this.stock.removeItem(EnchanterBlockEntity.LAPIS, EnchantingRules.APPLE_LAPIS);
                this.stock.removeItem(EnchanterBlockEntity.BLAZE, EnchantingRules.APPLE_BLAZE);
            }
            ItemStack result = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
            player.awardStat(Stats.ENCHANT_ITEM);
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, result, EnchantingRules.APPLE_LEVELS);
            }
            this.itemSlot.setItem(0, result);
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
            return true;
        }, false);
    }

    private boolean enchant(Player player) {
        if (!canEnchant(player)) return false;
        if (isApple()) return enchantApple(player);
        return this.access.evaluate((level, pos) -> {
            int tier = readTier(level, pos);
            int levels = price();
            if (levels > EnchantingRules.budget(tier) || !canEnchant(player)) return false;
            ItemStack stack = this.itemSlot.getItem(0);
            ItemStack result = stack.is(Items.BOOK) ? stack.transmuteCopy(Items.ENCHANTED_BOOK, 1) : stack.copy();
            for (int row = 0; row < 3; row++) {
                Holder<Enchantment> holder = enchantment(level, row);
                if (holder == null || this.chosen[row] <= 0) continue;
                // The table may have lost its top stage since the offer was drawn: an endgame level then is refused, never cut quietly.
                if (this.chosen[row] > EnchantingRules.levelCap(holder, tier)) return false;
                result.enchant(holder, this.chosen[row]);
            }
            // N37 (owner): the price is the level needed; like Vanilla only a few levels are used up (1-5).
            player.onEnchantmentPerformed(result, EnchantingRules.levelCost(levels));
            if (!player.hasInfiniteMaterials()) {
                this.stock.removeItem(EnchanterBlockEntity.LAPIS, EnchantingRules.lapisCost(levels));
                this.stock.removeItem(EnchanterBlockEntity.BLAZE, EnchantingRules.blazePowderCost(levels));
            }
            player.awardStat(Stats.ENCHANT_ITEM);
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, result, levels);
            }
            this.itemSlot.setItem(0, result);
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
            return true;
        }, false);
    }

    // ------------------------------------------------------------------ slots

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.stock.stopOpen(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.itemSlot));
    }

    @Override
    public boolean stillValid(Player player) {
        return (stillValid(this.access, player, EnchantingRegistry.DARK_ENCHANTER) || stillValid(this.access, player, EnchantingRegistry.ASTRAL_ENCHANTER))
                && this.stock.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < PLAYER_START) {
            if (!this.moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (EnchanterBlockEntity.fits(EnchanterBlockEntity.LAPIS, stack)) {
            if (!this.moveItemStackTo(stack, LAPIS_SLOT, LAPIS_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (EnchanterBlockEntity.fits(EnchanterBlockEntity.BLAZE, stack)) {
            if (!this.moveItemStackTo(stack, BLAZE_SLOT, BLAZE_SLOT + 1, false)) return ItemStack.EMPTY;
        } else {
            if (this.slots.get(ITEM_SLOT).hasItem()) return ItemStack.EMPTY;
            this.slots.get(ITEM_SLOT).setByPlayer(stack.split(1));
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }
}
