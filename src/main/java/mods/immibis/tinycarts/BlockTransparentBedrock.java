package mods.immibis.tinycarts;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public class BlockTransparentBedrock extends Block {
	public BlockTransparentBedrock() {
		super(Material.rock);
		
		setBlockUnbreakable();
		setResistance(6000000.0F);
		setStepSound(soundTypeStone);
		setBlockName("tinycarts.transparent_bedrock");
		disableStats();
		setBlockTextureName("bedrock");
	}
	
	@Override
	public boolean getUseNeighborBrightness() {
		return true;
	}
	
	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World par1World, int par2, int par3, int par4) {
		final float border = 1/16f;
		return AxisAlignedBB.getBoundingBox((double)par2 + border, (double)par3 + this.minY, (double)par4 + border, (double)par2 + 1-border, (double)par3 + 1-border, (double)par4 + 1-border);
	}
	
	@Override
	public void onEntityCollidedWithBlock(World par1World, int par2, int par3, int par4, Entity par5Entity) {
		if(par5Entity instanceof EntityPlayerMP)
			TinyCartsMod.removeFromCart(par5Entity, false);
	}
	
	// render in no passes = invisible
	@Override
	public boolean canRenderInPass(int pass) {
		return false;
	}
	
	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}
	
	@Override
	public boolean isOpaqueCube() {
		return false;
	}
	
	@Override
	public void breakBlock(World par1World, int par2, int par3, int par4, Block par5, int par6) {
		if(allowRemoval.get() > 0)
			return;
		
		allowPlacement.incrementAndGet();
		try {
			par1World.setBlock(par2, par3, par4, par5);
		} finally {
			allowPlacement.decrementAndGet();
		}
	}
	
	@Override
	public void onBlockAdded(World par1World, int par2, int par3, int par4) {
		if(allowPlacement.get() > 0)
			return;
		
		allowRemoval.incrementAndGet();
		try {
			par1World.setBlockToAir(par2, par3, par4);
		} finally {
			allowRemoval.decrementAndGet();
		}
	}
	
	static AtomicInteger allowPlacement = new AtomicInteger();
	static AtomicInteger allowRemoval = new AtomicInteger();
}
