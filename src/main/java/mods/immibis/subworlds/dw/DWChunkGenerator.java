package mods.immibis.subworlds.dw;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

public class DWChunkGenerator implements IChunkProvider {
	
	public static final BiomeGenBase BIOME = BiomeGenBase.plains;
	
	public final DWWorldProvider provider;
	private final int HSIZE; // chunks

	private WorldServer worldObj;
	//private EmptyChunk emptyChunk;
	
	public DWChunkGenerator(WorldServer worldObj, DWWorldProvider provider) {
		this.worldObj = worldObj;
		this.provider = provider;
		this.HSIZE = Math.max(provider.props.zsize, provider.props.xsize) / 16;
		//emptyChunk = new EmptyChunk(worldObj, -10000, -10000);
	}

	@Override
	public boolean chunkExists(int var1, int var2) {
		return true;
	}

	@Override
	public Chunk provideChunk(int var1, int var2) {
		boolean outOfBounds = var1 < 0 || var2 < 0 || var1 >= HSIZE || var2 >= HSIZE;
		
		// if(outOfBounds) return emptyChunk;
		
		Chunk chunk = new Chunk(this.worldObj, var1, var2);
		
		if(!outOfBounds) {
		
			/*for(int y = 0; y < provider.props.vsize; y++) {
				if(var1 == 0 && var2 == 0)
					chunk.setBlockIDWithMetadata(0, y, 0, Block.stone.blockID, 0);
				if(var1 == HSIZE-1 && var2 == 0)
					chunk.setBlockIDWithMetadata(15, y, 0, Block.stone.blockID, 0);
				if(var1 == 0 && var2 == HSIZE-1)
					chunk.setBlockIDWithMetadata(0, y, 15, Block.stone.blockID, 0);
				if(var1 == HSIZE-1 && var2 == HSIZE-1)
					chunk.setBlockIDWithMetadata(15, y, 15, Block.stone.blockID, 0);
			}
			
			for(int x = 0; x < 16; x++) {
				if(var1 == 0) {
					chunk.setBlockIDWithMetadata(0, 0, x, Block.stone.blockID, 0);
					chunk.setBlockIDWithMetadata(0, provider.props.vsize-1, x, Block.stone.blockID, 0);
				}
				
				if(var1 == HSIZE-1) {
					chunk.setBlockIDWithMetadata(15, 0, x, Block.stone.blockID, 0);
					chunk.setBlockIDWithMetadata(15, provider.props.vsize-1, x, Block.stone.blockID, 0);
				}
				
				if(var2 == 0) {
					chunk.setBlockIDWithMetadata(x, 0, 0, Block.stone.blockID, 0);
					chunk.setBlockIDWithMetadata(x, provider.props.vsize-1, 0, Block.stone.blockID, 0);
				}
				if(var2 == HSIZE-1) {
					chunk.setBlockIDWithMetadata(x, 0, 15, Block.stone.blockID, 0);
					chunk.setBlockIDWithMetadata(x, provider.props.vsize-1, 15, Block.stone.blockID, 0);
				}
			}
			
			for(int x = 0; x < 16; x++)
				for(int z = 0; z < 16; z++)
					chunk.setBlockIDWithMetadata(x, 0, z, Block.stone.blockID, 0);*/
			
			
			int nx = provider.props.xsize/2 - var1 * 16;
			int nz = provider.props.zsize/2 - var2 * 16;
			
			for(int dx = 0; dx < 2; dx++)
				for(int dy = 0; dy < 2; dy++)
					for(int dz = 0; dz < 2; dz++) {
						int x = nx+dx, y = provider.props.ysize/2+dy, z = nz+dz;
						if(x >= 0 && y >= 0 && z >= 0 && x < 16 && y < provider.props.ysize && z < 16)
							chunk.func_150807_a(x, y, z, Blocks.stone, 0);
					}
		}

        chunk.generateSkylightMap();
        
        Arrays.fill(chunk.getBiomeArray(), (byte)BIOME.biomeID);
        
        chunk.isTerrainPopulated = true;
        
        return chunk;
	}

	@Override
	public Chunk loadChunk(int var1, int var2) {
		return provideChunk(var1, var2);
	}

	@Override
	public void populate(IChunkProvider var1, int var2, int var3) {
	}

	@Override
	public boolean saveChunks(boolean var1, IProgressUpdate var2) {
		return true;
	}

	@Override
	public boolean unloadQueuedChunks() {
		return false;
	}

	@Override
	public boolean canSave() {
		return true;
	}

	@Override
	public String makeString() {
		return "DWChunkGenerator";
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List getPossibleCreatures(EnumCreatureType var1, int var2, int var3, int var4) {
		return Collections.emptyList();
	}
	
	@Override
	public ChunkPosition func_147416_a(World var1, String var2, int var3, int var4, int var5) {
		return null;
	}

	@Override
	public int getLoadedChunkCount() {
		return 0;
	}

	@Override
	public void recreateStructures(int var1, int var2) {
	}

	@Override
	public void saveExtraData() {
	}

}
