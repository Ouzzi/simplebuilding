package com.simplebuilding.crucible;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplelib.api.SimpleLibApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * 26.3: SimpleBuilding's crucible parts on top of SimpleLib (plan PLAN-CRUCIBLE-P5-2026-10-05). The
 * only place in SimpleBuilding that imports SimpleLib - and only its API (principle 6a). The 26.2
 * twin in common/src/mc26_2/java is empty ({@code McVersion.CRUCIBLE} is false there).
 * <ul>
 *   <li>Enderite crucible (27 slots, 8x, double stacks) and Enderite barrel, registered under
 *       {@code simplebuilding:} through the SimpleLib factory (SimpleLib's block entity, menu, renderer).</li>
 *   <li>Sledgehammer ways (principle 5a): building the iron crucible (2 durability per strike), attaching a
 *       barrel (6 strikes); upgrades run through the sledgehammer upgrade table
 *       ({@code SledgehammerUpgrades}). The Vanilla axe ways are switched off.</li>
 *   <li>Reinforced cauldron: soul lava as its extreme content, copper/enderite buckets.</li>
 * </ul>
 */
public final class CrucibleCompat {
    public static final int HAMMER_DAMAGE_PER_STRIKE = 2;

    private static @Nullable Block enderiteCrucible;
    private static @Nullable Block enderiteBarrel;

    public static void registerBlocks() {
        enderiteCrucible = register("enderite_crucible", SimpleLibApi.newCrucible(
                SimpleLibApi.crucibleProperties(MapColor.COLOR_PURPLE, 6.0F, 1500.0F, SoundType.NETHERITE_BLOCK).setId(key("enderite_crucible")),
                "enderite"));
        enderiteBarrel = register("enderite_barrel", SimpleLibApi.newBarrel(
                net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(6.0F, 1500.0F)
                        .sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().setId(key("enderite_barrel")),
                "enderite"));
    }

    public static void registerItems() {
        item("enderite_crucible", enderiteCrucible);
        item("enderite_barrel", enderiteBarrel);
    }

    /** After registration: switch the axe ways off and plug the sledgehammer and the buckets into SimpleLib. */
    public static void init() {
        SimpleLibApi.disableAxeWays();
        SimpleLibApi.registerToolUse(CrucibleCompat::hammerWants);
        SimpleLibApi.registerCauldronBucket(new SbCauldronBuckets());
        SimpleLibApi.setExtremeCauldronEffect(CrucibleCompat::soulLavaInside);
    }

    private static void soulLavaInside(Level level, Entity entity) {
        com.simplebuilding.fluid.SoulLava.touch(level, entity);
    }

    public static @Nullable Block enderiteCrucible() {
        return enderiteCrucible;
    }

    public static @Nullable Block enderiteBarrel() {
        return enderiteBarrel;
    }

    public static @Nullable Block reinforcedCauldron() {
        return SimpleLibApi.reinforcedCauldron();
    }

    public static BlockState reinforcedCauldron(String content) {
        return SimpleLibApi.reinforcedCauldron(content);
    }

    public static @Nullable String cauldronContent(BlockState state) {
        return SimpleLibApi.cauldronContent(state);
    }

    public static String heatAt(Level level, BlockPos crucible) {
        return SimpleLibApi.heatAt(level, crucible);
    }

    public static int crucibleSlots(Block block) {
        return SimpleLibApi.crucibleSlots(block);
    }

    public static int[] status(net.minecraft.world.level.block.entity.BlockEntity entity) {
        return SimpleLibApi.crucibleStatus(entity);
    }

    public static int requiredHeat(ItemStack input, net.minecraft.world.item.crafting.RecipeType<?> type) {
        return SimpleLibApi.requiredHeat(input, type);
    }

    public static int cookingTicks(int baseTicks, int heat) {
        return SimpleLibApi.crucibleCookingTicks(baseTicks, heat);
    }

    public static int warmingTicks() { return SimpleLibApi.warmingTicks(); }

    public static boolean warmable(ItemStack input) { return SimpleLibApi.warmable(input); }

    public static int stackMultiplier(Block block) {
        return SimpleLibApi.stackMultiplier(block);
    }

    public static boolean isAttached(BlockState state) {
        return SimpleLibApi.isAttached(state);
    }

    public static boolean upgradesInPlace(Block block) {
        return SimpleLibApi.upgradesInPlace(block);
    }

    public static void upgradeInPlace(ServerLevel level, BlockPos pos, Block to) {
        SimpleLibApi.upgradeCrucible(level, pos, to);
    }

    public static boolean axeWaysEnabled() {
        return SimpleLibApi.axeWaysEnabled();
    }

    /** A sledgehammer click on a crucible/barrel reaches the hammer instead of the menu when it upgrades, builds or attaches. */
    private static boolean hammerWants(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !(player.getMainHandItem().getItem() instanceof SledgehammerItem)) return false;
        if (com.simplebuilding.util.SledgehammerUpgrades.shouldSkipBlockUse(state, level, pos, player, hand)) return true;
        return SimpleLibApi.canAttach(level, pos, state);
    }

    /**
     * The sledgehammer's in-world crucible ways: build the iron crucible on an iron block (off hand:
     * walls, then handles) and attach a barrel to a crucible. Null when neither applies.
     */
    public static @Nullable InteractionResult hammerUse(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND) return null;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ItemStack tool = context.getItemInHand();
        if (SimpleLibApi.canAttach(level, pos, state)) {
            return SimpleLibApi.attachStrike(level, pos, player, tool, HAMMER_DAMAGE_PER_STRIKE) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        if (SimpleLibApi.canBuildStrike(state, player.getOffhandItem())) {
            if (!com.simplebuilding.api.WorldPermissions.mayAct(level, player, pos)) return InteractionResult.FAIL;
            boolean hit = SimpleLibApi.buildStrike(level, pos, player, tool, HAMMER_DAMAGE_PER_STRIKE);
            if (hit && player instanceof net.minecraft.server.level.ServerPlayer server
                    && level.getBlockState(pos).getBlock() == BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("simplelib", "iron_crucible"))) {
                com.simplebuilding.advancement.ModTriggers.feature(server, com.simplebuilding.advancement.ModTriggers.CRUCIBLE_BUILT);
            }
            return hit ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return null;
    }

    /**
     * SimpleBuilding's containers for the reinforced cauldron: soul lava is its "extreme" content (iron bucket
     * breaks when pouring it, the Enderite bucket comes back), copper and Enderite buckets take and pour water
     * and lava like everywhere else (copper breaks on lava, cannot take soul lava).
     */
    private static final class SbCauldronBuckets implements SimpleLibApi.CauldronBucket {
        @Override
        public @Nullable String pours(ItemStack held) {
            if (!(held.getItem() instanceof com.simplebuilding.fluid.ModBucketItem bucket)) return null;
            net.minecraft.world.level.material.Fluid content = bucket.getContent();
            if (content.isSame(com.simplebuilding.fluid.ModFluids.SOUL_LAVA)) return "extreme";
            if (content.isSame(net.minecraft.world.level.material.Fluids.LAVA)) return "lava";
            if (content.isSame(net.minecraft.world.level.material.Fluids.WATER)) return "water";
            return null;
        }

        @Override
        public ItemStack afterPour(ItemStack held) {
            return held.getItem() instanceof com.simplebuilding.fluid.ModBucketItem bucket ? bucket.afterPour(held) : ItemStack.EMPTY;
        }

        @Override
        public @Nullable ItemStack take(ItemStack held, String content) {
            com.simplebuilding.fluid.ModBucketItem.Kind kind;
            if (held.is(net.minecraft.world.item.Items.BUCKET)) kind = com.simplebuilding.fluid.ModBucketItem.Kind.IRON;
            else if (held.getItem() instanceof com.simplebuilding.fluid.ModBucketItem bucket
                    && bucket.getContent() == net.minecraft.world.level.material.Fluids.EMPTY
                    && com.simplebuilding.fluid.ModBucketItem.canScoop(held)) kind = bucket.kind();
            else return null;
            net.minecraft.world.level.material.Fluid fluid = switch (content) {
                case "extreme" -> com.simplebuilding.fluid.ModFluids.SOUL_LAVA;
                case "lava" -> kind == com.simplebuilding.fluid.ModBucketItem.Kind.IRON ? null : net.minecraft.world.level.material.Fluids.LAVA;
                case "water" -> kind == com.simplebuilding.fluid.ModBucketItem.Kind.IRON ? null : net.minecraft.world.level.material.Fluids.WATER;
                default -> null;
            };
            if (fluid == null) return null;
            Item filled = com.simplebuilding.fluid.ModBucketItem.filled(kind, fluid);
            return filled == null ? null : com.simplebuilding.fluid.ModBucketItem.fill(held, filled);
        }
    }

    private static Block register(String name, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK, key(name), block);
    }

    private static void item(String name, @Nullable Block block) {
        if (block == null) return;
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name));
        Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, new Item.Properties().fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC).setId(key).useBlockDescriptionPrefix()));
    }

    private static ResourceKey<Block> key(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name));
    }

    private CrucibleCompat() {}
}
