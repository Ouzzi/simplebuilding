package com.simpledinos;

import com.simpledinos.block.DinoEggBlock;
import com.simpledinos.block.DinoEggBlockEntity;
import com.simpledinos.block.FossilBlock;
import com.simpledinos.block.FossilBlockEntity;
import com.simpledinos.entity.Parasaurolophus;
import com.simpledinos.entity.Stegosaurus;
import com.simpledinos.entity.Trex;
import com.simpledinos.entity.Triceratops;
import com.simpledinos.entity.Utahraptor;
import com.simpledinos.item.DinoPlowItem;
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
 * Everything Simple Dinosaurs registers: the fossil block, one egg block per dinosaur, the T. rex, the Triceratops and the Stegosaurus,
 * the large saddle, the Dino Plow, three armors per built dinosaur, the eggshells of the four herbivores and raptors (a-dinorest1) and the guide book. Loaders call {@link #blocks}, {@link #items}, {@link #entityTypes} inside their matching
 * registry event (Fabric: {@link #registerAll}); the two block entity types come from the loader because their constructors
 * differ ({@link #registerBlockEntityTypes}). a-dinorest2: the Stegosaurus with its armor and the Dino Plow. a-dinorest3: the
 * Parasaurolophus with its armor. b-utah48: the Utahraptor with its armor and the raptor feather - all five dinosaurs hatch.
 */
public final class DinosRegistry {
    public static final ResourceKey<Block> FOSSIL_KEY = blockKey("fossil_block");
    public static final ResourceKey<EntityType<?>> TREX_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SimpleDinos.id("trex"));
    public static final ResourceKey<EntityType<?>> TRICERATOPS_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SimpleDinos.id("triceratops"));
    public static final ResourceKey<EntityType<?>> STEGOSAURUS_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SimpleDinos.id("stegosaurus"));
    public static final ResourceKey<EntityType<?>> PARASAUROLOPHUS_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SimpleDinos.id("parasaurolophus"));
    public static final ResourceKey<EntityType<?>> UTAHRAPTOR_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SimpleDinos.id("utahraptor"));

    /** Entities that may wear the large saddle / the dinosaur armor (tags in data/simpledinos/tags/entity_type). */
    public static final TagKey<EntityType<?>> LARGE_SADDLE_WEARERS = TagKey.create(Registries.ENTITY_TYPE, SimpleDinos.id("can_equip_large_saddle"));
    public static final TagKey<EntityType<?>> ARMOR_WEARERS = TagKey.create(Registries.ENTITY_TYPE, SimpleDinos.id("can_wear_dino_armor"));
    /** a-dinorest1: each dinosaur has its own armor (owner DINO28-1); the T. rex armor keeps the tag above, the Triceratops armor this one. */
    public static final TagKey<EntityType<?>> TRICERATOPS_ARMOR_WEARERS = TagKey.create(Registries.ENTITY_TYPE, SimpleDinos.id("can_wear_triceratops_armor"));
    public static final TagKey<EntityType<?>> STEGOSAURUS_ARMOR_WEARERS = TagKey.create(Registries.ENTITY_TYPE, SimpleDinos.id("can_wear_stegosaurus_armor"));
    public static final TagKey<EntityType<?>> PARASAUROLOPHUS_ARMOR_WEARERS = TagKey.create(Registries.ENTITY_TYPE, SimpleDinos.id("can_wear_parasaurolophus_armor"));
    public static final TagKey<EntityType<?>> UTAHRAPTOR_ARMOR_WEARERS = TagKey.create(Registries.ENTITY_TYPE, SimpleDinos.id("can_wear_utahraptor_armor"));

    public static final Block FOSSIL = new FossilBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(3.0F, 3.0F)
            .requiresCorrectToolForDrops().sound(SoundType.BONE_BLOCK).instrument(NoteBlockInstrument.XYLOPHONE).setId(FOSSIL_KEY));
    /** One egg block per kind (dinosaurs without an entity yet still have their egg). */
    public static final Map<DinoKind, Block> EGGS = new EnumMap<>(DinoKind.class);
    public static final Map<DinoKind, Item> EGG_ITEMS = new EnumMap<>(DinoKind.class);
    /** a-dinorest1 (concept DINO36 6): the shell that falls when a dinosaur hatches, per kind except the T. rex. */
    public static final Map<DinoKind, Item> EGGSHELLS = new EnumMap<>(DinoKind.class);

    /** Same size as the Vanilla horse's passenger point scaled up: the rider sits on the T. rex's back (3.6 m). */
    public static final EntityType<Trex> TREX = EntityType.Builder.<Trex>of(Trex::new, MobCategory.CREATURE)
            .sized(3.6F, 3.5F).eyeHeight(2.7F).passengerAttachments(2.66F).clientTrackingRange(12).build(TREX_KEY);
    /**
     * a-dinorest1: the core box covers the barrel body and the legs (2.6 x 2.6, the body is 2.45 blocks long, the arched back 2.6 high);
     * head with frill and horns (reach 2.85) and tail (2.9) have part boxes. The rider sits on the saddle over the shoulders.
     */
    public static final EntityType<Triceratops> TRICERATOPS = EntityType.Builder.<Triceratops>of(Triceratops::new, MobCategory.CREATURE)
            .sized(2.6F, 2.6F).eyeHeight(1.75F).passengerAttachments(2.45F).clientTrackingRange(10).build(TRICERATOPS_KEY);
    /**
     * a-dinorest2: the core box covers body, legs and plates (2.6 x 3.7: the body is 2.9 blocks long, the hip plates reach 3.66); the low
     * head (reach 2.55) and the tail with its spikes (3.9) have part boxes. The rider sits on the saddle over the shoulders, in front of
     * the plates (0.95 blocks forward).
     */
    public static final EntityType<Stegosaurus> STEGOSAURUS = EntityType.Builder.<Stegosaurus>of(Stegosaurus::new, MobCategory.CREATURE)
            .sized(2.6F, 3.7F).eyeHeight(1.4F).passengerAttachments(new net.minecraft.world.phys.Vec3(0.0, 2.5, 0.95)).clientTrackingRange(10)
            .build(STEGOSAURUS_KEY);
    /**
     * a-dinorest3: the core box covers body, legs and the hip ridge (2.2 x 2.5: the body is 2.26 blocks long, the ridge reaches 2.48); neck,
     * head and crest (reach 2.55, crest tip 3.25 high) and the tail (3.8) have part boxes. The rider sits on the saddle in the middle of the
     * back, in front of the hips (0.05 blocks behind the centre), clear of the crest even in the call pose.
     */
    public static final EntityType<Parasaurolophus> PARASAUROLOPHUS = EntityType.Builder.<Parasaurolophus>of(Parasaurolophus::new, MobCategory.CREATURE)
            .sized(2.2F, 2.5F).eyeHeight(2.3F).passengerAttachments(new net.minecraft.world.phys.Vec3(0.0, 2.28, -0.05)).clientTrackingRange(10)
            .build(PARASAUROLOPHUS_KEY);
    /**
     * b-utah48: the core box covers the slim body, the legs and the saddle (1.3 x 1.6: the body is 1.03 blocks long, the back 1.45 and the
     * saddle 1.55 high; it fits through corridors two blocks wide); the S-neck with the head (reach 1.27, top 2.0) and the stiff tail (2.4)
     * have part boxes. The rider sits on the saddle over the middle of the back.
     */
    public static final EntityType<Utahraptor> UTAHRAPTOR = EntityType.Builder.<Utahraptor>of(Utahraptor::new, MobCategory.CREATURE)
            .sized(1.3F, 1.6F).eyeHeight(1.75F).passengerAttachments(new net.minecraft.world.phys.Vec3(0.0, 1.5, 0.05)).clientTrackingRange(10)
            .build(UTAHRAPTOR_KEY);

    public static final ResourceKey<Item> FOSSIL_ITEM_KEY = itemKey("fossil_block");
    public static final ResourceKey<Item> LARGE_SADDLE_KEY = itemKey("large_saddle");
    public static final ResourceKey<Item> TREX_ARMOR_LEATHER_KEY = itemKey("trex_armor_leather");
    public static final ResourceKey<Item> TREX_ARMOR_IRON_KEY = itemKey("trex_armor_iron");
    public static final ResourceKey<Item> TREX_ARMOR_DIAMOND_KEY = itemKey("trex_armor_diamond");
    public static final ResourceKey<Item> TRICERATOPS_ARMOR_LEATHER_KEY = itemKey("triceratops_armor_leather");
    public static final ResourceKey<Item> TRICERATOPS_ARMOR_IRON_KEY = itemKey("triceratops_armor_iron");
    public static final ResourceKey<Item> TRICERATOPS_ARMOR_DIAMOND_KEY = itemKey("triceratops_armor_diamond");
    public static final ResourceKey<Item> STEGOSAURUS_ARMOR_LEATHER_KEY = itemKey("stegosaurus_armor_leather");
    public static final ResourceKey<Item> STEGOSAURUS_ARMOR_IRON_KEY = itemKey("stegosaurus_armor_iron");
    public static final ResourceKey<Item> STEGOSAURUS_ARMOR_DIAMOND_KEY = itemKey("stegosaurus_armor_diamond");
    public static final ResourceKey<Item> DINO_PLOW_KEY = itemKey("dino_plow");
    public static final ResourceKey<Item> PARASAUROLOPHUS_ARMOR_LEATHER_KEY = itemKey("parasaurolophus_armor_leather");
    public static final ResourceKey<Item> PARASAUROLOPHUS_ARMOR_IRON_KEY = itemKey("parasaurolophus_armor_iron");
    public static final ResourceKey<Item> PARASAUROLOPHUS_ARMOR_DIAMOND_KEY = itemKey("parasaurolophus_armor_diamond");
    public static final ResourceKey<Item> UTAHRAPTOR_ARMOR_LEATHER_KEY = itemKey("utahraptor_armor_leather");
    public static final ResourceKey<Item> UTAHRAPTOR_ARMOR_IRON_KEY = itemKey("utahraptor_armor_iron");
    public static final ResourceKey<Item> UTAHRAPTOR_ARMOR_DIAMOND_KEY = itemKey("utahraptor_armor_diamond");
    public static final ResourceKey<Item> RAPTOR_FEATHER_KEY = itemKey("raptor_feather");
    /** Durability of the Dino Plow (concept 4.2): one per tilled field. */
    public static final int PLOW_DURABILITY = 750;

    public static final Item FOSSIL_ITEM;
    public static final Item LARGE_SADDLE;
    public static final Item TREX_ARMOR_LEATHER;
    public static final Item TREX_ARMOR_IRON;
    public static final Item TREX_ARMOR_DIAMOND;
    public static final Item TRICERATOPS_ARMOR_LEATHER;
    public static final Item TRICERATOPS_ARMOR_IRON;
    public static final Item TRICERATOPS_ARMOR_DIAMOND;
    public static final Item STEGOSAURUS_ARMOR_LEATHER;
    public static final Item STEGOSAURUS_ARMOR_IRON;
    public static final Item STEGOSAURUS_ARMOR_DIAMOND;
    public static final Item DINO_PLOW;
    public static final Item PARASAUROLOPHUS_ARMOR_LEATHER;
    public static final Item PARASAUROLOPHUS_ARMOR_IRON;
    public static final Item PARASAUROLOPHUS_ARMOR_DIAMOND;
    public static final Item UTAHRAPTOR_ARMOR_LEATHER;
    public static final Item UTAHRAPTOR_ARMOR_IRON;
    public static final Item UTAHRAPTOR_ARMOR_DIAMOND;
    /** b-utah48 (concept DINO36 6): brushed off a grown Utahraptor; one gives three Vanilla feathers. */
    public static final Item RAPTOR_FEATHER;

    /**
     * P3-5 (b-dino82): the kind of egg a fossil holds ({@link DinoKind} ordinal) on its item. The Silk Touch drop copies it from the
     * block ({@code copy_components}), placing the item gives it back ({@link FossilBlockEntity#applyImplicitComponents}), so moving a
     * fossil never rolls a new kind. Items without it (creative, older stacks) roll at the place they are set down.
     */
    public static final net.minecraft.core.component.DataComponentType<Integer> FOSSIL_SORT = net.minecraft.core.component.DataComponentType.<Integer>builder()
            .persistent(com.mojang.serialization.Codec.intRange(0, DinoKind.values().length - 1))
            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build();
    public static final ResourceKey<net.minecraft.core.component.DataComponentType<?>> FOSSIL_SORT_KEY =
            ResourceKey.create(Registries.DATA_COMPONENT_TYPE, SimpleDinos.id("fossil_sort"));
    public static BlockEntityType<FossilBlockEntity> FOSSIL_BE;
    public static BlockEntityType<DinoEggBlockEntity> EGG_BE;

    static {
        for (DinoKind kind : DinoKind.values()) {
            ResourceKey<Block> key = blockKey(kind.id + "_egg");
            EGGS.put(kind, new DinoEggBlock(kind, BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.5F).sound(SoundType.BONE_BLOCK)
                    .noOcclusion().setId(key)));
            ResourceKey<Item> itemKey = itemKey(kind.id + "_egg");
            EGG_ITEMS.put(kind, new BlockItem(EGGS.get(kind), new Item.Properties().useBlockDescriptionPrefix().stacksTo(16).setId(itemKey)));
            if (kind != DinoKind.TREX) EGGSHELLS.put(kind, new Item(new Item.Properties().setId(itemKey(kind.id + "_eggshell"))));
        }
        FOSSIL_ITEM = new BlockItem(FOSSIL, new Item.Properties().useBlockDescriptionPrefix().setId(FOSSIL_ITEM_KEY));
        var entities = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ENTITY_TYPE);
        LARGE_SADDLE = new Item(new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE,
                Equippable.builder(EquipmentSlot.SADDLE).setEquipSound(SoundEvents.HORSE_SADDLE)
                        .setAllowedEntities(entities.getOrThrow(LARGE_SADDLE_WEARERS)).setEquipOnInteract(true).setCanBeSheared(true)
                        .setShearingSound(SoundEvents.SADDLE_UNEQUIP).build()).setId(LARGE_SADDLE_KEY));
        TREX_ARMOR_LEATHER = armor(ArmorMaterials.LEATHER, TREX_ARMOR_LEATHER_KEY, ARMOR_WEARERS);
        TREX_ARMOR_IRON = armor(ArmorMaterials.IRON, TREX_ARMOR_IRON_KEY, ARMOR_WEARERS);
        TREX_ARMOR_DIAMOND = armor(ArmorMaterials.DIAMOND, TREX_ARMOR_DIAMOND_KEY, ARMOR_WEARERS);
        TRICERATOPS_ARMOR_LEATHER = armor(ArmorMaterials.LEATHER, TRICERATOPS_ARMOR_LEATHER_KEY, TRICERATOPS_ARMOR_WEARERS);
        TRICERATOPS_ARMOR_IRON = armor(ArmorMaterials.IRON, TRICERATOPS_ARMOR_IRON_KEY, TRICERATOPS_ARMOR_WEARERS);
        TRICERATOPS_ARMOR_DIAMOND = armor(ArmorMaterials.DIAMOND, TRICERATOPS_ARMOR_DIAMOND_KEY, TRICERATOPS_ARMOR_WEARERS);
        STEGOSAURUS_ARMOR_LEATHER = armor(ArmorMaterials.LEATHER, STEGOSAURUS_ARMOR_LEATHER_KEY, STEGOSAURUS_ARMOR_WEARERS);
        STEGOSAURUS_ARMOR_IRON = armor(ArmorMaterials.IRON, STEGOSAURUS_ARMOR_IRON_KEY, STEGOSAURUS_ARMOR_WEARERS);
        STEGOSAURUS_ARMOR_DIAMOND = armor(ArmorMaterials.DIAMOND, STEGOSAURUS_ARMOR_DIAMOND_KEY, STEGOSAURUS_ARMOR_WEARERS);
        PARASAUROLOPHUS_ARMOR_LEATHER = armor(ArmorMaterials.LEATHER, PARASAUROLOPHUS_ARMOR_LEATHER_KEY, PARASAUROLOPHUS_ARMOR_WEARERS);
        PARASAUROLOPHUS_ARMOR_IRON = armor(ArmorMaterials.IRON, PARASAUROLOPHUS_ARMOR_IRON_KEY, PARASAUROLOPHUS_ARMOR_WEARERS);
        PARASAUROLOPHUS_ARMOR_DIAMOND = armor(ArmorMaterials.DIAMOND, PARASAUROLOPHUS_ARMOR_DIAMOND_KEY, PARASAUROLOPHUS_ARMOR_WEARERS);
        UTAHRAPTOR_ARMOR_LEATHER = armor(ArmorMaterials.LEATHER, UTAHRAPTOR_ARMOR_LEATHER_KEY, UTAHRAPTOR_ARMOR_WEARERS);
        UTAHRAPTOR_ARMOR_IRON = armor(ArmorMaterials.IRON, UTAHRAPTOR_ARMOR_IRON_KEY, UTAHRAPTOR_ARMOR_WEARERS);
        UTAHRAPTOR_ARMOR_DIAMOND = armor(ArmorMaterials.DIAMOND, UTAHRAPTOR_ARMOR_DIAMOND_KEY, UTAHRAPTOR_ARMOR_WEARERS);
        RAPTOR_FEATHER = new Item(new Item.Properties().setId(RAPTOR_FEATHER_KEY));
        // enchantable like an iron tool (14); Unbreaking and Mending come from the item tag minecraft:enchantable/durability
        DINO_PLOW = new DinoPlowItem(new Item.Properties().durability(PLOW_DURABILITY).repairable(net.minecraft.world.item.Items.IRON_INGOT)
                .enchantable(14).setId(DINO_PLOW_KEY));
    }

    /** Body armor with the Vanilla horse-armor values of the material (leather 3, iron 5, diamond 11), worn by the kinds in {@code wearers}. */
    private static Item armor(ArmorMaterial material, ResourceKey<Item> key, TagKey<EntityType<?>> wearers) {
        var entities = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ENTITY_TYPE);
        return new Item(new Item.Properties().attributes(material.createAttributes(ArmorType.BODY)).component(DataComponents.EQUIPPABLE,
                Equippable.builder(EquipmentSlot.BODY).setEquipSound(SoundEvents.HORSE_ARMOR)
                        .setAllowedEntities(entities.getOrThrow(wearers)).setDamageOnHurt(false).setCanBeSheared(true)
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

    public static AttributeSupplier.Builder triceratopsAttributes() {
        return Triceratops.createAttributes();
    }

    public static AttributeSupplier.Builder stegosaurusAttributes() {
        return Stegosaurus.createAttributes();
    }

    public static AttributeSupplier.Builder parasaurolophusAttributes() {
        return Parasaurolophus.createAttributes();
    }

    public static AttributeSupplier.Builder utahraptorAttributes() {
        return Utahraptor.createAttributes();
    }

    public static Item eggshell(DinoKind kind) {
        return EGGSHELLS.get(kind);
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
        sink.accept(TRICERATOPS_ARMOR_LEATHER_KEY, TRICERATOPS_ARMOR_LEATHER);
        sink.accept(TRICERATOPS_ARMOR_IRON_KEY, TRICERATOPS_ARMOR_IRON);
        sink.accept(TRICERATOPS_ARMOR_DIAMOND_KEY, TRICERATOPS_ARMOR_DIAMOND);
        sink.accept(STEGOSAURUS_ARMOR_LEATHER_KEY, STEGOSAURUS_ARMOR_LEATHER);
        sink.accept(STEGOSAURUS_ARMOR_IRON_KEY, STEGOSAURUS_ARMOR_IRON);
        sink.accept(STEGOSAURUS_ARMOR_DIAMOND_KEY, STEGOSAURUS_ARMOR_DIAMOND);
        sink.accept(DINO_PLOW_KEY, DINO_PLOW);
        sink.accept(PARASAUROLOPHUS_ARMOR_LEATHER_KEY, PARASAUROLOPHUS_ARMOR_LEATHER);
        sink.accept(PARASAUROLOPHUS_ARMOR_IRON_KEY, PARASAUROLOPHUS_ARMOR_IRON);
        sink.accept(PARASAUROLOPHUS_ARMOR_DIAMOND_KEY, PARASAUROLOPHUS_ARMOR_DIAMOND);
        sink.accept(UTAHRAPTOR_ARMOR_LEATHER_KEY, UTAHRAPTOR_ARMOR_LEATHER);
        sink.accept(UTAHRAPTOR_ARMOR_IRON_KEY, UTAHRAPTOR_ARMOR_IRON);
        sink.accept(UTAHRAPTOR_ARMOR_DIAMOND_KEY, UTAHRAPTOR_ARMOR_DIAMOND);
        sink.accept(RAPTOR_FEATHER_KEY, RAPTOR_FEATHER);
        EGGSHELLS.forEach((kind, item) -> sink.accept(itemKey(kind.id + "_eggshell"), item));
        com.simpledinos.guide.DinosGuide.register();
    }

    public static void entityTypes(BiConsumer<ResourceKey<EntityType<?>>, EntityType<?>> sink) {
        sink.accept(TREX_KEY, TREX);
        sink.accept(TRICERATOPS_KEY, TRICERATOPS);
        sink.accept(STEGOSAURUS_KEY, STEGOSAURUS);
        sink.accept(PARASAUROLOPHUS_KEY, PARASAUROLOPHUS);
        sink.accept(UTAHRAPTOR_KEY, UTAHRAPTOR);
    }

    public static void dataComponents(BiConsumer<ResourceKey<net.minecraft.core.component.DataComponentType<?>>, net.minecraft.core.component.DataComponentType<?>> sink) {
        sink.accept(FOSSIL_SORT_KEY, FOSSIL_SORT);
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
        dataComponents((k, v) -> Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, k, v));
        blocks((k, v) -> Registry.register(BuiltInRegistries.BLOCK, k, v));
        items((k, v) -> Registry.register(BuiltInRegistries.ITEM, k, v));
        entityTypes((k, v) -> Registry.register(BuiltInRegistries.ENTITY_TYPE, k, v));
        DinoSounds.registerAll();
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
        stacks.add(new ItemStack(TRICERATOPS_ARMOR_LEATHER));
        stacks.add(new ItemStack(TRICERATOPS_ARMOR_IRON));
        stacks.add(new ItemStack(TRICERATOPS_ARMOR_DIAMOND));
        stacks.add(new ItemStack(STEGOSAURUS_ARMOR_LEATHER));
        stacks.add(new ItemStack(STEGOSAURUS_ARMOR_IRON));
        stacks.add(new ItemStack(STEGOSAURUS_ARMOR_DIAMOND));
        stacks.add(new ItemStack(DINO_PLOW));
        stacks.add(new ItemStack(PARASAUROLOPHUS_ARMOR_LEATHER));
        stacks.add(new ItemStack(PARASAUROLOPHUS_ARMOR_IRON));
        stacks.add(new ItemStack(PARASAUROLOPHUS_ARMOR_DIAMOND));
        stacks.add(new ItemStack(UTAHRAPTOR_ARMOR_LEATHER));
        stacks.add(new ItemStack(UTAHRAPTOR_ARMOR_IRON));
        stacks.add(new ItemStack(UTAHRAPTOR_ARMOR_DIAMOND));
        stacks.add(new ItemStack(RAPTOR_FEATHER));
        for (Item shell : EGGSHELLS.values()) stacks.add(new ItemStack(shell));
        return stacks;
    }

    private DinosRegistry() {}
}
