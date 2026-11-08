package com.simplebuilding.modules.simplelayers;

import com.simplebuilding.modules.simplelayers.block.BackroomBlock;
import com.simplebuilding.modules.simplelayers.block.CloudBlock;
import com.simplebuilding.modules.simplelayers.block.CushionBlock;
import com.simplebuilding.modules.simplelayers.block.ExitDoorBlock;
import com.simplebuilding.modules.simplelayers.block.GlitchedBedrockBlock;
import com.simplebuilding.modules.simplelayers.block.MistBlock;
import com.simplebuilding.modules.simplelayers.block.NeonLightBlock;
import com.simplebuilding.modules.simplelayers.block.RiftBlock;
import com.simplebuilding.modules.simplelayers.block.TooltipBlockItem;
import com.simplebuilding.modules.simplelayers.world.BackroomsChunkGenerator;
import com.simplebuilding.modules.simplelayers.world.GlitchedBedrockFeature;
import com.simplebuilding.modules.simplelayers.world.MistPitFeature;
import com.simplebuilding.modules.simplelayers.world.SkyChestFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Everything Simple Layers registers (Fabric: directly in the initializer), in this order: sounds, blocks, items, creative tab,
 * world generation types. Cloud blocks: concept §1.1; Backrooms blocks: concept §2.1.
 */
public final class LayersRegistry {
    /** Item ids in tab order (the test centre station and the coverage tests read this). */
    public static final List<String> ITEM_PATHS = List.of("cloud_block", "cloud_stone", "mist_block", "damp_carpet", "wallpaper",
            "striped_wallpaper", "dotted_wallpaper", "ceiling_tile", "stained_ceiling_tile", "neon_light", "exit_sign",
            "glitched_bedrock", "almond_water", "backdoor_key");
    /** Fall damage factors (concept §1.1: Cloud Block -30 %, Cloud Stone -10 %). */
    public static final float CLOUD_DAMAGE = 0.7F, CLOUD_STONE_DAMAGE = 0.9F;

    public static SoundEvent BACKROOMS_HUM, BACKROOMS_ADDITIONS, CLOUD_WIND, NEON_BUZZ, LISTENER_STEPS;
    public static Block CLOUD_BLOCK, CLOUD_STONE, MIST_BLOCK;
    public static Block DAMP_CARPET, WALLPAPER, STRIPED_WALLPAPER, DOTTED_WALLPAPER, CEILING_TILE, STAINED_CEILING_TILE;
    public static Block NEON_LIGHT, EXIT_SIGN, EXIT_DOOR, GLITCHED_BEDROCK, RIFT;
    public static Item ALMOND_WATER, BACKDOOR_KEY;
    public static List<Item> ITEMS = List.of();
    public static CreativeModeTab TAB;

    private LayersRegistry() {}

    public static void registerSounds() {
        BACKROOMS_HUM = sound("ambient.backrooms.hum");
        BACKROOMS_ADDITIONS = sound("ambient.backrooms.additions");
        CLOUD_WIND = sound("ambient.cloud_sea.wind");
        NEON_BUZZ = sound("block.neon_light.buzz");
        LISTENER_STEPS = sound("ambient.backrooms.listener");
    }

    public static void registerBlocks() {
        CLOUD_BLOCK = block("cloud_block", p -> new CloudBlock(p, CLOUD_DAMAGE), props(MapColor.SNOW, SoundType.WOOL).instabreak()
                .noOcclusion().isValidSpawn((s, l, pos, type) -> false).isRedstoneConductor((s, l, pos) -> false));
        CLOUD_STONE = block("cloud_stone", p -> new CushionBlock(p, CLOUD_STONE_DAMAGE), props(MapColor.QUARTZ, SoundType.CALCITE).instabreak()
                .isValidSpawn((s, l, pos, type) -> false));
        MIST_BLOCK = block("mist_block", MistBlock::new, props(MapColor.SNOW, SoundType.WOOL).instabreak().noCollision().noOcclusion()
                .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false).isValidSpawn((s, l, pos, type) -> false)
                .isRedstoneConductor((s, l, pos) -> false));
        DAMP_CARPET = block("damp_carpet", BackroomBlock::new, props(MapColor.SAND, SoundType.WOOL).strength(0.8F));
        WALLPAPER = block("wallpaper", BackroomBlock::new, props(MapColor.COLOR_YELLOW, SoundType.WOOD).strength(1.0F));
        STRIPED_WALLPAPER = block("striped_wallpaper", BackroomBlock::new, props(MapColor.COLOR_YELLOW, SoundType.WOOD).strength(1.0F));
        DOTTED_WALLPAPER = block("dotted_wallpaper", BackroomBlock::new, props(MapColor.COLOR_YELLOW, SoundType.WOOD).strength(1.0F));
        CEILING_TILE = block("ceiling_tile", BackroomBlock::new, props(MapColor.SAND, SoundType.WOOD).strength(0.6F));
        STAINED_CEILING_TILE = block("stained_ceiling_tile", BackroomBlock::new, props(MapColor.SAND, SoundType.WOOD).strength(0.6F));
        NEON_LIGHT = block("neon_light", NeonLightBlock::new, props(MapColor.SNOW, SoundType.GLASS).strength(0.3F).lightLevel(NeonLightBlock::light));
        EXIT_SIGN = block("exit_sign", BackroomBlock::new, props(MapColor.COLOR_GREEN, SoundType.GLASS).strength(0.5F).lightLevel(s -> 8));
        EXIT_DOOR = block("exit_door", ExitDoorBlock::new, props(MapColor.COLOR_GRAY, SoundType.IRON).strength(3.0F).noLootTable()
                .pushReaction(PushReaction.IMMOVEABLE));
        GLITCHED_BEDROCK = block("glitched_bedrock", GlitchedBedrockBlock::new, props(MapColor.STONE, SoundType.STONE)
                .strength(GlitchedBedrockBlock.HARDNESS, 3600000.0F).requiresCorrectToolForDrops().noLootTable().pushReaction(PushReaction.IMMOVEABLE)
                .isValidSpawn((s, l, pos, type) -> false));
        RIFT = block("rift", RiftBlock::new, props(MapColor.COLOR_BLACK, SoundType.STONE).strength(-1.0F, 3600000.0F).noCollision()
                .noOcclusion().noLootTable().lightLevel(s -> 4).pushReaction(PushReaction.IMMOVEABLE).isValidSpawn((s, l, pos, type) -> false)
                .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false));
    }

    public static void registerItems() {
        List<Item> out = new ArrayList<>();
        for (Block b : List.of(CLOUD_BLOCK, CLOUD_STONE, MIST_BLOCK)) out.add(blockItem(b, true));
        for (Block b : List.of(DAMP_CARPET, WALLPAPER, STRIPED_WALLPAPER, DOTTED_WALLPAPER, CEILING_TILE, STAINED_CEILING_TILE, NEON_LIGHT, EXIT_SIGN)) {
            out.add(blockItem(b, false));
        }
        out.add(blockItem(GLITCHED_BEDROCK, true));
        ALMOND_WATER = item("almond_water", Item::new, new Item.Properties().stacksTo(16).usingConvertsTo(Items.GLASS_BOTTLE)
                .component(net.minecraft.core.component.DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(1.6F)
                        .animation(ItemUseAnimation.DRINK).sound(SoundEvents.GENERIC_DRINK).hasConsumeParticles(false)
                        .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.INSTANT_HEALTH, 1, 0)))
                        .onConsume(new RemoveStatusEffectsConsumeEffect(MobEffects.NAUSEA)).build()));
        out.add(ALMOND_WATER);
        BACKDOOR_KEY = item("backdoor_key", com.simplebuilding.modules.simplelayers.block.KeyItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        out.add(BACKDOOR_KEY);
        ITEMS = List.copyOf(out);
        com.simplebuilding.modules.simplelayers.guide.LayersGuide.register();
    }

    public static void registerTab() {
        TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleLayers.id("layers"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemgroup.simplelayers.layers"))
                .icon(() -> new ItemStack(CLOUD_BLOCK))
                .displayItems((parameters, output) -> tabStacks().forEach(output::accept))
                .build());
    }

    /** World generation types (before the data packs load): the Backrooms chunk generator and the three features. */
    public static void registerWorldgen() {
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, SimpleLayers.id("backrooms"), BackroomsChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE, SimpleLayers.id("mist_pit"), MistPitFeature.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE, SimpleLayers.id("sky_chest"), SkyChestFeature.CODEC);
        Registry.register(BuiltInRegistries.FEATURE_TYPE, SimpleLayers.id("glitched_bedrock"), GlitchedBedrockFeature.CODEC);
    }

    /** Every item in tab order, then the guide (empty while the module is switched off). */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (!SimpleLayers.on()) return out;
        ITEMS.forEach(item -> out.add(new ItemStack(item)));
        out.add(com.simplebuilding.modules.simplelayers.guide.LayersGuide.stack());
        return out;
    }

    /** The Backrooms' building blocks (wallpaper, carpet, tiles, lights, sign, door) - what the dimension protects. */
    public static boolean isBackroomBlock(Block block) {
        return block instanceof BackroomBlock;
    }

    private static BlockBehaviour.Properties props(MapColor color, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(color).sound(sound);
    }

    private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleLayers.id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    private static Item blockItem(Block block, boolean tooltip) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        return item(name, p -> tooltip ? new TooltipBlockItem(block, p) : new BlockItem(block, p), new Item.Properties().useBlockDescriptionPrefix());
    }

    private static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleLayers.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
    }

    private static SoundEvent sound(String name) {
        var id = SimpleLayers.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
