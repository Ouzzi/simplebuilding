package com.simplesandwiches.block;

import com.simplesandwiches.registry.ModBlockEntities;
import com.simplesandwiches.sandwich.SandwichContents;
import com.simplesandwiches.sandwich.SandwichFormula;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What lies on a cutting board: nothing, a loaf, the opened bread with butter and up to five
 * ingredients (bottom to top), or the closed sandwich. The state machine lives in
 * {@link CuttingBoardBlock}; this class only stores and syncs.
 */
public class CuttingBoardBlockEntity extends BlockEntity {
    public enum Stage implements StringRepresentable {
        EMPTY, LOAF, OPEN, CLOSED;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        static Stage parse(String s) {
            for (Stage st : values()) if (st.getSerializedName().equals(s)) return st;
            return EMPTY;
        }
    }

    private Stage stage = Stage.EMPTY;
    private final List<ItemStack> ingredients = new ArrayList<>();
    private boolean buttered;

    public CuttingBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CUTTING_BOARD, pos, state);
    }

    public Stage stage() { return stage; }
    public boolean buttered() { return buttered; }
    public List<ItemStack> ingredients() { return List.copyOf(ingredients); }

    void setStage(Stage stage) {
        this.stage = stage;
        if (stage == Stage.EMPTY || stage == Stage.LOAF) {
            ingredients.clear();
            buttered = false;
        }
        changed();
    }

    void addIngredient(ItemStack stack) {
        ingredients.add(stack.copyWithCount(1));
        changed();
    }

    ItemStack popIngredient() {
        if (ingredients.isEmpty()) return ItemStack.EMPTY;
        ItemStack top = ingredients.remove(ingredients.size() - 1);
        changed();
        return top;
    }

    void setButtered(boolean buttered) {
        this.buttered = buttered;
        changed();
    }

    /** Loads a finished sandwich onto an empty board (it can then be reopened with the knife). */
    void loadSandwich(SandwichContents contents) {
        ingredients.clear();
        for (Holder<Item> h : contents.ingredients()) ingredients.add(new ItemStack(h));
        buttered = contents.buttered();
        stage = Stage.CLOSED;
        changed();
    }

    public SandwichContents contents() {
        List<Holder<Item>> list = new ArrayList<>();
        for (ItemStack s : ingredients) list.add(s.typeHolder());
        return new SandwichContents(list, buttered);
    }

    public ItemStack sandwich() {
        return SandwichFormula.create(contents());
    }

    /** Everything that drops when the board is broken. Spread butter is lost (it is on the bread). */
    public List<ItemStack> drops() {
        List<ItemStack> out = new ArrayList<>();
        switch (stage) {
            case LOAF -> out.add(new ItemStack(Items.BREAD));
            case OPEN -> {
                out.add(new ItemStack(Items.BREAD));
                ingredients.forEach(s -> out.add(s.copy()));
            }
            case CLOSED -> out.add(sandwich());
            default -> {}
        }
        return out;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            for (ItemStack s : drops()) Block.popResource(level, pos, s);
        }
        stage = Stage.EMPTY;
        ingredients.clear();
        buttered = false;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        stage = Stage.parse(input.getStringOr("Stage", "empty"));
        buttered = input.getBooleanOr("Buttered", false);
        ingredients.clear();
        input.read("Ingredients", ItemStack.CODEC.listOf()).ifPresent(list -> {
            for (ItemStack s : list) if (ingredients.size() < SandwichContents.MAX && !s.isEmpty()) ingredients.add(s.copyWithCount(1));
        });
        if (stage == Stage.EMPTY || stage == Stage.LOAF) {
            ingredients.clear();
            buttered = false;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Stage", stage.getSerializedName());
        if (buttered) output.putBoolean("Buttered", true);
        if (!ingredients.isEmpty()) output.store("Ingredients", ItemStack.CODEC.listOf(), List.copyOf(ingredients));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
