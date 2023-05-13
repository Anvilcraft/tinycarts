package mods.immibis.subworlds.mws;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import mods.immibis.core.api.APILocator;
import mods.immibis.core.api.net.IPacket;
import mods.immibis.core.api.net.IPacketMap;
import mods.immibis.core.api.net.IPacketWrapper;
import mods.immibis.subworlds.mws.packets.*;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * MWS is Multi-World Sync. It handles sending worlds to clients that the player isn't in.
 * 
 * Each server world is associated with an MWSWorldManager, even if no clients are watching it.
 */
public class MWSManager {
	
	public static void debug(String s) {
		System.out.println("[SubWorlds MWS Debug] "+s);
	}
	
	public static final String CHANNEL = "ImmMWS";
	
	private static WeakHashMap<World, MWSWorldManager> managers = new WeakHashMap<World, MWSWorldManager>();
	
	/**
	 * Returns the MWSWorldManager that controls MWS for a given server world. 
	 */
	public static MWSWorldManager getWorldManager(World world) {
		if(world.isRemote)
			throw new IllegalArgumentException("Argument must be a server world.");
		
		MWSWorldManager m = managers.get(world);
		if(m == null)
			managers.put(world, m = new MWSWorldManager(world));
		return m;
	}
	
	@SideOnly(Side.CLIENT)
	private static Map<Integer, MWSClientWorld> clientWorlds;
	static {
		try {
			clientWorlds = new HashMap<Integer, MWSClientWorld>();
		} catch(NoSuchFieldError e) {
		}
	}
	
	/**
	 * Returns the MWSClientWorld for a given dimension ID, or null if none exists.
	 */
	@SideOnly(Side.CLIENT)
	public static MWSClientWorld getClientWorld(int dimID) {
		return clientWorlds.get(dimID);
	}
	
	
	
	@SideOnly(Side.CLIENT)
	public static void clientBegin(int dimID) {
		debug("MWS client start: "+dimID);
		clientWorlds.put(dimID, new MWSClientWorld(dimID));
	}
	
	@SideOnly(Side.CLIENT)
	public static void clientEnd(int dimID) {
		debug("MWS client end: "+dimID);
		MWSClientWorld w = clientWorlds.remove(dimID);
		if(w != null) {
			// is any cleanup necessary?
		}
	}
	
	@SideOnly(Side.CLIENT)
	public static Entity findClientEntity(int entID) {
		for(MWSClientWorld w : clientWorlds.values()) {
			Entity e = w.getEntityByID(entID);
			if(e != null)
				return e;
		}
		return null;
	}
	
	
	
	
	
	public static void init() {
		MinecraftForge.EVENT_BUS.register(new ForgeEventListener());
		APILocator.getNetManager().listen(new PacketMap());
		FMLCommonHandler.instance().bus().register(new FMLEventListener());
	}
	
	// must be public or Forge throws an exception
	public static class ForgeEventListener {
		@SubscribeEvent
		public void onWorldUnload(WorldEvent.Unload evt) {
			if(!evt.world.isRemote) {
				MWSWorldManager m = managers.get(evt.world);
				if(m != null)
					m.onWorldUnload();
			}
		}
	}
	
	public static final byte PKT_BEGIN = 0;
	public static final byte PKT_BLOCK = 1;
	public static final byte PKT_CHUNK = 2;
	public static final byte PKT_MULTIBLOCK = 3;
	public static final byte PKT_TILE = 4;
	public static final byte PKT_UNLOAD = 5;
	public static final byte PKT_END = 6;
	public static final byte PKT_SET_WORLD = 7;
	
	private static class PacketMap implements IPacketMap {

		@Override
		public String getChannel() {
			return CHANNEL;
		}

		@Override
		public IPacket createS2CPacket(byte id) {
			switch(id) {
			case PKT_BEGIN: return new PacketMWSBegin();
			case PKT_BLOCK: return new PacketMWSBlock();
			case PKT_CHUNK: return new PacketMWSChunk();
			case PKT_MULTIBLOCK: return new PacketMWSMultiBlock();
			case PKT_TILE: return new PacketMWSTile();
			case PKT_UNLOAD: return new PacketMWSUnload();
			case PKT_END: return new PacketMWSEnd();
			case PKT_SET_WORLD: return new PacketMWSSetWorld();
			default:
				return null;
			}
		}

		@Override
		public IPacket createC2SPacket(byte id) {
			return null;
		}
	}
	
	// must be public or forge crashes
	public static class FMLEventListener {
		@SubscribeEvent
		public void onServerTickEnd(TickEvent.ServerTickEvent evt) {
			if(evt.phase != TickEvent.Phase.END) return;
			for(MWSWorldManager m : managers.values())
				m.tick();
		}
		@SubscribeEvent
		@SideOnly(Side.CLIENT)
		public void onClientSideConnect(FMLNetworkEvent.ClientConnectedToServerEvent evt) {
			evt.manager.channel().pipeline().addBefore("packet_handler", "immibis subworlds mws subworld packet delayer congratulations if you are reading this far", new ChannelInboundHandlerAdapter() {
				boolean delayingMessages = false;
				int delayingMessagesForWorld;
				List<Object> delayedMessages = new ArrayList<>();
				@Override
				public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
					if(msg instanceof IPacketWrapper) {
						IPacket wp = ((IPacketWrapper)msg).packet;
						if(wp instanceof PacketMWSSetWorld) {
							int dim = ((PacketMWSSetWorld)wp).dim;
							if(delayingMessages && dim != delayingMessagesForWorld) {
								delayingMessages = false;
								delayedMessages.add(new IPacketWrapper(new PacketMWSSetWorld(PacketMWSSetWorld.NORMAL_DIM)));
								for(Object obj : delayedMessages)
									ctx.fireChannelRead(obj);
								delayedMessages.clear();
							}
							if(dim != PacketMWSSetWorld.NORMAL_DIM) {
								delayingMessages = true;
								delayingMessagesForWorld = dim;
								delayedMessages.add(msg);
							}
							return;
						}
					}
					if(delayingMessages)
						delayedMessages.add(msg);
					else
						ctx.fireChannelRead(msg);
				}
			});
		}
	}
}
