package mods.immibis.tinycarts;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

public class CommandExitCart extends CommandBase {
	@Override
	public boolean canCommandSenderUseCommand(ICommandSender par1iCommandSender) {
		return par1iCommandSender instanceof EntityPlayer;
	}
	
	@Override
	public String getCommandName() {
		return "exitcart";
	}

	@Override
	public String getCommandUsage(ICommandSender icommandsender) {
		return "/exitcart";
	}

	@Override
	public void processCommand(ICommandSender icommandsender, String[] astring) {
		if(icommandsender instanceof EntityPlayer)
			TinyCartsMod.removeFromCart((EntityPlayer)icommandsender, true);
		else
			icommandsender.addChatMessage(new ChatComponentText("That command is only applicable to players."));
	}
}
