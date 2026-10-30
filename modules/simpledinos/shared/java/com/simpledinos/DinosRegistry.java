package com.simpledinos;

import com.simpledinos.block.DinoEggBlock;
import com.simpledinos.block.DinoEggBlockEntity;
import com.simpledinos.block.FossilBlock;
import com.simpledinos.block.FossilBlockEntity;
import com.simpledinos.entity.Trex;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/**
 * Everything Simple Dinosaurs registers: the fossil block, one egg block per dinosaur, the T. rex, the large saddle, three
 * T. rex armors and the guide book. Loaders call {@link #blocks}, {@link #items}, {@link #entityTypes} inside their matching
 * registry event (Fabric: {@link #registerAll}); the two block entity types come from the loader because their constructors
 * differ ({@link #registerBlockEntityTypes}). The other four dinosaurs have eggs but no entity yet (stage 1 builds the T. rex).
 */
public final class DinosRegistry {
    public static final ResourceKey<Block> FOSSIL_KEY = blockKey("fossil_block");
    public static final ResourceKey<EntityType<?>> TREX_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SimpleDinos.id("trex"));

    /** Entities that may wear the large saddle / the dinosaur armor (tags in data/simpledinos/tags/entity_type). */
    public static final TagKey<EntityType<?>> LARGE_SADDLE_WEARERS = TagKey.create(Registries.ENTITY_TYPE, SimpleDinos.id("can_equip_large_saddle"));
    public static final TagKey<EntityType<?>> ARMOR_WEARERS = TagKey.create(Registries.ENTITY_TYPE, SimpleDinos.id("can_wear_dino_armor"));

    public static final Block FOSSIL = new FossilBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(3.0F, 3.0F)
            .requiresCorrectToolForDrops().sound(SoundType.BONE_BLOCK).instrument(NoteBlockInstrument.XYLOPHONE).setId(FOSSIL_KEY));
    /** One egg block per kind (dinosaurs without an entity yet still have their egg). */
    public static final Map<DinoKind, Block> EGGS = new EnumMap<>(DinoKind.class);
    public static final Map<DinoKind, Item> EGG_ITEMS = new EnumMap<>(DinoKind.class);

    /** Same size as the Vanilla horse's passenger point scaled up: the rider sits on the T. rex's back (3.6 m). */
    public static final EntityType<Trex> TREX = EntityType.Builder.<Trex>of(Trex::new, MobCategory.CREATURE)
            .sized(3.0F, 4.0F).eyeHeight(3.6F).passengerAttachments(3.55F).clientTrackingRange(12).build(TREX_KEY);

    public static final ResourceKey<Item> FOSSIL_ITEM_KEY = itemKey("fossil_block");
    public static final ResourceKey<Item> LARGE_SADDLE_KEY = itemKey("large_saddle");
    public static final ResourceKey<Item> TREX_ARMOR_LEATHER_KEY = itemKey("trex_armor_leather");
    public static final ResourceKey<Item> TREX_ARMOR_IRON_KEY = itemKey("trex_armor_iron");
    public static final ResourceKey<Item> TREX_ARMOR_DIAMOND_KEY = itemKey("trex_armor_diamond");

    public static final Item FOSSIL_ITEM;
    public static final Item LARGE_SADDLE;
    public static final Item TREX_ARMOR_LEATHER;
    public static final Item TREX_ARMOR_IRON;
    public static final Item TREX_ARMOR_DIAMOND;

    public static BlockEntityType<FossilBlockEntity> FOSSIL_BE;
    public static BlockEntityType<DinoEggBlockEntity> EGG_BE;

    static {
        for (DinoKind kind : DinoKind.values()) {
            ResourceKey<Block> key = blockKey(kind.id + "_egg");
            EGGS.put(kind, new DinoEggBlock(kind, BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.5F).sound(SoundType.BONE_BLOCK)
                    .noOcclusion().setId(key)));
            ResourceKey<Item> itemKey = itemKey(kind.id + "_egg");
            EGG_ITEMS.put(kind, new BlockItem(EGGS.get(kind), new Item.Properties().useBlockDescriptionPrefix().stacksTo(16).setId(itemKey)));
        }
        FOSSIL_ITEM = new BlockItem(FOSSIL, new Item.Properties().useBlockDescriptionPrefix().setId(FOSSIL_ITEM_KEY));
        var entities = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ENTITY_TYPE);
        LARGE_SADDLE = new Item(new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE,
                Equippable.builder(EquipmentSlot.SADDLE).setEquipSound(SoundEvents.HORSE_SADDLE)
                        .setAllowedEntities(entities.getOrThrow(LARGE_SADDLE_WEARERS)).setEquipOnInteract(true).setCanBeSheared(true)
                        .setShearingSound(SoundEvents.SADDLE_UNEQUIP).build()).setId(LARGE_SADDLE_KEY));
        TREX_ARMOR_LEATHER = armor(ArmorMaterials.LEATHER, TREX_ARMOR_LEATHER_KEY);
        TREX_ARMOR_IRON = armor(ArmorMaterials.IRON, TREX_ARMOR_IRON_KEY);
        TREX_ARMOR_DIAMOND = armor(ArmorMaterials.DIAMOND, TREX_ARMOR_DIAMOND_KEY);
    }

    /** Body armor with the Vanilla horse-armor values of the material (leather 3, iron 5, diamond 11), worn by the T. rex. */
    private static Item armor(ArmorMaterial material, ResourceKey<Item> key) {
        var entities = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ENTITY_TYPE);
        return new Item(new Item.Properties().attributes(material.createAttributes(ArmorType.BODY)).component(DataComponents.EQUIPPABLE,
                Equippable.builder(EquipmentSlot.BODY).setEquipSound(SoundEvents.HORSE_ARMOR)
                        .setAllowedEntities(entities.getOrThrow(ARMOR_WEARERS)).setDamageOnHurt(false).setCanBeSheared(true)
                        .setShearingSound(SoundEvents.HORSE_ARMOR_UNEQUIP).build()).stacksTo(1).setId(key));
    }

    private static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK, SimpleDinos.id(path));
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, SimpleDinos.id(path));
    }

    public static Item eggItem(DinoKind kind) {
        return EGG_ITEMS.get(kind);
    }

    public static AttributeSupplier.Builder trexAttributes() {
        return Trex.createAttributes();
    }

    public static void blocks(BiConsumer<ResourceKey<Block>, Block> sink) {
        sink.accept(FOSSIL_KEY, FOSSIL);
        EGGS.forEach((kind, block) -> sink.accept(blockKey(kind.id + "_egg"), block));
    }

    public static void items(BiConsumer<ResourceKey<Item>, Item> sink) {
        sink.accept(FOSSIL_ITEM_KEY, FOSSIL_ITEM);
        EGG_ITEMS.forEach((kind, item) -> sink.accept(itemKey(kind.id + "_egg"), item));
        sink.accept(LARGE_SADDLE_KEY, LARGE_SADDLE);
        sink.accept(TREX_ARMOR_LEATHER_KEY, TREX_ARMOR_LEATHER);
        sink.accept(TREX_ARMOR_IRON_KEY, TREX_ARMOR_IRON);
        sink.accept(TREX_ARMOR_DIAMOND_KEY, TREX_ARMOR_DIAMOND);
        com.simpledinos.guide.DinosGuide.register();
    }

    public static void entityTypes(BiConsumer<ResourceKey<EntityType<?>>, EntityType<?>> sink) {
        sink.accept(TREX_KEY, TREX);
    }

    public static Set<Block> fossilBlocks() {
        return Set.of(FOSSIL);
    }

    public static Set<Block> eggBlocks() {
        return Set.copyOf(EGGS.values());
    }

    /** Registers the two block entity types the loader built (the constructors differ between the loaders). */
    public static void registerBlockEntityTypes(BlockEntityType<FossilBlockEntity> fossil, BlockEntityType<DinoEggBlockEntity> egg) {
        FOSSIL_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleDinos.id("fossil_block"), fossil);
        EGG_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleDinos.id("dino_egg"), egg);
    }

    /** Fabric: registries are open during init. */
    public static void registerAll() {
        blocks((k, v) -> Registry.register(BuiltInRegistries.BLOCK, k, v));
        items((k, v) -> Registry.register(BuiltInRegistries.ITEM, k, v));
        entityTypes((k, v) -> Registry.register(BuiltInRegistries.ENTITY_TYPE, k, v));
    }

    /** Creative stacks of the module (empty while it is switched off). */
    public static List<ItemStack> tabStacks() {
        if (!SimpleDinos.on()) return List.of();
        List<ItemStack> stacks = new java.util.ArrayList<>();
        stacks.add(new ItemStack(FOSSIL_ITEM));
        for (DinoKind kind : DinoKind.values()) stacks.add(new ItemStack(EGG_ITEMS.get(kind)));
        stacks.add(new ItemStack(LARGE_SADDLE));
        stacks.add(new ItemStack(TREX_ARMOR_LEATHER));
        stacks.add(new ItemStack(TREX_ARMOR_IRON));
        stacks.add(new ItemStack(TREX_ARMOR_DIAMOND));
        return stacks;
    }

    private DinosRegistry() {}
}
