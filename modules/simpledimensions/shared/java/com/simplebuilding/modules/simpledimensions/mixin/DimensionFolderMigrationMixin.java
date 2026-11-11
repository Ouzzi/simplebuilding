package com.simplebuilding.modules.simpledimensions.mixin;
import java.nio.file.Path;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** WD25-14: the region folder of an old world ({@code dimensions/simpledimension/<id>}) moves to the new id before the level opens it. */
@Mixin(DimensionType.class)
public abstract class DimensionFolderMigrationMixin {
 @Inject(method="getStorageFolder",at=@At("HEAD"))
 private static void simpledimensions$moveFolder(ResourceKey<Level> name,Path baseFolder,CallbackInfoReturnable<Path> cir){
  com.simplebuilding.modules.simpledimensions.LegacyIds.moveDimensionFolder(name.identifier(),baseFolder);
 }
}
