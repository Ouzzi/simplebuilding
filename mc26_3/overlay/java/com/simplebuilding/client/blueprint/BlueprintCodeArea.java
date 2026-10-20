package com.simplebuilding.client.blueprint;

import com.simplebuilding.blueprint.BlueprintCode;
import com.simplelib.api.client.ui.UiCodeArea;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * The code field of the blueprint editor on 26.3: the generic {@link UiCodeArea} of SimpleLib (moved there for the
 * Simple Music sheet editor, owner decision KONZ20 N1) with the blueprint palette ({@code BlueprintCode#STYLE_*}).
 * 26.2 has no SimpleLib and keeps its own copy in common/src/mc26_2.
 */
public final class BlueprintCodeArea extends UiCodeArea {
    public static final int[] STYLE_COLORS = {
            0xFF3B2F20, // TEXT
            0xFF7D7A5C, // COMMENT
            0xFF1F4E9A, // BLOCK
            0xFF6A7FA8, // NAMESPACE
            0xFF7A3E9D, // PROPERTY
            0xFF2E7D32, // VALUE
            0xFF00797A, // ALIAS
            0xFFA0421D, // NUMBER
            0xFF6B5B45, // OPERATOR
            0xFFC62828, // ERROR
            0xFF8D6E63, // AIR
    };

    public BlueprintCodeArea(Font font, int x, int y, int width, int height, float scale, boolean readOnly, String value,
                             Consumer<String> listener) {
        this(font, x, y, width, height, scale, readOnly, value, listener, BlueprintCode.MAX_CODE_LENGTH);
    }

    /** As above with its own character limit (import/export window: command lists are longer than a build code). */
    public BlueprintCodeArea(Font font, int x, int y, int width, int height, float scale, boolean readOnly, String value,
                             Consumer<String> listener, int characterLimit) {
        super(font, x, y, width, height, scale, readOnly, value, listener, characterLimit, STYLE_COLORS,
                Component.translatable("simplebuilding.blueprint.editor.code"));
    }

    public void setHighlight(byte[] styles, List<BlueprintCode.Problem> problems) {
        List<Range> ranges = new ArrayList<>(problems.size());
        for (BlueprintCode.Problem problem : problems) {
            ranges.add(new Range(problem.start(), problem.end()));
        }
        setProblems(styles, ranges);
    }
}
