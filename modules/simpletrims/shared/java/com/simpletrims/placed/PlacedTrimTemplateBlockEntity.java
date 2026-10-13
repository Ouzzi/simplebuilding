package com.simpletrims.placed;

import com.simpletrims.TrimsRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the placed template (key {@code Template}, with all components) and sends it to the client. Named after the
 * template (Jade and every other display show "Glowing Armor Trim", not the block). Not saved: the strike counter
 * (expires after {@link PlacedTemplates#HIT_RESET_TICKS}) and the time of the last hint sound.
 */
public class PlacedTrimTemplateBlockEntity extends BlockEntity implements Nameable {
    private static final String TEMPLATE_TAG = "Template";

    private ItemStack template = ItemStack.EMPTY;
    private int hits;
    private @Nullable Item hitCatalyst;
    private long lastHitTime = Long.MIN_VALUE / 2;
    private long lastHintSound = Long.MIN_VALUE / 2;

    public PlacedTrimTemplateBlockEntity(BlockPos pos, BlockState state) {
        super(TrimsRegistry.PLACED_TEMPLATE_BE, pos, state);
    }

    public ItemStack getTemplate() {
        return template;
    }

    public void setTemplate(ItemStack stack) {
        template = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        hits = 0;
        hitCatalyst = null;
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public Component getName() {
        return template.isEmpty() ? getBlockState().getBlock().getName() : template.getHoverName();
    }

    @Override
    public @Nullable Component getCustomName() {
        return template.isEmpty() ? null : template.getHoverName();
    }

    public int hits() {
        return hits;
    }

    public long lastHitTime() {
        return lastHitTime;
    }

    /** Counts a strike with this catalyst at {@code now}; another catalyst or a long pause start over. */
    public int registerHit(Item catalyst, long now) {
        if (catalyst != hitCatalyst || now - lastHitTime > PlacedTemplates.HIT_RESET_TICKS) hits = 0;
        hits++;
        hitCatalyst = catalyst;
        lastHitTime = now;
        return hits;
    }

    public boolean takeHintSound(long now, int interval) {
        if (now - lastHintSound < interval) return false;
        lastHintSound = now;
        return true;
    }

    public Direction normal() {
        BlockState state = getBlockState();
        return state.getBlock() instanceof PlacedTrimTemplateBlock ? PlacedTrimTemplateBlock.normal(state) : Direction.UP;
    }

    /** Middle of the visible side, for particles. */
    public Vec3 surfaceCentre() {
        Direction normal = normal();
        double offset = -0.5 + 2.0 / 16.0;
        return Vec3.atCenterOf(worldPosition).add(normal.getStepX() * offset, normal.getStepY() * offset, normal.getStepZ() * offset);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PlacedTrimTemplateBlockEntity be) {
        if (level instanceof ServerLevel server && (level.getGameTime() + pos.asLong()) % PlacedTemplates.HINT_INTERVAL == 0) {
            PlacedTemplates.tryHint(server, pos, be);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        template = input.read(TEMPLATE_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!template.isEmpty()) output.store(TEMPLATE_TAG, ItemStack.CODEC, template);
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
