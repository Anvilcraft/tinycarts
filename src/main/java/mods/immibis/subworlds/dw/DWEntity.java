package mods.immibis.subworlds.dw;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import mods.immibis.subworlds.FakeEntity;
import mods.immibis.subworlds.mws.MWSListener;
import mods.immibis.subworlds.mws.MWSManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * DW stands for "Detached World".
 * Maximum size is always 48x48x48, this cannot easily be changed.
 * There might be smaller, 16x16x16 
 */
public abstract class DWEntity extends Entity {
	
	// set of players last known to be tracking this entity which are outside it
	private Set<EntityPlayerMP> trackingPlayers = new HashSet<EntityPlayerMP>();
	
	// set of players last known to be inside this entity
	private Set<EntityPlayerMP> enclosedPlayers = new HashSet<EntityPlayerMP>();
	
	// fake entity sent to enclosed players to render the external world. 
	private FakeEntity externalWorldFE;
	
	// internal world, cannot change after initialization
	private WorldServer internalWorld;
	
	//private int lastChunkX = Integer.MAX_VALUE, lastChunkZ = Integer.MAX_VALUE;
	
	public DWEntity(World par1World) {
		super(par1World);
		setSize(48, 48);
	}

	private static final int WATCHER_INDEX_DIMID = 8;

	@Override
	protected void entityInit() {
		dataWatcher.addObject(WATCHER_INDEX_DIMID, 0);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound var1) {
		
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound var1) {
		
	}
	
	protected abstract WorldServer getInternalWorld();
	protected abstract void updateExternalWorldFE(FakeEntity e);
	protected abstract FakeEntity createExternalWorldFE();
	protected abstract void applyGLRotation(float partialTick);
	
	private boolean requiresInteriorLoaded = false;
	public boolean requiresInteriorLoaded() {return requiresInteriorLoaded;}
	
	/*@Override
	public void setPosition(double par1, double par3, double par5) {
	}
	@Override
	public void setPositionAndRotation(double par1, double par3, double par5, float par7, float par8) {};
	*/	
	
	@Override
	public void onUpdate() {
		
		this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        
		/*if(worldObj.isRemote) {
			posX = serverPosX / 32.0;
			posY = serverPosY / 32.0;
			posZ = serverPosZ / 32.0;
		}*/
		
		if(!worldObj.isRemote) {
			
			
			int lastDimID = (internalWorld == null ? 0 : internalWorld.provider.dimensionId);
			internalWorld = getInternalWorld();
			if(internalWorld.provider.dimensionId != lastDimID)
				dataWatcher.updateObject(WATCHER_INDEX_DIMID, internalWorld.provider.dimensionId);
			
			// enclosed players see the outside world via MWS
			
			@SuppressWarnings("unchecked")
			Set<EntityPlayerMP> nowEnclosed = new HashSet<EntityPlayerMP>(internalWorld.playerEntities);
			for(final EntityPlayerMP pl : nowEnclosed)
				if(!enclosedPlayers.contains(pl)) {
					MWSManager.getWorldManager(worldObj).addListener(new MWSListener(pl.playerNetServerHandler.netManager) {
						@Override
						public void update() {
							/*x = (int)DWEntity.this.comX;
							y = (int)DWEntity.this.comY;
							z = (int)DWEntity.this.comZ;*/
							x = (int)posX;
							y = (int)posY;
							z = (int)posZ;
						}
					});
				}
			for(final EntityPlayerMP pl : enclosedPlayers)
				if(!nowEnclosed.contains(pl))
					MWSManager.getWorldManager(worldObj).removeListener(pl.playerNetServerHandler.netManager);
			
			enclosedPlayers = nowEnclosed;
			
			// tracking players see the enclosed world via MWS
			
			Set<EntityPlayerMP> nowTracking = DWUtils.getTrackingPlayers(this);
			for(final EntityPlayerMP pl : trackingPlayers)
				if(!nowTracking.contains(pl))
					MWSManager.getWorldManager(internalWorld).removeListener(pl.playerNetServerHandler.netManager);
			trackingPlayers.retainAll(nowTracking);
			
			for(final EntityPlayerMP pl : nowTracking)
				if(!trackingPlayers.contains(pl)) {
					MWSManager.getWorldManager(internalWorld).addListener(new MWSListener(pl.playerNetServerHandler.netManager) {
						@Override public void update() {}
					});
					trackingPlayers.add(pl);
				}
			
			requiresInteriorLoaded = enclosedPlayers.size() > 0 || trackingPlayers.size() > 0;
			
			if(externalWorldFE == null) {
				externalWorldFE = createExternalWorldFE();
			}
			
			//DWWorldProvider provider = (DWWorldProvider)getInternalWorld().provider;
			
			updateExternalWorldFE(externalWorldFE);
			externalWorldFE.setTrackingPlayers(enclosedPlayers);
			externalWorldFE.tick();
		}
	}
	
	@Override
	public boolean isInRangeToRenderDist(double distsq) {
		return true;
	}

	public int getEnclosedDimensionClient() {
		return dataWatcher.getWatchableObjectInt(WATCHER_INDEX_DIMID);
	}

	public void onUnloadOrDestroy() {
		for(final EntityPlayerMP pl : enclosedPlayers)
			MWSManager.getWorldManager(worldObj).removeListener(pl.playerNetServerHandler.netManager);
		if(internalWorld != null)
			for(final EntityPlayerMP pl : trackingPlayers)
				MWSManager.getWorldManager(internalWorld).removeListener(pl.playerNetServerHandler.netManager);
		if(externalWorldFE != null)
			externalWorldFE.setTrackingPlayers(Collections.<EntityPlayerMP>emptySet());
	}
}
