package com.simplebuilding.modules.simplemodels.mixin;
import com.simplebuilding.modules.simplemodels.Models;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only explicit model requests intercept vanilla work. Other mods' recharge/repair remains intact. */
@Mixin(value = AnvilMenu.class, priority = 1100)
public abstract class ModelAnvilMixin extends ItemCombinerMenu {
    @Shadow private String itemName;
    @Shadow @Final private DataSlot cost;
    @Shadow private int repairItemCountCost;
    @Unique private boolean simplemodels$modelRequest;
    protected ModelAnvilMixin(MenuType<?> type, int id, Inventory inv, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slots) {
        super(type, id, inv, access, slots);
    }
    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void simplemodels$assign(CallbackInfo ci) {
        simplemodels$modelRequest = Models.request(itemName);
        if (!simplemodels$modelRequest) return;
        ci.cancel();
        var output = net.minecraft.world.item.ItemStack.EMPTY;
        if (player instanceof ServerPlayer sp && inputSlots.getItem(1).isEmpty())
            output = Models.assign(inputSlots.getItem(0), itemName,
                    net.minecraft.commands.Commands.LEVEL_GAMEMASTERS.check(sp.createCommandSourceStack().permissions()));
        resultSlots.setItem(0, output);
        cost.set(output.isEmpty() ? 0 : Models.server.policy().levelCost);
        repairItemCountCost = 0;
        broadcastChanges();
    }
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void simplemodels$revalidate(net.minecraft.world.entity.player.Player player, boolean hasItem,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (!simplemodels$modelRequest) return;
        var expected = player instanceof ServerPlayer sp && inputSlots.getItem(1).isEmpty()
                ? Models.assign(inputSlots.getItem(0), itemName, net.minecraft.commands.Commands.LEVEL_GAMEMASTERS.check(sp.createCommandSourceStack().permissions()))
                : net.minecraft.world.item.ItemStack.EMPTY;
        if (expected.isEmpty() || !net.minecraft.world.item.ItemStack.matches(expected, resultSlots.getItem(0))
                || cost.get() != Models.server.policy().levelCost) cir.setReturnValue(false);
    }
}
