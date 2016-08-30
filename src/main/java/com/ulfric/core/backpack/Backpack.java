package com.ulfric.core.backpack;

import com.google.common.collect.Maps;
import com.ulfric.config.Document;
import com.ulfric.config.MutableDocument;
import com.ulfric.config.SimpleDocument;
import com.ulfric.data.DataContainer;
import com.ulfric.lib.coffee.numbers.NumberUtils;
import com.ulfric.lib.craft.entity.player.OfflinePlayer;
import com.ulfric.lib.craft.entity.player.Player;
import com.ulfric.lib.craft.inventory.Inventory;
import com.ulfric.lib.craft.inventory.InventoryUtils;
import com.ulfric.lib.craft.inventory.item.ItemParts;
import com.ulfric.lib.craft.inventory.item.ItemStack;
import com.ulfric.lib.craft.inventory.item.ItemUtils;
import com.ulfric.lib.craft.inventory.item.Material;

import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

class Backpack {

	private static final int BACKPACK_SIZE = 36;

	private final OfflinePlayer owner;

	private final Map<Integer, Inventory> pageStorage = Maps.newHashMap();
	private final DataContainer<UUID, Document> dataContainer;

	private int maxPage;

	Backpack(OfflinePlayer owner, int initialPage, int maxPage)
	{
		this.owner = owner;
		this.dataContainer = ModuleBackpack.getInstance().getSubscription().get(this.owner.getUniqueId());

		this.maxPage = Math.max(maxPage, 1);
	}

	private void loadPage(int page, Inventory contents)
	{
		this.pageStorage.put(page, contents);
	}

	protected void updatePage(int page, Inventory contents)
	{
		Inventory fixedContents = InventoryUtils.newInventory(BACKPACK_SIZE, "");

		IntStream.range(0, BACKPACK_SIZE).forEach(slot -> fixedContents.setItem(slot, contents.getItem(slot)));

		this.pageStorage.put(page, fixedContents);
	}

	protected Inventory getContents(int page)
	{
		return this.pageStorage.get(page);
	}

	public boolean inBounds(int page)
	{
		return page >= 1 && page <= this.maxPage;
	}

	protected void save()
	{
		Document document = this.dataContainer.getValue();

		MutableDocument mut = new SimpleDocument(document.deepCopy());
		this.into(mut);

		dataContainer.setValue(mut);
	}

	protected OfflinePlayer getOwner()
	{
		return owner;
	}

	public int getMaxPage()
	{
		return maxPage;
	}

	public void open(Player viewer, int page)
	{
		new BackpackPage(ModuleBackpack.getInstance(), this, viewer, page).open();
	}

	protected void into(MutableDocument document)
	{
		document.set("max", this.maxPage);

		MutableDocument pages = document.getDocument("pages");

		if (pages == null)
		{
			pages = document.createDocument("pages");
		}

		MutableDocument finalPages = pages;
		this.pageStorage.keySet().forEach(page ->
		{
			MutableDocument current = finalPages.getDocument(String.valueOf(page));

			if (current == null)
			{
				current = finalPages.createDocument(String.valueOf(page));
			}

			Inventory inventory = this.pageStorage.get(page);

			MutableDocument finalCurrent = current;
			IntStream.range(0, inventory.getSize()).forEach(slot ->
			{
				ItemStack item = inventory.getItem(slot);

				if (item == null)
				{
					item = ItemUtils.getItem(Material.of("AIR"));
				}

				finalCurrent.set(String.valueOf(slot), ItemParts.itemToString(item));
			});

		});

	}

	protected static Backpack fromDocument(OfflinePlayer owner, Document document)
	{
		int maxPage = document.getInteger("max");

		Backpack backpack = new Backpack(owner, 1, maxPage);

		Document pages = document.getDocument("pages");

		pages.getKeys().forEach(key ->
		{
			Integer pageNumber = NumberUtils.parseInteger(key);

			Document page = pages.getDocument(String.valueOf(pageNumber));

			Inventory inventory = InventoryUtils.newInventory(BACKPACK_SIZE, "");

			IntStream.range(0, BACKPACK_SIZE).forEach(slot ->
			{
				ItemStack item = ItemParts.stringToItem(page.getString(String.valueOf(slot)));

				inventory.setItem(slot, item);
			});

			backpack.loadPage(pageNumber, inventory);
		});

		return backpack;
	}
}
