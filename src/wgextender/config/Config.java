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

package wgextender.config;

import com.sk89q.worldguard.protection.flags.Flag;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import wgextender.WGExtender;
import wgextender.config.message.Messages;
import wgextender.utils.WGUtils;

import java.io.File;
import java.math.BigInteger;
import java.util.*;

public final class Config {
	private final Plugin plugin;
	private final File configFile;
	private final Messages msg;

	public Config(WGExtender plugin) {
		this.plugin = plugin;
		configFile = new File(plugin.getDataFolder(), "config.yml");
		msg = new Messages(plugin.getDataFolder());
	}

	public boolean claimExpandSelectionVertical = false;

	public boolean claimBlockLimitsEnabled = false;
	public Map<String, BigInteger> claimBlockLimits = new LinkedHashMap<>();
	public BigInteger claimBlockLimitDefault = BigInteger.ZERO;
	public BigInteger claimBlockMinimalVolume = BigInteger.ZERO;
	public BigInteger claimBlockMinimalHorizontal = BigInteger.ZERO;
	public BigInteger claimBlockMinimalVertical = BigInteger.ZERO;

	public boolean checkLavaFlow = false;
	public boolean checkWaterFlow = false;
	public boolean checkOtherLiquidFlow = false;
	public boolean checkFireSpreadToRegion = false;
	public boolean disableFireSpreadInRegion = false;
	public boolean disableBlockBurnInRegion = false;
	public boolean checkExplosionBlockDamage = false;
	public boolean checkExplosionEntityDamage = false;

	public boolean claimAutoFlagsEnabled = false;
	public boolean showAutoFlagMessages = false;
	public Map<Flag<?>, String> claimAutoFlags = new HashMap<>();

	public boolean restrictCommandsInRegionEnabled = false;
	public int restrictCommandsRecheckTicks = 100;
	public List<String> restrictedCommandsInRegion = new ArrayList<>();

	public boolean extendedWorldEditWandEnabled = false;

	public Boolean miscDefaultPvPFlagOperationMode = null;

	public boolean miscOldPvpFlags = true;

	private static final String ALLOW = "allow";
	private static final String DENY = "deny";
	private static final String DEFAULT = "default";

	public void loadConfig() {
		plugin.saveDefaultConfig();
		loadAll();
	}

	private void loadAll() {
		FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		loadClaimLimits(config);
		loadProtection(config);
		loadAutoFlags(config);
		loadMisc(config);
		loadMessages(config);
	}

	private void loadClaimLimits(@NotNull FileConfiguration config) {
		claimExpandSelectionVertical = config.getBoolean("claim.vertexpand", claimExpandSelectionVertical);

		claimBlockLimitsEnabled = config.getBoolean("claim.blocklimits.enabled", claimBlockLimitsEnabled);

		Map<String, BigInteger> claimBlockLimits = new LinkedHashMap<>();
		ConfigurationSection limitsSection = config.getConfigurationSection("claim.blocklimits.limits");
		if (limitsSection != null) {
			claimBlockLimitDefault = asBig(limitsSection, "default");
			for (String group : limitsSection.getKeys(false)) {
				claimBlockLimits.put(
						group.toLowerCase(),
						asBig(limitsSection, group)
				);
			}
		} else {
			claimBlockLimitDefault = BigInteger.ZERO; // TODO Default to WG or max value
		}
		this.claimBlockLimits = claimBlockLimits;

		ConfigurationSection minLimitsSection = config.getConfigurationSection("claim.blocklimits.minimal");
		if (minLimitsSection != null) {
			claimBlockMinimalVolume = asBig(minLimitsSection, "volume");
			claimBlockMinimalHorizontal = asBig(minLimitsSection, "horizontal");
			claimBlockMinimalVertical = asBig(minLimitsSection, "vertical");
		} else {
			claimBlockMinimalVolume = BigInteger.ZERO;
			claimBlockMinimalHorizontal = BigInteger.ZERO;
			claimBlockMinimalVertical = BigInteger.ZERO;
		}
	}

	private void loadProtection(@NotNull FileConfiguration config) {
		checkLavaFlow = config.getBoolean("regionprotect.flow.lava", checkLavaFlow);
		checkWaterFlow = config.getBoolean("regionprotect.flow.water", checkWaterFlow);
		checkOtherLiquidFlow = config.getBoolean("regionprotect.flow.other", checkOtherLiquidFlow);
		checkFireSpreadToRegion = config.getBoolean("regionprotect.fire.spread.toregion", checkFireSpreadToRegion);
		disableFireSpreadInRegion = config.getBoolean("regionprotect.fire.spread.inregion", disableFireSpreadInRegion);
		disableBlockBurnInRegion = config.getBoolean("regionprotect.fire.burn", disableBlockBurnInRegion);
		checkExplosionBlockDamage = config.getBoolean("regionprotect.explosion.block", checkExplosionBlockDamage);
		checkExplosionEntityDamage = config.getBoolean("regionprotect.explosion.entity", checkExplosionEntityDamage);

		restrictCommandsInRegionEnabled = config.getBoolean("restrictcommands.enabled", restrictCommandsInRegionEnabled);
		restrictCommandsRecheckTicks = config.getInt("restrictcommands.recheck-ticks", restrictCommandsRecheckTicks);
		restrictedCommandsInRegion = new ArrayList<>(config.getStringList("restrictcommands.commands"));
	}

	private void loadAutoFlags(@NotNull FileConfiguration config) {
		claimAutoFlagsEnabled = config.getBoolean("autoflags.enabled", claimAutoFlagsEnabled);
		showAutoFlagMessages = config.getBoolean("autoflags.show-messages", showAutoFlagMessages);
		Map<Flag<?>, String> claimAutoFlags = new LinkedHashMap<>();
		ConfigurationSection autoflagsSection = config.getConfigurationSection("autoflags.flags");
		if (autoflagsSection != null) {
			for (String flagStr : autoflagsSection.getKeys(false)) {
				Flag<?> flag = WGUtils.matchFlag(flagStr);
				if (flag != null) {
					claimAutoFlags.put(flag, autoflagsSection.getString(flagStr));
				}
			}
		}
		this.claimAutoFlags = claimAutoFlags;
	}

	private void loadMisc(@NotNull FileConfiguration config) {
		extendedWorldEditWandEnabled = config.getBoolean("extendedwewand", extendedWorldEditWandEnabled);

		miscDefaultPvPFlagOperationMode = switch (config.getString("misc.pvpmode", DEFAULT).toLowerCase(Locale.ROOT)) {
			case ALLOW -> Boolean.TRUE;
			case DENY -> Boolean.FALSE;
			default -> null;
		};
		miscOldPvpFlags = config.getBoolean("misc.old-pvp-flags");
	}

	private void loadMessages(@NotNull FileConfiguration config) {
		msg.setDecoder(switch (config.getString("messages.serializer", "LEGACY_AMPERSAND").toUpperCase(Locale.ROOT)) {
			case "MINIMESSAGE", "MINI_MESSAGE" -> MiniMessage.miniMessage();
			case "LEGACY_SECTION" -> LegacyComponentSerializer.legacySection();
			default -> LegacyComponentSerializer.legacyAmpersand();
		});
		msg.loadMessages();
	}

	private static @NotNull BigInteger asBig(@NotNull ConfigurationSection section, @NotNull String key) {
		if (section.isInt(key)) {
			return BigInteger.valueOf(section.getInt(key));
		} else {
			String value = section.getString(key, "0");
			if (value.equals("0")) return BigInteger.ZERO;
			return new BigInteger(value);
		}
	}

	public @NotNull Messages getMessages() {
		return this.msg;
	}
}
