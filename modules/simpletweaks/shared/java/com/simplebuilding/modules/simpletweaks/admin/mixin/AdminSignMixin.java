package com.simplebuilding.modules.simpletweaks.admin.mixin;

import com.simplebuilding.modules.simpletweaks.admin.blocklog.BlockRecorder;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Block log (S): a player edited the text of a sign; old text before, new text after the edit. */
@Mixin(SignBlockEntity.class)
public abstract class AdminSignMixin {
    @Unique private String simpletweaks$before;

    @Inject(method = "updateSignText", at = @At("HEAD"))
    private void simpletweaks$beforeEdit(Player player, SignTextSlot slot, List<FilteredText> lines, CallbackInfo ci) {
        simpletweaks$before = player instanceof ServerPlayer && BlockRecorder.signs() ? BlockRecorder.signText((SignBlockEntity) (Object) this, slot == SignTextSlot.FRONT) : null;
    }

    @Inject(method = "updateSignText", at = @At("RETURN"))
    private void simpletweaks$afterEdit(Player player, SignTextSlot slot, List<FilteredText> lines, CallbackInfo ci) {
        var before = simpletweaks$before;
        simpletweaks$before = null;
        if (player instanceof ServerPlayer p) BlockRecorder.signEdited(p, (SignBlockEntity) (Object) this, slot == SignTextSlot.FRONT, before);
    }
}
