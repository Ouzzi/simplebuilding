package com.simplebuilding.mixin;

import org.spongepowered.asm.mixin.Mixin;

/** Soul lava world generation is only on the 26.3 line (McVersion.CRUCIBLE). */
@Mixin(net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces.CastleEntrance.class)
public abstract class SoulLavaFortressMixin {}
