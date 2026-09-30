package com.simplevisuals.mixin;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(net.minecraft.server.packs.repository.PackRepository.class)
public interface PackRepositoryAccessor {
 @Accessor("sources") java.util.Set<net.minecraft.server.packs.repository.RepositorySource> visuals$sources();
 @Mutable @Accessor("sources") void visuals$sources(java.util.Set<net.minecraft.server.packs.repository.RepositorySource> sources);
}
