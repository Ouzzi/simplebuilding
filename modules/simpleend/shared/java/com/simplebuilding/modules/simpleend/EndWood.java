package com.simplebuilding.modules.simpleend;

import com.simplebuilding.modules.simpleend.block.EndBudBlock;
import com.simplebuilding.modules.simpleend.block.EndSticksBlock;
import com.simplebuilding.modules.simpleend.mixin.BlockSetTypeAccessor;
import com.simplebuilding.modules.simpleend.mixin.WoodTypeAccessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * One End wood set (Ashwood, Duskwood): the full Vanilla wood set of 26.3 (shelf included since
 * docs/ai/PLAN-ENDWOOD-FULL-2026-10-12.md) plus its bud, the
 * potted bud and the End-stick scrub block. Blocks/items/entity types are built inside the loader's registration
 * callbacks ({@link #blocks()}, {@link #items()}, {@link #entities()}).
 */
public final class EndWood {
    public static final EndWood ASHWOOD = new EndWood("ashwood", "ash_bud", MapColor.COLOR_LIGHT_GRAY, MapColor.COLOR_GRAY);
    public static final EndWood DUSKWOOD = new EndWood("duskwood", "dusk_bud", MapColor.TERRACOTTA_LIGHT_GRAY, MapColor.TERRACOTTA_GRAY);
    public static final List<EndWood> ALL = List.of(ASHWOOD, DUSKWOOD);

    public final String name;
    public final String budName;
    private final MapColor plankColor;
    private final MapColor barkColor;
    public WoodType woodType;
    public BlockSetType setType;
    public Block log, strippedLog, wood, strippedWood, planks, stairs, slab, fence, fenceGate, door, trapdoor, button, pressurePlate,
            sign, wallSign, hangingSign, wallHangingSign, shelf, leaves, bud, pottedBud, sticks;
    public Item boat, chestBoat;
    private EntityType<Boat> boatType;
    private EntityType<ChestBoat> chestBoatType;
    /** Every block of the set in registration order (id -> block); items are made for those with an item. */
    public final Map<String, Block> blockIds = new LinkedHashMap<>();
    public final Map<String, Item> itemIds = new LinkedHashMap<>();

    private EndWood(String name, String budName, MapColor plankColor, MapColor barkColor) {
        this.name = name;
        this.budName = budName;
        this.plankColor = plankColor;
        this.barkColor = barkColor;
    }

    public static EndWood byName(String name) {
        for (EndWood w : ALL) if (w.name.equals(name)) return w;
        throw new IllegalArgumentException(name);
    }

    public String id(String pattern) {
        return pattern.replace("W", name);
    }

    private Block block(String id, Function<Properties, Block> factory, Properties props) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleEnd.id(id));
        Block b = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(props.setId(key)));
        blockIds.put(id, b);
        return b;
    }

    private static Properties wood(MapColor color) {
        return Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava();
    }

    private Properties logProps() {
        return Properties.of().mapColor(barkColor).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava();
    }

    private static Properties wallOf(Block standing) {
        return Properties.of().overrideLootTable(standing.getLootTable()).overrideDescription(standing.getDescriptionId());
    }

    public void blocks() {
        String n = "simpleend_" + name;
        setType = BlockSetTypeAccessor.simpleend$register(new BlockSetType(n));
        woodType = WoodTypeAccessor.simpleend$register(new WoodType(n, setType));
        log = block(id("W_log"), RotatedPillarBlock::new, logProps());
        strippedLog = block(id("stripped_W_log"), RotatedPillarBlock::new, logProps().mapColor(plankColor));
        wood = block(id("W_wood"), RotatedPillarBlock::new, logProps());
        strippedWood = block(id("stripped_W_wood"), RotatedPillarBlock::new, logProps().mapColor(plankColor));
        planks = block(id("W_planks"), Block::new, wood(plankColor));
        Block base = planks;
        stairs = block(id("W_stairs"), p -> new StairBlock(base.defaultBlockState(), p) {}, Properties.ofLegacyCopy(planks));
        slab = block(id("W_slab"), SlabBlock::new, Properties.ofLegacyCopy(planks));
        fence = block(id("W_fence"), FenceBlock::new, wood(plankColor).forceSolidOn());
        fenceGate = block(id("W_fence_gate"), p -> new FenceGateBlock(woodType, p), wood(plankColor).forceSolidOn());
        door = block(id("W_door"), p -> new DoorBlock(setType, p) {},
                Properties.of().mapColor(plankColor).instrument(NoteBlockInstrument.BASS).strength(3.0F).noOcclusion().ignitedByLava().pushReaction(PushReaction.POPPED));
        trapdoor = block(id("W_trapdoor"), p -> new TrapDoorBlock(setType, p) {},
                Properties.of().mapColor(plankColor).instrument(NoteBlockInstrument.BASS).strength(3.0F).noOcclusion()
                        .isValidSpawn((s, g, p2, e) -> false).ignitedByLava());
        button = block(id("W_button"), p -> new ButtonBlock(setType, 30, p) {}, Properties.of().noCollision().strength(0.5F).pushReaction(PushReaction.POPPED));
        pressurePlate = block(id("W_pressure_plate"), p -> new PressurePlateBlock(setType, p) {},
                Properties.of().mapColor(plankColor).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(0.5F).ignitedByLava().pushReaction(PushReaction.POPPED));
        sign = block(id("W_sign"), p -> new StandingSignBlock(woodType, p),
                Properties.of().mapColor(plankColor).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava());
        wallSign = block(id("W_wall_sign"), p -> new WallSignBlock(woodType, p),
                wallOf(sign).mapColor(plankColor).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava());
        hangingSign = block(id("W_hanging_sign"), p -> new CeilingHangingSignBlock(woodType, p),
                Properties.of().mapColor(barkColor).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava());
        wallHangingSign = block(id("W_wall_hanging_sign"), p -> new WallHangingSignBlock(woodType, p),
                wallOf(hangingSign).mapColor(barkColor).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava());
        // Vanilla's shelf (Blocks.OAK_SHELF): planks colour, shelf sound, Vanilla shelf block entity (BlockEntityTypeMixin).
        shelf = block(id("W_shelf"), ShelfBlock::new,
                Properties.of().mapColor(plankColor).instrument(NoteBlockInstrument.BASS).sound(SoundType.SHELF).ignitedByLava().strength(2.0F, 3.0F));
        Properties leafProps = Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.2F).randomTicks().sound(SoundType.GRASS).noOcclusion()
                .isValidSpawn((s, g, p2, e) -> false).isSuffocating((s, g, p2) -> false)
                .ignitedByLava().pushReaction(PushReaction.POPPED).isRedstoneConductor((s, g, p2) -> false);
        leafProps = leafProps.noCollision(); // cobweb-like (N55, N65: both woods): entities pass through and get stuck
        leaves = block(id("W_leaves"), com.simplebuilding.modules.simpleend.block.EndLeavesBlock::new, leafProps);
        bud = block(budName, p -> new EndBudBlock(name, p),
                Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollision().randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.POPPED));
        pottedBud = block("potted_" + budName, p -> new FlowerPotBlock(bud, p), Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.POPPED));
        sticks = block(id("W_sticks"), EndSticksBlock::new,
                Properties.of().mapColor(barkColor).instrument(NoteBlockInstrument.BASS).strength(1.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    }

    private Item item(String id, Function<Item.Properties, Item> factory, Item.Properties props) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleEnd.id(id));
        Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
        itemIds.put(id, item);
        return item;
    }

    private Item blockItem(Block b, ResourceKey<net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider> fuel) {
        String id = BuiltInRegistries.BLOCK.getKey(b).getPath();
        Item.Properties p = new Item.Properties().useBlockDescriptionPrefix();
        if (fuel != null) p = p.cookingFuel(fuel);
        return item(id, props -> new BlockItem(b, props), p);
    }

    public void items() {
        var blocks = ContextIntProviders.COOKING_TIME_WOOD_BLOCKS;
        for (Block b : new Block[] {log, strippedLog, wood, strippedWood, planks, stairs, fence, fenceGate, trapdoor, pressurePlate}) blockItem(b, blocks);
        blockItem(slab, ContextIntProviders.COOKING_TIME_WOOD_SLABS);
        blockItem(button, ContextIntProviders.COOKING_TIME_WOOD_ITEMS_EXTRA_SMALL);
        item(id("W_door"), p -> new DoubleHighBlockItem(door, p), new Item.Properties().useBlockDescriptionPrefix().cookingFuel(ContextIntProviders.COOKING_TIME_WOOD_ITEMS_LARGE));
        item(id("W_sign"), p -> new StandingAndWallBlockItem(sign, wallSign, Direction.DOWN, p),
                new Item.Properties().useBlockDescriptionPrefix().cookingFuel(ContextIntProviders.COOKING_TIME_WOOD_ITEMS_LARGE).stacksTo(16).signText());
        item(id("W_hanging_sign"), p -> new HangingSignItem(hangingSign, wallHangingSign, p),
                new Item.Properties().useBlockDescriptionPrefix().cookingFuel(ContextIntProviders.COOKING_TIME_HANGING_SIGNS).stacksTo(16).signText());
        item(id("W_shelf"), p -> new BlockItem(shelf, p), new Item.Properties().useBlockDescriptionPrefix()
                .component(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY)
                .cookingFuel(ContextIntProviders.COOKING_TIME_WOOD_BLOCKS));
        item(id("W_leaves"), p -> new BlockItem(leaves, p), new Item.Properties().useBlockDescriptionPrefix().compostable(ContextIntProviders.COMPOSTABLE_LOW));
        item(budName, p -> new BlockItem(bud, p), new Item.Properties().useBlockDescriptionPrefix()
                .compostable(ContextIntProviders.COMPOSTABLE_LOW).cookingFuel(ContextIntProviders.COOKING_TIME_DRY_PLANTS));
        blockItem(sticks, ContextIntProviders.COOKING_TIME_WOOD_BLOCKS);
        boat = item(id("W_boat"), p -> new BoatItem(boatType(), p), new Item.Properties().cookingFuel(ContextIntProviders.COOKING_TIME_BOATS).stacksTo(1));
        chestBoat = item(id("W_chest_boat"), p -> new BoatItem(chestBoatType(), p), new Item.Properties().cookingFuel(ContextIntProviders.COOKING_TIME_BOATS).stacksTo(1));
    }

    public EntityType<Boat> boatType() {
        if (boatType == null) {
            boatType = EntityType.Builder.<Boat>of((t, level) -> new Boat(t, level, () -> boat), MobCategory.MISC)
                    .noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, SimpleEnd.id(id("W_boat"))));
        }
        return boatType;
    }

    public EntityType<ChestBoat> chestBoatType() {
        if (chestBoatType == null) {
            chestBoatType = EntityType.Builder.<ChestBoat>of((t, level) -> new ChestBoat(t, level, () -> chestBoat), MobCategory.MISC)
                    .noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, SimpleEnd.id(id("W_chest_boat"))));
        }
        return chestBoatType;
    }

    public void entities() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, SimpleEnd.id(id("W_boat")), boatType());
        Registry.register(BuiltInRegistries.ENTITY_TYPE, SimpleEnd.id(id("W_chest_boat")), chestBoatType());
    }

    /** Creative tab order, like a Vanilla wood row. */
    public List<Item> tabItems() {
        List<Item> out = new ArrayList<>();
        for (String id : List.of("W_log", "W_wood", "stripped_W_log", "stripped_W_wood", "W_planks", "W_stairs", "W_slab", "W_fence",
                "W_fence_gate", "W_door", "W_trapdoor", "W_pressure_plate", "W_button", "W_sign", "W_hanging_sign", "W_shelf", "W_boat",
                "W_chest_boat", "W_leaves", "B", "W_sticks")) {
            out.add(itemIds.get(id.equals("B") ? budName : id(id)));
        }
        return out;
    }
}
