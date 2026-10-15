package com.simplebuilding.modules.simpleend;

import com.simplebuilding.modules.simpleend.block.EndPlantBlock;
import com.simplebuilding.modules.simpleend.mixin.FireBlockInvoker;
import com.simplebuilding.modules.simpleend.world.EndFlora;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.dispenser.BoatDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/** Loader-neutral registration order: blocks, items, entity types, feature types, sounds, creative tab, then {@link #common()}. */
public final class EndRegistry {
    public static final TagKey<Block> PLANTS_MAY_PLACE_ON = TagKey.create(Registries.BLOCK, SimpleEnd.id("end_plants_may_place_on"));
    public static final TagKey<Block> BUD_MAY_PLACE_ON = TagKey.create(Registries.BLOCK, SimpleEnd.id("bud_may_place_on"));
    /** Items that float back up from the void and stay twice as long as dropped items. */
    public static final TagKey<Item> VOID_SAFE = TagKey.create(Registries.ITEM, SimpleEnd.id("void_safe"));
    public static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, SimpleEnd.id("main"));

    public static Block END_GRASS, END_BUSH, BURROWED_END_STONE;
    public static net.minecraft.world.entity.EntityType<com.simplebuilding.modules.simpleend.entity.Nibblemite> NIBBLEMITE;
    public static Item NIBBLEMITE_EGG, BURROWED_END_STONE_ITEM;
    public static Item END_GRASS_ITEM, END_BUSH_ITEM, DRIFT_FRUIT, SPOILED_DRIFT_FRUIT;
    public static final ResourceKey<net.minecraft.world.effect.MobEffect> DRIFTING_KEY = ResourceKey.create(Registries.MOB_EFFECT, SimpleEnd.id("drifting"));
    public static net.minecraft.world.effect.MobEffect DRIFTING;
    private static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> driftingHolder;
    public static SoundEvent MUSIC_ASH_VALLEY, MUSIC_DAWNLANDS;
    /** Log -> stripped log (axe), filled in {@link #common()}. */
    public static final Map<Block, Block> STRIPPABLES = new HashMap<>();

    private EndRegistry() {}

    private static Block block(String id, java.util.function.Function<Properties, Block> f, Properties p) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleEnd.id(id));
        return Registry.register(BuiltInRegistries.BLOCK, key, f.apply(p.setId(key)));
    }

    private static Item item(String id, java.util.function.Function<Item.Properties, Item> f, Item.Properties p) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleEnd.id(id));
        return Registry.register(BuiltInRegistries.ITEM, key, f.apply(p.setId(key)));
    }

    public static void blocks() {
        Properties plant = Properties.of().mapColor(MapColor.SAND).replaceable().noCollision().instabreak().sound(SoundType.GRASS)
                .ignitedByLava().offsetType(net.minecraft.world.level.block.state.BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.POPPED);
        END_GRASS = block("end_grass", p -> new EndPlantBlock(false, p), plant);
        END_BUSH = block("end_bush", p -> new EndPlantBlock(true, p), Properties.of().mapColor(MapColor.WOOD).replaceable().noCollision()
                .instabreak().sound(SoundType.GRASS).ignitedByLava().pushReaction(PushReaction.POPPED));
        BURROWED_END_STONE = block("burrowed_end_stone", com.simplebuilding.modules.simpleend.block.BurrowedEndStoneBlock::new,
                Properties.of().mapColor(MapColor.SAND).instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM)
                        .requiresCorrectToolForDrops().strength(3.0F, 9.0F).randomTicks());
        EndWood.ALL.forEach(EndWood::blocks);
    }

    public static net.minecraft.world.entity.EntityType<com.simplebuilding.modules.simpleend.entity.Nibblemite> nibblemite() {
        if (NIBBLEMITE == null) {
            NIBBLEMITE = net.minecraft.world.entity.EntityType.Builder.<com.simplebuilding.modules.simpleend.entity.Nibblemite>of(
                            com.simplebuilding.modules.simpleend.entity.Nibblemite::new, net.minecraft.world.entity.MobCategory.CREATURE)
                    .sized(0.32F, 0.24F).eyeHeight(0.1F).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, SimpleEnd.id("nibblemite")));
        }
        return NIBBLEMITE;
    }

    public static void items() {
        END_GRASS_ITEM = item("end_grass", p -> new BlockItem(END_GRASS, p), new Item.Properties().useBlockDescriptionPrefix()
                .compostable(ContextIntProviders.COMPOSTABLE_LOW).cookingFuel(ContextIntProviders.COOKING_TIME_DRY_PLANTS));
        END_BUSH_ITEM = item("end_bush", p -> new BlockItem(END_BUSH, p), new Item.Properties().useBlockDescriptionPrefix()
                .cookingFuel(ContextIntProviders.COOKING_TIME_DRY_PLANTS));
        EndWood.ALL.forEach(EndWood::items);
        BURROWED_END_STONE_ITEM = item("burrowed_end_stone", p -> new BlockItem(BURROWED_END_STONE, p), new Item.Properties().useBlockDescriptionPrefix());
        NIBBLEMITE_EGG = item("nibblemite_spawn_egg", p -> new net.minecraft.world.item.SpawnEggItem(p), new Item.Properties().spawnEgg(nibblemite()));
        com.simplebuilding.modules.simpleend.guide.EndGuide.register();
        // Schwebfrucht: 4 s Slow Falling plus a small hop (DriftFruitItem), always edible like chorus fruit.
        DRIFT_FRUIT = item("drift_fruit", DriftFruitItem::new, new Item.Properties().food(
                new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).alwaysEdible().build(),
                Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(
                        new MobEffectInstance(MobEffects.SLOW_FALLING, DriftFruitItem.SLOW_FALL_TICKS, 0))).build())
                .compostable(ContextIntProviders.COMPOSTABLE_MEDIUM));
        spoiledFruit();
    }

    /** Spoiled Drift Fruit, the fruit of End trees outside the End: 1 hunger point, then a short drift (code). */
    private static void spoiledFruit() {
        SPOILED_DRIFT_FRUIT = item("spoiled_drift_fruit", SpoiledDriftFruitItem::new, new Item.Properties().food(
                new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).alwaysEdible().build())
                .compostable(ContextIntProviders.COMPOSTABLE_LOW));
    }

    public static void effects() {
        DRIFTING = new DriftingEffect();
        driftingHolder = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, DRIFTING_KEY, DRIFTING);
    }

    /** The registered Drifting effect (null before the effect registry was filled). */
    public static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> drifting() {
        if (driftingHolder == null && DRIFTING != null) driftingHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(DRIFTING);
        return driftingHolder;
    }

    public static void entities() {
        EndWood.ALL.forEach(EndWood::entities);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, SimpleEnd.id("nibblemite"), nibblemite());
    }

    public static void featureTypes() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE, SimpleEnd.id("end_flora"), EndFlora.CODEC);
    }

    public static void sounds() {
        MUSIC_ASH_VALLEY = sound("music.ash_valley");
        MUSIC_DAWNLANDS = sound("music.dawnlands");
    }

    private static SoundEvent sound(String path) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, SimpleEnd.id(path), SoundEvent.createVariableRangeEvent(SimpleEnd.id(path)));
    }

    public static void tab() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB, CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemgroup.simpleend.main"))
                .icon(() -> new ItemStack(EndWood.ASHWOOD.bud))
                .displayItems((params, out) -> tabStacks().forEach(out::accept))
                .build());
    }

    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (EndWood w : EndWood.ALL) w.tabItems().forEach(i -> out.add(new ItemStack(i)));
        out.add(new ItemStack(END_GRASS_ITEM));
        out.add(new ItemStack(END_BUSH_ITEM));
        out.add(new ItemStack(DRIFT_FRUIT));
        out.add(new ItemStack(SPOILED_DRIFT_FRUIT));
        out.add(new ItemStack(BURROWED_END_STONE_ITEM));
        out.add(new ItemStack(NIBBLEMITE_EGG));
        if (com.simplebuilding.modules.simpleend.guide.EndGuide.book() != null) out.add(com.simplebuilding.modules.simpleend.guide.EndGuide.stack());
        return out;
    }

    /**
     * The same items sorted into the fitting Vanilla tabs (shared switch {@code CreativeTabSettings}: the loaders add them only
     * while the switch is on). The guide book stays in the own tab.
     */
    public static final List<ResourceKey<CreativeModeTab>> VANILLA_TABS = List.of(net.minecraft.world.item.CreativeModeTabs.NATURAL_BLOCKS, net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS, net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS,
            net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES, net.minecraft.world.item.CreativeModeTabs.INGREDIENTS, net.minecraft.world.item.CreativeModeTabs.FOOD_AND_DRINKS, net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS);

    public static Map<ResourceKey<CreativeModeTab>, List<ItemStack>> vanillaTabStacks() {
        Map<ResourceKey<CreativeModeTab>, List<ItemStack>> out = new java.util.LinkedHashMap<>();
        for (EndWood w : EndWood.ALL) {
            for (Item i : w.tabItems()) {
                ResourceKey<CreativeModeTab> tab;
                String path = BuiltInRegistries.ITEM.getKey(i).getPath();
                if (path.endsWith("_boat") || path.endsWith("_chest_boat")) tab = net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES;
                else if (path.endsWith("_sticks")) tab = net.minecraft.world.item.CreativeModeTabs.INGREDIENTS;
                else if (path.endsWith("_sign") || path.endsWith("_hanging_sign") || path.endsWith("_shelf")) tab = net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS;
                else if (path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_leaves") || i == w.itemIds.get(w.budName)) tab = net.minecraft.world.item.CreativeModeTabs.NATURAL_BLOCKS;
                else tab = net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS;
                out.computeIfAbsent(tab, k -> new ArrayList<>()).add(new ItemStack(i));
            }
        }
        out.computeIfAbsent(net.minecraft.world.item.CreativeModeTabs.NATURAL_BLOCKS, k -> new ArrayList<>()).addAll(List.of(
                new ItemStack(END_GRASS_ITEM), new ItemStack(END_BUSH_ITEM), new ItemStack(BURROWED_END_STONE_ITEM)));
        out.computeIfAbsent(net.minecraft.world.item.CreativeModeTabs.FOOD_AND_DRINKS, k -> new ArrayList<>()).addAll(List.of(
                new ItemStack(DRIFT_FRUIT), new ItemStack(SPOILED_DRIFT_FRUIT)));
        out.computeIfAbsent(net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS, k -> new ArrayList<>()).add(new ItemStack(NIBBLEMITE_EGG));
        return out;
    }

    /** After all registrations: stripping, flammability (like Vanilla overworld wood), boat dispensing. */
    public static void common() {
        FireBlock fire = (FireBlock) Blocks.FIRE;
        FireBlockInvoker f = (FireBlockInvoker) fire;
        for (EndWood w : EndWood.ALL) {
            STRIPPABLES.put(w.log, w.strippedLog);
            STRIPPABLES.put(w.wood, w.strippedWood);
            for (Block b : new Block[] {w.planks, w.slab, w.fenceGate, w.fence, w.stairs}) f.simpleend$setFlammable(b, 5, 20);
            for (Block b : new Block[] {w.log, w.strippedLog, w.wood, w.strippedWood}) f.simpleend$setFlammable(b, 5, 5);
            f.simpleend$setFlammable(w.shelf, 30, 20);
            f.simpleend$setFlammable(w.leaves, 30, 60);
            f.simpleend$setFlammable(w.sticks, 5, 20);
            DispenserBlock.registerBehavior(w.boat, new BoatDispenseItemBehavior(w.boatType()));
            DispenserBlock.registerBehavior(w.chestBoat, new BoatDispenseItemBehavior(w.chestBoatType()));
        }
        f.simpleend$setFlammable(END_GRASS, 60, 100);
        f.simpleend$setFlammable(END_BUSH, 60, 100);
    }

    /** Stripped form of a Simple End log/wood, keeping the axis; null for everything else. */
    public static BlockState stripped(BlockState state) {
        Block to = STRIPPABLES.get(state.getBlock());
        if (to == null) return null;
        return to.defaultBlockState().setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
    }
}
