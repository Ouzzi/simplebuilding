package com.simplemobheads.client;

import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * The model of a Simple Mobheads look. Vanilla's skull code draws {@link #root()} with the look's render type (block,
 * item, worn head - all through {@code SkullBlockRenderer#submitSkull}); the {@link Pass}es come right after it (eyes,
 * clothes, wool, overlays), see {@code SkullBlockRendererMixin}. A tinted base is drawn as a pass, the root then stays
 * invisible (its cubes still give the GUI extents).
 */
public class MobHeadModel extends SkullModelBase {
    /** An extra pass: own model (same pose as the head), render type and tint (ARGB, -1 = none). */
    public record Pass(SkullModelBase model, RenderType renderType, int tint) {
    }

    private final List<Pass> passes;

    public MobHeadModel(ModelPart root, List<Pass> passes) {
        super(root);
        this.passes = List.copyOf(passes);
    }

    public List<Pass> passes() {
        return passes;
    }

    /** A plain model for a pass (no own passes). */
    public static final class Part extends SkullModelBase {
        public Part(ModelPart root) {
            super(root);
        }
    }
}
