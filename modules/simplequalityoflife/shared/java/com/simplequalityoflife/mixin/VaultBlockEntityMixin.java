package com.simplequalityoflife.mixin;

import com.mojang.serialization.Codec;
import com.simplequalityoflife.util.IVaultCooldown;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(VaultBlockEntity.class)
public abstract class VaultBlockEntityMixin extends BlockEntity {

    // During load the block entity has no level yet; getServerData() returns null then.
    @Shadow @org.spongepowered.asm.mixin.Final private VaultServerData serverData;

    // AUTHLIB_CODEC, nicht CODEC! AUTHLIB_CODEC ist das Mojang-Pendant zum alten Yarn-Uuids.CODEC
    // und schreibt Strings. UUIDUtil.CODEC schreibt ein Int-Array, und NBT-Map-Keys muessen Strings
    // sein - das Speichern schlaegt dann still fehl und die Cooldowns gehen beim Neuladen verloren.
    @Unique
    private static final Codec<Map<UUID, Long>> LOOT_TIMES_CODEC = Codec.unboundedMap(UUIDUtil.AUTHLIB_CODEC, Codec.LONG);

    public VaultBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // saveAdditional und loadAdditional bleiben hier (sind korrekt!)
    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void writeCustomData(ValueOutput view, CallbackInfo ci) {
        VaultServerData data = this.serverData;

        if (data instanceof IVaultCooldown cooldownData) {
            Map<UUID, Long> times = cooldownData.getLootTimesMap();

            if (times != null && !times.isEmpty()) {
                view.store("SimpleBuildingLootTimes", LOOT_TIMES_CODEC, times);
            }
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void readCustomData(ValueInput view, CallbackInfo ci) {
        VaultServerData data = this.serverData;

        if (data instanceof IVaultCooldown cooldownData) {
            view.read("SimpleBuildingLootTimes", LOOT_TIMES_CODEC).ifPresent(loadedMap -> {
                cooldownData.setLootTimesMap(loadedMap);
            });
        }
    }
}
