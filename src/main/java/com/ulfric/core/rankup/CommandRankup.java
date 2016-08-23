package com.ulfric.core.rankup;

import com.ulfric.lib.coffee.command.Command;
import com.ulfric.lib.coffee.economy.Bank;
import com.ulfric.lib.coffee.economy.BankAccount;
import com.ulfric.lib.coffee.economy.CurrencyAmount;
import com.ulfric.lib.coffee.economy.MoneyFormatter;
import com.ulfric.lib.coffee.module.ModuleBase;
import com.ulfric.lib.craft.command.Enforcers;
import com.ulfric.lib.craft.entity.player.Player;

public class CommandRankup extends Command {

	public CommandRankup(ModuleBase owner)
	{
		super("rankup", owner);

		this.addEnforcer(Enforcers.IS_PLAYER, "rankup.must_be_player");
	}

	@Override
	public void run()
	{
		Player player = (Player) this.getSender();

		Rankup rankup = Rankups.INSTANCE.getActive(player);

		if (rankup == null)
		{
			player.sendLocalizedMessage("rankup.no_rank_found");

			return;
		}

		CurrencyAmount cost = rankup.getCost();

		if (cost != null)
		{
			BankAccount account = Bank.getOnlineAccount(player.getUniqueId());

			final long balance = account.getBalance(cost.getCurrency());

			long costAmount = cost.getAmount();

			if (balance < costAmount)
			{
				player.sendLocalizedMessage("rankup.cannot_afford", new MoneyFormatter(costAmount - balance).dualFormatWord(), rankup.getNext().getDisplayName());

				return;
			}

			account.take(cost, "Rankup to " + rankup.getNext().getDisplayName());
		}

		player.swapParents(rankup.getOld(), rankup.getOld());

		player.sendLocalizedMessage("rankup.ranked_up", rankup.getNext().getName());

		// TODO fireworks - 
	}

}