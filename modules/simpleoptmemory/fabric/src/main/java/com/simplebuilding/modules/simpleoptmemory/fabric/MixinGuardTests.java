package com.simplebuilding.modules.simpleoptmemory.fabric;

import com.simplebuilding.framework.api.OptCompat;
import com.simplebuilding.modules.simpleoptmemory.MemoryOpt;
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
    private static final String PKG = "com.simplebuilding.modules.simpleoptmemory.mixin.";

    private MixinGuardTests() {}

    private static ClassNode mixin(String... handlers) {
        ClassNode c = new ClassNode();
        c.name = "m/Mixin";
        for (String n : handlers) {
            MethodNode m = new MethodNode(Opcodes.ACC_PRIVATE, n, "()V", null, null);
            m.visibleAnnotations = List.of(new AnnotationNode("Lorg/spongepowered/asm/mixin/injection/Inject;"));
            c.methods.add(m);
        }
        MethodNode helper = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "simpleoptmemory$helper", "()V", null, null);
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
        ClassNode m = mixin("simpleoptmemory$a", "simpleoptmemory$b");
        h.assertTrue(MixinGuard.missing(m, target("handler$zzz000$simpleoptmemory$a", "simpleoptmemory$b")).isEmpty(),
                "merged (renamed) and plain handlers count as applied; @Unique helpers are not injectors");
        h.assertTrue(MixinGuard.missing(m, target("handler$zzz000$simpleoptmemory$a")).equals(List.of("simpleoptmemory$b")),
                "handler nobody calls = injector did not match");
        h.assertTrue(MixinGuard.missing(m, target()).size() == 2, "nothing applied");
        h.succeed();
    }

    public static void failureSwitchesAreaOff(GameTestHelper h) {
        List<OptCompat.Decision> live = OptCompat.decisions(MemoryOpt.MOD_ID);
        try {
            var read = com.google.gson.JsonParser.parseString("{}").getAsJsonObject();
            MemoryOpt.apply(com.simplebuilding.framework.api.OptSettings.of(read, MemoryOpt.defaults()), true, Set.<String>of()::contains, Map.of());
            h.assertTrue(MemoryOpt.live(MemoryOpt.STATE_TABLES), "on before the failure");
            String line = MemoryOpt.RUNTIME.mixinFailed(PKG + "state_tables.StateHolderMixin");
            h.assertTrue(line != null && line.contains("state_tables") && line.contains("StateHolderMixin"), "log line names area and mixin");
            h.assertTrue(!MemoryOpt.live(MemoryOpt.STATE_TABLES), "area off (decision and live switch) after a failed mixin");
            var now = OptCompat.decisions(MemoryOpt.MOD_ID).getFirst();
            h.assertTrue(!now.active(), "status (OptCompat table, /simpleopt status) shows the area as off");
        } finally {
            MemoryOpt.RUNTIME.publish(live);
        }
        h.succeed();
    }
}
