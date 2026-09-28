package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.PlacedBundleBlock;
import com.simplebuilding.blocks.entity.custom.PlacedBundleBlockEntity;
import com.simplebuilding.items.ModItems;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Spieltests der abgestellten Buendel (Besitzer 2026-09-28, {@link PlacedBundles}): Abstellen nur mit
 * Schleichen und nur auf eine Oberseite, fuer alle Stufen (auch gefaerbt und farbige Vanilla-Buendel),
 * das gezeigte Item wechselt nur fuer einen schleichenden Betrachter, Rechtsklick (echter Weg ueber
 * {@code ServerPlayerGameMode#useItemOn}) nimmt genau das gezeigte Item heraus, und Abbauen gibt das
 * Buendel mit seinem ganzen Inhalt zurueck.
 */
public final class PlacedBundleTests {

    /** Zeit, bis der Block-Entity-Takt das gezeigte Item sicher einmal weitergeschaltet hat. */
    public static final int CYCLE_MAX_TICKS = PlacedBundles.CYCLE_TICKS * 3 + 20;

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
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Anschauen und Herausnehmen
    // =====================================================================================

    /**
     * Das gezeigte Item wechselt nur, wenn ein Spieler schleicht und auf das Buendel schaut - direkt
     * ueber {@link PlacedBundles#tickCycle} und danach auch ueber den Takt der Block-Entity. Rechtsklick
     * mit leerer Hand (der echte Weg {@code useItemOn}) legt genau das gezeigte Item in die Hand, das
     * naechste rueckt nach; ohne Schleichen und mit einem Item in der Hand kommt es ins Inventar.
     */
    public static void sneakingViewersCycleTheTopItemAndRightClickTakesIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        // Der Betrachter schwebt ueber dem Buendel und schaut senkrecht hinunter: Schleichen senkt die
        // Augen, trifft aber weiter dasselbe Buendel.
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
        helper.assertTrue(PlacedBundles.isViewing(player, pos), "the sneaking player looking down is not viewing the bundle");
        helper.assertTrue(be.shownItem().is(Items.TORCH), "the top item is " + be.shownItem() + " instead of the last inserted torch");

        // Nicht schleichend: kein Wechsel.
        player.setShiftKeyDown(false);
        helper.assertTrue(!PlacedBundles.tickCycle(level, pos, be) && be.shownIndex() == 0, "the bundle cycled for a player who is not sneaking");
        // Schleichend: reihum.
        player.setShiftKeyDown(true);
        helper.assertTrue(PlacedBundles.tickCycle(level, pos, be) && be.shownItem().is(Items.APPLE), "one cycle shows " + be.shownItem());
        PlacedBundles.tickCycle(level, pos, be);
        PlacedBundles.tickCycle(level, pos, be);
        helper.assertTrue(be.shownItem().is(Items.TORCH), "three cycles over three items do not wrap to the torch: " + be.shownItem());
        PlacedBundles.tickCycle(level, pos, be);

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

        // Der Takt der Block-Entity schaltet selbst weiter, solange jemand schleichend hinschaut.
        PlacedBundles.setContents(be.getBundle(), List.of(new ItemStack(Items.TORCH, 8), new ItemStack(Items.STICK, 2)));
        be.setBundle(be.getBundle());
        be.setShownIndex(0);
        player.setShiftKeyDown(true);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos).subtract(0.0, 0.3, 0.0));
        helper.succeedWhen(() -> {
            helper.assertTrue(be.shownIndex() == 1, "the block entity tick did not cycle the top item");
            TestCleanup.run(helper);
        });
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
        TestCleanup.succeed(helper);
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
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }
}
