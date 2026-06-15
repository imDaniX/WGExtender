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

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;
import wgextender.config.ConfigurationProvider;
import wgextender.features.claimcommand.BlockLimitsHandler;
import wgextender.features.claimcommand.WGRegionCommandWrapper;
import wgextender.features.extendedwand.WEWandCommandWrapper;
import wgextender.features.extendedwand.WEWandHandler;
import wgextender.features.flags.ConsumeFlagsHandler;
import wgextender.features.flags.MobRenameFlagHandler;
import wgextender.features.flags.OldPVPFlagsHandler;
import wgextender.features.flags.WGExtenderFlags;
import wgextender.features.regionprotect.ownormembased.PvPHandlingListener;
import wgextender.features.regionprotect.ownormembased.RestrictCommandsHandler;
import wgextender.features.regionprotect.regionbased.Explode;
import wgextender.features.regionprotect.regionbased.FireBurn;
import wgextender.features.regionprotect.regionbased.LiquidFlow;
import wgextender.integration.LpIntegration;
import wgextender.integration.PapiIntegration;
import wgextender.integration.PluginIntegration;
import wgextender.utils.CommandWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public final class WGExtender extends JavaPlugin { // TODO Might wanna separate for the actual API?
	private final List<PluginIntegration> integrations = new ArrayList<>();
	private final List<CommandWrapper> commandWrappers = new ArrayList<>();
	private ConfigurationProvider cfgProvider;
	private BlockLimitsHandler blockLimitsHandler;

	private PvPHandlingListener pvpListener;
	private OldPVPFlagsHandler oldPvpHandler;

	@ApiStatus.Internal
	public @UnknownNullability ConfigurationProvider getConfigurationProvider() {
		return cfgProvider;
	}

	public @UnknownNullability BlockLimitsHandler getBlockLimitsHandler() {
		return blockLimitsHandler;
	}

	@ApiStatus.Internal
	@Override
	public void onLoad() {
		WGExtenderFlags.registerFlags(getLogger());
		integrations.add(new LpIntegration());
		integrations.add(new PapiIntegration(this));
	}

	@ApiStatus.Internal
	@Override
	public void onEnable() {
		cfgProvider = new ConfigurationProvider(this);
		cfgProvider.reload();
		Objects.requireNonNull(getCommand("wgex")).setExecutor(new WGExCommand(this));
		registerListeners(blockLimitsHandler = new BlockLimitsHandler(cfgProvider));
		registerListeners(new MobRenameFlagHandler(cfgProvider));
		registerListeners(new RestrictCommandsHandler(this));
		registerListeners(new LiquidFlow(cfgProvider));
		registerListeners(new FireBurn(cfgProvider));
		registerListeners(new Explode(cfgProvider));
		registerListeners(new WEWandHandler());
		registerListeners(new ConsumeFlagsHandler(cfgProvider));
		try {
			commandWrappers.add(new WGRegionCommandWrapper(this));
			commandWrappers.add(new WEWandCommandWrapper(getServer(), cfgProvider));
			commandWrappers.forEach(CommandWrapper::inject);
			pvpListener = new PvPHandlingListener(cfgProvider);
			pvpListener.inject(this);
			oldPvpHandler = new OldPVPFlagsHandler(this);
			if (cfgProvider.misc().oldPvpFlags()) {
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

		var pluginManager = getServer().getPluginManager();
		for (PluginIntegration integration : integrations) {
			boolean enable = true;
			for (var pluginName : integration.requiredPlugins()) {
				if (!pluginManager.isPluginEnabled(pluginName)) {
					enable = false;
					break;
				}
			}
			if (enable) {
				getLogger().info("Enabling " + integration.requiredPlugins() + " integration");
				integration.onEnable(this);
			}
		}
	}
	
	private void registerListeners(@NotNull Listener listener) {
		getServer().getPluginManager().registerEvents(listener, this);
	}

	@ApiStatus.Internal
	@Override
	public void onDisable() {
		try {
			commandWrappers.forEach(CommandWrapper::uninject);
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
