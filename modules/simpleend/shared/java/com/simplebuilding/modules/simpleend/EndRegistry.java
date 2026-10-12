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

    public static Block END_GRASS, END_BUSH;
    public static Item END_GRASS_ITEM, END_BUSH_ITEM, DRIFT_FRUIT;
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
        EndWood.ALL.forEach(EndWood::blocks);
    }

    public static void items() {
        END_GRASS_ITEM = item("end_grass", p -> new BlockItem(END_GRASS, p), new Item.Properties().useBlockDescriptionPrefix()
                .compostable(ContextIntProviders.COMPOSTABLE_LOW).cookingFuel(ContextIntProviders.COOKING_TIME_DRY_PLANTS));
        END_BUSH_ITEM = item("end_bush", p -> new BlockItem(END_BUSH, p), new Item.Properties().useBlockDescriptionPrefix()
                .cookingFuel(ContextIntProviders.COOKING_TIME_DRY_PLANTS));
        EndWood.ALL.forEach(EndWood::items);
        com.simplebuilding.modules.simpleend.guide.EndGuide.register();
        // Schwebfrucht: 4 s Slow Falling plus a small hop (DriftFruitItem), always edible like chorus fruit.
        DRIFT_FRUIT = item("drift_fruit", DriftFruitItem::new, new Item.Properties().food(
                new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).alwaysEdible().build(),
                Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(
                        new MobEffectInstance(MobEffects.SLOW_FALLING, DriftFruitItem.SLOW_FALL_TICKS, 0))).build())
                .compostable(ContextIntProviders.COMPOSTABLE_MEDIUM));
    }

    public static void entities() {
        EndWood.ALL.forEach(EndWood::entities);
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
        if (com.simplebuilding.modules.simpleend.guide.EndGuide.book() != null) out.add(com.simplebuilding.modules.simpleend.guide.EndGuide.stack());
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
