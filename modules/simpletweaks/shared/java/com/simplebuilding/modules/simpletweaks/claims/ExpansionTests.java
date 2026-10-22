package com.simplebuilding.modules.simpletweaks.claims;

import com.simplebuilding.modules.simpletweaks.claims.ClaimConfig.Cost;
import com.simplebuilding.modules.simpletweaks.claims.ClaimConfig.Tier;
import com.simplebuilding.modules.simpletweaks.deed.ClaimDeedItem;
import com.simplebuilding.modules.simpletweaks.deed.DeedData;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedNet;
import com.simplebuilding.modules.simpletweaks.table.ClaimMapLayer;
import com.simplebuilding.modules.simpletweaks.table.ClaimTableBlock;
import com.simplebuilding.modules.simpletweaks.table.TableService;
import com.simplebuilding.modules.simpletweaks.table.net.TableOpenPayload;
import com.simplebuilding.modules.simpletweaks.table.net.TableRequestPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimTests.*;

/** Phases 4 and 5 of the claim deed plan: land budget and expansion (server side) and the claims layer of the map. */
public final class ExpansionTests {
    private ExpansionTests() {}

    private static final List<Tier> TIERS = List.of(new Tier(3, List.of(new Cost("minecraft:iron_ingot", 2)), 1),
            new Tier(5, List.of(new Cost("minecraft:diamond", 1)), 2));

    private static ClaimConfig config() {
        return new ClaimConfig(true, 4, 8, 50, 20, false, 0, ClaimConfig.DEFAULT.dimensions()).withExpansion(2, TIERS);
    }
    private static int count(ServerPlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(item)) n += p.getInventory().getItem(i).getCount();
        return n;
    }
    private static ClaimStore.Key key(String dim, int x, int z) { return new ClaimStore.Key(dim, ChunkPos.pack(x, z)); }

    /** Pure rules of the budget and the config validation. */
    public static void budget(GameTestHelper h) {
        var c = config();
        yes(h, c.budgetOf(null) == 2 && c.budgetOf(3) == 3 && c.budgetOf(1) == 2 && c.budgetOf(9) == 4,
                "Budget: start size, bought size, never below the start, never above the server limit (held land does not count: credits)");
        yes(h, new ClaimConfig(true, 6, 8, 50, 20, false, 0, ClaimConfig.DEFAULT.dimensions()).budgetOf(1) == 6, "Without expansion the budget is the server limit");
        yes(h, c.nextTier(2).chunks() == 3 && c.nextTier(3) == null && c.nextTier(4) == null && c.nextTier(5) == null,
                "Next step above the budget, none above the server limit (4) or the end");
        boolean bad = false;
        try { c.withExpansion(2, List.of(new Tier(5, List.of(), 0), new Tier(5, List.of(), 0))); } catch (IllegalArgumentException e) { bad = true; }
        boolean bad2 = false;
        try { c.withExpansion(4, List.of(new Tier(4, List.of(), 0))); } catch (IllegalArgumentException e) { bad2 = true; }
        boolean bad3 = false;
        try { new Cost("iron", 1); } catch (IllegalArgumentException e) { bad3 = true; }
        yes(h, bad && bad2 && bad3, "Tiers must rise above the start size; a cost needs a namespaced id");
        var d = ClaimConfig.DEFAULT;
        yes(h, d.expansionEnabled() && d.startChunks() == 4 && d.expansionTiers().size() == 8 && d.expansionTiers().get(7).chunks() == 64 && d.maxClaimsPerPlayer() == 64,
                "Default curve: 4 chunks to start, eight steps up to 64");
        int[] levels = {10, 15, 20, 30, 35, 40, 50, 60};
        boolean costs = true;
        for (int i = 0; i < 8; i++) {
            var t = d.expansionTiers().get(i);
            costs &= t.levels() == levels[i] && t.items().equals(List.of(new Cost("minecraft:diamond", i + 1)));
        }
        yes(h, costs, "Default costs: step n takes n diamonds and levels 10, 15, 20, 30, 35, 40, 50, 60");
        h.succeed();
    }

    /** Budget limits claiming; expansion takes material and levels only after the save, and every refusal costs nothing. */
    public static void expand(GameTestHelper h) {
        var server = h.getLevel().getServer(); var previous = Claims.get(server); String dim = h.getLevel().dimension().identifier().toString();
        try {
            var root = temp();
            var claims = new Claims(config(), root, Map.of()); Claims.install(server, claims);
            var p = player(h, h.absolutePos(new BlockPos(1, 2, 1))); UUID me = p.getUUID();
            long t0 = server.overworld().getGameTime();
            yes(h, claims.create(key(dim, 100, 0), me, t0 - 5000) && claims.create(key(dim, 101, 0), me, t0 - 4000), "Two chunks fit the start budget");
            yes(h, !claims.create(key(dim, 102, 0), me, t0 - 3000) && claims.budget(me) == 2, "The third chunk is over the budget");
            yes(h, claims.expand(p) == Claims.Expand.NO_DEED, "No deed: refused");
            var foreign = new ItemStack(ClaimDeedItem.item()); foreign.set(DeedData.TYPE, new DeedData(UUID.randomUUID(), "Other", 0));
            p.getInventory().add(foreign);
            yes(h, claims.expand(p) == Claims.Expand.NO_DEED, "A deed that names someone else does not count");
            p.getInventory().add(new ItemStack(ClaimDeedItem.item()));
            yes(h, claims.expand(p) == Claims.Expand.ITEMS, "Missing materials: refused");
            p.getInventory().add(new ItemStack(Items.IRON_INGOT, 1));
            yes(h, claims.expand(p) == Claims.Expand.ITEMS && count(p, Items.IRON_INGOT) == 1, "Half the materials: refused, nothing taken");
            p.getInventory().add(new ItemStack(Items.IRON_INGOT, 3));
            yes(h, claims.expand(p) == Claims.Expand.LEVELS && count(p, Items.IRON_INGOT) == 4, "No levels: refused, nothing taken");
            p.giveExperienceLevels(3);
            yes(h, claims.expand(p) == Claims.Expand.OK, "Materials, levels and deed: bought");
            yes(h, count(p, Items.IRON_INGOT) == 2 && p.experienceLevel == 2 && claims.budget(me) == 3, "Two ingots and one level taken, budget 3");
            yes(h, claims.create(key(dim, 102, 0), me, t0 + 1000), "The new chunk fits");
            yes(h, !claims.create(key(dim, 103, 0), me, t0 + 2000), "The fourth is over the budget again");
            p.getInventory().add(new ItemStack(Items.DIAMOND, 1));
            yes(h, claims.expand(p) == Claims.Expand.SERVER_LIMIT && count(p, Items.DIAMOND) == 1 && claims.budget(me) == 3, "A step above the server limit (4) is refused and costs nothing");
            yes(h, new Claims(config(), root, Map.of()).budget(me) == 3, "The bought budget survives a restart");
            var q = player(h, h.absolutePos(new BlockPos(1, 2, 1)));
            yes(h, claims.budget(q.getUUID()) == 2 && claims.expand(q) == Claims.Expand.NO_DEED, "Budgets belong to the player");
            yes(h, claims.setBudget(q.getUUID(), 4) && claims.budget(q.getUUID()) == 4 && !claims.setBudget(q.getUUID(), 0) && !claims.setBudget(q.getUUID(), 257), "Operator budget within 1..256");
            var off = new Claims(new ClaimConfig(true, 4, 8, 50, 20, false, 0, ClaimConfig.DEFAULT.dimensions()), temp(), Map.of());
            yes(h, off.expand(p) == Claims.Expand.DISABLED && off.budget(me) == 4, "Expansion off: DISABLED, budget = server limit");
            // a failing save takes nothing
            var root2 = temp();
            var bad = new Claims(config(), root2, Map.of()); Claims.install(server, bad);
            var r = player(h, h.absolutePos(new BlockPos(1, 2, 1)));
            r.getInventory().add(new ItemStack(ClaimDeedItem.item())); r.getInventory().add(new ItemStack(Items.IRON_INGOT, 2)); r.giveExperienceLevels(1);
            yes(h, bad.create(key(dim, 7, 7), r.getUUID(), t0 - 5000), "Seed claim");
            try {
                var ledger = root2.resolve("simpletweaks-claims.json");
                var other = root2.resolve("elsewhere.json");
                java.nio.file.Files.move(ledger, other);
                java.nio.file.Files.createSymbolicLink(ledger, other);
                yes(h, bad.expand(r) == Claims.Expand.FAILED && count(r, Items.IRON_INGOT) == 2 && r.experienceLevel == 1, "A failed save costs no material and no level");
            } catch (UnsupportedOperationException | java.io.IOException e) {
                com.mojang.logging.LogUtils.getLogger().info("[claims] symbolic links unavailable - skipping the failed-save check");
            }
        } finally { Claims.install(server, previous); TableService.reset(); }
        h.succeed();
    }

    /** The table asks the server to expand: needs the opened table in reach, works with the map switched off, answers with fresh data. */
    public static void tableExpand(GameTestHelper h) {
        var server = h.getLevel().getServer(); var previous = Claims.get(server); var level = h.getLevel();
        var base = h.absolutePos(new BlockPos(2, 3, 2));
        var table = new BlockPos((base.getX() >> 4) * 16 + 16 + 4, base.getY(), (base.getZ() >> 4) * 16 + 8);
        var oldState = level.getBlockState(table);
        var oldSender = DeedNet.toClient;
        var sent = new ArrayList<CustomPacketPayload>();
        try {
            level.setBlock(table, ClaimTableBlock.block().defaultBlockState(), 3);
            var claims = new Claims(config().withVisibility("off"), temp(), Map.of()); Claims.install(server, claims);
            DeedNet.toClient = (pl, payload) -> { sent.add(payload); return true; };
            var p = player(h, table.east(2)); var far = player(h, table.east(2));
            p.getInventory().add(new ItemStack(ClaimDeedItem.item())); p.getInventory().add(new ItemStack(Items.IRON_INGOT, 2)); p.giveExperienceLevels(1);
            TableService.resetThrottle();
            TableService.handle(new TableRequestPayload(table, TableRequestPayload.EXPAND, 0, 0, 0, 0), p);
            yes(h, sent.isEmpty() && claims.budget(p.getUUID()) == 2, "No open table: the request does nothing");
            yes(h, TableService.open(p, table), "The table opens even with the map off");
            var open = (TableOpenPayload) sent.get(0);
            yes(h, open.state() == TableOpenPayload.BUY && open.nextChunks() == 3 && open.nextLevels() == 1 && open.nextCosts().equals(List.of(new TableOpenPayload.Cost("minecraft:iron_ingot", 2)))
                    && open.maxChunks() == 2, "Open payload offers the next step with its price");
            sent.clear(); TableService.resetThrottle();
            TableService.handle(new TableRequestPayload(table, TableRequestPayload.EXPAND, 0, 0, 0, 0), p);
            yes(h, claims.budget(p.getUUID()) == 3 && count(p, Items.IRON_INGOT) == 0 && p.experienceLevel == 0, "Expansion bought through the table");
            var fresh = (TableOpenPayload) sent.get(sent.size() - 1);
            yes(h, fresh.maxChunks() == 3 && fresh.state() == TableOpenPayload.LIMIT, "The answer carries the new budget; the next step is above the server limit");
            far.getInventory().add(new ItemStack(ClaimDeedItem.item())); far.getInventory().add(new ItemStack(Items.IRON_INGOT, 2)); far.giveExperienceLevels(1);
            TableService.resetThrottle();
            TableService.handle(new TableRequestPayload(table, TableRequestPayload.EXPAND, 0, 0, 0, 0), far);
            yes(h, claims.budget(far.getUUID()) == 2 && count(far, Items.IRON_INGOT) == 2, "A player who never opened the table cannot buy");
        } finally { DeedNet.toClient = oldSender; Claims.install(server, previous); level.setBlock(table, oldState, 3); TableService.reset(); }
        h.succeed();
    }

    /** The claims layer of the wayfinder map: strips per owner and row, own land marked, visibility rules, rectangle, dimension. */
    public static void mapLayer(GameTestHelper h) {
        var server = h.getLevel().getServer(); var previous = Claims.get(server); String dim = h.getLevel().dimension().identifier().toString();
        try {
            var root = temp();
            var owner = UUID.randomUUID(); var other = UUID.randomUUID(); var stranger = UUID.randomUUID();
            var cfg = new ClaimConfig(true, 8, 8, 50, 20, false, 0, ClaimConfig.DEFAULT.dimensions());
            var claims = new Claims(cfg.withVisibility("all"), root, Map.of()); Claims.install(server, claims);
            long t = 1000;
            for (int x : new int[]{10, 11, 12, 14}) yes(h, claims.create(key(dim, x, 4), owner, t += 100), "Seed owner chunk");
            yes(h, claims.create(key(dim, 10, 5), other, t += 100), "Seed other chunk");
            yes(h, claims.create(new ClaimStore.Key("minecraft:the_nether", ChunkPos.pack(10, 4)), other, t += 100), "Seed other dimension");
            var layer = new ClaimMapLayer(); var level = h.getLevel();
            var all = layer.collectFor(level, stranger, 0, 0, 16 * 40, 16 * 40);
            yes(h, all.size() == 3, "Three strips: owner 10-12, owner 14, other 10 (is " + all.size() + ")");
            var strip = all.stream().filter(m -> m.x() == 160 && m.z() == 64).findFirst().orElseThrow();
            yes(h, strip.kind().matches("area:[0-9a-f]{6}:0:3") && strip.lines().get(0).startsWith("Claim of "), "Strip of three chunks, not mine, with the owner's name");
            yes(h, layer.collectFor(level, owner, 0, 0, 640, 640).stream().filter(m -> m.kind().endsWith(":1:3")).count() == 1, "The owner's own strip is marked");
            var cut = layer.collectFor(level, stranger, 16 * 11, 16 * 4, 16 * 11 + 15, 16 * 4 + 15);
            yes(h, cut.size() == 1 && cut.get(0).kind().endsWith(":0:1"), "The rectangle cuts the strip to the asked chunks");
            Claims.install(server, new Claims(cfg.withVisibility("owners_hidden"), root, Map.of()));
            yes(h, layer.collectFor(level, stranger, 0, 0, 640, 640).stream().allMatch(m -> m.lines().equals(List.of("Foreign claim"))), "owners_hidden: no names");
            Claims.install(server, new Claims(cfg.withVisibility("own"), root, Map.of()));
            yes(h, layer.collectFor(level, stranger, 0, 0, 640, 640).isEmpty() && layer.collectFor(level, other, 0, 0, 640, 640).size() == 1, "own: a stranger sees nothing, a player only his land");
            Claims.install(server, new Claims(cfg.withVisibility("off"), root, Map.of()));
            yes(h, layer.collectFor(level, stranger, 0, 0, 640, 640).isEmpty(), "off: nothing");
            Claims.install(server, null);
            yes(h, layer.collectFor(level, stranger, 0, 0, 640, 640).isEmpty() && layer.collectFor(level, null, 0, 0, 640, 640).isEmpty(), "No claims or no viewer: nothing");
        } finally { Claims.install(server, previous); }
        h.succeed();
    }
}
