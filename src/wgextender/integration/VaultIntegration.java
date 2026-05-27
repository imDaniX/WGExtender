package wgextender.integration;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import wgextender.WGExtender;

public final class VaultIntegration implements Listener {
	private static final VaultIntegration INSTANCE = new VaultIntegration();

	public static VaultIntegration getInstance() {
		return INSTANCE;
	}

	private Permission permissions;

	private VaultIntegration() { }

	public Permission getPermissions() {
		return permissions;
	}

	public void initialize(WGExtender plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		hook(plugin.getServer());
	}

	private void hook(Server server) {
		try {
			permissions = server.getServicesManager().getRegistration(Permission.class).getProvider();
			if (!permissions.hasGroupSupport()) {
				throw new IllegalStateException();
			}
		} catch (Exception e) {
			permissions = null;
		}
	}

	@EventHandler
	public void onPluginEnable(PluginEnableEvent event) {
		hook(Bukkit.getServer());
	}

	@EventHandler
	public void onPluginDisable(PluginDisableEvent event) {
		hook(Bukkit.getServer());
	}
}
