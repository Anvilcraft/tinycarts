package mods.immibis.subworlds.mws.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.immibis.core.api.net.IPacket;
import mods.immibis.subworlds.mws.MWSManager;

public class PacketMWSSetWorld implements IPacket {
	
	public static final int NORMAL_DIM = Integer.MIN_VALUE;
	
	public int dim;
	
	public PacketMWSSetWorld() {}
	public PacketMWSSetWorld(int dim) {this.dim = dim;}

	@Override
	public byte getID() {
		return MWSManager.PKT_SET_WORLD;
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(dim);
	}

	@Override
	public void read(DataInputStream in) throws IOException {
		dim = in.readInt();
	}

	@SideOnly(Side.CLIENT)
	private static WorldClient oldWorld;
	
	@Override
	@SideOnly(Side.CLIENT)
	public void onReceived(EntityPlayer source) {
		if(dim == NORMAL_DIM) {
			if(oldWorld == null)
				throw new AssertionError();
			Minecraft.getMinecraft().theWorld = oldWorld;
			oldWorld = null;
		} else {
			oldWorld = Minecraft.getMinecraft().theWorld;
			WorldClient newWorld = MWSManager.getClientWorld(dim);
			if(newWorld != null)
				Minecraft.getMinecraft().theWorld = newWorld;
			else
				System.out.println("[WARNING] [SubWorlds MWS] Got set-world packet for unknown client world "+dim); 
		}
	}
	
	@Override
	public String getChannel() {
		return MWSManager.CHANNEL;
	}
}
