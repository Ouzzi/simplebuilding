package com.simplefoundry;

import com.simplefoundry.block.DeskBlock;
import com.simplefoundry.block.DeskBlockEntity;
import com.simplefoundry.block.GrateBlock;
import com.simplefoundry.block.SpoutBlock;
import com.simplefoundry.block.TrayBlock;
import com.simplefoundry.block.TrayBlockEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Everything Simple Foundry registers: six blocks (hull brick, melting chamber, desk, fire grate, spout, casting tray), their items, two block entities and the creative tab. */
public final class FoundryRegistry {
    /** Block path -> block and item path -> item, both in creative-tab order. */
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    /** Blocks that close the hull of a foundry (the six own blocks that may stand on it plus Vanilla glass). */
    public static final TagKey<Block> HULL = TagKey.create(Registries.BLOCK, SimpleFoundry.id("hull"));

    public static final Block FOUNDRY_BRICK = block("foundry_brick", Blocks.BRICKS, Block::new);
    public static final Block MELTING_CHAMBER = block("melting_chamber", Blocks.BRICKS, Block::new);
    public static final Block DESK = block("foundry_desk", Blocks.BRICKS, DeskBlock::new);
    public static final Block FIRE_GRATE = block("fire_grate", Blocks.BRICKS, GrateBlock::new);
    public static final Block FOUNDRY_SPOUT = block("foundry_spout", Blocks.BRICKS, SpoutBlock::new);
    public static final Block CASTING_TRAY = block("casting_tray", Blocks.BRICKS, TrayBlock::new);

    public static BlockEntityType<DeskBlockEntity> DESK_BE;
    public static BlockEntityType<TrayBlockEntity> TRAY_BE;
    public static CreativeModeTab TAB;

    static {
        for (String path : List.copyOf(BLOCKS.keySet())) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleFoundry.id(path));
            ITEMS.put(path, new BlockItem(BLOCKS.get(path), new Item.Properties().useBlockDescriptionPrefix().setId(key)));
        }
    }

    private static Block block(String path, Block base, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleFoundry.id(path));
        Block block = factory.apply(BlockBehaviour.Properties.ofFullCopy(base).setId(key).strength(2.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
        BLOCKS.put(path, block);
        return block;
    }

    public static Item item(String path) {
        return ITEMS.get(path);
    }

    /** Ids ("simplefoundry:path") of every item, for the test centre station. */
    public static List<String> itemIds() {
        List<String> out = new ArrayList<>();
        ITEMS.keySet().forEach(path -> out.add(SimpleFoundry.MOD_ID + ":" + path));
        return out;
    }

    /** Fabric: registries are open during init. */
    public static void registerAll() {
        BLOCKS.forEach((path, block) -> Registry.register(BuiltInRegistries.BLOCK, SimpleFoundry.id(path), block));
        ITEMS.forEach((path, item) -> Registry.register(BuiltInRegistries.ITEM, SimpleFoundry.id(path), item));
        DESK_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleFoundry.id("foundry_desk"), new BlockEntityType<>(DeskBlockEntity::new, java.util.Set.of(DESK)));
        TRAY_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleFoundry.id("casting_tray"), new BlockEntityType<>(TrayBlockEntity::new, java.util.Set.of(CASTING_TRAY)));
        // IG31-3: the items stand in the module's own tab only (CreativeTabPolicy); tab order by the mod tree
        TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleFoundry.id("foundry"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemgroup.simplefoundry.foundry"))
                .icon(() -> new ItemStack(DESK))
                .displayItems((parameters, output) -> {
                    if (SimpleFoundry.on()) ITEMS.values().forEach(item -> output.accept(new ItemStack(item)));
                })
                .build());
    }

    private FoundryRegistry() {}
}
