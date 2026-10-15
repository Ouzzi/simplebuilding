package com.simplebeacons.beacon;

import com.google.common.collect.ImmutableList;
import com.simplebeacons.BeaconsRegistry;
import com.simplebeacons.SimpleBeacons;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Strong Beacon block entity: Vanilla's {@code BeaconBlockEntity} rebuilt with up to six pyramid levels and the
 * effect list of {@link StrongBeaconLogic}. Saved under Vanilla's keys ({@code primary_effect}, {@code secondary_effect},
 * {@code Levels}, {@code CustomName}, {@code lock}) so the in-world upgrade only swaps the type.
 */
public class StrongBeaconBlockEntity extends BlockEntity implements MenuProvider, Nameable, BeaconBeamOwner {
    private static final Component DEFAULT_NAME = Component.translatable("container.simplebeacons.strong_beacon");
    private List<BeaconBeamOwner.Section> beamSections = new ArrayList<>();
    private List<BeaconBeamOwner.Section> checkingBeamSections = new ArrayList<>();
    int levels;
    private int lastCheckY;
    @Nullable Holder<MobEffect> primaryPower;
    @Nullable Holder<MobEffect> secondaryPower;
    private @Nullable Component name;
    private LockCode lockKey = LockCode.NO_LOCK;
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int dataId) {
            return switch (dataId) {
                case 0 -> StrongBeaconBlockEntity.this.levels;
                case 1 -> BeaconMenu.encodeEffect(StrongBeaconBlockEntity.this.primaryPower);
                case 2 -> BeaconMenu.encodeEffect(StrongBeaconBlockEntity.this.secondaryPower);
                // The server's settings travel with the menu data, so the client screen never reads its own config.
                case StrongBeaconMenu.DATA_MAX_LEVELS -> StrongBeaconLogic.maxLevels();
                case StrongBeaconMenu.DATA_RANGE_BASE -> com.simplebeacons.config.BeaconsConfig.strongBeaconRangeBase;
                case StrongBeaconMenu.DATA_RANGE_PER_LEVEL -> com.simplebeacons.config.BeaconsConfig.strongBeaconRangePerLevel;
                case StrongBeaconMenu.DATA_EFFECTS_ON -> StrongBeaconMenu.effectsMask();
                default -> 0;
            };
        }

        @Override
        public void set(int dataId, int value) {
            switch (dataId) {
                case 0 -> StrongBeaconBlockEntity.this.levels = value;
                case 1 -> {
                    if (StrongBeaconBlockEntity.this.level != null && !StrongBeaconBlockEntity.this.level.isClientSide()
                            && !StrongBeaconBlockEntity.this.beamSections.isEmpty()) {
                        playSound(StrongBeaconBlockEntity.this.level, StrongBeaconBlockEntity.this.worldPosition, SoundEvents.BEACON_POWER_SELECT);
                    }
                    StrongBeaconBlockEntity.this.primaryPower = filter(BeaconMenu.decodeEffect(value));
                }
                case 2 -> StrongBeaconBlockEntity.this.secondaryPower = filter(BeaconMenu.decodeEffect(value));
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return StrongBeaconMenu.DATA_COUNT;
        }
    };

    public StrongBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(BeaconsRegistry.STRONG_BEACON_BE, pos, state);
    }

    private static @Nullable Holder<MobEffect> filter(@Nullable Holder<MobEffect> effect) {
        return StrongBeaconLogic.isKnown(effect) ? effect : null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StrongBeaconBlockEntity entity) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        BlockPos checkPos;
        if (entity.lastCheckY < y) {
            checkPos = pos;
            entity.checkingBeamSections = new ArrayList<>();
            entity.lastCheckY = checkPos.getY() - 1;
        } else {
            checkPos = new BlockPos(x, entity.lastCheckY + 1, z);
        }
        BeaconBeamOwner.Section last = entity.checkingBeamSections.isEmpty() ? null
                : entity.checkingBeamSections.get(entity.checkingBeamSections.size() - 1);
        int lastSetBlock = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        for (int i = 0; i < 10 && checkPos.getY() <= lastSetBlock; i++) {
            BlockState above = level.getBlockState(checkPos);
            // Vanilla rule (the NeoForge hook getBeaconColorMultiplier is not on Fabric/Forge): beam blocks tint.
            Integer color = above.getBlock() instanceof net.minecraft.world.level.block.BeaconBeamBlock beam ? beam.getColor().getTextureDiffuseColor() : null;
            if (color != null) {
                if (entity.checkingBeamSections.size() <= 1) {
                    last = new BeaconBeamOwner.Section(color);
                    entity.checkingBeamSections.add(last);
                } else if (last != null) {
                    if (color == last.getColor()) {
                        last.increaseHeight();
                    } else {
                        last = new BeaconBeamOwner.Section(ARGB.average(last.getColor(), color));
                        entity.checkingBeamSections.add(last);
                    }
                }
            } else {
                if (last == null || above.getLightDampening() >= 15 && !above.is(Blocks.BEDROCK)) {
                    entity.checkingBeamSections.clear();
                    entity.lastCheckY = lastSetBlock;
                    break;
                }
                last.increaseHeight();
            }
            checkPos = checkPos.above();
            entity.lastCheckY++;
        }

        int previousLevels = entity.levels;
        if (level.getGameTime() % 80L == 0L) {
            if (!entity.beamSections.isEmpty()) {
                entity.levels = SimpleBeacons.on() ? StrongBeaconLogic.scanPyramid(level, pos, StrongBeaconLogic.maxLevels()) : 0;
            }
            if (entity.levels > 0 && !entity.beamSections.isEmpty()) {
                StrongBeaconLogic.applyEffects(level, pos, entity.levels, entity.primaryPower, entity.secondaryPower);
                playSound(level, pos, SoundEvents.BEACON_AMBIENT);
            }
        }

        if (entity.lastCheckY >= lastSetBlock) {
            entity.lastCheckY = level.getMinY() - 1;
            boolean wasActive = previousLevels > 0;
            entity.beamSections = entity.checkingBeamSections;
            if (!level.isClientSide()) {
                boolean isActive = entity.levels > 0;
                if (!wasActive && isActive) {
                    playSound(level, pos, SoundEvents.BEACON_ACTIVATE);
                    for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(x, y, z, x, y - 4, z).inflate(10.0, 5.0, 10.0))) {
                        CriteriaTriggers.CONSTRUCT_BEACON.trigger(player, entity.levels);
                    }
                } else if (wasActive && !isActive) {
                    playSound(level, pos, SoundEvents.BEACON_DEACTIVATE);
                }
            }
        }
    }

    public static void playSound(Level level, BlockPos pos, SoundEvent event) {
        level.playSound(null, pos, event, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    public int levels() {
        return this.levels;
    }

    public @Nullable Holder<MobEffect> primary() {
        return this.primaryPower;
    }

    public @Nullable Holder<MobEffect> secondary() {
        return this.secondaryPower;
    }

    /** Tests and the test centre: sets the levels as a finished pyramid scan would. */
    public void setLevelsForTest(int levels) {
        this.levels = levels;
    }

    /** The data the menu syncs (levels, primary, secondary). */
    public ContainerData dataAccess() {
        return this.dataAccess;
    }

    @Override
    public void setRemoved() {
        if (this.level != null) playSound(this.level, this.worldPosition, SoundEvents.BEACON_DEACTIVATE);
        super.setRemoved();
    }

    @Override
    public List<BeaconBeamOwner.Section> getBeamSections() {
        return this.levels == 0 ? ImmutableList.of() : this.beamSections;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    private static void storeEffect(ValueOutput output, String field, @Nullable Holder<MobEffect> effect) {
        if (effect != null) effect.unwrapKey().ifPresent(key -> output.putString(field, key.identifier().toString()));
    }

    private static @Nullable Holder<MobEffect> loadEffect(ValueInput input, String field) {
        return input.read(field, BuiltInRegistries.MOB_EFFECT.holderByNameCodec()).filter(StrongBeaconLogic::isKnown).orElse(null);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.primaryPower = loadEffect(input, "primary_effect");
        this.secondaryPower = loadEffect(input, "secondary_effect");
        this.levels = input.getIntOr("Levels", 0);
        this.name = parseCustomNameSafe(input, "CustomName");
        this.lockKey = LockCode.fromTag(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        storeEffect(output, "primary_effect", this.primaryPower);
        storeEffect(output, "secondary_effect", this.secondaryPower);
        output.putInt("Levels", this.levels);
        output.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
        this.lockKey.addToTag(output);
    }

    public void setCustomName(@Nullable Component name) {
        this.name = name;
    }

    @Override
    public @Nullable Component getCustomName() {
        return this.name;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (this.lockKey.canUnlock(player)) {
            return new StrongBeaconMenu(containerId, inventory, this.dataAccess, ContainerLevelAccess.create(this.level, this.getBlockPos()));
        }
        BaseContainerBlockEntity.sendChestLockedNotifications(Vec3.atCenterOf(this.getBlockPos()), player, this.getDisplayName());
        return null;
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    public Component getName() {
        return this.name != null ? this.name : DEFAULT_NAME;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.name = components.get(DataComponents.CUSTOM_NAME);
        this.lockKey = components.getOrDefault(DataComponents.LOCK, LockCode.NO_LOCK);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CUSTOM_NAME, this.name);
        if (!this.lockKey.equals(LockCode.NO_LOCK)) components.set(DataComponents.LOCK, this.lockKey);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("CustomName");
        output.discard("lock");
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        this.lastCheckY = level.getMinY() - 1;
    }
}
