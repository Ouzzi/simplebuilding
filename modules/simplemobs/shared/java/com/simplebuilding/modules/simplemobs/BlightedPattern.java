package com.simplebuilding.modules.simplemobs;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Blighted Wither summoning (owner N50): the Vanilla Wither figure - T of soul sand/soul soil with three wither skeleton
 * skulls - but before the third skull a helmet is put on the MIDDLE skull (right-click the skull with it, like the Dread):
 * a Netherite Helmet calls the Blighted Wither, a helmet of {@code #simplemobs:abyssal_wither_crowns} (Enderite) the
 * Abyssal Wither. The third skull then summons it instead of the Wither and uses up the helmet. Without a helmet the
 * Vanilla Wither comes as always; on a complete figure the Wither has already come, so the helmet must sit first.
 * <pre>
 *  ^ H ^    H = wither skeleton skull wearing the helmet, ^ = wither skeleton skull
 *  # # #    # = #minecraft:wither_summon_base_blocks
 *  . # .    . = air
 * </pre>
 * The helmet sits on the skull as a Vanilla item display (saved with the world); breaking the skull drops it again.
 * Not on Peaceful, switchable with {@code witherSummonEnabled}.
 */
public final class BlightedPattern {
    /** Entity tag of the helmet display on a crowned skull. */
    public static final String CROWN_TAG = "simplemobs.wither_crown";
    private static @Nullable BlockPattern pattern, open;

    private BlightedPattern() {}

    private static java.util.function.Predicate<BlockState> skull() {
        return BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_SKULL).or(BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_WALL_SKULL));
    }

    /** The Vanilla Wither figure (WitherSkullBlock keeps its own private). */
    public static BlockPattern pattern() {
        if (pattern == null) {
            pattern = BlockPatternBuilder.start()
                    .aisle("^^^", "###", "~#~")
                    .where('#', b -> b.getState().is(BlockTags.WITHER_SUMMON_BASE_BLOCKS))
                    .where('^', BlockInWorld.hasState(skull()))
                    .where('~', b -> b.getState().isAir())
                    .build();
        }
        return pattern;
    }

    /** The figure with the middle skull in place and the side places free or holding a skull. */
    private static BlockPattern open() {
        if (open == null) {
            open = BlockPatternBuilder.start()
                    .aisle("?^?", "###", "~#~")
                    .where('#', b -> b.getState().is(BlockTags.WITHER_SUMMON_BASE_BLOCKS))
                    .where('^', BlockInWorld.hasState(skull()))
                    .where('?', b -> b.getState().isAir() || skull().test(b.getState()))
                    .where('~', b -> b.getState().isAir())
                    .build();
        }
        return open;
    }

    /** The middle skull of a match. */
    public static BlockPos middle(BlockPattern.BlockPatternMatch match) {
        return match.getBlock(1, 0, 0).getPos();
    }

    /** The stage a helmet summons, or null when it is no crown. */
    public static WitherLogic.@Nullable Stage stageOf(ItemStack helmet) {
        if (helmet.is(Items.NETHERITE_HELMET)) return WitherLogic.NETHERITE;
        if (helmet.is(WitherMobs.ABYSSAL_CROWNS)) return WitherLogic.ENDERITE;
        return null;
    }

    /** Whether a helmet may be put on this skull now: the middle skull of an unfinished figure, not crowned yet. */
    public static boolean canCrown(Level level, BlockPos skull) {
        if (!MobsRegistry.on() || !MobsConfig.witherSummonEnabled) return false;
        if (level.getDifficulty() == Difficulty.PEACEFUL || skull.getY() < level.getMinY()) return false;
        if (!skull().test(level.getBlockState(skull)) || crownAt(level, skull) != null) return false;
        BlockPattern.BlockPatternMatch match = open().find(level, skull);
        if (match == null || !middle(match).equals(skull)) return false;
        return level.getBlockState(match.getBlock(0, 0, 0).getPos()).isAir() || level.getBlockState(match.getBlock(2, 0, 0).getPos()).isAir()
                // BS28b-2: a complete figure waits for its crown when a side skull wears a helmet (no Vanilla Wither came)
                || !outerHelmets(level, match).isEmpty();
    }

    /** The helmets of the two outer skulls of a figure (Simple Mobheads), empty stacks left out. */
    public static List<ItemStack> outerHelmets(Level level, BlockPattern.BlockPatternMatch match) {
        List<ItemStack> out = new ArrayList<>();
        for (int x : new int[] {0, 2}) {
            ItemStack helmet = ArmoredWither.helmetAt(level, match.getBlock(x, 0, 0).getPos());
            if (!helmet.isEmpty()) out.add(helmet);
        }
        return out;
    }

    /**
     * {@code WitherSkullBlock#checkSpawn} (BS28b-2): a complete figure with a helmet on a side skull does not call the Vanilla
     * Wither; it waits for the crown of the middle skull. True = Vanilla must not spawn (smoke at the middle skull).
     */
    public static boolean armoredWaits(Level level, BlockPos skull) {
        if (!(level instanceof ServerLevel server) || !MobsRegistry.on() || !MobsConfig.witherSummonEnabled) return false;
        if (level.getDifficulty() == Difficulty.PEACEFUL || skull.getY() < level.getMinY() || !skull().test(level.getBlockState(skull))) return false;
        BlockPattern.BlockPatternMatch match = pattern().find(level, skull);
        if (match == null || outerHelmets(level, match).isEmpty()) return false;
        BlockPos middle = middle(match);
        server.sendParticles(ParticleTypes.SOUL, middle.getX() + 0.5, middle.getY() + 0.7, middle.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.01);
        return true;
    }

    /** The helmet display on this skull, if any. */
    public static Display.@Nullable ItemDisplay crownAt(Level level, BlockPos skull) {
        var found = level.getEntitiesOfClass(Display.ItemDisplay.class, new AABB(skull), d -> d.entityTags().contains(CROWN_TAG));
        return found.isEmpty() ? null : found.getFirst();
    }

    /** The helmet of a crown display. */
    public static ItemStack helmet(Display.ItemDisplay crown) {
        var slot = crown.getSlot(0);
        return slot == null ? ItemStack.EMPTY : slot.get();
    }

    /** Puts one helmet on the skull (server): it sits there as an item display until the third skull or the skull breaks. */
    public static boolean crown(Level level, BlockPos skull, ItemStack helmet) {
        if (!(level instanceof ServerLevel server) || stageOf(helmet) == null || !canCrown(level, skull)) return false;
        CompoundTag tag = new CompoundTag();
        tag.putString("billboard", "vertical");
        CompoundTag t = new CompoundTag();
        t.put("left_rotation", floats(0, 0, 0, 1));
        t.put("right_rotation", floats(0, 0, 0, 1));
        t.put("translation", floats(0, 0, 0));
        t.put("scale", floats(0.55f, 0.55f, 0.55f));
        tag.put("transformation", t);
        ListTag tags = new ListTag();
        tags.add(StringTag.valueOf(CROWN_TAG));
        tag.put("Tags", tags);
        var input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag);
        var entity = EntityType.create(EntityTypes.ITEM_DISPLAY, input, level, EntitySpawnReason.TRIGGERED).orElse(null);
        if (!(entity instanceof Display.ItemDisplay display)) return false;
        var slot = display.getSlot(0);
        if (slot == null) return false;
        slot.set(helmet.copyWithCount(1));
        BlockState state = level.getBlockState(skull);
        var box = state.getShape(level, skull).bounds();
        // centred over the skull's head, inside the skull's block so the display stays tied to it
        display.snapTo(skull.getX() + (box.minX + box.maxX) / 2, skull.getY() + box.maxY + 0.2, skull.getZ() + (box.minZ + box.maxZ) / 2, 0f, 0f);
        server.addFreshEntity(display);
        server.playSound(null, skull, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.BLOCKS, 1.2F, 0.6F);
        server.sendParticles(ParticleTypes.SOUL, skull.getX() + 0.5, skull.getY() + 0.7, skull.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.01);
        trySummon(level, skull); // BS28b-2: on a complete armored figure the crown is the last piece
        return true;
    }

    private static ListTag floats(float... values) {
        ListTag list = new ListTag();
        for (float v : values) list.add(net.minecraft.nbt.FloatTag.valueOf(v));
        return list;
    }

    /** A crown whose skull is gone drops its helmet (called every 10 ticks from the display's tick). */
    public static void tickCrown(ServerLevel level, Display.ItemDisplay crown) {
        if (skull().test(level.getBlockState(crown.blockPosition()))) return;
        ItemStack helmet = helmet(crown);
        if (!helmet.isEmpty()) crown.spawnAtLocation(level, helmet.copy());
        crown.discard();
    }

    /** A helmet put on a skull where it cannot sit: a puff of smoke, the helmet is kept. */
    public static void fizzle(Level level, BlockPos skull) {
        DreadPattern.fizzle(level, skull);
    }

    /** A skull was placed at {@code skull}: summons the Blighted or Abyssal Wither if the middle skull wears a helmet (server). */
    public static boolean trySummon(Level level, BlockPos skull) {
        if (!(level instanceof ServerLevel server) || !MobsRegistry.on() || !MobsConfig.witherSummonEnabled) return false;
        if (level.getDifficulty() == Difficulty.PEACEFUL || skull.getY() < level.getMinY()) return false;
        if (!skull().test(level.getBlockState(skull))) return false;
        BlockPattern.BlockPatternMatch match = pattern().find(level, skull);
        if (match == null) return false;
        Display.ItemDisplay crown = crownAt(level, middle(match));
        WitherLogic.Stage stage = crown == null ? null : stageOf(helmet(crown));
        if (stage == null) return false;
        BlightedWither wither = (stage == WitherLogic.ENDERITE ? WitherMobs.ABYSSAL_WITHER : WitherMobs.BLIGHTED_WITHER).create(server, EntitySpawnReason.TRIGGERED);
        if (wither == null) return false;
        crown.discard();
        // BS28b-2: the side helmets are used up and make the Wither a little stronger (and pay back when it dies)
        List<Item> sides = new ArrayList<>();
        for (int x : new int[] {0, 2}) {
            BlockPos side = match.getBlock(x, 0, 0).getPos();
            ItemStack helmet = ArmoredWither.helmetAt(level, side);
            if (!helmet.isEmpty()) {
                sides.add(helmet.getItem());
                ArmoredWither.consume(level, side);
            }
        }
        ArmoredWither.apply(wither, sides);
        CarvedPumpkinBlock.clearPatternBlocks(level, match);
        BlockPos spawn = match.getBlock(1, 2, 0).getPos();
        float yaw = match.getForwards().getAxis() == Direction.Axis.X ? 0.0F : 90.0F;
        wither.snapTo(spawn.getX() + 0.5, spawn.getY() + 0.55, spawn.getZ() + 0.5, yaw, 0.0F);
        wither.yBodyRot = yaw;
        wither.makeInvulnerable();
        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, wither.getBoundingBox().inflate(50.0))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, wither);
        }
        server.addFreshEntity(wither);
        CarvedPumpkinBlock.updatePatternBlocks(level, match);
        return true;
    }
}
