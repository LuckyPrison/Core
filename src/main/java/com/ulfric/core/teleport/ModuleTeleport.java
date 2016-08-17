package com.ulfric.core.teleport;

import com.ulfric.lib.coffee.module.Module;

public final class ModuleTeleport extends Module {

	public ModuleTeleport()
	{
		super("teleport", "Teleport parent module", "1.0.0", "Packet");
	}

	@Override
	public void onFirstEnable()
	{
		this.addCommand(new CommandTeleport(this));

		this.addModule(new ModuleSpawn());
	}

}