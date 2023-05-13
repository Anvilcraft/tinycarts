package mods.immibis.tinycarts;

import mods.immibis.subworlds.dw.DWWorldProvider;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

public class ItemMinecartAwesome extends Item {
    public ItemMinecartAwesome()
    {
        this.maxStackSize = 1;
        
        this.setCreativeTab(CreativeTabs.tabTransport);
        this.setTextureName("tinycarts:minecart");
        this.setUnlocalizedName("tinycarts.cart");
    }

    @Override
	public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int par4, int par5, int par6, int par7, float par8, float par9, float par10)
    {
    	if(par3World.provider instanceof DWWorldProvider) {
    		if(par3World.isRemote)
    			par2EntityPlayer.addChatMessage(new ChatComponentText("Don't even think about it."));
    		return false;
    	}

        Block i1 = par3World.getBlock(par4, par5, par6);

        if (BlockRailBase.func_150051_a(i1))
        {
            if (!par3World.isRemote)
            {
                EntityMinecart entityminecart = new EntityMinecartAwesome(par3World, (double)((float)par4 + 0.5F), (double)((float)par5 + 0.5F), (double)((float)par6 + 0.5F));

                if (par1ItemStack.hasDisplayName())
                {
                    entityminecart.setMinecartName(par1ItemStack.getDisplayName());
                }

                par3World.spawnEntityInWorld(entityminecart);
            }

            --par1ItemStack.stackSize;
            return true;
        }
        else
        {
            return false;
        }
    }
}
