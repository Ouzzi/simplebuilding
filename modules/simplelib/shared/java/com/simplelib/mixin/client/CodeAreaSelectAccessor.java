package com.simplelib.mixin.client;

import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The selection anchor of a {@link MultilineTextField}: its {@code StringView} is protected, so
 * {@link com.simplelib.api.client.ui.UiCodeArea} works the selection out from the cursor and this anchor.
 */
@Mixin(MultilineTextField.class)
public interface CodeAreaSelectAccessor {
    @Accessor("selectCursor")
    int simplelib$selectCursor();
}
