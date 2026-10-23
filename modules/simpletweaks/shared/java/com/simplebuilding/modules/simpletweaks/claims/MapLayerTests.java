package com.simplebuilding.modules.simpletweaks.claims;

import com.simplebuilding.framework.api.MapLayerSources;
import com.simplebuilding.framework.api.SubMods;
import com.simplebuilding.modules.simpletweaks.claims.ClaimRights.Rules;
import com.simplebuilding.modules.simpletweaks.play.Mods;
import com.simplebuilding.modules.simpletweaks.table.ClaimMapLayer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimTests.*;

/** SP22-8 branch 4: claims on the wayfinder map of Simple Maps in every dimension, names by claimVisibility, the mapsPresent flag. */
public final class MapLayerTests {
    private MapLayerTests() {}

    private static ClaimStore.Key key(String dim, int x, int z) { return new ClaimStore.Key(dim, ChunkPos.pack(x, z)); }
    private static final int WIDE = 16 * 64;

    /** Each map asks for its own dimension and gets that dimension's claims, own and foreign; own land carries the credits. */
    public static void dimensions(GameTestHelper h) {
        var server = h.getLevel().getServer(); var previous = Claims.get(server);
        try {
            var owner = UUID.randomUUID(); var other = UUID.randomUUID();
            var cfg = new ClaimConfig(true, 8, 8, 50, 20, false, 0, ClaimConfig.DEFAULT.dimensions()).withVisibility("all");
            var claims = new Claims(cfg, temp(), Map.of()); Claims.install(server, claims);
            long t = 1000;
            String over = Level.OVERWORLD.identifier().toString(), nether = Level.NETHER.identifier().toString(), end = Level.END.identifier().toString();
            yes(h, claims.create(key(over, 2, 2), owner, t += 100) && claims.create(key(nether, 3, 3), owner, t += 100)
                    && claims.create(key(nether, 5, 3), other, t += 100) && claims.create(key(end, 4, 1), other, t += 100), "Seed land in three dimensions");
            var layer = new ClaimMapLayer();
            var netherLevel = server.getLevel(Level.NETHER); var endLevel = server.getLevel(Level.END); var overLevel = server.getLevel(Level.OVERWORLD);
            yes(h, netherLevel != null && endLevel != null && overLevel != null, "The test server has the three vanilla dimensions");
            var inNether = layer.collectFor(netherLevel, owner, 0, 0, WIDE, WIDE);
            yes(h, inNether.size() == 2, "Nether map: own and foreign nether claims only (is " + inNether.size() + ")");
            var mine = inNether.stream().filter(m -> m.x() == 48 && m.z() == 48).findFirst().orElse(null);
            yes(h, mine != null && mine.kind().endsWith(":1:1") && mine.lines().equals(List.of(ClaimMapLayer.ownLine(claims.credits(owner))))
                    && claims.credits(owner) == 6, "Own nether strip is marked and carries the free credits (8 paid - 2 held)");
            var foreign = inNether.stream().filter(m -> m.x() == 80).findFirst().orElse(null);
            yes(h, foreign != null && foreign.kind().endsWith(":0:1") && foreign.lines().get(0).startsWith("Claim of "), "Foreign nether strip with the owner's name");
            var inEnd = layer.collectFor(endLevel, owner, 0, 0, WIDE, WIDE);
            yes(h, inEnd.size() == 1 && inEnd.get(0).x() == 64 && inEnd.get(0).z() == 16 && inEnd.get(0).kind().endsWith(":0:1"), "End map: the foreign end claim");
            var inOver = layer.collectFor(overLevel, other, 0, 0, WIDE, WIDE);
            yes(h, inOver.size() == 1 && inOver.get(0).x() == 32 && inOver.get(0).lines().get(0).startsWith("Claim of "), "Overworld map of the other player: only the overworld claim, foreign");
            yes(h, ClaimMapLayer.ownLine(1).equals("Your land · 1 credit") && ClaimMapLayer.ownLine(0).equals("Your land · 0 credits")
                    && ClaimMapLayer.ownLine(256).length() <= 80, "Own line, singular/plural, fits the 80-character wire limit");
        } finally { Claims.install(server, previous); }
        h.succeed();
    }

    /** Foreign names follow claimVisibility: all names; owners_hidden names only where the viewer holds a right; own only involved land; off nothing. */
    public static void visibility(GameTestHelper h) {
        var server = h.getLevel().getServer(); var previous = Claims.get(server); var level = h.getLevel();
        String dim = level.dimension().identifier().toString();
        try {
            var root = temp();
            var owner = UUID.randomUUID(); var guest = UUID.randomUUID(); var stranger = UUID.randomUUID(); var other = UUID.randomUUID();
            ClaimRightsTests.seeded(h, root, key(dim, 20, 20), owner, new Rules(0, Map.of(guest, ClaimRights.USE)), Map.of());
            var layer = new ClaimMapLayer();
            for (String mode : List.of("all", "owners_hidden", "own", "off")) {
                var claims = new Claims(ClaimRightsTests.roomy().withVisibility(mode), root, Map.of()); Claims.install(server, claims);
                if (mode.equals("all")) yes(h, claims.create(key(dim, 24, 20), other, 1000), "Second owner's chunk");
                var s = layer.collectFor(level, stranger, 16 * 16, 16 * 16, 16 * 30, 16 * 30);
                var g = layer.collectFor(level, guest, 16 * 16, 16 * 16, 16 * 30, 16 * 30);
                switch (mode) {
                    case "all" -> yes(h, s.size() == 2 && s.stream().allMatch(m -> m.lines().get(0).startsWith("Claim of ")), "all: a stranger sees both owners' names");
                    case "owners_hidden" -> {
                        yes(h, s.size() == 2 && s.stream().allMatch(m -> m.lines().equals(List.of("Foreign claim"))), "owners_hidden: a stranger sees land, no names");
                        yes(h, g.size() == 2 && g.stream().filter(m -> m.lines().get(0).startsWith("Claim of ")).count() == 1, "owners_hidden: a guest sees only the name of the land he holds a right in");
                    }
                    case "own" -> yes(h, s.isEmpty() && g.size() == 1 && g.get(0).x() == 320 && g.get(0).lines().get(0).startsWith("Claim of "), "own: a stranger sees nothing, a guest only the land he holds a right in");
                    default -> yes(h, s.isEmpty() && g.isEmpty() && layer.collectFor(level, owner, 0, 0, WIDE, WIDE).isEmpty(), "off: nobody sees claims on the map");
                }
            }
        } finally { Claims.install(server, previous); }
        h.succeed();
    }

    /** mapsPresent follows the real state (Simple Maps registered and switched on); with Simple Maps the claims source is there. */
    public static void present(GameTestHelper h) {
        boolean maps = BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath(ClaimMaps.MOD, "wayfinder_map"));
        yes(h, ClaimMaps.present() == (maps && SubMods.enabled(ClaimMaps.MOD)), "Real state: mapsPresent iff Simple Maps is there (" + maps + ")");
        if (maps && Claims.enabled(h.getLevel().getServer()))
            yes(h, MapLayerSources.get(ClaimMapLayer.ID) != null, "With Simple Maps and claims on, the claims source is registered");
        var saved = Mods.check;
        try {
            Mods.check = id -> false;
            yes(h, !ClaimMaps.present(), "Without Simple Maps: no flag (the deed shows the hint)");
            Mods.check = id -> id.equals(ClaimMaps.MOD);
            yes(h, ClaimMaps.present() == SubMods.enabled(ClaimMaps.MOD), "With Simple Maps: flag, unless a super-mod switched it off");
            Mods.check = id -> id.equals("simplemapsx");
            yes(h, !ClaimMaps.present(), "Only the exact mod id counts");
        } finally { Mods.check = saved; }
        h.succeed();
    }
}
