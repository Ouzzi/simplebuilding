package com.simplebuilding.modules.simplequestionary;

import com.simplequestionary.client.QuestionaryScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Loaded only by JEI (entrypoint jei_mod_plugin). N60P: the Questionary book is wider than its menu image; telling JEI the book's area
 * keeps the ingredient list off the book, so the right page arrow is clickable with JEI installed (ui_probe run 2: the arrow click
 * opened JEI's RecipesGui).
 */
@JeiPlugin
public final class QuestionaryJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("simplequestionary", "jei");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(QuestionaryScreen.class, new IGuiContainerHandler<QuestionaryScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(QuestionaryScreen screen) {
                int[] a = screen.bookArea();
                return List.of(new Rect2i(a[0], a[1], a[2], a[3]));
            }
        });
    }
}
