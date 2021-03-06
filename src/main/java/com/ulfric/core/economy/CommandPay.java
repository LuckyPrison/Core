package com.ulfric.core.economy;

import java.util.UUID;

import com.ulfric.lib.coffee.command.Command;
import com.ulfric.lib.coffee.command.CommandSender;
import com.ulfric.lib.coffee.economy.Bank;
import com.ulfric.lib.coffee.economy.BankAccount;
import com.ulfric.lib.coffee.economy.Currency;
import com.ulfric.lib.coffee.economy.CurrencyAmount;
import com.ulfric.lib.coffee.economy.MoneyFormatter;
import com.ulfric.lib.coffee.economy.OfflineBankAccount;
import com.ulfric.lib.coffee.module.ModuleBase;
import com.ulfric.lib.craft.entity.player.OfflinePlayer;
import com.ulfric.lib.craft.entity.player.Player;

final class CommandPay extends Command {

	public CommandPay(ModuleBase module)
	{
		super("pay", module);
		this.addArgument(CurrencyAmount.ARGUMENT);
		this.addArgument(OfflinePlayer.ARGUMENT);
	}

	@Override
	public void run()
	{
		CommandSender sender = this.getSender();
		OfflinePlayer player = (OfflinePlayer) this.getObject("offline-player");

		if (player.getUniqueId().equals(sender.getUniqueId()))
		{
			sender.sendLocalizedMessage("economy-pay-specify-player-self");

			return;
		}

		CurrencyAmount amount = (CurrencyAmount) this.getObject("price");
		Currency currency = amount.getCurrency();

		String payeeName = player.getName();
		UUID uuid = sender.getUniqueId();

		if (uuid == null)
		{
			if (!sender.hasPermission("pay.console"))
			{
				sender.sendLocalizedMessage("economy-pay-cannot-charge");

				return;
			}
		}

		else
		{
			if (!currency.isPayable())
			{
				sender.sendLocalizedMessage("economy-currency-unpayable", currency.getName());

				return;
			}

			BankAccount senderAccount = Bank.getOnlineAccount(uuid);

			long senderBalance = senderAccount.getBalance(currency);

			long difference = amount.getAmount() - senderBalance;
			if (difference > 0)
			{
				sender.sendLocalizedMessage("economy-pay-cannot-afford", new MoneyFormatter(currency.getFormat(), difference).dualFormatWord());

				return;
			}

			senderAccount.take(amount, "Payment to " + payeeName);
		}

		String senderName = sender.getName();

		OfflineBankAccount recipientAccount = Bank.getAccount(player.getUniqueId());

		recipientAccount.give(amount, "Payment from " + senderName);

		String amtFormat = amount.toFormatter().dualFormatWord().toString();

		sender.sendLocalizedMessage("economy-payment-sent", payeeName, amtFormat);

		Player recipient = player.toPlayer();

		if (recipient == null) return;

		recipient.sendLocalizedMessage("economy-payment-received", senderName, amtFormat);
	}

}