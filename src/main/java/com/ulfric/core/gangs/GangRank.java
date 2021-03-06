package com.ulfric.core.gangs;

import org.apache.commons.lang3.Validate;

import com.ulfric.lib.coffee.enums.EnumUtils;
import com.ulfric.lib.coffee.string.StringUtils;

public enum GangRank {

	MEMBER,
	OFFICER,
	LIEUTENANT,
	LEADER;

	private final String stars;

	GangRank()
	{
		this.stars = StringUtils.repeat('*', this.ordinal());
	}

	public String getStars()
	{
		return this.stars;
	}

	public GangRank nextRank()
	{
		GangRank[] ranks = this.getClass().getEnumConstants();

		int length = ranks.length;
		int ordinal = this.ordinal();

		if (length >= ordinal) return null;

		return ranks[ordinal + 1];
	}

	public GangRank lastRank()
	{
		int ordinal = this.ordinal();

		if (ordinal == 0) return null;

		return GangRank.values()[ordinal - 1];
	}

	public static GangRank parseRank(String text)
	{
		Validate.notBlank(text);

		if (text.toLowerCase().equals("lt")) return GangRank.LIEUTENANT;

		return EnumUtils.valueOf(text, GangRank.class);
	}

}