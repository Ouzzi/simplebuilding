package com.simplevisuals.mixin;
import com.simplevisuals.client.VisualsHud;
import net.minecraft.client.gui.*;import net.minecraft.client.DeltaTracker;import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Hud.class)
public abstract class HudMixin {
 @Shadow private int toolHighlightTimer; @Shadow private ItemStack lastToolHighlight;
 @Inject(method="extractRenderState",at=@At("TAIL")) private void visuals$hud(GuiGraphicsExtractor g,DeltaTracker d,CallbackInfo ci){VisualsHud.render(g);}
 @Inject(method="extractSelectedItemName",at=@At("TAIL")) private void visuals$held(GuiGraphicsExtractor g,CallbackInfo ci){VisualsHud.held(g,lastToolHighlight,toolHighlightTimer);}
 @Inject(method="tick()V",at=@At("HEAD")) private void visuals$enchants(CallbackInfo ci){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player!=null&&lastToolHighlight!=null&&!ItemStack.isSameItemSameComponents(mc.player.getMainHandItem(),lastToolHighlight)&&mc.player.getMainHandItem().is(lastToolHighlight.getItem())){toolHighlightTimer=40;lastToolHighlight=mc.player.getMainHandItem().copy();}}
}
