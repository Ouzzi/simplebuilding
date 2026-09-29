package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.PlacedBundleBlock;
import com.simplebuilding.blocks.entity.custom.PlacedBundleBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.networking.ModMessageHandlers;
import com.simplebuilding.networking.PlacedBundleScrollPayload;
import com.simplebuilding.util.PlacedBundles;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Spieltests der abgestellten Buendel (Besitzer 2026-09-28/29, {@link PlacedBundles}): Abstellen nur
 * mit Schleichen und nur auf eine Oberseite, fuer alle Stufen (auch gefaerbt und farbige
 * Vanilla-Buendel); das gezeigte Item wechselt nur ueber das Mausrad-Paket eines schleichenden
 * Spielers in Reichweite (nie von selbst); Rechtsklick (echter Weg ueber
 * {@code ServerPlayerGameMode#useItemOn}) nimmt genau das gezeigte Item heraus, Schleichen +
 * Rechtsklick mit einem Item legt es nach den Regeln des Buendels hinein; Abbauen gibt das Buendel mit
 * seinem ganzen Inhalt zurueck.
 */
public final class PlacedBundleTests {

    /** So lange schaut ein schleichender Spieler hin, ohne dass das gezeigte Item von selbst wechseln darf. */
    public static final int NO_AUTO_CYCLE_TICKS = 50;
    public static final int SCROLL_MAX_TICKS = NO_AUTO_CYCLE_TICKS + 40;

    private PlacedBundleTests() {
    }

    // =====================================================================================
    // Abstellen
    // =====================================================================================

    /**
     * Schleichen + Rechtsklick auf eine Oberseite stellt jedes Buendel ab - Vanilla, farbig (blau),
     * verstaerkt, Netherit, Enderit und ein gefaerbtes verstaerktes: Stufe und Farbe im Blockzustand,
     * Vorderseite zum Spieler, derselbe Stapel samt Inhalt und Namen in der Block-Entity, die auch so
     * heisst. Auf eine Seitenflaeche, an die Decke, ohne Schleichen oder mit einem Koecher wird nichts
     * abgestellt.
     */
    public static void sneakUsePlacesBundlesOnlyOnTopFaces(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(4.5, 2.0, 6.5));
        player.setShiftKeyDown(true);
        List<Item> items = List.of(Items.BUNDLE, vanilla("blue_bundle"), ModItems.REINFORCED_BUNDLE, ModItems.NETHERITE_BUNDLE,
                ModItems.ENDERITE_BUNDLE, ModItems.REINFORCED_BUNDLE);
        List<PlacedBundleBlock.Tier> tiers = List.of(PlacedBundleBlock.Tier.BUNDLE, PlacedBundleBlock.Tier.BUNDLE,
                PlacedBundleBlock.Tier.REINFORCED, PlacedBundleBlock.Tier.NETHERITE, PlacedBundleBlock.Tier.ENDERITE,
                PlacedBundleBlock.Tier.REINFORCED);
        for (int i = 0; i < items.size(); i++) {
            BlockPos floor = new BlockPos(i, 1, 1);
            helper.setBlock(floor, Blocks.STONE);
            ItemStack bundle = filled(items.get(i), new ItemStack(Items.TORCH, 8), new ItemStack(Items.APPLE, 2));
            if (i == 5) {
                bundle.set(DataComponents.DYED_COLOR, new DyedItemColor(0x80C71F));
                bundle.set(DataComponents.CUSTOM_NAME, Component.literal("Picnic"));
            }
            InteractionResult result = use(helper, player, bundle.copy(), floor, Direction.UP);
            helper.assertTrue(result.consumesAction(), "sneak + use on top of stone did not place " + bundle + ": " + result);
            BlockPos at = floor.above();
            BlockState state = helper.getBlockState(at);
            helper.assertTrue(state.is(ModBlocks.PLACED_BUNDLE), "no placed bundle for " + bundle + " but " + state);
            helper.assertTrue(state.getValue(PlacedBundleBlock.TIER) == tiers.get(i),
                    bundle + " was placed as tier " + state.getValue(PlacedBundleBlock.TIER) + " instead of " + tiers.get(i));
            boolean dyed = i == 1 || i == 5;
            helper.assertTrue(state.getValue(PlacedBundleBlock.DYED) == dyed, bundle + " dyed=" + state.getValue(PlacedBundleBlock.DYED));
            helper.assertTrue(state.getValue(PlacedBundleBlock.FACING) == player.getDirection().getOpposite(),
                    "the bundle faces " + state.getValue(PlacedBundleBlock.FACING) + " instead of the player");
            PlacedBundleBlockEntity be = bundleAt(helper, at);
            helper.assertTrue(ItemStack.isSameItemSameComponents(be.getBundle(), bundle) && be.getBundle().getCount() == 1,
                    "the placed bundle holds " + be.getBundle() + " instead of " + bundle);
            helper.assertTrue(be.getDisplayName().getString().equals(bundle.getHoverName().getString()),
                    "the placed bundle is called " + be.getDisplayName().getString() + " instead of " + bundle.getHoverName().getString());
        }
        helper.assertTrue(bundleAt(helper, new BlockPos(1, 2, 1)).dyeColor() == 0x3C44AA,
                "the blue vanilla bundle tints " + Integer.toHexString(bundleAt(helper, new BlockPos(1, 2, 1)).dyeColor()));
        helper.assertTrue(bundleAt(helper, new BlockPos(5, 2, 1)).dyeColor() == 0x80C71F, "the dyed bundle lost its colour");

        // Seitenflaeche und Decke: nichts - obwohl unter der Zielstelle jeweils ein tragender Block
        // steht, entscheidet allein die geklickte Seite.
        helper.setBlock(new BlockPos(1, 1, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 2, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 2, 4), Blocks.STONE);
        InteractionResult side = use(helper, player, new ItemStack(ModItems.REINFORCED_BUNDLE), new BlockPos(1, 2, 4), Direction.NORTH);
        helper.assertTrue(!side.consumesAction() && !helper.getBlockState(new BlockPos(1, 2, 3)).is(ModBlocks.PLACED_BUNDLE),
                "a bundle was placed against a wall: " + side);
        helper.setBlock(new BlockPos(3, 4, 4), Blocks.STONE);
        use(helper, player, new ItemStack(Items.BUNDLE), new BlockPos(3, 4, 4), Direction.DOWN);
        helper.assertTrue(!helper.getBlockState(new BlockPos(3, 3, 4)).is(ModBlocks.PLACED_BUNDLE), "a bundle was hung under a ceiling");

        // Ohne Schleichen und mit einem Koecher: nichts.
        helper.setBlock(new BlockPos(5, 1, 4), Blocks.STONE);
        player.setShiftKeyDown(false);
        use(helper, player, new ItemStack(Items.BUNDLE), new BlockPos(5, 1, 4), Direction.UP);
        helper.assertTrue(helper.getBlockState(new BlockPos(5, 2, 4)).isAir(), "a bundle was placed without sneaking");
        player.setShiftKeyDown(true);
        use(helper, player, new ItemStack(ModItems.QUIVER), new BlockPos(5, 1, 4), Direction.UP);
        helper.assertTrue(helper.getBlockState(new BlockPos(5, 2, 4)).isAir(), "a quiver was placed as a bundle");
        helper.succeed();
    }

    // =====================================================================================
    // Anschauen und Herausnehmen
    // =====================================================================================

    /**
     * Das gezeigte Item waehlt der Spieler mit Schleichen + Mausrad: das Paket
     * ({@code PlacedBundleScrollPayload}, echter Handler {@link ModMessageHandlers#handlePlacedBundleScroll})
     * schaltet vor und zurueck, reihum in beide Richtungen - aber nicht ohne Schleichen, nicht ausser
     * Reichweite und nicht bei nur einem Item. Rechtsklick mit leerer Hand (echter Weg
     * {@code useItemOn}) legt genau das gezeigte Item in die Hand, das naechste rueckt nach; ohne
     * Schleichen und mit einem Item in der Hand kommt es ins Inventar, bei vollem Inventar vor die
     * Fuesse. Und das gezeigte Item wechselt nie von selbst, auch wenn jemand lange schleichend
     * hinschaut.
     */
    public static void sneakScrollPacketsCycleTheTopItemAndRightClickTakesIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        // Der Spieler schwebt ueber dem Buendel und schaut senkrecht hinunter.
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 3.2, 2.5));
        player.setNoGravity(true);
        player.setShiftKeyDown(true);
        ItemStack bundle = filled(ModItems.REINFORCED_BUNDLE, new ItemStack(Items.TORCH, 8), new ItemStack(Items.APPLE, 3),
                new ItemStack(Items.FEATHER, 5));
        use(helper, player, bundle, new BlockPos(2, 1, 2), Direction.UP);
        BlockPos rel = new BlockPos(2, 2, 2);
        BlockPos pos = helper.absolutePos(rel);
        PlacedBundleBlockEntity be = bundleAt(helper, rel);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos).subtract(0.0, 0.3, 0.0));
        helper.assertTrue(be.shownItem().is(Items.TORCH), "the top item is " + be.shownItem() + " instead of the last inserted torch");

        // Nicht schleichend: das Paket aendert nichts.
        player.setShiftKeyDown(false);
        ModMessageHandlers.handlePlacedBundleScroll(new PlacedBundleScrollPayload(pos, 1), player);
        helper.assertTrue(be.shownIndex() == 0, "a scroll packet from a player who is not sneaking moved the top item to " + be.shownItem());
        // Schleichend: vor, zurueck, und rueckwaerts ueber den Anfang hinaus.
        player.setShiftKeyDown(true);
        ModMessageHandlers.handlePlacedBundleScroll(new PlacedBundleScrollPayload(pos, 1), player);
        helper.assertTrue(be.shownItem().is(Items.APPLE), "one scroll step down shows " + be.shownItem() + " instead of the apples");
        ModMessageHandlers.handlePlacedBundleScroll(new PlacedBundleScrollPayload(pos, -1), player);
        helper.assertTrue(be.shownItem().is(Items.TORCH), "one scroll step back shows " + be.shownItem() + " instead of the torch");
        ModMessageHandlers.handlePlacedBundleScroll(new PlacedBundleScrollPayload(pos, -1), player);
        helper.assertTrue(be.shownItem().is(Items.FEATHER), "scrolling back past the first item does not wrap to the feathers: " + be.shownItem());
        // Ein uebergrosser Schritt zaehlt als einer (der Client schickt nur +1/-1).
        ModMessageHandlers.handlePlacedBundleScroll(new PlacedBundleScrollPayload(pos, 1000), player);
        helper.assertTrue(be.shownItem().is(Items.TORCH), "a scroll packet with a huge step moved by more than one: " + be.shownItem());
        ModMessageHandlers.handlePlacedBundleScroll(new PlacedBundleScrollPayload(pos, 1), player);
        helper.assertTrue(be.shownItem().is(Items.APPLE), "the scroll back to the apples failed: " + be.shownItem());

        // Ausser Reichweite: nichts (der Server prueft selbst, der Client koennte luegen).
        ServerPlayer far = mockPlayer(helper, new Vec3(2.5, 2.0, 2.5 + 14.0));
        far.setShiftKeyDown(true);
        ModMessageHandlers.handlePlacedBundleScroll(new PlacedBundleScrollPayload(pos, 1), far);
        helper.assertTrue(be.shownItem().is(Items.APPLE), "a scroll packet from 14 blocks away moved the top item to " + be.shownItem());

        // Rechtsklick mit leerer Hand: der Apfel (gezeigt) in die Hand, die Federn ruecken nach.
        InteractionResult take = player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hitTop(pos));
        helper.assertTrue(take.consumesAction(), "right-clicking the placed bundle returned " + take);
        ItemStack inHand = player.getMainHandItem();
        helper.assertTrue(inHand.is(Items.APPLE) && inHand.getCount() == 3, "the hand holds " + inHand + " instead of the three shown apples");
        helper.assertTrue(be.contents().size() == 2 && be.shownItem().is(Items.FEATHER),
                "after taking the apples the bundle holds " + be.contents() + ", showing " + be.shownItem());
        // Ohne Schleichen, Apfel in der Hand: die Federn ins Inventar, der Apfel bleibt in der Hand.
        player.setShiftKeyDown(false);
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hitTop(pos));
        helper.assertTrue(player.getMainHandItem().is(Items.APPLE), "the apples left the hand");
        helper.assertTrue(player.getInventory().countItem(Items.FEATHER) == 5, "the feathers did not reach the inventory");
        helper.assertTrue(be.contents().size() == 1 && be.shownItem().is(Items.TORCH), "left in the bundle: " + be.contents());
        // Nur noch ein Item: das Mausrad hat nichts zu waehlen.
        player.setShiftKeyDown(true);
        helper.assertTrue(!PlacedBundles.scroll(player, pos, 1), "a bundle with a single item scrolled");

        // Volles Inventar: das gezeigte Item faellt vor die Fuesse. Im Ueberlebensmodus - einem
        // Kreativspieler verschluckt Vanillas Inventory#add, was nicht passt.
        player.setGameMode(GameType.SURVIVAL);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        player.setShiftKeyDown(false);
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hitTop(pos));
        helper.assertTrue(be.isEmpty(), "the torches stayed in the bundle although they had to be dropped: " + be.contents());
        boolean torchesDropped = level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3.0)).stream()
                .anyMatch(entity -> entity.getItem().is(Items.TORCH) && entity.getItem().getCount() == 8);
        helper.assertTrue(torchesDropped, "with a full inventory the eight torches were not dropped at the player's feet");
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }

        // Nie von selbst: mit zwei Items und einem schleichenden Betrachter bleibt der Index stehen.
        PlacedBundles.setContents(be.getBundle(), List.of(new ItemStack(Items.TORCH, 8), new ItemStack(Items.STICK, 2)));
        be.setBundle(be.getBundle());
        be.setShownIndex(0);
        player.setShiftKeyDown(true);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos).subtract(0.0, 0.3, 0.0));
        helper.runAfterDelay(NO_AUTO_CYCLE_TICKS, () -> {
            helper.assertTrue(be.shownIndex() == 0, "the top item changed by itself to " + be.shownItem()
                    + " - only the scroll wheel may change it");
            helper.succeed();
        });
    }

    // =====================================================================================
    // Hineinlegen
    // =====================================================================================

    /**
     * Schleichen + Rechtsklick mit einem Item (echter Weg {@code ServerPlayerGameMode#useItemOn}, bei
     * dem Vanilla den Block beim Schleichen gar nicht fragt) legt es in das abgestellte Buendel, so weit
     * es passt: ein Vanilla-Buendel fasst einen Stapel (16 Enderperlen, danach nichts mehr), ein
     * verstaerktes 96 Items (von zwei 64er-Stapeln Bruchstein kommen 64 + 32 hinein, 32 bleiben in
     * der Hand). Das hineingelegte Item liegt oben und wird gezeigt. Was nicht in ein Buendel darf
     * (Shulkerkiste), bleibt in der Hand. Ein Spieler im Ueberlebensmodus, damit der Stapel in der
     * Hand wirklich schrumpft.
     */
    public static void sneakRightClickWithAnItemDepositsIntoThePlacedBundle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 3.2, 3.5));
        player.setNoGravity(true);
        player.setGameMode(GameType.SURVIVAL);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.STONE);
        use(helper, player, filled(Items.BUNDLE, new ItemStack(Items.STRING, 1)), new BlockPos(2, 1, 2), Direction.UP);
        use(helper, player, filled(ModItems.REINFORCED_BUNDLE), new BlockPos(4, 1, 2), Direction.UP);
        BlockPos vanillaPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos reinforcedPos = helper.absolutePos(new BlockPos(4, 2, 2));
        PlacedBundleBlockEntity vanilla = bundleAt(helper, new BlockPos(2, 2, 2));
        PlacedBundleBlockEntity reinforced = bundleAt(helper, new BlockPos(4, 2, 2));
        helper.assertTrue(vanilla.contents().size() == 1 && reinforced.isEmpty(),
                "the two bundles were not placed as prepared: " + vanilla.contents() + " / " + reinforced.contents());

        // Vanilla-Buendel: ein Faden (1/64) liegt drin, 15 Enderperlen (je 1/16) passen noch - nicht 16.
        ItemStack pearls = new ItemStack(Items.ENDER_PEARL, 16);
        player.setItemInHand(InteractionHand.MAIN_HAND, pearls);
        InteractionResult deposit = player.gameMode.useItemOn(player, level, pearls, InteractionHand.MAIN_HAND, hitTop(vanillaPos));
        helper.assertTrue(deposit.consumesAction(), "sneak + right-click with pearls on the placed bundle returned " + deposit);
        helper.assertTrue(player.getMainHandItem().is(Items.ENDER_PEARL) && player.getMainHandItem().getCount() == 1,
                "the hand holds " + player.getMainHandItem() + " instead of the one pearl that did not fit");
        helper.assertTrue(vanilla.shownItem().is(Items.ENDER_PEARL) && vanilla.shownItem().getCount() == 15 && vanilla.shownIndex() == 0,
                "the placed vanilla bundle shows " + vanilla.shownItem() + " at " + vanilla.shownIndex() + " instead of the 15 pearls on top");
        helper.assertTrue(vanilla.contents().size() == 2, "the vanilla bundle holds " + vanilla.contents());
        // Voll: die letzte Perle bleibt in der Hand, nichts veraendert sich.
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hitTop(vanillaPos));
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && vanilla.contents().size() == 2,
                "the full vanilla bundle took more: hand " + player.getMainHandItem() + ", bundle " + vanilla.contents());

        // Verstaerktes Buendel: 96 Items (1,5 Stapel).
        ItemStack cobble = new ItemStack(Items.COBBLESTONE, 64);
        player.setItemInHand(InteractionHand.MAIN_HAND, cobble);
        player.gameMode.useItemOn(player, level, cobble, InteractionHand.MAIN_HAND, hitTop(reinforcedPos));
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the first 64 cobblestone did not all go in: hand " + player.getMainHandItem());
        ItemStack more = new ItemStack(Items.COBBLESTONE, 64);
        player.setItemInHand(InteractionHand.MAIN_HAND, more);
        player.gameMode.useItemOn(player, level, more, InteractionHand.MAIN_HAND, hitTop(reinforcedPos));
        helper.assertTrue(player.getMainHandItem().is(Items.COBBLESTONE) && player.getMainHandItem().getCount() == 32,
                "the reinforced bundle (96 items) left " + player.getMainHandItem() + " in the hand instead of 32 cobblestone");
        int stored = reinforced.contents().stream().mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(stored == 96, "the reinforced bundle holds " + stored + " cobblestone instead of 96");
        helper.assertTrue(reinforced.shownItem().is(Items.COBBLESTONE) && reinforced.shownIndex() == 0,
                "the reinforced bundle shows " + reinforced.shownItem() + " at " + reinforced.shownIndex());

        // Was nicht in ein Buendel darf, bleibt in der Hand.
        helper.setBlock(new BlockPos(2, 1, 4), Blocks.STONE);
        use(helper, player, filled(ModItems.NETHERITE_BUNDLE), new BlockPos(2, 1, 4), Direction.UP);
        BlockPos netheritePos = helper.absolutePos(new BlockPos(2, 2, 4));
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        player.setItemInHand(InteractionHand.MAIN_HAND, shulker);
        player.gameMode.useItemOn(player, level, shulker, InteractionHand.MAIN_HAND, hitTop(netheritePos));
        helper.assertTrue(player.getMainHandItem().is(Items.SHULKER_BOX) && bundleAt(helper, new BlockPos(2, 2, 4)).isEmpty(),
                "a shulker box went into the placed bundle");

        // Ohne Schleichen legt ein Item in der Hand nichts hinein (dann nimmt der Klick heraus).
        player.setShiftKeyDown(false);
        ItemStack sticks = new ItemStack(Items.STICK, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, sticks);
        player.gameMode.useItemOn(player, level, sticks, InteractionHand.MAIN_HAND, hitTop(reinforcedPos));
        helper.assertTrue(player.getMainHandItem().is(Items.STICK) && player.getMainHandItem().getCount() == 4,
                "a right-click without sneaking put sticks into the bundle: hand " + player.getMainHandItem());
        helper.succeed();
    }

    // =====================================================================================
    // Drops
    // =====================================================================================

    /**
     * Abbauen gibt das Buendel mit Inhalt, Farbe und Namen zurueck, ebenso ein weggenommener Boden;
     * im Kreativmodus faellt ein volles Buendel trotzdem heraus. Mittlere Maustaste liefert das Buendel.
     */
    public static void placedBundlesDropThemselvesWithTheirContents(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 5.5));
        player.setShiftKeyDown(true);
        ItemStack bundle = filled(ModItems.NETHERITE_BUNDLE, new ItemStack(Items.DIAMOND, 4), new ItemStack(Items.TORCH, 16));
        bundle.set(DataComponents.DYED_COLOR, new DyedItemColor(0xB02E26));
        bundle.set(DataComponents.CUSTOM_NAME, Component.literal("Loot"));
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        use(helper, player, bundle.copy(), new BlockPos(1, 1, 1), Direction.UP);
        BlockPos rel = new BlockPos(1, 2, 1);
        ItemStack picked = helper.getBlockState(rel).getCloneItemStack(helper.getLevel(), helper.absolutePos(rel), true);
        helper.assertTrue(ItemStack.isSameItemSameComponents(picked, bundle), "pick block gave " + picked);
        helper.getLevel().destroyBlock(helper.absolutePos(rel), true);
        ItemEntity broken = dropped(helper, ModItems.NETHERITE_BUNDLE);
        helper.assertTrue(broken != null && ItemStack.isSameItemSameComponents(broken.getItem(), bundle),
                "breaking the placed bundle dropped " + (broken == null ? "nothing" : broken.getItem()) + " instead of " + bundle);
        broken.discard();

        // Boden weg: das Buendel faellt mit Inhalt heraus.
        helper.setBlock(new BlockPos(4, 1, 1), Blocks.STONE);
        ItemStack vanilla = filled(vanilla("red_bundle"), new ItemStack(Items.STRING, 3));
        use(helper, player, vanilla.copy(), new BlockPos(4, 1, 1), Direction.UP);
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 1)).is(ModBlocks.PLACED_BUNDLE), "the red bundle was not placed");
        helper.setBlock(new BlockPos(4, 1, 1), Blocks.AIR);
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 1)).isAir(), "the bundle stayed without its floor");
        ItemEntity fallen = dropped(helper, vanilla("red_bundle"));
        helper.assertTrue(fallen != null && ItemStack.isSameItemSameComponents(fallen.getItem(), vanilla),
                "the bundle without floor dropped " + (fallen == null ? "nothing" : fallen.getItem()));

        // Kreativ abgebaut (der Mock-Spieler ist im Kreativmodus): ein volles Buendel faellt trotzdem.
        helper.setBlock(new BlockPos(1, 1, 3), Blocks.STONE);
        ItemStack enderite = filled(ModItems.ENDERITE_BUNDLE, new ItemStack(Items.ENDER_PEARL, 2));
        use(helper, player, enderite.copy(), new BlockPos(1, 1, 3), Direction.UP);
        player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(1, 2, 3)));
        helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, 3)).isAir(), "the creative player did not break the bundle");
        ItemEntity creative = dropped(helper, ModItems.ENDERITE_BUNDLE);
        helper.assertTrue(creative != null && ItemStack.isSameItemSameComponents(creative.getItem(), enderite),
                "a full bundle broken in creative mode dropped " + (creative == null ? "nothing" : creative.getItem()));
        helper.succeed();
    }

    // =====================================================================================

    /** Ein Vanilla-Item per Id: 26.2 fuehrt die farbigen Buendel als ColorCollection, 1.21.11 je Farbe ein Feld. */
    private static Item vanilla(String path) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(path));
    }

    private static ItemStack filled(Item item, ItemStack... contents) {
        ItemStack bundle = new ItemStack(item);
        PlacedBundles.setContents(bundle, List.of(contents));
        return bundle;
    }

    /** Rechtsklick mit {@code stack} auf die Seite {@code face} des Blocks {@code on} (relativ). */
    private static InteractionResult use(GameTestHelper helper, Player player, ItemStack stack, BlockPos on, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(on);
        Vec3 hit = Vec3.atCenterOf(abs).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, abs, false)));
    }

    private static BlockHitResult hitTop(BlockPos abs) {
        return new BlockHitResult(new Vec3(abs.getX() + 0.5, abs.getY() + 11.0 / 16.0, abs.getZ() + 0.5), Direction.UP, abs, false);
    }

    private static PlacedBundleBlockEntity bundleAt(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, PlacedBundleBlockEntity.class);
    }

    /** Das erste Item dieser Art, das in der Teststruktur liegt, oder null. */
    private static ItemEntity dropped(GameTestHelper helper, Item item) {
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds())) {
            if (entity.getItem().is(item) && entity.isAlive()) {
                return entity;
            }
        }
        return null;
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }
}
