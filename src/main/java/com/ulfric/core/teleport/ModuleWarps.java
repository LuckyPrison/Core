package com.ulfric.core.teleport;

import java.util.Map;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.StringUtils;

import com.ulfric.config.ConfigFile;
import com.ulfric.config.MutableDocument;
import com.ulfric.lib.coffee.command.Argument;
import com.ulfric.lib.coffee.command.Command;
import com.ulfric.lib.coffee.command.CommandSender;
import com.ulfric.lib.coffee.command.Resolvers;
import com.ulfric.lib.coffee.module.Module;
import com.ulfric.lib.coffee.string.NamedBase;
import com.ulfric.lib.craft.entity.player.Player;
import com.ulfric.lib.craft.location.Destination;
import com.ulfric.lib.craft.location.Location;

final class ModuleWarps extends Module {

	private static final ModuleWarps INSTANCE = new ModuleWarps();

	public static ModuleWarps getInstance()
	{
		return ModuleWarps.INSTANCE;
	}

	private ModuleWarps()
	{
		super("warps", "Warping n' stuff", "1.0.0", "Packet");
	}

	public Destination getWarp(String name)
	{
		Warp warp = this.map.get(name);

		if (warp == null) return null;

		return warp.getDestination();
	}

	Warp getWarp(CommandSender sender, String name)
	{
		String lower = name.toLowerCase();

		Warp found = this.map.get(lower);

		if (found != null)
		{
			if (sender.hasPermission("warps." + found.getName()))
			{
				return found;
			}
		}

		int lowest = 4;

		for (Map.Entry<String, Warp> entry : this.map.entrySet())
		{
			Warp warp = entry.getValue();

			if (!sender.hasPermission("warps." + warp.getName())) continue;

			String path = entry.getKey();

			int distance = StringUtils.getLevenshteinDistance(lower, path, lowest);

			if (distance == 0) return warp;

			if (distance == -1) continue;

			lowest = distance;
			found = warp;
		}

		return found;
	}

	private static final class Warp extends NamedBase
	{
		Warp(String name, Location location, int delay, ConfigFile file)
		{
			super(name);
			this.file = file;
			this.destination = Destination.newDestination(location, delay);
		}

		Warp(ConfigFile file, MutableDocument root)
		{
			super(root.getString("name").trim());
			this.file = file;
			this.destination = Destination.fromDocument(root.getDocument("destination"));
		}

		private final Destination destination;
		private final ConfigFile file;

		public Destination getDestination()
		{
			return this.destination;
		}

		public void accept(Player player, boolean attemptRelative)
		{
			this.destination.accept(player, attemptRelative);
		}

		public void save()
		{
			this.file.getRoot().set("destination", this.destination.toDocument());
			this.file.save();
		}
	}

	Map<String, Warp> map;

	@Override
	public void onFirstEnable()
	{
		this.map = new CaseInsensitiveMap<>();

		this.addCommand(new CommandWarp());
		this.addCommand(new CommandSetWarp());
	}

	@Override
	public void onModuleEnable()
	{
		ConfigFile mainConfig = this.getModuleConfig();

		int count = 0;

		for (ConfigFile config : this.getModuleConfigs())
		{
			if (config == mainConfig) continue;

			Warp warp = new Warp(config, config.getRoot());

			if (this.map.putIfAbsent(warp.getName(), warp) != null) continue;

			count++;
		}

		this.log("Loaded " + count + " warps from the disk");
	}

	@Override
	public void onModuleDisable()
	{
		this.map.clear();
	}

	private static final class CommandWarp extends Command
	{
		public CommandWarp()
		{
			super("warp", ModuleWarps.getInstance());

			this.addArgument(Argument.builder().setPath("warp").addResolver(ModuleWarps.getInstance()::getWarp).setUsage("warp.specify_warp").build());
		}

		@Override
		public void run()
		{
			Warp warp = (Warp) this.getObject("warp");

			warp.accept((Player) this.getSender(), false);
		}
	}

	private static final class CommandSetWarp extends Command
	{
		public CommandSetWarp()
		{
			super("setwarp", ModuleWarps.getInstance());

			this.addArgument(Argument.builder().setPath("name").addSimpleResolver(str -> ModuleWarps.getInstance().getWarp(str) == null ? str : null).setUsage("warp.set_specify_unique_name").build());
			this.addOptionalArgument(Argument.builder().setPath("delay").addResolver(Resolvers.INTEGER).build());
		}

		@Override
		public void run()
		{
			String name = (String) this.getObject("name");
			int delay = (int) this.getObject("delay", 5);

			ModuleWarps warps = ModuleWarps.getInstance();

			ConfigFile file = warps.getModuleConfig(name);

			Warp warp = new Warp(name.trim(), ((Player) this.getSender()).getLocation(), delay, file);

			warps.map.put(warp.getName(), warp);

			warp.save();

			this.getSender().sendLocalizedMessage("warps.set_warp", warp.getName(), warp.getDestination().getDelay());
		}
	}

}