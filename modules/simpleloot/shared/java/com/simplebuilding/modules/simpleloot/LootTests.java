package com.simplebuilding.modules.simpleloot;

import com.simplebuilding.framework.api.SubMods;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultServerData;

/** Shared bodies; Fabric wraps them in ModuleGameTest, NeoForge/Forge register {@link #ALL}. */
public final class LootTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    static {
        ALL.put("launch", LootTests::launch);
        ALL.put("vault", LootTests::vault);
        ALL.put("vault_persistence", LootTests::vaultPersistence);
        ALL.put("config", LootTests::config);
        ALL.put("qol_migration", LootTests::qolMigration);
        ALL.put("super_switch", LootTests::superSwitch);
    }

    private LootTests() {}

    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(SimpleLoot.MOD_ID, name);
    }

    private static void configured(Runnable body) {
        var old = SimpleLoot.CONFIG;
        try {
            SimpleLoot.CONFIG = new LootConfig();
            body.run();
        } finally {
            SimpleLoot.CONFIG = old;
            SubMods.set(SimpleLoot.MOD_ID, true);
        }
    }

    public static void launch(GameTestHelper h) {
        var root = h.getLevel().getServer().getCommands().getDispatcher().getRoot();
        h.assertTrue(root.getChild(SimpleLoot.MOD_ID) != null, "/simpleloot registered");
        h.succeed();
    }

    public static void vault(GameTestHelper h) {
        configured(() -> {
            var data = new VaultServerData();
            var cooldown = (IVaultCooldown) data;
            var uuid = UUID.randomUUID();
            h.assertTrue(cooldown.hasLootedRecently(uuid, 100), "Missing legacy timestamp starts cooldown");
            cooldown.markLooted(uuid, 100);
            h.assertTrue(cooldown.hasLootedRecently(uuid, 99) && cooldown.hasLootedRecently(uuid, 100 + 23999), "Rollback/early repeat refused");
            SimpleLoot.config().vaultCooldownDays = 1;
            h.assertTrue(!cooldown.hasLootedRecently(uuid, 100 + 24000), "Exact cooldown expiry");
            var copy = new VaultServerData();
            ((IVaultCooldown) copy).setLootTimesMap(cooldown.getLootTimesMap());
            h.assertTrue(((IVaultCooldown) copy).getLootTimesMap().get(uuid) == 100, "UUID/time map preserved");
        });
        h.succeed();
    }

    public static void vaultPersistence(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, Blocks.VAULT);
        var v = (VaultBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
        var uuid = UUID.randomUUID();
        ((IVaultCooldown) v.getServerData()).markLooted(uuid, 12345);
        var tag = v.saveWithFullMetadata(h.getLevel().registryAccess());
        h.assertTrue(tag.contains("SimpleBuildingLootTimes"), "Legacy persistent map key written");
        var loaded = (VaultBlockEntity) BlockEntity.loadStatic(v.getBlockPos(), v.getBlockState(), tag, h.getLevel().registryAccess());
        loaded.setLevel(h.getLevel());
        h.assertTrue(((IVaultCooldown) loaded.getServerData()).getLootTimesMap().get(uuid) == 12345, "Actual block-entity save/reload keeps UUID and timestamp");
        h.succeed();
    }

    public static void config(GameTestHelper h) {
        var c = new LootConfig();
        h.assertTrue(c.enableVaultCooldown && c.vaultCooldownDays == 100, "defaults like Simple QoL");
        c.vaultCooldownDays = 0; c.normalize();
        h.assertTrue(c.vaultCooldownDays == 1, "lower bound");
        c.vaultCooldownDays = Integer.MAX_VALUE; c.normalize();
        h.assertTrue(c.vaultCooldownDays == 36500, "upper bound");
        h.succeed();
    }

    /** First start: the old qOL values are taken over once; an existing own file is never overwritten. */
    public static void qolMigration(GameTestHelper h) {
        try {
            var dir = Files.createTempDirectory("simpleloot-migration");
            Files.writeString(dir.resolve("simplequalityoflife.json"), "{\"qOL\":{\"enableVaultCooldown\":false,\"vaultCooldownDays\":7,\"enableAutowalk\":true}}");
            var taken = SimpleLoot.read(dir);
            h.assertTrue(!taken.enableVaultCooldown && taken.vaultCooldownDays == 7, "old QoL values taken over");
            h.assertTrue(Files.isRegularFile(dir.resolve("simpleloot.json")), "own file written as marker");
            Files.writeString(dir.resolve("simpleloot.json"), "{\"vaultCooldownDays\":3}");
            var own = SimpleLoot.read(dir);
            h.assertTrue(own.enableVaultCooldown && own.vaultCooldownDays == 3, "own file wins over QoL, missing keys default");
            var empty = SimpleLoot.read(Files.createTempDirectory("simpleloot-empty"));
            h.assertTrue(empty.enableVaultCooldown && empty.vaultCooldownDays == 100, "no file: defaults");
        } catch (java.io.IOException e) {
            h.fail("migration io: " + e);
        }
        h.succeed();
    }

    /** "Enable Simple Loot" off in the super-mod: vaults behave like Vanilla (no repeat reward). */
    public static void superSwitch(GameTestHelper h) {
        configured(() -> {
            SubMods.set(SimpleLoot.MOD_ID, false);
            h.assertTrue(!SimpleLoot.active(), "switch read through framework");
        });
        h.assertTrue(SimpleLoot.active(), "switch restored");
        h.succeed();
    }
}
