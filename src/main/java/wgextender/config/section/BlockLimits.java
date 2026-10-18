package wgextender.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import wgextender.config.Pointer;
import wgextender.config.Snapshot;
import wgextender.utils.CaseInsensitive;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import static wgextender.config.section.Sections.at;

public record BlockLimits(
        boolean enabled,
        @NotNull BigInteger defaultLimit,
        @NotNull Map<String, BigInteger> limits,
        @NotNull BigInteger minimalVolume,
        @NotNull BigInteger minimalHorizontal,
        @NotNull BigInteger minimalVertical
) {
    public static final Pointer.Scoped<BlockLimits> POINTER = Pointer.scoped(Snapshot.Scope::blockLimits);

    public static @NotNull BlockLimits load(@NotNull ConfigurationSection config) {
        return at(config, "claim", claimSection -> at(claimSection, "blocklimits", blockLimitsSection -> at(blockLimitsSection, "minimal", minimalSection -> {
            ConfigurationSection limitsSection = blockLimitsSection.getConfigurationSection("limits");
            Map<String, BigInteger> limits = CaseInsensitive.newMap();
            BigInteger defaultLimit = BigInteger.ZERO;
            if (limitsSection != null) {
                defaultLimit = readBigInteger(limitsSection, "default");
                for (String group : limitsSection.getKeys(false)) {
                    limits.put(group.toLowerCase(Locale.ROOT), readBigInteger(limitsSection, group));
                }
            }
            // TODO Compare max limit with the WG's and warn the user
            return new BlockLimits(
                    blockLimitsSection.getBoolean("enabled", false),
                    defaultLimit,
                    Collections.unmodifiableMap(limits),
                    readBigInteger(minimalSection, "volume"),
                    readBigInteger(minimalSection, "horizontal"),
                    readBigInteger(minimalSection, "vertical")
            );
        })));
    }

    private static @NotNull BigInteger readBigInteger(@NotNull ConfigurationSection section, @NotNull String key) {
        if (section.isLong(key)) return BigInteger.valueOf(section.getLong(key));
        if (section.isDouble(key)) return BigInteger.valueOf((long) section.getDouble(key));
        String value = section.getString(key, "0");
        return value.equals("0") ? BigInteger.ZERO : new BigInteger(value);
    }
}
