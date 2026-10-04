package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.custom.AutoSmitherBlock;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.recipe.CountBasedSmithingRecipe;
import com.simplebuilding.screen.AutoSmitherMenu;
import com.simplebuilding.util.TrimUpgrades;
import java.util.Optional;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Inhalt des Auto-Schmieds: Vorlage (0), Basis (1), Material (2). Trichter und Seiten legen nach Vanillas Schmiede-Mengen
 * ein ({@link RecipePropertySet#SMITHING_TEMPLATE}, {@code _BASE}, {@code _ADDITION} - dieselben Pruefungen wie die Slots
 * des Schmiedetischs); nur fertige Ergebnisse im vierten Slot lassen sich herausziehen.
 *
 * <p>Ergebnis wie am Schmiedetisch: erst die Mod-Aufwertungen ({@link TrimUpgrades}, sonst rechnet Vanilla), dann
 * {@link RecipeType#SMITHING}; verbraucht wird je ein Teil, beim {@link CountBasedSmithingRecipe} dessen Material-Anzahl.
 */
public class AutoSmitherBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int TEMPLATE_SLOT = 0;
    public static final int BASE_SLOT = 1;
    public static final int ADDITION_SLOT = 2;
    public static final int RESULT_SLOT = 3;
    public static final int SIZE = 4;
    private static final int[] SLOTS = IntStream.range(0, SIZE).toArray();
    private static final Component DEFAULT_NAME = Component.translatable("container.simplebuilding.auto_smither");

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int craftingTicksRemaining;
    /** Fuer die Redstone-Anzeige im Menue (wie beim Crafter): 1, solange ein Signal anliegt. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return AutoSmitherBlockEntity.this.getBlockState().getValue(AutoSmitherBlock.TRIGGERED) ? 1 : 0;
        }

        @Override
        public void set(int id, int value) {
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    /** Ergebnis eines Schmiedevorgangs und wie viel Material er braucht. */
    public record Outcome(ItemStack result, int additionCount) {
    }

    public AutoSmitherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_SMITHER_BE, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AutoSmitherBlockEntity smither) {
        if (smither.craftingTicksRemaining > 0 && --smither.craftingTicksRemaining == 0 && state.getValue(AutoSmitherBlock.CRAFTING)) {
            level.setBlockAndUpdate(pos, state.setValue(AutoSmitherBlock.CRAFTING, false));
        }
    }

    public void setCraftingTicksRemaining(int ticks) {
        this.craftingTicksRemaining = ticks;
    }

    /** Was die aktuellen Eingaenge ergeben, oder null. */
    public @Nullable Outcome outcome(ServerLevel level) {
        return outcome(level, getItem(TEMPLATE_SLOT), getItem(BASE_SLOT), getItem(ADDITION_SLOT));
    }

    /** Ergebnis aus drei Stapeln wie am Schmiedetisch (auch fuer die Vorschau im Menue). */
    public static @Nullable Outcome outcome(ServerLevel level, ItemStack template, ItemStack base, ItemStack addition) {
        ItemStack upgrade = TrimUpgrades.result(template, base, addition);
        if (upgrade != null) {
            return upgrade.isEmpty() ? null : new Outcome(upgrade, 1);
        }
        SmithingRecipeInput input = new SmithingRecipeInput(template, base, addition);
        Optional<RecipeHolder<SmithingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.SMITHING, input, level);
        if (recipe.isEmpty()) {
            return null;
        }
        ItemStack result = recipe.get().value().assemble(input);
        int additionCount = recipe.get().value() instanceof CountBasedSmithingRecipe counted ? Math.max(1, counted.getAdditionCount()) : 1;
        if (result.isEmpty() || (!addition.isEmpty() && addition.getCount() < additionCount)) {
            return null;
        }
        return new Outcome(result, additionCount);
    }

    /** Verbraucht die Teile eines Schmiedevorgangs. */
    public void consume(Outcome outcome) {
        removeItem(TEMPLATE_SLOT, 1);
        removeItem(BASE_SLOT, 1);
        removeItem(ADDITION_SLOT, outcome.additionCount());
        setChanged();
    }

    /** Komparator: belegte Eingaenge, 5 je Slot (0, 5, 10, 15). */
    public int redstoneSignal() {
        int filled = 0;
        for (int slot = 0; slot < RESULT_SLOT; slot++) {
            if (!getItem(slot).isEmpty()) {
                filled++;
            }
        }
        return filled * 5;
    }

    /** Welcher Slot dieses Item annimmt, nach Vanillas Schmiede-Mengen; -1, wenn keiner. */
    public int slotFor(ItemStack stack) {
        for (int slot = 0; slot < RESULT_SLOT; slot++) {
            if (fitsSlot(slot, stack)) {
                return slot;
            }
        }
        return -1;
    }

    private boolean fitsSlot(int slot, ItemStack stack) {
        if (this.level == null || stack.isEmpty() || slot < 0 || slot >= RESULT_SLOT) {
            return false;
        }
        var key = switch (slot) {
            case TEMPLATE_SLOT -> RecipePropertySet.SMITHING_TEMPLATE;
            case BASE_SLOT -> RecipePropertySet.SMITHING_BASE;
            default -> RecipePropertySet.SMITHING_ADDITION;
        };
        return this.level.recipeAccess().propertySet(key).test(stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return fitsSlot(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == RESULT_SLOT;
    }

    /** Reserve space before consuming inputs, even if the front container is full. */
    public boolean canStoreResult(ItemStack result) {
        ItemStack stored = getItem(RESULT_SLOT);
        return (stored.isEmpty() || ItemStack.isSameItemSameComponents(stored, result))
                && stored.getCount() + result.getCount() <= getMaxStackSize(result);
    }

    public void storeResult(ItemStack result) {
        if (result.isEmpty()) {
            return;
        }
        ItemStack stored = getItem(RESULT_SLOT);
        if (stored.isEmpty()) {
            setItem(RESULT_SLOT, result);
        } else {
            stored.grow(result.getCount());
            setChanged();
        }
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new AutoSmitherMenu(containerId, inventory, this, this.data);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.craftingTicksRemaining = input.getIntOr("crafting_ticks_remaining", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("crafting_ticks_remaining", this.craftingTicksRemaining);
    }
}
