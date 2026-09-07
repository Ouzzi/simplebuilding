package com.simplebuilding.gametest;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.trading.VillagerTrade;

import java.util.Set;
import java.util.TreeSet;

/**
 * The mod ships 20 data-driven villager trades. This asserts they actually reach the
 * villager_trade registry of a running server -- which is a different question from
 * whether the vanilla trade tags pick them up (see TradeAndMigrationGameTest).
 *
 * <p>The expected ids are not typed out here: they are read from the data pack the server is
 * running, through the same {@link FileToIdConverter} the registry loader uses, and then compared
 * with the registry in both directions. A bare count cannot tell "twenty files loaded" from
 * "nineteen loaded and one renamed file loaded twice as something else"; a file that the codec
 * rejects, a path typo, or a resource condition that quietly drops an entry all show up as a named
 * difference instead.
 *
 * <p>What this test deliberately does <em>not</em> look at is the content of a trade - price, uses,
 * experience, enchantment pool. Those are built into real offers and compared field by field in
 * {@code TradeAndMigrationTests#tradeDefinitionsProduceTheExpectedOffers}.
 *
 * <p><strong>Counterpart on MC 1.21.11:</strong> {@code allModTradesResolveAgainstTheServerRegistries}.
 * That line has no villager_trade registry - its offers are built in code - so the same concern,
 * "every shipped trade is reachable and names things that exist", is asked of the definition table
 * instead of the registry. {@code LINE_DIFFERENCES} in {@code tools/testrunner/run.py} records the
 * pairing, so deleting either side turns the parity gate red rather than quietly halving the check.
 *
 * <p><strong>What breaks this test:</strong> a renamed, moved or deleted trade file; a json the
 * codec rejects (the file stays on disk, the registry entry disappears); a load condition that
 * switches a trade off by default; and any entry appearing under our namespace that has no file.
 */
public final class TradeRegistryTests {

    private static final String NAMESPACE = "simplebuilding";

    /** How many trade files {@code data/simplebuilding/villager_trade/} is supposed to ship. */
    private static final int EXPECTED_TRADES = 20;

    public static void allModTradesReachTheRegistry(GameTestHelper helper) {
        FileToIdConverter converter = FileToIdConverter.registry(Registries.VILLAGER_TRADE);
        Set<String> shipped = new TreeSet<>();
        for (Identifier file : converter.listMatchingResources(
                helper.getLevel().getServer().getResourceManager()).keySet()) {
            Identifier id = converter.fileToId(file);
            if (NAMESPACE.equals(id.getNamespace())) {
                shipped.add(id.toString());
            }
        }
        helper.assertValueEqual(shipped.size(), EXPECTED_TRADES,
                "number of villager_trade json files the running data pack carries under "
                        + NAMESPACE + " (found " + shipped + ")");

        Registry<VillagerTrade> registry = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.VILLAGER_TRADE);
        Set<String> loaded = new TreeSet<>();
        for (Identifier key : registry.keySet()) {
            if (NAMESPACE.equals(key.getNamespace())) {
                loaded.add(key.toString());
            }
        }

        Set<String> droppedByTheLoader = new TreeSet<>(shipped);
        droppedByTheLoader.removeAll(loaded);
        helper.assertTrue(droppedByTheLoader.isEmpty(),
                droppedByTheLoader.size() + " shipped trade file(s) never reached the villager_trade "
                        + "registry: " + droppedByTheLoader + " - the loader dropped them, which it does "
                        + "silently when a json fails to parse or a load condition rejects it");

        Set<String> withoutAFile = new TreeSet<>(loaded);
        withoutAFile.removeAll(shipped);
        helper.assertTrue(withoutAFile.isEmpty(),
                withoutAFile.size() + " villager_trade entr(y|ies) live under the " + NAMESPACE
                        + " namespace without a file of ours behind them: " + withoutAFile);

        helper.assertValueEqual(loaded.size(), EXPECTED_TRADES,
                "number of simplebuilding entries in the villager_trade registry");

        helper.succeed();
    }
}
