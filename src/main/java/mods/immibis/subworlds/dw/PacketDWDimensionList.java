package mods.immibis.subworlds.dw;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import mods.immibis.core.api.net.IPacket;
import mods.immibis.core.api.net.PacketUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

public class PacketDWDimensionList implements IPacket {
	
	public boolean clearExisting = false;
	public Map<Integer, WorldProps> data = new HashMap<Integer, WorldProps>();
	
	@Override
	public byte getID() {
		return DWManager.PKT_DIMENSION_LIST;
	}

	@Override
	public String getChannel() {
		return DWManager.CHANNEL;
	}

	@Override
	public void read(DataInputStream in) throws IOException {
		clearExisting = in.readBoolean();
		int len = in.readInt();
		for(int k = 0; k < len; k++) {
			int id = in.readInt();
			WorldProps wp = new WorldProps();
			wp.read(PacketUtils.readNBT(in));
			data.put(id, wp);
		}
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeBoolean(clearExisting);
		out.writeInt(data.size());
		for(Map.Entry<Integer, WorldProps> e : data.entrySet()) {
			out.writeInt(e.getKey());
			NBTTagCompound tag = new NBTTagCompound();
			e.getValue().write(tag);
			PacketUtils.writeNBT(tag, out);
		}
	}

	@Override
	public void onReceived(EntityPlayer source) {
		if(source == null && MinecraftServer.getServer() == null) {
			DWManager.registerDimensions(data, clearExisting);
		}
	}

}
