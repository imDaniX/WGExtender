/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package wgextender;

import org.bukkit.Server;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;
import wgextender.config.Config;
import wgextender.features.claimcommand.BlockLimitsHandler;
import wgextender.features.claimcommand.WGRegionCommandWrapper;
import wgextender.features.extendedwand.WEWandCommandWrapper;
import wgextender.features.extendedwand.WEWandListener;
import wgextender.features.flags.ConsumeFlagsHandler;
import wgextender.features.flags.OldPVPFlagsHandler;
import wgextender.features.flags.WGExtenderFlags;
import wgextender.features.regionprotect.ownormembased.PvPHandlingListener;
import wgextender.features.regionprotect.ownormembased.RestrictCommands;
import wgextender.features.regionprotect.regionbased.BlockBurn;
import wgextender.features.regionprotect.regionbased.Explode;
import wgextender.features.regionprotect.regionbased.FireSpread;
import wgextender.features.regionprotect.regionbased.LiquidFlow;
import wgextender.integration.PapiIntegration;

import java.util.Objects;
import java.util.logging.Level;

public final class WGExtender extends JavaPlugin { // TODO Might wanna separate for the actual API
	private Config config;
	private BlockLimitsHandler claimLimitsHandler;

	private PvPHandlingListener pvpListener;
	private OldPVPFlagsHandler oldPvpHandler;

	@ApiStatus.Internal
	public @UnknownNullability Config getPluginConfig() {
		return config;
	}

	public @UnknownNullability BlockLimitsHandler getBlockLimitsHandler() {
		return claimLimitsHandler;
	}

	@ApiStatus.Internal
	@Override
	public void onLoad() {
		WGExtenderFlags.registerFlags(getLogger());
	}

	@ApiStatus.Internal
	@Override
	public void onEnable() {
		config = new Config(this);
		config.loadConfig();
		Server server = getServer();
		Objects.requireNonNull(getCommand("wgex")).setExecutor(new WGExCommand(this));
		registerListeners(claimLimitsHandler = new BlockLimitsHandler(config));
		registerListeners(new RestrictCommands(this));
		registerListeners(new LiquidFlow(config));
		registerListeners(new FireSpread(config));
		registerListeners(new BlockBurn(config));
		registerListeners(new Explode(config));
		registerListeners(new WEWandListener());
		registerListeners(new ConsumeFlagsHandler(config));
		try {
			WGRegionCommandWrapper.inject(this); // TODO This static call can be non-static
			WEWandCommandWrapper.inject(server, config); // TODO This static call can be non-static
			pvpListener = new PvPHandlingListener(config);
			pvpListener.inject(this);
			oldPvpHandler = new OldPVPFlagsHandler(this);
			if (config.miscOldPvpFlags) {
				getLogger().warning(
						"Enabling the old-PvP flags. Do note that they're not supported, " +
						"as they're very out of scope of extending WG capabilities and may harm performance. " +
						"Consider turning them off by setting 'misc.old-pvp-flags' to 'false'"
				);
				oldPvpHandler.start(this);
			}
		} catch (Throwable t) {
			getLogger().log(Level.SEVERE, "Unable to inject, shutting down", t);
			getServer().shutdown();
		}


		if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			new PapiIntegration(this).register();
		}
	}
	
	private void registerListeners(@NotNull Listener listener) {
		getServer().getPluginManager().registerEvents(listener, this);
	}

	@ApiStatus.Internal
	@Override
	public void onDisable() {
		try {
			WEWandCommandWrapper.uninject(getServer());
			WGRegionCommandWrapper.uninject(getServer());
			pvpListener.uninject();
			oldPvpHandler.stop(this);
		} catch (Throwable t) {
			if (getServer().isStopping()) {
				getLogger().log(Level.SEVERE, "Unable to uninject", t);
			} else {
				getLogger().log(Level.SEVERE, "Unable to uninject, shutting down", t);
				getServer().shutdown();
			}
		}
	}
}
