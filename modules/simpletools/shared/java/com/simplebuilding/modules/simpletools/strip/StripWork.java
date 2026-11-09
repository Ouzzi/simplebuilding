package com.simplebuilding.modules.simpletools.strip;

import com.simplelib.api.AxeStrips;
import java.lang.reflect.Proxy;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * What an axe does to one block, asked without a placed block (the strip station holds items, not blocks). 26.3 keeps the
 * axe rules in the data-driven block transformer of the axe item (strip, scrape, wax off, in Vanilla's order), so this reads
 * that very list: each rule's state provider is asked about a one-block view that shows the block item's default state.
 * Rules of mods that bypass the transformer come from {@link AxeStrips}.
 */
public final class StripWork {
    /** The item one block turns into, with the sound and level event ({@link BlockTransformer.TransformParticle}) of its rule. */
    public record Step(ItemStack result, SoundEvent sound, BlockTransformer.TransformParticle particle) {}

    private StripWork() {}

    public static boolean isAxe(ItemStack axe) {
        return !axe.isEmpty() && axe.is(ItemTags.AXES) && axe.has(DataComponents.BLOCK_TRANSFORMER);
    }

    /** The step the axe takes on one item of {@code input}, or null when the axe does nothing to it. */
    public static @Nullable Step step(ItemStack axe, ItemStack input) {
        if (!isAxe(axe) || !(input.getItem() instanceof BlockItem blockItem)) return null;
        BlockState state = blockItem.getBlock().defaultBlockState();
        Holder<BlockTransformer> transformer = axe.get(DataComponents.BLOCK_TRANSFORMER);
        if (transformer != null) {
            LevelAccessor view = view(state);
            for (BlockTransformer.BlockTransformData data : transformer.value().transforms()) {
                BlockState out;
                try {
                    out = data.blockStateProvider().value().getOptionalState(view, RandomSource.create(), BlockPos.ZERO);
                } catch (RuntimeException e) {
                    continue; // a rule that needs more of the world than one block never applies to an item
                }
                Step step = out == null ? null : stepTo(input, out, data.sound().value(), data.particle());
                if (step != null) return step;
            }
        }
        BlockState extra = AxeStrips.stripped(state);
        return extra == null ? null : stepTo(input, extra, SoundEvents.AXE_STRIP.value(), BlockTransformer.TransformParticle.NONE);
    }

    private static @Nullable Step stepTo(ItemStack input, BlockState out, SoundEvent sound, BlockTransformer.TransformParticle particle) {
        if (out.is(Blocks.AIR) || out.getBlock().asItem() == net.minecraft.world.item.Items.AIR) return null;
        ItemStack result = new ItemStack(out.getBlock().asItem());
        return result.getItem() == input.getItem() ? null : new Step(result, sound, particle);
    }

    /** A level of one block: the given state at {@link BlockPos#ZERO}, air everywhere else; no other question is answered. */
    private static LevelAccessor view(BlockState state) {
        return (LevelAccessor) Proxy.newProxyInstance(LevelAccessor.class.getClassLoader(), new Class<?>[] {LevelAccessor.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getBlockState":
                    return BlockPos.ZERO.equals(args[0]) ? state : Blocks.AIR.defaultBlockState();
                case "isStateAtPosition":
                    @SuppressWarnings("unchecked") Predicate<BlockState> test = (Predicate<BlockState>) args[1];
                    return test.test(BlockPos.ZERO.equals(args[0]) ? state : Blocks.AIR.defaultBlockState());
                case "getFluidState":
                    return (BlockPos.ZERO.equals(args[0]) ? state : Blocks.AIR.defaultBlockState()).getFluidState();
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                case "toString":
                    return "StripWork.view";
                default:
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return false;
                    if (type == int.class) return 0;
                    if (type == long.class) return 0L;
                    if (type == float.class) return 0F;
                    if (type == double.class) return 0D;
                    if (type == byte.class) return (byte) 0;
                    if (type == short.class) return (short) 0;
                    if (type == char.class) return (char) 0;
                    return null;
            }
        });
    }
}
