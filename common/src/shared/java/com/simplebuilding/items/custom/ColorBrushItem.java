package com.simplebuilding.items.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import com.simplebuilding.util.ModTags;
import java.util.Optional;

/**
 * Colour brush (owner 2026-10-09): a reinforced vanilla brush. Round 3: it is a {@link BrushItem} - without a dye it
 * brushes like the vanilla one (suspicious sand and gravel, dust on other blocks). Conflict rule: on a brushable block
 * it always brushes, even with dyes in the inventory; elsewhere a stroke paints when there is ink, the block is
 * dyeable and gets another colour; otherwise it brushes like the vanilla brush. Round 2: every stroke takes the next dye from the inventory the way a bow finds its
 * arrows - the other hand first, then the hand holding the brush, then the inventory in vanilla slot order - and
 * recolours a dyeable block in place: wool, carpet, concrete (powder), terracotta, glazed terracotta, glass and panes,
 * candles, beds, banners, shulker boxes and blocks in {@code simplebuilding:dyeable_families}, keeping state and
 * contents. A {@link PaintBoxItem} with dyes counts as ink too: it gives a random one of its colours per stroke.
 * No loading, no pipette; creative strokes cost nothing. No wood stain: wood families stay out by design.
 */
public final class ColorBrushItem extends net.minecraft.world.item.BrushItem {
    public static final int MAX_DURABILITY = 256;
    /** Model keys of {@link #inkKey}: no ink, or a paint box (rainbow tip; otherwise the dye colour's name). */
    public static final String INK_NONE = "none", INK_PALETTE = "palette";
    /** The client player for the tooltip; set by the client model property, stays null on servers. */
    public static java.util.function.Supplier<Player> clientPlayer = () -> null;
    private static final DyeColor[] COLORS = {
            DyeColor.WHITE, DyeColor.ORANGE, DyeColor.MAGENTA, DyeColor.LIGHT_BLUE,
            DyeColor.YELLOW, DyeColor.LIME, DyeColor.PINK, DyeColor.GRAY,
            DyeColor.LIGHT_GRAY, DyeColor.CYAN, DyeColor.PURPLE, DyeColor.BLUE,
            DyeColor.BROWN, DyeColor.GREEN, DyeColor.RED, DyeColor.BLACK
    };
    public ColorBrushItem(Properties properties) {
        super(properties.durability(MAX_DURABILITY));
    }

    /** The colour of a vanilla dye item, or null. */
    public static DyeColor dyeColor(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null && id.getNamespace().equals("minecraft")) {
            String path = id.getPath();
            for (DyeColor color : COLORS) {
                if (path.equals(color.getName() + "_dye")) return color;
            }
        }
        return null;
    }

    /** Ink: a dye, or a colour palette holding at least one dye. */
    public static boolean isInk(ItemStack stack) {
        return dyeColor(stack) != null || PaintBoxItem.hasDyes(stack);
    }

    /**
     * The stack the next stroke draws from, found like {@code Player#getProjectile}: hands first (other hand before the
     * main hand, {@link ProjectileWeaponItem#getHeldProjectile}), then the inventory in slot order. Empty if none.
     */
    public static ItemStack nextInk(Player player) {
        ItemStack held = ProjectileWeaponItem.getHeldProjectile(player, ColorBrushItem::isInk);
        if (!held.isEmpty()) return held;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (isInk(candidate)) return candidate;
        }
        return ItemStack.EMPTY;
    }

    /** {@link #INK_NONE}, {@link #INK_PALETTE} or the next dye's colour name; drives the bristle tip of the model. */
    public static String inkKey(Player player) {
        if (player == null) return INK_NONE;
        ItemStack ink = nextInk(player);
        if (ink.isEmpty()) return INK_NONE;
        DyeColor color = dyeColor(ink);
        return color == null ? INK_PALETTE : color.getName();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<net.minecraft.network.chat.Component> lines,
                                net.minecraft.world.item.TooltipFlag flag) {
        Player player = clientPlayer.get();
        if (player != null) {
            ItemStack ink = nextInk(player);
            net.minecraft.network.chat.Component next = ink.isEmpty()
                    ? net.minecraft.network.chat.Component.translatable("tooltip.simplebuilding.color_brush.no_ink")
                    : net.minecraft.network.chat.Component.translatable("tooltip.simplebuilding.color_brush.next", ink.getHoverName());
            lines.accept(next.copy().withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplebuilding.color_brush")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        super.appendHoverText(stack, context, display, lines, flag);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (!isBrushable(state)) {
            InteractionResult painted = stroke(context, context.getLevel().getRandom());
            if (painted != InteractionResult.PASS) return painted;
        }
        return super.useOn(context); // vanilla brushing
    }

    /** Suspicious sand, suspicious gravel and every other {@link net.minecraft.world.level.block.BrushableBlock}. */
    public static boolean isBrushable(BlockState state) {
        return state.getBlock() instanceof net.minecraft.world.level.block.BrushableBlock;
    }

    /** One paint stroke, PASS when nothing is painted; {@code random} picks a box colour (tests pass a seeded one). */
    public static InteractionResult stroke(UseOnContext context, RandomSource random) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack brush = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (recolored(state, otherColor(state)) == null) return InteractionResult.PASS;
        ItemStack ink = nextInk(player);
        if (ink.isEmpty()) return InteractionResult.PASS;
        DyeColor blockColor = dyeColorForBlock(state);
        DyeColor color = dyeColor(ink);
        boolean fromBox = color == null;
        if (fromBox) {
            color = PaintBoxItem.pickColor(ink, blockColor, random);
            if (color == null) return InteractionResult.PASS;
        }
        BlockState replacement = recolored(state, color);
        if (replacement == null || replacement.equals(state)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (player.isSpectator() || !player.mayBuild() || !level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, context.getClickedFace(), brush)
                    || !com.simplebuilding.api.WorldPermissions.mayChange(level, player, pos)) {
                return InteractionResult.PASS;
            }
            paint(level, pos, state, replacement);
            if (!player.getAbilities().instabuild) {
                if (fromBox) PaintBoxItem.removeOne(ink, color);
                else ink.shrink(1);
                brush.hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
            }
            level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 0.7f, 1.0f);
            ((net.minecraft.server.level.ServerLevel) level).sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, replacement), pos.getX() + .5, pos.getY() + .5,
                    pos.getZ() + .5, 6, .25, .25, .25, .02);
        }
        return InteractionResult.SUCCESS;
    }

    /** Some colour other than the block's own, to ask whether the block belongs to a brush family at all. */
    private static DyeColor otherColor(BlockState state) {
        return dyeColorForBlock(state) == DyeColor.WHITE ? DyeColor.BLACK : DyeColor.WHITE;
    }

    /**
     * Swaps the block and keeps what it holds: the block entity data (shulker contents, banner patterns, names) moves
     * over, and the other half of a bed is recoloured with it so the bed does not break apart.
     */
    private static void paint(Level level, BlockPos pos, BlockState state, BlockState replacement) {
        if (state.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
            BlockPos other = pos.relative(net.minecraft.world.level.block.BedBlock.getConnectedDirection(state));
            BlockState otherState = level.getBlockState(other);
            BlockState otherReplacement = otherState.is(state.getBlock()) ? recolored(otherState, colorOf(replacement)) : null;
            if (otherReplacement != null) {
                level.setBlock(other, otherReplacement, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
        net.minecraft.world.level.block.entity.BlockEntity old = level.getBlockEntity(pos);
        net.minecraft.nbt.CompoundTag data = old == null ? null : old.saveCustomOnly(level.registryAccess());
        if (old instanceof net.minecraft.world.Container container) container.clearContent(); // no drop on removal
        level.setBlockAndUpdate(pos, replacement);
        net.minecraft.world.level.block.entity.BlockEntity fresh = level.getBlockEntity(pos);
        if (data != null && fresh != null) {
            fresh.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(
                    net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), data));
            fresh.setChanged();
            level.sendBlockUpdated(pos, replacement, replacement, Block.UPDATE_CLIENTS);
        }
    }

    private static DyeColor colorOf(BlockState state) {
        return dyeColorForBlock(state);
    }

    private static BlockState recolored(BlockState state, DyeColor color) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (!isDyeablePath(state, path)) return null;
        String target = targetPath(path, color);
        if (target == null) return null;
        Identifier sourceId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        Block block = BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath(sourceId.getNamespace(), target)).orElse(null);
        if (block == null || block == state.getBlock()) return null;
        BlockState result = block.defaultBlockState();
        for (Property<?> property : state.getProperties()) {
            result = copyProperty(state, result, property);
        }
        return result;
    }

    private static BlockState copyProperty(BlockState source, BlockState target, Property<?> property) {
        Property<?> targetProperty = target.getBlock().getStateDefinition().getProperty(property.getName());
        if (targetProperty == null) return target;
        return copyPropertyValue(source, target, property, targetProperty);
    }

    private static <T extends Comparable<T>> BlockState copyPropertyValue(BlockState source, BlockState target,
                                                                           Property<T> sourceProperty, Property<?> rawTarget) {
        @SuppressWarnings("unchecked") Property<T> targetProperty = (Property<T>) rawTarget;
        Optional<T> value = sourceProperty.getValue(source.getValue(sourceProperty).toString());
        return value.isPresent() ? target.setValue(targetProperty, value.get()) : target;
    }

    private static String targetPath(String path, DyeColor color) {
        String target = color.getName();
        if (path.equals("glass") || path.equals("glass_pane")) return target + "_stained_" + path;
        for (DyeColor source : COLORS) {
            String prefix = source.getName() + "_";
            if (path.startsWith(prefix)) return target + "_" + path.substring(prefix.length());
        }
        return target + "_" + path;
    }

    private static boolean isDyeablePath(BlockState state, String path) {
        if (state.is(ModTags.Blocks.DYEABLE_FAMILIES)) return true;
        for (DyeColor color : COLORS) {
            if (path.startsWith(color.getName() + "_")) {
                return isDyeableSuffix(path.substring(color.getName().length() + 1));
            }
        }
        return isDyeableSuffix(path);
    }

    private static boolean isDyeableSuffix(String path) {
        return switch (path) {
            case "wool", "carpet", "concrete", "concrete_powder", "terracotta", "stained_glass",
                    "glass", "stained_glass_pane", "glass_pane", "candle", "bed", "banner",
                    "shulker_box", "glazed_terracotta", "wall_banner", "wool_stairs", "wool_slab",
                    "concrete_stairs", "concrete_slab", "candle_cake" -> true;
            default -> false;
        };
    }

    static DyeColor dyeColorForBlock(BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        for (DyeColor color : COLORS) {
            if (path.startsWith(color.getName() + "_")) return color;
        }
        return null;
    }

}
