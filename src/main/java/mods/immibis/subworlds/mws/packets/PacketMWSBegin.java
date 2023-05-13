package mods.immibis.subworlds.mws.packets;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.immibis.core.api.net.IPacket;
import mods.immibis.subworlds.mws.MWSManager;
import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PacketMWSBegin implements /*Runnable,*/ IPacket {
	
	public int dim;
	
	public PacketMWSBegin() {}
	public PacketMWSBegin(int dim) {this.dim = dim;}

	@Override
	public byte getID() {
		return MWSManager.PKT_BEGIN;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(dim);
	}

	@Override
	public void read(DataInputStream in) throws IOException {
		dim = in.readInt();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void onReceived(EntityPlayer source) {
		/*MainThreadTaskQueue.enqueue(this, Side.CLIENT);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void run() {*/
		MWSManager.clientBegin(dim);
	}
	
	@Override
	public String getChannel() {
		return MWSManager.CHANNEL;
	}

}
