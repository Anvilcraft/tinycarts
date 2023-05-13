package mods.immibis.subworlds.mws;



import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import mods.immibis.core.api.APILocator;
import mods.immibis.subworlds.dw.DWWorldProvider;
import mods.immibis.subworlds.mws.packets.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * Holds MWS state for one world.
 */
public class MWSWorldManager {
	private final WeakReference<World> worldObj;
	private final int dimensionID; // if world is unloaded, we need to know the dimension number to inform clients
	
	private final int VIEW_DISTANCE = MinecraftServer.getServer().getConfigurationManager().getViewDistance();
	
	MWSWorldManager(World world) {
		this.worldObj = new WeakReference<World>(world);
		this.dimensionID = world.provider.dimensionId;
		world.addWorldAccess(new WorldListener());
	}
	
	private class Range {
		// inclusive min, exclusive max
		public final int minx, miny, minz, maxx, maxy, maxz;
		public Range(int minx, int miny, int minz, int maxx, int maxy, int maxz) {
			this.minx = minx;
			this.miny = miny;
			this.minz = minz;
			this.maxx = maxx;
			this.maxy = maxy;
			this.maxz = maxz;
		}
		
		public int getBlockCount() {
			return (maxx - minx) * (maxy - miny) * (maxz - minz);
		}
	}
	
	private Set<MWSListener> listeners = new HashSet<MWSListener>();
	
	private class SyncedChunk {
		public final int x, z;
		
		public SyncedChunk(int x, int z) {
			this.x = x;
			this.z = z;
		}
		
		private Range[] updateRanges = new Range[8];
		private int nUpdateRanges;
		private int nUpdatedBlocks;
		
		//public Set<MWSListener> listeners = new HashSet<MWSListener>();
		
		public void addRange(Range r) {
			if(nUpdateRanges < updateRanges.length) {
				updateRanges[nUpdateRanges] = r;
				nUpdatedBlocks += r.getBlockCount();
			}
			nUpdateRanges++;
		}

		@SuppressWarnings("unchecked")
		public Collection<Packet> nextUpdatePackets() {
			if(nUpdateRanges == 0)
				return null;
			
			World world = worldObj.get();
			if(world == null)
				return null;
			
			Collection<Packet> rv = null;
			
			if(nUpdatedBlocks == 1) {
				int x = updateRanges[0].minx + (this.x << 4);
				int y = updateRanges[0].miny;
				int z = updateRanges[0].minz + (this.z << 4);
				
				updateRanges[0] = null;
				
				Packet blockPacket = APILocator.getNetManager().wrap(new PacketMWSBlock(x, y, z, world), true);
				
				TileEntity te = world.getTileEntity(x, y, z);
				Packet tedesc = te == null ? null : te.getDescriptionPacket();
				if(tedesc != null)
					rv = Arrays.asList(APILocator.getNetManager().wrap(new PacketMWSTile(world, tedesc), true), blockPacket);
				else
					rv = Arrays.asList(blockPacket);
				
			} else if(nUpdatedBlocks < 64 && nUpdateRanges <= updateRanges.length) {
				PacketMWSMultiBlock mbp = new PacketMWSMultiBlock(this.x, this.z, nUpdatedBlocks, world);
				rv = new ArrayList<Packet>(8);
				rv.add(APILocator.getNetManager().wrap(mbp, true));
				
				//Chunk c = world.getChunkFromChunkCoords(this.x, this.z);
				
				for(int k = 0; k < nUpdateRanges; k++) {
					Range r = updateRanges[k];
					updateRanges[k] = null;
					
					for(int x_ = r.minx; x_ < r.maxx; x_++)
					for(int y = r.miny; y < r.maxy; y++)
					for(int z_ = r.minz; z_ < r.maxz; z_++) {
						int x = x_ + (this.x << 4);
						int z = z_ + (this.z << 4);
						
						mbp.addBlock(x, y, z, world);
						
						TileEntity te = world.getTileEntity(x, y, z);
						if(te != null) {
							Packet teDesc = te.getDescriptionPacket();
							if(teDesc != null)
								rv.add(APILocator.getNetManager().wrap(new PacketMWSTile(world, teDesc), true));
						}
					}
				}
				
			} else {
				Chunk c = world.getChunkFromChunkCoords(this.x, this.z);
				rv = new ArrayList<Packet>(16);
				rv.add(APILocator.getNetManager().wrap(new PacketMWSChunk(this.x, this.z, world, c), true));
				for(TileEntity te : (Collection<TileEntity>)c.chunkTileEntityMap.values()) {
					Packet teDesc = te.getDescriptionPacket();
					if(teDesc != null)
						rv.add(APILocator.getNetManager().wrap(new PacketMWSTile(world, teDesc), true));
				}
			}
			
			nUpdateRanges = 0;
			nUpdatedBlocks = 0;
			
			return rv;
		}
	}
	
	private Set<SyncedChunk> changedChunks = new HashSet<SyncedChunk>();
	private Set<SyncedChunk> allChunks = new HashSet<SyncedChunk>();
	private LongHashMap chunks = new LongHashMap();
	
	private SyncedChunk getSyncedChunk(int x, int z, boolean markForUpdate) {
		long index = ChunkCoordIntPair.chunkXZ2Int(x, z);
		SyncedChunk s = (SyncedChunk)chunks.getValueByKey(index);
		if(s == null) {
			s = new SyncedChunk(x, z);
			chunks.add(index, s);
			allChunks.add(s);
		}
		if(markForUpdate)
			synchronized(changedChunks) {
				changedChunks.add(s);
			}
		return s;
	}
	
	private class WorldListener implements IWorldAccess {

		@Override
		public void markBlockForUpdate(int var1, int var2, int var3) {
			getSyncedChunk(var1 >> 4, var3 >> 4, true).addRange(new Range(var1&15, var2, var3&15, var1&15, var2, var3&15));
		}

		@Override
		public void markBlockForRenderUpdate(int var1, int var2, int var3) {}

		@Override
		public void markBlockRangeForRenderUpdate(int var1, int var2, int var3,int var4, int var5, int var6) {
			int mincx = var1 >> 4;
			int mincz = var3 >> 4;
			int maxcx = var4 >> 4;
			int maxcz = var6 >> 4;
			for(int cx = mincx; cx <= maxcx; cx++) {
				int minx = (cx == mincx ? var1 & 15 : 0);
				int maxx = (cx == maxcx ? (var4 & 15) + 1 : 16);
				for(int cz = mincz; cz <= maxcz; cz++) {
					int minz = (cz == mincz ? var3 & 15 : 0);
					int maxz = (cz == maxcz ? (var6 & 15) + 1 : 16);
					
					getSyncedChunk(cx, cz, true).addRange(new Range(minx, var2, minz, maxx, var5, maxz));
				}
			}
		}

		@Override
		public void playSound(String var1, double var2, double var4, double var6, float var8, float var9) {
		}

		@Override
		public void spawnParticle(String var1, double var2, double var4, double var6, double var8, double var10, double var12) {
		}

		@Override
		public void onEntityCreate(Entity var1) {
		}

		@Override
		public void onEntityDestroy(Entity var1) {
		}

		@Override
		public void playRecord(String var1, int var2, int var3, int var4) {
		}

		@Override
		public void playAuxSFX(EntityPlayer var1, int var2, int var3, int var4, int var5, int var6) {
		}

		@Override
		public void destroyBlockPartially(int var1, int var2, int var3, int var4, int var5) {
		}

		@Override
		public void playSoundToNearExcept(EntityPlayer var1, String var2, double var3, double var5, double var7, float var9, float var10) {
		}

		@Override
		public void broadcastSound(int var1, int var2, int var3, int var4, int var5) {
		}

		@Override
		public void onStaticEntitiesChanged() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	/*private boolean isInViewingRange(SyncedChunk sc, MWSListener w) {
		int minx = (sc.x - VIEW_DISTANCE) * 16;
		int maxx = (sc.x + VIEW_DISTANCE + 1) * 16;
		int minz = (sc.z - VIEW_DISTANCE) * 16;
		int maxz = (sc.z + VIEW_DISTANCE + 1) * 16;
		return w.x >= minx && w.z >= minz && w.x <= maxx && w.z <= maxz; 
	}*/
	
	private void sendUpdate(SyncedChunk sc) {
		Collection<Packet> packets = sc.nextUpdatePackets();
		if(packets == null)
			return;
		
		int minx = (sc.x - VIEW_DISTANCE) * 16;
		int maxx = (sc.x + VIEW_DISTANCE + 1) * 16;
		int minz = (sc.z - VIEW_DISTANCE) * 16;
		int maxz = (sc.z + VIEW_DISTANCE + 1) * 16;
		
		for(MWSListener w : listeners) {
			if(w.x >= minx && w.x <= maxx && w.z >= minz && w.z <= maxz) {
				for(Packet p : packets) {
					w.client.scheduleOutboundPacket(p);
				}
			}
		}
	}
	
	public void tick() {
		for(MWSListener l : new ArrayList<MWSListener>(listeners)) {
			l.update();
			if(l.isDead)
				listeners.remove(l);
			
			if(l.ticksToNextRangeCheck <= 0) {
				l.ticksToNextRangeCheck = l.RANGE_CHECK_INTERVAL;
				updateLoadedChunks(l);
			} else
				l.ticksToNextRangeCheck--;
		}
		
		synchronized(changedChunks) {
			if(!changedChunks.isEmpty()) {
				Collection<SyncedChunk> copy = new ArrayList<SyncedChunk>(changedChunks);
				changedChunks.clear();
				for(SyncedChunk sc : copy)
					sendUpdate(sc);
			}
		}
		
		final int MAX_CHUNKS_PER_TICK = 5;
		
		int nSent = 0;
		
		while(sendQueue.size() > 0) {
			SendQueueEntry e = sendQueue.poll();
			if(!listeners.contains(e.l))
				continue;
			
			//if(SubWorldsMod.sendQueueThrottle(e.l.client))
			//	break;
			
			//System.out.println("sending "+e.hashCode()+": "+e.cx+","+e.cz+" to "+e.l);
			
			e.send();
			nSent++;
			if(nSent >= MAX_CHUNKS_PER_TICK)
				break;
		}
	}
	
	private static class SendQueueEntry {
		public MWSListener l;
		public int cx, cz;
		public World world;
		public Chunk c;
		
		public SendQueueEntry(MWSListener l, int cx, int cz, World world, Chunk c) {
			this.c = c;
			this.cx = cx;
			this.cz = cz;
			this.world = world;
			this.l = l;
		}
		
		@SuppressWarnings("unchecked")
		public void send() {
			APILocator.getNetManager().send(new PacketMWSChunk(cx, cz, world, c), l.client, true);
			
			//APILocator.getNetManager().send(new PacketMWSSetWorld(world.provider.dimensionId), l.client, true);
			for(TileEntity te : (Collection<TileEntity>)c.chunkTileEntityMap.values()) {
				Packet teDesc = te.getDescriptionPacket();
				if(teDesc != null)
					l.client.scheduleOutboundPacket(APILocator.getNetManager().wrap(new PacketMWSTile(world, teDesc), true));
					//l.client.scheduleOutboundPacket(teDesc);
			}
			//APILocator.getNetManager().send(new PacketMWSSetWorld(PacketMWSSetWorld.NORMAL_DIM), l.client, true);
		}
	}
	
	private Queue<SendQueueEntry> sendQueue = new LinkedList<SendQueueEntry>();
	
	public void addListener(MWSListener l) {
		World world = worldObj.get();
		if(world == null)
			return;
		
		l.update();
		
		if(l.isDead)
			return;
		
		if(!listeners.add(l))
			return;
		
		int minx, minz, maxx, maxz;
		
		if(world.provider instanceof DWWorldProvider) {
			minx = minz = 0;
			maxx = ((DWWorldProvider)world.provider).props.xsize >> 4;
			maxz = ((DWWorldProvider)world.provider).props.zsize >> 4;
		} else {
			minx = (l.x>>4) - VIEW_DISTANCE;
			maxx = (l.x>>4) + VIEW_DISTANCE + 1;
			minz = (l.z>>4) - VIEW_DISTANCE;
			maxz = (l.z>>4) + VIEW_DISTANCE + 1;
		}
		
		l.client.scheduleOutboundPacket(APILocator.getNetManager().wrap(new PacketMWSBegin(world.provider.dimensionId), true));
		
		for(int x = minx; x <= maxx; x++)
		for(int z = minz; z <= maxz; z++) {
			sendQueue.add(new SendQueueEntry(l, x, z, world, world.getChunkFromChunkCoords(x, z)));
		}
	}
	
	private void updateLoadedChunks(MWSListener l) {
		int minx, minz, maxx, maxz;
		
		World world = worldObj.get();
		if(world == null)
			return;
		
		if(world.provider instanceof DWWorldProvider) {
			minx = minz = 0;
			maxx = ((DWWorldProvider)world.provider).props.xsize >> 4;
			maxz = ((DWWorldProvider)world.provider).props.zsize >> 4;
		} else {
			minx = (l.x>>4) - VIEW_DISTANCE;
			maxx = (l.x>>4) + VIEW_DISTANCE + 1;
			minz = (l.z>>4) - VIEW_DISTANCE;
			maxz = (l.z>>4) + VIEW_DISTANCE + 1;
		}
		
		Set<ChunkCoordIntPair> targetChunks = new HashSet<ChunkCoordIntPair>();
		
		for(int x = minx; x <= maxx; x++)
		for(int z = minz; z <= maxz; z++) {
			//System.out.println(l.x+" "+l.z+" "+x+" "+z);
			targetChunks.add(new ChunkCoordIntPair(x, z));
		}
		
		Set<ChunkCoordIntPair> toLoad = new HashSet<ChunkCoordIntPair>(targetChunks);
		Set<ChunkCoordIntPair> toUnload = new HashSet<ChunkCoordIntPair>(l.loadedChunks);
		toLoad.removeAll(l.loadedChunks);
		toUnload.removeAll(targetChunks);
		
		/*System.out.println("target: "+targetChunks);
		System.out.println("loaded: "+l.loadedChunks);
		System.out.println("  load: "+toLoad);
		System.out.println("unload: "+toUnload);*/
		
		for(ChunkCoordIntPair ccip : toLoad) {
			l.loadedChunks.add(ccip);
			//System.out.println("load "+ccip);
			sendQueue.add(new SendQueueEntry(l, ccip.chunkXPos, ccip.chunkZPos, world, world.getChunkFromChunkCoords(ccip.chunkXPos, ccip.chunkZPos)));
		}
		
		for(ChunkCoordIntPair ccip : toUnload) {
			l.client.scheduleOutboundPacket(APILocator.getNetManager().wrap(new PacketMWSUnload(world, ccip.chunkXPos, ccip.chunkZPos), true));
			//System.out.println("unload "+ccip);
			l.loadedChunks.remove(ccip);
		}
	}

	public void removeListener(NetworkManager client) {
		for(MWSListener l : listeners) {
			if(l.client == client) {
				removeListener(l);
				break;
			}
		}
	}
	
	public void removeListener(MWSListener l) {
		l.isDead = true;
		listeners.remove(l);
		l.client.scheduleOutboundPacket(APILocator.getNetManager().wrap(new PacketMWSEnd(dimensionID), true));
	}
	
	void onWorldUnload() {
		Packet p = APILocator.getNetManager().wrap(new PacketMWSEnd(dimensionID), true);
		for(MWSListener l : listeners) {
			l.isDead = true;
			l.client.scheduleOutboundPacket(p);
		}
		listeners.clear();
		worldObj.clear();
	}
}
