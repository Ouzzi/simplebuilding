package com.simplebuilding.modules.simpletweaks.admin.mixin;

import com.simplebuilding.modules.simpletweaks.admin.blocklog.BlockRecorder;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Block log (X): blocks an explosion destroyed, with their old state and contents. */
@Mixin(ServerExplosion.class)
public abstract class AdminExplosionMixin {
    @Shadow @Final private ServerLevel level;
    @Unique private Map<BlockPos, String[]> simpletweaks$before;

    @Inject(method = "interactWithBlocks", at = @At("HEAD"))
    private void simpletweaks$beforeBlocks(List<BlockPos> targets, CallbackInfo ci) {
        simpletweaks$before = null;
        if (!BlockRecorder.explosions()) return;
        var map = new LinkedHashMap<BlockPos, String[]>();
        for (var pos : targets) {
            var before = BlockRecorder.worldBefore(level, pos);
            if (before != null) map.put(pos.immutable(), before);
        }
        simpletweaks$before = map;
    }

    @Inject(method = "interactWithBlocks", at = @At("RETURN"))
    private void simpletweaks$afterBlocks(List<BlockPos> targets, CallbackInfo ci) {
        var map = simpletweaks$before;
        simpletweaks$before = null;
        if (map == null || map.isEmpty()) return;
        var self = (ServerExplosion) (Object) this;
        Object actor = BlockRecorder.explosionActor(self.getDirectSourceEntity(), self.getIndirectSourceEntity());
        for (var e : map.entrySet()) BlockRecorder.worldBroke(level, actor, 'X', e.getKey(), e.getValue());
    }
}
