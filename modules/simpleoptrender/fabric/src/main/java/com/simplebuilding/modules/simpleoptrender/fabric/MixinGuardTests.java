package com.simplebuilding.modules.simpleoptrender.fabric;

import com.simplebuilding.framework.api.OptCompat;
import com.simplebuilding.modules.simpleoptrender.RenderOpt;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Concept §4.4: a mixin that does not apply switches its area off (MixinGuard), never crashes. Synthetic class nodes. */
public final class MixinGuardTests {
    private static final String PKG = "com.simplebuilding.modules.simpleoptrender.mixin.";

    private MixinGuardTests() {}

    private static ClassNode mixin(String... handlers) {
        ClassNode c = new ClassNode();
        c.name = "m/Mixin";
        for (String n : handlers) {
            MethodNode m = new MethodNode(Opcodes.ACC_PRIVATE, n, "()V", null, null);
            m.visibleAnnotations = List.of(new AnnotationNode("Lorg/spongepowered/asm/mixin/injection/Inject;"));
            c.methods.add(m);
        }
        MethodNode helper = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "simpleoptrender$helper", "()V", null, null);
        helper.visibleAnnotations = List.of(new AnnotationNode("Lorg/spongepowered/asm/mixin/Unique;"));
        c.methods.add(helper);
        return c;
    }

    private static ClassNode target(String... called) {
        ClassNode c = new ClassNode();
        c.name = "t/Target";
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        m.instructions = new InsnList();
        for (String n : called) m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "t/Target", n, "()V", false));
        c.methods.add(m);
        return c;
    }

    public static void missingHandlers(GameTestHelper h) {
        ClassNode m = mixin("simpleoptrender$a", "simpleoptrender$b");
        h.assertTrue(MixinGuard.missing(m, target("handler$zzz000$simpleoptrender$a", "simpleoptrender$b")).isEmpty(),
                "merged (renamed) and plain handlers count as applied; @Unique helpers are not injectors");
        h.assertTrue(MixinGuard.missing(m, target("handler$zzz000$simpleoptrender$a")).equals(List.of("simpleoptrender$b")),
                "handler nobody calls = injector did not match");
        h.assertTrue(MixinGuard.missing(m, target()).size() == 2, "nothing applied");
        h.succeed();
    }

    public static void failureSwitchesAreaOff(GameTestHelper h) {
        List<OptCompat.Decision> live = OptCompat.decisions(RenderOpt.MOD_ID);
        int depth = RenderOpt.leafDepth();
        var culling = RenderOpt.culling();
        var details = RenderOpt.details();
        try {
            var read = com.google.gson.JsonParser.parseString("{}").getAsJsonObject();
            RenderOpt.apply(com.simplebuilding.framework.api.OptSettings.of(read, RenderOpt.defaults()), true, Set.<String>of()::contains, Map.of());
            h.assertTrue(RenderOpt.active(RenderOpt.LEAF_CULLING), "on before the failure");
            String line = RenderOpt.RUNTIME.mixinFailed(PKG + "leaf_culling.ModelBlockRendererMixin");
            h.assertTrue(line != null && line.contains("leaf_culling") && line.contains("ModelBlockRendererMixin"), "log line names area and mixin");
            h.assertTrue(!RenderOpt.active(RenderOpt.LEAF_CULLING) && !RenderOpt.live(RenderOpt.LEAF_CULLING), "area off (decision and live switch) after a failed mixin");
            h.assertTrue(RenderOpt.live(RenderOpt.NO_TELEMETRY), "other areas unaffected");
        } finally {
            RenderOpt.restore(live, depth, culling, details);
        }
        h.succeed();
    }
}
