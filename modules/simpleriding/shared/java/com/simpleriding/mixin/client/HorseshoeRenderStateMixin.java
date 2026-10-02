package com.simpleriding.mixin.client;

import com.simpleriding.client.HorseshoeState;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import org.spongepowered.asm.mixin.*;

@Mixin(EquineRenderState.class)
public abstract class HorseshoeRenderStateMixin implements HorseshoeState {
 @Unique private int simpleriding$shoes;
 @Override public int simpleriding$shoes(){return simpleriding$shoes;}
 @Override public void simpleriding$setShoes(int code){simpleriding$shoes=code;}
}
