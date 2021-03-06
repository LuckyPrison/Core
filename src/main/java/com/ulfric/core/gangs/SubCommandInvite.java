package com.ulfric.core.gangs;

import java.util.UUID;

import com.ulfric.lib.coffee.command.CommandSender;
import com.ulfric.lib.coffee.enums.EnumUtils;
import com.ulfric.lib.coffee.module.ModuleBase;
import com.ulfric.lib.craft.entity.player.OfflinePlayer;
import com.ulfric.lib.craft.entity.player.Player;

public class SubCommandInvite extends InviteGangCommand {

	public SubCommandInvite(ModuleBase owner)
	{
		super("invite", owner);
	}

	@Override
	public void run()
	{
		CommandSender sender = this.getSender();

		OfflinePlayer offlinePlayer = (OfflinePlayer) this.getObject(OfflinePlayer.ARGUMENT.getPath());
		Player onlinePlayer = offlinePlayer.toPlayer();
		UUID targetUUID = offlinePlayer.getUniqueId();

		Gang gang = this.getGang();

		GangMember member = gang.getMember(targetUUID);

		if (member != null)
		{
			sender.sendLocalizedMessage("gangs-invite-already-member", member.getName(), EnumUtils.format(member.getRank()));

			return;
		}

		if (gang.isInvited(targetUUID))
		{
			sender.sendLocalizedMessage("gangs-invite-already-invited", offlinePlayer.getName());

			return;
		}

		gang.addInvite(targetUUID);

		sender.sendLocalizedMessage("gangs-invited-success", offlinePlayer.getName());

		if (onlinePlayer == null) return;

		onlinePlayer.sendLocalizedMessage("gangs-invited-to", sender.getName(), gang.getName());
	}

}