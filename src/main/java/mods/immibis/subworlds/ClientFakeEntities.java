package mods.immibis.subworlds;

import java.util.HashMap;
import java.util.Map;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

// maintains a list of fake entities on the client. there is no server equivalent.
// on the server, fake entities are updated and destroyed by whatever created them.
@SideOnly(Side.CLIENT)
public class ClientFakeEntities {
	private static Map<Integer, FakeEntity> ents = new HashMap<Integer, FakeEntity>();
	
	public static void debug(String s) {
		System.out.println("[SubWorlds CFE Debug] "+s);
	}
	
	public static void add(FakeEntity ent) {
		debug("Add CFE "+ent.entityID+": "+ent);
		ents.put(ent.entityID, ent);
	}
	
	public static FakeEntity remove(int entID) {
		debug("Remove CFE "+entID);
		return ents.remove(entID);
	}
	
	public static void reset() {
		debug("Reset CFEs");
		ents.clear();
	}
	
	public static FakeEntity get(int entID) {
		return ents.get(entID);
	}
	
	public static void init() {
		FMLCommonHandler.instance().bus().register(new ClientTickHandler());
		MinecraftForge.EVENT_BUS.register(new EventListener());
	}
	
	public static class ClientTickHandler {
		@SubscribeEvent
		public void onClientTickEnd(TickEvent.ClientTickEvent evt) {
			if(evt.phase != TickEvent.Phase.END) return;
			for(FakeEntity e : ents.values())
				e.tick();
		}
	}
	
	// must be public or forge throws errors
	public static class EventListener {
		@SubscribeEvent
		public void onRWL(RenderWorldLastEvent evt) {
			for(FakeEntity e : ents.values())
				e.renderBase(evt.partialTicks);
		}
	}
}
