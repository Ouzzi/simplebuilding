package com.simplebuilding.modules.simpleoptrender.fabric;

import com.simplebuilding.modules.simpleoptrender.RenderOpt;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Concept §4.4: a mixin that does not apply must switch its area off, never crash the game. The config is not
 * {@code required} and uses {@code defaultRequire: 0} (a require of 1 or more makes Mixin throw an Error that nothing
 * can catch), so a missed injector is silent in Mixin itself. {@link #missing} finds it afterwards: every injector
 * handler of the mixin is merged into the target and called from it; a handler nobody calls did not match.
 * Failures Mixin does report (invalid mixin, missing target) arrive through {@link Handler}. Both end in
 * {@link com.simplebuilding.framework.api.OptAreas#mixinFailed} on {@link RenderOpt#RUNTIME}, so
 * every mixin of the area sees {@code RenderOpt.live} turn false.
 */
final class MixinGuard {
    private MixinGuard() {}

    /** Names of the injector handlers of {@code mixin} that the merged {@code target} never calls. */
    static List<String> missing(ClassNode mixin, ClassNode target) {
        List<String> out = new ArrayList<>();
        for (MethodNode m : mixin.methods) {
            if (!isInjector(m)) continue;
            if (!called(target, m.name)) out.add(m.name);
        }
        return out;
    }

    private static boolean isInjector(MethodNode m) {
        if (m.visibleAnnotations == null) return false;
        for (AnnotationNode a : m.visibleAnnotations)
            if (a.desc.startsWith("Lorg/spongepowered/asm/mixin/injection/") || a.desc.startsWith("Lcom/llamalad7/mixinextras/injector/")) return true;
        return false;
    }

    /** Mixin renames a merged handler to {@code handler$<id>$<name>}; both forms count. */
    private static boolean called(ClassNode target, String handler) {
        String tail = "$" + handler;
        for (MethodNode m : target.methods) {
            if (m.instructions == null) continue;
            for (var insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode call && matches(call.name, handler, tail)) return true;
                if (insn instanceof InvokeDynamicInsnNode dyn && dyn.bsmArgs != null)
                    for (Object arg : dyn.bsmArgs)
                        if (arg instanceof Handle h && matches(h.getName(), handler, tail)) return true;
            }
        }
        return false;
    }

    private static boolean matches(String name, String handler, String tail) {
        return name.equals(handler) || name.endsWith(tail);
    }

    /** Called by the plugin after a mixin was applied; logs and switches the area off if an injector did not match. */
    static void check(ClassNode target, String mixinClassName, IMixinInfo info) {
        try {
            List<String> gone = missing(info.getClassNode(0), target);
            if (gone.isEmpty()) return;
            String line = RenderOpt.RUNTIME.mixinFailed(mixinClassName);
            if (line != null) RenderOpt.LOG.warn("{} (handler {} not called in {})", line, gone, target.name);
        } catch (RuntimeException | LinkageError e) {
            RenderOpt.LOG.debug("mixin check skipped for {}", mixinClassName, e);
        }
    }

    /** Receives what Mixin itself reports for a non-required config: invalid mixin, unknown target. */
    public static final class Handler implements IMixinErrorHandler {
        @Override public ErrorAction onPrepareError(IMixinConfig config, Throwable th,
                                                     IMixinInfo mixin, ErrorAction action) {
            return failed(mixin, th, action);
        }

        @Override public ErrorAction onApplyError(String targetClassName, Throwable th, IMixinInfo mixin, ErrorAction action) {
            return failed(mixin, th, action);
        }

        private static ErrorAction failed(IMixinInfo mixin, Throwable th, ErrorAction action) {
            if (mixin != null && mixin.getClassName().startsWith(RenderOpt.class.getPackageName() + ".mixin.")) {
                String line = RenderOpt.RUNTIME.mixinFailed(mixin.getClassName());
                if (line != null) RenderOpt.LOG.warn("{} ({})", line, th.getMessage());
                return ErrorAction.WARN;
            }
            return action;
        }
    }
}
