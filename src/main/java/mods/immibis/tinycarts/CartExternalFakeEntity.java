package mods.immibis.tinycarts;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.immibis.core.api.APILocator;
import mods.immibis.core.api.net.IPacket;
import mods.immibis.subworlds.ClientFakeEntities;
import mods.immibis.subworlds.FakeEntity;
import mods.immibis.subworlds.dw.DWEntityRenderer;
import mods.immibis.subworlds.dw.DWWorldProvider;
import mods.immibis.subworlds.mws.MWSClientWorld;
import mods.immibis.subworlds.mws.MWSManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class CartExternalFakeEntity extends FakeEntity {
	public final int dimID;
	
	// rotation is applied first when rendering, then translation.
	// x/y/z is the coordinates of the centre of the player's world in the FE world (not vice versa)
	public double x, y, z, yaw;
	//public Quaternion rotation = new Quaternion();
	
	private double prevX, prevY, prevZ, prevYaw;
	//private Quaternion prevRotation = rotation.clone();
	
	public CartExternalFakeEntity(boolean isClient, int dimID) {
		super(isClient);
		this.dimID = dimID;
	}
	
	public CartExternalFakeEntity(boolean isClient, int entID, int dimID) {
		super(isClient, entID);
		this.dimID = dimID;
	}

	@Override
	public Packet getUpdatePacket() {
		final double MOVE_THRESHOLD = 0.2;
		if(prevYaw == yaw && Math.abs(x - prevX) < MOVE_THRESHOLD && Math.abs(y - prevY) < MOVE_THRESHOLD && Math.abs(z - prevZ) < MOVE_THRESHOLD) {
			return null;
		}
		
		prevYaw = yaw;
		prevX = x;
		prevY = y;
		prevZ = z;
		
		return APILocator.getNetManager().wrap(new UpdatePacket(this), true);
	}

	@Override
	public Packet getDescriptionPacket() {
		return APILocator.getNetManager().wrap(new CreatePacket(this), true);
	}

	@Override
	public Packet getDestructionPacket() {
		return APILocator.getNetManager().wrap(new DeletePacket(entityID), true);
	}
	
	private boolean updated = false;
	@Override
	public void tick() {
		super.tick();
		
		if(isClient) {
			if(updated)
				updated = false;
			else {
				prevX = x;
				prevY = y;
				prevZ = z;
				prevYaw = yaw;
			}
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void render(double pl_x, double pl_y, double pl_z, float partialTick) {
		MWSClientWorld subWorld = MWSManager.getClientWorld(dimID);
		World plWorld = Minecraft.getMinecraft().theWorld;
		if(subWorld == null || !(plWorld.provider instanceof DWWorldProvider)) {
			super.render(pl_x, pl_y, pl_z, partialTick);
			return;
		}
		
		GL11.glPushMatrix();
		GL11.glTranslated(-pl_x, -pl_y, -pl_z);
		// now 0,0,0 = origin of the world the player is in
		
		DWWorldProvider pv = (DWWorldProvider)plWorld.provider;
		GL11.glTranslated(pv.props.xsize/2, 0, pv.props.zsize/2);
		// now 0,0,0 = centre-bottom of world the player is in
		
		double cx = prevX + (x - prevX) * partialTick;
		double cy = prevY + (y - prevY) * partialTick;
		double cz = prevZ + (z - prevZ) * partialTick;
		
		// why not just cx,cy,cz?
		GL11.glTranslated(cx-x, cy-y, cz-z);
		
		GL11.glScalef(EntityMinecartAwesome.SCALE, EntityMinecartAwesome.SCALE, EntityMinecartAwesome.SCALE);
		GL11.glRotatef((float)yaw, 0, 1, 0);
		
		DWEntityRenderer.renderClientWorld(subWorld, partialTick, 0, 0, 0, x, y, z);
		GL11.glPopMatrix();
		
		OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        RenderHelper.disableStandardItemLighting();
        GL11.glColor3f(1, 1, 1);
	}
	
	public static class DeletePacket implements IPacket {
		@Override
		public String getChannel() {
			return NetworkHandler.CHANNEL;
		}
		
		@Override
		public byte getID() {
			return NetworkHandler.PKT_CEFE_DELETE;
		}
		
		public int entID;
		
		public DeletePacket() {}
		public DeletePacket(int id) {entID = id;}
		
		@Override
		public void onReceived(EntityPlayer source) {
			ClientFakeEntities.remove(entID);
		}
		
		@Override
		public void read(DataInputStream in) throws IOException {
			entID = in.readInt();
		}
		
		@Override
		public void write(DataOutputStream out) throws IOException {
			out.writeInt(entID);
		}
	}
	
	public static class CreatePacket implements IPacket {
		@Override
		public String getChannel() {
			return NetworkHandler.CHANNEL;
		}
		
		@Override
		public byte getID() {
			return NetworkHandler.PKT_CEFE_CREATE;
		}
		
		public int entID, dimID;
		public double x, y, z, r;
		
		public CreatePacket() {}
		public CreatePacket(CartExternalFakeEntity ent) {
			entID = ent.entityID;
			dimID = ent.dimID;
			x = ent.x;
			y = ent.y;
			z = ent.z;
			r = ent.yaw;
		}
		
		@Override
		public void onReceived(EntityPlayer source) {
			CartExternalFakeEntity e = new CartExternalFakeEntity(true, entID, dimID);
			e.x = x;
			e.y = y;
			e.z = z;
			e.yaw = r;
			ClientFakeEntities.add(e);
		}
		
		@Override
		public void read(DataInputStream in) throws IOException {
			entID = in.readInt();
			x = in.readDouble();
			y = in.readDouble();
			z = in.readDouble();
			r = in.readDouble();
		}
		
		@Override
		public void write(DataOutputStream out) throws IOException {
			out.writeInt(entID);
			out.writeDouble(x);
			out.writeDouble(y);
			out.writeDouble(z);
			out.writeDouble(r);
		}
	}
	
	public static class UpdatePacket implements IPacket {
		@Override
		public String getChannel() {
			return NetworkHandler.CHANNEL;
		}
		
		@Override
		public byte getID() {
			return NetworkHandler.PKT_CEFE_UPDATE;
		}
		
		public int entID;
		public double x, y, z;
		public float rot;
		
		public UpdatePacket() {}
		public UpdatePacket(CartExternalFakeEntity ent) {
			entID = ent.entityID;
			x = ent.x;
			y = ent.y;
			z = ent.z;
			rot = (float)ent.yaw;
		}
		
		@Override
		public void onReceived(EntityPlayer source) {
			CartExternalFakeEntity ent = (CartExternalFakeEntity)ClientFakeEntities.get(entID);
			
			ent.prevX = x;
			ent.prevY = y;
			ent.prevZ = z;
			
			ent.updated = true;
			
			ent.x = x;
			ent.y = y;
			ent.z = z;
			ent.yaw = rot;
		}
		
		@Override
		public void read(DataInputStream in) throws IOException {
			entID = in.readInt();
			x = in.readDouble();
			y = in.readDouble();
			z = in.readDouble();
			rot = in.readFloat();
		}
		
		@Override
		public void write(DataOutputStream out) throws IOException {
			out.writeInt(entID);
			out.writeDouble(x);
			out.writeDouble(y);
			out.writeDouble(z);
			out.writeFloat(rot);
		}
	}
}
