package mods.immibis.tinycarts;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import mods.immibis.subworlds.dw.DWWorldProvider;
import mods.immibis.subworlds.dw.WorldProps;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

public class InteriorChunkGen implements IChunkProvider {

	private WorldProps props;
	private WorldServer w;
	
	public InteriorChunkGen(WorldServer w, DWWorldProvider wp) {
		this.props = wp.props;
		this.w = w;
	}
	
	@Override
	public Chunk provideChunk(int i, int j) {
		Chunk c = new Chunk(w, i, j);
		
		BlockTransparentBedrock.allowPlacement.incrementAndGet();
		
		try {
			if(i == 0 && j == 0) {
				for(int x = 0; x < props.xsize; x++)
					for(int z = 0; z < props.zsize; z++) {
						c.func_150807_a(x, 0, z, TinyCartsMod.transparentBedrock, 0);
						c.func_150807_a(x, props.ysize-1, z, TinyCartsMod.transparentBedrock, 0);
						c.func_150807_a(x, 1, z, Blocks.planks, 0);
					}
				
				for(int x = 0; x < props.xsize; x++)
					for(int y = 0; y < props.ysize; y++) {
						c.func_150807_a(x, y, 0, TinyCartsMod.transparentBedrock, 0);
						c.func_150807_a(x, y, props.zsize-1, TinyCartsMod.transparentBedrock, 0);
						c.func_150807_a(0, y, x, TinyCartsMod.transparentBedrock, 0);
						c.func_150807_a(props.xsize-1, y, x, TinyCartsMod.transparentBedrock, 0);
					}
			}
		} finally {
			BlockTransparentBedrock.allowPlacement.decrementAndGet();
		}
		
		c.generateSkylightMap();
        Arrays.fill(c.getBiomeArray(), (byte)BiomeGenBase.plains.biomeID);
        c.isTerrainPopulated = true;
		
		return c;
	}

	@Override
	public boolean chunkExists(int i, int j) {
		return true;
	}

	@Override
	public Chunk loadChunk(int i, int j) {
		return provideChunk(i, j);
	}

	@Override
	public void populate(IChunkProvider ichunkprovider, int i, int j) {
		
	}

	@Override
	public boolean saveChunks(boolean flag, IProgressUpdate iprogressupdate) {
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
		return getClass().getName();
	}

	@Override
	public List<?> getPossibleCreatures(EnumCreatureType enumcreaturetype, int i, int j, int k) {
		return Collections.EMPTY_LIST;
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
	public void recreateStructures(int i, int j) {
	}

	@Override
	public void saveExtraData() {
	}
}
