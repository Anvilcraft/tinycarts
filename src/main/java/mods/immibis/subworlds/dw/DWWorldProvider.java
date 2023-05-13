package mods.immibis.subworlds.dw;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class DWWorldProvider extends WorldProvider {
	
	public WorldProps props;
	
	public static final boolean
		LIGHTNING = false,
		RAIN = false,
		RESPAWN_HERE = false,
		FOG = false,
		FULLBRIGHT = true;
	
	public DWTeleporter teleporter;
	
	@Override
	public void setDimension(int dim) {
		props = DWManager.getProps(dim);
		super.setDimension(dim);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public Vec3 getSkyColor(Entity cameraEntity, float partialTicks) {
		return super.getSkyColor(cameraEntity, partialTicks);
	}

	@Override
	public String getDimensionName() {
		return "Detached World";
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public float[] calcSunriseSunsetColors(float par1, float par2) {
		return null;
	}
	
	@Override
	public boolean canCoordinateBeSpawn(int par1, int par2) {
		return par1 >= 0 && par1 < props.xsize && par2 >= 0 && par2 < props.zsize;
	}
	
	@Override
	public boolean canDoLightning(Chunk chunk) {
		return LIGHTNING;
	}
	
	@Override
	public boolean canDoRainSnowIce(Chunk chunk) {
		return RAIN;
	}
	
	/*@Override
	public boolean canMineBlock(EntityPlayer player, int x, int y, int z) {
		return x >= 0 && y >= 0 && z >= 0 && x < HSIZE && y < VSIZE && z < HSIZE && super.canMineBlock(player, x, y, z);
	}*/
	
	@Override
	public boolean canRespawnHere() {
		return RESPAWN_HERE;
	}
	
	@Override
	public IChunkProvider createChunkGenerator() {
		try {
			return props.generatorClass.getConstructor(WorldServer.class, DWWorldProvider.class).newInstance((WorldServer)worldObj, this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		//new DWChunkGenerator((WorldServer)worldObj, this);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean doesXZShowFog(int par1, int par2) {
		return FOG;
	}
	
	@Override
	protected void generateLightBrightnessTable() {
		if(FULLBRIGHT) {
			for(int k = 0; k < 16; k++)
				lightBrightnessTable[k] = 1;
		} else
			super.generateLightBrightnessTable();
	}
	
	@Override
	public int getActualHeight() {
		return props.ysize;
	}
	
	@Override
	public int getAverageGroundLevel() {
		return 0;
	}
	
	@Override
	public BiomeGenBase getBiomeGenForCoords(int x, int z) {
		return DWChunkGenerator.BIOME;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public Vec3 getFogColor(float par1, float par2) {
		//return worldObj.getWorldVec3Pool().getVecFromPool((int)(par1*255), 255, (int)(par2*255));
		return super.getFogColor(par1, par2);
	}
	
	@Override
	public int getHeight() {
		return props.ysize;
	}
	
	@Override
	public ChunkCoordinates getRandomizedSpawnPoint() {
		return new ChunkCoordinates(props.xsize/2, props.ysize/2, props.zsize/2);
	}
	
	@Override
	public int getRespawnDimension(EntityPlayerMP player) {
		return RESPAWN_HERE ? dimensionId : 0;
	}
	
	@Override
	public String getSaveFolder() {
		return "CRAFT" + dimensionId;
	}
	
	@Override
	public long getSeed() {
		return 0;
	}
	
	@Override
	public ChunkCoordinates getSpawnPoint() {
		return getRandomizedSpawnPoint();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public double getVoidFogYFactor() {
		return 1;
	}
	
	@Override
	public String getWelcomeMessage() {
		return "Entering sub-world...";
	}
	
	@Override
	public String getDepartMessage() {
		return "Leaving sub-world...";
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean getWorldHasVoidParticles() {
		return false;
	}
	
	@Override
	public boolean isDaytime() {
		return true;
	}
	
	@Override
	public boolean isSurfaceWorld() {
		return false;
	}
	
	@Override
	protected void registerWorldChunkManager() {
		worldChunkMgr = new DWWorldChunkManager(this);
		if(!worldObj.isRemote)
			teleporter = new DWTeleporter((WorldServer)worldObj);
	}
	
	@Override
	public void setAllowedSpawnTypes(boolean allowHostile, boolean allowPeaceful) {
		super.setAllowedSpawnTypes(false, false);
	}
	
	@Override
	public void updateWeather() {
	}
	
	@Override
	public boolean canSnowAt(int x, int y, int z, boolean checkLight) {
		return false;
	}
	
	@Override
	public boolean canBlockFreeze(int x, int y, int z, boolean byWater) {
		return false;
	}
	
	@Override
	public boolean canMineBlock(EntityPlayer player, int x, int y, int z) {
		return super.canMineBlock(player, x, y, z);
	}
	
	@Override
	public float calculateCelestialAngle(long par1, float par3) {
		return super.calculateCelestialAngle(par1, par3);
	}
	
	@Override
	public void calculateInitialWeather() {
	}

}
