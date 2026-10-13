package com.simplemobheads.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplemobheads.MobHeadType;
import com.simplemobheads.SimpleMobHeads;
import com.simplemobheads.data.HeadDef;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Models and render types of the Simple Mobheads looks (concept §3.6). A look either names hand-made geometry
 * ({@link CustomHeadModels}) or a part of the mob's own entity model: the layer ({@code minecraft:cat#main}) is baked from
 * the {@link EntityModelSet}, the part ({@code head}, '' = the whole model, e.g. fish) is taken out, its rotation kept (or
 * the look's), and it is moved so that its bottom sits on the skull floor (y = 0) and its middle on the block's middle;
 * larger than {@value #MAX_SIZE} px it is scaled down (unless the look gives a scale). Extra passes (eyes, clothes, wool,
 * patterns) bake their own layer and get the very same pose, so they line up. Textures are the mob's real texture by
 * reference - resource packs that repaint a mob repaint its head too. A missing layer (mob of a missing mod) falls back to
 * a plain 8 px cube.
 */
public final class MobHeadModels {
    /** Largest edge of a head in model pixels before it is scaled down (Vanilla heads: 8, the dragon's 16). */
    public static final float MAX_SIZE = 12.0F;
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private MobHeadModels() {
    }

    public static Identifier texture(MobHeadType type) {
        return Identifier.parse(type.look().texture());
    }

    /** The render type the skull code draws a look's root with ({@code texture} = the item model's override, or null). */
    public static RenderType renderType(MobHeadType type, @Nullable Identifier texture) {
        HeadDef.Look look = type.look();
        Identifier tex = texture != null ? texture : texture(type);
        if (!look.custom().isEmpty()) {
            return RenderTypes.entityCutoutZOffset(tex);
        }
        return baseType(look.render(), tex);
    }

    private static RenderType baseType(String render, Identifier tex) {
        return switch (render) {
            case "translucent" -> RenderTypes.entityTranslucent(tex);
            case "emissive" -> RenderTypes.entityTranslucentEmissive(tex);
            default -> RenderTypes.entityCutout(tex);
        };
    }

    private static RenderType passType(String render, Identifier tex) {
        return switch (render) {
            case "eyes" -> RenderTypes.eyes(tex);
            case "breeze_eyes" -> RenderTypes.breezeEyes(tex);
            case "translucent" -> RenderTypes.entityTranslucent(tex);
            case "emissive" -> RenderTypes.entityTranslucentEmissive(tex);
            default -> RenderTypes.entityCutout(tex);
        };
    }

    /** ARGB of a tint ({@code dye:red}), -1 for none. */
    public static int tint(String tint) {
        if (tint.startsWith("dye:")) {
            DyeColor color = DyeColor.byName(tint.substring(4), null);
            return color == null ? -1 : color.getTextureDiffuseColor();
        }
        return -1;
    }

    public static SkullModelBase create(EntityModelSet models, MobHeadType type) {
        HeadDef.Look look = type.look();
        if (!look.custom().isEmpty()) {
            MobHeadModel custom = CustomHeadModels.create(look.custom(), texture(type));
            if (custom != null) {
                return custom;
            }
        }
        try {
            return generic(models, look);
        } catch (RuntimeException e) {
            if (WARNED.add(look.kind())) {
                SimpleMobHeads.LOG.warn("Simple Mobheads: no model for {} ({}#{}): {} - plain cube", look.kind(), look.layer(), look.part(), e.toString());
            }
            return new MobHeadModel(SkullModel.createMobHeadLayer().bakeRoot(), List.of());
        }
    }

    private static MobHeadModel generic(EntityModelSet models, HeadDef.Look look) {
        Set<String> hide = new HashSet<>(look.hide());
        ModelPart base = part(models, look.layer(), look.part(), hide);
        PartPose pose = pose(base, look, hide);
        apply(base, pose);
        ModelPart root = wrap(base);
        List<MobHeadModel.Pass> passes = new ArrayList<>();
        int tint = tint(look.tint());
        if (tint != -1) {
            // Vanilla's skull code draws the root untinted: the root only gives the extents, the tinted copy is a pass.
            root.visible = false;
            ModelPart copy = part(models, look.layer(), look.part(), hide);
            apply(copy, pose);
            passes.add(new MobHeadModel.Pass(new MobHeadModel.Part(wrap(copy)), baseType(look.render(), Identifier.parse(look.texture())), tint));
        }
        for (HeadDef.Pass pass : look.passes()) {
            ModelPart part = part(models, pass.layer(), pass.part(), hide);
            apply(part, pose);
            passes.add(new MobHeadModel.Pass(new MobHeadModel.Part(wrap(part)), passType(pass.render(), Identifier.parse(pass.texture())), tint(pass.tint())));
        }
        return new MobHeadModel(root, passes);
    }

    /** {@code namespace:path#layer} -> the baked part {@code name} ('' = the whole model) with the hidden parts switched off. */
    static ModelPart part(EntityModelSet models, String layer, String name, Set<String> hide) {
        int hash = layer.indexOf('#');
        ModelLayerLocation location = new ModelLayerLocation(Identifier.parse(hash < 0 ? layer : layer.substring(0, hash)),
                hash < 0 ? "main" : layer.substring(hash + 1));
        ModelPart root = models.bakeLayer(location);
        ModelPart part = name.isEmpty() ? root : find(root, name);
        if (part == null) {
            throw new IllegalArgumentException("no part '" + name + "' in " + location);
        }
        for (String hidden : hide) {
            ModelPart h = find(part, hidden);
            if (h != null) {
                h.visible = false;
            }
        }
        return part;
    }

    /** The first descendant called {@code name} (depth first, in model order), or null. */
    static @Nullable ModelPart find(ModelPart root, String name) {
        if (root.hasChild(name)) {
            return root.getChild(name);
        }
        List<String> paths = new ArrayList<>();
        root.visit(new PoseStack(), (pose, path, index, cube) -> paths.add(path));
        for (String path : paths) {
            String[] segments = path.split("/");
            ModelPart current = root;
            for (String segment : segments) {
                if (segment.isEmpty()) {
                    continue;
                }
                current = current.getChild(segment);
                if (segment.equals(name)) {
                    return current;
                }
            }
        }
        return null;
    }

    /** Where the part goes: bottom on the floor, middle on the block's middle, scaled down when too big. */
    static PartPose pose(ModelPart part, HeadDef.Look look, Set<String> hide) {
        PartPose initial = part.getInitialPose();
        float[] rot = look.rot();
        float rx = rot != null ? (float) Math.toRadians(rot[0]) : initial.xRot();
        float ry = rot != null ? (float) Math.toRadians(rot[1]) : initial.yRot();
        float rz = rot != null ? (float) Math.toRadians(rot[2]) : initial.zRot();
        apply(part, PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, rx, ry, rz));
        float[] b = bounds(part, hide);
        float size = Math.max(b[3] - b[0], Math.max(b[4] - b[1], b[5] - b[2]));
        float scale = look.scale() > 0.0F ? look.scale() : (size > MAX_SIZE ? MAX_SIZE / size : 1.0F);
        float cx = (b[0] + b[3]) / 2.0F;
        float cz = (b[2] + b[5]) / 2.0F;
        return PartPose.offsetAndRotation(-cx * scale, -b[4] * scale, -cz * scale, rx, ry, rz).withScale(scale);
    }

    private static void apply(ModelPart part, PartPose pose) {
        part.setInitialPose(pose);
        part.resetPose();
    }

    /** Bounds in model pixels {minX, minY, minZ, maxX, maxY, maxZ} of the visible cubes, in the part's current pose. */
    static float[] bounds(ModelPart part, Set<String> hide) {
        float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        part.visit(new PoseStack(), (pose, path, index, cube) -> {
            for (String segment : path.split("/")) {
                if (hide.contains(segment)) {
                    return;
                }
            }
            for (int corner = 0; corner < 8; corner++) {
                float x = (corner & 1) == 0 ? cube.minX : cube.maxX;
                float y = (corner & 2) == 0 ? cube.minY : cube.maxY;
                float z = (corner & 4) == 0 ? cube.minZ : cube.maxZ;
                Vector3f v = pose.pose().transformPosition(x / 16.0F, y / 16.0F, z / 16.0F, new Vector3f());
                b[0] = Math.min(b[0], v.x * 16.0F);
                b[1] = Math.min(b[1], v.y * 16.0F);
                b[2] = Math.min(b[2], v.z * 16.0F);
                b[3] = Math.max(b[3], v.x * 16.0F);
                b[4] = Math.max(b[4], v.y * 16.0F);
                b[5] = Math.max(b[5], v.z * 16.0F);
            }
        });
        if (b[0] > b[3]) {
            throw new IllegalArgumentException("no visible cubes");
        }
        return b;
    }

    private static ModelPart wrap(ModelPart head) {
        return new ModelPart(List.of(), Map.of("head", head));
    }
}
