package com.simplebuilding.modules.simpletools.strip;

import com.simplebuilding.modules.simpletools.SimpleTools;
import com.simplebuilding.modules.simpletools.ToolsRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What lies in a strip station cage: an input stack (put in by hand or by a hopper from above) and an output stack
 * (what the axe made of it; a hopper below takes only this). One axe click works the whole input stack.
 */
public class StripStationBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int INPUT = 0, OUTPUT = 1;
    private static final int[] NONE = {}, IN = {INPUT}, OUT = {OUTPUT};

    /** What one axe click did. */
    public enum Outcome { DONE, PARTIAL, WORN, BLOCKED, NOTHING }

    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;

    public StripStationBlockEntity(BlockPos pos, BlockState state) {
        super(ToolsRegistry.STRIP_STATION_BE, pos, state);
    }

    public ItemStack input() { return input; }
    public ItemStack output() { return output; }

    /** Whether any axe does something to this item (Vanilla's Iron Axe stands for "an axe"). */
    public static boolean workable(ItemStack stack) {
        return !stack.isEmpty() && StripWork.step(new ItemStack(Items.IRON_AXE), stack) != null;
    }

    private boolean hoppersOn() {
        return SimpleTools.active(level) && SimpleTools.configFor(level).stripStationHopper;
    }

    // --- hand actions (server) ----------------------------------------------------------------

    /** Puts as much of {@code held} into the input as fits (same item only); returns how many went in. */
    public int insert(ItemStack held) {
        if (!workable(held)) return 0;
        if (!input.isEmpty() && !ItemStack.isSameItemSameComponents(input, held)) return 0;
        int room = held.getMaxStackSize() - input.getCount();
        int moved = Math.min(room, held.getCount());
        if (moved <= 0) return 0;
        if (input.isEmpty()) input = held.copyWithCount(moved);
        else input.grow(moved);
        held.shrink(moved);
        changed();
        return moved;
    }

    /** The output, else the input, or empty. */
    public ItemStack take() {
        ItemStack out = !output.isEmpty() ? output : input;
        if (out.isEmpty()) return ItemStack.EMPTY;
        if (out == output) output = ItemStack.EMPTY;
        else input = ItemStack.EMPTY;
        changed();
        return out;
    }

    /** Whether this axe has something to do here (also on the client, to predict the click). */
    public boolean canWork(ItemStack axe) {
        return StripWork.step(axe, input) != null || (input.isEmpty() && StripWork.step(axe, output) != null);
    }

    /**
     * The axe works the whole input stack: one durability per block, only as many as the axe can spare (it is left at 1
     * and never breaks), sound and particles once. Copper goes one step per click (wax first, then oxidation) like Vanilla;
     * a finished output that the axe can work further moves back to the input on the next click.
     */
    public Outcome work(ServerLevel server, Player player, InteractionHand hand) {
        ItemStack axe = player.getItemInHand(hand);
        if (input.isEmpty() && StripWork.step(axe, output) != null) {
            input = output;
            output = ItemStack.EMPTY;
        }
        StripWork.Step step = StripWork.step(axe, input);
        if (step == null) return Outcome.NOTHING;
        ItemStack result = step.result();
        if (!output.isEmpty() && !ItemStack.isSameItemSameComponents(output, result)) return Outcome.BLOCKED;
        int want = Math.min(input.getCount(), result.getMaxStackSize() - output.getCount());
        if (want <= 0) return Outcome.BLOCKED;
        int count = want;
        if (!player.hasInfiniteMaterials() && axe.isDamageableItem()) count = Math.min(count, axe.getMaxDamage() - axe.getDamageValue() - 1);
        if (count <= 0) return Outcome.WORN;
        ItemStack worked = input.copyWithCount(1);
        input.shrink(count);
        if (input.isEmpty()) input = ItemStack.EMPTY;
        if (output.isEmpty()) output = result.copyWithCount(count);
        else output.grow(count);
        if (!player.hasInfiniteMaterials()) axe.hurtAndBreak(count, player, hand.asEquipmentSlot());
        BlockPos pos = getBlockPos();
        server.playSound(null, pos, step.sound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        if (step.particle() == net.minecraft.core.component.BlockTransformer.TransformParticle.NONE) {
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, worked), pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 8, 0.25, 0.2, 0.25, 0.05);
        } else {
            step.particle().send(server, null, pos);
        }
        changed();
        return count < want ? Outcome.PARTIAL : Outcome.DONE;
    }

    // --- container (hoppers) -------------------------------------------------------------------

    @Override
    public int getContainerSize() { return 2; }

    @Override
    public boolean isEmpty() { return input.isEmpty() && output.isEmpty(); }

    @Override
    public ItemStack getItem(int slot) { return slot == INPUT ? input : slot == OUTPUT ? output : ItemStack.EMPTY; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack have = getItem(slot);
        if (have.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack out = have.split(amount);
        changed();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack have = getItem(slot);
        if (slot == INPUT) input = ItemStack.EMPTY;
        else if (slot == OUTPUT) output = ItemStack.EMPTY;
        return have;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == INPUT) input = stack;
        else if (slot == OUTPUT) output = stack;
        else return;
        changed();
    }

    @Override
    public boolean stillValid(Player player) { return false; }

    @Override
    public void clearContent() {
        input = ItemStack.EMPTY;
        output = ItemStack.EMPTY;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) { return slot == INPUT && workable(stack); }

    @Override
    public int[] getSlotsForFace(Direction face) {
        if (!hoppersOn()) return NONE;
        return face == Direction.UP ? IN : face == Direction.DOWN ? OUT : NONE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        return hoppersOn() && face == Direction.UP && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return hoppersOn() && face == Direction.DOWN && slot == OUTPUT;
    }

    // --- storage and sync ----------------------------------------------------------------------

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            if (!input.isEmpty()) Block.popResource(level, pos, input);
            if (!output.isEmpty()) Block.popResource(level, pos, output);
        }
        clearContent();
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        input = in.read("Input", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        output = in.read("Output", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!input.isEmpty()) out.store("Input", ItemStack.CODEC, input);
        if (!output.isEmpty()) out.store("Output", ItemStack.CODEC, output);
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
