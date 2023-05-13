package mods.immibis.subworlds.dw;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.world.ChunkPosition;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.gen.layer.GenLayer;

@SuppressWarnings("rawtypes")
public class DWWorldChunkManager extends WorldChunkManager {
	public final DWWorldProvider provider;
	
	public DWWorldChunkManager(DWWorldProvider provider) {
		this.provider = provider;
	}
	
	@Override
	public boolean areBiomesViable(int par1, int par2, int par3, List par4List) {
		return par4List.contains(DWChunkGenerator.BIOME);
	}
	
	@Override
	public ChunkPosition findBiomePosition(int par1, int par2, int par3, List par4List, Random par5Random) {
		if(par4List.contains(DWChunkGenerator.BIOME))
			return new ChunkPosition(provider.props.xsize/2, provider.props.ysize/2, provider.props.zsize/2);
		return null;
	}
	
	@Override
	public BiomeGenBase[] getBiomeGenAt(BiomeGenBase[] ar, int par2, int par3, int par4, int par5, boolean par6) {
		if (ar == null || ar.length < par4 * par5)
            ar = new BiomeGenBase[par4 * par5];
		
		Arrays.fill(ar, DWChunkGenerator.BIOME);
		
		return ar;
	}
	
	@Override
	public BiomeGenBase getBiomeGenAt(int par1, int par2) {
		return DWChunkGenerator.BIOME;
	}
	
	@Override
	public BiomeGenBase[] getBiomesForGeneration(BiomeGenBase[] ar, int par2, int par3, int par4, int par5) {
		if (ar == null || ar.length < par4 * par5)
            ar = new BiomeGenBase[par4 * par5];
		
		Arrays.fill(ar, DWChunkGenerator.BIOME);
		
		return ar;
	}
	
	@Override
	public List getBiomesToSpawnIn() {
		return Collections.singletonList(DWChunkGenerator.BIOME);
	}
	
	@Override
	public GenLayer[] getModdedBiomeGenerators(WorldType worldType, long seed, GenLayer[] original) {
		return original;
	}
	
	@Override
	public float[] getRainfall(float[] ar, int par2, int par3, int par4, int par5) {
		if(ar == null || ar.length < par4 * par5)
			ar = new float[par4 * par5];
		
		Arrays.fill(ar, DWChunkGenerator.BIOME.rainfall);
		
		return ar;
	}
	
	@Override
	public float getTemperatureAtHeight(float par1, int par2) {
		return par1;
	}
}
