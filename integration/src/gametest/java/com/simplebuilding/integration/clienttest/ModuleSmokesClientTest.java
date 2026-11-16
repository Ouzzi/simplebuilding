package com.simplebuilding.integration.clienttest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * b-client60o: the collective run ({@code -PmoduleClientTest=vis24all}) runs every module client smoke through this one
 * entrypoint, so a red smoke no longer ends the run: the failure is logged ({@code [smokes] FAIL <class>}), the next smoke
 * starts from the title screen with default options, and at the end one error names every red smoke. The classes come from
 * {@code sbmodules-clienttest-smokes.txt}, written by integration/build.gradle (generateClientTestMetadata). A smoke that
 * leaves a world open cannot be recovered from; the run then stops there (with all failures so far).
 */
public final class ModuleSmokesClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        List<String> classes = smokes();
        List<String> failed = new ArrayList<>();
        boolean first = true;
        for (String name : classes) {
            if (!first) context.restoreDefaultGameOptions();
            first = false;
            System.out.println("[smokes] RUN " + name);
            try {
                FabricClientGameTest test = (FabricClientGameTest) Class.forName(name).getDeclaredConstructor().newInstance();
                test.runTest(context);
                System.out.println("[smokes] OK " + name);
            } catch (Throwable t) {
                if (t instanceof VirtualMachineError error) throw error;
                failed.add(name + ": " + t);
                System.out.println("[smokes] FAIL " + name + ": " + t);
                t.printStackTrace(System.out);
                boolean inWorld = context.computeOnClient(client -> client.level != null);
                if (inWorld) throw new AssertionError("Module smoke " + name + " failed and left its world open; stopped. Failed so far: " + failed, t);
            }
            context.runOnClient(client -> { if (!(client.gui.screen() instanceof TitleScreen)) client.gui.setScreen(new TitleScreen()); });
            context.waitTicks(1);
        }
        System.out.println("[smokes] " + (classes.size() - failed.size()) + "/" + classes.size() + " green");
        if (!failed.isEmpty()) throw new AssertionError(failed.size() + " of " + classes.size() + " client smokes failed: " + String.join(" | ", failed));
    }

    private static List<String> smokes() {
        try (InputStream in = ModuleSmokesClientTest.class.getResourceAsStream("/sbmodules-clienttest-smokes.txt")) {
            if (in == null) throw new AssertionError("sbmodules-clienttest-smokes.txt missing (integration/build.gradle writes it)");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().map(String::strip).filter(s -> !s.isEmpty()).toList();
        } catch (IOException e) {
            throw new AssertionError("sbmodules-clienttest-smokes.txt unreadable", e);
        }
    }
}
