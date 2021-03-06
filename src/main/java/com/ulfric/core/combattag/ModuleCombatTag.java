package com.ulfric.core.combattag;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.ulfric.config.ConfigFile;
import com.ulfric.config.MutableDocument;
import com.ulfric.core.achievement.Achievement;
import com.ulfric.core.achievement.Categories;
import com.ulfric.core.achievement.Category;
import com.ulfric.lib.coffee.event.Event;
import com.ulfric.lib.coffee.event.Handler;
import com.ulfric.lib.coffee.event.Listener;
import com.ulfric.lib.coffee.module.Module;
import com.ulfric.lib.coffee.string.WordUtils;
import com.ulfric.lib.coffee.time.TimeUtils;
import com.ulfric.lib.craft.block.MaterialData;
import com.ulfric.lib.craft.entity.player.GameMode;
import com.ulfric.lib.craft.entity.player.Player;
import com.ulfric.lib.craft.entity.player.PlayerUtils;
import com.ulfric.lib.craft.event.player.PlayerDamagePlayerEvent;
import com.ulfric.lib.craft.event.player.PlayerDeathEvent;
import com.ulfric.lib.craft.event.player.PlayerFutureTeleportEvent;
import com.ulfric.lib.craft.event.player.PlayerQuitEvent;
import com.ulfric.lib.craft.event.player.PlayerTeleportEvent;
import com.ulfric.lib.craft.inventory.item.Material;

public class ModuleCombatTag extends Module {

	public ModuleCombatTag()
	{
		super("combat-tag", "Prevent players from logging out in combat", "1.0.0", "Packet");
	}

	Set<GameMode> ignoreGameModes;
	long ticks;
	long seconds;
	String permission;

	@Override
	public void onModuleEnable()
	{
		ConfigFile config = this.getModuleConfig();
		MutableDocument document = config.getRoot();
		this.ticks = document.getInteger("ticks", 20 * 15);
		this.seconds = this.ticks / 20;
		this.permission = document.getString("permission", "combattag.ignore");
		this.ignoreGameModes.addAll(document.getStringList("ignore-game-modes", ImmutableList.of()).stream().map(GameMode::of).filter(Objects::nonNull).collect(Collectors.toList()));
		config.save();

		this.log("Tag time (in ticks): " + this.ticks);
		this.log("Tag time (in seconds): " + this.seconds);
		this.log("Bypass permission: " + this.permission);
		this.log("Ignoring gamemodes: " + WordUtils.merge(this.ignoreGameModes));
	}

	@Override
	public void onFirstEnable()
	{
		this.ignoreGameModes = Sets.newHashSet();

		this.addCommand(new CommandCombatTag(this));
		this.addCommand(new CommandUnCombatTag(this));

		this.addListener(new Listener(this)
		{
			@Handler(ignoreCancelled = true)
			public void onPvP(PlayerDamagePlayerEvent event)
			{
				this.tag(event.getPlayer(), event.getDamagedEntity());
			}

			// TODO more causes for tagging (like tnt explosions)

			private void tag(Player damager, Player damaged)
			{
				if (ModuleCombatTag.this.ignoreGameModes.contains(damager.getGameMode())) return;

				if (damaged.hasPermission(ModuleCombatTag.this.permission)) return;

				UUID damagerUUID = damager.getUniqueId();
				UUID damagedUUID = damaged.getUniqueId();

				if (!Tags.INSTANCE.createTag(damagerUUID, damagedUUID, ModuleCombatTag.this.ticks))
				{
					damager.sendLocalizedMessage("combattag-tagged-you-attacked", damaged.getName(), ModuleCombatTag.this.seconds);
				}

				if (Tags.INSTANCE.createTag(damagedUUID, damagerUUID, ModuleCombatTag.this.ticks)) return;

				damaged.sendLocalizedMessage("combattag-tagged-damaged", damager.getName(), ModuleCombatTag.this.seconds);
			}

			@Handler
			public void onDeath(PlayerDeathEvent event)
			{
				Tags.INSTANCE.removeTag(event.getPlayer().getUniqueId());
			}

			@Handler(ignoreCancelled = true)
			public void onTryTeleport(PlayerFutureTeleportEvent event)
			{
				this.teleportation(event.getPlayer(), event);
			}

			@Handler(ignoreCancelled = true)
			public void onTeleport(PlayerTeleportEvent event)
			{
				this.teleportation(event.getPlayer(), event);
			}

			private void teleportation(Player player, Event event)
			{
				CombatTag tag = Tags.INSTANCE.getTag(player.getUniqueId());

				if (tag == null) return;

				player.sendLocalizedMessage("combattag-no-teleport");

				event.setCancelled(true);
			}

			@Handler
			public void onLeave(PlayerQuitEvent event)
			{
				Player player = event.getPlayer();

				CombatTag tag = Tags.INSTANCE.removeTag(player.getUniqueId());

				if (tag == null) return;

				Validate.isTrue(tag.getTask().isIncomplete());

				PlayerKilledByCombatTagEvent call = new PlayerKilledByCombatTagEvent(player, tag);

				call.fire();

				if (call.isCancelled()) return;

				player.kill();

				Player tagger = PlayerUtils.getOnlinePlayer(tag.getTagger());

				if (tagger == null) return;

				long remaining = System.currentTimeMillis() - tag.getCreated();

				tagger.sendLocalizedMessage("combattag-killed-enemy", player.getName(), TimeUtils.formatMillis(remaining, TimeUnit.MILLISECONDS));

				// TODO is it possible to kick a player as they're disconnecting, and show them the kick page? Glitch if so, but seems plausible
			}
		});

		Category category = Categories.INSTANCE.getByName("combat");

		if (category == null)
		{
			category = Category.builder().setName("Combat").setItem(MaterialData.of("DIAMOND_SWORD")).build();

			Categories.INSTANCE.register(category);
		}

		Achievement achievement = Achievement.builder().setCode("combattag").setName("Angel of Death").setDescription("Combat Tag someone, so that they log out and die").setItem(MaterialData.of(Material.of("REDSTONE_DUST"))).build();

		category.addAchievement(achievement);
	}

	@Override
	public void onModuleDisable()
	{
		Tags.INSTANCE.clear();
		this.ignoreGameModes.clear();
	}

}