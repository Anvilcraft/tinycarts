package mods.immibis.subworlds;

import mods.immibis.core.api.porting.SidedProxy;
import mods.immibis.subworlds.dw.DWEntity;
import mods.immibis.subworlds.dw.DWEntityRenderer;
import mods.immibis.subworlds.dw.DWManager;
import mods.immibis.subworlds.mws.MWSManager;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(version="0.0", dependencies="required-after:ImmibisCore", modid=SubWorldsMod.MODID, name="SubWorlds")
public class SubWorldsMod {
	
	public static final String MODID = "SubWorlds";
	
	@Instance(MODID)
	public static SubWorldsMod INSTANCE;
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onClientConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent evt) {
		ClientFakeEntities.reset();
	}
	
	@SideOnly(Side.CLIENT)
	public static class ClientProxy {
		public ClientProxy() {
			ClientFakeEntities.init();
			RenderingRegistry.registerEntityRenderingHandler(DWEntity.class, new DWEntityRenderer());
		}
	}
	
	@EventHandler
	public void init(FMLInitializationEvent evt) {
		DWManager.init();
		MWSManager.init();
		
		FMLCommonHandler.instance().bus().register(this);
		
		SidedProxy.instance.createSidedObject("mods.immibis.subworlds.SubWorldsMod$ClientProxy", null);
		EntityRegistry.registerModEntity(DWEntity.class, "detachedWorld", 0, this, 200, 5, true);
	}
}
