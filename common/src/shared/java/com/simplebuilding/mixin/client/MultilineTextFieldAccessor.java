package com.simplebuilding.mixin.client;

import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Der Anker der Auswahl: {@code MultilineTextField.StringView} ist geschuetzt, der Blaupausen-
 * Editor rechnet die Auswahl deshalb selbst aus Cursor und diesem Anker (BlueprintCodeArea).
 */
@Mixin(MultilineTextField.class)
public interface MultilineTextFieldAccessor {
    @Accessor("selectCursor")
    int simplebuilding$selectCursor();
}
