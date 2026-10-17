package com.simplebuilding.modules.simpletweaks;

import com.simplebuilding.modules.simpletweaks.admin.*;
import com.simplebuilding.modules.simpletweaks.admin.invsee.Invsee;

/** Fabric wiring of the admin features (menus, lifecycle, commands); ticks and joins come from shared mixins. */
final class AdminFabric {
    static void init() {
        Invsee.registerMenus();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(Admin::start);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(Admin::stop);
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d, r, e) -> AdminCommands.register(d));
    }
    private AdminFabric() {}
}
