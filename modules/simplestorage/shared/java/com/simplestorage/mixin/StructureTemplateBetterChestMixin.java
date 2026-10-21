package com.simplestorage.mixin;

import com.simplestorage.rare.BetterChests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bastion: template chests with a loot table in their NBT rarely become the Netherite Chest ({@link BetterChests}). */
@Mixin(StructureTemplate.class)
public abstract class StructureTemplateBetterChestMixin {
    @Inject(method = "processBlockInfos", at = @At("RETURN"), cancellable = true)
    private static void simplestorage$betterChests(ServerLevelAccessor level, BlockPos position, BlockPos referencePos,
                                                   StructurePlaceSettings settings, List<StructureTemplate.StructureBlockInfo> blockInfoList,
                                                   CallbackInfoReturnable<List<StructureTemplate.StructureBlockInfo>> cir) {
        List<StructureTemplate.StructureBlockInfo> processed = cir.getReturnValue();
        List<StructureTemplate.StructureBlockInfo> upgraded = BetterChests.upgradeTemplate(level, settings, processed);
        if (upgraded != processed) {
            cir.setReturnValue(upgraded);
        }
    }
}
