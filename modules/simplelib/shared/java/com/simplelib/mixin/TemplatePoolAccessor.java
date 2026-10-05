package com.simplelib.mixin;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Template pools can only be replaced by datapacks, not extended; the village kitchen is appended at server start. */
@Mixin(StructureTemplatePool.class)
public interface TemplatePoolAccessor {
    @Accessor("templates")
    ObjectArrayList<StructurePoolElement> simplelib$templates();

    @Accessor("rawTemplates")
    List<Pair<StructurePoolElement, Integer>> simplelib$rawTemplates();
}
