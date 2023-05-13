package mods.immibis.subworlds.mws;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.network.NetworkManager;
import net.minecraft.world.ChunkCoordIntPair;


/**
 * Contains a reference to a client, as well as a
 * mechanism for determining which part of the world to send to this client.
 */
public abstract class MWSListener {
	public final NetworkManager client;
	
	public MWSListener(NetworkManager client) {
		this.client = client;
	}
	
	// See update()
	public int x, y, z;
	public boolean isDead;
	
	public int ticksToNextRangeCheck = 0;
	public int RANGE_CHECK_INTERVAL = 20;
	Set<ChunkCoordIntPair> loadedChunks = new HashSet<ChunkCoordIntPair>();
	
	/**
	 * Override this to update x, y and z with the centre of the area that will be sent to the client.
	 * Set isDead to true to remove the listener.
	 */
	public abstract void update();
}
