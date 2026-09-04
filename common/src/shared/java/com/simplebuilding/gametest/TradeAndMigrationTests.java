package com.simplebuilding.gametest;

import com.simplebuilding.items.ModItems;
import com.mojang.authlib.GameProfile;
import com.simplebuilding.util.LegacySpatulaMigration;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.VillagerTradeTags;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.TradeSets;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * In-game coverage for the two mechanics that were rewritten for the 26.2 line:
 *
 * <ul>
 *   <li>villager trades are no longer registered from code but shipped as data
 *       ({@code data/simplebuilding/villager_trade/**}) and merged into the vanilla
 *       trade pools through {@code data/minecraft/tags/villager_trade/**};</li>
 *   <li>{@link LegacySpatulaMigration} rewrites the pre-rename "spatula" items into
 *       chisels, both in player inventories and for loose item entities.</li>
 * </ul>
 *
 * Everything here runs against the live server registries / a live level, so a broken
 * datapack path, a wrong tag id or a codec mismatch fails the test instead of passing
 * silently.
 */
public final class TradeAndMigrationTests {

    /** Tick budget for {@link #masonVillagerCanRollAModTrade}. */
    public static final int MASON_VILLAGER_MAX_TICKS = 200;

    /** Tick budget for {@link #legacySpatulaItemEntityIsRewrittenInPlace}. */
    public static final int LEGACY_ITEM_ENTITY_MAX_TICKS = 60;

    /** Hotbar slot the login migration case puts its legacy spatula in. */
    private static final int LOGIN_SLOT = 8;

    private static final String NAMESPACE = "simplebuilding";

    /** All 20 trade files under {@code data/simplebuilding/villager_trade/}. */
    private static final List<String> EXPECTED_TRADE_IDS = List.of(
            "simplebuilding:librarian/3/emerald_building_book",
            "simplebuilding:librarian/4/emerald_advanced_book",
            "simplebuilding:librarian/5/emerald_master_book",
            "simplebuilding:mason/2/emerald_copper_core",
            "simplebuilding:mason/2/netherite_diamond_core",
            "simplebuilding:mason/4/emerald_copper_building_wand",
            "simplebuilding:toolsmith/3/emerald_iron_chisel",
            "simplebuilding:toolsmith/3/emerald_copper_chisel",
            "simplebuilding:toolsmith/3/emerald_gold_chisel",
            "simplebuilding:toolsmith/4/emerald_diamond_sledgehammer",
            "simplebuilding:toolsmith/4/emerald_iron_sledgehammer",
            "simplebuilding:toolsmith/5/emerald_mining_pickaxe",
            "simplebuilding:wandering_trader/emerald_copper_cores",
            "simplebuilding:wandering_trader/emerald_iron_cores",
            "simplebuilding:wandering_trader/emerald_gold_core",
            "simplebuilding:wandering_trader/emerald_octant",
            "simplebuilding:wandering_trader/emerald_reinforced_bundle",
            "simplebuilding:wandering_trader/emerald_wand_book",
            "simplebuilding:wandering_trader/octant_emerald",
            "simplebuilding:wandering_trader/reinforced_bundle_emerald");

    /**
     * The {@code weighted_enchant} pool of the three toolsmith chisel trades, as
     * {@code <enchantment id>@<level>} - see
     * {@code data/simplebuilding/villager_trade/toolsmith/3/emerald_*_chisel.json}.
     */
    private static final Set<String> CHISEL_ENCHANT_POOL = Set.of(
            "simplebuilding:fast_chiseling@1",
            "simplebuilding:fast_chiseling@2");

    /** The same for {@code toolsmith/4/emerald_iron_sledgehammer}, which also has a second chance. */
    private static final Set<String> SLEDGEHAMMER_ENCHANT_POOL = Set.of(
            "simplebuilding:break_through@1",
            "simplebuilding:override@1",
            "simplebuilding:range@1",
            "minecraft:unbreaking@2",
            "minecraft:efficiency@3");

    // ------------------------------------------------------------------
    // (a) trades
    // ------------------------------------------------------------------

    /**
     * Every shipped trade JSON has to end up in the {@code villager_trade} datapack
     * registry of the running server. A file that fails to parse (wrong codec field,
     * unknown item, broken loot function) is silently dropped by the loader, so this
     * catches exactly that class of regression.
     */
    public static void allModTradesAreLoadedIntoTheDatapackRegistry(GameTestHelper helper) {
        Registry<VillagerTrade> trades = helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);

        List<String> missing = new ArrayList<>();
        for (String id : EXPECTED_TRADE_IDS) {
            if (trades.getValue(Identifier.parse(id)) == null) {
                missing.add(id);
            }
        }
        helper.assertTrue(missing.isEmpty(), "villager_trade registry is missing " + missing.size()
                + " simplebuilding entries: " + missing);

        // Guard against a stray namespace typo adding entries nobody expects.
        long ownEntries = trades.keySet().stream().filter(key -> NAMESPACE.equals(key.getNamespace())).count();
        helper.assertValueEqual((int) ownEntries, EXPECTED_TRADE_IDS.size(),
                "number of simplebuilding entries in the villager_trade registry");

        helper.succeed();
    }

    /**
     * The tag files under {@code data/minecraft/tags/villager_trade/**} have to <em>merge</em>
     * into the vanilla pools: our entries must be in there, and the vanilla entries must
     * still be in there (a missing {@code "replace": false} would wipe them).
     */
    public static void modTradesAreMergedIntoTheVanillaTradePools(GameTestHelper helper) {
        Registry<VillagerTrade> trades = helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);

        // The librarian pools are the only ones Trade Rebalance replaces wholesale.
        if (!tradeRebalanceActive(helper)) {
            assertPoolContains(helper, trades, VillagerTradeTags.LIBRARIAN_LEVEL_3,
                    "simplebuilding:librarian/3/emerald_building_book");
            assertPoolContains(helper, trades, VillagerTradeTags.LIBRARIAN_LEVEL_4,
                    "simplebuilding:librarian/4/emerald_advanced_book");
            assertPoolContains(helper, trades, VillagerTradeTags.LIBRARIAN_LEVEL_5,
                    "simplebuilding:librarian/5/emerald_master_book");
        }
        assertPoolContains(helper, trades, VillagerTradeTags.MASON_LEVEL_2,
                "simplebuilding:mason/2/emerald_copper_core",
                "simplebuilding:mason/2/netherite_diamond_core");
        assertPoolContains(helper, trades, VillagerTradeTags.MASON_LEVEL_4,
                "simplebuilding:mason/4/emerald_copper_building_wand");
        assertPoolContains(helper, trades, VillagerTradeTags.TOOLSMITH_LEVEL_3,
                "simplebuilding:toolsmith/3/emerald_iron_chisel",
                "simplebuilding:toolsmith/3/emerald_copper_chisel",
                "simplebuilding:toolsmith/3/emerald_gold_chisel");
        assertPoolContains(helper, trades, VillagerTradeTags.TOOLSMITH_LEVEL_4,
                "simplebuilding:toolsmith/4/emerald_diamond_sledgehammer",
                "simplebuilding:toolsmith/4/emerald_iron_sledgehammer");
        assertPoolContains(helper, trades, VillagerTradeTags.TOOLSMITH_LEVEL_5,
                "simplebuilding:toolsmith/5/emerald_mining_pickaxe");
        assertPoolContains(helper, trades, VillagerTradeTags.WANDERING_TRADER_BUYING,
                "simplebuilding:wandering_trader/reinforced_bundle_emerald",
                "simplebuilding:wandering_trader/octant_emerald");
        assertPoolContains(helper, trades, VillagerTradeTags.WANDERING_TRADER_COMMON,
                "simplebuilding:wandering_trader/emerald_copper_cores",
                "simplebuilding:wandering_trader/emerald_iron_cores");
        assertPoolContains(helper, trades, VillagerTradeTags.WANDERING_TRADER_UNCOMMON,
                "simplebuilding:wandering_trader/emerald_octant",
                "simplebuilding:wandering_trader/emerald_reinforced_bundle",
                "simplebuilding:wandering_trader/emerald_gold_core",
                "simplebuilding:wandering_trader/emerald_wand_book");

        helper.succeed();
    }

    /**
     * Walks the chain a real merchant walks: profession + level -> trade set -> tag ->
     * trade holders. If our tag ids do not match the trade set the profession points at,
     * the entries exist but are never offered by anybody.
     */
    public static void professionTradeSetsResolveTheModTrades(GameTestHelper helper) {
        assertTradeSetForProfession(helper, VillagerProfession.MASON, 2, TradeSets.MASON_LEVEL_2,
                "simplebuilding:mason/2/emerald_copper_core",
                "simplebuilding:mason/2/netherite_diamond_core");
        assertTradeSetForProfession(helper, VillagerProfession.TOOLSMITH, 3, TradeSets.TOOLSMITH_LEVEL_3,
                "simplebuilding:toolsmith/3/emerald_iron_chisel",
                "simplebuilding:toolsmith/3/emerald_copper_chisel",
                "simplebuilding:toolsmith/3/emerald_gold_chisel");
        if (!tradeRebalanceActive(helper)) {
            assertTradeSetForProfession(helper, VillagerProfession.LIBRARIAN, 5, TradeSets.LIBRARIAN_LEVEL_5,
                    "simplebuilding:librarian/5/emerald_master_book");
        }

        helper.succeed();
    }

    /**
     * Turns shipped trade definitions into actual {@link MerchantOffer}s and checks the numbers
     * that came out of the JSON: wanted item + count, optional second cost, given item + count,
     * max uses, xp - and the enchantment the offer's {@code given_item_modifiers} put on the
     * result. This is what a player would see in the trade GUI.
     *
     * <p>Every other trade test in this file only ever asks whether a trade id sits in the right
     * pool. That leaves the entire content of a trade file free to change: the price, the number
     * of uses, the experience, and above all the {@code simplebuilding:weighted_enchant} function
     * - a merchant handing out plain, unenchanted tools looks exactly like a working merchant from
     * the outside. So both trades that carry that function are built here.
     *
     * <p>The enchantment is asserted as "at least one, and every one of them out of the pool the
     * json declares, at the level it declares" rather than as one fixed pick. The function draws
     * weighted from that pool; pinning a single outcome would pin {@link #tradeContext}'s seed
     * instead of the trade.
     */
    public static void tradeDefinitionsProduceTheExpectedOffers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 1));
        LootContext context = tradeContext(helper, villager);

        // mason/2/emerald_copper_core: 25 emeralds -> 1 copper core, 2 uses, 10 xp.
        MerchantOffer core = offerOf(helper, level, "simplebuilding:mason/2/emerald_copper_core", context);
        assertOffer(helper, core, "mason/2/emerald_copper_core",
                net.minecraft.world.item.Items.EMERALD, 25, ModItems.COPPER_CORE, 1, 2, 10);
        helper.assertTrue(core.getCostB().isEmpty(),
                "mason/2/emerald_copper_core must not have a second cost item");

        // wandering_trader/octant_emerald is a buying trade: 1 octant -> 8 emeralds.
        MerchantOffer buying = offerOf(helper, level, "simplebuilding:wandering_trader/octant_emerald", context);
        assertOffer(helper, buying, "wandering_trader/octant_emerald",
                ModItems.OCTANT, 1, net.minecraft.world.item.Items.EMERALD, 8, 3, 5);

        // toolsmith/4/emerald_iron_sledgehammer uses "additional_wants" -> second cost slot.
        MerchantOffer sledgehammer =
                offerOf(helper, level, "simplebuilding:toolsmith/4/emerald_iron_sledgehammer", context);
        assertOffer(helper, sledgehammer, "toolsmith/4/emerald_iron_sledgehammer",
                net.minecraft.world.item.Items.EMERALD, 16, ModItems.IRON_SLEDGEHAMMER, 1, 1, 30);
        helper.assertTrue(sledgehammer.getCostB().is(net.minecraft.world.item.Items.IRON_PICKAXE),
                "toolsmith/4/emerald_iron_sledgehammer second cost should be an iron pickaxe, was "
                        + sledgehammer.getCostB());
        assertOfferEnchantments(helper, sledgehammer, "toolsmith/4/emerald_iron_sledgehammer",
                SLEDGEHAMMER_ENCHANT_POOL);

        // toolsmith/3: the three chisels, 6 emeralds each, and every one of them Fast Chiseling.
        assertChiselTrade(helper, level, context, "toolsmith/3/emerald_copper_chisel", ModItems.COPPER_CHISEL);
        assertChiselTrade(helper, level, context, "toolsmith/3/emerald_iron_chisel", ModItems.IRON_CHISEL);
        assertChiselTrade(helper, level, context, "toolsmith/3/emerald_gold_chisel", ModItems.GOLD_CHISEL);

        helper.succeed();
    }

    /**
     * End-to-end: a real villager with the mason profession at level 2 has to be able to
     * roll one of our trades. The trade set picks a random subset of the merged pool, so the
     * offers are regenerated a bounded number of times; missing every single time means the
     * mod trades are not part of the pool the merchant draws from.
     */
    public static void masonVillagerCanRollAModTrade(GameTestHelper helper) {
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 1));

        Set<Item> wanted = Set.of(ModItems.COPPER_CORE, ModItems.DIAMOND_CORE);
        Set<Item> seen = new LinkedHashSet<>();
        boolean rolledModTrade = false;
        int emptyRolls = 0;

        for (int attempt = 0; attempt < 64 && !rolledModTrade; attempt++) {
            // Switching the profession clears the cached offers; the next getOffers() re-rolls them.
            setProfession(helper, villager, VillagerProfession.NITWIT, 1);
            setProfession(helper, villager, VillagerProfession.MASON, 2);

            List<MerchantOffer> offers = villager.getOffers();
            if (offers.isEmpty()) {
                emptyRolls++;
                continue;
            }
            for (MerchantOffer offer : offers) {
                Item result = offer.getResult().getItem();
                seen.add(result);
                if (wanted.contains(result)) {
                    rolledModTrade = true;
                }
            }
        }

        helper.assertValueEqual(emptyRolls, 0, "mason level 2 produced empty offer lists");
        helper.assertTrue(rolledModTrade,
                "a mason villager (level 2) never offered copper_core or diamond_core in 64 rolls; "
                        + "results seen were " + seen);

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // (b) legacy spatula -> chisel migration
    // ------------------------------------------------------------------

    /**
     * Player-side migration: stacks in the inventory and in the open container menu are
     * rewritten to the matching chisel, keeping the stack size and the component patch;
     * items that are not legacy spatulas must be left completely alone.
     *
     * <p>The mock player has to be a <em>connected</em> one: writing into the crafting grid makes
     * vanilla run {@code CraftingMenu#slotChangedCraftingGrid}, which unconditionally dereferences
     * {@code ServerPlayer#connection}. That matches production, where the migration only ever runs
     * from the join event and therefore always sees a player with a network handler.
     *
     * <p>The last step puts the player through a second login instead of calling the migration by
     * hand, because that is the part nothing else covers: both loaders register the migration on
     * their "player joined" event ({@code ServerPlayConnectionEvents.JOIN} on Fabric, a
     * {@code PlayerEvent.PlayerLoggedInEvent} handler on NeoForge), both fire it from
     * {@code PlayerList#placeNewPlayer}, and deleting either registration would leave every
     * assertion above green while no old spatula in any world is ever converted again.
     *
     * <p><strong>Not covered:</strong> the second half of that wiring, the server start hook that
     * runs {@link LegacySpatulaMigration#migrateWorlds}. It fires once, long before any gametest
     * body runs, and a shared gametest cannot re-enter a server start on either loader.
     */
    public static void legacySpatulasInPlayerInventoryBecomeChisels(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));

        // Spatulas and chisels are damageable tools, so a damaged one is always a single item;
        // the damage component itself refuses to apply to anything bigger than one.
        ItemStack stoneSpatula = new ItemStack(ModItems.STONE_SPATULA, 1);
        stoneSpatula.set(DataComponents.DAMAGE, 5);
        stoneSpatula.set(DataComponents.CUSTOM_NAME, Component.literal("Grandpa's tool"));
        player.getInventory().setItem(0, stoneSpatula);

        // The stack size is covered separately, on a stack without components to validate.
        ItemStack copperSpatulas = new ItemStack(ModItems.COPPER_SPATULA, 3);
        player.getInventory().setItem(1, copperSpatulas);

        ItemStack netheriteSpatula = new ItemStack(ModItems.NETHERITE_SPATULA, 1);
        player.getInventory().setItem(3, netheriteSpatula);

        // Untouched control: already-migrated item plus a vanilla item.
        ItemStack alreadyChisel = new ItemStack(ModItems.IRON_CHISEL, 1);
        player.getInventory().setItem(4, alreadyChisel);
        ItemStack vanilla = new ItemStack(net.minecraft.world.item.Items.DIAMOND, 12);
        player.getInventory().setItem(5, vanilla);

        // A spatula that only lives in the open menu (crafting grid slot), not in the inventory.
        ItemStack menuSpatula = new ItemStack(ModItems.GOLD_SPATULA, 1);
        player.containerMenu.getSlot(InventoryMenu.CRAFT_SLOT_START).set(menuSpatula);

        LegacySpatulaMigration.migratePlayer(player);

        ItemStack migrated = player.getInventory().getItem(0);
        helper.assertTrue(migrated.is(ModItems.STONE_CHISEL),
                "stone_spatula should have become stone_chisel, was " + migrated);
        helper.assertValueEqual(migrated.getCount(), 1, "migrated stack size");
        helper.assertValueEqual(migrated.get(DataComponents.DAMAGE), 5, "migrated damage component");
        helper.assertValueEqual(migrated.get(DataComponents.CUSTOM_NAME),
                Component.literal("Grandpa's tool"), "migrated custom name component");

        ItemStack migratedStack = player.getInventory().getItem(1);
        helper.assertTrue(migratedStack.is(ModItems.COPPER_CHISEL),
                "copper_spatula should have become copper_chisel, was " + migratedStack);
        helper.assertValueEqual(migratedStack.getCount(), 3, "migrated multi item stack size");

        helper.assertTrue(player.getInventory().getItem(3).is(ModItems.NETHERITE_CHISEL),
                "netherite_spatula should have become netherite_chisel, was "
                        + player.getInventory().getItem(3));

        helper.assertTrue(player.getInventory().getItem(4).is(ModItems.IRON_CHISEL),
                "an existing iron_chisel must survive the migration untouched");
        helper.assertTrue(player.getInventory().getItem(5).is(net.minecraft.world.item.Items.DIAMOND),
                "a vanilla stack must survive the migration untouched");
        helper.assertValueEqual(player.getInventory().getItem(5).getCount(), 12,
                "vanilla stack size after migration");

        ItemStack migratedMenuStack = player.containerMenu.getSlot(InventoryMenu.CRAFT_SLOT_START).getItem();
        helper.assertTrue(migratedMenuStack.is(ModItems.GOLD_CHISEL),
                "gold_spatula in the open menu should have become gold_chisel, was " + migratedMenuStack);
        helper.assertValueEqual(migratedMenuStack.getCount(), 1, "migrated menu stack size");

        // --- and now the hook that is supposed to call all of that in a real game ---
        ServerPlayer joining = logIn(helper, new ItemStack(ModItems.STONE_SPATULA, 2));

        ItemStack afterLogin = joining.getInventory().getItem(LOGIN_SLOT);
        helper.assertTrue(afterLogin.is(ModItems.STONE_CHISEL),
                "logging in left the spatula in the inventory alone (it is " + afterLogin + "); the "
                        + "mod's join listener is gone, so the migration never runs outside this test");
        helper.assertValueEqual(afterLogin.getCount(), 2, "stack size after the login migration");

        helper.succeed();
    }

    /**
     * World-side migration: a loose spatula lying on the ground is rewritten in place, so the
     * same {@link ItemEntity} now carries the chisel with the original count and components.
     */
    public static void legacySpatulaItemEntityIsRewrittenInPlace(GameTestHelper helper) {
        // Two entities on purpose: the damage component only validates on a single item, so
        // "components survive" and "stack size survives" cannot be checked on the same stack.
        ItemEntity damaged = helper.spawnItem(ModItems.DIAMOND_SPATULA, new BlockPos(1, 2, 1));
        ItemStack legacyDamaged = new ItemStack(ModItems.DIAMOND_SPATULA, 1);
        legacyDamaged.set(DataComponents.DAMAGE, 42);
        legacyDamaged.set(DataComponents.CUSTOM_NAME, Component.literal("Old flattener"));
        damaged.setItem(legacyDamaged);

        ItemEntity multiple = helper.spawnItem(ModItems.IRON_SPATULA, new BlockPos(5, 2, 1));
        multiple.setItem(new ItemStack(ModItems.IRON_SPATULA, 3));

        ItemEntity control = helper.spawnItem(net.minecraft.world.item.Items.STICK, new BlockPos(3, 2, 3));

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(damaged.isAlive(), "the dropped spatula entity vanished before migration");
                    helper.assertTrue(multiple.isAlive(), "the dropped spatula stack vanished before migration");
                    LegacySpatulaMigration.migrateWorlds(helper.getLevel().getServer());
                })
                .thenExecute(() -> {
                    ItemStack after = damaged.getItem();
                    helper.assertTrue(after.is(ModItems.DIAMOND_CHISEL),
                            "the item entity should now carry a diamond_chisel, was " + after);
                    helper.assertValueEqual(after.getCount(), 1, "item entity stack size after migration");
                    helper.assertValueEqual(after.get(DataComponents.DAMAGE), 42,
                            "item entity damage component after migration");
                    helper.assertValueEqual(after.get(DataComponents.CUSTOM_NAME),
                            Component.literal("Old flattener"), "item entity custom name after migration");

                    ItemStack afterStack = multiple.getItem();
                    helper.assertTrue(afterStack.is(ModItems.IRON_CHISEL),
                            "the item entity should now carry an iron_chisel, was " + afterStack);
                    helper.assertValueEqual(afterStack.getCount(), 3,
                            "item entity stack size of the multi item stack after migration");

                    helper.assertTrue(control.getItem().is(net.minecraft.world.item.Items.STICK),
                            "an unrelated item entity must not be rewritten");
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /**
     * Vanilla's optional "Trade Rebalance" datapack declares {@code "replace": true} on the three
     * librarian trade tags, which discards every earlier contributor -- including this mod. It is
     * off in normal worlds but ON in the gametest environment, which enables all experimental packs.
     * Mason, toolsmith and wandering trader pools are not touched by it.
     */
    /**
     * Builds a player that is already carrying {@code carried} and logs it into the running
     * server. Both loaders raise their "player joined" event from
     * {@code PlayerList#placeNewPlayer}, so this is the loader neutral way to reach the listener
     * the migration is registered on.
     *
     * <p>The ingredients are vanilla's own, copied from
     * {@code GameTestHelper#makeMockServerPlayerInLevel}: a {@link ServerPlayer} on this level, a
     * fresh {@link Connection} on an {@link EmbeddedChannel} and an initial listener cookie. The
     * player is built by hand rather than through that helper only because the item has to be in
     * the inventory <em>before</em> the login, and the helper constructs and logs in in one step.
     * {@code placeNewPlayer} reads no saved player data, so what is set here is exactly what the
     * join event sees.
     *
     * <p>Handed back to the player list at the end of the test like every other mock player in
     * this suite - one that stays behind keeps the list non-empty and stalls the gametest server
     * on shutdown.
     */
    @SuppressWarnings("resource")
    private static ServerPlayer logIn(GameTestHelper helper, ItemStack carried) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-migration-player"),
                ClientInformation.createDefault());
        player.getInventory().setItem(LOGIN_SLOT, carried);
        helper.assertTrue(player.getInventory().getItem(LOGIN_SLOT).is(carried.getItem()),
                "the item was not in the inventory before the login, so the login proves nothing");

        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));
        helper.runBeforeTestEnd(() -> server.getPlayerList().remove(player));

        helper.assertTrue(server.getPlayerList().getPlayers().contains(player),
                "the player never made it into the player list, so no join event was fired");
        return player;
    }

    private static boolean tradeRebalanceActive(GameTestHelper helper) {
        return helper.getLevel().getServer().getResourceManager().listPacks()
                .anyMatch(pack -> "trade_rebalance".equals(pack.packId()));
    }

    private static void assertPoolContains(GameTestHelper helper, Registry<VillagerTrade> trades,
                                           TagKey<VillagerTrade> tag, String... expectedIds) {
        Set<String> actual = new LinkedHashSet<>();
        for (Holder<VillagerTrade> holder : trades.getTagOrEmpty(tag)) {
            actual.add(holder.getRegisteredName());
        }

        for (String expected : expectedIds) {
            helper.assertTrue(actual.contains(expected),
                    "trade tag " + tag.location() + " does not contain " + expected + "; it holds " + actual);
        }

        boolean hasVanilla = actual.stream().anyMatch(id -> !id.startsWith(NAMESPACE + ":"));
        helper.assertTrue(hasVanilla,
                "trade tag " + tag.location() + " lost its vanilla entries - the tag file most likely "
                        + "replaces instead of merges; it holds " + actual);
    }

    private static void assertTradeSetForProfession(GameTestHelper helper,
                                                    ResourceKey<VillagerProfession> professionKey, int level,
                                                    ResourceKey<TradeSet> expectedTradeSet, String... expectedIds) {
        var registries = helper.getLevel().registryAccess();
        VillagerProfession profession = registries.lookupOrThrow(Registries.VILLAGER_PROFESSION)
                .getOrThrow(professionKey).value();

        ResourceKey<TradeSet> tradeSetKey = profession.getTrades(level);
        helper.assertValueEqual(tradeSetKey, expectedTradeSet,
                "trade set of " + professionKey.identifier() + " at level " + level);

        TradeSet tradeSet = registries.lookupOrThrow(Registries.TRADE_SET).getOrThrow(tradeSetKey).value();
        HolderSet<VillagerTrade> pool = tradeSet.getTrades();

        Set<String> actual = new LinkedHashSet<>();
        for (Holder<VillagerTrade> holder : pool) {
            actual.add(holder.getRegisteredName());
        }
        for (String expected : expectedIds) {
            helper.assertTrue(actual.contains(expected),
                    "trade set " + tradeSetKey.identifier() + " does not offer " + expected + "; it holds " + actual);
        }
    }

    private static LootContext tradeContext(GameTestHelper helper, Villager villager) {
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, villager.position())
                .withParameter(LootContextParams.THIS_ENTITY, villager)
                .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
                .create(LootContextParamSets.VILLAGER_TRADE);
        // Fixed seed so trades with a weighted enchantment pool stay reproducible.
        return new LootContext.Builder(params).withOptionalRandomSeed(20260828L).create(Optional.empty());
    }

    private static MerchantOffer offerOf(GameTestHelper helper, ServerLevel level, String id, LootContext context) {
        VillagerTrade trade = level.registryAccess()
                .lookupOrThrow(Registries.VILLAGER_TRADE)
                .getValue(Identifier.parse(id));
        helper.assertTrue(trade != null, "trade " + id + " is not registered");

        MerchantOffer offer = trade.getOffer(context);
        helper.assertTrue(offer != null, "trade " + id + " produced no offer");
        return offer;
    }

    /**
     * One of the three toolsmith chisel trades: 6 emeralds for one chisel, two uses, 10 xp, and a
     * Fast Chiseling enchantment out of its pool. All three files are identical apart from the
     * chisel they hand over, so all three are asserted - a single one would leave the other two
     * free to be repriced.
     */
    private static void assertChiselTrade(GameTestHelper helper, ServerLevel level, LootContext context,
                                          String path, Item chisel) {
        MerchantOffer offer = offerOf(helper, level, NAMESPACE + ":" + path, context);
        assertOffer(helper, offer, path, net.minecraft.world.item.Items.EMERALD, 6, chisel, 1, 2, 10);
        helper.assertTrue(offer.getCostB().isEmpty(), path + " must not have a second cost item");
        assertOfferEnchantments(helper, offer, path, CHISEL_ENCHANT_POOL);
    }

    /**
     * The enchantments {@code simplebuilding:weighted_enchant} put on a trade's result, as
     * {@code <enchantment id>@<level>}. At least one is required - an empty result is exactly what
     * an emptied {@code WeightedEnchantFunction#run} produces - and each one has to be in the pool
     * the trade json declares.
     */
    private static void assertOfferEnchantments(GameTestHelper helper, MerchantOffer offer, String id,
                                                Set<String> pool) {
        ItemStack result = offer.getResult();
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(result);
        helper.assertTrue(!enchantments.isEmpty(),
                id + ": the merchant hands out an unenchanted " + result.getItem()
                        + "; its weighted_enchant modifier did nothing");

        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            String drawn = enchantment.getRegisteredName() + "@" + enchantments.getLevel(enchantment);
            helper.assertTrue(pool.contains(drawn),
                    id + ": the merchant put " + drawn + " on the tool, which is not one of the "
                            + "enchantment/level pairs its json offers (" + pool + ")");
        }
    }

    private static void assertOffer(GameTestHelper helper, MerchantOffer offer, String id,
                                    Item wantedItem, int wantedCount,
                                    Item givenItem, int givenCount,
                                    int maxUses, int xp) {
        ItemStack costA = offer.getBaseCostA();
        helper.assertTrue(costA.is(wantedItem),
                id + ": expected cost item " + wantedItem + ", was " + costA);
        helper.assertValueEqual(costA.getCount(), wantedCount, id + ": cost count");

        ItemStack result = offer.getResult();
        helper.assertTrue(result.is(givenItem),
                id + ": expected result item " + givenItem + ", was " + result);
        helper.assertValueEqual(result.getCount(), givenCount, id + ": result count");

        helper.assertValueEqual(offer.getMaxUses(), maxUses, id + ": max uses");
        helper.assertValueEqual(offer.getXp(), xp, id + ": trade xp");
    }

    private static void setProfession(GameTestHelper helper, Villager villager,
                                      ResourceKey<VillagerProfession> profession, int level) {
        VillagerData data = villager.getVillagerData()
                .withProfession(helper.getLevel().registryAccess(), profession)
                .withLevel(level);
        villager.setVillagerData(data);
    }
}
