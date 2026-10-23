package com.simplebuilding.gametest;

import com.simplebuilding.blueprint.BlueprintContent;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.dev.testcentre.FeatureStations;
import com.simplebuilding.dev.testcentre.TabBrowser;
import com.simplebuilding.dev.testcentre.TcContext;
import com.simplebuilding.dev.testcentre.TcOp;
import com.simplebuilding.dev.testcentre.TestCentreBuilder;
import com.simplebuilding.dev.testcentre.TestCentreCommand;
import com.simplebuilding.dev.testcentre.TestCentreKits;
import com.simplebuilding.dev.testcentre.TestCentreLayout;
import com.simplebuilding.items.ModItemGroupsContent;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.tweaks.item.TweaksItems;
import com.simplebuilding.tweaks.block.FlypadBlock;
import com.simplebuilding.tweaks.block.PadBlock;
import com.simplebuilding.tweaks.block.PadTiers;
import com.simplebuilding.util.OctantShape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

/**
 * Die Testzentrale ({@code /sbtestcentre}, docs/TESTZENTRALE.md): jedes Mod-Item hat dort einen Platz,
 * und der Bau laeuft ohne Fehler genau so ab, wie er geplant ist.
 *
 * <p>Der Abdeckungstest ist absichtlich streng: ein neues Item, das keinem Abschnitt zufaellt, landet
 * in der Wand "Unsortiert" und macht diesen Test rot. Abhilfe ist ein Platz in einem Abschnitt
 * ({@code TestCentreSections}) - oder, fuer technische Eintraege, ein begruendeter Eintrag in
 * {@code TestCentreLayout.EXCLUDED}.
 */
public final class TestCentreTests {

    private TestCentreTests() {
    }

    /** Regressions: unloaded-origin minY, invalid old marker, falling join and respawn scatter. */
    public static void freshWorldOriginAndEntranceAreSafe(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.HUB_TEST_WORLD) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        var marker = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("simplebuilding_testcentre.txt");
        var spawn = level.getRespawnData();
        var rules = level.getGameRules();
        int radius = rules.get(net.minecraft.world.level.gamerules.GameRules.RESPAWN_RADIUS);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> level.getServer().getPlayerList().remove(player));
        byte[] before;
        try {
            before = java.nio.file.Files.exists(marker) ? java.nio.file.Files.readAllBytes(marker) : null;
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        try {
            java.nio.file.Files.deleteIfExists(marker);
            BlockPos fresh = TestCentreCommand.savedOrDefaultOrigin(level);
            helper.assertTrue(fresh.getY() > level.getMinY(), "fresh origin must leave room for a floor");
            helper.assertTrue(level.hasChunk(0, 0), "origin chunk must be loaded before reading its height");
            helper.assertTrue(fresh.getY() == Math.max(level.getMinY() + 1,
                    level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0)),
                    "fresh origin must use the generated terrain height");
            java.nio.file.Files.writeString(marker, "0 " + level.getMinY() + " 0 old-hash\n");
            helper.assertTrue(TestCentreCommand.savedOrDefaultOrigin(level).equals(fresh), "repair old minY origin");
            BlockPos origin = helper.absolutePos(new BlockPos(2, 3, 2));
            java.nio.file.Files.writeString(marker, origin.getX() + " " + origin.getY() + " " + origin.getZ() + "\n");
            helper.assertTrue(TestCentreCommand.savedOrDefaultOrigin(level).equals(origin), "rebuild must retain its origin");
            var plan = TestCentreLayout.plan(level, origin);
            BlockPos entrance = plan.entrance();
            for (int attempt = 0; attempt < 2; attempt++) {
                level.setBlockAndUpdate(entrance.below(), Blocks.AIR.defaultBlockState());
                player.snapTo(entrance.getX(), level.getMinY() - 5, entrance.getZ(), 0, 0);
                player.setDeltaMovement(0, -2, 0);
                player.fallDistance = 50;
                TestCentreCommand.arriveAtEntrance(level, plan, player);
                helper.assertTrue(player.blockPosition().equals(entrance), "join/rebuild must teleport to entrance");
                helper.assertTrue(level.getBlockState(entrance.below()).isSolid(), "entrance needs a real floor");
                helper.assertTrue(level.noCollision(player), "entrance must have clear headroom");
                helper.assertTrue(player.getDeltaMovement().lengthSqr() == 0 && player.fallDistance == 0,
                        "landing must clear falling motion and damage");
                helper.assertTrue(level.getRespawnData().pos().equals(entrance), "world spawn must be the entrance");
                helper.assertTrue(rules.get(net.minecraft.world.level.gamerules.GameRules.RESPAWN_RADIUS) == 0,
                        "respawn must not scatter players off the platform");
            }
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        } finally {
            level.setRespawnData(spawn);
            rules.set(net.minecraft.world.level.gamerules.GameRules.RESPAWN_RADIUS, radius, level.getServer());
            try {
                if (before == null) java.nio.file.Files.deleteIfExists(marker);
                else java.nio.file.Files.write(marker, before);
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }
        helper.succeed();
    }

    /** Jedes Mod-Item und jeder Mod-Block steht in einem Abschnitt (ausser den begruendeten Ausnahmen). */
    public static void everyModItemAndBlockHasItsPlaceInTheTestCentre(GameTestHelper helper) {
        // Split stage 5b: soul lava and the buckets are Simple Crucibles' (no simplebuilding: block to exclude any more).
        TestCentreLayout.Plan plan = TestCentreLayout.plan(helper.getLevel(), BlockPos.ZERO);
        List<String> problems = new ArrayList<>();
        for (Item item : plan.leftovers()) {
            problems.add("item " + BuiltInRegistries.ITEM.getKey(item) + " has no section (it only shows up under 'unsorted')");
        }
        for (Block block : TestCentreLayout.modBlocks()) {
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            boolean shown = plan.coveredBlocks().contains(block) || plan.coveredItems().contains(block.asItem());
            if (!shown && !TestCentreLayout.EXCLUDED.containsKey(id)) {
                problems.add("block " + id + " is neither placed nor framed in any section");
            }
        }
        // Jeder Mod-Block mit Item steht auch gesetzt in der Welt (die Galerie nimmt, was sonst nur im
        // Rahmen haengt) - ausser den Bloecken mit Wirkung auf die Umgebung.
        for (Block block : TestCentreLayout.modBlocks()) {
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            if (block.asItem() != net.minecraft.world.item.Items.AIR && !plan.coveredBlocks().contains(block)
                    && !TestCentreLayout.frameOnly(block) && !TestCentreLayout.EXCLUDED.containsKey(id)) {
                problems.add("block " + id + " is only framed, never placed");
            }
        }
        for (String excluded : TestCentreLayout.EXCLUDED.keySet()) {
            Identifier id = Identifier.parse(excluded);
            if (!BuiltInRegistries.ITEM.containsKey(id) && !BuiltInRegistries.BLOCK.containsKey(id)) {
                problems.add("exclusion " + excluded + " names nothing that is registered - remove it");
            }
        }
        helper.assertTrue(plan.sections().size() == TestCentreLayout.sectionIds().size(),
                "planned sections " + plan.sections().size() + " but sectionIds() lists " + TestCentreLayout.sectionIds().size());
        helper.assertTrue(helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("sbtestcentre") != null,
                "/sbtestcentre is not registered on this loader");
        helper.assertTrue(problems.isEmpty(), problems.size() + " test centre coverage problem(s): " + String.join("; ", problems));
        helper.succeed();
    }

    /**
     * TZ22-1 (Besitzer 22.10.: "ich sehe nicht alles, was in der Mod existiert"): jeder Eintrag der Register aller geladenen
     * Module der Familie ({@link TcContext#FAMILY}) hat seinen Platz - Items und Bloecke stehen in einem Abschnitt (gesetzt,
     * im Rahmen, am Staender oder in einem Behaelter), jeder Mob hat seine Zelle. Ausnahmen nur begruendet: technische Bloecke
     * ohne Item ({@link TestCentreLayout#technical}, {@link TestCentreLayout#EXCLUDED}), Entity-Typen, die kein Mob sind
     * (MISC: Geschosse, Fahrzeuge, Marker; nicht beschwoerbar), und die Strukturen (eigener Bereich in TZ22-3, nur gezaehlt).
     * Ein geladener Namensraum "simple..." ausserhalb der Familie heisst: neues Modul, in {@code TcContext.FAMILY} eintragen.
     */
    public static void everyFamilyRegistryEntryIsInTheTestCentre(GameTestHelper helper) {
        TestCentreLayout.Plan plan = TestCentreLayout.plan(helper.getLevel(), BlockPos.ZERO);
        List<String> problems = new ArrayList<>();
        Map<String, Integer> excused = new LinkedHashMap<>();
        Set<String> namespaces = new java.util.TreeSet<>();
        java.util.function.Consumer<Identifier> family = id -> {
            if (id.getNamespace().startsWith("simple") && !TcContext.FAMILY.contains(id.getNamespace())) {
                problems.add("namespace " + id.getNamespace() + " is not in TcContext.FAMILY (new module?)");
            }
            if (TcContext.isFamily(id)) {
                namespaces.add(id.getNamespace());
            }
        };
        int items = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            family.accept(id);
            if (!TcContext.isFamily(id) || plan.coveredItems().contains(item)) {
                items += TcContext.isFamily(id) ? 1 : 0;
                continue;
            }
            if (TestCentreLayout.EXCLUDED.containsKey(id.toString()) || TcContext.isSpacerId(id)) {
                excused.merge("item excluded", 1, Integer::sum);
            } else {
                problems.add("item " + id + " is not in the test centre");
            }
        }
        int blocks = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            family.accept(id);
            if (!TcContext.isFamily(id)) {
                continue;
            }
            Item item = TcContext.itemOf(block);
            if (plan.coveredBlocks().contains(block) || item != Items.AIR && plan.coveredItems().contains(item)) {
                blocks++;
            } else if (TestCentreLayout.EXCLUDED.containsKey(id.toString())) {
                excused.merge("block excluded", 1, Integer::sum);
            } else if (TestCentreLayout.technical(block) != null) {
                excused.merge("technical block (wall/potted variant, liquid, module exclusion)", 1, Integer::sum);
            } else {
                problems.add("block " + id + (item == Items.AIR ? " has no item and is neither placed nor excused" : " is not in the test centre"));
            }
        }
        int mobs = 0;
        for (net.minecraft.world.entity.EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            family.accept(id);
            if (!TcContext.isFamily(id)) {
                continue;
            }
            if (type.getCategory() == net.minecraft.world.entity.MobCategory.MISC || !type.canSummon()) {
                excused.merge("entity type that is no mob (MISC / not summonable)", 1, Integer::sum);
            } else if (plan.anchors().containsKey(com.simplebuilding.dev.testcentre.MobStations.anchorName(id))) {
                mobs++;
            } else {
                problems.add("mob " + id + " has no cell in the mob station");
            }
        }
        // TZ22-3: Strukturen sind keine Ausnahme mehr. Jede Familien-Struktur ist ein Jigsaw, dessen Startpool-Teile alle im
        // Abschnitt "structures" stehen; dazu jedes Familien-Teil eines beliebigen Pools (auch eingehaengte Dorfhaeuser).
        Set<Identifier> shownTemplates = new java.util.HashSet<>();
        plan.sections().forEach(section -> section.ops().forEach(op -> {
            if (op instanceof TcOp.Structure structure) {
                shownTemplates.add(structure.template());
            }
        }));
        var registries = helper.getLevel().registryAccess();
        Map<Identifier, Set<Identifier>> pieces = com.simplebuilding.dev.testcentre.StructureStation.familyPieces(registries);
        int structures = 0;
        for (var entry : com.simplebuilding.dev.testcentre.StructureStation.familyStructures(registries).entrySet()) {
            family.accept(entry.getKey());
            if (entry.getValue().isEmpty()) {
                problems.add("structure " + entry.getKey() + " is no jigsaw structure - the test centre cannot show its pieces");
            } else if (!pieces.containsKey(entry.getValue().get())) {
                problems.add("structure " + entry.getKey() + " starts with pool " + entry.getValue().get() + " without family pieces");
            } else {
                structures++;
            }
        }
        int structurePieces = 0;
        for (Set<Identifier> templates : pieces.values()) {
            for (Identifier template : templates) {
                if (shownTemplates.contains(template)) {
                    structurePieces++;
                } else {
                    problems.add("structure piece " + template + " is not in the test centre");
                }
            }
        }
        com.simplebuilding.Simplebuilding.LOGGER.info("test centre inventory: {} namespaces {}, {} items, {} blocks, {} mobs, {} structures"
                        + " ({} pieces) shown; excused {}", namespaces.size(), namespaces, items, blocks, mobs, structures, structurePieces, excused);
        if (!problems.isEmpty()) {
            com.simplebuilding.Simplebuilding.LOGGER.warn("test centre inventory problems: {}", String.join("; ", problems));
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " registry entr(y/ies) missing from the test centre: "
                + String.join("; ", problems.subList(0, Math.min(40, problems.size()))));
        helper.succeed();
    }

    /**
     * TZ22-2 (Besitzer 22.10.: "es leckt noch etwas, dadurch, dass es so viele Item-Frames sind"): kein Rahmen an einer Wand
     * haelt, was dort selbst als Block stehen koennte ({@code Exhibits.wallState}), und keine Station rahmt ein Item, das sie
     * schon gesetzt zeigt ({@code TestCentreLayout.shownPlaced}; die Tab-Waende sind ein Verzeichnis und duerfen das).
     * Rahmen und Staender je Abschnitt stehen im Log.
     */
    public static void noFrameHoldsWhatCouldStandAsBlock(GameTestHelper helper) {
        TestCentreLayout.Plan plan = TestCentreLayout.plan(helper.getLevel(), BlockPos.ZERO);
        List<String> problems = new ArrayList<>();
        for (TestCentreLayout.Section section : plan.sections()) {
            Set<Item> placed = section.id().startsWith(TabBrowser.PREFIX) ? Set.of() : TestCentreLayout.shownPlaced(section.ops());
            for (TcOp op : section.ops()) {
                if (!(op instanceof TcOp.Frame frame)) {
                    continue;
                }
                String item = section.id() + ": " + BuiltInRegistries.ITEM.getKey(frame.stack().getItem());
                if (frame.facing() == Direction.NORTH && com.simplebuilding.dev.testcentre.Exhibits.onWall(frame.stack())) {
                    problems.add(item + " hangs in a frame although it could stand in the wall");
                } else if (frame.stack().getComponentsPatch().isEmpty() && placed.contains(frame.stack().getItem())) {
                    problems.add(item + " is framed although the station already shows it placed");
                }
            }
        }
        com.simplebuilding.Simplebuilding.LOGGER.info(plan.summary());
        helper.assertTrue(problems.isEmpty(), problems.size() + " needless frame(s): " + String.join("; ", problems.subList(0, Math.min(30, problems.size()))));
        helper.succeed();
    }

    /**
     * TZ22-4 (Besitzer 22.10.: "Der Standardbefehl baut alles. Optionen schraenken die Groesse ein, z. B. ohne die Strukturen
     * oder nur einzelne Bereiche"): {@code without structures} plant keine Vorlage und ist kleiner, {@code only mobs} nur
     * Steuerwand und Mobs; Abdeckung bleibt gleich (geplant wird alles); die Knoepfe der Steuerwand tragen die Optionen mit;
     * Optionen-Text liest sich zurueck, unbekannte Bereiche werfen; {@code help}, {@code build … only|without} sind registriert.
     * Dazu TZ22-3: mit Strukturen im Register steht der Abschnitt {@code structures} in der vollen Planung.
     */
    public static void partialBuildsOnlyContainTheChosenAreas(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TestCentreLayout.Plan full = TestCentreLayout.plan(level, BlockPos.ZERO);
        TestCentreLayout.Options withoutStructures = TestCentreLayout.Options.parse("without structures");
        TestCentreLayout.Plan without = TestCentreLayout.plan(level, BlockPos.ZERO, withoutStructures);
        java.util.function.Function<TestCentreLayout.Plan, Long> templates = plan -> plan.sections().stream()
                .flatMap(section -> section.ops().stream()).filter(op -> op instanceof TcOp.Structure).count();
        boolean hasStructures = !com.simplebuilding.dev.testcentre.StructureStation.familyPieces(level.registryAccess()).isEmpty();
        helper.assertTrue(!hasStructures || templates.apply(full) > 0, "the full plan places no structure piece although there are some");
        helper.assertTrue(templates.apply(without) == 0, "'without structures' still places " + templates.apply(without) + " structure pieces");
        long fullArea = (long) full.bounds().getXSpan() * full.bounds().getZSpan();
        long withoutArea = (long) without.bounds().getXSpan() * without.bounds().getZSpan();
        helper.assertTrue(!hasStructures || withoutArea < fullArea, "'without structures' is not smaller: " + withoutArea + " vs " + fullArea);
        helper.assertTrue(without.leftovers().equals(full.leftovers()), "a partial build must not change the coverage");
        helper.assertTrue(!without.fingerprint().equals(full.fingerprint()) || !hasStructures, "partial and full build share a fingerprint");
        // Die Steuerwand baut mit denselben Optionen neu.
        List<String> commands = new ArrayList<>();
        without.section("controls").ops().forEach(op -> {
            if (op instanceof TcOp.Command command) {
                commands.add(command.command());
            }
        });
        helper.assertTrue(commands.stream().anyMatch(c -> c.startsWith("sbtestcentre build ") && c.endsWith(" without structures")),
                "the rebuild-all button forgets the options: " + commands);
        helper.assertTrue(commands.stream().filter(c -> c.startsWith("sbtestcentre section ")).allMatch(c -> c.endsWith(" without structures")),
                "a section button forgets the options");
        helper.assertTrue(commands.stream().noneMatch(c -> c.startsWith("sbtestcentre section structures ")),
                "a button rebuilds a section that is not built");

        TestCentreLayout.Plan mobs = TestCentreLayout.plan(level, BlockPos.ZERO, TestCentreLayout.Options.parse("only mobs"));
        List<String> ids = mobs.sections().stream().map(TestCentreLayout.Section::id).toList();
        helper.assertTrue(ids.equals(List.of("controls", "mobs")), "'only mobs' plans " + ids);
        TestCentreLayout.Plan workshop = TestCentreLayout.plan(level, BlockPos.ZERO, TestCentreLayout.Options.parse("only workshop without chisel"));
        List<String> workshopIds = workshop.sections().stream().map(TestCentreLayout.Section::id).toList();
        helper.assertTrue(workshopIds.contains("tools") && !workshopIds.contains("chisel") && !workshopIds.contains("mobs"),
                "'only workshop without chisel' plans " + workshopIds);

        TestCentreLayout.Options mixed = TestCentreLayout.Options.parse("without catalog ONLY workshop, structures");
        helper.assertTrue(TestCentreLayout.Options.parse(mixed.text()).equals(mixed) && mixed.text().equals("only structures,workshop without catalog"),
                "options do not read back: " + mixed.text());
        helper.assertTrue(TestCentreLayout.Options.parse("").isAll() && TestCentreLayout.Options.parse("all").isAll(), "empty options are not 'all'");
        for (String bad : List.of("only nowhere", "mobs", "without tools,nothing")) {
            boolean threw = false;
            try {
                TestCentreLayout.Options.parse(bad);
            } catch (IllegalArgumentException expected) {
                threw = true;
            }
            helper.assertTrue(threw, "options '" + bad + "' were accepted");
        }
        var root = level.getServer().getCommands().getDispatcher().getRoot().getChild("sbtestcentre");
        helper.assertTrue(root != null && root.getCommand() != null && root.getChild("help") != null,
                "/sbtestcentre has no help");
        helper.assertTrue(root.getChild("build").getChild("only") != null && root.getChild("build").getChild("without") != null
                && root.getChild("build").getChild("here").getChild("without") != null, "/sbtestcentre build has no only/without");
        helper.succeed();
    }

    /** Hoechste Wand (Lage ueber dem Boden) je Abschnitt, ausser Tuermen mit Zweck (Fallturm des Sinkdaempfers). */
    static final Set<String> TALL_ON_PURPOSE = Set.of("sinkdamper");

    /**
     * TZ22-5 (Besitzer 22.10.: "Waende niedriger", "keine ellenlangen Reihen"): kein Abschnitt breiter als
     * {@link TestCentreLayout#MAX_ROW_WIDTH}, keine Ausstellungswand ({@code TcCanvas.WALL}/{@code TRIM}) hoeher als
     * sechs Bloecke, und jeder Bezirk beginnt eine eigene Reihe (ein spaeterer Bezirk Strukturen, TZ22-3, haengt sich an).
     */
    public static void theLayoutIsCompactWithLowWalls(GameTestHelper helper) {
        TestCentreLayout.Plan plan = TestCentreLayout.plan(helper.getLevel(), BlockPos.ZERO);
        List<String> problems = new ArrayList<>();
        for (TestCentreLayout.Section section : plan.sections()) {
            if (section.width() > TestCentreLayout.MAX_ROW_WIDTH) {
                problems.add(section.id() + " is " + section.width() + " wide");
            }
            if (TALL_ON_PURPOSE.contains(section.id())) {
                continue;
            }
            int top = -1;
            for (TcOp op : section.ops()) {
                if (op instanceof TcOp.Place place && (place.state().equals(com.simplebuilding.dev.testcentre.TcCanvas.WALL)
                        || place.state().equals(com.simplebuilding.dev.testcentre.TcCanvas.TRIM))) {
                    top = Math.max(top, place.pos().getY() - plan.origin().getY());
                }
            }
            if (top > com.simplebuilding.dev.testcentre.TcCanvas.SHELF_HEIGHT - 1) {
                problems.add(section.id() + " has a wall " + (top + 1) + " high");
            }
        }
        // Bezirke: der erste Abschnitt jedes Bezirks steht an der linken Kante seiner Raster-Spalte, kein Abschnitt ragt
        // in die naechste Spalte.
        for (List<String> district : TestCentreLayout.DISTRICTS) {
            boolean first = true;
            int column = -1;
            for (String id : district) {
                if (!TestCentreLayout.sectionIds().contains(id) || plan.section(id).ops().isEmpty()) {
                    continue;
                }
                int x = plan.section(id).offset().getX();
                if (first && x % TestCentreLayout.DISTRICT_PITCH != 0) {
                    problems.add("district of " + id + " does not start at its grid column (x " + x + ")");
                }
                if (first) {
                    column = x / TestCentreLayout.DISTRICT_PITCH;
                    first = false;
                }
                int right = x + plan.section(id).width();
                if (x / TestCentreLayout.DISTRICT_PITCH != column || right > column * TestCentreLayout.DISTRICT_PITCH + TestCentreLayout.MAX_ROW_WIDTH) {
                    problems.add(id + " leaves the grid column of its district (x " + x + " to " + right + ")");
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " layout problem(s): " + String.join("; ", problems));
        helper.succeed();
    }

    /**
     * Die ganze Zentrale wird fern aller anderen Tests gebaut und mit ihrer Planung verglichen: jeder
     * geplante Block steht (der jeweils letzte je Stelle), jedes Schild hat seinen Text, Rahmen und
     * Staender sind genau so viele wie geplant, nichts liegt als Drop herum. Danach wird ein Abschnitt
     * neu gebaut - das darf keine Rahmen verdoppeln.
     */
    public static void theWholeCentreBuildsAndMatchesItsPlan(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = new BlockPos(20000, helper.absolutePos(BlockPos.ZERO).getY() + 1, 20000);
        TestCentreLayout.Plan plan = TestCentreLayout.plan(level, origin);
        TestCentreBuilder.Result result = TestCentreBuilder.build(level, plan);
        com.simplebuilding.Simplebuilding.LOGGER.info("{} | built in {} ms ({} steps, {} entities)", plan.summary(),
                result.millis(), result.ops(), result.entities());
        try {
            helper.assertTrue(!level.getBlockState(plan.entrance().below()).isAir(), "built entrance must have a floor");
            Map<BlockPos, BlockState> expected = new HashMap<>();
            int frames = 0;
            int stands = 0;
            List<BlockPos> signs = new ArrayList<>();
            for (TestCentreLayout.Section section : plan.sections()) {
                for (TcOp op : section.ops()) {
                    frames += op.frames();
                    switch (op) {
                        case TcOp.Place place -> expected.put(place.pos(), place.state());
                        case TcOp.Stand stand -> stands++;
                        case TcOp.Sign sign -> signs.add(sign.pos());
                        default -> {
                        }
                    }
                }
            }
            List<String> wrong = new ArrayList<>();
            for (Map.Entry<BlockPos, BlockState> entry : expected.entrySet()) {
                Block actual = level.getBlockState(entry.getKey()).getBlock();
                if (actual != entry.getValue().getBlock() && wrong.size() < 10) {
                    wrong.add(entry.getKey() + " holds " + BuiltInRegistries.BLOCK.getKey(actual) + " instead of "
                            + BuiltInRegistries.BLOCK.getKey(entry.getValue().getBlock()));
                }
            }
            helper.assertTrue(wrong.isEmpty(), "blocks differ from the plan: " + String.join("; ", wrong));
            // TZ22-3: jede Strukturvorlage steht - in ihrem Kasten mindestens so viele feste Bloecke wie die Vorlage hat.
            for (TestCentreLayout.Section section : plan.sections()) {
                for (TcOp op : section.ops()) {
                    if (op instanceof TcOp.Structure structure) {
                        var template = com.simplebuilding.version.McVersion.structureTemplates(level).get(structure.template());
                        helper.assertTrue(template.isPresent(), "structure template " + structure.template() + " is missing");
                        int want = com.simplebuilding.dev.testcentre.StructureStation.solidBlocks(template.get());
                        int have = 0;
                        for (BlockPos pos : BlockPos.betweenClosed(structure.pos(), structure.far())) {
                            if (!level.getBlockState(pos).isAir()) {
                                have++;
                            }
                        }
                        helper.assertTrue(want > 0 && have >= want, "structure " + structure.template() + " at " + structure.pos()
                                + ": " + have + " solid blocks placed, template has " + want);
                    }
                }
            }
            for (BlockPos pos : signs) {
                helper.assertTrue(level.getBlockEntity(pos) instanceof SignBlockEntity sign
                        && !com.simplebuilding.version.McVersion.signFrontLine(sign, 0).getString().isEmpty(), "no labelled sign at " + pos);
            }
            AABB area = AABB.of(plan.bounds());
            int placedFrames = level.getEntitiesOfClass(ItemFrame.class, area).size();
            int placedStands = level.getEntitiesOfClass(ArmorStand.class, area).size();
            helper.assertTrue(placedFrames == frames, "item frames: planned " + frames + ", placed " + placedFrames);
            helper.assertTrue(placedStands == stands, "armour stands: planned " + stands + ", placed " + placedStands);
            // TZ22-2: Setzbares steht als Block; Rahmen bleiben fuer Werkzeuge, Ruestung, Buecher, Eimer ... (vorher 2630).
            helper.assertTrue(frames > 100, "suspiciously few frames planned: " + frames);
            // Die Blaupause im Rahmen wurde beim Bau wirklich gescannt, der Oktant daneben traegt seine Auswahl.
            for (TestCentreLayout.Section section : plan.sections()) {
                for (TcOp op : section.ops()) {
                    if (op instanceof TcOp.BlueprintFrame frame) {
                        List<ItemFrame> found = level.getEntitiesOfClass(ItemFrame.class, new AABB(frame.pos()));
                        helper.assertTrue(!found.isEmpty() && !found.getFirst().getItem()
                                        .getOrDefault(ModDataComponentTypes.BLUEPRINT, BlueprintContent.EMPTY).equals(BlueprintContent.EMPTY),
                                "the blueprint frame at " + frame.pos() + " holds no scanned blueprint");
                    }
                    if (op instanceof TcOp.OctantFrame frame) {
                        List<ItemFrame> found = level.getEntitiesOfClass(ItemFrame.class, new AABB(frame.pos()));
                        helper.assertTrue(!found.isEmpty() && OctantShape.bounds(OctantShape.data(found.getFirst().getItem())) != null,
                                "the octant frame at " + frame.pos() + " holds no octant with both corners");
                    }
                    if (op instanceof TcOp.Hammock hammock) {
                        var spot = new com.simplebuilding.blocks.custom.HammockLayout.Spot(
                                hammock.pos(), hammock.dx(), hammock.dz());
                        helper.assertTrue(com.simplebuilding.blocks.custom.HammockLayout.intact(level, spot.clothHead()),
                                "built hammock is not linked or its anchor is invalid: " + spot);
                    }
                }
            }
            int drops = level.getEntitiesOfClass(ItemEntity.class, area).size();
            helper.assertTrue(drops == 0, drops + " dropped items lie in the test centre after the build");

            TestCentreBuilder.buildSection(level, plan, "armour");
            int again = level.getEntitiesOfClass(ItemFrame.class, area).size();
            helper.assertTrue(again == frames, "rebuilding the armour section changed the frame count from " + frames + " to " + again);
            BoundingBox box = plan.section("armour").box(origin);
            helper.assertTrue(box.getXSpan() > 10, "armour section is implausibly small: " + box);
        } finally {
            TestCentreBuilder.clear(level, plan.bounds());
        }
        helper.succeed();
    }
    // ------------------------------------------------------------------ Befehlsbloecke und Ausgabe-Knoepfe

    /** Alle Befehlsbloecke der Planung (Steuerwand und Ausgabe-Knoepfe), in Planungsreihenfolge. */
    static List<TcOp.Command> commands(TestCentreLayout.Plan plan) {
        List<TcOp.Command> out = new ArrayList<>();
        for (TestCentreLayout.Section section : plan.sections()) {
            for (TcOp op : section.ops()) {
                if (op instanceof TcOp.Command command) {
                    out.add(command);
                }
            }
        }
        return out;
    }

    /**
     * Ohne Welt, aus der Planung: kein Befehlsblock beruehrt einen anderen oder dessen Knopf, kein
     * Leiterblock (Stein, Wand) beruehrt zwei Befehlsbloecke, Knopf- und Schildplatz sind frei. Ein Knopf
     * versorgt seinen Befehlsblock STARK; ein stark versorgter Leiter schaltet jeden Befehlsblock daneben
     * mit - bis 2026-09-25 loeste so jeder Knopf der Steuerwand auch den Nachbarbefehl aus.
     *
     * <p>Dazu die Ausgabe-Knoepfe: jede Station ausser der Steuerwand hat vorn links einen, der Knopf zeigt
     * zum Gang (Norden, vorderste Reihe), sein Kit ist nicht leer und passt ins Inventar; der Befehl
     * {@code /sbtestcentre give} ist registriert und fuellt Haupt-, Nebenhand und Inventar. Der
     * Fingerabdruck der Planung ist stabil und haengt am Ursprung. Das Flypad-Flugfeld beruehrt kein
     * anderes Pad.
     */
    @SuppressWarnings("removal")
    public static void commandBlocksAreIsolatedAndEveryStationHasItsGiveButton(GameTestHelper helper) {
        BlockPos origin = new BlockPos(0, 64, 0);
        TestCentreLayout.Plan plan = TestCentreLayout.plan(helper.getLevel(), origin);
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (TestCentreLayout.Section section : plan.sections()) {
            for (TcOp op : section.ops()) {
                if (op instanceof TcOp.Place place) {
                    blocks.put(place.pos(), place.state());
                }
            }
        }
        List<TcOp.Command> commands = commands(plan);
        Map<BlockPos, TcOp.Command> byPos = new HashMap<>();
        Map<BlockPos, TcOp.Command> byButton = new HashMap<>();
        for (TcOp.Command command : commands) {
            byPos.put(command.pos(), command);
            byButton.put(command.pos().relative(command.facing()), command);
        }
        List<String> problems = new ArrayList<>();
        for (TcOp.Command command : commands) {
            String name = "'" + command.command() + "' at " + command.pos().toShortString();
            BlockPos button = command.pos().relative(command.facing());
            BlockPos sign = command.pos().above().relative(command.facing());
            if (blocks.containsKey(button) && !blocks.get(button).isAir()) {
                problems.add(name + ": its button place is taken by " + blocks.get(button));
            }
            if (blocks.containsKey(sign) && !blocks.get(sign).isAir()) {
                problems.add(name + ": its sign place is taken by " + blocks.get(sign));
            }
            if (blocks.containsKey(command.pos())) {
                problems.add(name + ": a block is planned into the command block itself");
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = command.pos().relative(direction);
                TcOp.Command other = byPos.get(next);
                if (other != null) {
                    problems.add(name + " touches the command block '" + other.command() + "'");
                }
                TcOp.Command buttonOwner = byButton.get(next);
                if (buttonOwner != null && buttonOwner != command) {
                    problems.add(name + " touches the button of '" + buttonOwner.command() + "'");
                }
            }
        }
        // Leiterbloecke neben zwei Befehlsbloecken.
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            if (!entry.getValue().isRedstoneConductor(EmptyBlockGetter.INSTANCE, entry.getKey())) {
                continue;
            }
            List<String> touching = new ArrayList<>();
            for (Direction direction : Direction.values()) {
                TcOp.Command command = byPos.get(entry.getKey().relative(direction));
                if (command != null) {
                    touching.add("'" + command.command() + "'");
                }
            }
            if (touching.size() > 1) {
                problems.add("the conductor " + entry.getValue() + " at " + entry.getKey().toShortString() + " touches " + touching);
            }
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " command block isolation problem(s): "
                + String.join("; ", problems.subList(0, Math.min(10, problems.size()))));

        // Ausgabe-Knoepfe: jede Station mit Inhalt, Knopf vorn zum Gang.
        List<String> stations = new ArrayList<>();
        for (TestCentreLayout.Section section : plan.sections()) {
            String id = section.id();
            if (id.equals("controls")) {
                continue;
            }
            TestCentreKits.Kit kit = plan.kits().get(id);
            // Eine Station ohne Kit ist nur erlaubt, wenn sie nichts zeigt (etwa "devices", sobald jede
            // Tab-Zeile eine eigene Station hat, oder ein leeres "unsorted"): dann gibt es keinen Knopf.
            if (kit == null) {
                if (!TestCentreKits.of(new TcContext(helper.getLevel().registryAccess()), id, section.ops()).isEmpty()) {
                    stations.add(id + " shows items but has no kit");
                }
                if (section.ops().stream().anyMatch(op -> op instanceof TcOp.Command command
                        && command.command().equals(TestCentreLayout.giveCommand(id)))) {
                    stations.add(id + " has nothing to give but a give button");
                }
                continue;
            }
            helper.assertTrue(!kit.mainHand().isEmpty(), "the kit of " + id + " leaves the main hand empty");
            helper.assertTrue(kit.inventory().size() + (kit.mainHand().isEmpty() ? 0 : 1) <= TestCentreKits.MAIN_SLOTS,
                    "the kit of " + id + " does not fit into the inventory: " + kit.inventory().size());
            BlockPos front = origin.offset(section.offset());
            TcOp.Command give = null;
            for (TcOp op : section.ops()) {
                if (op instanceof TcOp.Command command && command.command().equals(TestCentreLayout.giveCommand(id))) {
                    give = command;
                }
            }
            if (give == null) {
                stations.add(id + " has no give button");
                continue;
            }
            if (give.facing() != Direction.NORTH || give.pos().relative(give.facing()).getZ() != front.getZ()) {
                stations.add(id + ": the give button does not face the aisle (" + give.facing() + ", z "
                        + give.pos().relative(give.facing()).getZ() + " instead of " + front.getZ() + ")");
            }
        }
        helper.assertTrue(stations.isEmpty(), "give buttons: " + String.join("; ", stations));
        for (String withOffHand : List.of("tools", "chisel", "inworld", "upgrades", "templates", "planning", "states")) {
            TestCentreKits.Kit kit = plan.kits().get(withOffHand);
            helper.assertTrue(kit != null && !kit.offHand().isEmpty(), "the kit of " + withOffHand + " should fill the off hand");
        }
        helper.assertTrue(plan.kits().get("planning").offHand().is(ModItems.OCTANT),
                "the planning kit should hold the octant in the off hand (roof mode needs it there)");

        // Der Befehl selbst: registriert, und er ersetzt das Inventar durch das Kit.
        var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("sbtestcentre");
        helper.assertTrue(root != null && root.getChild("give") != null && root.getChild("give").getChild("station") != null,
                "/sbtestcentre give <station> is not registered");
        TestCentreKits.Kit blocksKit = plan.kits().get("blocks");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            player.getInventory().setItem(20, new ItemStack(Items.DIRT, 5));
            TestCentreCommand.giveKit(player, blocksKit);
            helper.assertTrue(ItemStack.isSameItemSameComponents(player.getMainHandItem(), blocksKit.mainHand()),
                    "give: main hand holds " + player.getMainHandItem() + " instead of " + blocksKit.mainHand());
            int dirt = 0;
            int kitStacks = 0;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.is(Items.DIRT)) {
                    dirt += stack.getCount();
                }
                for (ItemStack wanted : blocksKit.inventory()) {
                    if (ItemStack.isSameItemSameComponents(stack, wanted)) {
                        kitStacks++;
                        break;
                    }
                }
            }
            helper.assertTrue(dirt == 0, "give should replace the inventory, but " + dirt + " dirt stayed");
            helper.assertTrue(kitStacks == blocksKit.inventory().size(),
                    "give: " + kitStacks + " of " + blocksKit.inventory().size() + " kit stacks arrived in the inventory");
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }

        // Fingerabdruck: gleiche Planung gleicher Wert, anderer Ursprung (andere Befehle) anderer Wert.
        String fingerprint = plan.fingerprint();
        helper.assertTrue(fingerprint.equals(TestCentreLayout.plan(helper.getLevel(), origin).fingerprint()),
                "the plan fingerprint is not stable - every join would rebuild the centre");
        helper.assertTrue(!fingerprint.equals(TestCentreLayout.plan(helper.getLevel(), origin.east(7)).fingerprint()),
                "the plan fingerprint ignores the origin");

        // Flypad: im Flugfeld steht kein anderes Pad und keine Druckplatte.
        List<String> crowded = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            if (!(entry.getValue().getBlock() instanceof FlypadBlock flypad)) {
                continue;
            }
            AABB field = PadTiers.flyArea(entry.getKey(), flypad.getTier());
            for (Map.Entry<BlockPos, BlockState> other : blocks.entrySet()) {
                Block block = other.getValue().getBlock();
                boolean pad = block instanceof PadBlock || block instanceof BasePressurePlateBlock;
                if (pad && !other.getKey().equals(entry.getKey()) && field.intersects(new AABB(other.getKey()))) {
                    crowded.add(BuiltInRegistries.BLOCK.getKey(block) + " at " + other.getKey().toShortString());
                }
            }
        }
        helper.assertTrue(crowded.isEmpty(), "the flypad's flight field reaches other pads: " + crowded);
        helper.succeed();
    }

    /**
     * Module split 3c / TZ22-1: a station another module registers through {@code framework.api.TestCentreStations} shows
     * its items in the section "modules" (before "mobs"), in the owner's bay, unknown ids skipped - as a block when it can
     * stand, else in a frame - unless another station already shows them. Registered and taken out within this one call.
     */
    public static void foreignModuleStationsGetTheirOwnSection(GameTestHelper helper) {
        if (!com.simplebuilding.dev.testcentre.ModuleStations.SUPPORTED) {
            helper.succeed(); // 26.2: no framework, no foreign stations
            return;
        }
        TestCentreLayout.Plan baseline = TestCentreLayout.plan(helper.getLevel(), BlockPos.ZERO);
        List<String> baselineIds = baseline.sections().stream().map(TestCentreLayout.Section::id).toList();
        // Two Vanilla items no station shows yet: one that stands as a block, one that needs a frame.
        Item block = null;
        Item framed = null;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft") || item == Items.AIR
                    || baseline.coveredItems().contains(item)) {
                continue;
            }
            boolean stands = !com.simplebuilding.dev.testcentre.Exhibits.floorStates(new ItemStack(item)).isEmpty();
            if (stands && block == null) {
                block = item;
            } else if (!stands && !(item instanceof net.minecraft.world.item.BlockItem) && framed == null) {
                framed = item;
            }
        }
        helper.assertTrue(block != null && framed != null, "no unshown Vanilla items to test with");
        String blockId = BuiltInRegistries.ITEM.getKey(block).toString();
        String framedId = BuiltInRegistries.ITEM.getKey(framed).toString();
        com.simplebuilding.dev.testcentre.ModuleStations.register("simpletest", "station", "section.simpletest.station",
                List.of(blockId, "nomod:missing", framedId));
        TestCentreLayout.Plan plan;
        try {
            plan = TestCentreLayout.plan(helper.getLevel(), BlockPos.ZERO);
        } finally {
            com.simplebuilding.dev.testcentre.ModuleStations.unregister("simpletest", "station");
        }
        List<String> ids = plan.sections().stream().map(TestCentreLayout.Section::id).toList();
        helper.assertTrue(ids.indexOf(TestCentreLayout.MODULES_SECTION) == ids.indexOf("mobs") - 1,
                "the modules section is not planned right before mobs: " + ids);
        TestCentreLayout.Section modules = plan.section(TestCentreLayout.MODULES_SECTION);
        Item standing = block;
        Item lying = framed;
        boolean placed = modules.ops().stream().anyMatch(op -> op instanceof TcOp.Place place && place.state().getBlock().asItem() == standing);
        boolean inFrame = frames(modules).values().stream().anyMatch(stack -> stack.getItem() == lying);
        helper.assertTrue(placed, "the modules section does not set " + blockId + " as a block");
        helper.assertTrue(inFrame, "the modules section does not frame " + framedId);
        helper.assertTrue(frames(modules).values().stream().noneMatch(stack -> stack.getItem() == standing),
                "the modules section frames " + blockId + " although it can stand as a block");
        // Simple Ovens (required with SimpleBuilding on 26.3) is shown: its furnaces stand in "machines", not again here.
        Item furnace = com.simplebuilding.util.OvenRefs.item("enderite_furnace");
        helper.assertTrue(baseline.coveredItems().contains(furnace), "the Simple Ovens furnace is not in the test centre");
        TestCentreLayout.Plan without = TestCentreLayout.plan(helper.getLevel(), BlockPos.ZERO);
        helper.assertTrue(without.sections().stream().map(TestCentreLayout.Section::id).toList().equals(baselineIds),
                "after unregistering the test station the plan differs from before");
        helper.assertTrue(TestCentreLayout.SECTION_IDS.stream().allMatch(baselineIds::contains),
                "the plan lost sections of SECTION_IDS: " + baselineIds);
        helper.succeed();
    }

    // ------------------------------------------------------------------ Teststationen (B12/P10)

    /** Die Rahmen eines Abschnitts nach Lage, in Planungsreihenfolge. */
    static Map<BlockPos, ItemStack> frames(TestCentreLayout.Section section) {
        Map<BlockPos, ItemStack> out = new LinkedHashMap<>();
        for (TcOp op : section.ops()) {
            if (op instanceof TcOp.Frame frame) {
                out.put(frame.pos(), frame.stack());
            }
        }
        return out;
    }

    /**
     * Station "states": kaputt neben heil. Resonanzstab und Rotator leer, halb und voll; das Echolot in
     * allen drei Riss-Stufen seines Modells und repariert; jedes abnutzbare Werkzeug und Ruestungsteil der
     * Haltbarkeits-Zeilen fast verbraucht mit dem neuen Stueck direkt daneben; rissiger und heiler
     * Diamantblock gesetzt, Amboss, Hochofen mit rissigen Diamanten, abgenutzter und neuer Ruestungsstaender.
     */
    public static void brokenAndRepairedStatesStandSideBySide(GameTestHelper helper) {
        TestCentreLayout.Plan plan = TestCentreLayout.plan(helper.getLevel(), BlockPos.ZERO);
        TcContext ctx = new TcContext(helper.getLevel().registryAccess());
        TestCentreLayout.Section states = plan.section("states");
        Map<BlockPos, ItemStack> frames = frames(states);
        List<String> problems = new ArrayList<>();
        for (Item gadget : List.of(TweaksItems.LASER_POINTER, ModItems.ROTATOR)) {
            boolean empty = frames.values().stream().anyMatch(s -> s.is(gadget) && s.getDamageValue() == s.getMaxDamage());
            boolean half = frames.values().stream().anyMatch(s -> s.is(gadget) && s.getDamageValue() > 0 && s.getDamageValue() < s.getMaxDamage());
            boolean full = frames.values().stream().anyMatch(s -> s.is(gadget) && s.getDamageValue() == 0);
            if (!empty || !half || !full) {
                problems.add(BuiltInRegistries.ITEM.getKey(gadget) + " lacks a state (empty " + empty + ", half " + half + ", full " + full + ")");
            }
        }
        Set<Integer> crackStages = new HashSet<>();
        boolean repaired = false;
        for (ItemStack stack : frames.values()) {
            if (stack.is(TweaksItems.ECHO_COMPASS)) {
                if (stack.isDamaged()) {
                    // Wie das Item-Modell: normierter Schaden, Schwellen 0, 1/3, 2/3.
                    crackStages.add(Math.min(2, (int) (3.0F * stack.getDamageValue() / stack.getMaxDamage())));
                } else {
                    repaired = true;
                }
            }
        }
        if (crackStages.size() != 3 || !repaired) {
            problems.add("echo sounder: crack stages " + crackStages + " of 3, repaired " + repaired);
        }
        int pairs = 0;
        for (String row : FeatureStations.DURABILITY_ROWS) {
            for (ItemStack stack : ctx.row(row)) {
                if (!stack.isDamageableItem() || FeatureStations.hasOwnStateLine(stack.getItem())) {
                    continue;
                }
                boolean found = false;
                for (Map.Entry<BlockPos, ItemStack> entry : frames.entrySet()) {
                    ItemStack worn = entry.getValue();
                    ItemStack partner = frames.get(entry.getKey().east());
                    if (worn.is(stack.getItem()) && worn.getDamageValue() == worn.getMaxDamage() - 1
                            && partner != null && partner.is(stack.getItem()) && partner.getDamageValue() == 0) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    problems.add(BuiltInRegistries.ITEM.getKey(stack.getItem()) + " (" + row + ") has no worn frame next to a new one");
                }
                pairs++;
            }
        }
        Set<Block> placed = new HashSet<>();
        for (TcOp op : states.ops()) {
            if (op instanceof TcOp.Place place) {
                placed.add(place.state().getBlock());
            }
        }
        for (Block block : List.of(ModBlocks.CRACKED_DIAMOND_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.ANVIL, Blocks.BLAST_FURNACE)) {
            if (!placed.contains(block)) {
                problems.add("states does not place " + BuiltInRegistries.BLOCK.getKey(block));
            }
        }
        boolean blasting = states.ops().stream().anyMatch(op -> op instanceof TcOp.Fill fill
                && fill.contents().stream().anyMatch(s -> s.is(ModItems.CRACKED_DIAMOND)));
        if (!blasting) {
            problems.add("the blast furnace holds no cracked diamonds");
        }
        long wornStands = states.ops().stream().filter(op -> op instanceof TcOp.Stand stand
                && stand.gear().stream().anyMatch(ItemStack::isDamaged)).count();
        long newStands = states.ops().stream().filter(op -> op instanceof TcOp.Stand stand
                && stand.gear().stream().noneMatch(ItemStack::isDamaged)).count();
        if (wornStands < 1 || newStands < 1) {
            problems.add("armour stands: worn " + wornStands + ", new " + newStands);
        }
        helper.assertTrue(pairs > 40, "suspiciously few durability pairs: " + pairs);
        helper.assertTrue(problems.isEmpty(), problems.size() + " state problem(s): " + String.join("; ", problems));
        helper.succeed();
    }

    /**
     * Item-orientierter Rundgang: je Kreativ-Tab der Mod ein Abschnitt {@code tab_<id>}, dessen Rahmen
     * genau den Tab zeigen (gleiche Stapel, gleiche Reihenfolge), mit Ausgabe-Knopf - ausser was dort als Block steht oder
     * haengt und was eine Station schon gesetzt zeigt (TZ22-2). Die Waende zaehlen
     * nicht fuer die Abdeckung: jedes Item braucht trotzdem eine Station.
     */
    public static void everyCreativeTabHasItsItemBrowserWall(GameTestHelper helper) {
        TestCentreLayout.Plan plan = TestCentreLayout.plan(helper.getLevel(), BlockPos.ZERO);
        TcContext ctx = new TcContext(helper.getLevel().registryAccess());
        List<String> problems = new ArrayList<>();
        // TZ22-2: was eine Station gesetzt zeigt, rahmt die Tab-Wand nicht noch einmal.
        Set<Item> placedAnywhere = new HashSet<>();
        for (TestCentreLayout.Section section : plan.sections()) {
            if (!section.id().startsWith(TabBrowser.PREFIX)) {
                placedAnywhere.addAll(TestCentreLayout.shownPlaced(section.ops()));
            }
        }
        for (ModItemGroupsContent.Tab tab : ModItemGroupsContent.Tab.values()) {
            String id = TabBrowser.sectionId(tab);
            if (!TestCentreLayout.SECTION_IDS.contains(id)) {
                problems.add(id + " is missing from SECTION_IDS");
                continue;
            }
            // TZ22-2: was sich in eine Wand setzen laesst, steht dort als Block; der Rest haengt in Tab-Reihenfolge im Rahmen.
            List<ItemStack> expected = ctx.tab(tab);
            List<ItemStack> framedExpected = new ArrayList<>();
            Set<Block> placed = new HashSet<>();
            for (TcOp op : plan.section(id).ops()) {
                if (op instanceof TcOp.Place place) {
                    placed.add(place.state().getBlock());
                }
            }
            for (ItemStack stack : expected) {
                BlockState block = com.simplebuilding.dev.testcentre.Exhibits.wallState(stack);
                if (block == null) {
                    block = com.simplebuilding.dev.testcentre.Exhibits.wallFrontState(stack);
                }
                if (block == null) {
                    if (!(stack.getComponentsPatch().isEmpty() && placedAnywhere.contains(stack.getItem()))) {
                        framedExpected.add(stack);
                    }
                } else if (!placed.contains(block.getBlock())) {
                    problems.add(id + " should set " + BuiltInRegistries.ITEM.getKey(stack.getItem()) + " as a block in the wall");
                }
            }
            List<ItemStack> shown = new ArrayList<>(frames(plan.section(id)).values());
            if (shown.size() != framedExpected.size()) {
                problems.add(id + " frames " + shown.size() + " stacks, the tab holds " + framedExpected.size() + " that need a frame");
                continue;
            }
            for (int i = 0; i < framedExpected.size(); i++) {
                if (!ItemStack.isSameItemSameComponents(shown.get(i), framedExpected.get(i))) {
                    problems.add(id + " frame " + i + " shows " + shown.get(i) + " instead of " + framedExpected.get(i));
                    break;
                }
            }
            if (!expected.isEmpty() && plan.kits().get(id) == null) {
                problems.add(id + " has no give button");
            }
        }
        helper.assertTrue(!ctx.tab(ModItemGroupsContent.Tab.TOOLS).isEmpty(), "the tools tab is empty");
        helper.assertTrue(problems.isEmpty(), problems.size() + " item browser problem(s): " + String.join("; ", problems));
        helper.succeed();
    }

    /**
     * Testorientierter Rundgang: jede Mod-Verzauberung steht auf einem Gegenstand (kein Buch) in einer
     * Station zum Ausprobieren; jedes Kleinteil und jedes Ei liegt abgelegt in "placeables"; jeder
     * Eisenstab steht unter freiem Himmel (nichts darueber, kein Kupfer-Blitzableiter, der ihm die Blitze
     * nimmt) und die Steuerwand hat einen Gewitter-Knopf; der Fallturm des Sinkdaempfers ist hoch genug,
     * seine Leiter reicht bis oben, und ein Staender traegt den vollen Enderit-Satz.
     */
    public static void everyFeatureStationSetsUpItsScenario(GameTestHelper helper) {
        BlockPos origin = new BlockPos(0, 64, 0);
        TestCentreLayout.Plan plan = TestCentreLayout.plan(helper.getLevel(), origin);
        TcContext ctx = new TcContext(helper.getLevel().registryAccess());
        List<String> problems = new ArrayList<>();
        if (!ModBlocks.HAMMOCKS.isEmpty()) {
            assertHammockExhibits(helper, plan);
        }

        // Verzauberungen.
        List<ItemStack> handsOn = new ArrayList<>();
        for (String id : List.of("mining", "planning", "chisel", "enchants")) {
            handsOn.addAll(frames(plan.section(id)).values());
        }
        ctx.enchantmentLookup().listElements().forEach(ref -> {
            if (!TcContext.isMod(ref.key().identifier())) {
                return;
            }
            boolean shown = handsOn.stream().anyMatch(stack -> !stack.is(Items.ENCHANTED_BOOK)
                    && stack.getEnchantments().getLevel(ref) > 0);
            if (!shown) {
                problems.add("enchantment " + ref.key().identifier() + " has no hands-on station (only a book)");
            }
        });

        // Kleinteile und Eier.
        TestCentreLayout.Section placeables = plan.section("placeables");
        if (com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            List<Item> parts = FeatureStations.smallParts();
            helper.assertTrue(parts.contains(ModItems.STONE_PEBBLE), "the small parts lack the stone pebble: " + parts);
            for (Item part : parts) {
                boolean laid = placeables.ops().stream().anyMatch(op -> op instanceof TcOp.Fill fill
                        && !fill.contents().isEmpty() && fill.contents().getFirst().is(part));
                if (!laid) {
                    problems.add("small part " + BuiltInRegistries.ITEM.getKey(part) + " is not laid down in placeables");
                }
            }
            Set<Object> eggs = new HashSet<>();
            for (TcOp op : placeables.ops()) {
                if (op instanceof TcOp.Place place && place.state().getBlock() == ModBlocks.PLACED_EGG) {
                    eggs.add(place.state().getValue(com.simplebuilding.blocks.custom.PlacedEggBlock.EGG));
                }
            }
            if (eggs.size() != com.simplebuilding.blocks.custom.PlacedEggBlock.Egg.values().length) {
                problems.add("placeables stands up " + eggs.size() + " egg kinds of "
                        + com.simplebuilding.blocks.custom.PlacedEggBlock.Egg.values().length);
            }
        }

        // Metallstaebe (Eisen, Gold, Netherit, Enderit) unter freiem Himmel.
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (TestCentreLayout.Section section : plan.sections()) {
            for (TcOp op : section.ops()) {
                if (op instanceof TcOp.Place place) {
                    blocks.put(place.pos(), place.state());
                }
            }
        }
        int rods = 0;
        Set<Object> metalRods = new HashSet<>();
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            if (entry.getValue().getBlock() instanceof net.minecraft.world.level.block.LightningRodBlock
                    && !(entry.getValue().getBlock() instanceof com.simplebuilding.blocks.custom.MetalRodBlock)) {
                problems.add("a copper lightning rod at " + entry.getKey().toShortString() + " would take the iron rods' lightning");
            }
            if (!(entry.getValue().getBlock() instanceof com.simplebuilding.blocks.custom.MetalRodBlock)) {
                continue;
            }
            metalRods.add(entry.getValue().getBlock());
            if (entry.getValue().is(ModBlocks.IRON_POLE)) {
                rods++;
            }
            for (Map.Entry<BlockPos, BlockState> other : blocks.entrySet()) {
                BlockPos pos = other.getKey();
                if (pos.getX() == entry.getKey().getX() && pos.getZ() == entry.getKey().getZ() && pos.getY() > entry.getKey().getY()
                        && !other.getValue().isAir()) {
                    problems.add("the metal rod " + entry.getValue().getBlock() + " at " + entry.getKey().toShortString() + " has " + other.getValue() + " above it");
                }
            }
        }
        if (ModBlocks.IRON_POLE != null && rods == 0) {
            problems.add("no iron rod stands anywhere");
        }
        if (ModBlocks.IRON_POLE != null && !metalRods.containsAll(List.of(ModBlocks.GOLD_POLE, ModBlocks.DIAMOND_POLE, ModBlocks.NETHERITE_POLE, ModBlocks.ENDERITE_POLE))) {
            problems.add("not every metal rod stands in the test centre: " + metalRods);
        }
        if (commands(plan).stream().noneMatch(command -> command.command().equals("weather thunder"))) {
            problems.add("the controls have no thunder button");
        }

        // Fallturm.
        TestCentreLayout.Section tower = plan.section("sinkdamper");
        BlockPos top = plan.anchors().get("sink_tower_top");
        if (top == null || top.getY() - origin.getY() < 10) {
            problems.add("the sink damper tower is missing or too low: " + top);
        } else {
            long ladders = tower.ops().stream().filter(op -> op instanceof TcOp.Place place && place.state().is(Blocks.LADDER)).count();
            if (ladders < top.getY() - origin.getY()) {
                problems.add("the tower ladder has " + ladders + " rungs for " + (top.getY() - origin.getY()) + " blocks");
            }
            BlockState below = blocks.get(top.below());
            if (below == null || below.isAir()) {
                problems.add("the tower top has no platform");
            }
        }
        boolean suit = tower.ops().stream().anyMatch(op -> op instanceof TcOp.Stand stand && stand.gear().stream()
                .filter(com.simplebuilding.util.EnderiteSinkDamper::isEnderiteArmor).count() == 4);
        if (!suit) {
            problems.add("no armour stand wears the full enderite set at the tower");
        }
        ridingSaddlesAHorse(helper, plan, ctx, problems);
        helper.assertTrue(problems.isEmpty(), problems.size() + " station problem(s): " + String.join("; ", problems));
        helper.succeed();
    }

    /** Nach der ersten Ausfuehrung noch weitere Ticks auf doppelte/fremde Ausfuehrungen pruefen. */
    static final int CHECK_AFTER = 3;
    /** Every requested layout has its own labeled, disjoint bay; all colors and anchor types appear. */
    private static void assertHammockExhibits(GameTestHelper helper, TestCentreLayout.Plan plan) {
        var machines = plan.section("machines");
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        Map<BlockPos, Integer> writes = new HashMap<>();
        Map<BlockPos, TcOp.Sign> signs = new HashMap<>();
        for (var section : plan.sections()) {
            if (!section.id().equals("machines")) {
                helper.assertTrue(!machines.box(plan.origin()).intersects(section.box(plan.origin())),
                        "machines overlap section " + section.id());
            }
            for (TcOp op : section.ops()) {
                if (op instanceof TcOp.Place place) {
                    blocks.put(place.pos(), place.state());
                    writes.merge(place.pos(), 1, Integer::sum);
                }
                if (op instanceof TcOp.Sign sign) signs.put(sign.pos(), sign);
            }
        }
        var spans = new HashSet<String>();
        var colors = new HashSet<Block>();
        var anchors = new HashSet<BlockState>();
        var occupied = new HashSet<BlockPos>();
        int count = 0;
        for (TcOp op : machines.ops()) {
            if (!(op instanceof TcOp.Hammock hammock)) continue;
            count++;
            var spot = new com.simplebuilding.blocks.custom.HammockLayout.Spot(hammock.pos(), hammock.dx(), hammock.dz());
            helper.assertTrue(spot.valid(), "invalid exhibit span " + spot);
            String span = hammock.dx() + ":" + hammock.dz();
            spans.add(span);
            colors.add(blocks.get(spot.clothHead()).getBlock());
            var cells = new ArrayList<>(spot.cells());
            for (BlockPos anchor : List.of(spot.anchor(), spot.otherAnchor())) {
                BlockState post = blocks.get(anchor);
                anchors.add(post);
                helper.assertTrue(post != null && (post.is(ModBlocks.NETHERITE_POLE)
                                ? Blocks.OAK_LOG.defaultBlockState() : post).equals(blocks.get(anchor.below())),
                        "exhibit post is not two blocks tall: " + anchor);
                cells.add(anchor);
                cells.add(anchor.below());
            }
            for (BlockPos cell : cells) {
                helper.assertTrue(occupied.add(cell) && writes.getOrDefault(cell, 0) == 1 && !signs.containsKey(cell),
                        "hammock exhibit overlaps another placement at " + cell);
                helper.assertTrue(machines.box(plan.origin()).isInside(cell), "exhibit outside machines: " + cell);
            }
            BlockPos label = new BlockPos(Math.min(spot.anchor().getX(), spot.otherAnchor().getX()),
                    spot.anchor().getY() - 1, spot.anchor().getZ() - 1);
            TcOp.Sign sign = signs.get(label);
            helper.assertTrue(sign != null && sign.lines().size() == 4 && sign.lines().getFirst().getString().equals(span)
                            && sign.lines().stream().noneMatch(line -> line.getString().isBlank()),
                    "missing four-line hammock label for " + spot);
        }
        helper.assertTrue(count == 22, "expected 22 hammock exhibits, got " + count);
        helper.assertTrue(spans.containsAll(List.of("3:0", "4:0", "5:0", "0:3", "0:4", "0:5",
                "3:3", "4:4", "5:5", "3:1", "4:1", "5:2", "3:2", "4:3", "-3:2")),
                "missing hammock lengths, axes or angles: " + spans);
        helper.assertTrue(colors.containsAll(ModBlocks.HAMMOCKS), "not all hammock colors are displayed");
        helper.assertTrue(anchors.contains(Blocks.OAK_FENCE.defaultBlockState())
                        && anchors.contains(Blocks.OAK_LOG.defaultBlockState())
                        && anchors.contains(ModBlocks.NETHERITE_POLE.defaultBlockState()
                                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.UP)),
                "missing fence, log or netherite anchor");
        for (var rod : com.simplebuilding.blocks.custom.StandingRodBlock.Rod.values()) {
            helper.assertTrue(anchors.contains(ModBlocks.STANDING_ROD.defaultBlockState()
                            .setValue(com.simplebuilding.blocks.custom.StandingRodBlock.ROD, rod)),
                    "missing standing rod anchor: " + rod);
        }
    }

    // Serial execution needs a command tick, three observation ticks and a release tick
    // per button, plus asynchronous loading of the remote centre. The old 600 covered
    // only the fixed four-tick schedule and could expire before the final buttons.
    public static final int BUTTON_RUN_MAX_TICKS = 1200;

    /**
     * Die Zentrale steht wirklich (fern aller Tests, Chunks erzwungen), jeder Befehlsblock bekommt statt
     * seines Befehls einen eigenen Scoreboard-Zaehler. Dann wird jeder
     * Knopf der Reihe nach gedrueckt wie von Hand ({@link ButtonBlock#press}); nach seiner Ausfuehrung muss
     * genau der eigene Zaehler um eins gestiegen sein und kein anderer. So faellt jeder Knopf auf, der
     * einen zweiten Befehlsblock ausloest (auch ueber Umwege, die der statische Test nicht kennt), einen
     * doppelt oder gar keinen.
     */
    public static void eachButtonRunsExactlyItsOwnCommandBlock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = new BlockPos(26000, helper.absolutePos(BlockPos.ZERO).getY() + 1, 26000);
        TestCentreLayout.Plan plan = TestCentreLayout.plan(level, origin);
        BoundingBox bounds = plan.bounds();
        Scoreboard scoreboard = level.getScoreboard();
        String objectiveName = "sb_tc_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        Objective objective = scoreboard.addObjective(objectiveName, ObjectiveCriteria.DUMMY,
                Component.literal(objectiveName), ObjectiveCriteria.RenderType.INTEGER, false, null);
        List<int[]> forced = new ArrayList<>();
        for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) {
            for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                if (level.setChunkForced(cx, cz, true)) {
                    forced.add(new int[]{cx, cz});
                }
                level.getChunk(cx, cz);
            }
        }
        Runnable cleanup = () -> {
            TestCentreBuilder.clear(level, bounds);
            level.getBlockTicks().clearArea(bounds);
            level.getFluidTicks().clearArea(bounds);
            for (int[] chunk : forced) {
                level.setChunkForced(chunk[0], chunk[1], false);
            }
            scoreboard.removeObjective(objective);
        };
        helper.runBeforeTestEnd(cleanup);
        List<TcOp.Command> commands = commands(plan);
        // Wait until forced tickets take effect before writing the structure. Each command
        // then waits for its actual execution, including any pending chunk/entity loading.
        var sequence = helper.startSequence().thenWaitUntil(() -> {
            for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) {
                for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                    helper.assertTrue(level.shouldTickBlocksAt(
                                    new net.minecraft.world.level.ChunkPos(cx, cz).pack()),
                            "test centre forced ticket is not active: " + cx + ", " + cz);
                }
            }
        }).thenExecute(cleanupOnFailure(cleanup, () -> {
            // This fixed remote area is outside the GameTest structure's automatic reset.
            // Pending ticks from a previous run can suppress a newly scheduled tick at the
            // same position, even after its block and block entity have been replaced.
            level.getBlockTicks().clearArea(bounds);
            level.getFluidTicks().clearArea(bounds);
            TestCentreBuilder.build(level, plan);
            helper.assertTrue(commands.size() > 20, "suspiciously few command blocks: " + commands.size());
            for (int i = 0; i < commands.size(); i++) {
                TcOp.Command command = commands.get(i);
                helper.assertTrue(level.getBlockEntity(command.pos()) instanceof CommandBlockEntity,
                        "no command block at " + command.pos().toShortString());
                ((CommandBlockEntity) level.getBlockEntity(command.pos())).getCommandBlock().setCommand(
                        // A marker count also depends on entity-section visibility. Scoreboard
                        // increments measure every command execution without that extra clock.
                        "scoreboard players add button_" + i + " " + objectiveName + " 1");
            }
        }));
        for (int i = 0; i < commands.size(); i++) {
            int index = i;
            TcOp.Command pressed = commands.get(i);
            BlockPos buttonPos = pressed.pos().relative(pressed.facing());
            String[] waitingState = {"not pressed"};
            sequence.thenExecute(cleanupOnFailure(cleanup, () -> {
                BlockState button = level.getBlockState(buttonPos);
                helper.assertTrue(button.getBlock() instanceof ButtonBlock,
                        "no button in front of '" + pressed.command() + "' at " + pressed.pos().toShortString() + " but " + button);
                helper.assertTrue(!button.getValue(ButtonBlock.POWERED), "button was already powered: " + pressed.command());
                CommandBlockEntity entity = (CommandBlockEntity) level.getBlockEntity(pressed.pos());
                helper.assertValueEqual(entity.getCommandBlock().getCommand(),
                        "scoreboard players add button_" + index + " " + objectiveName + " 1",
                        "counter command changed before pressing " + pressed.command());
                helper.assertTrue(!entity.isPowered(), "command block was already powered: " + pressed.command());
                ((ButtonBlock) button.getBlock()).press(button, level, buttonPos, null);
                helper.assertTrue(entity.isPowered(), "button did not power its command block: " + pressed.command());
                helper.assertTrue(entity.wasConditionMet(), "command block condition was not met: " + pressed.command());
            })).thenWaitUntil(() -> {
                if (level.getBlockEntity(pressed.pos()) instanceof CommandBlockEntity entity) {
                    waitingState[0] = entity.getCommandBlock().getCommand() + "; powered=" + entity.isPowered()
                            + "; scheduled=" + level.getBlockTicks().hasScheduledTick(pressed.pos(), Blocks.COMMAND_BLOCK)
                            + "; ready=" + level.isPositionTickingWithEntitiesLoaded(
                                    net.minecraft.world.level.ChunkPos.pack(pressed.pos()));
                }
                helper.assertTrue(commandRuns(scoreboard, objective, index) > 0,
                        "waiting for command block " + (index + 1) + "/" + commands.size()
                                + " after pressing '" + pressed.command() + "': " + waitingState[0]);
            })
                    .thenExecuteAfter(CHECK_AFTER, cleanupOnFailure(cleanup, () -> {
                List<String> wrong = new ArrayList<>();
                for (int j = 0; j < commands.size(); j++) {
                    int runs = commandRuns(scoreboard, objective, j);
                    int expected = j <= index ? 1 : 0;
                    if (runs != expected) {
                        wrong.add("'" + commands.get(j).command() + "' ran " + runs + "x");
                    }
                }
                helper.assertTrue(wrong.isEmpty(), "after pressing the button of '" + pressed.command() + "': " + wrong);
                // Loslassen wie der geplante Tick des Knopfs: Zustand zurueck, Traeger benachrichtigen.
                BlockState button = level.getBlockState(buttonPos);
                level.setBlock(buttonPos, button.setValue(ButtonBlock.POWERED, false), Block.UPDATE_ALL);
                level.updateNeighborsAt(pressed.pos(), button.getBlock());
            })).thenExecuteAfter(1, () -> {});
        }
        sequence.thenExecute(cleanup).thenSucceed();
    }

    private static int commandRuns(Scoreboard scoreboard, Objective objective, int index) {
        var score = scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly("button_" + index), objective);
        return score == null ? 0 : score.value();
    }

    // ------------------------------------------------------------------ Mob-Station (Queue N43)

    /**
     * Mob station: every mob of every loaded mod (no Vanilla, no MISC) has its cell - sign with its name, buttons to
     * spawn it still and live and to remove it, a viewpoint anchor - and the buttons' commands really spawn it there.
     * Deceiver: the armored button gives 240 max health. Shellker: a Vanilla End gateway with its bedrock frame, and the
     * guard button puts four Shellkers on the bedrock cross around the gateway. SimpleBuilding alone has no mod mobs; then
     * the same cell code runs with a Vanilla pig as stand-in.
     */
    public static void everyModMobHasItsOwnStationThatSpawnsItInItsHabitat(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<net.minecraft.world.entity.EntityType<?>> found = com.simplebuilding.dev.testcentre.MobStations.modMobs();
        List<net.minecraft.world.entity.EntityType<?>> mobs = found.isEmpty()
                ? List.of(net.minecraft.world.entity.EntityTypes.PIG) : found;
        if (!found.isEmpty()) {
            TestCentreLayout.Plan plan = TestCentreLayout.plan(level, new BlockPos(0, 64, 0));
            for (var type : found) {
                helper.assertTrue(plan.anchors().containsKey(com.simplebuilding.dev.testcentre.MobStations.anchorName(
                        BuiltInRegistries.ENTITY_TYPE.getKey(type))), "the plan has no mob cell for " + BuiltInRegistries.ENTITY_TYPE.getKey(type));
            }
        }
        BlockPos origin = new BlockPos(30000, helper.absolutePos(BlockPos.ZERO).getY() + 1, 30000);
        com.simplebuilding.dev.testcentre.TcCanvas canvas = com.simplebuilding.dev.testcentre.MobStations.build(mobs);
        List<TcOp> ops = new ArrayList<>();
        canvas.ops().forEach(op -> ops.add(op.moved(origin)));
        BoundingBox box = new BoundingBox(origin.getX(), origin.getY() - 1, origin.getZ() - 1,
                origin.getX() + canvas.width() + 1, origin.getY() + canvas.height() + 2, origin.getZ() + canvas.depth() + 1);
        AABB area = AABB.of(box);

        // Planning: per mob a sign with its name, still/live spawn and a remove button.
        Map<String, Integer> expected = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        for (var type : mobs) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            long summons = ops.stream().filter(op -> op instanceof TcOp.Command c && c.command().contains("summon " + id + " ")).count();
            boolean remove = ops.stream().anyMatch(op -> op instanceof TcOp.Command c && c.command().startsWith("kill @e[type=" + id + ","));
            boolean sign = ops.stream().anyMatch(op -> op instanceof TcOp.Sign sg && sg.lines().stream()
                    .anyMatch(line -> line.getString().equals(type.getDescription().getString())));
            if (summons < 2 || !remove || !sign) {
                problems.add(id + ": " + summons + " spawn button(s), remove " + remove + ", sign " + sign);
            }
            int count = 2;
            if (id.toString().equals("simplemobs:deceiver")) count += 1;
            if (id.toString().equals("simplemobs:shellker")) count += 4;
            expected.put(id.toString(), count);
        }
        helper.assertTrue(problems.isEmpty(), "mob cells: " + String.join("; ", problems));

        List<int[]> forced = new ArrayList<>();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                if (level.setChunkForced(cx, cz, true)) {
                    forced.add(new int[]{cx, cz});
                }
                level.getChunk(cx, cz);
            }
        }
        Runnable cleanup = () -> {
            TestCentreBuilder.clear(level, box);
            for (int[] chunk : forced) {
                level.setChunkForced(chunk[0], chunk[1], false);
            }
        };
        helper.runBeforeTestEnd(cleanup);
        java.util.function.Function<String, Integer> count = id -> level.getEntities(
                (net.minecraft.world.entity.Entity) null, area.inflate(2.0),
                e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(id)).size();
        java.util.function.Consumer<String> run = command -> level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput()
                        .withPosition(net.minecraft.world.phys.Vec3.atCenterOf(ops.stream()
                                .filter(op -> op instanceof TcOp.Command c && c.command().equals(command))
                                .findFirst().orElseThrow().pos())), command);
        helper.startSequence().thenWaitUntil(() -> {
            for (int[] chunk : forced) {
                helper.assertTrue(level.shouldTickBlocksAt(new net.minecraft.world.level.ChunkPos(chunk[0], chunk[1]).pack()),
                        "mob station forced ticket is not active");
            }
        }).thenExecute(cleanupOnFailure(cleanup, () -> {
            TestCentreBuilder.clear(level, box);
            TestCentreBuilder.apply(level, ops);
            for (TcOp op : ops) {
                if (op instanceof TcOp.Command c) {
                    helper.assertTrue(level.getBlockEntity(c.pos()) instanceof CommandBlockEntity entity
                            && entity.getCommandBlock().getCommand().equals(c.command()), "no command block for '" + c.command() + "'");
                    if (c.command().contains(" summon ")) {
                        run.accept(c.command());
                    }
                }
            }
        })).thenWaitUntil(() -> {
            for (Map.Entry<String, Integer> entry : expected.entrySet()) {
                helper.assertTrue(count.apply(entry.getKey()) == entry.getValue(),
                        entry.getKey() + ": " + count.apply(entry.getKey()) + " spawned, expected " + entry.getValue());
            }
        }).thenExecute(cleanupOnFailure(cleanup, () -> {
            List<net.minecraft.world.entity.Entity> spawned = level.getEntities((net.minecraft.world.entity.Entity) null, area.inflate(2.0),
                    e -> expected.containsKey(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString()));
            helper.assertTrue(spawned.stream().anyMatch(e -> e instanceof net.minecraft.world.entity.Mob mob && mob.isNoAi()),
                    "the still button should spawn a mob without AI");
            if (expected.containsKey("simplemobs:deceiver")) {
                helper.assertTrue(spawned.stream().anyMatch(e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals("simplemobs:deceiver")
                        && e instanceof net.minecraft.world.entity.LivingEntity living && living.getMaxHealth() >= 240.0F),
                        "the armored Deceiver should have 240 max health");
            }
            if (expected.containsKey("simplemobs:shellker")) {
                BlockPos gateway = null;
                for (TcOp op : ops) {
                    if (op instanceof TcOp.Place place && place.state().is(Blocks.END_GATEWAY)) {
                        gateway = place.pos();
                    }
                }
                helper.assertTrue(gateway != null && level.getBlockState(gateway).is(Blocks.END_GATEWAY), "the Shellker cell has no End gateway");
                int bedrock = 0;
                for (BlockPos pos : BlockPos.betweenClosed(gateway.offset(-1, -2, -1), gateway.offset(1, 2, 1))) {
                    if (level.getBlockState(pos).is(Blocks.BEDROCK)) bedrock++;
                }
                helper.assertTrue(bedrock == 12, "the End gateway frame should hold 12 bedrock like Vanilla's, found " + bedrock);
                int guards = 0;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos cell = gateway.relative(d);
                    guards += level.getEntities((net.minecraft.world.entity.Entity) null, new AABB(cell),
                            e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals("simplemobs:shellker")
                                    && e.blockPosition().equals(cell) && level.getBlockState(cell.below()).is(Blocks.BEDROCK)).size();
                }
                helper.assertTrue(guards == 4, "four Shellker guards should sit on the bedrock around the gateway, found " + guards);
            }
            for (TcOp op : ops) {
                if (op instanceof TcOp.Command c && c.command().startsWith("kill @e[type=")) {
                    run.accept(c.command());
                }
            }
        })).thenWaitUntil(() -> {
            for (String id : expected.keySet()) {
                helper.assertTrue(count.apply(id) == 0, "the remove button left " + count.apply(id) + " " + id);
            }
        }).thenExecute(cleanup).thenSucceed();
    }

    // ------------------------------------------------------------------ TZ22-6: Schienen-Teststrecke

    /** Laufzeit der Schienen-Strecke: Aufbau, zwei Fahrten mit Halt, Aufraeumen (gemessen ~250 Ticks). */
    public static final int RAIL_RUN_MAX_TICKS = 700;

    /** Gemessene Fahrt einer Lore auf der Teststrecke (x-Schritt je Tick, Stillstand, Ankunft am Anschub-Ende). */
    private static final class CartRun {
        net.minecraft.world.entity.vehicle.minecart.AbstractMinecart cart;
        double last, fastest, maxX;
        int still, launchedAt = -1;

        void track(net.minecraft.world.entity.vehicle.minecart.AbstractMinecart found) {
            cart = found;
            last = maxX = found.getX();
            fastest = 0;
            still = 0;
            launchedAt = -1;
        }

        void tick(int now, double launchEndX) {
            if (cart == null || cart.isRemoved()) return;
            double x = cart.getX();
            double step = Math.abs(x - last);
            fastest = Math.max(fastest, step);
            still = step < 1e-4 ? still + 1 : 0;
            maxX = Math.max(maxX, x);
            if (launchedAt < 0 && x >= launchEndX) launchedAt = now;
            last = x;
        }
    }

    /**
     * TZ22-6 (Besitzer 22.10.: "keine richtigen Tests ..., sondern nur vorgebaut"): die Schienen-Teststrecke der Zentrale
     * ({@code TestCentreSections.endRailTracks}, im Abschnitt "devices") wird fern aller Tests mit dem Bau-Code der
     * Zentrale gesetzt (leise, wie im Spiel) und wie von Hand befahren:
     * <ol>
     *   <li>Bremse-Hebel an: das Signal laeuft Redstone - Bruecke - Nihil-Redstone (3. Linie) bis an jede Nihil-Schiene.</li>
     *   <li>Knopf "Loren aufstellen": je eine Lore am Start, ohne Start-Hebel bleiben beide stehen.</li>
     *   <li>Start-Hebel: beide fahren los. Astral (Astral-Redstone, 2. Linie) ist schneller als Vanilla (schnellster Schritt
     *       ueber 0,4 Bl./Tick, nie ueber der eingestellten Hoechstgeschwindigkeit), erreicht das Ende des Anschubs frueher und
     *       haelt auf den gespeisten Nihil-Schienen; die Vanilla-Lore bleibt bei 0,4.</li>
     *   <li>Bremse aus, "Loren entfernen", neu aufstellen (Start-Hebel noch an: sie fahren sofort): die Astral-Lore rollt
     *       ueber die Nihil-Schienen bis ans Ende. "Loren entfernen" raeumt die Strecke.</li>
     * </ol>
     */
    public static void endRailTracksRunTheirCarts(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.END_RAILS) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        TcContext ctx = new TcContext(level.registryAccess());
        helper.assertTrue(com.simplebuilding.dev.testcentre.TestCentreSections.devices(ctx).anchors().containsKey("end_rails_start_lever"),
                "the devices section does not carry the End rail track");
        com.simplebuilding.dev.testcentre.TcCanvas track = com.simplebuilding.dev.testcentre.TestCentreSections.endRailTracks();
        BlockPos origin = new BlockPos(29000, helper.absolutePos(BlockPos.ZERO).getY() + 1, 29000);
        BoundingBox box = new BoundingBox(origin.getX(), origin.getY() - 1, origin.getZ(),
                origin.getX() + track.width(), origin.getY() + track.height() + 1, origin.getZ() + track.depth());
        Map<String, BlockPos> at = new HashMap<>();
        track.anchors().forEach((name, pos) -> at.put(name, origin.offset(pos)));
        int laneA = com.simplebuilding.dev.testcentre.TestCentreSections.RAIL_LANE_ASTRAL;
        double launchEnd = origin.getX() + com.simplebuilding.dev.testcentre.TestCentreSections.RAIL_LAUNCH_END + 0.5;
        int brakeStart = com.simplebuilding.dev.testcentre.TestCentreSections.RAIL_BRAKE_START;
        int end = com.simplebuilding.dev.testcentre.TestCentreSections.RAIL_END;
        double cap = com.simplebuilding.blocks.custom.EndRailPhysics.maxSpeedPerTick();
        List<int[]> forced = new ArrayList<>();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                if (level.setChunkForced(cx, cz, true)) forced.add(new int[]{cx, cz});
                level.getChunk(cx, cz);
            }
        }
        Runnable cleanup = () -> {
            TestCentreBuilder.clear(level, box);
            level.getBlockTicks().clearArea(box);
            for (int[] chunk : forced) level.setChunkForced(chunk[0], chunk[1], false);
        };
        helper.runBeforeTestEnd(cleanup);
        AABB area = AABB.of(box).inflate(1);
        java.util.function.Supplier<List<net.minecraft.world.entity.vehicle.minecart.AbstractMinecart>> carts =
                () -> level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.minecart.AbstractMinecart.class, area);
        java.util.function.Consumer<String> pull = name -> {
            BlockState state = level.getBlockState(at.get(name));
            helper.assertTrue(state.getBlock() instanceof net.minecraft.world.level.block.LeverBlock, "no lever at " + name + " but " + state);
            ((net.minecraft.world.level.block.LeverBlock) state.getBlock()).pull(state, level, at.get(name), null);
        };
        java.util.function.Consumer<String> press = name -> {
            BlockState state = level.getBlockState(at.get(name));
            helper.assertTrue(state.getBlock() instanceof ButtonBlock && !state.getValue(ButtonBlock.POWERED), "no released button at " + name + " but " + state);
            ((ButtonBlock) state.getBlock()).press(state, level, at.get(name), null);
        };
        java.util.function.Predicate<Integer> railPowered = x -> {
            BlockState state = level.getBlockState(origin.offset(x, 0, laneA));
            return state.hasProperty(com.simplebuilding.blocks.custom.EndRailBlock.POWERED) && state.getValue(com.simplebuilding.blocks.custom.EndRailBlock.POWERED);
        };
        CartRun astral = new CartRun(), vanilla = new CartRun();
        int[] now = {0};
        helper.onEachTick(() -> {
            now[0]++;
            astral.tick(now[0], launchEnd);
            vanilla.tick(now[0], launchEnd);
        });
        Runnable assign = () -> {
            List<net.minecraft.world.entity.vehicle.minecart.AbstractMinecart> found = carts.get();
            helper.assertTrue(found.size() == 2, "the place button should set two carts, found " + found.size());
            for (var cart : found) {
                (cart.blockPosition().getZ() == origin.getZ() + laneA ? astral : vanilla).track(cart);
            }
            helper.assertTrue(astral.cart != null && vanilla.cart != null && astral.cart != vanilla.cart, "one cart per lane expected");
        };
        helper.startSequence().thenWaitUntil(() -> {
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
                for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                    helper.assertTrue(level.shouldTickBlocksAt(new net.minecraft.world.level.ChunkPos(cx, cz).pack())
                            && level.isPositionEntityTicking(new BlockPos(cx << 4, origin.getY(), cz << 4)), "track chunks not ticking yet");
                }
            }
        }).thenExecute(cleanupOnFailure(cleanup, () -> {
            TestCentreBuilder.clear(level, box);
            level.getBlockTicks().clearArea(box);
            TestCentreBuilder.apply(level, track.ops().stream().map(op -> op.moved(origin)).toList());
        })).thenIdle(4).thenExecute(cleanupOnFailure(cleanup, () -> {
            helper.assertTrue(!railPowered.test(1) && !railPowered.test(brakeStart), "a rail is powered before any lever was pulled");
            pull.accept("end_rails_brake_lever");
        })).thenIdle(8).thenExecute(cleanupOnFailure(cleanup, () -> {
            for (int x = brakeStart; x < end; x++) {
                helper.assertTrue(railPowered.test(x), "the brake lever (redstone, bridge, Nihil Redstone) does not power the Nihil rail " + x);
            }
            helper.assertTrue(!railPowered.test(1), "the brake lever powered the Astral launch");
            press.accept("end_rails_place_button");
        })).thenWaitUntil(() -> helper.assertTrue(carts.get().size() == 2, "waiting for the two carts"))
                .thenExecute(cleanupOnFailure(cleanup, assign)).thenIdle(6).thenExecute(cleanupOnFailure(cleanup, () -> {
            helper.assertTrue(astral.still >= 4 && vanilla.still >= 4 && Math.abs(astral.cart.getX() - (origin.getX() + 1.5)) < 0.3,
                    "the Astral cart should wait at the start until the lever, it is at " + astral.cart.getX());
            pull.accept("end_rails_start_lever");
        })).thenWaitUntil(() -> helper.assertTrue(astral.cart.getX() > launchEnd && astral.still >= 20 && vanilla.still >= 20,
                "waiting for both carts to come to rest: Astral at " + astral.cart.getX() + " (still " + astral.still + "), vanilla at "
                        + vanilla.cart.getX() + " (still " + vanilla.still + ")"))
                .thenExecute(cleanupOnFailure(cleanup, () -> {
            String run = "Astral fastest " + astral.fastest + " (cap " + cap + "), vanilla fastest " + vanilla.fastest
                    + "; at the launch end after " + astral.launchedAt + " / " + vanilla.launchedAt + " ticks";
            com.simplebuilding.Simplebuilding.LOGGER.info("End rail track: {}; Astral stopped at {}, vanilla at {}", run,
                    astral.cart.getX() - origin.getX(), vanilla.cart.getX() - origin.getX());
            helper.assertTrue(astral.fastest > com.simplebuilding.blocks.custom.EndRailPhysics.VANILLA_MAX_SPEED + 0.05,
                    "the Astral cart never got past vanilla speed: " + run);
            helper.assertTrue(astral.fastest <= cap + 1e-6, "the Astral cart passed the configured top speed: " + run);
            helper.assertTrue(vanilla.fastest > 0.2 && vanilla.fastest <= com.simplebuilding.blocks.custom.EndRailPhysics.VANILLA_MAX_SPEED + 0.02,
                    "the vanilla cart should run at up to 0.4 blocks per tick: " + run);
            helper.assertTrue(astral.launchedAt > 0 && (vanilla.launchedAt < 0 || astral.launchedAt < vanilla.launchedAt),
                    "the Astral cart should reach the end of the launch first: " + run);
            double stop = astral.cart.getX() - origin.getX();
            helper.assertTrue(stop >= brakeStart && stop < end, "the powered Nihil rails should hold the Astral cart, it stopped at " + stop);
            pull.accept("end_rails_brake_lever");
            press.accept("end_rails_remove_button");
        })).thenWaitUntil(() -> helper.assertTrue(carts.get().isEmpty(), "the remove button left " + carts.get().size() + " carts"))
                .thenWaitUntil(() -> helper.assertTrue(!level.getBlockState(at.get("end_rails_place_button")).getValue(ButtonBlock.POWERED),
                        "waiting for the place button to pop out"))
                .thenExecute(cleanupOnFailure(cleanup, () -> {
            helper.assertTrue(!railPowered.test(brakeStart), "the Nihil rails stay powered with the brake lever off");
            press.accept("end_rails_place_button");
        })).thenWaitUntil(() -> helper.assertTrue(carts.get().size() == 2, "waiting for the two carts again"))
                .thenExecute(cleanupOnFailure(cleanup, assign))
                .thenWaitUntil(() -> helper.assertTrue(astral.cart.getX() > launchEnd && astral.still >= 20,
                        "waiting for the Astral cart to roll out: at " + astral.cart.getX()))
                .thenExecute(cleanupOnFailure(cleanup, () -> {
            double far = astral.maxX - origin.getX();
            helper.assertTrue(far >= end - 1.5, "without the brake the Astral cart should roll over the Nihil rails to the end, it reached " + far);
        })).thenWaitUntil(() -> helper.assertTrue(!level.getBlockState(at.get("end_rails_remove_button")).getValue(ButtonBlock.POWERED),
                "waiting for the remove button to pop out"))
                .thenExecute(cleanupOnFailure(cleanup, () -> press.accept("end_rails_remove_button")))
                .thenWaitUntil(() -> helper.assertTrue(carts.get().isEmpty(), "the remove button left carts"))
                .thenExecute(cleanup).thenSucceed();
    }

    // ------------------------------------------------------------------ TZ22-8: Wege auf den Schildern

    /**
     * TZ22-8 (Besitzer 22.10.: "am Eisenblock noch Hammer + 4 -> 2 Iron Poles, obwohl die Herstellung auf den Tiegel
     * umgestellt ist"): jedes Schild der Zentrale, das einen Weg zeigt, traegt ihn als Daten ({@code TcWays.Way}), und
     * jeden dieser Wege gibt es im laufenden Spiel (Aufwertungs-Engine, InWorldTransformations, Tiegel-API + Tags,
     * Hochofen-Rezepte). Kein anderes Schild zeigt einen Pfeil ("->"), ausser den beiden Ablauf-Texten. Gegenprobe:
     * ausgedachte Wege (der alte Tiegelbau am Eisenblock, ein falsches Rezept) werden abgelehnt.
     */
    public static void everyTransformationSignShowsAnExistingWay(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TestCentreLayout.Plan plan = TestCentreLayout.plan(level, BlockPos.ZERO);
        Set<String> flowTexts = Set.of(com.simplebuilding.dev.testcentre.TcText.PREFIX + "section.machines.sub",
                com.simplebuilding.dev.testcentre.TcText.PREFIX + "states.gadgets.sub");
        List<String> problems = new ArrayList<>();
        Set<com.simplebuilding.dev.testcentre.TcWays.Kind> kinds = new HashSet<>();
        int ways = 0;
        for (TestCentreLayout.Section section : plan.sections()) {
            for (TcOp op : section.ops()) {
                if (!(op instanceof TcOp.Sign sign)) {
                    continue;
                }
                if (sign.way() != null) {
                    ways++;
                    kinds.add(sign.way().kind());
                    String problem = com.simplebuilding.dev.testcentre.TcWays.problem(sign.way(), level);
                    if (problem != null) {
                        problems.add(section.id() + " " + sign.pos().toShortString() + ": " + problem);
                    }
                    continue;
                }
                for (Component line : sign.lines()) {
                    String text = line.getString();
                    String key = line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : "";
                    if ((text.contains("->") || text.contains("→")) && !flowTexts.contains(key)) {
                        problems.add(section.id() + " " + sign.pos().toShortString() + ": the sign line '" + text + "' (" + key
                                + ") shows a way that no data backs");
                    }
                }
            }
        }
        List<com.simplebuilding.dev.testcentre.TcWays.Kind> expected = new ArrayList<>(List.of(com.simplebuilding.dev.testcentre.TcWays.Kind.UPGRADE,
                com.simplebuilding.dev.testcentre.TcWays.Kind.CRUSH, com.simplebuilding.dev.testcentre.TcWays.Kind.SHEAR,
                com.simplebuilding.dev.testcentre.TcWays.Kind.BLASTING));
        if (com.simplebuilding.version.McVersion.CRUCIBLE) {
            expected.add(com.simplebuilding.dev.testcentre.TcWays.Kind.CRUCIBLE_BUILD);
        }
        for (var kind : expected) {
            if (!kinds.contains(kind)) problems.add("no sign shows a way of kind " + kind);
        }
        // Gegenprobe: was es nicht (mehr) gibt, faellt durch.
        List<Item> hammers = new TcContext(level.registryAccess()).rowItems("sledgehammers");
        ItemStack hammer = hammers.isEmpty() ? ItemStack.EMPTY : new ItemStack(hammers.getLast());
        var invented = new com.simplebuilding.dev.testcentre.TcWays.Way(com.simplebuilding.dev.testcentre.TcWays.Kind.UPGRADE,
                new ItemStack(Items.IRON_BLOCK), List.of(new ItemStack(Items.HEAVY_WEIGHTED_PRESSURE_PLATE, 4)), hammer,
                new ItemStack(ModItems.IRON_POLE == null ? Items.IRON_BARS : ModItems.IRON_POLE, 2));
        helper.assertTrue(com.simplebuilding.dev.testcentre.TcWays.problem(invented, level) != null, "the invented iron block way was accepted");
        var wrongRecipe = com.simplebuilding.dev.testcentre.TcWays.blasting(ModItems.CRACKED_DIAMOND, Items.EMERALD);
        helper.assertTrue(com.simplebuilding.dev.testcentre.TcWays.problem(wrongRecipe, level) != null, "a blasting recipe that does not exist was accepted");
        var build = com.simplebuilding.dev.testcentre.TcWays.crucibleBuild(hammers);
        if (com.simplebuilding.version.McVersion.CRUCIBLE) {
            helper.assertTrue(build != null && build.subject().is(Items.CAULDRON), "the crucible is built on a cauldron: " + build);
            var onIronBlock = new com.simplebuilding.dev.testcentre.TcWays.Way(build.kind(), new ItemStack(Items.IRON_BLOCK), build.inputs(),
                    build.tool(), build.result());
            helper.assertTrue(com.simplebuilding.dev.testcentre.TcWays.problem(onIronBlock, level) != null,
                    "the old crucible build on an iron block was accepted");
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " sign way problem(s) of " + ways + ": "
                + String.join("; ", problems.subList(0, Math.min(10, problems.size()))));
        helper.succeed();
    }

    /**
     * Reit-Station (Nachtrag 13, "Sonstiges"): nur mit Simple Riding; ihr Knopf "Pferd holen" ruft wirklich ein Pferd mit
     * Tailwind-Sattel und Leaping-Harnisch (Hoechststufe). Der Befehl laeuft hier an einer Stelle der Teststruktur.
     */
    private static void ridingSaddlesAHorse(GameTestHelper helper, TestCentreLayout.Plan plan, TcContext ctx, List<String> problems) {
        var tailwind = ctx.enchantment(FeatureStations.TAILWIND);
        var leaping = ctx.enchantment(FeatureStations.LEAPING);
        List<TcOp.Command> horse = new ArrayList<>();
        for (TestCentreLayout.Section section : plan.sections()) {
            for (TcOp op : section.ops()) {
                if (op instanceof TcOp.Command command && command.command().startsWith("summon minecraft:horse")) {
                    horse.add(command);
                }
            }
        }
        if (tailwind.isEmpty() || leaping.isEmpty()) {
            if (!horse.isEmpty()) {
                problems.add("the riding station stands without Simple Riding");
            }
            return;
        }
        if (horse.size() != 1) {
            problems.add("the riding station should have one horse button, has " + horse.size());
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        var source = level.getServer().createCommandSourceStack().withLevel(level)
                .withPosition(net.minecraft.world.phys.Vec3.atCenterOf(at)).withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(source, horse.getFirst().command());
        List<net.minecraft.world.entity.LivingEntity> horses = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                new AABB(at).inflate(12), entity -> entity.getType() == net.minecraft.world.entity.EntityType.HORSE);
        try {
            if (horses.size() != 1) {
                problems.add("the horse button summoned " + horses.size() + " horses: " + horse.getFirst().command());
                return;
            }
            var mount = horses.getFirst();
            ItemStack saddle = mount.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.SADDLE);
            ItemStack body = mount.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.BODY);
            int wind = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(tailwind.get(), saddle);
            int leap = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(leaping.get(), body);
            if (!saddle.is(Items.SADDLE) || wind != tailwind.get().value().getMaxLevel()) {
                problems.add("the horse's saddle is " + saddle + " with Tailwind " + wind);
            }
            if (body.isEmpty() || leap != leaping.get().value().getMaxLevel()) {
                problems.add("the horse's armour is " + body + " with Leaping " + leap);
            }
        } finally {
            horses.forEach(net.minecraft.world.entity.Entity::discard);
        }
    }

    /**
     * Nachtrag 13, UI-Bloecke: jeder Familien-Block, dessen Block-Entity ein Behaelter mit Menue ist, steht in {@code ui}
     * (sofern er ruhig setzbar ist), die Endertruhe und die Shulkerkiste auch. Teilbau {@code only ui} fern der anderen Tests
     * bauen, den Befuell-Knopf ausfuehren: jeder Behaelter, der ein Beispiel-Item annimmt, ist gefuellt, die Endertruhe des
     * Testspielers ist voll, die Tooltip-Truhe haelt eine gefuellte Shulkerkiste. Befehl und Knopf sind da.
     */
    public static void uiBlocksAndTooltipsFillByButton(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        BlockPos origin = new BlockPos(23000, helper.absolutePos(BlockPos.ZERO).getY() + 1, 23000);
        TestCentreLayout.Plan plan = TestCentreLayout.plan(level, origin, TestCentreLayout.Options.parse("only ui"));
        TestCentreLayout.Section ui = plan.section(com.simplebuilding.dev.testcentre.UiStation.SECTION);
        Set<Block> placed = new HashSet<>();
        boolean button = false;
        for (TcOp op : ui.ops()) {
            if (op instanceof TcOp.Place place) {
                placed.add(place.state().getBlock());
            }
            if (op instanceof TcOp.Command command && command.command().equals(com.simplebuilding.dev.testcentre.UiStation.FILL_COMMAND)) {
                button = true;
            }
        }
        if (!button) {
            problems.add("the ui station has no fill button");
        }
        if (level.getServer().getCommands().getDispatcher().getRoot().getChild("sbtestcentre").getChild("fill") == null) {
            problems.add("/sbtestcentre fill is not registered");
        }
        int expected = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (!TcContext.isFamily(id) || block.asItem() == Items.AIR
                    || !(block instanceof net.minecraft.world.level.block.EntityBlock entityBlock)) {
                continue;
            }
            Object entity;
            try {
                entity = entityBlock.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
            } catch (RuntimeException e) {
                continue;
            }
            if (!(entity instanceof net.minecraft.world.Container) || !(entity instanceof net.minecraft.world.MenuProvider)) {
                continue;
            }
            expected++;
            if (!placed.contains(block) && !com.simplebuilding.dev.testcentre.Exhibits.blockStates(block).isEmpty()) {
                problems.add(id + " (container with a menu) is not in the ui station");
            }
        }
        for (Block vanilla : List.of(Blocks.ENDER_CHEST, Blocks.SHULKER_BOX, Blocks.CHEST, Blocks.FURNACE, Blocks.CRAFTING_TABLE)) {
            if (!placed.contains(vanilla)) {
                problems.add(BuiltInRegistries.BLOCK.getKey(vanilla) + " is not in the ui station");
            }
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        TestCentreBuilder.build(level, plan);
        try {
            player.getEnderChestInventory().clearContent();
            int filled = com.simplebuilding.dev.testcentre.UiStation.fill(level, plan, com.simplebuilding.dev.testcentre.UiStation.SECTION,
                    List.of(player));
            BoundingBox box = ui.box(origin);
            int containers = 0;
            for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                if (!(level.getBlockEntity(pos) instanceof net.minecraft.world.Container container)) {
                    continue;
                }
                containers++;
                boolean accepts = false;
                for (int slot = 0; slot < container.getContainerSize() && !accepts; slot++) {
                    for (ItemStack sample : com.simplebuilding.dev.testcentre.UiStation.samples()) {
                        accepts |= container.canPlaceItem(slot, sample);
                    }
                }
                if (accepts && container.isEmpty()) {
                    problems.add("the container at " + pos.toShortString() + " (" + BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock())
                            + ") stayed empty after the fill button");
                }
            }
            if (player.getEnderChestInventory().isEmpty()) {
                problems.add("the fill button left the player's ender chest empty");
            }
            BlockPos chestPos = null;
            for (TcOp op : ui.ops()) {
                if (op instanceof TcOp.Fill fill && fill.contents().stream().anyMatch(stack -> stack.is(Items.SHULKER_BOX))) {
                    chestPos = fill.pos();
                }
            }
            boolean shulker = chestPos != null && level.getBlockEntity(chestPos) instanceof net.minecraft.world.Container chest
                    && java.util.stream.IntStream.range(0, chest.getContainerSize()).mapToObj(chest::getItem).anyMatch(stack -> stack.is(Items.SHULKER_BOX)
                    && stack.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER,
                    net.minecraft.world.item.component.ItemContainerContents.EMPTY).nonEmptyItemCopyStream().findAny().isPresent());
            if (!shulker) {
                problems.add("the tooltip chest holds no filled shulker box");
            }
            com.simplebuilding.Simplebuilding.LOGGER.info("test centre ui: {} blocks, {} family containers with a menu, {} containers built, {} filled",
                    placed.size(), expected, containers, filled);
        } finally {
            TestCentreBuilder.clear(level, plan.bounds());
            level.getServer().getPlayerList().remove(player);
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " ui problem(s): " + String.join("; ", problems.subList(0, Math.min(10, problems.size()))));
        helper.succeed();
    }

    /**
     * Nachtrag 13, Crafting-Rezepte aus den Live-Daten: jedes Crafting-Rezept der Familie mit Platzierung (RecipeManager des
     * Servers) steht genau einmal als Crafter-Muster in {@code recipes}, das Muster passt ({@code matches}) und ergibt etwas;
     * kein Crafter ohne Rezept. Rezepte mit einer Zutat ohne Item (leerer Tag) werden nur gezaehlt.
     */
    public static void craftingRecipesAreaShowsEveryLiveRecipe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TestCentreLayout.Plan plan = TestCentreLayout.plan(level, BlockPos.ZERO);
        List<List<ItemStack>> grids = com.simplebuilding.dev.testcentre.RecipeStation.grids(plan.section(com.simplebuilding.dev.testcentre.RecipeStation.SECTION).ops());
        Map<String, Integer> shown = new HashMap<>();
        grids.forEach(grid -> shown.merge(com.simplebuilding.dev.testcentre.RecipeStation.key(grid), 1, Integer::sum));
        List<String> problems = new ArrayList<>();
        int expected = 0;
        int skipped = 0;
        for (var holder : level.getServer().getRecipeManager().getRecipes()) {
            Identifier id = holder.id().identifier();
            if (!TcContext.isFamily(id) || !(holder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe recipe)
                    || !com.simplebuilding.dev.testcentre.RecipeStation.placeable(recipe)) {
                continue;
            }
            List<ItemStack> grid = com.simplebuilding.dev.testcentre.RecipeStation.grid(recipe);
            if (grid == null) {
                skipped++;
                continue;
            }
            expected++;
            if (com.simplebuilding.dev.testcentre.RecipeStation.result(recipe, grid, level).isEmpty()) {
                problems.add(id + ": its pattern does not craft");
            }
            if (shown.merge(com.simplebuilding.dev.testcentre.RecipeStation.key(grid), -1, Integer::sum) < 0) {
                problems.add(id + " has no crafter");
            }
        }
        shown.forEach((key, left) -> {
            if (left > 0) {
                problems.add(left + " crafter(s) without a live recipe: " + key);
            }
        });
        if (expected == 0) {
            problems.add("no family crafting recipe found");
        }
        if (grids.size() != expected) {
            problems.add(grids.size() + " crafters for " + expected + " recipes");
        }
        com.simplebuilding.Simplebuilding.LOGGER.info("test centre recipes: {} crafting recipes shown as crafters, {} skipped (ingredient without item)",
                expected, skipped);
        helper.assertTrue(problems.isEmpty(), problems.size() + " recipe problem(s): " + String.join("; ", problems.subList(0, Math.min(10, problems.size()))));
        helper.succeed();
    }

    /**
     * Nachtrag 13, In-World-Aufwertungen nach Stufen: jede Aufwertung der Engine mit Familien-Ziel steht in {@code upgrades} -
     * Ausgangsblock gesetzt (Familie immer, Vanilla-Starts je Ziel einmal) mit Weg-Schild, das Ziel in derselben Zeile weiter
     * rechts; {@code inworld} zeigt keine Aufwertung mehr doppelt.
     */
    public static void upgradeChainsRunByTier(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TestCentreLayout.Plan plan = TestCentreLayout.plan(level, BlockPos.ZERO);
        TestCentreLayout.Section upgrades = plan.section("upgrades");
        Map<Block, List<BlockPos>> at = new HashMap<>();
        Set<Item> waySubjects = new HashSet<>();
        for (TcOp op : upgrades.ops()) {
            if (op instanceof TcOp.Place place) {
                at.computeIfAbsent(place.state().getBlock(), b -> new ArrayList<>()).add(place.pos());
            }
            if (op instanceof TcOp.Sign sign && sign.way() != null && sign.way().kind() == com.simplebuilding.dev.testcentre.TcWays.Kind.UPGRADE) {
                waySubjects.add(sign.way().subject().getItem());
            }
        }
        List<String> problems = new ArrayList<>();
        Set<Block> vanillaTargets = new HashSet<>();
        int steps = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            var upgrade = com.simplebuilding.util.SledgehammerUpgrades.upgradeOf(block);
            if (upgrade == null || upgrade.from() != block || !TcContext.isFamily(BuiltInRegistries.BLOCK.getKey(upgrade.to()))) {
                continue;
            }
            boolean family = TcContext.isFamily(BuiltInRegistries.BLOCK.getKey(block));
            if (!family) {
                if (at.containsKey(block)) {
                    vanillaTargets.add(upgrade.to());
                }
                continue;
            }
            steps++;
            if (!at.containsKey(block)) {
                problems.add(BuiltInRegistries.BLOCK.getKey(block) + " is not in the upgrade chains");
                continue;
            }
            if (!waySubjects.contains(block.asItem())) {
                problems.add(BuiltInRegistries.BLOCK.getKey(block) + " has no upgrade way sign");
            }
        }
        // Jede Stufe: das Ziel steht in derselben Zeile weiter rechts (Kettenfolge).
        for (Map.Entry<Block, List<BlockPos>> entry : at.entrySet()) {
            var upgrade = com.simplebuilding.util.SledgehammerUpgrades.upgradeOf(entry.getKey());
            if (upgrade == null || upgrade.from() != entry.getKey()) {
                continue;
            }
            for (BlockPos from : entry.getValue()) {
                boolean next = at.getOrDefault(upgrade.to(), List.of()).stream()
                        .anyMatch(to -> to.getY() == from.getY() && to.getZ() == from.getZ() && to.getX() > from.getX());
                if (!next) {
                    problems.add(BuiltInRegistries.BLOCK.getKey(entry.getKey()) + " at " + from.toShortString() + " is not followed by "
                            + BuiltInRegistries.BLOCK.getKey(upgrade.to()));
                }
            }
        }
        for (Block block : BuiltInRegistries.BLOCK) {
            var upgrade = com.simplebuilding.util.SledgehammerUpgrades.upgradeOf(block);
            if (upgrade != null && upgrade.from() == block && !TcContext.isFamily(BuiltInRegistries.BLOCK.getKey(block))
                    && TcContext.isFamily(BuiltInRegistries.BLOCK.getKey(upgrade.to())) && !vanillaTargets.contains(upgrade.to())) {
                problems.add("no vanilla start upgrades to " + BuiltInRegistries.BLOCK.getKey(upgrade.to()));
                vanillaTargets.add(upgrade.to());
            }
        }
        for (TcOp op : plan.section("inworld").ops()) {
            if (op instanceof TcOp.Sign sign && sign.way() != null && sign.way().kind() == com.simplebuilding.dev.testcentre.TcWays.Kind.UPGRADE) {
                problems.add("inworld still shows the upgrade of " + sign.way().subject());
            }
        }
        if (steps == 0) {
            problems.add("no family upgrade found");
        }
        com.simplebuilding.Simplebuilding.LOGGER.info("test centre upgrades: {} family steps in {} chains", steps,
                com.simplebuilding.dev.testcentre.TestCentreSections.upgradeChains().size());
        helper.assertTrue(problems.isEmpty(), problems.size() + " upgrade problem(s): " + String.join("; ", problems.subList(0, Math.min(10, problems.size()))));
        helper.succeed();
    }

    private static Runnable cleanupOnFailure(Runnable cleanup, Runnable action) {
        return () -> {
            try {
                action.run();
            } catch (RuntimeException failure) {
                cleanup.run();
                throw failure;
            }
        };
    }
}
