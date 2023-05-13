package mods.immibis.subworlds.mws.packets;


import io.netty.buffer.Unpooled;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.immibis.core.api.net.IPacket;
import mods.immibis.subworlds.mws.MWSManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PacketMWSTile implements IPacket {
	
	public Packet teDesc;
	public int dim;
	public IOException exception;
	
	public PacketMWSTile() {}

	public PacketMWSTile(World w, Packet teDesc) {
		this.teDesc = teDesc;
		dim = w.provider.dimensionId;
	}

	@Override
	public byte getID() {
		return MWSManager.PKT_TILE;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		if(exception != null)
			throw new IOException("delayed wrapped encoding exception", exception);
		
		Integer packetID = (Integer)EnumConnectionState.PLAY.func_150755_b().inverse().get(teDesc.getClass());
		if(packetID == null) {
			//System.out.println("can't send "+teDesc);
			out.writeByte(0);
		}
		else
	        out.writeByte(packetID.intValue());
		
		out.writeInt(dim);
		
		if(packetID != null) {
			PacketBuffer packetbuffer = new PacketBuffer(Unpooled.buffer());
			teDesc.writePacketData(packetbuffer);
			
			// TODO waste of memory/garbage
			byte[] data = new byte[packetbuffer.readableBytes()];
			packetbuffer.readBytes(data);
			out.writeInt(data.length);
			out.write(data);
		}
	}

	@Override
	public void read(DataInputStream in) throws IOException {
		int packetID = in.readByte() & 255;
		dim = in.readInt();
		
		if(packetID == 0)
			return;
		
		int len = in.readInt();
		if(len > 2097152)
			throw new IOException("input packet size > 2MB");
	
		// TODO waste of memory/garbage (considering this is coming from a ByteArrayInputStream)
		byte[] data = new byte[len];
		in.readFully(data);
		
		Packet packet;
		try {
			packet = ((Class<? extends Packet>)EnumConnectionState.PLAY.func_150755_b().get(packetID)).getConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IOException("failed to instantiate packet", e);
		}
		packet.readPacketData(new PacketBuffer(Unpooled.wrappedBuffer(data)));
		this.teDesc = packet;
	}

	@Override
	public String getChannel() {
		return MWSManager.CHANNEL;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void onReceived(EntityPlayer source) {
		if(teDesc == null)
			return;
		
		WorldClient w = MWSManager.getClientWorld(dim);
		if(w != null) {
			Minecraft mc = Minecraft.getMinecraft();
			WorldClient oldWorld = mc.theWorld;
			
			mc.theWorld = w;
			
			try {
				INetHandler handler = Minecraft.getMinecraft().getNetHandler();
				System.out.println("Received tile packet "+teDesc);
				teDesc.processPacket(handler);
			} catch(Exception e) {
				throw new RuntimeException(e);
			} finally {
				mc.theWorld = oldWorld;
			}
		}
	}

}
