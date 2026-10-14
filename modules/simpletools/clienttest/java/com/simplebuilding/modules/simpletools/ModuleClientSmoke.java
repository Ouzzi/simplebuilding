package com.simplebuilding.modules.simpletools;
import com.simplebuilding.modules.simpletools.vault.DeathLedger;
import com.simplebuilding.modules.simpletools.vault.Fate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Boots the mod and shows the Death Vault screen with one drop of every fate (screenshots, also with the future buy-back option). */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simpletools")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simpletools-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(ModuleClientSmoke::recordDeath);
            world.getServer().runOnServer(server -> open(server));
            context.waitTicks(45);
            context.takeScreenshot("simpletools-vault");
            world.getServer().runOnServer(server -> {
                SimpleTools.config().deathVaultRedeem = true;
                open(server);
            });
            context.waitTicks(45);
            context.takeScreenshot("simpletools-vault-redeem");
            world.getServer().runOnServer(server -> SimpleTools.config().deathVaultRedeem = false);
            context.runOnClient(client -> client.gui.setScreen(null));
        }
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static ItemEntity drop(ServerLevel level, ServerPlayer p, ItemStack stack, boolean add) {
        ItemEntity e = new ItemEntity(level, p.getX() + 6, p.getY() + 2, p.getZ(), stack);
        e.setDeltaMovement(0, 0, 0);
        if (add) level.addFreshEntity(e);
        return e;
    }

    /** One drop per fate: two lying (one in a loaded chunk, one unloaded), picked up by the owner and by another player, burned, despawned, void, other. */
    private static void recordDeath(MinecraftServer server) {
        ServerPlayer p = player(server);
        ServerLevel level = p.level();
        List<ItemEntity> drops = new ArrayList<>();
        ItemEntity lying = drop(level, p, new ItemStack(Items.DIAMOND_PICKAXE), true);
        ItemEntity lying2 = drop(level, p, new ItemStack(Items.IRON_CHESTPLATE), true);
        ItemEntity away = drop(level, p, new ItemStack(Items.GOLDEN_APPLE, 3), false);
        ItemEntity owner = drop(level, p, new ItemStack(Items.COBBLESTONE, 64), true);
        ItemEntity other = drop(level, p, new ItemStack(Items.ENDER_PEARL, 8), true);
        ItemEntity burned = drop(level, p, new ItemStack(Items.LEATHER_BOOTS), true);
        ItemEntity despawned = drop(level, p, new ItemStack(Items.BREAD, 12), true);
        ItemEntity fallen = drop(level, p, new ItemStack(Items.ELYTRA), true);
        ItemEntity gone = drop(level, p, new ItemStack(Items.TNT, 4), true);
        drops.addAll(List.of(lying, lying2, away, owner, other, burned, despawned, fallen, gone));
        DeathLedger ledger = DeathLedger.get(server);
        ledger.record(p.getUUID(), level.getGameTime() - 2400, Optional.empty(), drops);
        ((com.simplebuilding.modules.simpletools.mixin.ItemEntityAgeAccessor) lying).simpletools$setAge(1500);
        ((com.simplebuilding.modules.simpletools.mixin.ItemEntityAgeAccessor) lying2).simpletools$setAge(5400);
        ((com.simplebuilding.modules.simpletools.mixin.ItemEntityAgeAccessor) away).simpletools$setAge(3000);
        ledger.unloaded(away);
        ledger.pickedUp(owner.getUUID(), p);
        var last = ledger.last(p.getUUID()).orElseThrow();
        for (var e : last.entries) {
            if (e.entity.equals(other.getUUID())) {
                e.state = Fate.PICKED_OTHER.id;
                e.who = "Steve";
            }
        }
        burned.hurtServer(level, level.damageSources().lava(), 100);
        ((com.simplebuilding.modules.simpletools.mixin.ItemEntityAgeAccessor) despawned).simpletools$setAge(DeathLedger.LIFETIME - 1);
        despawned.tick();
        fallen.setPos(fallen.getX(), level.getMinY() - 100, fallen.getZ());
        fallen.checkBelowWorld();
        gone.kill(level);
    }

    private static void open(MinecraftServer server) {
        ServerPlayer p = player(server);
        ServerLevel level = p.level();
        BlockPos pos = p.blockPosition().offset(0, 0, 2);
        level.setBlockAndUpdate(pos, ToolsRegistry.DEATH_VAULT.defaultBlockState());
        p.openMenu(level.getBlockState(pos).getMenuProvider(level, pos));
    }
}
