package com.simplebuildingtools.test;

import com.simplebuildingtools.blueprint.BlueprintBuilder;
import com.simplebuildingtools.blueprint.BlueprintCode;
import com.simplebuildingtools.blueprint.BlueprintContent;
import com.simplebuildingtools.blueprint.BlueprintEntity;
import com.simplebuildingtools.blueprint.BlueprintModel;
import com.simplebuildingtools.blueprint.BlueprintScanner;
import com.simplebuildingtools.component.BtComponents;
import com.simplebuildingtools.items.BtItems;
import com.simplebuildingtools.items.custom.BlueprintItem;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * N77 Teil 1 (Plan {@code docs/ai/PLAN-N77-SCAN-ENTITIES-2026-11-16.md}): die Blaupause nimmt Item-Frames, Leucht-Rahmen,
 * Ruestungsstaender und KI-lose Mobs mit - Scan, Code und Bau. Alte Codes (auch der Testzentrale-Scan des Besitzers vom 16.11.)
 * parsen und schreiben sich unveraendert.
 */
public final class BlueprintEntityTests {
    private BlueprintEntityTests() {
    }

    /** Rasterpunkt, an dem die Szene liegt (Ecke der Scan-Box). */
    private static final BlockPos LOW = new BlockPos(1, 1, 1);
    private static final BlockPos HIGH = new BlockPos(6, 3, 6);
    private static final BlockPos STONE_A = new BlockPos(3, 2, 3);
    private static final BlockPos STONE_B = new BlockPos(5, 1, 5);

    // =====================================================================================
    // CODE (ohne Welt)
    // =====================================================================================

    /**
     * Der Entity-Abschnitt ist optional: alte Codes (der Testzentrale-Scan des Besitzers) parsen fehlerfrei und schreiben sich
     * byte-gleich; eine Zeile je Entity parst, schreibt sich kanonisch und kommt gleich zurueck; Fehler werden gemeldet, nicht
     * geworfen.
     *
     * <p><strong>Was diesen Test bricht:</strong> ein Serialisierer, der ohne Entities etwas anderes schreibt, ein Parser, der
     * {@code @}-Zeilen nicht kennt oder bei kaputten Items abstuerzt, Kommentare in Namen, verlorene Felder.
     */
    public static void entityCodeParsesAndOldCodesStayIdentical(GameTestHelper helper) {
        // Besitzer-Scan 16.11. (30x3x119, 6896 Bloecke, ohne Frames).
        String owner = readFixture(helper, "blueprint/tz-owner-1611-layout-scan.txt").stripTrailing();
        Set<String> missingMods = new HashSet<>();
        for (String line : owner.split("\n")) {
            String word = line.startsWith("$") ? line.substring(line.indexOf('=') + 1).strip() : line.split(" ")[0];
            int colon = word.indexOf(':');
            if (!line.startsWith("#") && colon > 0 && !namespaceLoaded(word.substring(0, colon))) {
                missingMods.add(word.substring(0, colon));
            }
        }
        BlueprintCode.ParseResult parsed = BlueprintCode.parse(owner);
        if (missingMods.isEmpty()) {
            helper.assertTrue(parsed.ok(), "the owner's scan has problems: " + parsed.problems());
            helper.assertTrue(parsed.model().size() == 6896 && parsed.model().sizeX() == 30 && parsed.model().sizeY() == 3
                    && parsed.model().sizeZ() == 119, "the owner's scan changed: " + parsed.model());
            helper.assertTrue(BlueprintCode.serialize(parsed.model()).equals(owner), "the owner's scan does not write back byte for byte");
        } else {
            // Ohne die Nachbarmods: nur deren Bloecke sind unbekannt, alles andere parst wie zuvor.
            for (BlueprintCode.Problem p : parsed.problems()) {
                String id = p.args().isEmpty() ? "" : p.args().get(0);
                helper.assertTrue(p.key().equals("unknown_block") && id.contains(":") && missingMods.contains(id.substring(0, id.indexOf(':'))),
                        "the owner's scan has a problem beyond the missing mods " + missingMods + ": " + p);
            }
            helper.assertTrue(parsed.model().size() > 6000, "the owner's scan lost blocks: " + parsed.model());
        }
        helper.assertTrue(parsed.model().entities().isEmpty(), "the owner's scan suddenly has entities");
        BlueprintModel old = BlueprintCode.parse("stone 0..2,0,0\noak_stairs[facing=east] 1,1,0").model();
        helper.assertTrue(BlueprintCode.serialize(old).equals("# 3x2x1, 4 blocks\nstone 0..2,0,0\noak_stairs[facing=east] 1,1,0"),
                "an old code writes differently: " + BlueprintCode.serialize(old));

        String code = String.join("\n",
                "stone 2,1,2",
                "@item_frame 2,1,1 north diamond_sword[enchantments={\"minecraft:sharpness\":5},custom_name=\"Klinge #1\"] rot=2 invisible fixed # a comment",
                "@glow_item_frame 2,2,2 up torch rot=7",
                "@item_frame 2,0,2 down",
                "@armor_stand 0.5,1,3.5 yaw=-45.5 head=iron_helmet hand=stone_sword*1 head_pose=-10,20,0 small arms noplate",
                "@pig 4.25,1,0.5 yaw=90 name=\"Bob\" noai silent invulnerable");
        BlueprintCode.ParseResult result = BlueprintCode.parse(code);
        helper.assertTrue(result.ok(), "the entity code has problems: " + result.problems());
        BlueprintModel model = result.model();
        helper.assertTrue(model.size() == 1 && model.entities().size() == 5, "parsed " + model);
        BlueprintEntity frame = model.entities().get(0);
        helper.assertTrue(frame.kind() == BlueprintEntity.Kind.FRAME && frame.facing() == Direction.NORTH && frame.rotation() == 2
                && frame.flags().equals(List.of("invisible", "fixed")) && frame.item().contains("Klinge #1"),
                "the first frame lost a field: " + frame.line());
        helper.assertTrue(model.minX() == 0 && model.minY() == 0 && model.minZ() == 0 && model.maxX() == 4 && model.maxZ() == 3,
                "entities do not count for the bounding box: " + model.minX() + "," + model.minY() + "," + model.minZ() + " .. "
                        + model.maxX() + "," + model.maxY() + "," + model.maxZ());
        String written = BlueprintCode.serialize(model);
        helper.assertTrue(written.startsWith("# 5x3x4, 1 blocks, 5 entities\n"), "header: " + written);
        helper.assertTrue(written.contains("\n@armor_stand 0.5,1,3.5 yaw=-45.5 head=iron_helmet hand=stone_sword head_pose=-10,20,0 small arms noplate"),
                "the armor stand is not written canonically:\n" + written);
        BlueprintCode.ParseResult back = BlueprintCode.parse(written);
        helper.assertTrue(back.ok() && back.model().equals(model) && BlueprintCode.serialize(back.model()).equals(written),
                "the entity code does not round-trip:\n" + written + "\n" + back.problems());

        // Fehler: gemeldet, nicht geworfen, und die Zeile bleibt aus dem Modell.
        String[][] bad = {
                {"@no_such_entity 0,0,0", "entity_unknown"},
                {"@item_frame 0,0,0 up yaw=5", "entity_bad_token"},
                {"@item_frame 0,0,0", "entity_usage"},
                {"@item_frame 0,0,0 sideways", "entity_facing"},
                {"@item_frame 0,0,0 up rot=8", "entity_value"},
                {"@item_frame 0,0.5,0 up", "entity_position"},
                {"@armor_stand 0,256,0", "entity_position"},
                {"@item_frame 0,0,0 up no_such_item", "entity_item_unknown"},
                {"@item_frame 0,0,0 up stone[no_such_component=1]", "entity_component_unknown"},
                {"@item_frame 0,0,0 up stone[custom_name={]", "entity_item_syntax"},
                {"@item_frame 0,0,0 up stone*0", "entity_item_syntax"},
                {"@item_frame 0,0,0 up stone small", "entity_bad_token"},
                {"@pig 0,0,0 rot=1", "entity_bad_token"},
                {"@item_frame 0,0,0 up stone dirt", "entity_bad_token"},
                {"@armor_stand 0,0,0 name={", "entity_item_syntax"},
        };
        for (String[] c : bad) {
            BlueprintCode.ParseResult r = BlueprintCode.parse(c[0]);
            List<String> keys = r.problems().stream().map(BlueprintCode.Problem::key).toList();
            helper.assertTrue(keys.contains(c[1]) && r.model().entities().isEmpty(), c[0] + ": expected " + c[1] + ", got " + keys);
        }
        StringBuilder many = new StringBuilder();
        for (int i = 0; i <= BlueprintEntity.MAX_ENTITIES; i++) {
            many.append("@item_frame ").append(i % 256).append(",0,").append(i / 256).append(" up\n");
        }
        BlueprintCode.ParseResult tooMany = BlueprintCode.parse(many.toString());
        helper.assertTrue(tooMany.model().entities().size() == BlueprintEntity.MAX_ENTITIES
                && tooMany.problems().size() == 1 && tooMany.problems().get(0).key().equals("entity_too_many"),
                "the entity limit does not hold: " + tooMany.model().entities().size() + " " + tooMany.problems().size());
        // Einfaerbung: Kommentar hinter einem Namen mit '#', der Name selbst ist kein Kommentar.
        String styled = "@pig 0,0,0 name=\"a#b\" # c";
        byte[] styles = BlueprintCode.parse(styled).styles();
        helper.assertTrue(styles[styled.indexOf("a#b") + 1] != BlueprintCode.STYLE_COMMENT && styles[styled.length() - 1] == BlueprintCode.STYLE_COMMENT,
                "a '#' inside a name starts a comment");
        helper.succeed();
    }

    // =====================================================================================
    // SCAN -> CODE -> BAU
    // =====================================================================================

    /**
     * Scan -> Code -> Bau ergibt dieselben Frames und Entities: acht Frames (zwei Leucht-Rahmen) an allen sechs Seiten (Boden und
     * Decke eingeschlossen), Drehung 0..7, unsichtbar+fest, verzaubertes und benanntes Item, ein Ruestungsstaender mit Ausruestung,
     * Pose und Schaltern, ein KI-loses Schwein. Der neu gebaute Ort scannt sich zum selben Code; ein um eine Viertel gedrehter Bau
     * dreht Richtungen, Bodenrahmen-Items und Blickrichtung mit.
     *
     * <p><strong>Was diesen Test bricht:</strong> ein Scan ohne Entities, verlorene Komponenten/Schalter/Drehungen, Entities vor
     * ihren Waenden (abgefallen), falsche Stellen nach der Drehung, doppelte Entities.
     */
    public static void scanCodeBuildKeepsFramesStandsAndMobs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper, true);
        clearArea(helper);
        scene(helper);

        String code = scan(helper);
        BlueprintCode.ParseResult parsed = BlueprintCode.parse(code);
        helper.assertTrue(parsed.ok(), "the scanned code has problems: " + parsed.problems() + "\n" + code);
        BlueprintModel model = parsed.model();
        long frames = model.entities().stream().filter(e -> e.kind() == BlueprintEntity.Kind.FRAME).count();
        helper.assertTrue(frames == 8 && model.entities().size() == 10, "scanned " + model.entities().size() + " entities ("
                + frames + " frames), expected 8 frames + stand + pig:\n" + code);
        for (String part : new String[]{"@glow_item_frame", "rot=7", "invisible fixed", "enchantments=", "custom_name=", "Klinge #1",
                "@armor_stand", "small arms noplate", "head=iron_helmet", "head_pose=-10,20,0", "@pig", "noai", "name=\"Piggy\""}) {
            helper.assertTrue(code.contains(part), "the scan lost '" + part + "':\n" + code);
        }
        Set<Integer> rotations = new HashSet<>();
        Set<Direction> facings = new HashSet<>();
        for (BlueprintEntity e : model.entities()) {
            if (e.kind() == BlueprintEntity.Kind.FRAME) {
                rotations.add(e.rotation());
                facings.add(e.facing());
            }
        }
        helper.assertTrue(rotations.size() == 8 && facings.size() == 6, "rotations " + rotations + ", facings " + facings);

        // Alles weg, dann aus dem Code an dieselbe Stelle bauen (Blick nach Sueden, Mitte x = 4).
        clearArea(helper);
        ItemStack wand = new ItemStack(BtItems.DIAMOND_BUILDING_WAND);
        ItemStack blueprint = blueprint(code);
        hold(player, wand, blueprint);
        BlueprintBuilder.Result built = build(helper, player, wand, blueprint, new BlockPos(4, 0, 1));
        helper.assertTrue(built != null && built.finished() && built.missing() == 0 && built.blocked() == 0,
                "the build did not finish cleanly: " + built);
        helper.assertTrue(built.placed() == 2 + 10, "placed " + (built == null ? -1 : built.placed()) + ", expected 2 blocks + 10 entities");
        String again = scan(helper);
        helper.assertTrue(again.equals(code), "the rebuilt place scans differently:\n--- first\n" + code + "\n--- rebuilt\n" + again);
        ItemFrame top = frameAt(helper, STONE_A.above(), Direction.UP);
        helper.assertTrue(top instanceof GlowItemFrame && top.getRotation() == 3 && top.getItem().is(Items.DIAMOND_SWORD)
                && top.getItem().isEnchanted() && "Klinge #1".equals(top.getItem().getHoverName().getString()),
                "the glow frame on top lost its sword: " + top);
        ItemFrame bottom = frameAt(helper, STONE_A.below(), Direction.DOWN);
        helper.assertTrue(bottom != null && bottom.isInvisible() && saved(bottom).getBooleanOr("Fixed", false) && bottom.getRotation() == 7,
                "the invisible fixed frame below is not invisible/fixed: " + bottom);
        ArmorStand stand = level.getEntitiesOfClass(ArmorStand.class, area(helper)).stream().findFirst().orElse(null);
        helper.assertTrue(stand != null && stand.isSmall() && stand.showArms() && !stand.showBasePlate()
                        && stand.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)
                        && stand.getHeadPose().equals(new Rotations(-10, 20, 0)) && Math.abs(stand.getYRot() - 45) < 0.01,
                "the armor stand lost equipment, pose or flags: " + stand);
        Mob pig = level.getEntitiesOfClass(Mob.class, area(helper)).stream().findFirst().orElse(null);
        helper.assertTrue(pig != null && pig.isNoAi() && pig.isSilent() && "Piggy".equals(pig.getCustomName().getString()),
                "the pig lost its flags: " + pig);

        // Ein zweiter Klick auf dieselbe Stelle doppelt nichts.
        BlueprintBuilder.Result twice = build(helper, player, wand, blueprint, new BlockPos(4, 0, 1));
        helper.assertTrue(level.getEntitiesOfClass(ItemFrame.class, area(helper)).size() == 8 && twice != null && twice.placed() == 0,
                "a second build doubled entities: " + twice);

        // Eine Viertel im Uhrzeigersinn: Ziel 6,1,3 - der Bau liegt dann in x 1..6, z 0..5.
        clearArea(helper);
        blueprint.set(BtComponents.BLUEPRINT_ROTATION, 1);
        BlueprintBuilder.Result turned = build(helper, player, wand, blueprint, new BlockPos(6, 0, 3));
        helper.assertTrue(turned != null && turned.finished() && turned.blocked() == 0 && turned.missing() == 0,
                "the turned build did not finish cleanly: " + turned);
        helper.assertTrue(level.getEntitiesOfClass(ItemFrame.class, area(helper)).size() == 8, "the turned build lost frames");
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 2)).is(Blocks.STONE) && helper.getBlockState(new BlockPos(2, 1, 4)).is(Blocks.STONE),
                "the turned stones are not where the rotation puts them");
        ItemFrame east = frameAt(helper, new BlockPos(5, 2, 2), Direction.EAST);
        helper.assertTrue(east != null && east.getItem().is(Items.APPLE) && east.getRotation() == 1,
                "the north frame did not turn to the east side of its stone: " + east);
        ItemFrame turnedTop = frameAt(helper, new BlockPos(4, 3, 2), Direction.UP);
        helper.assertTrue(turnedTop != null && turnedTop.getRotation() == 5, "the floor frame's item did not turn with the build: " + turnedTop);
        ItemFrame turnedBottom = frameAt(helper, new BlockPos(4, 1, 2), Direction.DOWN);
        helper.assertTrue(turnedBottom != null && turnedBottom.getRotation() == 5, "the ceiling frame's item did not turn back: " + turnedBottom);
        ArmorStand turnedStand = level.getEntitiesOfClass(ArmorStand.class, area(helper)).stream().findFirst().orElse(null);
        Vec3 standAt = helper.absoluteVec(new Vec3(2.5, 1, 0.5));
        helper.assertTrue(turnedStand != null && turnedStand.position().distanceTo(standAt) < 0.01 && Math.abs(turnedStand.getYRot() - 135) < 0.01,
                "the turned stand is at " + (turnedStand == null ? null : turnedStand.position()) + " yaw "
                        + (turnedStand == null ? 0 : turnedStand.getYRot()) + ", expected " + standAt + " yaw 135");
        Mob turnedPig = level.getEntitiesOfClass(Mob.class, area(helper)).stream().findFirst().orElse(null);
        helper.assertTrue(turnedPig != null && turnedPig.position().distanceTo(helper.absoluteVec(new Vec3(6.5, 1, 5.5))) < 0.01,
                "the turned pig is at " + (turnedPig == null ? null : turnedPig.position()));
        clearArea(helper);
        helper.succeed();
    }

    /**
     * Ueberleben: ein Frame kostet sein Item, sein Inhalt kommt nur aus dem Inventar (genau dieses Item, es wird verbraucht),
     * Schalter wie {@code fixed} gibt es nur kreativ, Mobs gar nicht, und ein Frame ohne Wand wird nicht gesetzt.
     *
     * <p><strong>Was diesen Test bricht:</strong> Gratis-Items aus der Blaupause (Dupe), unzerstoerbare Frames im Ueberleben,
     * schwebende Frames, die sofort abfallen.
     */
    public static void survivalBuildPaysForFramesAndTheirItems(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper, false);
        clearArea(helper);
        String code = "stone 0,0,1\n@item_frame 0,0,0 north diamond fixed\n@glow_item_frame 1,0,0 north\n@pig 2,0,0";
        ItemStack wand = new ItemStack(BtItems.DIAMOND_BUILDING_WAND);
        ItemStack blueprint = blueprint(code);
        hold(player, wand, blueprint, new ItemStack(Items.STONE, 1), new ItemStack(Items.ITEM_FRAME, 1));
        BlueprintBuilder.Result first = build(helper, player, wand, blueprint, new BlockPos(4, 0, 1));
        helper.assertTrue(first != null && first.finished(), "the survival build did not run: " + first);
        List<ItemFrame> frames = level.getEntitiesOfClass(ItemFrame.class, area(helper));
        helper.assertTrue(frames.size() == 1 && frames.get(0).getItem().isEmpty() && !(frames.get(0) instanceof GlowItemFrame),
                "without a diamond the frame must stand empty and the glow frame (no glow frame item, no wall) must be missing: " + frames);
        helper.assertTrue(!saved(frames.get(0)).getBooleanOr("Fixed", false), "a survival build made a fixed frame");
        helper.assertTrue(level.getEntitiesOfClass(Mob.class, area(helper)).isEmpty(), "a survival build spawned a mob");
        helper.assertTrue(player.getInventory().countItem(Items.ITEM_FRAME) == 0, "the frame item was not used up");
        helper.assertTrue(first.missing() == 2 && first.blocked() == 1, "expected 2 missing (diamond, pig) and 1 blocked (glow frame "
                + "without a wall): " + first);

        // Mit Diamant: der Bau nimmt ihn aus dem Inventar.
        clearArea(helper);
        hold(player, wand, blueprint, new ItemStack(Items.STONE, 1), new ItemStack(Items.ITEM_FRAME, 1), new ItemStack(Items.DIAMOND, 1));
        build(helper, player, wand, blueprint, new BlockPos(4, 0, 1));
        frames = level.getEntitiesOfClass(ItemFrame.class, area(helper));
        helper.assertTrue(frames.size() == 1 && frames.get(0).getItem().is(Items.DIAMOND) && player.getInventory().countItem(Items.DIAMOND) == 0,
                "the diamond did not move from the inventory into the frame: " + frames);
        clearArea(helper);
        helper.succeed();
    }

    // =====================================================================================
    // SZENE UND HILFEN
    // =====================================================================================

    /** Zwei Steine, acht Frames an allen Seiten, ein Ruestungsstaender, ein KI-loses Schwein. */
    private static void scene(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(STONE_A, Blocks.STONE);
        helper.setBlock(STONE_B, Blocks.STONE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 5);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Klinge #1"));
        frame(helper, true, STONE_A.above(), Direction.UP, sword, 3, false);
        frame(helper, false, STONE_A.below(), Direction.DOWN, new ItemStack(Items.TORCH), 7, true);
        frame(helper, false, STONE_A.north(), Direction.NORTH, new ItemStack(Items.APPLE), 1, false);
        frame(helper, false, STONE_A.south(), Direction.SOUTH, ItemStack.EMPTY, 0, false);
        frame(helper, false, STONE_A.west(), Direction.WEST, new ItemStack(Items.STICK), 5, false);
        frame(helper, false, STONE_A.east(), Direction.EAST, new ItemStack(Items.SHIELD), 6, false);
        frame(helper, false, STONE_B.north(), Direction.NORTH, new ItemStack(Items.MAP), 2, false);
        frame(helper, true, STONE_B.above(), Direction.UP, new ItemStack(Items.GOLD_INGOT, 1), 4, false);

        ArmorStand stand = new ArmorStand(level, 0, 0, 0);
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 1, 5.5));
        stand.snapTo(at.x, at.y, at.z, 45, 0);
        stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        stand.setHeadPose(new Rotations(-10, 20, 0));
        withFlags(level, stand, "Small", "ShowArms", "NoBasePlate");
        level.addFreshEntity(stand);

        Entity pig = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
        Vec3 pigAt = helper.absoluteVec(new Vec3(6.5, 1, 1.5));
        pig.snapTo(pigAt.x, pigAt.y, pigAt.z, 90, 0);
        ((Mob) pig).setNoAi(true);
        pig.setSilent(true);
        pig.setCustomName(Component.literal("Piggy"));
        level.addFreshEntity(pig);
    }

    private static void frame(GameTestHelper helper, boolean glow, BlockPos rel, Direction dir, ItemStack item, int rotation, boolean invisibleFixed) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);
        ItemFrame frame = glow ? new GlowItemFrame(level, abs, dir) : new ItemFrame(level, abs, dir);
        if (!item.isEmpty()) {
            frame.setItem(item, false);
        }
        frame.setRotation(rotation);
        if (invisibleFixed) {
            withFlags(level, frame, "Invisible", "Fixed");
        }
        level.addFreshEntity(frame);
    }

    private static void withFlags(ServerLevel level, Entity entity, String... keys) {
        CompoundTag tag = saved(entity);
        for (String key : keys) {
            tag.putBoolean(key, true);
        }
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
    }

    private static CompoundTag saved(Entity entity) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.level().registryAccess());
        entity.saveWithoutId(out);
        return out.buildResult();
    }

    private static ItemFrame frameAt(GameTestHelper helper, BlockPos rel, Direction dir) {
        BlockPos abs = helper.absolutePos(rel);
        return helper.getLevel().getEntitiesOfClass(ItemFrame.class, area(helper)).stream()
                .filter(f -> f.getPos().equals(abs) && f.getDirection() == dir).findFirst().orElse(null);
    }

    private static AABB area(GameTestHelper helper) {
        BlockPos a = helper.absolutePos(new BlockPos(0, 1, 0));
        return new AABB(a.getX(), a.getY(), a.getZ(), a.getX() + 8, a.getY() + 6, a.getZ() + 8);
    }

    private static String scan(GameTestHelper helper) {
        ItemStack octant = new ItemStack(BtItems.OCTANT);
        BlockPos pa = helper.absolutePos(LOW);
        BlockPos pb = helper.absolutePos(HIGH);
        CompoundTag nbt = new CompoundTag();
        nbt.putIntArray("Pos1", new int[]{pa.getX(), pa.getY(), pa.getZ()});
        nbt.putIntArray("Pos2", new int[]{pb.getX(), pb.getY(), pb.getZ()});
        octant.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        BlueprintScanner.Outcome outcome = BlueprintScanner.scanAtTable(helper.getLevel(), helper.absolutePos(new BlockPos(7, 1, 7)), octant,
                new ItemStack(BtItems.BLUEPRINT));
        helper.assertTrue(outcome.error() == null, "scan refused: " + outcome.error());
        return BlueprintItem.content(outcome.result()).code();
    }

    /** Boden aus Stein (y 0), darueber alles leer, keine Entities ausser Spielern. */
    private static void clearArea(GameTestHelper helper) {
        for (Entity e : helper.getLevel().getEntities((Entity) null, area(helper).inflate(1), e -> !(e instanceof Player))) {
            e.discard();
        }
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.SMOOTH_STONE);
                for (int y = 1; y < 7; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper, boolean instabuild) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 2.0, 0.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.getAbilities().instabuild = instabuild;
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    private static ItemStack blueprint(String code) {
        ItemStack stack = new ItemStack(BtItems.BLUEPRINT);
        stack.set(BtComponents.BLUEPRINT, new BlueprintContent(code, "Test", "Tester", true));
        return stack;
    }

    private static void hold(ServerPlayer player, ItemStack wand, ItemStack blueprint, ItemStack... supplies) {
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, wand);
        player.setItemInHand(InteractionHand.OFF_HAND, blueprint);
        for (int i = 0; i < supplies.length; i++) {
            player.getInventory().setItem(i + 1, supplies[i]);
        }
    }

    /** Klick (Blick nach Sueden) auf {@code clicked} und sofortiger Abschluss des Auftrags; im Ueberleben mit Bestaetigungsklick. */
    private static BlueprintBuilder.Result build(GameTestHelper helper, ServerPlayer player, ItemStack wand, ItemStack blueprint, BlockPos clicked) {
        BlueprintBuilder.Result result = BlueprintBuilder.build(helper.getLevel(), player, wand, blueprint, helper.absolutePos(clicked), Direction.UP);
        if (result != null && result.warned()) {
            result = BlueprintBuilder.build(helper.getLevel(), player, wand, blueprint, helper.absolutePos(clicked), Direction.UP);
        }
        if (result != null && !result.finished()) {
            BlueprintBuilder.Result rest = BlueprintBuilder.completeJob(player);
            return rest != null ? rest : result;
        }
        return result;
    }

    private static boolean namespaceLoaded(String namespace) {
        for (var id : BuiltInRegistries.BLOCK.keySet()) {
            if (id.getNamespace().equals(namespace)) {
                return true;
            }
        }
        return false;
    }

    private static String readFixture(GameTestHelper helper, String name) {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            Path file = dir.resolve("testing").resolve("fixtures").resolve(name);
            if (Files.isRegularFile(file)) {
                try {
                    return Files.readString(file, StandardCharsets.UTF_8);
                } catch (java.io.IOException e) {
                    throw helper.assertionException("cannot read " + file + ": " + e);
                }
            }
            dir = dir.getParent();
        }
        throw helper.assertionException("no testing/fixtures/" + name + " above " + Path.of("").toAbsolutePath());
    }
}
