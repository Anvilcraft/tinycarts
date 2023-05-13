package mods.immibis.tinycarts;

import mods.immibis.core.api.net.IPacket;
import mods.immibis.core.api.net.IPacketMap;

public class NetworkHandler implements IPacketMap {
	
	public static final String CHANNEL = "TinyCarts";
	
	public static final byte PKT_CEFE_CREATE = 0;
	public static final byte PKT_CEFE_DELETE = 1;
	public static final byte PKT_CEFE_UPDATE = 2;

	@Override
	public String getChannel() {
		return CHANNEL;
	}

	@Override
	public IPacket createS2CPacket(byte id) {
		switch(id) {
		case PKT_CEFE_CREATE: return new CartExternalFakeEntity.CreatePacket();
		case PKT_CEFE_DELETE: return new CartExternalFakeEntity.DeletePacket();
		case PKT_CEFE_UPDATE: return new CartExternalFakeEntity.UpdatePacket();
		}
		return null;
	}

	@Override
	public IPacket createC2SPacket(byte id) {
		return null;
	}

}
