package com.simplestorage.mixin;

import com.simplestorage.bundle.PlacedStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A falling set-down bag (B23-M12, {@link PlacedStorage}) that cannot land drops what it holds - the bundle, quiver or
 * backpack with all its contents, rebuilt from the carried block entity data - instead of Vanilla's plain block item
 * (none for the placed bundle, an empty backpack for a backpack). Every other falling block drops as in Vanilla.
 */
@Mixin(FallingBlockEntity.class)
public abstract class FallingStorageDropMixin {
    @Shadow private BlockState blockState;
    @Shadow public CompoundTag blockData;

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity simplestorage$dropStored(FallingBlockEntity self, ServerLevel level, ItemLike item) {
        if (PlacedStorage.isFalling(this.blockState.getBlock())) {
            ItemStack stored = PlacedStorage.fallenStack(level, this.blockState, this.blockData);
            return stored.isEmpty() ? null : self.spawnAtLocation(level, stored);
        }
        return self.spawnAtLocation(level, item);
    }
}
