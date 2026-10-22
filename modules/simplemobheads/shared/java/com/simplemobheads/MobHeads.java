package com.simplemobheads;

import com.mojang.serialization.Codec;
import com.simplemobheads.data.HeadDef;
import com.simplemobheads.data.HeadTable;
import com.simplemobheads.drop.CopyVariantFunction;
import com.simplemobheads.drop.HeadDropCondition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.MobVisibility;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.waypoints.Waypoint;
import org.jspecify.annotations.Nullable;

/**
 * Registration of every head of {@link HeadTable}: per head a standing and a wall block (Vanilla {@code SkullBlock} /
 * {@code WallSkullBlock} with Vanilla's {@code SkullBlockEntity}), an item, a skull type per look. Always registered,
 * whether or not the mob's mod is there (worlds never lose a block). Loaders call the steps from their registry events.
 */
public final class MobHeads {
    /** The variant of a head ({@code siamese}); kept by the placed head (block entity components) and its drop. */
    public static final DataComponentType<String> VARIANT = DataComponentType.<String>builder()
            .persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build();

    /** Silverfish head: the wearer shrinks to half size (attribute {@code minecraft:scale}, moved in from SimpleBuilding). */
    public static final double SILVERFISH_SCALE = -0.5;
    /** Slime head: one block more fall without damage (Vanilla 3, with the head 4). */
    public static final double SLIME_SAFE_FALL = 1.0;
    /** Behind this Vanilla item the heads sit in Functional Blocks and the search tab (the last Vanilla head). */
    public static final Item HEAD_ANCHOR = Items.DRAGON_HEAD;
    public static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, SimpleMobHeads.id("heads"));

    private static final Map<String, HeadDef> HEADS = new LinkedHashMap<>();
    private static final Map<String, MobHeadType> TYPES = new LinkedHashMap<>();
    private static final Map<String, Block> STANDING = new LinkedHashMap<>();
    private static final Map<String, Block> WALL = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

    static {
        for (HeadDef head : HeadTable.HEADS) {
            HEADS.put(head.id(), head);
            for (HeadDef.Look look : head.looks()) {
                TYPES.put(look.kind(), new MobHeadType(head, look));
            }
        }
    }

    private MobHeads() {
    }

    public static List<HeadDef> heads() {
        return List.copyOf(HEADS.values());
    }

    public static @Nullable HeadDef head(String id) {
        return HEADS.get(id);
    }

    /** The skull type of a look ({@code simplemobheads:cat}, {@code simplemobheads:cat/siamese}). */
    public static @Nullable MobHeadType type(String kind) {
        return TYPES.get(kind);
    }

    public static List<MobHeadType> types() {
        return List.copyOf(TYPES.values());
    }

    /** The skull type a head shows with {@code variant} (its base type for none or an unknown one). */
    public static MobHeadType type(HeadDef head, @Nullable String variant) {
        return TYPES.get(head.look(variant).kind());
    }

    public static @Nullable Item item(String headId) {
        return ITEMS.get(headId);
    }

    public static @Nullable Block standing(String headId) {
        return STANDING.get(headId);
    }

    public static @Nullable Block wall(String headId) {
        return WALL.get(headId);
    }

    public static List<Item> items() {
        return List.copyOf(ITEMS.values());
    }

    public static List<Block> blocks() {
        List<Block> out = new ArrayList<>(STANDING.values());
        out.addAll(WALL.values());
        return out;
    }

    /** The head of a head item, or null. */
    public static @Nullable HeadDef of(ItemStack stack) {
        return stack.getItem() instanceof MobHeadItem item ? item.head() : null;
    }

    /** A head stack showing {@code variant} (no component for the base look). */
    public static ItemStack stack(HeadDef head, @Nullable String variant) {
        ItemStack stack = new ItemStack(ITEMS.get(head.id()));
        if (variant != null && !variant.isEmpty() && !variant.equals(head.firstVariant()) && head.variantIds().contains(variant)) {
            stack.set(VARIANT, variant);
        }
        return stack;
    }

    // --- registration steps (Fabric: in order at init; NeoForge/Forge: from their registry events) ---------------

    public static void components() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleMobHeads.id("variant"), VARIANT);
    }

    public static void lootTypes(boolean functions, boolean conditions) {
        if (functions) {
            Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, SimpleMobHeads.id("copy_variant"), CopyVariantFunction.CODEC);
        }
        if (conditions) {
            Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, SimpleMobHeads.id("head_drop"), HeadDropCondition.CODEC);
        }
    }

    public static void registerBlocks() {
        for (HeadDef head : HEADS.values()) {
            MobHeadType type = TYPES.get(head.looks().get(0).kind());
            ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleMobHeads.id(head.item()));
            BlockBehaviour.Properties standingProperties = BlockBehaviour.Properties.of().setId(key)
                    .instrument(NoteBlockInstrument.CUSTOM_HEAD).strength(1.0F).pushReaction(PushReaction.POPPED).noOcclusion();
            Block standing = Registry.register(BuiltInRegistries.BLOCK, key,
                    head.big() ? new BigSkullBlock(type, standingProperties) : new SkullBlock(type, standingProperties));
            ResourceKey<Block> wallKey = ResourceKey.create(Registries.BLOCK, SimpleMobHeads.id(head.wall()));
            BlockBehaviour.Properties wallProperties = BlockBehaviour.Properties.of().setId(wallKey)
                    .overrideLootTable(standing.getLootTable()).overrideDescription(standing.getDescriptionId())
                    .strength(1.0F).pushReaction(PushReaction.POPPED);
            if (head.big()) {
                wallProperties.noOcclusion();
            }
            Block wall = Registry.register(BuiltInRegistries.BLOCK, wallKey,
                    head.big() ? new BigWallSkullBlock(type, wallProperties) : new WallSkullBlock(type, wallProperties));
            STANDING.put(head.id(), standing);
            WALL.put(head.id(), wall);
        }
    }

    public static void registerItems() {
        com.simplemobheads.guide.MobHeadsGuide.register();
        for (HeadDef head : HEADS.values()) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleMobHeads.id(head.item()));
            Item.Properties properties = new Item.Properties().setId(key).useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON)
                    .equippableUnswappable(EquipmentSlot.HEAD)
                    .component(DataComponents.ATTRIBUTE_MODIFIERS, modifiers(head))
                    .component(DataComponents.NOTE_BLOCK_SOUND, Identifier.parse(head.sounds().get(0)));
            List<String> mobs = new ArrayList<>(List.of(head.mob()));
            mobs.addAll(head.camouflage());
            // Camouflage (concept §3.5.1): the head's own mob (the giant head: every zombie kind, EH21) sees the wearer only half
            // as far - Vanilla's component, as on the zombie head. A mob of a missing mod is left out (none: no effect).
            properties.delayedComponent(DataComponents.MOB_VISIBILITY, context -> {
                HolderGetter<EntityType<?>> types = context.lookupOrThrow(Registries.ENTITY_TYPE);
                List<net.minecraft.core.Holder<EntityType<?>>> seen = new ArrayList<>();
                for (String mob : mobs) {
                    types.get(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.parse(mob))).ifPresent(seen::add);
                }
                return new MobVisibility(seen.isEmpty() ? HolderSet.empty() : HolderSet.direct(seen), 0.5F);
            });
            if (head.drop().equals("tribute")) {
                // N-A: the tribute's motto as a grey lore line; the item itself only names the crowned pig head.
                properties.component(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
                        net.minecraft.network.chat.Component.translatable("lore.simplemobheads.crowned_pig_head")
                                .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC))));
            }
            ITEMS.put(head.id(), Registry.register(BuiltInRegistries.ITEM, key,
                    new MobHeadItem(head, STANDING.get(head.id()), WALL.get(head.id()), properties)));
        }
    }

    /**
     * Like Vanilla's heads the wearer is hidden from the locator bar; the silverfish and slime heads carry their secret as a
     * hidden attribute modifier (moved in from SimpleBuilding unchanged).
     */
    private static ItemAttributeModifiers modifiers(HeadDef head) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder().add(Attributes.WAYPOINT_TRANSMIT_RANGE,
                Waypoint.WAYPOINT_TRANSMIT_RANGE_HIDE_MODIFIER, EquipmentSlotGroup.HEAD, ItemAttributeModifiers.Display.hidden());
        if (head.ability().equals("silverfish")) {
            builder.add(Attributes.SCALE, new AttributeModifier(SimpleMobHeads.id("silverfish_head_scale"), SILVERFISH_SCALE,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.HEAD, ItemAttributeModifiers.Display.hidden());
        } else if (head.ability().equals("slime")) {
            builder.add(Attributes.SAFE_FALL_DISTANCE, new AttributeModifier(SimpleMobHeads.id("slime_head_safe_fall"), SLIME_SAFE_FALL,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD, ItemAttributeModifiers.Display.hidden());
        }
        return builder.build();
    }

    /**
     * A giant head (EH21 G-A, owner "bigger"): drawn 1.5 blocks big, so the block to click and to stand on is a full block (a
     * Vanilla skull's 8 px box would sit lost inside it).
     */
    public static final VoxelShape BIG_SHAPE = Block.column(16.0, 0.0, 16.0);

    static final class BigSkullBlock extends SkullBlock {
        BigSkullBlock(SkullBlock.Type type, BlockBehaviour.Properties properties) {
            super(type, properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return BIG_SHAPE;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return BIG_SHAPE;
        }
    }

    static final class BigWallSkullBlock extends WallSkullBlock {
        BigWallSkullBlock(SkullBlock.Type type, BlockBehaviour.Properties properties) {
            super(type, properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return BIG_SHAPE;
        }
    }

    /** Base heads in table order (Functional Blocks / search tab, behind the dragon head). */
    public static List<ItemStack> baseStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (HeadDef head : HEADS.values()) {
            out.add(new ItemStack(ITEMS.get(head.id())));
        }
        return List.copyOf(out);
    }

    /** The own tab: the guide, then every head, each followed by its further variants. */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (com.simplemobheads.guide.MobHeadsGuide.book() != null) {
            out.add(com.simplemobheads.guide.MobHeadsGuide.stack());
        }
        for (HeadDef head : HEADS.values()) {
            out.add(new ItemStack(ITEMS.get(head.id())));
            for (String variant : head.variantIds()) {
                if (!variant.equals(head.firstVariant())) {
                    out.add(stack(head, variant));
                }
            }
        }
        return List.copyOf(out);
    }

    public static void registerTab() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB,
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(net.minecraft.network.chat.Component.translatable("itemgroup.simplemobheads.heads"))
                        .icon(() -> new ItemStack(ITEMS.get("cat")))
                        .displayItems((parameters, output) -> tabStacks().forEach(output::accept))
                        .build());
    }
}
