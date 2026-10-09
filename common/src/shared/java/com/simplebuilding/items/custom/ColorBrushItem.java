package com.simplebuilding.items.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import com.simplebuilding.util.ModTags;
import java.util.Optional;

/**
 * Colour brush (concept A, 2026-10-09): loads a dye colour (8 strokes per dye, up to 256) and recolours dyeable blocks
 * in place - wool, carpet, concrete (powder), terracotta, glazed terracotta, glass and panes, candles, beds, banners,
 * shulker boxes and blocks in {@code simplebuilding:dyeable_families} - keeping state and contents. No wood stain:
 * wood families stay out by design. Holding use repeats the stroke like any block use, so sweeping paints.
 */
public final class ColorBrushItem extends Item {
    public static final int MAX_CHARGES = 256;
    public static final int STROKES_PER_DYE = 8;
    private static final String COLOR = "Color";
    private static final String CHARGES = "Charges";
    private static final DyeColor[] COLORS = {
            DyeColor.WHITE, DyeColor.ORANGE, DyeColor.MAGENTA, DyeColor.LIGHT_BLUE,
            DyeColor.YELLOW, DyeColor.LIME, DyeColor.PINK, DyeColor.GRAY,
            DyeColor.LIGHT_GRAY, DyeColor.CYAN, DyeColor.PURPLE, DyeColor.BLUE,
            DyeColor.BROWN, DyeColor.GREEN, DyeColor.RED, DyeColor.BLACK
    };
    public ColorBrushItem(Properties properties) {
        super(properties.durability(MAX_CHARGES));
    }

    private static int storedCharges(ItemStack stack) {
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                CustomData.EMPTY).copyTag().getIntOr(CHARGES, 0);
    }

    public static int charges(ItemStack stack) {
        return storedCharges(stack);
    }

    private static DyeColor storedColor(ItemStack stack) {
        int value = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                CustomData.EMPTY).copyTag().getIntOr(COLOR, -1);
        return value < 0 || value >= COLORS.length ? null : COLORS[value];
    }

    public static DyeColor color(ItemStack stack) {
        return storedColor(stack);
    }

    private static void store(ItemStack stack, DyeColor color, int charges) {
        CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, tag -> {
            if (color == null || charges <= 0) {
                tag.remove(COLOR);
                tag.remove(CHARGES);
            } else {
                tag.putInt(COLOR, colorIndex(color));
                tag.putInt(CHARGES, Math.min(MAX_CHARGES, charges));
            }
        });
    }

    private static int colorIndex(DyeColor color) {
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i] == color) return i;
        }
        return 0;
    }

    private static DyeColor dyeColor(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null && id.getNamespace().equals("minecraft")) {
            String path = id.getPath();
            for (DyeColor color : COLORS) {
                if (path.equals(color.getName() + "_dye")) return color;
            }
        }
        return null;
    }

    private static ItemStack findDye(Player player, InteractionHand hand) {
        ItemStack otherHand = player.getItemInHand(hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (dyeColor(otherHand) != null) return otherHand;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (dyeColor(candidate) != null && !candidate.isEmpty()) return candidate;
        }
        return ItemStack.EMPTY;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<net.minecraft.network.chat.Component> lines,
                                net.minecraft.world.item.TooltipFlag flag) {
        DyeColor color = storedColor(stack);
        if (color != null) {
            lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplebuilding.color_brush.loaded",
                    net.minecraft.network.chat.Component.translatable("color.minecraft." + color.getName()), storedCharges(stack))
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        lines.accept(net.minecraft.network.chat.Component.translatable("tooltip.simplebuilding.color_brush")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        super.appendHoverText(stack, context, display, lines, flag);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack brush = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        ItemStack dye = findDye(player, hand);
        if (dye.isEmpty()) return InteractionResult.PASS;
        DyeColor color = dyeColor(dye);
        if (!level.isClientSide()) {
            int existing = storedColor(brush) == color ? storedCharges(brush) : 0;
            if (existing >= MAX_CHARGES) return InteractionResult.PASS;
            int dyes = 1;
            store(brush, color, (storedColor(brush) == color ? storedCharges(brush) : 0)
                    + dyes * STROKES_PER_DYE);
            if (!player.getAbilities().instabuild) dye.shrink(dyes);
            level.playSound(null, player.blockPosition(), SoundEvents.DYE_USE, SoundSource.PLAYERS, 0.8f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack brush = context.getItemInHand();
        ItemStack other = player.getItemInHand(context.getHand() == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (player.isShiftKeyDown() && dyeColor(other) != null) {
            if (!level.isClientSide()) {
                DyeColor color = dyeColor(other);
                int existing = storedColor(brush) == color ? storedCharges(brush) : 0;
                if (existing < MAX_CHARGES) {
                    store(brush, color, existing + STROKES_PER_DYE);
                    if (!player.getAbilities().instabuild) other.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && (other.is(Items.SPONGE) || isWaterBottle(other))) {
            if (!level.isClientSide()) store(brush, null, 0);
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        DyeColor current = storedColor(brush);
        if (player.isShiftKeyDown() && current == null) {
            // Pipette: takes the block's colour; survival pays it with one matching dye from the inventory.
            DyeColor picked = isDyeablePath(state, BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath())
                    ? dyeColorForBlock(state) : null;
            if (picked == null) return InteractionResult.PASS;
            ItemStack dye = player.getAbilities().instabuild ? ItemStack.EMPTY : findDye(player, picked);
            if (!player.getAbilities().instabuild && dye.isEmpty()) return InteractionResult.PASS;
            if (!level.isClientSide()) {
                store(brush, picked, player.getAbilities().instabuild ? MAX_CHARGES : STROKES_PER_DYE);
                dye.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        if (current == null || storedCharges(brush) <= 0) return InteractionResult.PASS;
        BlockState replacement = recolored(state, current);
        if (replacement == null || replacement.equals(state)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (player.isSpectator() || !player.mayBuild() || !level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, context.getClickedFace(), brush)
                    || !com.simplebuilding.api.WorldPermissions.mayChange(level, player, pos)) {
                return InteractionResult.PASS;
            }
            paint(level, pos, state, replacement);
            if (!player.getAbilities().instabuild) {
                store(brush, current, storedCharges(brush) - 1);
                brush.hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
            }
            level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 0.7f, 1.0f);
            ((net.minecraft.server.level.ServerLevel) level).sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, replacement), pos.getX() + .5, pos.getY() + .5,
                    pos.getZ() + .5, 6, .25, .25, .25, .02);
        }
        return InteractionResult.SUCCESS;
    }

    /** A matching dye of {@code color} from the inventory, or empty. */
    private static ItemStack findDye(Player player, DyeColor color) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (dyeColor(candidate) == color) return candidate;
        }
        return ItemStack.EMPTY;
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

    private static DyeColor dyeColorForBlock(BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        for (DyeColor color : COLORS) {
            if (path.startsWith(color.getName() + "_")) return color;
        }
        return null;
    }

    private static boolean isWaterBottle(ItemStack stack) {
        return stack.is(Items.POTION)
                && stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                        PotionContents.EMPTY).is(Potions.WATER);
    }
}
