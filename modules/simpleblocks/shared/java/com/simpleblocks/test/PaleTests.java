package com.simpleblocks.test;

import com.simpleblocks.BlocksRegistry;
import com.simpleblocks.pale.PaleBlocks;
import com.simpleblocks.pale.PaleConfig;
import com.simplelib.api.aging.Aging;
import com.simplelib.api.aging.AgingChain;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HayBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * DW04B (b-pale01, docs/ai/PLAN-B-PALE01-2026-11-04.md): the Pale Garden chains on SimpleLib's aging API. Random ticks are
 * called directly with the chance set to 1 (0 for "never"); the Pale Garden is put under one block with Vanilla's
 * {@code /fillbiome} logic and restored afterwards. All on the module's empty 8 x 8 x 8 structure, the aging block in the
 * middle so no block of another test is within the 4-block neighbour scan.
 */
public final class PaleTests {
    private static final BlockPos MID = new BlockPos(4, 2, 4);

    private PaleTests() {}

    public static void register(Map<String, Consumer<GameTestHelper>> all, Map<String, Integer> maxTicks) {
        all.put("pale_ages_only_in_pale_garden", PaleTests::agesOnlyInPaleGarden);
        all.put("pale_wax_stops_axe_scrapes", PaleTests::waxStopsAxeScrapes);
        all.put("pale_chance_zero_never", PaleTests::chanceZeroNever);
        all.put("pale_dispenser_waxes", PaleTests::dispenserWaxes);
        maxTicks.put("pale_dispenser_waxes", 60);
        all.put("pale_shears_keep_stage", PaleTests::shearsKeepStage);
        all.put("pale_blocks_like_vanilla", PaleTests::likeVanilla);
        all.put("pale_golem_from_pale_head", PaleTests::golemFromPaleHead);
        maxTicks.put("pale_golem_from_pale_head", 40);
    }

    /** A Vanilla pumpkin fades through faded, bleached and pale only inside the Pale Garden; the end stage stops ticking. */
    public static void agesOnlyInPaleGarden(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos outside = new BlockPos(0, 2, 0);
        helper.setBlock(MID, Blocks.PUMPKIN);
        helper.setBlock(outside, Blocks.PUMPKIN);
        helper.assertTrue(Blocks.PUMPKIN.defaultBlockState().isRandomlyTicking(), "the Vanilla pumpkin random-ticks (aging chain base)");
        Holder<Biome> before = paleGarden(helper, MID);
        boolean outsidePale = level.getBiome(helper.absolutePos(outside)).is(PaleBlocks.PALES_BLOCKS);
        try {
            withChance("pumpkin", 1, () -> {
                for (int i = 0; i < 4; i++) {
                    tick(helper, MID);
                    tick(helper, outside);
                }
            });
        } finally {
            restore(helper, MID, before);
        }
        helper.assertTrue(helper.getBlockState(MID).is(PaleBlocks.PUMPKIN.stages().get(3)), "in the Pale Garden: pale pumpkin after 3 ticks, is " + helper.getBlockState(MID));
        helper.assertTrue(!helper.getBlockState(MID).isRandomlyTicking(), "the end stage does not tick");
        helper.assertTrue(outsidePale || helper.getBlockState(outside).is(Blocks.PUMPKIN), "outside the Pale Garden it stays a pumpkin, is " + helper.getBlockState(outside));
        helper.succeed();
    }

    /** Honeycomb waxes (no aging any more), an axe takes the wax off, a second axe use scrapes one stage back. */
    public static void waxStopsAxeScrapes(GameTestHelper helper) {
        AgingChain melon = PaleBlocks.MELON;
        helper.setBlock(MID, melon.stages().get(1));
        ServerPlayer player = survivor(helper);
        ItemStack comb = new ItemStack(Items.HONEYCOMB, 2);
        use(helper, player, comb, MID);
        helper.assertTrue(helper.getBlockState(MID).is(melon.waxed().get(1)), "honeycomb waxed the faded melon, is " + helper.getBlockState(MID));
        helper.assertValueEqual(comb.getCount(), 1, "one honeycomb used");
        Holder<Biome> before = paleGarden(helper, MID);
        try {
            withChance("melon", 1, () -> {
                for (int i = 0; i < 3; i++) tick(helper, MID);
            });
        } finally {
            restore(helper, MID, before);
        }
        helper.assertTrue(helper.getBlockState(MID).is(melon.waxed().get(1)), "waxed: no aging in the Pale Garden, is " + helper.getBlockState(MID));
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        use(helper, player, axe, MID);
        helper.assertTrue(helper.getBlockState(MID).is(melon.stages().get(1)), "axe took the wax off, is " + helper.getBlockState(MID));
        use(helper, player, axe, MID);
        helper.assertTrue(helper.getBlockState(MID).is(Blocks.MELON), "axe scraped one stage back to the Vanilla melon, is " + helper.getBlockState(MID));
        helper.assertValueEqual(axe.getDamageValue(), 2, "the axe lost 2 durability");
        use(helper, player, axe, MID);
        helper.assertTrue(helper.getBlockState(MID).is(Blocks.MELON), "the axe does nothing to a plain melon");
        helper.succeed();
    }

    /** Chance 0 (and the switch off) means never, even inside the Pale Garden. */
    public static void chanceZeroNever(GameTestHelper helper) {
        helper.setBlock(MID, Blocks.HAY_BLOCK);
        Holder<Biome> before = paleGarden(helper, MID);
        boolean on = PaleConfig.enabled;
        try {
            withChance("hayBlock", 0, () -> {
                for (int i = 0; i < 20; i++) tick(helper, MID);
            });
            helper.assertTrue(helper.getBlockState(MID).is(Blocks.HAY_BLOCK), "chance 0: the hay bale stays, is " + helper.getBlockState(MID));
            PaleConfig.enabled = false;
            withChance("hayBlock", 1, () -> {
                for (int i = 0; i < 5; i++) tick(helper, MID);
            });
            helper.assertTrue(helper.getBlockState(MID).is(Blocks.HAY_BLOCK), "switch off: the hay bale stays, is " + helper.getBlockState(MID));
            PaleConfig.enabled = true;
            withChance("hayBlock", 1, () -> tick(helper, MID));
            helper.assertTrue(helper.getBlockState(MID).is(PaleBlocks.HAY_BLOCK.stages().get(1)), "chance 1: faded hay bale, is " + helper.getBlockState(MID));
        } finally {
            PaleConfig.enabled = on;
            restore(helper, MID, before);
        }
        helper.succeed();
    }

    /** A powered dispenser with honeycomb waxes the faded pumpkin in front of it (like copper). */
    public static void dispenserWaxes(GameTestHelper helper) {
        BlockPos dispenser = new BlockPos(2, 2, 2), front = new BlockPos(3, 2, 2);
        helper.setBlock(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        helper.setBlock(front, PaleBlocks.PUMPKIN.stages().get(1));
        if (helper.getLevel().getBlockEntity(helper.absolutePos(dispenser)) instanceof DispenserBlockEntity be) be.setItem(0, new ItemStack(Items.HONEYCOMB));
        helper.setBlock(new BlockPos(1, 2, 2), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> helper.assertTrue(helper.getBlockState(front).is(PaleBlocks.PUMPKIN.waxed().get(1)),
                "dispenser waxed the faded pumpkin, is " + helper.getBlockState(front)));
    }

    /** Shears carve a faded pumpkin into a faded carved pumpkin, a waxed pumpkin into a waxed carved pumpkin. */
    public static void shearsKeepStage(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        BlockPos a = new BlockPos(2, 2, 2), b = new BlockPos(5, 2, 5);
        helper.setBlock(a, PaleBlocks.PUMPKIN.stages().get(1));
        helper.setBlock(b, PaleBlocks.PUMPKIN.waxed().get(0));
        use(helper, player, new ItemStack(Items.SHEARS), a);
        use(helper, player, new ItemStack(Items.SHEARS), b);
        helper.assertTrue(helper.getBlockState(a).is(PaleBlocks.CARVED_PUMPKIN.stages().get(1)), "faded pumpkin -> faded carved pumpkin, is " + helper.getBlockState(a));
        helper.assertTrue(helper.getBlockState(b).is(PaleBlocks.CARVED_PUMPKIN.waxed().get(0)), "waxed pumpkin -> waxed carved pumpkin, is " + helper.getBlockState(b));
        helper.assertTrue(!helper.getEntities(EntityTypes.ITEM).isEmpty(), "carving dropped pumpkin seeds");
        helper.succeed();
    }

    /** Same class, tags, item components and loot as the Vanilla block; all 30 in the tab; the Pale Garden is in the biome tag. */
    public static void likeVanilla(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int count = 0;
        for (AgingChain chain : PaleBlocks.CHAINS) {
            Block base = chain.base();
            for (Block block : chain.createdBlocks()) {
                count++;
                helper.assertTrue(base.getClass().isInstance(block), block + " is a " + base.getClass().getSimpleName());
                // Vanilla's own tags and components (other mods' tags, e.g. conventional c:, are theirs to fill)
                base.builtInRegistryHolder().tags().filter(t -> vanilla(t.location())).forEach(tag ->
                        helper.assertTrue(block.defaultBlockState().is(tag), block + " lacks block tag " + tag.location()));
                Item item = block.asItem(), baseItem = base.asItem();
                baseItem.builtInRegistryHolder().tags().filter(t -> vanilla(t.location())).forEach(tag ->
                        helper.assertTrue(item.builtInRegistryHolder().is(tag), item + " lacks item tag " + tag.location()));
                for (DataComponentType<?> type : baseItem.components().keySet()) {
                    if (vanilla(net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type))) {
                        helper.assertTrue(item.components().has(type), item + " lacks item component " + type);
                    }
                }
                helper.assertValueEqual(block.defaultBlockState().getLightEmission(), base.defaultBlockState().getLightEmission(), block + " light");
                helper.assertTrue(BlocksRegistry.tabStacks().stream().anyMatch(s -> s.is(item)), item + " in the Simple Blocks tab");
            }
        }
        helper.assertValueEqual(count, 30, "pale blocks");
        helper.assertTrue(PaleBlocks.HAY_BLOCK.stages().get(3) instanceof HayBlock && PaleBlocks.JACK_O_LANTERN.stages().get(2) instanceof CarvedPumpkinBlock,
                "hay and jack o'lantern keep their Vanilla classes");
        BlockPos pos = helper.absolutePos(MID);
        ItemStack axe = new ItemStack(Items.IRON_AXE), silk = new ItemStack(Items.IRON_AXE);
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        BlockState melon = PaleBlocks.MELON.stages().get(2).defaultBlockState();
        List<ItemStack> slices = Block.getDrops(melon, level, pos, null, null, axe);
        helper.assertTrue(slices.size() == 1 && slices.get(0).is(Items.MELON_SLICE) && slices.get(0).getCount() >= 3, "bleached melon drops melon slices: " + slices);
        List<ItemStack> self = Block.getDrops(melon, level, pos, null, null, silk);
        helper.assertTrue(self.size() == 1 && self.get(0).is(melon.getBlock().asItem()), "with Silk Touch it drops itself: " + self);
        List<ItemStack> hay = Block.getDrops(PaleBlocks.HAY_BLOCK.waxed().get(2).defaultBlockState(), level, pos, null, null, ItemStack.EMPTY);
        helper.assertTrue(hay.size() == 1 && hay.get(0).is(PaleBlocks.HAY_BLOCK.waxed().get(2).asItem()), "waxed bleached hay drops itself: " + hay);
        Holder<Biome> pale = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PALE_GARDEN);
        helper.assertTrue(pale.is(PaleBlocks.PALES_BLOCKS), "the Pale Garden is in simpleblocks:pales_blocks");
        helper.assertTrue(Aging.entry(Blocks.PUMPKIN) != null && Aging.entry(PaleBlocks.PUMPKIN.waxed().get(0)).waxed(), "chains registered in SimpleLib");
        helper.succeed();
    }

    /** Two snow blocks under a pale carved pumpkin make a snow golem, like with the Vanilla carved pumpkin. */
    public static void golemFromPaleHead(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.SNOW_BLOCK);
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.SNOW_BLOCK);
        helper.setBlock(new BlockPos(4, 3, 4), PaleBlocks.CARVED_PUMPKIN.stages().get(3));
        helper.succeedWhen(() -> {
            helper.assertEntityPresent(EntityTypes.SNOW_GOLEM);
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 3, 4)).isAir(), "the pattern was used up");
        });
    }

    // --- helpers --------------------------------------------------------------------------------------------------------

    private static boolean vanilla(net.minecraft.resources.Identifier id) {
        return id != null && "minecraft".equals(id.getNamespace());
    }

    private static void tick(GameTestHelper helper, BlockPos pos) {
        ServerLevel level = helper.getLevel();
        RandomSource random = level.getRandom();
        helper.getBlockState(pos).randomTick(level, helper.absolutePos(pos), random);
    }

    private static void withChance(String chain, double chance, Runnable body) {
        Double before = PaleConfig.CHANCE.get(chain);
        PaleConfig.CHANCE.put(chain, chance);
        try {
            body.run();
        } finally {
            PaleConfig.CHANCE.put(chain, before);
        }
    }

    /** Puts the biome cell of {@code pos} into the Pale Garden; returns the biome that was there. */
    private static Holder<Biome> paleGarden(GameTestHelper helper, BlockPos pos) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(pos);
        Holder<Biome> before = level.getBiome(abs);
        FillBiomeCommand.fill(level, abs, abs, level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PALE_GARDEN));
        helper.assertTrue(level.getBiome(abs).is(Biomes.PALE_GARDEN), "biome set to the Pale Garden");
        return before;
    }

    private static void restore(GameTestHelper helper, BlockPos pos, Holder<Biome> before) {
        BlockPos abs = helper.absolutePos(pos);
        FillBiomeCommand.fill(helper.getLevel(), abs, abs, before);
    }

    private static ServerPlayer survivor(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /** Uses the stack on the top face of {@code target} like a player (the server's full use path: block state, then item). */
    private static void use(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos target) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(target);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
    }
}
