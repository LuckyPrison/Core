package com.ulfric.core.combattag;

import com.ulfric.lib.coffee.command.Argument;
import com.ulfric.lib.coffee.command.Command;
import com.ulfric.lib.coffee.command.CommandSender;
import com.ulfric.lib.coffee.module.ModuleBase;
import com.ulfric.lib.craft.entity.player.Player;
import com.ulfric.lib.craft.entity.player.PlayerUtils;

class CommandCombatTag extends Command {

	CommandCombatTag(ModuleBase module)
	{
		super("combattag", module, "ct");

		this.addArgument(Argument.builder().setPath("player").addResolver(PlayerUtils::getOnlinePlayer).setDefaultValue(cmd -> PlayerUtils.getPlayer(cmd.getSender())).build());
	}

	@Override
	public void run()
	{
		CommandSender sender = this.getSender();
		Player player = (Player) this.getObject("player");

		CombatTag tag = Tags.INSTANCE.getTag(player.getUniqueId());

		if (tag == null)
		{
			if (player.getUniqueId().equals(sender.getUniqueId()))
			{
				sender.sendLocalizedMessage("combattag.check_self_not_tagged");

				return;
			}

			sender.sendLocalizedMessage("combattag.check_other_not_tagged", player.getName());

			return;
		}

		if (player.getUniqueId().equals(sender.getUniqueId()))
		{
			sender.sendLocalizedMessage("combattag.check_self_tagged");

			return;
		}

		sender.sendLocalizedMessage("combattag.check_other_tagged", player.getName());
	}

}