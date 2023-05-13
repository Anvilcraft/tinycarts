package mods.immibis.subworlds.mws.packets;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.immibis.core.MainThreadTaskQueue;
import mods.immibis.core.api.net.IPacket;
import mods.immibis.subworlds.mws.MWSManager;
import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PacketMWSEnd implements IPacket, Runnable {

	public int dim;
	
	public PacketMWSEnd() {}
	public PacketMWSEnd(int dim) {this.dim = dim;}

	@Override
	public byte getID() {
		return MWSManager.PKT_END;
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
		MWSManager.clientEnd(dim);
	}

}
