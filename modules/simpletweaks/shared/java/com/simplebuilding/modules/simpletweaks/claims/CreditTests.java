package com.simplebuilding.modules.simpletweaks.claims;

import com.simplebuilding.modules.simpletweaks.claims.ClaimConfig.Cost;
import com.simplebuilding.modules.simpletweaks.claims.ClaimConfig.Tier;
import com.simplebuilding.modules.simpletweaks.deed.ClaimDeedItem;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimTests.*;

/** SP22-8 branch 1: chunk credits (paid land minus held land), the one-time budget migration, all dimensions, buying ahead. */
public final class CreditTests {
    private CreditTests() {}

    private static final List<Tier> TIERS = List.of(new Tier(3, List.of(new Cost("minecraft:iron_ingot", 2)), 1),
            new Tier(5, List.of(new Cost("minecraft:diamond", 1)), 2));
    /** Start 2, one step to 3 below the server limit (4), the step to 5 above it. */
    private static ClaimConfig config() {
        return new ClaimConfig(true, 4, 8, 50, 20, false, 0, List.of(ClaimConfig.ALL_DIMENSIONS)).withExpansion(2, TIERS);
    }
    private static int count(ServerPlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(item)) n += p.getInventory().getItem(i).getCount();
        return n;
    }
    private static ClaimStore.Key key(String dim, int x, int z) { return new ClaimStore.Key(dim, ChunkPos.pack(x, z)); }

    /** Giving up a chunk refunds nothing, the paid budget stays and the chunk comes back as a credit. */
    public static void unclaim(GameTestHelper h) {
        var server = h.getLevel().getServer(); var previous = Claims.get(server); String dim = h.getLevel().dimension().identifier().toString();
        try {
            var root = temp(); var claims = new Claims(config(), root, Map.of()); Claims.install(server, claims);
            var p = player(h, h.absolutePos(new BlockPos(1, 2, 1))); UUID me = p.getUUID();
            long t0 = server.overworld().getGameTime();
            yes(h, claims.credits(me) == 2 && claims.budget(me) == 2, "Start: two free credits");
            yes(h, claims.create(key(dim, 200, 0), me, t0 - 9000) && claims.create(key(dim, 201, 0), me, t0 - 8000) && claims.credits(me) == 0, "Two chunks use both credits");
            yes(h, !claims.create(key(dim, 202, 0), me, t0 - 7000), "No credit: no third chunk");
            p.getInventory().add(new ItemStack(ClaimDeedItem.item())); p.getInventory().add(new ItemStack(Items.IRON_INGOT, 2)); p.giveExperienceLevels(1);
            yes(h, claims.expand(p) == Claims.Expand.OK && claims.budget(me) == 3 && claims.credits(me) == 1, "Bought step: one credit");
            yes(h, claims.create(key(dim, 202, 0), me, t0 + 1000) && claims.credits(me) == 0, "The credit claims the third chunk");
            var stranger = UUID.randomUUID();
            yes(h, !claims.unclaim(stranger, key(dim, 200, 0), t0 + 2000) && !claims.unclaim(me, key(dim, 300, 0), t0 + 2000), "Only the owner gives up, only held chunks");
            yes(h, claims.unclaim(me, key(dim, 200, 0), t0 + 2000), "The owner gives up a chunk");
            yes(h, claims.budget(me) == 3 && claims.credits(me) == 1 && claims.chunksOf(me) == 2, "Budget stays, one credit back");
            yes(h, count(p, Items.IRON_INGOT) == 0 && p.experienceLevel == 0, "No material and no levels refunded");
            var restarted = new Claims(config(), root, Map.of());
            yes(h, restarted.budget(me) == 3 && restarted.credits(me) == 1, "The credit survives a restart");
            yes(h, restarted.create(key(dim, 210, 5), me, t0 + 3000) && restarted.credits(me) == 0, "The credit claims a new chunk elsewhere");
        } finally { Claims.install(server, previous); }
        h.succeed();
    }

    /** Land held before chunk credits becomes paid once: giving it up gives credits; an operator's later budget sticks. */
    public static void migration(GameTestHelper h) {
        try {
            String dim = "minecraft:overworld";
            UUID old = UUID.randomUUID(), small = UUID.randomUUID(), lowered = UUID.randomUUID();
            var root = temp(); var ledger = root.resolve("simpletweaks-claims.json");
            var claimsMap = new java.util.HashMap<ClaimStore.Key, ClaimStore.Claim>();
            for (int x = 0; x < 4; x++) claimsMap.put(key(dim, 500 + x, 0), new ClaimStore.Claim(old));
            claimsMap.put(key(dim, 600, 0), new ClaimStore.Claim(small));
            for (int x = 0; x < 3; x++) claimsMap.put(key("minecraft:the_nether", 700 + x, 0), new ClaimStore.Claim(lowered));
            // A ledger of the previous build: version 2, no marker, budgets only where bought.
            new ClaimStore(ledger, Map.of()).replace(claimsMap, Map.of(), Map.of(lowered, 2));
            String before = Files.readString(ledger);
            yes(h, !before.contains("creditsMigrated"), "Old ledger has no marker");
            var claims = new Claims(config(), root, Map.of());
            yes(h, claims.budget(old) == 4 && claims.credits(old) == 0, "Old land above the start (4 > 2) is paid now");
            yes(h, claims.budget(small) == 2 && claims.credits(small) == 1, "Land within the start size: start budget");
            yes(h, claims.budget(lowered) == 3 && claims.credits(lowered) == 0, "Stored budget below the land held is raised to it");
            yes(h, Files.readString(ledger).contains("\"creditsMigrated\":true"), "Marker written");
            var backup = root.resolve("simpletweaks-claims.v2.json");
            yes(h, Files.exists(backup) && Files.readString(backup).equals(before), "Copy of the version 2 ledger kept");
            yes(h, claims.unclaim(old, key(dim, 500, 0), 100) && claims.credits(old) == 1 && claims.budget(old) == 4, "Giving up old land gives a credit (the gap is closed)");
            yes(h, claims.create(key(dim, 900, 9), old, 200) && claims.chunksOf(old) == 4, "The credit claims again");
            yes(h, claims.setBudget(lowered, 2), "Operator lowers a budget below the land held");
            var restarted = new Claims(config(), root, Map.of());
            yes(h, restarted.budget(lowered) == 2 && restarted.credits(lowered) == 0 && restarted.chunksOf(lowered) == 3, "Migration runs once: the operator's budget sticks, no land is taken");
            // Nothing to migrate: no write, no copy.
            var root2 = temp(); var ledger2 = root2.resolve("simpletweaks-claims.json");
            new ClaimStore(ledger2, Map.of()).replace(Map.of(key(dim, 1, 1), new ClaimStore.Claim(small)), Map.of(), Map.of());
            String plain = Files.readString(ledger2);
            var quiet = new Claims(config(), root2, Map.of());
            yes(h, quiet.credits(small) == 1 && Files.readString(ledger2).equals(plain) && !Files.exists(root2.resolve("simpletweaks-claims.v2.json")), "Nothing to raise: ledger untouched");
            yes(h, quiet.create(key(dim, 2, 2), small, 100) && Files.readString(ledger2).contains("\"creditsMigrated\":true"), "The next save carries the marker");
        } catch (java.io.IOException e) { throw new AssertionError(e); }
        h.succeed();
    }

    /** "*" allows every dimension; the old default list migrates to "*" once, an administrator's list stays. */
    public static void dimensions(GameTestHelper h) {
        try {
            var p = temp().resolve("claims.json");
            Files.writeString(p, "{\"configVersion\":2,\"dimensions\":[\"minecraft:overworld\",\"minecraft:the_nether\",\"minecraft:the_end\"]}");
            var c = ClaimConfig.load(p);
            yes(h, c.dimensions().equals(List.of("*")) && Files.readString(p).contains("\"*\"") && Files.readString(p).contains("\"configVersion\": " + ClaimConfig.CONFIG_VERSION), "Old default list becomes \"*\", version written");
            yes(h, ClaimConfig.load(p).dimensions().equals(List.of("*")), "Migrated file loads with \"*\"");
            Files.writeString(p, "{\"configVersion\":2,\"dimensions\":[\"minecraft:the_end\",\"minecraft:overworld\",\"minecraft:the_nether\"]}");
            yes(h, ClaimConfig.load(p).dimensions().equals(List.of("*")), "Same list in another order is still the old default");
            Files.writeString(p, "{\"configVersion\":2,\"dimensions\":[\"minecraft:overworld\"]}");
            yes(h, ClaimConfig.load(p).dimensions().equals(List.of("minecraft:overworld")), "An administrator's own list stays");
            Files.writeString(p, "{\"configVersion\":" + ClaimConfig.CONFIG_VERSION + ",\"dimensions\":[\"minecraft:overworld\",\"minecraft:the_nether\",\"minecraft:the_end\"]}");
            yes(h, ClaimConfig.load(p).dimensions().equals(ClaimConfig.LEGACY_DIMENSIONS), "After the migration the three-dimension list is a choice and stays");
            Files.writeString(p, "{\"configVersion\":2,\"enabled\":false}");
            yes(h, !ClaimConfig.load(p).enabled(), "A version 2 opt-out stays off");
            Files.writeString(p, "{\"enabled\":false,\"dimensions\":[\"minecraft:overworld\",\"minecraft:the_nether\",\"minecraft:the_end\"]}");
            var v1 = ClaimConfig.load(p);
            yes(h, v1.enabled() && v1.dimensions().equals(List.of("*")), "Version 1: both migrations at once");
        } catch (java.io.IOException e) { throw new AssertionError(e); }
        yes(h, ClaimConfig.DEFAULT.dimensions().equals(List.of("*")) && ClaimConfig.DEFAULT.allowsDimension("simpledimensions:farm"), "Default: every dimension");
        boolean keyRefused = false;
        try { new ClaimStore.Key("*", 0); } catch (IllegalArgumentException e) { keyRefused = true; }
        boolean badRefused = false;
        try { new ClaimConfig(true, 1, 1, 1, 20, false, 0, List.of("*", "../x")); } catch (IllegalArgumentException e) { badRefused = true; }
        yes(h, keyRefused && badRefused, "\"*\" is no chunk dimension; invalid ids stay refused");
        var all = new Claims(config(), temp(), Map.of());
        yes(h, all.create(key("simpledimensions:farm", 3, 3), UUID.randomUUID(), 100), "\"*\": a custom dimension is claimable");
        var listed = new Claims(new ClaimConfig(true, 4, 8, 50, 20, false, 0, List.of("minecraft:overworld")), temp(), Map.of());
        yes(h, !listed.create(key("simpledimensions:farm", 3, 3), UUID.randomUUID(), 100) && listed.create(key("minecraft:overworld", 3, 3), UUID.randomUUID(), 100), "A concrete list still restricts");
        h.succeed();
    }

    /** Buying ahead gives credits; without a credit "buy and claim" saves budget and chunk together; the server limit caps. */
    public static void buy(GameTestHelper h) {
        var server = h.getLevel().getServer(); var previous = Claims.get(server); String dim = h.getLevel().dimension().identifier().toString();
        try {
            var root = temp(); var claims = new Claims(config(), root, Map.of()); Claims.install(server, claims);
            long t0 = server.overworld().getGameTime();
            var ahead = player(h, h.absolutePos(new BlockPos(1, 2, 1)).offset(3072, 0, 3072));
            ahead.getInventory().add(new ItemStack(ClaimDeedItem.item())); ahead.getInventory().add(new ItemStack(Items.IRON_INGOT, 2)); ahead.giveExperienceLevels(1);
            yes(h, claims.expand(ahead) == Claims.Expand.OK && claims.credits(ahead.getUUID()) == 3 && claims.chunksOf(ahead.getUUID()) == 0, "Bought ahead without land: three credits");
            yes(h, count(ahead, Items.IRON_INGOT) == 0 && ahead.experienceLevel == 0, "Ahead purchase took the step's cost");

            var pos = h.absolutePos(new BlockPos(1, 2, 1)).offset(4096, 0, 4096);
            var q = player(h, pos); UUID me = q.getUUID();
            q.getInventory().add(new ItemStack(ClaimDeedItem.item())); q.getInventory().add(new ItemStack(Items.IRON_INGOT, 4)); q.giveExperienceLevels(2);
            yes(h, claims.create(key(dim, 40, 40), me, t0 - 9000) && claims.create(key(dim, 41, 40), me, t0 - 8000) && claims.credits(me) == 0, "Seed: both start credits used");
            var here = new ClaimStore.Key(dim, q.chunkPosition().pack());
            yes(h, claims.buyAndClaim(q) == Claims.Expand.OK, "No credit: buy and claim");
            yes(h, me.equals(claims.view().get(here).owner()) && claims.budget(me) == 3 && claims.chunksOf(me) == 3 && claims.credits(me) == 0, "Chunk claimed and budget raised");
            yes(h, count(q, Items.IRON_INGOT) == 2 && q.experienceLevel == 1, "Cost taken once");
            var reloaded = new Claims(config(), root, Map.of());
            yes(h, reloaded.budget(me) == 3 && reloaded.view().containsKey(here), "Budget and chunk saved together");
            yes(h, claims.buyAndClaim(q) == Claims.Expand.PLACE && count(q, Items.IRON_INGOT) == 2 && q.experienceLevel == 1, "Claimed chunk: refused, nothing taken");
            q.setPos(pos.getX() + 32.5, pos.getY(), pos.getZ() + .5);
            q.getInventory().add(new ItemStack(Items.DIAMOND, 1));
            yes(h, claims.buyAndClaim(q) == Claims.Expand.SERVER_LIMIT && count(q, Items.DIAMOND) == 1 && claims.chunksOf(me) == 3, "Next step above the server limit: refused, nothing taken, nothing claimed");
            // Restart clears cooldowns; give up a chunk, then the credit claims for free.
            claims = new Claims(config(), root, Map.of()); Claims.install(server, claims);
            yes(h, claims.unclaim(me, key(dim, 40, 40), t0 - 1000) && claims.credits(me) == 1, "Given up: one credit");
            yes(h, claims.buyAndClaim(q) == Claims.Expand.OK && claims.view().containsKey(new ClaimStore.Key(dim, q.chunkPosition().pack()))
                    && count(q, Items.IRON_INGOT) == 2 && count(q, Items.DIAMOND) == 1 && q.experienceLevel == 1 && claims.credits(me) == 0, "With a credit: claimed for free");
            yes(h, new ClaimConfig(true, 4, 8, 50, 20, false, 0, List.of("*")).withExpansion(2, TIERS).budgetOf(9) == 4, "maxClaimsPerPlayer caps the paid land");
        } finally { Claims.install(server, previous); }
        h.succeed();
    }
}
