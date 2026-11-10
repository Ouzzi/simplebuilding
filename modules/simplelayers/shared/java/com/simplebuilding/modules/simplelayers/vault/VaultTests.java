package com.simplebuilding.modules.simplelayers.vault;

import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.vault.block.ChimeCrystalBlock;
import com.simplebuilding.modules.simplelayers.vault.block.PathTileBlock;
import com.simplebuilding.modules.simplelayers.vault.block.SealPart;
import com.simplebuilding.modules.simplelayers.vault.block.SealPedestalBlock;
import com.simplebuilding.modules.simplelayers.vault.block.SealSocketBlock;
import com.simplebuilding.modules.simplelayers.vault.block.VaultGateBlock;
import com.simplebuilding.modules.simplelayers.vault.block.VaultLampBlock;
import com.simplebuilding.modules.simplelayers.vault.gen.VaultGen;
import com.simplebuilding.modules.simplelayers.vault.gen.VaultLayout;
import com.simplebuilding.modules.simplelayers.vault.gen.VaultPiece;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bedrock Vault tests (UNDER21, plan docs/ai/PLAN-B-UNDER21-2026-11-10.md), registered into {@code LayersTests.ALL} (ids
 * module_game_test_vault_*). The pure generator parts run everywhere; the parts that need the vault dimension run where data
 * pack dimensions load (with SimpleBuilding) and check only the registries in the standalone run (like the Backrooms tests).
 */
public final class VaultTests {
    private static final long SEED = 21L;

    private VaultTests() {}

    public static void register(Map<String, Consumer<GameTestHelper>> all) {
        all.put("vault_registered", VaultTests::registered);
        all.put("vault_bedrock_hull", VaultTests::bedrockHull);
        all.put("vault_layout_connected", VaultTests::layoutConnected);
        all.put("vault_layered_biome", VaultTests::layeredBiome);
        all.put("vault_gate_closes", VaultTests::gateCloses);
        all.put("vault_puzzle_solvable", VaultTests::puzzleSolvable);
        all.put("vault_bedrock_held", VaultTests::bedrockHeld);
        all.put("vault_gap_generated", VaultTests::gapGenerated);
    }

    /** The vault level, or null after checking the registries (standalone servers bake no data pack dimensions). */
    static ServerLevel vaultOrRegistryOnly(GameTestHelper h) {
        ServerLevel level = h.getLevel().getServer().getLevel(VaultRegistry.VAULT);
        if (level != null) return level;
        h.assertTrue(h.getLevel().registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).containsKey(SimpleLayers.id("bedrock_vault")), "dimension type");
        h.assertTrue(h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).containsKey(SimpleLayers.id("bedrock_vault")), "layered biome");
        h.succeed();
        return null;
    }

    static void registered(GameTestHelper h) {
        for (String b : List.of("vault_gate", "vault_lamp", "vault_lever", "chime_crystal", "path_tile", "seal_pedestal", "seal_socket", "ascent_altar",
                "ascent_seal", "ascent_stream")) {
            h.assertTrue(BuiltInRegistries.BLOCK.containsKey(SimpleLayers.id(b)), "block " + b);
            h.assertFalse(BuiltInRegistries.ITEM.containsKey(SimpleLayers.id(b)), "no item for " + b);
        }
        for (String i : VaultRegistry.ITEM_PATHS) h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleLayers.id(i)), "item " + i);
        h.assertTrue(BuiltInRegistries.CHUNK_GENERATOR.containsKey(SimpleLayers.id("bedrock_vault")), "chunk generator");
        h.assertTrue(BuiltInRegistries.FEATURE_TYPE.containsKey(SimpleLayers.id("vault_gap")) && BuiltInRegistries.FEATURE_TYPE.containsKey(SimpleLayers.id("vault_cache")), "features");
        h.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(SimpleLayers.id("vault_bound")) && VaultEffect.holder() != null, "effect");
        h.assertTrue(VaultConfig.enabled && VaultConfig.gapRarity == 400 && VaultConfig.closeSeconds == 30, "config defaults");
        for (VaultPiece p : VaultPiece.values()) {
            BlockState s = VaultBlocks.state(p);
            h.assertTrue(p == VaultPiece.AIR || !s.isAir(), "piece " + p + " parses to a block");
        }
        h.assertTrue(VaultBlocks.state(VaultPiece.LEVER_N).getValue(LeverBlock.FACING) == net.minecraft.core.Direction.NORTH, "piece properties applied");
        h.succeed();
    }

    /** Every room behind one cladding layer, bedrock behind it, no thin wall between two spaces; the region border solid bedrock. */
    static void bedrockHull(GameTestHelper h) {
        for (int i = 0; i < 3; i++) {
            VaultGen.Volume v = VaultGen.complex(SEED + i, i * 7 - 5, 3 - i * 11);
            List<String> problems = v.hullProblems();
            h.assertTrue(problems.isEmpty(), "hull " + i + ": " + (problems.isEmpty() ? "" : problems.get(0)));
            // samples: from the arrival, straight out of the Entrance Hall: cladding, then bedrock
            VaultLayout l = v.layout();
            VaultLayout.Room e = l.room(VaultLayout.Type.ENTRANCE);
            int[] g = l.gate.get(0);
            int[] out = switch (l.entranceSide) {
                case 0 -> new int[] {0, -1};
                case 1 -> new int[] {1, 0};
                case 2 -> new int[] {0, 1};
                default -> new int[] {-1, 0};
            };
            h.assertTrue(v.get(g[0], g[1], g[2]) == VaultPiece.GATE_OPEN, "gate in the wall plane");
            h.assertTrue(v.get(g[0] + out[0], g[1], g[2] + out[1]) == VaultPiece.BEDROCK, "bedrock right behind the gate");
            h.assertTrue(v.get(e.x0(), e.y0() - 1, e.z0()).shell() && v.get(e.x0(), e.y0() - 2, e.z0()) == VaultPiece.BEDROCK, "floor cladding, bedrock below");
            h.assertTrue(v.get(e.cx(), e.y1() + 2, e.cz()) == VaultPiece.BEDROCK, "bedrock above the ceiling");
        }
        ServerLevel vault = vaultOrRegistryOnly(h);
        if (vault == null) return;
        VaultLayout l = VaultLayout.at(VaultTravel.seed(vault), 0, 0);
        VaultLayout.Room e = l.room(VaultLayout.Type.ENTRANCE);
        vault.getChunk(e.cx() >> 4, e.cz() >> 4);
        h.assertTrue(vault.getBlockState(new BlockPos(e.cx(), e.y0() - 2, e.cz())).is(Blocks.BEDROCK), "generated: bedrock under the floor");
        h.assertFalse(vault.getBlockState(new BlockPos(e.cx(), e.y0() - 1, e.cz())).is(Blocks.BEDROCK), "generated: cladding on top");
        h.assertTrue(vault.getBlockState(new BlockPos(l.bx + 1, 64, l.bz + 1)).is(Blocks.BEDROCK), "generated: solid bedrock between the vaults");
        h.assertTrue(vault.getBlockState(new BlockPos(e.cx(), 0, e.cz())).is(Blocks.BEDROCK) && vault.getBlockState(new BlockPos(e.cx(), 127, e.cz())).is(Blocks.BEDROCK), "bottom and top");
        h.succeed();
    }

    /** From the arrival every room, both floors of the halls and every riddle part is reachable on foot; the arrival is free. */
    static void layoutConnected(GameTestHelper h) {
        for (int i = 0; i < 3; i++) {
            VaultGen.Volume v = VaultGen.complex(SEED * 3 + i, 40 - i * 13, i * 9 - 2);
            VaultLayout l = v.layout();
            h.assertTrue(v.stand(l.arrival[0], l.arrival[1], l.arrival[2]), "free arrival " + i);
            boolean[] seen = v.reach(l.arrival);
            for (VaultLayout.Room r : l.rooms) {
                boolean any = false;
                for (int x = r.x0(); x <= r.x1() && !any; x++) for (int z = r.z0(); z <= r.z1() && !any; z++) {
                    int k = v.idx(x, r.y0(), z);
                    any = k >= 0 && seen[k];
                    if (!any && r.level() < 0) {
                        k = v.idx(x, VaultLayout.F1, z);
                        any = k >= 0 && seen[k];
                    }
                }
                h.assertTrue(any, "room " + r.type() + " reachable in vault " + i);
            }
            for (int p = 0; p < VaultLayout.PARTS; p++) {
                h.assertTrue(v.reachable(seen, l.pedestals[p][0], l.pedestals[p][1], l.pedestals[p][2]), "pedestal " + p);
                h.assertTrue(v.reachable(seen, l.sockets[p][0], l.sockets[p][1], l.sockets[p][2]), "socket " + p);
            }
            for (int[] q : l.levers) h.assertTrue(v.reachable(seen, q[0], q[1], q[2]), "lever");
            for (int[] q : l.crystals) h.assertTrue(v.reachable(seen, q[0], q[1] - 1, q[2]), "crystal");
            h.assertTrue(v.reachable(seen, l.altar[0], l.altar[1] + 1, l.altar[2]), "altar");
            h.assertTrue(l.rooms.size() >= 14, "a complex: " + l.rooms.size() + " rooms");
        }
        h.succeed();
    }

    static void layeredBiome(GameTestHelper h) {
        ServerLevel vault = vaultOrRegistryOnly(h);
        if (vault == null) return;
        VaultLayout l = VaultLayout.at(VaultTravel.seed(vault), 0, 0);
        BlockPos at = new BlockPos(l.arrival[0], l.arrival[1], l.arrival[2]);
        vault.getChunk(at.getX() >> 4, at.getZ() >> 4);
        h.assertTrue(vault.getBiome(at).is(SimpleLayers.id("bedrock_vault")), "the layered biome at the arrival");
        h.assertTrue(vault.dimensionType().hasCeiling() && !vault.dimensionType().hasSkyLight(), "a ceiling, no sky");
        h.assertTrue(vault.getMinY() == 0 && vault.getHeight() == VaultGen.HEIGHT, "height 128");
        h.succeed();
    }

    /** Occupied: the gate stays open for vault.closeSeconds, then seals; empty: it opens again. */
    static void gateCloses(GameTestHelper h) {
        ServerLevel vault = vaultOrRegistryOnly(h);
        if (vault == null) return;
        VaultTravel.reset();
        VaultLayout l = VaultLayout.at(VaultTravel.seed(vault), 1 << 14, 1 << 14);
        int[] g = l.gate.get(0);
        vault.getChunk(g[0] >> 4, g[2] >> 4);
        h.assertTrue(vault.getBlockState(new BlockPos(g[0], g[1], g[2])).is(VaultRegistry.VAULT_GATE), "gate generated");
        long t0 = 1000;
        h.assertTrue(VaultTravel.update(vault, l, true, t0), "open right after the entry");
        h.assertTrue(VaultTravel.update(vault, l, true, t0 + VaultConfig.closeSeconds * 20L - 1), "still open just before the time");
        h.assertFalse(VaultTravel.update(vault, l, true, t0 + VaultConfig.closeSeconds * 20L), "sealed after the time");
        for (int[] p : l.gate) h.assertFalse(vault.getBlockState(new BlockPos(p[0], p[1], p[2])).getValue(VaultGateBlock.OPEN), "every gate block sealed");
        h.assertTrue(VaultTravel.update(vault, l, false, t0 + 2000), "empty: open again");
        h.assertTrue(VaultTravel.gateOpen(vault, l), "gate blocks open");
        VaultTravel.reset();
        h.succeed();
    }

    /** The whole riddle played through: levers, chime, path → three pedestals → three sockets → seal gone, stream to the top. */
    static void puzzleSolvable(GameTestHelper h) {
        // pure: every mural has exactly one lever setting; sequences use all four tones; paths are contiguous from row 0 to the last
        for (int target = 1; target < 32; target++) h.assertTrue(VaultLayout.lampsFor(VaultLayout.leversFor(target)) == target, "levers for " + target);
        for (int i = 0; i < 20; i++) {
            VaultLayout l = VaultLayout.of(SEED + i, i, -i);
            h.assertTrue(l.target > 0 && l.target < 32, "mural never dark");
            List<Integer> p = l.path;
            h.assertTrue(p.get(0) / VaultLayout.PATH_COLS == 0 && p.get(p.size() - 1) / VaultLayout.PATH_COLS == VaultLayout.PATH_ROWS - 1, "path spans the field");
            for (int k = 1; k < p.size(); k++) {
                int a = p.get(k - 1), b = p.get(k);
                h.assertTrue(Math.abs(a % 5 - b % 5) + Math.abs(a / 5 - b / 5) == 1, "path steps to a neighbour");
            }
        }
        ServerLevel vault = vaultOrRegistryOnly(h);
        if (vault == null) return;
        VaultLayout l = VaultLayout.at(VaultTravel.seed(vault), -(1 << 13), 1 << 13);
        for (VaultLayout.Room r : l.rooms) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) vault.getChunk((r.cx() >> 4) + dx, (r.cz() >> 4) + dz);
        VaultPuzzles.forgetChimes();
        // light: set the levers to the one solution
        int levers = VaultLayout.leversFor(l.target);
        for (int i = 0; i < VaultLayout.LEVERS; i++) {
            BlockPos p = at(l.levers[i]);
            BlockState s = vault.getBlockState(p);
            h.assertTrue(s.is(VaultRegistry.VAULT_LEVER), "lever " + i + " generated");
            vault.setBlock(p, s.setValue(LeverBlock.POWERED, (levers >> i & 1) == 1), 3);
        }
        VaultPuzzles.leverPulled(vault, at(l.levers[0]), null);
        for (int i = 0; i < VaultLayout.LEVERS; i++) {
            h.assertTrue(vault.getBlockState(at(l.lamps[i])).getValue(VaultLampBlock.LIT) == ((l.target >> i & 1) == 1), "lamp " + i + " follows");
        }
        h.assertTrue(ready(vault, l, SealPart.LIGHT), "light pedestal ready");
        // sound: one wrong strike fades, then the frieze's order
        int wrong = (l.sequence[0] + 1) % VaultLayout.TONES;
        VaultPuzzles.strike(vault, crystal(l, wrong), wrong, null);
        for (int tone : l.sequence) {
            BlockPos c = crystal(l, tone);
            h.assertTrue(vault.getBlockState(c).getValue(ChimeCrystalBlock.TONE) == tone, "crystal tone");
            VaultPuzzles.strike(vault, c, tone, null);
        }
        h.assertTrue(ready(vault, l, SealPart.SOUND), "sound pedestal ready");
        // path: two right steps, a wrong one puts it out, then the whole path (from the far end this time)
        List<Integer> path = l.path;
        VaultPuzzles.step(vault, at(l.pathBlock(path.get(0), 0, 0)), null);
        VaultPuzzles.step(vault, at(l.pathBlock(path.get(1), 1, 1)), null);
        h.assertTrue(lit(vault, l, path.get(0)) && lit(vault, l, path.get(1)), "right steps light");
        int off = -1;
        for (int c = 0; c < VaultLayout.PATH_COLS * VaultLayout.PATH_ROWS && off < 0; c++) if (!path.contains(c)) off = c;
        VaultPuzzles.step(vault, at(l.pathBlock(off, 0, 0)), null);
        h.assertFalse(lit(vault, l, path.get(0)), "a wrong step puts the path out");
        for (int k = path.size() - 1; k >= 0; k--) VaultPuzzles.step(vault, at(l.pathBlock(path.get(k), 0, 1)), null);
        h.assertTrue(ready(vault, l, SealPart.PATH), "path pedestal ready");
        // seal parts into the altar
        for (SealPart part : SealPart.values()) {
            BlockPos ped = at(l.pedestals[part.index()]);
            ItemStack stack = VaultPuzzles.takeFragment(vault, ped, vault.getBlockState(ped), null);
            h.assertTrue(stack != null && SealFragmentItem.fits(stack, part, l.rx, l.rz), "pedestal gives the bound part " + part.name);
            h.assertFalse(SealFragmentItem.fits(stack, part, l.rx + 1, l.rz), "not for another vault");
            BlockPos socket = at(l.sockets[part.index()]);
            h.assertTrue(VaultPuzzles.insert(vault, socket, vault.getBlockState(socket), null, stack), "socket takes it");
            h.assertTrue(vault.getBlockState(socket).getValue(SealSocketBlock.FILLED), "socket filled");
        }
        h.assertTrue(VaultPuzzles.ascentOpen(vault, l), "the dome seal gave way");
        BlockPos low = at(l.altar).above(), top = new BlockPos(l.altar[0], VaultLayout.SHAFT_TOP, l.altar[2]);
        h.assertTrue(vault.getBlockState(low).is(VaultRegistry.ASCENT_STREAM) && vault.getBlockState(top).is(VaultRegistry.ASCENT_STREAM), "stream from the altar to the top");
        h.assertTrue(VaultTravel.handsOver(top) && !VaultTravel.handsOver(low), "the top hands over to the Overworld");
        h.succeed();
    }

    /** Bedrock, friezes and markers are held with the effect in the vault; the cladding is not; riddle blocks are unbreakable. */
    static void bedrockHeld(GameTestHelper h) {
        for (Block b : List.of(Blocks.BEDROCK, Blocks.AMETHYST_BLOCK, Blocks.PRISMARINE_BRICKS, Blocks.QUARTZ_BLOCK, Blocks.OCHRE_FROGLIGHT)) {
            h.assertTrue(b.defaultBlockState().is(VaultRegistry.VAULT_HELD), b + " held");
        }
        for (VaultPiece p : List.of(VaultPiece.CALCITE, VaultPiece.POLISHED_DEEPSLATE, VaultPiece.DEEPSLATE_TILES, VaultPiece.TUFF_BRICKS, VaultPiece.SMOOTH_STONE)) {
            h.assertFalse(VaultBlocks.state(p).is(VaultRegistry.VAULT_HELD), p + " cladding stays mineable");
        }
        BlockPos rel = new BlockPos(2, 2, 2), abs = h.absolutePos(rel);
        for (Block b : List.of(VaultRegistry.VAULT_GATE, VaultRegistry.VAULT_LAMP, VaultRegistry.CHIME_CRYSTAL, VaultRegistry.PATH_TILE, VaultRegistry.SEAL_PEDESTAL,
                VaultRegistry.SEAL_SOCKET, VaultRegistry.ASCENT_ALTAR, VaultRegistry.ASCENT_SEAL)) {
            h.setBlock(rel, b);
            h.assertTrue(h.getLevel().getBlockState(abs).getDestroySpeed(h.getLevel(), abs) < 0.0F, b + " unbreakable");
        }
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.addEffect(new MobEffectInstance(VaultEffect.holder(), 200));
        h.assertFalse(VaultEffect.due(player), "not handed out outside the vault");
        h.assertFalse(VaultEffect.held(Blocks.AMETHYST_BLOCK.defaultBlockState(), player), "outside the vault amethyst breaks");
        h.setBlock(rel, Blocks.AMETHYST_BLOCK);
        h.assertTrue(h.getLevel().getBlockState(abs).getDestroyProgress(player, h.getLevel(), abs) > 0.0F, "and the mixin lets it");
        h.succeed();
    }

    static void gapGenerated(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        BlockPos floor = new BlockPos(o.getX() + 2400, level.getMinY(), o.getZ() + 8);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.getChunk((floor.getX() >> 4) + dx, (floor.getZ() >> 4) + dz);
        VaultGapFeature.build(level, floor);
        int gates = 0, ladders = 0, walls = 0;
        for (int dx = -1; dx <= 2; dx++) for (int dz = -1; dz <= 2; dz++) for (int dy = 0; dy <= VaultGapFeature.TOP_DY; dy++) {
            BlockState s = level.getBlockState(floor.offset(dx, dy, dz));
            if (s.is(VaultRegistry.VAULT_GATE) && s.getValue(VaultGateBlock.OPEN)) gates++;
            if (s.is(Blocks.LADDER)) ladders++;
            if (s.is(Blocks.BEDROCK) && (dx < 0 || dx > 1 || dz < 0 || dz > 1)) walls++;
        }
        h.assertTrue(gates == 4 && level.getBlockState(floor.offset(0, VaultGapFeature.GATE_DY, 0)).is(VaultRegistry.VAULT_GATE), "2 x 2 open gate " + gates);
        h.assertTrue(level.getBlockState(floor).is(Blocks.BEDROCK) && level.getBlockState(floor.offset(1, 0, 1)).is(Blocks.BEDROCK), "bedrock under the gate");
        h.assertTrue(ladders >= 3 && walls == 12 * (VaultGapFeature.TOP_DY + 1), "ladder " + ladders + ", bedrock walls " + walls);
        h.assertTrue(VaultConfig.gapRarity > 0, "gaps on by default");
        h.succeed();
    }

    private static BlockPos at(int[] p) {
        return new BlockPos(p[0], p[1], p[2]);
    }

    private static BlockPos crystal(VaultLayout l, int tone) {
        for (int k = 0; k < VaultLayout.TONES; k++) if (l.crystalTone[k] == tone) return at(l.crystals[k]);
        throw new IllegalStateException("no crystal for tone " + tone);
    }

    private static boolean ready(ServerLevel level, VaultLayout l, SealPart part) {
        return level.getBlockState(at(l.pedestals[part.index()])).getValue(SealPedestalBlock.READY);
    }

    private static boolean lit(ServerLevel level, VaultLayout l, int cell) {
        return level.getBlockState(at(l.pathBlock(cell, 0, 0))).getValue(PathTileBlock.LIT);
    }
}
