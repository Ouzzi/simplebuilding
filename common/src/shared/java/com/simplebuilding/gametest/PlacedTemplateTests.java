package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.PlacedTemplateBlock;
import com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.PlacedAttractors;
import com.simplebuilding.util.PlacedPlate;
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
     * Small parts (owner 2026-10-02): sneak + right-click lays a stick or a Stone Pebble flat like a template and uses one;
     * server.features.placeVanillaItems off keeps vanilla items in hand, server.features.placeDisabledItems blocks single IDs.
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
            net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            BlockPos floor = new BlockPos(1, 1, 1);
            helper.setBlock(floor, Blocks.STONE);
            helper.setBlock(floor.above(), Blocks.AIR);
            player.setShiftKeyDown(true);
            ItemStack sticks = new ItemStack(Items.STICK, 3);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sticks);
            BlockPos at = helper.absolutePos(floor);
            net.minecraft.world.InteractionResult result = sticks.useOn(new net.minecraft.world.item.context.UseOnContext(player,
                    net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(
                            net.minecraft.world.phys.Vec3.atCenterOf(at).add(0, 0.5, 0), net.minecraft.core.Direction.UP, at, false)));
            helper.assertTrue(result.consumesAction(), "sneak + right-click with a stick answered " + result);
            helper.assertTrue(PlacedTemplates.templateAt(helper.getLevel(), at.above()).is(Items.STICK), "no stick lies on the stone");
            helper.assertValueEqual(sticks.getCount(), 2, "sticks left in hand");
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
    public static void placedEggsGoBackWithSilkTouchAndHatchLikeAThrownEgg(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos floor = new BlockPos(1, 1, 1);
        helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(floor.above(), Blocks.AIR);
        player.setShiftKeyDown(true);
        ItemStack eggs = new ItemStack(Items.BLUE_EGG, 2);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, eggs);
        BlockPos at = helper.absolutePos(floor);
        net.minecraft.world.InteractionResult result = eggs.useOn(new net.minecraft.world.item.context.UseOnContext(player,
                net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(
                        net.minecraft.world.phys.Vec3.atCenterOf(at).add(0, 0.5, 0), net.minecraft.core.Direction.UP, at, false)));
        helper.assertTrue(result.consumesAction(), "sneak + right-click with a blue egg answered " + result);
        net.minecraft.world.level.block.state.BlockState placed = helper.getLevel().getBlockState(at.above());
        helper.assertTrue(placed.is(ModBlocks.PLACED_EGG) && placed.getValue(com.simplebuilding.blocks.custom.PlacedEggBlock.EGG)
                == com.simplebuilding.blocks.custom.PlacedEggBlock.Egg.BLUE, "no blue egg stands on the stone: " + placed);
        helper.assertValueEqual(eggs.getCount(), 1, "eggs left in hand");
        ItemStack silk = new ItemStack(Items.IRON_PICKAXE);
        silk.enchant(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
        placed.spawnAfterBreak(helper.getLevel(), at.above(), silk, true);
        net.minecraft.world.phys.AABB around = new net.minecraft.world.phys.AABB(at.above()).inflate(2.0);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, around).stream()
                .anyMatch(e -> e.getItem().is(Items.BLUE_EGG)), "silk touch did not give the blue egg back");
        // the thrown egg's rule, with a random source whose first draws hatch exactly one chick
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
        var chick = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.chicken.Chicken.class, around).stream().findFirst().orElseThrow();
        helper.assertTrue(chick.isBaby(), "the chick is a baby");
        int none = 0;
        net.minecraft.util.RandomSource miss = null;
        for (long seed = 0; seed < 10_000 && miss == null; seed++) {
            if (net.minecraft.util.RandomSource.create(seed).nextInt(8) != 0) miss = net.minecraft.util.RandomSource.create(seed);
        }
        none = com.simplebuilding.blocks.custom.PlacedEggBlock.hatch(helper.getLevel(), at.above(), new ItemStack(Items.EGG), miss);
        helper.assertValueEqual(none, 0, "seven eggs in eight do not hatch");
        helper.succeed();
    }
}
