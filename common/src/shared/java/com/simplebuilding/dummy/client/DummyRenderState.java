package com.simplebuilding.dummy.client;

import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import org.jspecify.annotations.Nullable;

/** Puppen-Zustand: die aufgeloeste Spielerhaut, wenn die Puppe wie ein Spieler heisst (sonst {@code null}). */
public class DummyRenderState extends ArmorStandRenderState {
    public PlayerSkinRenderCache.@Nullable RenderInfo skin;
}
