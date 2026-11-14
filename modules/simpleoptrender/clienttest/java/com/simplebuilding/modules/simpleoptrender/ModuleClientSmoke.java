package com.simplebuilding.modules.simpleoptrender;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
/** Boots on the client and decided its areas in the mixin plugin (all on unless a covering mod or the config says otherwise). */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simpleoptrender")) throw new AssertionError("Module did not boot");
        if (RenderOpt.decidedAreas() != RenderOpt.AREAS.size())
            throw new AssertionError("Areas not decided at startup");
        // simpleoptrender-3 (PLAN40): with the area live, the client's telemetry manager hands out Vanilla's DISABLED sender: no session sender, nothing is sent.
        if (RenderOpt.live(RenderOpt.NO_TELEMETRY)) {
            boolean disabled = context.computeOnClient(mc ->
                    mc.getTelemetryManager().getOutsideSessionSender() == net.minecraft.client.telemetry.TelemetryEventSender.DISABLED);
            if (!disabled) throw new AssertionError("Telemetry sender is active although no_telemetry is live");
        }
        context.takeScreenshot("simpleoptrender-title");
    }
}
