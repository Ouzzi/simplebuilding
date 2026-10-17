package com.simplebuilding.modules.simpletweaks.admin.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanish: the tracked entities, to re-evaluate who sees a player after /vanish. */
@Mixin(ChunkMap.class)
public interface AdminChunkMapAccessor {
    @Accessor("entityMap") Int2ObjectMap<?> simpletweaks$entityMap();
}
