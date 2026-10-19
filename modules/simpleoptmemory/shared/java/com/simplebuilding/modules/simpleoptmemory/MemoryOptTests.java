package com.simplebuilding.modules.simpleoptmemory;

import com.simplebuilding.framework.api.OptCompat;
import com.simplebuilding.framework.api.OptSettings;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;

/** Simple Opt Memory checks (Fabric wrapper ModuleGameTest): compat, config, the O4 sharing rule and its live effect. */
public final class MemoryOptTests {
    private MemoryOptTests() {}

    private static OptSettings settings(String json) {
        return OptSettings.of(com.google.gson.JsonParser.parseString(json).getAsJsonObject(), MemoryOpt.defaults());
    }

    public static void compatModes(GameTestHelper h) {
        List<OptCompat.Decision> live = OptCompat.decisions(MemoryOpt.MOD_ID);
        try {
            MemoryOpt.apply(settings("{}"), true, Set.<String>of()::contains, Map.of());
            h.assertTrue(MemoryOpt.live(MemoryOpt.STATE_TABLES), "auto without FerriteCore: ours on");
            MemoryOpt.apply(settings("{}"), true, Set.of("ferritecore")::contains, Map.of());
            h.assertTrue(!MemoryOpt.live(MemoryOpt.STATE_TABLES), "FerriteCore present: theirs wins");
            MemoryOpt.apply(settings("{\"areas\":{\"state_tables\":{\"mode\":\"ours\"}}}"), true, Set.of("ferritecore")::contains, Map.of());
            h.assertTrue(!MemoryOpt.live(MemoryOpt.STATE_TABLES), "ours next to FerriteCore (same constructors): falls back");
            MemoryOpt.apply(settings("{}"), false, Set.<String>of()::contains, Map.of());
            h.assertTrue(!MemoryOpt.live(MemoryOpt.STATE_TABLES), "super-mod switch off");
        } finally {
            MemoryOpt.RUNTIME.publish(live);
        }
        h.assertTrue(MemoryOpt.priorities().get("state_tables") == OptCompat.Winner.THEIRS, "shipped table: theirs");
        h.assertTrue(MemoryOpt.WATCHED.contains("modernfix"), "O5 rival watched");
        try {
            var dir = Files.createTempDirectory("simpleoptmemory-test");
            OptSettings.load(dir, MemoryOpt.FILE, MemoryOpt.defaults(), s -> {});
            String text = Files.readString(dir.resolve(MemoryOpt.FILE));
            h.assertTrue(text.contains("state_tables") && text.contains("conflictScan"), "config written");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        h.succeed();
    }

    /** Identity rule: same class and same elements share, anything else stays its own; boolean tables by content. */
    public static void sharingRule(GameTestHelper h) {
        Object a = new Object(), b = new Object();
        Object[] one = {a, b}, same = {a, b}, other = {b, a};
        h.assertTrue(StateTables.share(one) == one && StateTables.share(same) == one, "identical elements share");
        h.assertTrue(StateTables.share(other) == other, "order counts");
        String s1 = new String("x"), s2 = new String("x");
        String[] eq1 = {s1}, eq2 = {s2};
        h.assertTrue(StateTables.share(eq1) == eq1 && StateTables.share(eq2) == eq2, "equal but not identical values never merge");
        CharSequence[] wider = {a.toString()};
        String[] narrow = {(String) wider[0]};
        h.assertTrue(StateTables.share(wider) == wider && StateTables.share(narrow) == narrow, "array class counts");
        Object[] empty = {};
        h.assertTrue(StateTables.share(empty) == empty, "empty arrays untouched");
        boolean[] f1 = {true, false, true}, f2 = {true, false, true}, f3 = {false, false, true};
        h.assertTrue(StateTables.share(f1) == f1 && StateTables.share(f2) == f1 && StateTables.share(f3) == f3, "face tables by content");
        h.succeed();
    }

    private static Object field(Class<?> owner, Object target, String name) throws ReflectiveOperationException {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    /** Live: built Vanilla states share their tables and still behave the same. */
    public static void sharedStates(GameTestHelper h) {
        BlockState oak = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        BlockState spruce = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        h.assertTrue(oak.getValue(StairBlock.FACING) == Direction.EAST && spruce.setValue(StairBlock.FACING, Direction.WEST).getValue(StairBlock.FACING) == Direction.WEST,
                "values read and set as before");
        h.assertTrue(oak.toString().contains("facing=east") && oak.getValues().toList().equals(spruce.getValues().toList()), "property listing unchanged");
        if (!MemoryOpt.live(MemoryOpt.STATE_TABLES)) {
            MemoryOpt.LOG.info("state_tables not live in this run ({}), live part skipped", OptCompat.decisions(MemoryOpt.MOD_ID));
            h.succeed();
            return;
        }
        try {
            h.assertTrue(field(StateHolder.class, oak, "propertyValues") == field(StateHolder.class, spruce, "propertyValues"),
                    "oak and spruce stairs share one value array per state");
            h.assertTrue(field(StateHolder.class, oak, "propertyKeys") == field(StateHolder.class, spruce, "propertyKeys"), "and one key array");
            Object stoneCache = field(BlockBehaviour.BlockStateBase.class, Blocks.STONE.defaultBlockState(), "cache");
            Object dirtCache = field(BlockBehaviour.BlockStateBase.class, Blocks.DIRT.defaultBlockState(), "cache");
            h.assertTrue(field(stoneCache.getClass(), stoneCache, "faceSturdy") == field(dirtCache.getClass(), dirtCache, "faceSturdy"),
                    "stone and dirt share the face-support table");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        h.assertTrue(StateTables.shared() > 1000 && StateTables.savedBytes() > 0, StateTables.summary());
        MemoryOpt.LOG.info(StateTables.summary());
        h.succeed();
    }
}
