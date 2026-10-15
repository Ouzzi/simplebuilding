package com.simplesandwiches.registry;

import com.simplebuilding.framework.api.TransformHints;
import com.simplesandwiches.Sandwiches;
import com.simplesandwiches.block.CuttingBoardBlock;
import com.simplesandwiches.block.CuttingBoardBlockEntity;
import com.simplesandwiches.block.MilkCauldronBlock;
import com.simplesandwiches.mixin.CauldronDispatcherAccessor;
import com.simplesandwiches.sandwich.SandwichContents;
import com.simplesandwiches.sandwich.SandwichFormula;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Loader-neutral registration order: components, blocks, items (each loader calls these at its
 * registration time), the creative tab, then {@link #setup()} for the cauldron interaction and the
 * framework hand hint.
 */
public final class SandwichRegistry {
    public static final String HINT_ID = "simplesandwiches:knife";

    public static void tab() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Sandwiches.id("kitchen"),
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemgroup.simplesandwiches.kitchen"))
                        .icon(() -> SandwichFormula.create(sample(Items.COOKED_BEEF, ModItems.CHEESE_SLICE)))
                        .displayItems((params, out) -> tabItems().forEach(out::accept))
                        .build());
    }

    /** Creative-tab content: empty while the super-mod switch is off. */
    public static List<ItemStack> tabItems() {
        return Sandwiches.on() ? tabStacks() : List.of();
    }

    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (com.simplesandwiches.guide.SandwichGuide.book() != null) out.add(com.simplesandwiches.guide.SandwichGuide.stack());
        out.add(new ItemStack(ModItems.KNIFE));
        ModItems.CUTTING_BOARDS.values().forEach(i -> out.add(new ItemStack(i)));
        out.add(new ItemStack(ModItems.CHEESE_BLOCK));
        out.add(new ItemStack(ModItems.CHEESE_SLICE));
        out.add(new ItemStack(ModItems.BUTTER_BLOCK));
        out.add(new ItemStack(ModItems.BUTTER_SLICE));
        out.add(new ItemStack(ModItems.CAKE_SLICE));
        out.add(SandwichFormula.create(new SandwichContents(List.of(), true)));
        out.add(SandwichFormula.create(sample(ModItems.CHEESE_SLICE)));
        out.add(SandwichFormula.create(sample(Items.COOKED_BEEF, ModItems.CHEESE_SLICE)));
        out.add(SandwichFormula.create(sample(Items.COOKED_CHICKEN, Items.CARROT, ModItems.CHEESE_SLICE)));
        return out;
    }

    /**
     * The same items sorted into the fitting Vanilla tabs (shared switch {@code CreativeTabSettings}: the loaders add them only
     * while the switch is on and the mod is on). Guide book and sample sandwiches stay in the own tab.
     */
    public static final List<net.minecraft.resources.ResourceKey<CreativeModeTab>> VANILLA_TABS = List.of(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS,
            net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES, net.minecraft.world.item.CreativeModeTabs.FOOD_AND_DRINKS);

    public static java.util.Map<net.minecraft.resources.ResourceKey<CreativeModeTab>, List<ItemStack>> vanillaTabStacks() {
        java.util.Map<net.minecraft.resources.ResourceKey<CreativeModeTab>, List<ItemStack>> out = new java.util.LinkedHashMap<>();
        List<ItemStack> boards = new ArrayList<>();
        ModItems.CUTTING_BOARDS.values().forEach(i -> boards.add(new ItemStack(i)));
        out.put(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS, boards);
        out.put(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES, List.of(new ItemStack(ModItems.KNIFE)));
        out.put(net.minecraft.world.item.CreativeModeTabs.FOOD_AND_DRINKS, List.of(new ItemStack(ModItems.CHEESE_BLOCK),
                new ItemStack(ModItems.CHEESE_SLICE), new ItemStack(ModItems.BUTTER_BLOCK), new ItemStack(ModItems.BUTTER_SLICE),
                new ItemStack(ModItems.CAKE_SLICE)));
        return out;
    }

    private static SandwichContents sample(Item... items) {
        List<Holder<Item>> list = new ArrayList<>();
        for (Item i : items) list.add(i.builtInRegistryHolder());
        return new SandwichContents(list, false);
    }

    public static void setup() {
        ((CauldronDispatcherAccessor) (Object) CauldronInteractions.EMPTY).simplesandwiches$put(Items.MILK_BUCKET, MilkCauldronBlock::fillWithMilk);
        // Other mods' milk containers (item tag): tag entry of the dispatcher.
        ((CauldronDispatcherAccessor) (Object) CauldronInteractions.EMPTY).simplesandwiches$put(MilkCauldronBlock.MILK_BUCKETS, MilkCauldronBlock::fillWithMilk);
        TransformHints.register(HINT_ID, query -> query.mainHand() && query.level() instanceof Level level
                && query.player() instanceof Player player && query.hit() instanceof BlockHitResult hit
                && level.getBlockEntity(hit.getBlockPos()) instanceof CuttingBoardBlockEntity board
                && hints(CuttingBoardBlock.plan(board, player.getMainHandItem(), player.getOffhandItem())));
    }

    /** Hand hint for every board action that changes the board (not for a refused or empty click). */
    private static boolean hints(CuttingBoardBlock.Action action) {
        return action != CuttingBoardBlock.Action.NONE && action != CuttingBoardBlock.Action.FULL;
    }

    private SandwichRegistry() {}
}
