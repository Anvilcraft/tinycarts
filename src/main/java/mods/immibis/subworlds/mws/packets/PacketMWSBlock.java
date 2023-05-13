package mods.immibis.subworlds.mws.packets;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.immibis.core.MainThreadTaskQueue;
import mods.immibis.core.api.net.IPacket;
import mods.immibis.subworlds.mws.MWSManager;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PacketMWSBlock implements IPacket, Runnable {
	
	int x, y, z, type, meta, dim;

	public PacketMWSBlock(int x, int y, int z, World w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.dim = w.provider.dimensionId;
		this.type = Block.getIdFromBlock(w.getBlock(x, y, z));
        this.meta = w.getBlockMetadata(x, y, z);
	}

	public PacketMWSBlock() {}

	@Override
	public byte getID() {
		return MWSManager.PKT_BLOCK;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(x);
		out.writeInt(y);
		out.writeInt(z);
		out.writeShort((short)type);
		out.writeByte((byte)meta);
		out.writeInt(dim);
	}

	@Override
	public void read(DataInputStream in) throws IOException {
		x = in.readInt();
		y = in.readInt();
		z = in.readInt();
		type = in.readShort();
		meta = in.readByte();
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
		World w = MWSManager.getClientWorld(dim);
		if(w != null) {
			w.setBlock(x, y, z, Block.getBlockById(type), meta, 3);
		}
	}

}
