package mods.immibis.subworlds.dw;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mods.immibis.core.api.APILocator;
import mods.immibis.core.api.net.IPacket;
import mods.immibis.core.api.net.IPacketMap;
import mods.immibis.core.api.util.NBTType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public class DWManager {
	
	// Note: DimensionManager isn't thread safe.
	// We avoid calling it from the client thread when the integrated server is running.

	public static final int PROVIDER_TYPE_ID = "SpaceCraftMod".hashCode();
	
	public static final String CHANNEL = "SpaceCraftDW";
	public static final byte PKT_DIMENSION_LIST = 0;
	
	private static Map<Integer, WorldProps> registeredDimensions = new HashMap<Integer, WorldProps>();
	
	private static SaveSpecificData getSaveData() {
		MapStorage ms = DimensionManager.getWorld(0).mapStorage;
		SaveSpecificData ssd = (SaveSpecificData)ms.loadData(SaveSpecificData.class, "subworlds-dw-save-data");
		if(ssd == null)
			ms.setData("subworlds-dw-save-data", ssd = new SaveSpecificData("subworlds-dw-save-data"));
		return ssd;
	}
	
	/**
	 * Creates a new empty dimension for Detached World use,
	 * returns the dimension ID.
	 * only called on the server
	 */
	public static int createWorld(WorldProps props) {
		int dim = DimensionManager.getNextFreeDimId();
		
		System.out.println("DWManager: creating dimension #"+dim);
		
		registerDimensions(Collections.singletonMap(dim, props), false);
		
		MinecraftServer.getServer().worldServerForDimension(dim);
		System.out.println("DWManager: created dimension #"+dim);
		return dim;
	}
	public static int createWorld() {
		return createWorld(new WorldProps());
	}
	
	public static void init() {
		DimensionManager.registerProviderType(PROVIDER_TYPE_ID, DWWorldProvider.class, false);
		MinecraftForge.EVENT_BUS.register(new EventHandler());
		FMLCommonHandler.instance().bus().register(new ConnectionHandler());
		APILocator.getNetManager().listen(new PacketMap());
		new ChunkLoadingCallback();
	}
	
	private static class PacketMap implements IPacketMap {

		@Override
		public String getChannel() {
			return CHANNEL;
		}

		@Override
		public IPacket createS2CPacket(byte id) {
			if(id == PKT_DIMENSION_LIST)
				return new PacketDWDimensionList();
			return null;
		}

		@Override
		public IPacket createC2SPacket(byte id) {
			return null;
		}
		
	}
	
	public static class ConnectionHandler {
		@SubscribeEvent
		public String connectionReceived(FMLNetworkEvent.ServerConnectionFromClientEvent evt) {
			PacketDWDimensionList packet = new PacketDWDimensionList();
			packet.clearExisting = true;
			packet.data = registeredDimensions;
			APILocator.getNetManager().send(packet, evt.manager, true);
			return null;
		}
	}
	
	// only called on the server, unless there is no server
	@SuppressWarnings("unchecked")
	static void registerDimensions(Map<Integer, WorldProps> dimIDs, boolean clearExisting) {
		if(clearExisting) {
			for(int i : registeredDimensions.keySet())
				DimensionManager.unregisterDimension(i);
			registeredDimensions.clear();
		}
		SaveSpecificData sd = MinecraftServer.getServer() == null ? null : getSaveData();
		for(int i : dimIDs.keySet()) {
			DimensionManager.registerDimension(i, PROVIDER_TYPE_ID);
			registeredDimensions.put(i, dimIDs.get(i));
			
			if(sd != null && !sd.dw_worlds.containsKey(i)) {
				sd.dw_worlds.put(i, dimIDs.get(i));
				sd.setDirty(true);
			}
		}
		
		if(sd != null)
		{
			PacketDWDimensionList packet = new PacketDWDimensionList();
			packet.clearExisting = clearExisting;
			packet.data = dimIDs;
			for(EntityPlayer pl : (List<EntityPlayer>)MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
				APILocator.getNetManager().sendToClient(packet, pl);
			}
		}
		
		
		
	}
	
	// must be public or Forge crashes
	public static class EventHandler {
		@SubscribeEvent
		public void onWorldLoad(WorldEvent.Load evt) {
			if(!evt.world.isRemote && evt.world.provider.dimensionId == 0) {
				registerDimensions(getSaveData().dw_worlds, true);
			}
			if(!evt.world.isRemote)
				evt.world.addWorldAccess(new WorldListener(evt.world));
		}
		
		@SubscribeEvent
		public void onWorldUnload(WorldEvent.Unload evt) {
			if(!evt.world.isRemote && evt.world.provider.dimensionId == 0) {
				registerDimensions(Collections.<Integer, WorldProps>emptyMap(), true);
			}
		}
	}
	
	// must be public as instances are created by reflection
	public static class SaveSpecificData extends WorldSavedData {
		public SaveSpecificData(String par1Str) {
			super(par1Str);
		}
		
		public Map<Integer, WorldProps> dw_worlds = new HashMap<Integer, WorldProps>();

		@Override
		public void readFromNBT(NBTTagCompound var1) {
			if(var1.hasKey("dw_worlds")) {
				NBTTagList t = var1.getTagList("dw_worlds", NBTType.COMPOUND);
				for(int k = 0; k < t.tagCount(); k++)
				{
					WorldProps props = new WorldProps();
					NBTTagCompound tag = t.getCompoundTagAt(k);
					int dim = tag.getInteger("dim");
					props.read(tag);
					dw_worlds.put(dim, props);
				}
			}
		}

		@Override
		public void writeToNBT(NBTTagCompound var1) {
			NBTTagList t = new NBTTagList();
			for(Map.Entry<Integer, WorldProps> i : dw_worlds.entrySet()) {
				NBTTagCompound c = new NBTTagCompound();
				c.setInteger("dim", i.getKey());
				i.getValue().write(c);
				t.appendTag(c);
			}
			var1.setTag("dw_worlds", t);
		}
		
	}
	
	private static class WorldListener implements IWorldAccess {
		//public final World worldObj;
		public WorldListener(World world) {
			//this.worldObj = world;
		}
		
		@Override
		public void onEntityDestroy(Entity var1) {
			if(var1 instanceof DWEntity)
				((DWEntity)var1).onUnloadOrDestroy();
		}
		
		@Override public void markBlockForUpdate(int var1, int var2, int var3) {}
		@Override public void markBlockForRenderUpdate(int var1, int var2, int var3) {}
		@Override public void markBlockRangeForRenderUpdate(int var1, int var2, int var3, int var4, int var5, int var6) {}
		@Override public void playSound(String var1, double var2, double var4, double var6, float var8, float var9) {}
		@Override public void playSoundToNearExcept(EntityPlayer var1, String var2, double var3, double var5, double var7, float var9, float var10) {}
		@Override public void spawnParticle(String var1, double var2, double var4, double var6, double var8, double var10, double var12) {}
		@Override public void onEntityCreate(Entity var1) {}
		@Override public void playRecord(String var1, int var2, int var3, int var4) {}
		@Override public void broadcastSound(int var1, int var2, int var3, int var4, int var5) {}
		@Override public void playAuxSFX(EntityPlayer var1, int var2, int var3, int var4, int var5, int var6) {}
		@Override public void destroyBlockPartially(int var1, int var2, int var3, int var4, int var5) {}
		@Override public void onStaticEntitiesChanged() {}
	}

	public static WorldProps getProps(int dim) {
		WorldProps p = registeredDimensions.get(dim);
		if(p == null)
			return new WorldProps(); // probably on client. TODO verify correct
		return p;
	}

}
