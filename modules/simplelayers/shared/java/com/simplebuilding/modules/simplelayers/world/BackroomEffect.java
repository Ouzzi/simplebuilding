package com.simplebuilding.modules.simplelayers.world;

import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.block.BackroomBlock;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The "Backroom" status effect (owner 09.11., LAYERS12 objection; plan docs/ai/PLAN-B-LAYERS13-2026-11-09.md): every player in
 * the Backrooms carries it while {@code backrooms.unbreakableWalls} is on, and while he carries it he cannot mine the rooms'
 * blocks ({@link com.simplebuilding.modules.simplelayers.block.BackroomBlock#protectedFor}). The icon shows why the walls hold
 * (A14). Refreshed every {@link #PERIOD} ticks for {@link #DURATION}: outside the Backrooms it runs out within two seconds.
 */
public final class BackroomEffect extends MobEffect {
    public static final int COLOR = 0xD2BC64, PERIOD = 10, DURATION = 40;
    public static final ResourceKey<MobEffect> KEY = ResourceKey.create(Registries.MOB_EFFECT, SimpleLayers.id("backroom"));
    private static Holder<MobEffect> holder;

    private BackroomEffect() {
        super(MobEffectCategory.NEUTRAL, COLOR);
    }

    public static void register() {
        holder = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, KEY, new BackroomEffect());
    }

    public static Holder<MobEffect> holder() {
        return holder;
    }

    /** Whether the server hands the effect to this player now: in the Backrooms, not spectating, the protection switched on. */
    public static boolean due(Player player) {
        return LayersConfig.unbreakableWalls && SimpleLayers.on() && !player.isSpectator() && player.level().dimension() == SimpleLayers.BACKROOMS;
    }

    /** A14: hitting a held wall says why it does not give way (action bar, server side only). */
    public static void hintOnAttack(Player player, Level level, BlockPos pos) {
        if (!level.isClientSide() && level.getBlockState(pos).getBlock() instanceof BackroomBlock && BackroomBlock.protectedFor(player)) {
            player.sendOverlayMessage(Component.translatable("message.simplelayers.backroom_walls"));
        }
    }

    public static void tick(MinecraftServer server) {
        if (holder == null || server.getTickCount() % PERIOD != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (due(player)) player.addEffect(new MobEffectInstance(holder, DURATION, 0, true, false, true));
        }
    }
}
