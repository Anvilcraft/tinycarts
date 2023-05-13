package mods.immibis.subworlds.mws.packets;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import mods.immibis.core.api.net.IPacket;
import mods.immibis.subworlds.mws.MWSManager;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PacketMWSChunk implements IPacket {
	
	public int cx, cz, dim;
	public short[] type;
	public byte[] meta;
	public byte[] light;
	public short mask;

	public PacketMWSChunk(int x, int z, World w, Chunk c) {
		cx = x;
		cz = z;
		dim = w.provider.dimensionId;
		
		fromChunk(c);
	}

	public PacketMWSChunk() {}

	@Override
	public byte getID() {
		return MWSManager.PKT_CHUNK;
	}

	public void fromChunk(Chunk c) {
		type = new short[16*16*256];
		meta = new byte[16*16*256];
		light = new byte[16*16*256];
		
		mask = 0;
		for(int k = 0; k < 16; k++)
			if(c.getBlockStorageArray()[k] != null)
				mask |= (1 << k);
		
		int pos = 0;
		for(int y = 0; y < 256; y++)
			for(int x = 0; x < 16; x++)
				for(int z = 0; z < 16; z++, pos++) {
					type[pos] = (short)Block.getIdFromBlock(c.getBlock(x, y, z));
					meta[pos] = (byte)c.getBlockMetadata(x, y, z);
					byte L = (byte)(c.getSavedLightValue(EnumSkyBlock.Block, x, y, z) & 15);
					byte SL = (byte)(c.getSavedLightValue(EnumSkyBlock.Sky, x, y, z) & 15);
					light[pos] = (byte)((SL << 4) | L);
				}
	}
	
	
	public void toChunk(Chunk c) {
		ExtendedBlockStorage[] ebs = c.getBlockStorageArray();
		
		int pos = 0;
		for(int y = 0; y < 256; y++) {
			ExtendedBlockStorage segment = ebs[y >> 4];
			if((mask & (1 << (y >> 4))) == 0) {
				pos += 256;
				ebs[y >> 4] = null;
				continue;
			}
			if(segment == null)
				segment = ebs[y >> 4] = new ExtendedBlockStorage(y >> 4, true);
			for(int x = 0; x < 16; x++)
				for(int z = 0; z < 16; z++, pos++) {
					int SL = (light[pos] >> 4) & 15;
					int L = light[pos] & 15;
					segment.setExtBlocklightValue(x, y&15, z, L);
					segment.setExtSkylightValue(x, y&15, z, SL);
					segment.func_150818_a(x, y&15, z, Block.getBlockById(type[pos]));
					segment.setExtBlockMetadata(x, y&15, z, meta[pos]);
				}
		}
		
		int x = c.xPosition << 4;
		int z = c.zPosition << 4;
		
		for(int k = 0; k < 16; k++)
			if((mask & (1 << k)) != 0)
				c.worldObj.markBlockRangeForRenderUpdate(x, k << 4, z, x+15, (k<<4)+15, z+15);
	}

	private static ThreadLocal<byte[]> buffer128kb = new ThreadLocal<byte[]>() {
		@Override
		protected byte[] initialValue() {return new byte[131072];}
	};
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(cx);
		out.writeInt(cz);
		out.writeInt(dim);
		out.writeShort(mask);
		DeflaterOutputStream o = new DeflaterOutputStream(out, new Deflater());
		//OutputStream o = out;
		byte[] buffer = buffer128kb.get();
		int pos = 0;
		for(short s : type) {
			buffer[pos++] = (byte)(s >> 8);
			buffer[pos++] = (byte)s;
		}
		o.write(buffer);
		o.write(meta);
		o.write(light);
		o.finish();
	}
	
	@Override
	public void read(DataInputStream in) throws IOException {
		cx = in.readInt();
		cz = in.readInt();
		dim = in.readInt();
		mask = in.readShort();
		
		InflaterInputStream i = new InflaterInputStream(in);
		//InputStream i = in;
		type = new short[65536];
		meta = new byte[65536];
		light = new byte[65536];
		byte[] buffer = buffer128kb.get();
		int pos = 0;
		readFully(i, buffer);
		for(int k = 0; k < 65536; k++, pos += 2) {
			int b1 = buffer[pos] & 255;
			int b2 = buffer[pos+1] & 255;
			//if(b1 == -1 || b2 == -1)
			//	throw new IOException("unexpected EOF");
			type[k] = (short)((b1 << 8) | b2);
		}
		readFully(i, meta);
		readFully(i, light);
	}
	
	private void readFully(InputStream i, byte[] b) throws IOException {
		int pos = 0;
		while(pos < b.length) {
			int read = i.read(b, pos, b.length - pos);
			if(read < 0)
				throw new IOException("Unexpected end of stream (after "+pos+" bytes, need "+b.length+")");
			pos += read;
		}
	}

	@Override
	public String getChannel() {
		return MWSManager.CHANNEL;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void onReceived(EntityPlayer source) {
		/*MainThreadTaskQueue.enqueue(this, Side.CLIENT);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void run() {*/
		WorldClient w = MWSManager.getClientWorld(dim);
		if(w != null) {
			w.doPreChunk(cx, cz, true);
			Chunk c = w.getChunkFromChunkCoords(cx, cz);
			toChunk(c);
			System.out.println("Received chunk "+dim+"/"+cx+"/"+cz);
		}
	}

}
