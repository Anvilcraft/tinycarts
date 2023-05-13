package mods.immibis.tinycarts;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import mods.immibis.subworlds.FakeEntity;
import mods.immibis.subworlds.SubWorldsMod;
import mods.immibis.subworlds.dw.DWEntity;
import mods.immibis.subworlds.dw.DWManager;
import mods.immibis.subworlds.dw.DWWorldProvider;
import mods.immibis.subworlds.dw.WorldProps;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.minecart.MinecartInteractEvent;

import org.lwjgl.opengl.GL11;

public class EntityMinecartAwesome extends EntityMinecart {
	
	public static final int XSIZE = 16;
	public static final int ZSIZE = 10;
	public static final int YSIZE = 10;
	
	public static final int SCALE = 10; // render scale; internal blocks per external block

	public EntityMinecartAwesome(World par1World) {
		super(par1World);
	}
	
	public EntityMinecartAwesome(World w, double x, double y, double z) {
		super(w, x, y, z);
		if(w.isRemote)
			throw new IllegalStateException();
		
		WorldProps props = new WorldProps();
		props.xsize = XSIZE;
		props.ysize = YSIZE;
		props.zsize = ZSIZE;
		props.generatorClass = InteriorChunkGen.class;
		internalWorldID = DWManager.createWorld(props);
	}
	
	
	@Override
	public double getMountedYOffset() {
		return 0.3f;
	}
	
	
	private int internalWorldID;
	@Override
	protected void readEntityFromNBT(NBTTagCompound par1nbtTagCompound) {
		super.readEntityFromNBT(par1nbtTagCompound);
		internalWorldID = par1nbtTagCompound.getInteger("intwid");
	}
	@Override
	protected void writeEntityToNBT(NBTTagCompound par1nbtTagCompound) {
		super.writeEntityToNBT(par1nbtTagCompound);
		par1nbtTagCompound.setInteger("intwid", internalWorldID);
	}
	
	private DWSubEntity getDW() {
		if(riddenByEntity instanceof DWSubEntity)
			return (DWSubEntity)riddenByEntity;
		else if(worldObj.isRemote)
			return null;
		else {
			DWSubEntity dwsub = new DWSubEntity(worldObj);
			dwsub.posX = posX;
			dwsub.posY = posY;
			dwsub.posZ = posZ;
			worldObj.spawnEntityInWorld(dwsub);
			dwsub.mountEntity(this);
			return dwsub;
		}
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		
		getDW();
	}
	
	@Override
	public boolean interactFirst(EntityPlayer par1EntityPlayer)
    {
        if(MinecraftForge.EVENT_BUS.post(new MinecartInteractEvent(this, par1EntityPlayer))) 
            return true;
        
        if(!(par1EntityPlayer instanceof EntityPlayerMP))
        	return false;
        
        if(worldObj.isRemote)
        	return true;
        
        
        
        ServerConfigurationManager scm = MinecraftServer.getServer().getConfigurationManager();
        scm.transferPlayerToDimension((EntityPlayerMP)par1EntityPlayer, internalWorldID, ((DWWorldProvider)getDW().getInternalWorld().provider).teleporter);
        
        return true;
    }

	@Override
	public int getMinecartType() {
		return -1;
	}
	
	// XXX the whole purpose of this field is hacky. subworlds should keep track of this for us.
	static Map<Integer, WeakReference<DWSubEntity>> entitiesByInternalID = new HashMap<Integer, WeakReference<DWSubEntity>>();
	
	public static class DWSubEntity extends DWEntity {
		public DWSubEntity(World w) {
			super(w);
			setSize(Math.min(XSIZE, ZSIZE)/(float)SCALE, YSIZE/(float)SCALE);
		}
		
		public EntityMinecartAwesome getCart() {
			if(ridingEntity instanceof EntityMinecartAwesome)
				return (EntityMinecartAwesome)ridingEntity;
			return null;
		}
		
		private int internalWorldID = Integer.MIN_VALUE;
		
		@Override
		public void onUnloadOrDestroy() {
			if(!worldObj.isRemote) {
				synchronized(entitiesByInternalID) {
					int intWID = internalWorldID;
					if(intWID != Integer.MIN_VALUE) {
						WeakReference<DWSubEntity> ent = entitiesByInternalID.get(intWID);
						if(ent != null && ent.get() == this) {
							entitiesByInternalID.remove(intWID);
						}
					}
				}
			}
			mountEntity(null);
			super.onUnloadOrDestroy();
		}
		
		private Ticket externalTicket, internalTicket;
		
		private static final int EXTERNAL_VIEW_DISTANCE = 7;
		
		private static final boolean DEBUG = false;
		
		private void updateChunkLoading() {
			boolean load = requiresInteriorLoaded();
			if(load != (externalTicket != null)) {
				if(DEBUG) System.out.println("[TinyCarts DEBUG] External loading: now "+load);
				if(load) {
					WorldServer world = (WorldServer)worldObj;
					
					externalTicket = ForgeChunkManager.requestTicket(SubWorldsMod.INSTANCE, world, Type.NORMAL);
					
					int baseX = (int)(posX) >> 4;
					int baseZ = (int)(posZ) >> 4;
					
					for(int dx = -EXTERNAL_VIEW_DISTANCE; dx <= EXTERNAL_VIEW_DISTANCE; dx++)
						for(int dz = -EXTERNAL_VIEW_DISTANCE; dz <= EXTERNAL_VIEW_DISTANCE; dz++) {
							int x = dx + baseX, z = dz + baseZ;
							ForgeChunkManager.forceChunk(externalTicket, new ChunkCoordIntPair(x, z));
							world.theChunkProviderServer.loadChunk(x, z);
						}
					
				} else {
					
					ForgeChunkManager.releaseTicket(externalTicket);
					externalTicket = null;
				}
			}

			if(load != (internalTicket != null)) {
				if(load) {
					WorldServer world = getInternalWorld();
					if(world == null)
						throw new IllegalStateException("Failed to load internal world.");
					
					internalTicket = ForgeChunkManager.requestTicket(SubWorldsMod.INSTANCE, world, Type.NORMAL);
					
					int size = (Math.max(XSIZE, YSIZE)+15)/16;
					
					if(DEBUG) System.out.println("[TinyCarts DEBUG] Internal loading: now true ("+size+"x"+size+")");
					for(int x = 0; x < size; x++) 
						for(int z = 0; z < size; z++) {
							ForgeChunkManager.forceChunk(internalTicket, new ChunkCoordIntPair(x, z));
							world.theChunkProviderServer.loadChunk(x, z);
						}
					
				} else {
					
					if(DEBUG) System.out.println("[TinyCarts DEBUG] Internal loading: now false");
					ForgeChunkManager.releaseTicket(internalTicket);
					internalTicket = null;
				}
			}
			
			if(!isDead) { // should always be true
				int intWID = internalWorldID = getCart().internalWorldID;
				synchronized(entitiesByInternalID) {
					WeakReference<DWSubEntity> ref = entitiesByInternalID.get(intWID);
					DWSubEntity reffed = (ref == null ? null : ref.get());
					if(reffed == null || (reffed.isDead && reffed != this)) {
						entitiesByInternalID.put(intWID, new WeakReference<DWSubEntity>(this));
					}
				}
			} else assert false : "updateChunkLoading on dead entity?";
		}
		
		private static float getActualClientMinecartYaw(EntityMinecart cart, double old) {
			double x = cart.posX, y = cart.posY, z = cart.posZ;
			Vec3 vec3 = cart.func_70489_a(x, y, z);
	        //float f5 = cart.rotationPitch;
	        double d6 = 0.30000001192092896D;
	        float yaw = cart.rotationYaw;
	        
	        if (vec3 != null)
	        {
	            Vec3 vec31 = cart.func_70495_a(x, y, z, d6);
	            Vec3 vec32 = cart.func_70495_a(x, y, z, -d6);

	            if (vec31 == null)
	            {
	                vec31 = vec3;
	            }

	            if (vec32 == null)
	            {
	                vec32 = vec3;
	            }

	            //renderX += vec3.xCoord - x;
	            //renderY += (vec31.yCoord + vec32.yCoord) / 2.0D - y;
	            //renderZ += vec3.zCoord - z;
	            Vec3 vec33 = vec32.addVector(-vec31.xCoord, -vec31.yCoord, -vec31.zCoord);

	            if (vec33.lengthVector() != 0.0D)
	            {
	                vec33 = vec33.normalize();
	                yaw = (float)(Math.atan2(vec33.zCoord, vec33.xCoord) * 180.0D / Math.PI);
	                //f5 = (float)(Math.atan(vec33.yCoord) * 73.0D);
	            }
	        }
	        
	        /*if(Math.abs(angleDiff(old, yaw)) > Math.abs(angleDiff(old, yaw+180)))
	        	if(yaw > 0)
	        		yaw -= 180;
	        	else
	        		yaw += 180;*/
	        
	        return yaw;
		}
		
		/*private static double angleDiff(double a, double b) {
			b -= a;
			if(b < 0)
				b = (b % 360) + 360;
			b = (b + 180) % 360 - 180;
			assert !(b < -180 || b > 180) : "angleDiff is broken";
			return b;
		}*/
		
		@Override
		public void onUpdate() {
			if(!worldObj.isRemote && (getCart() == null || getCart().getDW() != this)) {
				setDead();
				return;
			}
			
			//if(ridingEntity != null && worldObj.isRemote)
			//	System.out.println(worldObj.isRemote+" "+(int)posX+" "+(int)posY+" "+(int)posZ+" "+(int)ridingEntity.posX+" "+(int)ridingEntity.posY+" "+(int)ridingEntity.posZ);
			
			if(getCart() != null)
				rotationYaw = !worldObj.isRemote ? ridingEntity.rotationYaw : getActualClientMinecartYaw(getCart(), rotationYaw);
			//if(worldObj.isRemote) {
				//System.out.println("yaw "+rotationYaw);
			//}
			
			super.onUpdate();
			
			if(!worldObj.isRemote) {
				updateChunkLoading();
			}
		}

		@Override
		protected void applyGLRotation(float partialTick) {
			GL11.glScalef(1.0f/SCALE, 1.0f/SCALE, 1.0f/SCALE);
			if(ridingEntity != null)
				GL11.glRotatef(-rotationYaw, 0, 1, 0);
		}
		
		@Override
		protected FakeEntity createExternalWorldFE() {
			return new CartExternalFakeEntity(false, getCart().internalWorldID);
		}
		
		@Override
		protected WorldServer getInternalWorld() {
			return MinecraftServer.getServer().worldServerForDimension(getCart().internalWorldID);
		}
		
		@Override
		protected void updateExternalWorldFE(FakeEntity e_) {
			CartExternalFakeEntity e = (CartExternalFakeEntity)e_;
			e.x = posX;
			e.y = posY;
			e.z = posZ;
			e.yaw = ridingEntity == null ? 0 : ridingEntity.rotationYaw;
		}
		
		// this is called when a position update is received from the server.
		// by overriding it we make sure the client doesn't try to be smart and push this entity out of bounding boxes.
		// also, ignore the position from the update packet. because it's incorrect for entities that are riding other entities.
		// also, ignore the rotation. because we copy that from the cart entity.
		// in fact, just do nothing when a position/rotation update is received.
		@Override
		public void setPositionAndRotation2(double par1, double par3, double par5, float par7, float par8, int par9) {
			//this.setPosition(par1, par3, par5);
			//this.setRotation(par7, par8);
		}
	}

}
