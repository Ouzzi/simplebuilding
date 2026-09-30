package com.simplemoney;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.fml.loading.FMLPaths;
@Mod(value="simplemoney",dist=Dist.CLIENT)
public final class MoneyClient {
 public MoneyClient(ModContainer container) { container.registerExtensionPoint(IConfigScreenFactory.class,(mod,parent)->com.simplemoney.client.MoneyConfigScreen.create(parent,FMLPaths.CONFIGDIR.get())); }
}
