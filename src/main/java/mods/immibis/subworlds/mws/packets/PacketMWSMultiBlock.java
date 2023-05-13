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

public class PacketMWSMultiBlock implements IPacket.Asynchronous, Runnable {
	
	public PacketMWSMultiBlock() {}
	
	public int size, pos, dim, cx, cz;
	public byte[] meta;
	public short[] type;
	public byte[] x;
	public int[] y;
	public byte[] z;
	
	public PacketMWSMultiBlock(int cx, int cz, int size, World w) {
		this.size = size;
		this.cx = cx;
		this.cz = cz;
		dim = w.provider.dimensionId;
		pos = 0;
		meta = new byte[size];
		type = new short[size];
		x = new byte[size];
		y = new int[size];
		z = new byte[size];
	}

	@Override
	public byte getID() {
		return MWSManager.PKT_MULTIBLOCK;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(pos);
		out.writeInt(cx);
		out.writeInt(cz);
		out.writeInt(dim);
		for(int k = 0; k < pos; k++) {
			out.writeByte(x[k]);
			out.writeInt(y[k]);
			out.writeByte(z[k]);
			out.writeByte(meta[k]);
			out.writeShort(type[k]);
		}
	}

	@Override
	public void read(DataInputStream in) throws IOException {
		size = pos = in.readInt();
		cx = in.readInt();
		cz = in.readInt();
		dim = in.readInt();
		for(int k = 0; k < size; k++) {
			x[k] = in.readByte();
			y[k] = in.readInt();
			z[k] = in.readByte();
			meta[k] = in.readByte();
			type[k] = in.readShort();
		}
	}

	public void addBlock(int x2, int y2, int z2, World w) {
		x[pos] = (byte)x2;
		y[pos] = y2;
		z[pos] = (byte)z2;
		type[pos] = (short)Block.getIdFromBlock(w.getBlock(x2 + (cx << 4), y2, z2 + (cz << 4)));
		meta[pos] = (byte)w.getBlockMetadata(x2 + (cx << 4), y2, z2 + (cz << 4));
		pos++;
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
			for(int k = 0; k < pos; k++) {
				w.setBlock((cx<<4) + x[k], y[k], (cz<<4) + z[k], Block.getBlockById(type[k]), meta[k], 2);
			}
		}
	}

}
