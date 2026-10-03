package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.PlacedTemplateBlock;
import com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.PlacedAttractors;
import com.simplebuilding.util.PlacedPlate;
import com.simplebuilding.util.PlacedSmallParts;
import com.simplebuilding.util.PlacedTemplates;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Spieltests der abgelegten Schmiedevorlagen (Besitzer 2026-09-28, {@link PlacedTemplates}):
 * Ablegen mit Schleichen + Rechtsklick auf Boden, Wand und Decke (ohne Schleichen nicht), Wasser
 * zerstoert sie nicht, sie fallen mit allen Komponenten als sie selbst heraus, die Besatz-Aufwertung
 * braucht an der abgelegten Vorlage drei Schlaege, und die Hinweis-Funken kommen nur fuer Spieler mit
 * Leuchttinte oder Glowstonestaub in der Hand.
 */
public final class PlacedTemplateTests {

    /** Zeit fuer das Wasser (fliesst alle 5 Ticks einen Block weiter) und die Drops. */
    public static final int WATER_MAX_TICKS = 60;

    private PlacedTemplateTests() {
    }

    // =====================================================================================
    // Ablegen
    // =====================================================================================

    /**
     * Schleichen + Rechtsklick legt jede Art Vorlage ab - Netherit-Aufwertung, Besatzvorlage, die
     * Enderit-Aufwertung der Mod (ein schlichtes Item) und die leuchtende Vorlage: flach auf den
     * Boden (Oberkante in Blickrichtung), an die Wand (Schauseite zur geklickten Seite) und unter die
     * Decke. Die Block-Entity haelt denselben Stapel samt Namen.
     */
    public static void sneakUsePlacesTemplatesOnTheFloorAgainstTheWallAndUnderTheCeiling(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(4.5, 2.0, 4.5));
        player.setShiftKeyDown(true);

        // Boden
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        ItemStack named = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Anvil note"));
        InteractionResult floor = use(helper, player, named, new BlockPos(1, 1, 1), Direction.UP);
        helper.assertTrue(floor.consumesAction(), "sneak + use on the floor did not place the template: " + floor);
        BlockState floorState = helper.getBlockState(new BlockPos(1, 2, 1));
        helper.assertTrue(floorState.is(ModBlocks.PLACED_SMITHING_TEMPLATE), "no placed template on the floor but " + floorState);
        helper.assertTrue(floorState.getValue(PlacedTemplateBlock.FACE) == AttachFace.FLOOR
                        && floorState.getValue(PlacedTemplateBlock.FACING) == player.getDirection(),
                "the floor template lies " + floorState + " instead of flat with its top edge towards " + player.getDirection());
        ItemStack stored = template(helper, new BlockPos(1, 2, 1));
        helper.assertTrue(ItemStack.isSameItemSameComponents(stored, named) && stored.getCount() == 1,
                "the floor template holds " + stored + " instead of the named netherite upgrade");

        // Wand: die Nordseite eines Steins, die Vorlage zeigt nach Norden
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.STONE);
        use(helper, player, new ItemStack(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE), new BlockPos(3, 2, 2), Direction.NORTH);
        BlockState wall = helper.getBlockState(new BlockPos(3, 2, 1));
        helper.assertTrue(wall.is(ModBlocks.PLACED_SMITHING_TEMPLATE) && wall.getValue(PlacedTemplateBlock.FACE) == AttachFace.WALL
                        && wall.getValue(PlacedTemplateBlock.FACING) == Direction.NORTH,
                "the wall template is " + wall + " instead of hanging on the wall facing north");
        helper.assertTrue(template(helper, new BlockPos(3, 2, 1)).is(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE),
                "the wall template lost its item");

        // Decke
        helper.setBlock(new BlockPos(5, 4, 1), Blocks.STONE);
        use(helper, player, new ItemStack(ModItems.ENDERITE_UPGRADE_TEMPLATE), new BlockPos(5, 4, 1), Direction.DOWN);
        BlockState ceiling = helper.getBlockState(new BlockPos(5, 3, 1));
        helper.assertTrue(ceiling.is(ModBlocks.PLACED_SMITHING_TEMPLATE) && ceiling.getValue(PlacedTemplateBlock.FACE) == AttachFace.CEILING,
                "the enderite upgrade template under the ceiling is " + ceiling);

        // Die leuchtende Vorlage der Mod geht auch.
        helper.setBlock(new BlockPos(1, 1, 4), Blocks.STONE);
        use(helper, player, new ItemStack(ModItems.GLOWING_TRIM_TEMPLATE), new BlockPos(1, 1, 4), Direction.UP);
        helper.assertTrue(template(helper, new BlockPos(1, 2, 4)).is(ModItems.GLOWING_TRIM_TEMPLATE),
                "the glowing trim template was not placed");
        helper.succeed();
    }

    /**
     * Ohne Schleichen bleibt eine Vorlage ein gewoehnliches Item: der Rechtsklick legt nichts ab und
     * gibt PASS zurueck. Andere Items legen auch mit Schleichen nichts ab.
     */
    public static void withoutSneakingTheTemplateKeepsItsNormalBehaviour(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        player.setShiftKeyDown(false);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        ItemStack template = new ItemStack(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
        InteractionResult plain = use(helper, player, template, new BlockPos(1, 1, 1), Direction.UP);
        helper.assertTrue(plain == InteractionResult.PASS, "a right click without sneaking returned " + plain + " instead of PASS");
        helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, 1)).isAir(), "a right click without sneaking placed "
                + helper.getBlockState(new BlockPos(1, 2, 1)));
        helper.assertTrue(template.getCount() == 1, "the template stack changed to " + template);

        player.setShiftKeyDown(true);
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
        InteractionResult paper = use(helper, player, new ItemStack(Items.PAPER), new BlockPos(3, 1, 1), Direction.UP);
        helper.assertTrue(paper == InteractionResult.PASS && helper.getBlockState(new BlockPos(3, 2, 1)).isAir(),
                "sneaking with paper placed something: " + paper + ", " + helper.getBlockState(new BlockPos(3, 2, 1)));
        helper.succeed();
    }

    // =====================================================================================
    // Wasser und Drops
    // =====================================================================================

    /**
     * Fliessendes Wasser laeuft um eine abgelegte Vorlage herum statt sie wegzuspuelen, in eine
     * Wasserquelle abgelegt wird sie wassergefuellt. Abbauen und ein weggenommener Halt geben genau
     * den gespeicherten Stapel zurueck, samt Namen.
     */
    public static void placedTemplatesSurviveWaterAndDropThemselvesWithTheirData(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 5.5));
        player.setShiftKeyDown(true);
        // Becken aus Stein: innen nur die Vorlage (1,2,1) und die Wasserquelle daneben (2,2,1) - das
        // Wasser kann nirgends hin als in die Vorlage.
        basin(helper, 0, 0, 3, 2);
        use(helper, player, new ItemStack(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE), new BlockPos(1, 1, 1), Direction.UP);
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.WATER);

        // In eine Wasserquelle abgelegt (eigenes Becken): wassergefuellt.
        basin(helper, 4, 0, 6, 2);
        helper.setBlock(new BlockPos(5, 2, 1), Blocks.WATER);
        use(helper, player, new ItemStack(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE), new BlockPos(5, 1, 1), Direction.UP);
        BlockState soaked = helper.getBlockState(new BlockPos(5, 2, 1));
        helper.assertTrue(soaked.is(ModBlocks.PLACED_SMITHING_TEMPLATE) && soaked.getValue(PlacedTemplateBlock.WATERLOGGED),
                "a template placed into a water source is " + soaked + " instead of waterlogged");

        // Abbauen: derselbe Stapel mit Namen.
        helper.setBlock(new BlockPos(1, 1, 4), Blocks.STONE);
        ItemStack named = new ItemStack(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Keep me"));
        use(helper, player, named, new BlockPos(1, 1, 4), Direction.UP);
        helper.assertTrue(template(helper, new BlockPos(1, 2, 4)).is(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE), "the named template was not placed");
        helper.getLevel().destroyBlock(helper.absolutePos(new BlockPos(1, 2, 4)), true);
        ItemEntity broken = dropped(helper, Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
        helper.assertTrue(broken != null && ItemStack.isSameItemSameComponents(broken.getItem(), named) && broken.getItem().getCount() == 1,
                "breaking the placed template dropped " + (broken == null ? "nothing (items: " + items(helper) + ")" : broken.getItem())
                        + " instead of the named template");
        // Halt weg: die Vorlage an der Wand faellt mit heraus.
        helper.setBlock(new BlockPos(4, 2, 5), Blocks.STONE);
        use(helper, player, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new BlockPos(4, 2, 5), Direction.NORTH);
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 4)).is(ModBlocks.PLACED_SMITHING_TEMPLATE), "wall template missing");
        helper.setBlock(new BlockPos(4, 2, 5), Blocks.AIR);
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 4)).isAir(), "the wall template stayed without its wall");
        ItemEntity fallen = dropped(helper, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        helper.assertTrue(fallen != null, "the wall template dropped nothing when its wall went away (items: " + items(helper) + ")");

        helper.runAfterDelay(30, () -> {
            BlockState state = helper.getBlockState(new BlockPos(1, 2, 1));
            helper.assertTrue(state.is(ModBlocks.PLACED_SMITHING_TEMPLATE), "flowing water washed the template away: " + state);
            helper.assertTrue(template(helper, new BlockPos(1, 2, 1)).is(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE),
                    "the template under flowing water lost its item");
            helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 1)).is(Blocks.WATER),
                    "the water next to the template is gone, the test proves nothing");
            helper.assertTrue(!state.getValue(PlacedTemplateBlock.WATERLOGGED),
                    "flowing water filled the template like a source would");
            succeed(helper);
        });
    }

    // =====================================================================================
    // Aufwertung mit dem Vorschlaghammer
    // =====================================================================================

    /**
     * Die Besatz-Aufwertung an der abgelegten Vorlage braucht {@link PlacedTemplates#PLACED_HITS}
     * Schlaege: der echte Linksklick-Weg ({@code handleBlockBreakAction}), der Ueberlebens-Haken
     * ({@code attack}) und der Kreativ-Haken ({@code canDestroyBlock}) zaehlen je einen Schlag, erst
     * der dritte wertet auf, der Block bricht dabei nie. Ein Wechsel des Materials faengt von vorn
     * an. Eine Aufwertungsvorlage (keine Besatzvorlage) zaehlt keinen Schlag, und die Hammer-Neigung
     * gilt nur fuer aufwertbare Vorlagen.
     */
    public static void placedTrimTemplatesNeedThreeHammerHits(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        player.setShiftKeyDown(true);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 1, 1), Blocks.STONE);
        use(helper, player, new ItemStack(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE), new BlockPos(1, 1, 1), Direction.UP);
        use(helper, player, new ItemStack(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE), new BlockPos(3, 1, 1), Direction.UP);
        use(helper, player, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new BlockPos(5, 1, 1), Direction.UP);
        player.setShiftKeyDown(false);

        ServerLevel level = helper.getLevel();
        BlockPos first = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos second = helper.absolutePos(new BlockPos(3, 2, 1));
        BlockPos upgrade = helper.absolutePos(new BlockPos(5, 2, 1));
        ItemStack hammer = new ItemStack(ModItems.IRON_SLEDGEHAMMER);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLOWSTONE_DUST, 8));

        helper.assertTrue(PlacedTemplates.isHammerTarget(level, first, player), "a placed trim template is no hammer target");
        helper.assertTrue(!PlacedTemplates.isHammerTarget(level, upgrade, player), "a placed upgrade template counts as hammer target");
        BlockState state = level.getBlockState(first);
        helper.assertTrue(state.getDestroyProgress(player, level, first) == 0.0F,
                "hammer + glowstone still break the placed template (progress " + state.getDestroyProgress(player, level, first) + ")");

        // Der echte Linksklick, dreimal.
        for (int hit = 1; hit <= PlacedTemplates.PLACED_HITS; hit++) {
            player.gameMode.handleBlockBreakAction(first, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, Direction.UP,
                    level.getMaxY(), hit);
            BlockState now = level.getBlockState(first);
            helper.assertTrue(now.is(ModBlocks.PLACED_SMITHING_TEMPLATE), "hit " + hit + " broke the placed template: " + now);
            Item expected = hit < PlacedTemplates.PLACED_HITS ? Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE : ModItems.EMITTING_TRIM_TEMPLATE;
            ItemStack held = helper.getBlockEntity(new BlockPos(1, 2, 1), PlacedTemplateBlockEntity.class).getTemplate();
            helper.assertTrue(held.is(expected), "after hit " + hit + " the template is " + held + " instead of " + expected);
        }
        // Seit der Rahmen-Weg entfallen ist (Q3, 2026-10-02), loest die abgelegte Vorlage den Fortschritt aus.
        var glowUp = level.getServer().getAdvancements().get(net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", "hammer/glow_up"));
        helper.assertTrue(glowUp != null && player.getAdvancements().getOrStartProgress(glowUp).isDone(),
                "upgrading a placed template did not grant the Glow Up advancement");

        // Beide Haken einzeln, mit Materialwechsel dazwischen.
        PlacedTemplateBlockEntity be = helper.getBlockEntity(new BlockPos(3, 2, 1), PlacedTemplateBlockEntity.class);
        level.getBlockState(second).attack(level, second, player);
        helper.assertTrue(be.hits() == 1, "the survival hook counted " + be.hits() + " hits instead of 1");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLOW_INK_SAC, 8));
        level.getBlockState(second).attack(level, second, player);
        helper.assertTrue(be.hits() == 1, "switching to glow ink did not restart the count: " + be.hits());
        boolean destroys = hammer.getItem().canDestroyBlock(hammer, level.getBlockState(second), level, second, player);
        helper.assertTrue(!destroys, "the sledgehammer may destroy a placed template while holding glow ink");
        helper.assertTrue(be.hits() == (player.getAbilities().instabuild ? 2 : 1), "the creative hook counted " + be.hits());
        for (int i = 0; i < PlacedTemplates.PLACED_HITS && be.getTemplate().is(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE); i++) {
            level.getBlockState(second).attack(level, second, player);
        }
        helper.assertTrue(be.getTemplate().is(ModItems.GLOWING_TRIM_TEMPLATE), "glow ink made " + be.getTemplate());

        // Aufwertungsvorlage: kein Schlag, nichts aendert sich.
        level.getBlockState(upgrade).attack(level, upgrade, player);
        PlacedTemplateBlockEntity upgradeBe = helper.getBlockEntity(new BlockPos(5, 2, 1), PlacedTemplateBlockEntity.class);
        helper.assertTrue(upgradeBe.hits() == 0 && upgradeBe.getTemplate().is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                "a hammer hit counted on an upgrade template: " + upgradeBe.hits() + ", " + upgradeBe.getTemplate());

        // Ohne Material in der Nebenhand ist es ein gewoehnlicher Block.
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        helper.assertTrue(!PlacedTemplates.isHammerStance(player) && level.getBlockState(upgrade).getDestroyProgress(player, level, upgrade) > 0.0F,
                "without a catalyst the placed template cannot be mined");
        succeed(helper);
    }

    // =====================================================================================
    // Hinweis
    // =====================================================================================

    /**
     * Eine aufwertbare abgelegte Vorlage zeigt ihre Funken nur einem Spieler in der Naehe, der
     * Glowstonestaub oder Leuchttinte in einer Hand haelt - nicht mit leeren Haenden, nicht aus der
     * Ferne, und eine Aufwertungsvorlage nie.
     */
    public static void hintSparksOnlyShowNearPlayersHoldingGlowstoneOrGlowInk(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        player.setShiftKeyDown(true);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.STONE);
        use(helper, player, new ItemStack(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE), new BlockPos(2, 1, 2), Direction.UP);
        use(helper, player, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new BlockPos(4, 1, 2), Direction.UP);
        player.setShiftKeyDown(false);
        ServerLevel level = helper.getLevel();
        BlockPos trim = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos upgrade = helper.absolutePos(new BlockPos(4, 2, 2));
        PlacedTemplateBlockEntity be = helper.getBlockEntity(new BlockPos(2, 2, 2), PlacedTemplateBlockEntity.class);

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        helper.assertTrue(PlacedTemplates.hintViewer(level, trim) != player, "empty hands got a hint");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLOWSTONE_DUST));
        helper.assertTrue(PlacedTemplates.hintViewer(level, trim) == player, "glowstone dust in the main hand got no hint");
        helper.assertTrue(PlacedTemplates.tryHint(level, trim, be) == player, "tryHint did not fire for the glowstone holder");
        helper.assertTrue(PlacedTemplates.hintViewer(level, upgrade) == null, "an upgrade template gave a hint");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLOW_INK_SAC));
        helper.assertTrue(PlacedTemplates.hintViewer(level, trim) == player, "glow ink in the off hand got no hint");

        // Aus der Ferne nicht.
        Vec3 far = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5 + PlacedTemplates.HINT_RANGE + 3.0));
        player.snapTo(far.x, far.y, far.z, 0.0F, 0.0F);
        helper.assertTrue(PlacedTemplates.hintViewer(level, trim) != player, "a player " + (PlacedTemplates.HINT_RANGE + 3.0)
                + " blocks away got a hint");

        // Der Ton ist gedrosselt.
        long now = level.getGameTime();
        helper.assertTrue(be.takeHintSound(now + 1000, PlacedTemplates.HINT_SOUND_INTERVAL)
                        && !be.takeHintSound(now + 1001, PlacedTemplates.HINT_SOUND_INTERVAL)
                        && be.takeHintSound(now + 1000 + PlacedTemplates.HINT_SOUND_INTERVAL, PlacedTemplates.HINT_SOUND_INTERVAL),
                "the hint sound is not rate limited to once per " + PlacedTemplates.HINT_SOUND_INTERVAL + " ticks");
        succeed(helper);
    }

    // =====================================================================================
    // Name, Trefferform, Blaupausen (Besitzer 2026-09-28)
    // =====================================================================================

    /**
     * Eine abgelegte Vorlage heisst wie die Vorlage selbst, nicht "Abgelegte Schmiedevorlage": die
     * Block-Entity ist {@code Nameable} (Jade fragt dort), Pick-Block liefert die Vorlage, ein
     * Amboss-Name reist mit. Der Block selbst behaelt seinen Namen.
     */
    public static void placedTemplatesCarryTheNameOfTheirTemplate(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        player.setShiftKeyDown(true);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
        ItemStack glowing = new ItemStack(ModItems.GLOWING_TRIM_TEMPLATE);
        use(helper, player, glowing.copy(), new BlockPos(1, 1, 1), Direction.UP);
        ItemStack named = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Anvil note"));
        use(helper, player, named.copy(), new BlockPos(3, 1, 1), Direction.UP);

        for (Object[] check : new Object[][]{{new BlockPos(1, 2, 1), glowing}, {new BlockPos(3, 2, 1), named}}) {
            BlockPos pos = (BlockPos) check[0];
            ItemStack expected = (ItemStack) check[1];
            PlacedTemplateBlockEntity be = helper.getBlockEntity(pos, PlacedTemplateBlockEntity.class);
            String want = expected.getHoverName().getString();
            helper.assertTrue(be.hasCustomName() && be.getCustomName() != null && be.getCustomName().getString().equals(want),
                    "the placed template's custom name is " + be.getCustomName() + " instead of " + want);
            helper.assertTrue(be.getDisplayName().getString().equals(want) && be.getName().getString().equals(want),
                    "the placed template is displayed as " + be.getDisplayName().getString() + " instead of " + want);
            ItemStack picked = helper.getBlockState(pos).getCloneItemStack(helper.getLevel(), helper.absolutePos(pos), true);
            helper.assertTrue(ItemStack.isSameItemSameComponents(picked, expected) && picked.getHoverName().getString().equals(want),
                    "pick block on the placed template gave " + picked);
        }
        helper.assertTrue(!helper.getBlockEntity(new BlockPos(1, 2, 1), PlacedTemplateBlockEntity.class).getDisplayName().getString()
                        .equals(ModBlocks.PLACED_SMITHING_TEMPLATE.getName().getString()),
                "the glowing template still shows the block name " + ModBlocks.PLACED_SMITHING_TEMPLATE.getName().getString());
        succeed(helper);
    }

    /**
     * Die Trefferform ist genau die Platte: je Lage duenn (hoechstens gut 1,5 Pixel) und nur dort, wo
     * die Item-Textur deckt - ein Strahl durch die freie Ecke neben der Vorlage geht durch, einer durch
     * die Mitte trifft. Die Maske der Mod-Vorlage kommt aus ihrer Textur (nicht die volle Flaeche),
     * die Vanilla-Masken aus der Tabelle. Und die Texturen der ablegbaren Mod-Vorlagen haben kein fast
     * durchsichtiges Pixel mehr, das dem Ausstanzen eine Seitenflaeche stiehlt (Loch links in der
     * leuchtenden Vorlage).
     */
    public static void theHitboxCoversOnlyThePixelsOfThePlate(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 5.5));
        player.setShiftKeyDown(true);
        double thin = PlacedPlate.SCALE * PlacedPlate.THICKNESS_SCALE / 16.0 + PlacedPlate.GAP + 1.0E-3;
        // Alle Lagen: duenn an der richtigen Seite, innerhalb des Blocks, nicht leer.
        for (AttachFace face : AttachFace.values()) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                VoxelShape shape = PlacedPlate.shape(ModItems.GLOWING_TRIM_TEMPLATE, face, facing);
                helper.assertTrue(!shape.isEmpty(), "empty shape for " + face + "/" + facing);
                net.minecraft.world.phys.AABB box = shape.bounds();
                Direction normal = face == AttachFace.FLOOR ? Direction.UP : face == AttachFace.CEILING ? Direction.DOWN : facing;
                double depth = switch (normal.getAxis()) {
                    case X -> box.getXsize();
                    case Y -> box.getYsize();
                    case Z -> box.getZsize();
                };
                helper.assertTrue(depth <= thin, face + "/" + facing + " is " + depth + " thick, more than the plate (" + thin + ")");
                double back = normal.getAxisDirection() == Direction.AxisDirection.POSITIVE ? box.min(normal.getAxis()) : 1.0 - box.max(normal.getAxis());
                helper.assertTrue(back < 0.01, face + "/" + facing + " floats " + back + " away from its support");
                helper.assertTrue(box.minX >= 0 && box.minY >= 0 && box.minZ >= 0 && box.maxX <= 1 && box.maxY <= 1 && box.maxZ <= 1,
                        face + "/" + facing + " leaves the block: " + box);
            }
        }
        // Die Maske der Mod-Vorlage kommt aus der Textur: Ecke oben links frei, Mitte deckend.
        short[] mask = PlacedPlate.mask(ModItems.GLOWING_TRIM_TEMPLATE);
        helper.assertTrue(mask != PlacedPlate.FULL && (mask[0] & 0x8000) == 0 && (mask[8] & 0x0100) != 0,
                "the glowing template's mask was not read from its texture: " + java.util.Arrays.toString(mask));
        helper.assertTrue((PlacedPlate.mask(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)[0] & 0xFFFF) == 0,
                "the netherite upgrade's top row should be empty (vanilla table)");

        // In der Welt: Strahl von oben durch die freie Ecke vs. durch die Mitte.
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        use(helper, player, new ItemStack(ModItems.GLOWING_TRIM_TEMPLATE), new BlockPos(1, 1, 1), Direction.UP);
        BlockPos abs = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockState state = helper.getBlockState(new BlockPos(1, 2, 1));
        VoxelShape shape = state.getShape(helper.getLevel(), abs);
        helper.assertTrue(shape.bounds().getYsize() <= thin, "the placed template's shape is " + shape.bounds().getYsize() + " high");
        net.minecraft.world.phys.BlockHitResult centre = shape.clip(new Vec3(abs.getX() + 0.5, abs.getY() + 1.0, abs.getZ() + 0.5),
                new Vec3(abs.getX() + 0.5, abs.getY() - 0.1, abs.getZ() + 0.5), abs);
        helper.assertTrue(centre != null, "a ray through the middle of the template misses it");
        int corners = 0;
        for (double[] c : new double[][]{{0.08, 0.08}, {0.92, 0.08}, {0.08, 0.92}, {0.92, 0.92}}) {
            net.minecraft.world.phys.BlockHitResult hit = shape.clip(new Vec3(abs.getX() + c[0], abs.getY() + 1.0, abs.getZ() + c[1]),
                    new Vec3(abs.getX() + c[0], abs.getY() - 0.1, abs.getZ() + c[1]), abs);
            if (hit == null) {
                corners++;
            }
        }
        // Die Besatzvorlage ist ein schraeges Schild: mindestens zwei Ecken des Blocks sind frei.
        helper.assertTrue(corners >= 2, "only " + corners + " of the block's corners are free, the shape is not the template's outline");
        // Die volle Platte (ohne Block-Entity) ist breiter als die Vorlage.
        VoxelShape full = PlacedPlate.shape(null, AttachFace.FLOOR, state.getValue(PlacedTemplateBlock.FACING));
        helper.assertTrue(full.bounds().getXsize() * full.bounds().getZsize() > shape.bounds().getXsize() * shape.bounds().getZsize() - 1.0E-6,
                "the full plate is smaller than the template");

        // Keine fast durchsichtigen Pixel in den ablegbaren Mod-Texturen.
        List<String> holes = new java.util.ArrayList<>();
        for (Item item : List.of(ModItems.GLOWING_TRIM_TEMPLATE, ModItems.EMITTING_TRIM_TEMPLATE, ModItems.BASIC_UPGRADE_TEMPLATE,
                ModItems.ENDERITE_UPGRADE_TEMPLATE, ModItems.BLUEPRINT)) {
            String path = "/assets/simplebuilding/textures/item/" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath() + ".png";
            try (java.io.InputStream in = PlacedPlate.class.getResourceAsStream(path)) {
                helper.assertTrue(in != null, "missing texture " + path);
                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(in);
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int alpha = image.getRGB(x, y) >>> 24;
                        if (alpha > 0 && alpha < PlacedPlate.SOLID_ALPHA) {
                            holes.add(path + " (" + x + "," + y + ") alpha " + alpha);
                        }
                    }
                }
            } catch (java.io.IOException e) {
                holes.add(path + ": " + e);
            }
        }
        helper.assertTrue(holes.isEmpty(), "nearly transparent pixels leave holes in the extruded plate: " + holes);
        succeed(helper);
    }

    /**
     * Blaupausen legen sich wie Vorlagen ab (eigener Block {@code placed_blueprint}, gleiche
     * Block-Entity): Boden, Wand, Decke; sie heissen wie die Blaupause, haben die duenne Plattenform
     * und fallen beim Abbauen mit allen Komponenten heraus. Ohne Schleichen wird nichts abgelegt.
     */
    public static void blueprintsArePlacedLikeTemplatesAndDropThemselves(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 5.5));
        player.setShiftKeyDown(false);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        ItemStack blueprint = new ItemStack(ModItems.BLUEPRINT);
        blueprint.set(DataComponents.CUSTOM_NAME, Component.literal("Tower plan"));
        InteractionResult plain = use(helper, player, blueprint.copy(), new BlockPos(1, 1, 1), Direction.UP);
        helper.assertTrue(!plain.consumesAction() && helper.getBlockState(new BlockPos(1, 2, 1)).isAir(),
                "a blueprint was placed without sneaking: " + plain);

        player.setShiftKeyDown(true);
        InteractionResult floor = use(helper, player, blueprint.copy(), new BlockPos(1, 1, 1), Direction.UP);
        BlockState state = helper.getBlockState(new BlockPos(1, 2, 1));
        helper.assertTrue(floor.consumesAction() && state.is(ModBlocks.PLACED_BLUEPRINT) && state.getValue(PlacedTemplateBlock.FACE) == AttachFace.FLOOR,
                "sneak + use did not lay the blueprint on the floor: " + floor + ", " + state);
        PlacedTemplateBlockEntity be = helper.getBlockEntity(new BlockPos(1, 2, 1), PlacedTemplateBlockEntity.class);
        helper.assertTrue(ItemStack.isSameItemSameComponents(be.getTemplate(), blueprint), "the placed blueprint holds " + be.getTemplate());
        helper.assertTrue(be.getDisplayName().getString().equals("Tower plan"), "the placed blueprint is called " + be.getDisplayName().getString());
        double thin = PlacedPlate.SCALE * PlacedPlate.THICKNESS_SCALE / 16.0 + PlacedPlate.GAP + 1.0E-3;
        helper.assertTrue(state.getShape(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1))).bounds().getYsize() <= thin,
                "the placed blueprint is not a thin plate");

        helper.setBlock(new BlockPos(3, 2, 2), Blocks.STONE);
        use(helper, player, new ItemStack(ModItems.BLUEPRINT), new BlockPos(3, 2, 2), Direction.NORTH);
        BlockState wall = helper.getBlockState(new BlockPos(3, 2, 1));
        helper.assertTrue(wall.is(ModBlocks.PLACED_BLUEPRINT) && wall.getValue(PlacedTemplateBlock.FACE) == AttachFace.WALL,
                "the blueprint on the wall is " + wall);
        helper.setBlock(new BlockPos(5, 4, 1), Blocks.STONE);
        use(helper, player, new ItemStack(ModItems.BLUEPRINT), new BlockPos(5, 4, 1), Direction.DOWN);
        BlockState ceiling = helper.getBlockState(new BlockPos(5, 3, 1));
        helper.assertTrue(ceiling.is(ModBlocks.PLACED_BLUEPRINT) && ceiling.getValue(PlacedTemplateBlock.FACE) == AttachFace.CEILING,
                "the blueprint under the ceiling is " + ceiling);

        helper.getLevel().destroyBlock(helper.absolutePos(new BlockPos(1, 2, 1)), true);
        ItemEntity broken = dropped(helper, ModItems.BLUEPRINT);
        helper.assertTrue(broken != null && ItemStack.isSameItemSameComponents(broken.getItem(), blueprint) && broken.getItem().getCount() == 1,
                "breaking the placed blueprint dropped " + (broken == null ? "nothing (items: " + items(helper) + ")" : broken.getItem()));
        succeed(helper);
    }

    // =====================================================================================
    // Attractor (Besitzer 2026-09-28)
    // =====================================================================================

    /** Ticks, nach denen das gezogene Item beim abgelegten Attractor angekommen sein muss. */
    public static final int ATTRACTOR_WAIT_TICKS = 80;
    public static final int ATTRACTOR_MAX_TICKS = 120;

    /**
     * Der Attractor legt sich mit Schleichen + Rechtsklick ab wie eine Vorlage (derselbe Block, eigene
     * pixelgenaue Trefferform, Name des Attractors) und zieht dort lose Items im Umkreis zu sich: ein
     * Item vier Bloecke entfernt kommt an, eines ausserhalb von {@link PlacedAttractors#RANGE} bleibt
     * liegen, ein Attractor mit Filter zieht nur sein Item. Abgebaut faellt er mit Filter und Namen heraus.
     */
    public static void placedAttractorsPullLooseItemsTowardThemselves(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(0.5, 2.0, 7.5));
        player.setShiftKeyDown(true);
        ServerLevel level = helper.getLevel();
        // Ebener Steinboden auf y 1: Attractors und Items liegen auf derselben Hoehe.
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        InteractionResult placed = use(helper, player, new ItemStack(ModItems.MAGNET), new BlockPos(1, 1, 1), Direction.UP);
        helper.assertTrue(placed.consumesAction() && helper.getBlockState(new BlockPos(1, 2, 1)).is(ModBlocks.PLACED_SMITHING_TEMPLATE),
                "sneak + use did not place the attractor: " + placed + ", " + helper.getBlockState(new BlockPos(1, 2, 1)));
        PlacedTemplateBlockEntity attractor = helper.getBlockEntity(new BlockPos(1, 2, 1), PlacedTemplateBlockEntity.class);
        helper.assertTrue(attractor.getTemplate().is(ModItems.MAGNET), "the placed block holds " + attractor.getTemplate());
        String name = new ItemStack(ModItems.MAGNET).getHoverName().getString();
        helper.assertTrue(attractor.getDisplayName().getString().equals(name),
                "the placed attractor is called " + attractor.getDisplayName().getString() + " instead of " + name);
        helper.assertTrue(PlacedPlate.mask(ModItems.MAGNET) != PlacedPlate.FULL,
                "the attractor's hitbox is the full plate, not its pixels");

        // Zweiter Attractor mit Filter (Stein) und Namen.
        ItemStack filtered = new ItemStack(ModItems.MAGNET);
        filtered.enchant(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(com.simplebuilding.enchantment.ModEnchantments.CONSTRUCTORS_TOUCH), 1);
        player.setShiftKeyDown(false);
        net.minecraft.nbt.CompoundTag filter = new net.minecraft.nbt.CompoundTag();
        filter.putString("MagnetFilter", "minecraft:stone");
        filtered.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(filter));
        filtered.set(DataComponents.CUSTOM_NAME, Component.literal("Stone keeper"));
        use(helper, player, filtered.copy(), new BlockPos(6, 1, 6), Direction.UP);
        PlacedTemplateBlockEntity keeper = helper.getBlockEntity(new BlockPos(6, 2, 6), PlacedTemplateBlockEntity.class);
        player.setShiftKeyDown(false);

        // Items: nah am ersten (4 Bloecke), weit weg vom ersten aber nah am gefilterten.
        Vec3 nearPos = helper.absoluteVec(new Vec3(5.5, 2.2, 1.5));
        ItemEntity near = new ItemEntity(level, nearPos.x, nearPos.y, nearPos.z, new ItemStack(Items.COBBLESTONE), 0.0, 0.0, 0.0);
        near.setPickUpDelay(40);
        level.addFreshEntity(near);
        Vec3 farPos = helper.absoluteVec(new Vec3(7.0, 2.2, 5.5));
        ItemEntity far = new ItemEntity(level, farPos.x, farPos.y, farPos.z, new ItemStack(Items.DIRT), 0.0, 0.0, 0.0);
        far.setPickUpDelay(40);
        level.addFreshEntity(far);

        BlockPos attractorPos = helper.absolutePos(new BlockPos(1, 2, 1));
        Vec3 target = PlacedAttractors.target(attractor);
        helper.assertTrue(target.distanceTo(far.position()) > PlacedAttractors.range(),
                "test setup broken: the far item is within range of the first attractor");
        int pulled = PlacedAttractors.pull(level, attractorPos, attractor);
        helper.assertTrue(pulled == 1, "the attractor pulled " + pulled + " items instead of exactly the near one");
        helper.assertTrue(near.getDeltaMovement().x < 0.0, "the near item is not pulled toward the attractor: " + near.getDeltaMovement());
        ItemEntity settled = new ItemEntity(level, target.x + 0.2, target.y, target.z,
                new ItemStack(Items.DIAMOND), 0, 0, 0);
        settled.setDeltaMovement(new Vec3(0.05, 0, 0));
        settled.setOnGround(true);
        level.addFreshEntity(settled);
        double previous = 0.05;
        for (int step = 0; step < 100; step++) {
            PlacedAttractors.pull(level, attractorPos, attractor);
            Vec3 motion = settled.getDeltaMovement();
            helper.assertTrue(motion.x >= 0 && motion.x <= previous && motion.y == 0 && motion.z == 0,
                    "placed attractor oscillated at step " + step + ": " + motion);
            settled.setPos(settled.position().add(motion));
            previous = motion.x;
        }
        settled.discard();
        int keeperPulled = PlacedAttractors.pull(level, helper.absolutePos(new BlockPos(6, 2, 6)), keeper);
        helper.assertTrue(keeperPulled == 0, "the stone-filtered attractor pulled " + keeperPulled + " items (dirt next to it)");
        helper.assertTrue(PlacedAttractors.canPull(far, null) && !PlacedAttractors.canPull(far, "minecraft:stone"),
                "the filter check is wrong for dirt");

        // Abbauen: faellt mit Filter und Namen heraus.
        level.destroyBlock(helper.absolutePos(new BlockPos(6, 2, 6)), true);
        ItemEntity dropped = dropped(helper, ModItems.MAGNET);
        helper.assertTrue(dropped != null && ItemStack.isSameItemSameComponents(dropped.getItem(), filtered),
                "breaking the placed attractor dropped " + (dropped == null ? "nothing (items: " + items(helper) + ")" : dropped.getItem())
                        + " instead of the named, filtered attractor");
        if (dropped != null) {
            dropped.discard();
        }

        double farStart = far.position().distanceTo(target);
        // Ab hier zieht die Block-Entity selbst (Server-Tick der Platte).
        helper.runAfterDelay(ATTRACTOR_WAIT_TICKS, () -> {
            double distance = near.position().distanceTo(PlacedAttractors.target(attractor));
            helper.assertTrue(distance < 1.2, "after " + ATTRACTOR_WAIT_TICKS + " ticks the near item is still "
                    + distance + " blocks from the attractor (at " + near.position() + ")");
            helper.assertTrue(far.position().distanceTo(target) > farStart - 0.5,
                    "the item out of range moved toward the attractor");
            succeed(helper);
        });
    }

    // =====================================================================================
    // Abgelegter Oktant
    // =====================================================================================

    /**
     * Abgelegter Oktant (Besitzer 2026-09-29): ungesperrt setzt Schleichen + Rechtsklick weiter die
     * zweite Ecke und legt nichts ab; gesperrt legt derselbe Klick den Oktanten samt Ecken, Form und
     * Sperre als Platte ab. Rechtsklick auf die Platte (echter Weg {@code useItemOn}, leere Hand und mit
     * einem Item ohne Schleichen) blendet die Auswahl fuer genau diesen Spieler ein und wieder aus - ein
     * zweiter Spieler hat seinen eigenen Schalter. Wer eingeblendet hat, steht im Update-Paket der
     * Block-Entity (dort liest der Client, ob er Auswahl und Leuchten zeichnet) und uebersteht Speichern
     * und Laden. Eine abgelegte Schmiedevorlage reagiert nicht auf Rechtsklick.
     */
    public static void lockedOctantsArePlacedAndRightClickTogglesTheOutlinePerPlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer alice = mockPlayer(helper, new Vec3(3.5, 2.0, 5.5));
        ServerPlayer bob = mockPlayer(helper, new Vec3(4.5, 2.0, 5.5));
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 1, 2), Blocks.STONE);
        BlockPos first = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos second = helper.absolutePos(new BlockPos(3, 3, 3));

        // Ungesperrt: Schleichen + Rechtsklick ist die zweite Ecke, nichts wird abgelegt.
        ItemStack unlocked = octant(first, second, false);
        alice.setShiftKeyDown(true);
        use(helper, alice, unlocked, new BlockPos(2, 1, 2), Direction.UP);
        helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 2)).isAir(), "an unlocked octant was put down by a sneak click");
        helper.assertTrue(unlocked.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntArray("Pos2")
                        .map(a -> a.length == 3 && a[0] == helper.absolutePos(new BlockPos(2, 1, 2)).getX()).orElse(false),
                "the unlocked sneak click did not set the second corner");

        // Gesperrt: abgelegt, mit allen Daten.
        ItemStack locked = octant(first, second, true);
        InteractionResult placed = use(helper, alice, locked.copy(), new BlockPos(2, 1, 2), Direction.UP);
        BlockPos rel = new BlockPos(2, 2, 2);
        BlockPos pos = helper.absolutePos(rel);
        helper.assertTrue(placed.consumesAction() && helper.getBlockState(rel).is(ModBlocks.PLACED_SMITHING_TEMPLATE),
                "the locked octant was not put down: " + placed + ", " + helper.getBlockState(rel));
        PlacedTemplateBlockEntity be = helper.getBlockEntity(rel, PlacedTemplateBlockEntity.class);
        helper.assertTrue(ItemStack.isSameItemSameComponents(be.getTemplate(), locked),
                "the placed octant holds " + be.getTemplate() + " instead of the locked octant with its corners");
        helper.assertTrue(PlacedTemplates.isPlacedOctant(level, pos), "the placed octant is not recognised as one");
        helper.assertTrue(be.outlineViewers().isEmpty(), "a freshly placed octant already shows its outline to " + be.outlineViewers());

        // Alice schaltet ein (leere Hand), Bob sieht nichts.
        alice.setShiftKeyDown(false);
        alice.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult on = alice.gameMode.useItemOn(alice, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, plateHit(pos));
        helper.assertTrue(on.consumesAction(), "right-clicking the placed octant returned " + on);
        helper.assertTrue(be.showsOutlineTo(alice.getUUID()) && !be.showsOutlineTo(bob.getUUID()),
                "after Alice's click the outline shows to " + be.outlineViewers());
        // Bob schaltet mit einem Item in der Hand (ohne Schleichen) seinen eigenen Schalter ein.
        ItemStack dirt = new ItemStack(Items.DIRT);
        bob.setItemInHand(InteractionHand.MAIN_HAND, dirt);
        bob.gameMode.useItemOn(bob, level, dirt, InteractionHand.MAIN_HAND, plateHit(pos));
        helper.assertTrue(be.showsOutlineTo(alice.getUUID()) && be.showsOutlineTo(bob.getUUID()),
                "after Bob's click the outline shows to " + be.outlineViewers());
        helper.assertTrue(bob.getMainHandItem().is(Items.DIRT) && bob.getMainHandItem().getCount() == 1,
                "Bob's click placed or used his dirt instead of toggling the outline");
        // Im Update-Paket fuer die Clients - und nach Speichern und Laden noch da.
        CompoundTag update = be.getUpdateTag(level.registryAccess());
        helper.assertTrue(update.contains("OutlineViewers"), "the client update of the placed octant does not say who sees the outline: " + update);
        PlacedTemplateBlockEntity reloaded = new PlacedTemplateBlockEntity(pos, be.getBlockState());
        reloaded.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), be.saveCustomOnly(level.registryAccess())));
        helper.assertTrue(reloaded.showsOutlineTo(alice.getUUID()) && reloaded.showsOutlineTo(bob.getUUID()),
                "who sees the outline did not survive saving and loading: " + reloaded.outlineViewers());
        // Alice schaltet wieder aus, Bob behaelt seinen.
        alice.gameMode.useItemOn(alice, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, plateHit(pos));
        helper.assertTrue(!be.showsOutlineTo(alice.getUUID()) && be.showsOutlineTo(bob.getUUID()),
                "after Alice's second click the outline shows to " + be.outlineViewers());
        bob.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        bob.gameMode.useItemOn(bob, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, plateHit(pos));
        helper.assertTrue(be.outlineViewers().isEmpty(), "after both switched off the outline still shows to " + be.outlineViewers());
        helper.assertTrue(!be.getUpdateTag(level.registryAccess()).contains("OutlineViewers"),
                "an octant nobody watches still sends a viewer list");

        // Eine abgelegte Schmiedevorlage kennt keinen Schalter.
        alice.setShiftKeyDown(true);
        use(helper, alice, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new BlockPos(5, 1, 2), Direction.UP);
        alice.setShiftKeyDown(false);
        BlockPos templatePos = helper.absolutePos(new BlockPos(5, 2, 2));
        PlacedTemplateBlockEntity template = helper.getBlockEntity(new BlockPos(5, 2, 2), PlacedTemplateBlockEntity.class);
        alice.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult ignored = alice.gameMode.useItemOn(alice, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, plateHit(templatePos));
        helper.assertTrue(!ignored.consumesAction() && template.outlineViewers().isEmpty(),
                "right-clicking a placed smithing template toggled something: " + ignored + ", " + template.outlineViewers());
        helper.succeed();
    }

    /** Ein Oktant mit beiden Ecken, gesperrt oder nicht. */
    private static ItemStack octant(BlockPos first, BlockPos second, boolean locked) {
        ItemStack stack = new ItemStack(ModItems.OCTANT);
        CompoundTag nbt = new CompoundTag();
        nbt.putIntArray("Pos1", new int[]{first.getX(), first.getY(), first.getZ()});
        nbt.putIntArray("Pos2", new int[]{second.getX(), second.getY(), second.getZ()});
        nbt.putString("Shape", "SPHERE");
        nbt.putBoolean("Locked", locked);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        return stack;
    }

    /** Treffer auf die Mitte einer am Boden liegenden Platte. */
    private static BlockHitResult plateHit(BlockPos abs) {
        return new BlockHitResult(new Vec3(abs.getX() + 0.5, abs.getY() + 0.05, abs.getZ() + 0.5), Direction.UP, abs, false);
    }

    // =====================================================================================

    /** Rechtsklick mit {@code stack} auf die Seite {@code face} des Blocks {@code on} (relativ). */
    private static InteractionResult use(GameTestHelper helper, Player player, ItemStack stack, BlockPos on, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(on);
        Vec3 hit = Vec3.atCenterOf(abs).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, abs, false)));
    }

    /** Steinbecken: Boden auf y 1 von (x0,z0) bis (x1,z1), Rand auf y 2, innen frei. */
    private static void basin(GameTestHelper helper, int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                boolean rim = x == x0 || x == x1 || z == z0 || z == z1;
                helper.setBlock(new BlockPos(x, 2, z), rim ? Blocks.STONE : Blocks.AIR);
            }
        }
    }

    private static ItemStack template(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, PlacedTemplateBlockEntity.class).getTemplate();
    }

    /** Das erste Item dieser Art, das in der Teststruktur liegt, oder null. */
    private static ItemEntity dropped(GameTestHelper helper, Item item) {
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds())) {
            if (entity.getItem().is(item)) {
                return entity;
            }
        }
        return null;
    }

    private static List<String> items(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).stream()
                .map(entity -> entity.getItem() + "@" + entity.blockPosition().toShortString()).toList();
    }

    private static void succeed(GameTestHelper helper) {
        helper.succeed();
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * Small parts (owner 2026-10-02): sneak + right-click lays a stick or a Stone Pebble on the floor (a small-parts pile)
     * and uses one; server.features.placeVanillaItems off keeps vanilla items in hand, server.features.placeDisabledItems
     * blocks single IDs.
     */
    public static void smallPartsLieDownAndTheServerOptionsGateThem(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        var features = com.simplebuilding.config.ServerTuning.get().features;
        boolean vanilla = features.placeVanillaItems;
        String disabled = features.placeDisabledItems;
        try {
            features.placeVanillaItems = true;
            features.placeDisabledItems = "";
            helper.assertTrue(PlacedTemplates.isPlaceableSmall(new ItemStack(Items.STICK)), "a stick is placeable");
            helper.assertTrue(PlacedTemplates.isPlaceableSmall(new ItemStack(Items.IRON_INGOT)), "an iron ingot is placeable");
            helper.assertTrue(PlacedTemplates.isPlaceableSmall(new ItemStack(ModItems.STONE_PEBBLE)), "a stone pebble is placeable");
            helper.assertFalse(PlacedTemplates.isPlaceableSmall(new ItemStack(Items.DIRT)), "dirt is not a small part");
            ServerPlayer player = mockPlayer(helper, new Vec3(4.5, 2.0, 4.5));
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            BlockPos floor = new BlockPos(1, 1, 1);
            helper.setBlock(floor, Blocks.STONE);
            helper.setBlock(floor.above(), Blocks.AIR);
            player.setShiftKeyDown(true);
            ItemStack sticks = new ItemStack(Items.STICK, 3);
            InteractionResult result = use(helper, player, sticks, floor, Direction.UP);
            helper.assertTrue(result.consumesAction(), "sneak + right-click with a stick answered " + result);
            helper.assertValueEqual(parts(helper, floor.above()), List.of(Items.STICK), "parts lying on the stone");
            helper.assertValueEqual(sticks.getCount(), 2, "sticks left in hand");
            // on a wall a small part still lies alone like a template
            helper.setBlock(floor.north(), Blocks.AIR);
            InteractionResult wall = use(helper, player, sticks, floor, Direction.NORTH);
            helper.assertTrue(wall.consumesAction() && template(helper, floor.north()).is(Items.STICK)
                    && helper.getBlockState(floor.north()).getValue(PlacedTemplateBlock.FACE) == AttachFace.WALL, "no stick lies on the wall: " + wall);
            features.placeVanillaItems = false;
            helper.assertFalse(PlacedTemplates.isPlaceableSmall(new ItemStack(Items.STICK)), "vanilla parts off: stick");
            helper.assertTrue(PlacedTemplates.isPlaceableSmall(new ItemStack(ModItems.STONE_PEBBLE)), "vanilla parts off: the pebble stays placeable");
            features.placeDisabledItems = "simplebuilding:stone_pebble, flint";
            helper.assertFalse(PlacedTemplates.isPlaceableSmall(new ItemStack(ModItems.STONE_PEBBLE)), "listed pebble");
            helper.assertTrue(PlacedTemplates.isPlaceableSmall(new ItemStack(ModItems.FLINT_CHIP)), "unlisted flint chip");
            features.placeVanillaItems = true;
            helper.assertFalse(PlacedTemplates.isPlaceableSmall(new ItemStack(Items.FLINT)), "listed flint without namespace");
        } finally {
            features.placeVanillaItems = vanilla;
            features.placeDisabledItems = disabled;
        }
        helper.succeed();
    }

    /**
     * Placed eggs (owner 2026-10-02): sneak + right-click stands a blue egg up; silk touch gives it back, otherwise it
     * breaks and hatches like a thrown egg - one chick in 8, the chick a baby of the egg's variant.
     */
    public static void placedEggsGoBackWithSilkTouchAndHatchLikeThrownEggs(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        ServerPlayer player = mockPlayer(helper, new Vec3(4.5, 2.0, 4.5));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos floor = new BlockPos(1, 1, 1);
        helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(floor.above(), Blocks.AIR);
        player.setShiftKeyDown(true);
        ItemStack eggs = new ItemStack(Items.BLUE_EGG, 2);
        InteractionResult result = use(helper, player, eggs, floor, Direction.UP);
        helper.assertTrue(result.consumesAction(), "sneak + right-click with a blue egg answered " + result);
        helper.assertValueEqual(parts(helper, floor.above()), List.of(Items.BLUE_EGG), "parts standing on the stone");
        helper.assertValueEqual(eggs.getCount(), 1, "eggs left in hand");
        player.setItemInHand(InteractionHand.MAIN_HAND, silkPickaxe(helper));
        player.gameMode.destroyBlock(helper.absolutePos(floor.above()));
        helper.assertTrue(helper.getBlockState(floor.above()).isAir(), "the silk touch pickaxe did not break the egg");
        helper.assertValueEqual(droppedCount(helper, floor.above(), Items.BLUE_EGG), 1, "blue eggs back from silk touch");
        // the thrown egg's rule, with a random source whose first draws hatch exactly one chick
        BlockPos at = helper.absolutePos(floor);
        net.minecraft.util.RandomSource hatching = null;
        for (long seed = 0; seed < 10_000 && hatching == null; seed++) {
            net.minecraft.util.RandomSource probe = net.minecraft.util.RandomSource.create(seed);
            if (probe.nextInt(8) == 0 && probe.nextInt(32) != 0) {
                hatching = net.minecraft.util.RandomSource.create(seed);
            }
        }
        helper.assertTrue(hatching != null, "no seed hatches one chick");
        int chicks = com.simplebuilding.blocks.custom.PlacedEggBlock.hatch(helper.getLevel(), at.above(), new ItemStack(Items.BLUE_EGG), hatching);
        helper.assertValueEqual(chicks, 1, "chicks from a hatching egg");
        net.minecraft.world.phys.AABB around = new net.minecraft.world.phys.AABB(at.above()).inflate(2.0);
        var chick = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.chicken.Chicken.class, around).stream().findFirst().orElseThrow();
        helper.assertTrue(chick.isBaby(), "the chick is a baby");
        net.minecraft.util.RandomSource miss = null;
        for (long seed = 0; seed < 10_000 && miss == null; seed++) {
            if (net.minecraft.util.RandomSource.create(seed).nextInt(8) != 0) miss = net.minecraft.util.RandomSource.create(seed);
        }
        int none = com.simplebuilding.blocks.custom.PlacedEggBlock.hatch(helper.getLevel(), at.above(), new ItemStack(Items.EGG), miss);
        helper.assertValueEqual(none, 0, "seven eggs in eight do not hatch");
        helper.succeed();
    }

    /**
     * Small parts on one spot (owner 2026-10-02, like sea pickles): sneak + right-click with another part on the pile or
     * on the floor below it adds it, up to 4 in any mix (pebble + flint chip + flint chip + egg); the fifth stays in hand,
     * the server options still gate vanilla parts, the hitbox stays inside the block, and breaking it by hand without
     * Silk Touch gives every lying part back exactly once while the egg breaks.
     */
    public static void smallPartsStackUpToFourInAnyMixAndTheFifthIsRefused(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        var features = com.simplebuilding.config.ServerTuning.get().features;
        boolean vanilla = features.placeVanillaItems;
        String disabled = features.placeDisabledItems;
        try {
            features.placeVanillaItems = true;
            features.placeDisabledItems = "";
            ServerPlayer player = mockPlayer(helper, new Vec3(4.5, 2.0, 4.5));
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            BlockPos floor = new BlockPos(1, 1, 1);
            BlockPos spot = floor.above();
            helper.setBlock(floor, Blocks.STONE);
            helper.setBlock(spot, Blocks.AIR);
            ItemStack pebbles = new ItemStack(ModItems.STONE_PEBBLE, 2);
            ItemStack chips = new ItemStack(ModItems.FLINT_CHIP, 3);
            ItemStack eggs = new ItemStack(Items.EGG, 2);
            helper.assertTrue(use(helper, player, pebbles, floor, Direction.UP).consumesAction(), "the pebble was not placed");
            helper.assertTrue(helper.getBlockState(spot).is(ModBlocks.PLACED_SMALL_PARTS), "no small-parts pile but " + helper.getBlockState(spot));
            helper.assertTrue(use(helper, player, chips, spot, Direction.UP).consumesAction(), "the chip was not added on the pile");
            helper.assertTrue(use(helper, player, chips, floor, Direction.UP).consumesAction(), "the chip was not added from the floor below");
            features.placeVanillaItems = false;
            helper.assertFalse(use(helper, player, eggs, spot, Direction.UP).consumesAction(), "vanilla parts off: the egg was added anyway");
            helper.assertValueEqual(eggs.getCount(), 2, "eggs left after the gated try");
            features.placeVanillaItems = true;
            helper.assertTrue(use(helper, player, eggs, spot, Direction.UP).consumesAction(), "the egg was not added");
            helper.assertValueEqual(parts(helper, spot), List.of(ModItems.STONE_PEBBLE, ModItems.FLINT_CHIP, ModItems.FLINT_CHIP, Items.EGG), "the pile");
            helper.assertValueEqual(pebbles.getCount() + chips.getCount() + eggs.getCount(), 3, "parts left in hand");
            InteractionResult fifth = use(helper, player, pebbles, spot, Direction.UP);
            helper.assertFalse(fifth.consumesAction(), "the fifth part was taken: " + fifth);
            helper.assertFalse(use(helper, player, chips, floor, Direction.UP).consumesAction(), "the fifth part was taken from the floor below");
            helper.assertValueEqual(pebbles.getCount() + chips.getCount(), 2, "the fifth part stays in hand");
            helper.assertValueEqual(parts(helper, spot).size(), PlacedSmallParts.MAX_PARTS, "parts on the full spot");
            BlockPos abs = helper.absolutePos(spot);
            VoxelShape shape = helper.getBlockState(spot).getShape(helper.getLevel(), abs);
            helper.assertFalse(shape.isEmpty(), "the pile has no hitbox");
            var bounds = shape.bounds();
            helper.assertTrue(bounds.minX >= 0 && bounds.minZ >= 0 && bounds.maxX <= 1 && bounds.maxZ <= 1 && bounds.maxY <= 7.0 / 16.0,
                    "the hitbox leaves the block or towers: " + bounds);
            helper.assertTrue(bounds.maxX - bounds.minX > 0.5 && bounds.maxZ - bounds.minZ > 0.5, "the hitbox does not cover the four spots: " + bounds);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.gameMode.destroyBlock(abs);
            helper.assertTrue(helper.getBlockState(spot).isAir(), "the pile was not broken");
            helper.assertValueEqual(droppedCount(helper, spot, ModItems.STONE_PEBBLE), 1, "pebbles dropped");
            helper.assertValueEqual(droppedCount(helper, spot, ModItems.FLINT_CHIP), 2, "flint chips dropped");
            helper.assertValueEqual(droppedCount(helper, spot, Items.EGG), 0, "eggs dropped without silk touch");
        } finally {
            features.placeVanillaItems = vanilla;
            features.placeDisabledItems = disabled;
        }
        helper.succeed();
    }

    /**
     * Breaking a pile (owner 2026-10-02): with Silk Touch every part comes back, eggs included; without a tool (as by an
     * explosion, a piston or a missing floor) only the lying parts drop, and each egg hatches for itself with the thrown
     * egg's rule.
     */
    public static void brokenPilesHatchEachEggLikeThrownEggsAndSilkTouchReturnsThem(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        ServerPlayer player = mockPlayer(helper, new Vec3(4.5, 2.0, 4.5));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos silkSpot = pile(helper, new BlockPos(1, 1, 1), new ItemStack(Items.BLUE_EGG), new ItemStack(Items.BROWN_EGG), new ItemStack(ModItems.STONE_PEBBLE));
        player.setItemInHand(InteractionHand.MAIN_HAND, silkPickaxe(helper));
        player.gameMode.destroyBlock(helper.absolutePos(silkSpot));
        helper.assertTrue(helper.getBlockState(silkSpot).isAir(), "the silk touch pickaxe did not break the pile");
        helper.assertValueEqual(droppedCount(helper, silkSpot, Items.BLUE_EGG), 1, "blue eggs back from silk touch");
        helper.assertValueEqual(droppedCount(helper, silkSpot, Items.BROWN_EGG), 1, "brown eggs back from silk touch");
        helper.assertValueEqual(droppedCount(helper, silkSpot, ModItems.STONE_PEBBLE), 1, "pebbles back from silk touch");

        BlockPos plainSpot = pile(helper, new BlockPos(5, 1, 5), new ItemStack(Items.EGG), new ItemStack(Items.EGG), new ItemStack(ModItems.FLINT_CHIP));
        helper.getLevel().destroyBlock(helper.absolutePos(plainSpot), true);
        helper.assertTrue(helper.getBlockState(plainSpot).isAir(), "the pile was not destroyed");
        helper.assertValueEqual(droppedCount(helper, plainSpot, ModItems.FLINT_CHIP), 1, "flint chips dropped without a tool");
        helper.assertValueEqual(droppedCount(helper, plainSpot, Items.EGG), 0, "eggs dropped without a tool");

        // each egg draws for itself: find a seed, work out what two thrown eggs would hatch, compare
        long seed = -1;
        int expected = 0;
        for (long s = 0; s < 100_000 && seed < 0; s++) {
            int simulated = thrownEggChicks(net.minecraft.util.RandomSource.create(s), 2);
            if (simulated >= 2) {
                seed = s;
                expected = simulated;
            }
        }
        helper.assertTrue(seed >= 0, "no seed hatches both eggs");
        int chicks = PlacedSmallParts.breakEggs(helper.getLevel(), helper.absolutePos(plainSpot),
                List.of(new ItemStack(Items.EGG), new ItemStack(ModItems.FLINT_CHIP), new ItemStack(Items.BLUE_EGG)),
                net.minecraft.util.RandomSource.create(seed));
        helper.assertValueEqual(chicks, expected, "chicks from two hatching eggs");
        helper.succeed();
    }

    /**
     * Old worlds (owner 2026-10-02): a single placed egg ({@code placed_egg}) and a small part lying alone on the floor
     * ({@code placed_smithing_template}) stay as they are and turn into a pile when a part is added; a lying smithing
     * template does not.
     */
    public static void oldPlacedEggsAndLyingSmallPartsTurnIntoPilesWhenPartsAreAdded(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        ServerPlayer player = mockPlayer(helper, new Vec3(4.5, 2.0, 4.5));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        BlockPos eggSpot = new BlockPos(1, 2, 1);
        helper.setBlock(eggSpot.below(), Blocks.STONE);
        helper.setBlock(eggSpot, ModBlocks.PLACED_EGG.defaultBlockState()
                .setValue(com.simplebuilding.blocks.custom.PlacedEggBlock.EGG, com.simplebuilding.blocks.custom.PlacedEggBlock.Egg.BROWN));
        ItemStack pebbles = new ItemStack(ModItems.STONE_PEBBLE, 2);
        helper.assertTrue(use(helper, player, pebbles, eggSpot, Direction.UP).consumesAction(), "the pebble was not added to the old egg");
        helper.assertValueEqual(parts(helper, eggSpot), List.of(Items.BROWN_EGG, ModItems.STONE_PEBBLE), "the old egg's pile");

        BlockPos chipSpot = new BlockPos(3, 2, 1);
        helper.setBlock(chipSpot.below(), Blocks.STONE);
        helper.setBlock(chipSpot, ModBlocks.PLACED_SMITHING_TEMPLATE.defaultBlockState()
                .setValue(PlacedTemplateBlock.FACE, AttachFace.FLOOR).setValue(PlacedTemplateBlock.FACING, Direction.EAST));
        helper.getBlockEntity(chipSpot, PlacedTemplateBlockEntity.class).setTemplate(new ItemStack(ModItems.FLINT_CHIP));
        ItemStack eggs = new ItemStack(Items.EGG, 2);
        helper.assertTrue(use(helper, player, eggs, chipSpot.below(), Direction.UP).consumesAction(), "the egg was not added to the lying chip");
        helper.assertValueEqual(parts(helper, chipSpot), List.of(ModItems.FLINT_CHIP, Items.EGG), "the lying chip's pile");
        helper.assertTrue(helper.getBlockState(chipSpot).getValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.FACING) == Direction.EAST,
                "the pile does not keep the chip's direction");

        BlockPos templateSpot = new BlockPos(5, 2, 1);
        helper.setBlock(templateSpot.below(), Blocks.STONE);
        helper.setBlock(templateSpot, ModBlocks.PLACED_SMITHING_TEMPLATE.defaultBlockState().setValue(PlacedTemplateBlock.FACE, AttachFace.FLOOR));
        helper.getBlockEntity(templateSpot, PlacedTemplateBlockEntity.class).setTemplate(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        helper.assertFalse(use(helper, player, pebbles, templateSpot, Direction.UP).consumesAction(), "a pebble was put onto a smithing template");
        helper.assertTrue(template(helper, templateSpot).is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), "the smithing template was replaced");
        helper.assertValueEqual(pebbles.getCount(), 1, "pebbles left in hand");
        helper.succeed();
    }

    /**
     * The 3D egg (owner 2026-10-02): the cuboids of {@code block/placed_egg_<colour>} (tools/textures/placed_egg_textures.py)
     * are the egg hitbox {@link PlacedSmallParts#EGG_BOXES}, and the item definition the renderer draws it through points
     * at that model.
     */
    public static void theEggModelMatchesTheEggHitbox(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        for (String colour : List.of("white", "blue", "brown")) {
            com.google.gson.JsonObject model = resourceJson(helper, "/assets/simplebuilding/models/block/placed_egg_" + colour + ".json");
            com.google.gson.JsonArray elements = model.getAsJsonArray("elements");
            helper.assertValueEqual(elements.size(), PlacedSmallParts.EGG_BOXES.length, colour + " egg cuboids");
            for (int i = 0; i < elements.size(); i++) {
                float[] box = PlacedSmallParts.EGG_BOXES[i];
                com.google.gson.JsonArray from = elements.get(i).getAsJsonObject().getAsJsonArray("from");
                com.google.gson.JsonArray to = elements.get(i).getAsJsonObject().getAsJsonArray("to");
                float[] expected = {8.0F - box[2], box[0], 8.0F - box[2], 8.0F + box[2], box[1], 8.0F + box[2]};
                float[] actual = {from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat(),
                        to.get(0).getAsFloat(), to.get(1).getAsFloat(), to.get(2).getAsFloat()};
                helper.assertTrue(java.util.Arrays.equals(expected, actual), colour + " egg cuboid " + i + " is "
                        + java.util.Arrays.toString(actual) + ", the hitbox " + java.util.Arrays.toString(expected));
            }
            com.google.gson.JsonObject definition = resourceJson(helper, "/assets/simplebuilding/items/placed_egg_" + colour + ".json");
            helper.assertValueEqual(definition.getAsJsonObject("model").get("model").getAsString(), "simplebuilding:block/placed_egg_" + colour,
                    colour + " egg item definition");
        }
        helper.succeed();
    }

    // =====================================================================================
    // Placeables v2 (Besitzer 2026-10-03): neue Kleinteile, Kerzen und Seegurken gemischt, Partikel
    // =====================================================================================

    /**
     * New small parts (owner 2026-10-03): bones, feathers, arrows, glowstone dust, nether stars ... and the mod's own
     * small materials are in the tag, lie down and mix up to four; glowing ones have a particle, plain ones none.
     */
    public static void theNewSmallPartsLieDownMixAndGlowingOnesHaveParticles(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        List<Item> expected = new java.util.ArrayList<>(List.of(Items.BONE, Items.FEATHER, Items.ARROW, Items.SPECTRAL_ARROW,
                Items.BLAZE_ROD, Items.BREEZE_ROD, Items.GLOWSTONE_DUST, Items.GLOW_INK_SAC, Items.PRISMARINE_CRYSTALS,
                Items.NETHER_STAR, Items.RABBIT_FOOT, Items.TURTLE_SCUTE, Items.ARMADILLO_SCUTE, Items.DISC_FRAGMENT_5, Items.GHAST_TEAR));
        for (Item own : java.util.Arrays.asList(ModItems.NIHILITH_SHARD, ModItems.ASTRALIT_DUST, ModItems.ENDER_QUARTZ, ModItems.RAW_ENDERITE,
                ModItems.ENDERITE_SCRAP, ModItems.CRACKED_DIAMOND, ModItems.SAGE_ORB)) {
            if (own != null) {
                expected.add(own);
            }
        }
        for (Item item : expected) {
            helper.assertTrue(new ItemStack(item).is(com.simplebuilding.util.ModTags.Items.PLACEABLE_SMALL), item + " is not in placeable_small");
        }
        helper.assertFalse(new ItemStack(Items.CANDLE).is(com.simplebuilding.util.ModTags.Items.PLACEABLE_SMALL),
                "candles must stay out of the tag (alone they stay vanilla)");
        ServerPlayer player = mockPlayer(helper, new Vec3(4.5, 2.0, 4.5));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        BlockPos floor = new BlockPos(1, 1, 1);
        helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(floor.above(), Blocks.AIR);
        for (Item item : List.of(Items.BONE, Items.FEATHER, Items.GLOWSTONE_DUST, Items.NETHER_STAR)) {
            helper.assertTrue(use(helper, player, new ItemStack(item, 2), floor, Direction.UP).consumesAction(), item + " was not laid down");
        }
        helper.assertValueEqual(parts(helper, floor.above()), List.of(Items.BONE, Items.FEATHER, Items.GLOWSTONE_DUST, Items.NETHER_STAR),
                "the mixed new parts");
        helper.assertTrue(com.simplebuilding.util.PlacedPartParticles.glowOf(new ItemStack(Items.GLOWSTONE_DUST)) != null, "glowstone dust has no particle");
        helper.assertTrue(com.simplebuilding.util.PlacedPartParticles.glowOf(new ItemStack(Items.NETHER_STAR)) != null, "the nether star has no particle");
        if (ModItems.SAGE_ORB != null) {
            helper.assertTrue(com.simplebuilding.util.PlacedPartParticles.glowOf(new ItemStack(ModItems.SAGE_ORB)) != null, "the sage orb has no particle");
        }
        helper.assertTrue(com.simplebuilding.util.PlacedPartParticles.glowOf(new ItemStack(Items.BONE)) == null, "a bone glows");
        helper.assertValueEqual(helper.getBlockState(floor.above()).getLightEmission(), 0, "light of a pile without candles or pickles");
        helper.succeed();
    }

    /**
     * Candles and sea pickles (owner 2026-10-03): alone they stay the vanilla blocks; sneak + right-click with another part
     * on a vanilla candle (or the floor below it) turns it into a pile - candle + 2 pebbles + egg - keeping the candle,
     * a same-coloured candle stays vanilla; a lit vanilla candle stays lit in the pile; breaking gives every part back.
     */
    public static void candlesAndSeaPicklesMixWithSmallPartsOnlyWhenMixed(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        ServerPlayer player = mockPlayer(helper, new Vec3(4.5, 2.0, 4.5));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        // alone: a candle placed on bare stone is the vanilla candle
        BlockPos alone = new BlockPos(5, 1, 1);
        helper.setBlock(alone, Blocks.STONE);
        helper.setBlock(alone.above(), Blocks.AIR);
        helper.assertTrue(use(helper, player, new ItemStack(Items.CANDLE, 2), alone, Direction.UP).consumesAction(), "the candle was not placed");
        helper.assertTrue(helper.getBlockState(alone.above()).is(Blocks.CANDLE), "a lone candle is not vanilla but " + helper.getBlockState(alone.above()));
        ItemStack sameCandles = new ItemStack(Items.CANDLE, 2);
        use(helper, player, sameCandles, alone.above(), Direction.UP);
        helper.assertTrue(helper.getBlockState(alone.above()).is(Blocks.CANDLE), "a same-coloured candle turned the candle into a pile");
        helper.assertValueEqual(sameCandles.getCount(), 2, "same-coloured candles left");

        // mixed: candle + pebble (on the candle) + pebble (from the floor) + egg
        BlockPos floor = new BlockPos(1, 1, 1);
        BlockPos spot = floor.above();
        helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(spot, Blocks.CANDLE);
        ItemStack pebbles = new ItemStack(ModItems.STONE_PEBBLE, 3);
        helper.assertTrue(use(helper, player, pebbles, spot, Direction.UP).consumesAction(), "the pebble did not mix with the candle");
        helper.assertTrue(helper.getBlockState(spot).is(ModBlocks.PLACED_SMALL_PARTS), "no pile but " + helper.getBlockState(spot));
        helper.assertTrue(use(helper, player, pebbles, floor, Direction.UP).consumesAction(), "the pebble was not added from the floor");
        helper.assertTrue(use(helper, player, new ItemStack(Items.EGG), spot, Direction.UP).consumesAction(), "the egg was not added");
        helper.assertValueEqual(parts(helper, spot), List.of(Items.CANDLE, ModItems.STONE_PEBBLE, ModItems.STONE_PEBBLE, Items.EGG), "the mix");
        helper.assertValueEqual(helper.getBlockState(spot).getValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.CANDLES), 1, "candle count");
        helper.assertFalse(use(helper, player, new ItemStack(Items.CANDLE), spot, Direction.UP).consumesAction(), "a fifth part (candle) was taken");

        // a lit vanilla candle pair stays lit, a candle added to a pile joins it
        BlockPos litSpot = new BlockPos(3, 2, 1);
        helper.setBlock(litSpot.below(), Blocks.STONE);
        helper.setBlock(litSpot, Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 2)
                .setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
        helper.assertTrue(use(helper, player, new ItemStack(ModItems.FLINT_CHIP), litSpot, Direction.UP).consumesAction(), "the chip did not mix");
        BlockState lit = helper.getBlockState(litSpot);
        helper.assertTrue(lit.is(ModBlocks.PLACED_SMALL_PARTS) && lit.getValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.LIT),
                "the lit candles went out: " + lit);
        helper.assertValueEqual(lit.getLightEmission(), 6, "light of two lit candles in a pile");
        Item redCandle = Items.DYED_CANDLE.pick(net.minecraft.world.item.DyeColor.RED);
        helper.assertTrue(use(helper, player, new ItemStack(redCandle), litSpot, Direction.UP).consumesAction(), "a red candle was not added to the pile");
        helper.assertValueEqual(helper.getBlockState(litSpot).getLightEmission(), 9, "light of three lit candles");

        // breaking by hand gives every part back (candles as candles)
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.gameMode.destroyBlock(helper.absolutePos(litSpot));
        helper.assertTrue(helper.getBlockState(litSpot).isAir(), "the pile was not broken");
        helper.assertValueEqual(droppedCount(helper, litSpot, Items.CANDLE), 2, "candles dropped");
        helper.assertValueEqual(droppedCount(helper, litSpot, redCandle), 1, "red candles dropped");
        helper.assertValueEqual(droppedCount(helper, litSpot, ModItems.FLINT_CHIP), 1, "flint chips dropped");
        helper.succeed();
    }

    /**
     * Light and fire on a mixed pile (owner 2026-10-03): flint and steel lights the candles (and wears), an empty hand puts
     * them out, water puts them out and keeps them from being lit; sea pickles glow 3 + 3 each only under water (a wet
     * vanilla pickle pair mixed with a pebble glows 9), dry they are dark.
     */
    public static void mixedCandlesLightAndGoOutAndPicklesGlowOnlyUnderWater(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        ServerPlayer player = mockPlayer(helper, new Vec3(4.5, 2.0, 4.5));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos spot = pile(helper, new BlockPos(1, 1, 1), new ItemStack(Items.CANDLE), new ItemStack(ModItems.STONE_PEBBLE));
        helper.assertValueEqual(helper.getBlockState(spot).getLightEmission(), 0, "light of unlit candles");
        ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
        player.setItemInHand(InteractionHand.MAIN_HAND, flint);
        InteractionResult lighting = helper.getBlockState(spot).useItemOn(flint, helper.getLevel(), player, InteractionHand.MAIN_HAND, hitTop(helper, spot));
        helper.assertTrue(lighting.consumesAction(), "flint and steel answered " + lighting);
        helper.assertTrue(helper.getBlockState(spot).getValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.LIT), "the candle was not lit");
        helper.assertValueEqual(helper.getBlockState(spot).getLightEmission(), 3, "light of one lit candle");
        helper.assertValueEqual(flint.getDamageValue(), 1, "flint and steel wear");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult out = helper.getBlockState(spot).useItemOn(ItemStack.EMPTY, helper.getLevel(), player, InteractionHand.MAIN_HAND, hitTop(helper, spot));
        helper.assertTrue(out.consumesAction(), "the empty hand answered " + out);
        helper.assertFalse(helper.getBlockState(spot).getValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.LIT), "the candle still burns");
        helper.assertValueEqual(helper.getBlockState(spot).getLightEmission(), 0, "light after putting it out");

        // water puts it out, and wet candles do not light
        helper.setBlock(spot, helper.getBlockState(spot).setValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.LIT, true));
        BlockPos abs = helper.absolutePos(spot);
        BlockState burning = helper.getBlockState(spot);
        helper.assertTrue(((net.minecraft.world.level.block.SimpleWaterloggedBlock) burning.getBlock())
                .placeLiquid(helper.getLevel(), abs, burning, net.minecraft.world.level.material.Fluids.WATER.getSource(false)), "water did not fill the pile");
        BlockState wet = helper.getBlockState(spot);
        helper.assertTrue(wet.getValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.WATERLOGGED)
                && !wet.getValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.LIT), "water did not put the candle out: " + wet);
        helper.assertFalse(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.canLight(wet), "a wet candle can be lit");

        // pickles: a wet vanilla pair mixed with a pebble glows 9, a dry mix is dark
        player.setShiftKeyDown(true);
        BlockPos pickleSpot = new BlockPos(3, 2, 1);
        basin(helper, 2, 0, 4, 2);
        helper.setBlock(pickleSpot.below(), Blocks.STONE);
        helper.setBlock(pickleSpot, Blocks.SEA_PICKLE.defaultBlockState().setValue(net.minecraft.world.level.block.SeaPickleBlock.PICKLES, 2)
                .setValue(net.minecraft.world.level.block.SeaPickleBlock.WATERLOGGED, true));
        helper.assertTrue(use(helper, player, new ItemStack(ModItems.STONE_PEBBLE), pickleSpot, Direction.UP).consumesAction(),
                "the pebble did not mix with the pickles");
        BlockState pickles = helper.getBlockState(pickleSpot);
        helper.assertTrue(pickles.is(ModBlocks.PLACED_SMALL_PARTS) && pickles.getValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.WATERLOGGED),
                "no wet pile but " + pickles);
        helper.assertValueEqual(parts(helper, pickleSpot), List.of(Items.SEA_PICKLE, Items.SEA_PICKLE, ModItems.STONE_PEBBLE), "the pickle mix");
        helper.assertValueEqual(pickles.getLightEmission(), 9, "light of two wet pickles");
        BlockPos dry = pile(helper, new BlockPos(6, 1, 5), new ItemStack(Items.SEA_PICKLE), new ItemStack(ModItems.FLINT_CHIP));
        helper.assertValueEqual(helper.getBlockState(dry).getLightEmission(), 0, "light of a dry pickle");
        helper.assertValueEqual(helper.getBlockState(dry).getValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.PICKLES), 1, "pickle count");
        helper.succeed();
    }

    /**
     * Save compatibility (owner 2026-10-03): a pile saved before candles and pickles (only facing and waterlogged) loads as
     * the pile with lit=false, candles=0, pickles=0 and no light; the candle and pickle item definitions the renderer draws
     * through point at the vanilla block models.
     */
    public static void oldPilesLoadUnlitAndTheCandleModelsAreTheVanillaOnes(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        // Ein heutiger Zustand, als NBT geschrieben, dann ohne die neuen Eigenschaften - so steht er in alten Welten.
        CompoundTag tag = net.minecraft.nbt.NbtUtils.writeBlockState(ModBlocks.PLACED_SMALL_PARTS.defaultBlockState()
                .setValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.FACING, Direction.EAST)
                .setValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.LIT, true)
                .setValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.CANDLES, 2));
        CompoundTag properties = null;
        for (String key : tag.keySet()) {
            if (tag.get(key) instanceof CompoundTag compound && compound.contains("facing")) {
                properties = compound;
            }
        }
        helper.assertTrue(properties != null, "no properties in " + tag);
        properties.remove("lit");
        properties.remove("candles");
        properties.remove("pickles");
        // So liest auch die Welt Blockzustaende aus NBT: fehlende Eigenschaften bekommen den Standardwert.
        BlockState old = net.minecraft.nbt.NbtUtils.readBlockState(
                helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), tag);
        helper.assertTrue(old.is(ModBlocks.PLACED_SMALL_PARTS), "the old pile did not load: " + old);
        helper.assertTrue(!old.getValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.LIT)
                && old.getValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.CANDLES) == 0
                && old.getValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.PICKLES) == 0
                && old.getValue(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock.FACING) == Direction.EAST, "old pile state " + old);
        helper.assertValueEqual(old.getLightEmission(), 0, "light of an old pile");
        int candles = 0;
        for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (!PlacedSmallParts.isCandle(new ItemStack(item))) {
                continue;
            }
            candles++;
            String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
            for (String lit : List.of("", "_lit")) {
                com.google.gson.JsonObject definition = resourceJson(helper, "/assets/simplebuilding/items/placed_" + path + lit + ".json");
                helper.assertValueEqual(definition.getAsJsonObject("model").get("model").getAsString(), "minecraft:block/" + path + "_one_candle" + lit,
                        path + lit + " item definition");
            }
        }
        helper.assertValueEqual(candles, 17, "vanilla candles");
        for (String pickle : List.of("sea_pickle", "dead_sea_pickle")) {
            com.google.gson.JsonObject definition = resourceJson(helper, "/assets/simplebuilding/items/placed_" + pickle + ".json");
            helper.assertValueEqual(definition.getAsJsonObject("model").get("model").getAsString(), "minecraft:block/" + pickle, pickle + " item definition");
        }
        helper.succeed();
    }

    /** Ein Treffer von oben auf die Mitte des Blocks {@code pos} (relativ). */
    private static BlockHitResult hitTop(GameTestHelper helper, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        return new BlockHitResult(new Vec3(abs.getX() + 0.5, abs.getY() + 0.05, abs.getZ() + 0.5), Direction.UP, abs, false);
    }

    // =====================================================================================

    /** Die Items des Haeufchens an {@code pos} (relativ), leer, wenn dort keins liegt. */
    private static List<Item> parts(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity be
                ? be.parts().stream().map(ItemStack::getItem).toList() : List.of();
    }

    /** Ein Haeufchen mit diesen Teilen auf Stein an {@code floor} (relativ); liefert seinen Platz. */
    private static BlockPos pile(GameTestHelper helper, BlockPos floor, ItemStack... stacks) {
        helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(floor.above(), ModBlocks.PLACED_SMALL_PARTS);
        helper.getBlockEntity(floor.above(), com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity.class).setParts(List.of(stacks));
        return floor.above();
    }

    /** Wie viele Items dieser Art um {@code pos} (relativ) herum liegen. */
    private static int droppedCount(GameTestHelper helper, BlockPos pos, Item item) {
        net.minecraft.world.phys.AABB around = new net.minecraft.world.phys.AABB(helper.absolutePos(pos)).inflate(1.5);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream()
                .filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static ItemStack silkPickaxe(GameTestHelper helper) {
        ItemStack silk = new ItemStack(Items.IRON_PICKAXE);
        silk.enchant(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
        return silk;
    }

    /** Was {@code eggs} geworfene Eier mit diesen Zufallszahlen schluepfen lassen (dieselben Ziehungen wie {@code PlacedEggBlock#hatch}). */
    private static int thrownEggChicks(net.minecraft.util.RandomSource random, int eggs) {
        int chicks = 0;
        for (int egg = 0; egg < eggs; egg++) {
            if (random.nextInt(8) != 0) {
                continue;
            }
            int count = random.nextInt(32) == 0 ? 4 : 1;
            for (int i = 0; i < count; i++) {
                random.nextFloat();
            }
            chicks += count;
        }
        return chicks;
    }

    private static com.google.gson.JsonObject resourceJson(GameTestHelper helper, String path) {
        try (java.io.InputStream in = PlacedTemplateTests.class.getResourceAsStream(path)) {
            helper.assertTrue(in != null, "missing resource " + path);
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }
}
