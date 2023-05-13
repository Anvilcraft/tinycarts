package mods.immibis.subworlds.mws.packets;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.immibis.core.MainThreadTaskQueue;
import mods.immibis.core.api.net.IPacket;
import mods.immibis.subworlds.mws.MWSManager;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PacketMWSUnload implements IPacket, Runnable {

	public int dim, x, z;
	
	public PacketMWSUnload() {}
	public PacketMWSUnload(World world, int x, int z) {this.dim = world.provider.dimensionId; this.x = x; this.z = z;}

	@Override
	public byte getID() {
		return MWSManager.PKT_UNLOAD;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(dim);
		out.writeInt(x);
		out.writeInt(z);
	}

	@Override
	public void read(DataInputStream in) throws IOException {
		dim = in.readInt();
		x = in.readInt();
		z = in.readInt();
	}

	@Override
	public String getChannel() {
		return MWSManager.CHANNEL;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void onReceived(EntityPlayer source) {
		MainThreadTaskQueue.enqueue(this, Side.CLIENT);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void run() {
		WorldClient w = MWSManager.getClientWorld(dim);
		if(w != null)
			w.doPreChunk(x, z, false);
	}

}
