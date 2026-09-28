package com.simplebuilding.compat.rei.neoforge;

import com.simplebuilding.compat.rei.SimplebuildingReiClientPlugin;
import me.shedaniel.rei.forge.REIPluginClient;

/**
 * NeoForge registration of {@link SimplebuildingReiClientPlugin}: REI's NeoForge loader finds client
 * plugins by this annotation, which only REI's NeoForge API has - so it sits on this subclass in a
 * NeoForge-only tree instead of on the shared class. Fabric uses the {@code rei_client} entrypoint.
 */
@REIPluginClient
public final class SimplebuildingReiNeoForgeClientPlugin extends SimplebuildingReiClientPlugin {
}
