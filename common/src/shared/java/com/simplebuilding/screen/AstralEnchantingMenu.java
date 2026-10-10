package com.simplebuilding.screen;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.enchanting.AstralEnchanting;
import com.simplebuilding.enchanting.AstralEnchantingTableBlockEntity;
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
 * Menu of the Astral Enchanter (owner 2026-10-09, queue N27). Built like Vanilla's enchanting menu: the item slot
 * belongs to the menu (it goes back to the player on close), lapis and blaze powder are the table's own slots
 * ({@link AstralEnchantingTableBlockEntity}, they stay in the block). Instead of three fixed offers there are three
 * random enchantments that fit the item, each with a slider 0..max; the sliders share the table's budget
 * ({@link AstralEnchanting}). Everything reaches the server as Vanilla menu button clicks: {@code row * 16 + level} sets
 * a slider, {@link #BUTTON_ENCHANT} enchants. The server checks every click against the budget and the stock again.
 */
public class AstralEnchantingMenu extends AbstractContainerMenu {
    public static final int ITEM_SLOT = 0;
    public static final int LAPIS_SLOT = 1;
    public static final int BLAZE_SLOT = 2;
    public static final int PLAYER_START = 3;
    public static final int PLAYER_END = PLAYER_START + 36;
    public static final int LEVELS_PER_ROW = 16;
    public static final int BUTTON_ENCHANT = 100;
    /** Slot positions (also read by the screen and ModScreenStyle). */
    public static final int ITEM_X = 17, ITEM_Y = 20, LAPIS_X = 8, BLAZE_X = 26, STOCK_Y = 44;
    /** Owner N31: a taller, airier screen - the player inventory 36 px lower than Vanilla's 84. */
    public static final int INVENTORY_Y = 120, IMAGE_HEIGHT = INVENTORY_Y + 82;
    private static final Identifier EMPTY_LAPIS = Identifier.withDefaultNamespace("container/slot/lapis_lazuli");
    private static final Identifier EMPTY_BLAZE = Identifier.withDefaultNamespace("container/slot/brewing_fuel");

    private final Container itemSlot = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            AstralEnchantingMenu.this.slotsChanged(this);
        }
    };
    private final Container stock;
    private final ContainerLevelAccess access;
    private final Player player;
    private final DataSlot tier = DataSlot.standalone();
    /** Shelf points * 2 + 1 with the floor (for the tooltip: what the next stage lacks). */
    private final DataSlot setup = DataSlot.standalone();
    /** Per row: enchantment id (holder id map, -1 none), max level, chosen level, points per level. */
    private final int[] enchant = {-1, -1, -1};
    private final int[] maxLevel = new int[3];
    private final int[] chosen = new int[3];
    private final int[] cost = new int[3];

    /** Client. */
    public AstralEnchantingMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(AstralEnchantingTableBlockEntity.SIZE), ContainerLevelAccess.NULL);
    }

    public AstralEnchantingMenu(int containerId, Inventory inventory, Container stock, ContainerLevelAccess access) {
        super(ModScreenHandlers.ASTRAL_ENCHANTING_MENU, containerId);
        checkContainerSize(stock, AstralEnchantingTableBlockEntity.SIZE);
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
        this.addSlot(new Slot(stock, AstralEnchantingTableBlockEntity.LAPIS, LAPIS_X, STOCK_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AstralEnchantingTableBlockEntity.fits(AstralEnchantingTableBlockEntity.LAPIS, stack);
            }

            @Override
            public Identifier getNoItemIcon() {
                return EMPTY_LAPIS;
            }
        });
        this.addSlot(new Slot(stock, AstralEnchantingTableBlockEntity.BLAZE, BLAZE_X, STOCK_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AstralEnchantingTableBlockEntity.fits(AstralEnchantingTableBlockEntity.BLAZE, stack);
            }

            @Override
            public Identifier getNoItemIcon() {
                return EMPTY_BLAZE;
            }
        });
        this.addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        this.addDataSlot(this.tier);
        this.addDataSlot(this.setup);
        for (int[] array : new int[][] {this.enchant, this.maxLevel, this.chosen, this.cost}) {
            for (int i = 0; i < 3; i++) this.addDataSlot(DataSlot.shared(array, i));
        }
        refresh();
    }

    // ------------------------------------------------------------------ state

    /** Server: reads the table's tier and draws the three enchantments for the item from the player's seed. */
    private void refresh() {
        this.access.execute((level, pos) -> {
            this.tier.set(readTier(level, pos));
            ItemStack stack = this.itemSlot.getItem(0);
            List<Holder<Enchantment>> choices = AstralEnchanting.choices(level.registryAccess(), stack, this.player.getEnchantmentSeed());
            IdMap<Holder<Enchantment>> ids = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
            for (int i = 0; i < 3; i++) {
                Holder<Enchantment> holder = i < choices.size() ? choices.get(i) : null;
                this.enchant[i] = holder == null ? -1 : ids.getId(holder);
                this.maxLevel[i] = holder == null ? 0 : holder.value().getMaxLevel();
                this.cost[i] = holder == null ? 0 : AstralEnchanting.pointsPerLevel(holder.value().getWeight());
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
        int points = AstralEnchanting.shelfPoints(level, pos);
        boolean floor = AstralEnchanting.hasFloor(level, pos);
        this.setup.set(points * 2 + (floor ? 1 : 0));
        int tier = AstralEnchanting.tier(points, floor);
        this.tier.set(tier);
        return tier;
    }

    public int shelfPoints() {
        return this.setup.get() >> 1;
    }

    public boolean hasFloor() {
        return (this.setup.get() & 1) != 0;
    }

    /** {@link AstralEnchanting#nextStep} for this table. */
    public int nextStep() {
        return AstralEnchanting.nextStep(shelfPoints(), hasFloor());
    }

    public int tier() {
        return this.tier.get();
    }

    public int budget() {
        return AstralEnchanting.budget(tier());
    }

    public int enchantId(int row) {
        return this.enchant[row];
    }

    public int maxLevel(int row) {
        return this.enchant[row] < 0 ? 0 : this.maxLevel[row];
    }

    public int chosen(int row) {
        return this.chosen[row];
    }

    public int pointsPerLevel(int row) {
        return this.cost[row];
    }

    public int[] chosenLevels() {
        return this.chosen.clone();
    }

    public int[] pointsPerLevel() {
        return this.cost.clone();
    }

    public int spent() {
        return AstralEnchanting.spent(this.chosen, this.cost);
    }

    /** Highest level the slider in {@code row} may reach now (the rest greyed out). */
    public int affordable(int row) {
        return AstralEnchanting.maxAffordable(row, this.chosen, this.cost, maxLevel(row), budget());
    }

    public int levelCost() {
        return AstralEnchanting.levelCost(spent(), tier());
    }

    public int requiredLevel() {
        return AstralEnchanting.requiredLevel(spent(), tier());
    }

    public int lapis() {
        return this.stock.getItem(AstralEnchantingTableBlockEntity.LAPIS).getCount();
    }

    public int blazePowder() {
        return this.stock.getItem(AstralEnchantingTableBlockEntity.BLAZE).getCount();
    }

    /** Whether {@code player} could enchant now (client: to light the button; server checks again). */
    public boolean canEnchant(Player player) {
        int spent = spent();
        if (this.itemSlot.getItem(0).isEmpty() || spent <= 0 || spent > budget()) return false;
        if (player.hasInfiniteMaterials()) return true;
        int levels = levelCost();
        return player.experienceLevel >= requiredLevel()
                && lapis() >= AstralEnchanting.lapisCost(levels) && blazePowder() >= AstralEnchanting.blazePowderCost(levels);
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

    private boolean enchant(Player player) {
        if (!canEnchant(player)) return false;
        return this.access.evaluate((level, pos) -> {
            int tier = readTier(level, pos);
            int spent = spent();
            if (spent > AstralEnchanting.budget(tier) || !canEnchant(player)) return false;
            int levels = AstralEnchanting.levelCost(spent, tier);
            ItemStack stack = this.itemSlot.getItem(0);
            ItemStack result = stack.is(Items.BOOK) ? stack.transmuteCopy(Items.ENCHANTED_BOOK, 1) : stack.copy();
            for (int row = 0; row < 3; row++) {
                Holder<Enchantment> holder = enchantment(level, row);
                if (holder != null && this.chosen[row] > 0) result.enchant(holder, Math.min(this.chosen[row], holder.value().getMaxLevel()));
            }
            player.onEnchantmentPerformed(result, levels);
            if (!player.hasInfiniteMaterials()) {
                this.stock.removeItem(AstralEnchantingTableBlockEntity.LAPIS, AstralEnchanting.lapisCost(levels));
                this.stock.removeItem(AstralEnchantingTableBlockEntity.BLAZE, AstralEnchanting.blazePowderCost(levels));
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
        return stillValid(this.access, player, ModBlocks.ASTRAL_ENCHANTING_TABLE) && this.stock.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < PLAYER_START) {
            if (!this.moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (AstralEnchantingTableBlockEntity.fits(AstralEnchantingTableBlockEntity.LAPIS, stack)) {
            if (!this.moveItemStackTo(stack, LAPIS_SLOT, LAPIS_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (AstralEnchantingTableBlockEntity.fits(AstralEnchantingTableBlockEntity.BLAZE, stack)) {
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
