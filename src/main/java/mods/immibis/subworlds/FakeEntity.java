package mods.immibis.subworlds;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.entity.RenderEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * An object which is synced with clients, like an entity, but isn't actually an entity.
 * They are not automatically associated with a world.
 */
public abstract class FakeEntity {
	public abstract Packet getUpdatePacket();
	public abstract Packet getDescriptionPacket();
	public abstract Packet getDestructionPacket();
	
	private static AtomicInteger nextEntityID = new AtomicInteger();
	
	public final int entityID;
	public final boolean isClient;
	
	public FakeEntity(boolean isClient) {
		entityID = nextEntityID.incrementAndGet();
		this.isClient = isClient;
	}
	
	public FakeEntity(boolean isClient, int entityID) {
		this.entityID = entityID;
		this.isClient = isClient;
	}
	
	private Set<EntityPlayerMP> trackingPlayers = new HashSet<EntityPlayerMP>();
	
	public final void setTrackingPlayers(Set<EntityPlayerMP> _new) {
		for(EntityPlayerMP pl : _new)
			if(!trackingPlayers.contains(pl)) {
				Packet packet = getDescriptionPacket();
				if(packet != null)
					pl.playerNetServerHandler.netManager.channel().write(packet);
			}
		
		for(EntityPlayerMP pl : trackingPlayers)
			if(!_new.contains(pl)) {
				Packet packet = getDestructionPacket();
				if(packet != null)
					pl.playerNetServerHandler.netManager.channel().write(packet);
			}
		
		trackingPlayers.clear();
		trackingPlayers.addAll(_new);
	}
	
	public void tick() {
		if(!isClient) {
			Packet p = getUpdatePacket();
			
			if(p != null)
				for(EntityPlayerMP pl : trackingPlayers)
					pl.playerNetServerHandler.netManager.channel().write(p);
		}
	}
	
	@SideOnly(Side.CLIENT)
	protected static RenderEntity defaultRender;
	@SideOnly(Side.CLIENT)
	protected static Entity defaultRenderEntity;
	
	static {
		try {
			RenderEntity rd = new RenderEntity();
			defaultRender = rd;
			defaultRenderEntity = new Entity(null) {
				{
					boundingBox.setBounds(-1, -1, -1, 1, 1, 1);
					lastTickPosX = lastTickPosY = lastTickPosZ = 0;
				}
		
				@Override protected void entityInit() {}
				@Override protected void readEntityFromNBT(NBTTagCompound var1) {}
				@Override protected void writeEntityToNBT(NBTTagCompound var1) {}
			};
		} catch(Error e) {
		}
	}
	
	@SideOnly(Side.CLIENT)
	// rendering origin is at world coordinates: -pl_x,-pl_y,-pl_z
	public void render(double pl_x, double pl_y, double pl_z, float partialTick) {
		defaultRender.doRender(defaultRenderEntity, 3, 3, 3, 0, partialTick);
		defaultRender.doRender(defaultRenderEntity, -pl_x-2, -pl_y-2, -pl_z-2, 0, partialTick);
	}
	
	@SideOnly(Side.CLIENT)
	void renderBase(float partialTick) {
		EntityClientPlayerMP pl = Minecraft.getMinecraft().thePlayer;
		double pl_x = pl.lastTickPosX + (pl.posX - pl.lastTickPosX) * partialTick;
		double pl_y = pl.lastTickPosY + (pl.posY - pl.lastTickPosY) * partialTick;
		double pl_z = pl.lastTickPosZ + (pl.posZ - pl.lastTickPosZ) * partialTick;
		render(pl_x, pl_y, pl_z, partialTick);
	}
}
