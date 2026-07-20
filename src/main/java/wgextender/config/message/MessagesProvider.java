package wgextender.config.message;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.ComponentDecoder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wgextender.WGExtender;
import wgextender.config.Configurable;
import wgextender.config.ConfigurationProvider;

import java.io.File;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

// TODO Option for per-player?
// TODO Use MM placeholders properly? No common API for different serializers though
public final class MessagesProvider implements Configurable<ConfigurationProvider.Messages> {
    private final WGExtender plugin;
    private final Map<MKey, String> messages = new EnumMap<>(MKey.class);
    private final File messagesFolder;

    private String lastLocale;

    private ComponentDecoder<String, ? extends Component> decoder;

    public MessagesProvider(@NotNull WGExtender plugin, @NotNull ConfigurationProvider configProvider) {
        this.plugin = plugin;
        this.messagesFolder = new File(plugin.getDataFolder(), "messages");
        this.decoder = Serializer.LEGACY.decoder;
        configProvider.register(this, ConfigurationProvider.Messages.SECTION);
    }

    @Override
    public void onReload(@NotNull ConfigurationProvider.Messages section) {
        var serializer = section.serializer();
        if (serializer == null) {
            plugin.logger().warn("Unknown messages serializer provided, falling back to LEGACY");
            serializer = Serializer.LEGACY;
        }
        this.decoder = serializer.decoder;
        loadMessages(section.locale());
    }

    private void loadMessages(@NotNull String locale) {
        if (!locale.equals(lastLocale)) {
            plugin.logger().info("Set locale to {}, getting messages from messages/messages_{}.yml", locale, locale);
        }
        prepareSupportedLocales();

        File messagesFile = new File(messagesFolder, "messages_" + locale + ".yml");
        if (!messagesFile.exists()) {
            plugin.logger().warn("Locale {} was not found in the messages folder! Loading fallback values.", locale);
            plugin.logger().warn("Supported plugin locales: {}", Arrays.toString(SupportedLocale.values()));
            for (MKey msg : MKey.values()) {
                messages.put(msg, msg.fallback);
            }
            return;
        }

        this.lastLocale = locale;

        FileConfiguration config = YamlConfiguration.loadConfiguration(messagesFile);
        for (MKey msg : MKey.values()) {
            String value = config.getString(msg.configurationKey);
            if (value != null) {
                messages.put(msg, value);
            } else {
                plugin.logger().warn(
                        "Locale {} does not have a value for {}! Loading fallback value '{}'.",
                        locale, msg.configurationKey, msg.fallback
                );
                messages.put(msg, msg.fallback);
            }
        }
    }

    private void prepareSupportedLocales() {
        for (var supLocale : SupportedLocale.values()) {
            if (!new File(messagesFolder, "messages_" + supLocale + ".yml").exists()) {
                try {
                    plugin.saveResource("messages/" + "messages_" + supLocale + ".yml", false);
                } catch (Exception e) {
                    plugin.logger().error(
                            "Failed to save messages for locale {}.",
                            supLocale, e
                    );
                }
            }
        }
    }

    public @NotNull Component decode(@NotNull String msg) {
        return decoder.deserialize(msg);
    }

    public @NotNull String get(@NotNull MKey msg) {
        return messages.get(msg);
    }

    public @NotNull String get(@NotNull MKey msg, @NotNull Object ph) {
        return messages.get(msg).replace(msg.placeholders[0], ph.toString());
    }

    public @NotNull String get(@NotNull MKey msg, @NotNull Object ph1, @NotNull Object ph2) {
        return messages.get(msg)
                .replace(msg.placeholders[0], ph1.toString())
                .replace(msg.placeholders[1], ph2.toString());
    }

    public @NotNull String get(@NotNull MKey msg, @NotNull Object @NotNull ... phs) {
        String result = messages.get(msg);
        for (int i = 0; i < phs.length; i++) {
            result = result.replace(msg.placeholders[i], phs[i].toString());
        }
        return result;
    }

    public @NotNull Component rich(@NotNull MKey msg) {
        return decode(get(msg));
    }

    public @NotNull Component rich(@NotNull MKey msg, @NotNull Object ph) {
        return decode(get(msg, ph));
    }

    public @NotNull Component rich(@NotNull MKey msg, @NotNull Object ph1, @NotNull Object ph2) {
        return decode(get(msg, ph1, ph2));
    }

    public @NotNull Component rich(@NotNull MKey msg, @NotNull Object @NotNull ... phs) {
        return decode(get(msg, phs));
    }

    public void sendMessage(@NotNull Audience audience, @NotNull MKey msg) {
        audience.sendMessage(rich(msg));
    }

    public void sendMessage(@NotNull Audience audience, @NotNull MKey msg, @NotNull Object ph) {
        audience.sendMessage(rich(msg, ph));
    }

    public void sendMessage(@NotNull Audience audience, @NotNull MKey msg, @NotNull Object ph1, @NotNull Object ph2) {
        audience.sendMessage(rich(msg, ph1, ph2));
    }

    public void sendMessage(@NotNull Audience audience, @NotNull MKey msg, @NotNull Object @NotNull ... phs) {
        audience.sendMessage(rich(msg, phs));
    }

    public enum SupportedLocale {
        EN, RU;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    // TODO InkyMessage? Decoders registry?
    public enum Serializer {
        LEGACY_AMPERSAND(fixup(LegacyComponentSerializer.legacyAmpersand())),
        LEGACY_SECTION(fixup(LegacyComponentSerializer.legacySection())),
        LEGACY(input -> LEGACY_AMPERSAND.decoder.deserialize(input.replace('§', '&'))),
        MINIMESSAGE(MiniMessage.miniMessage());

        private final ComponentDecoder<String, ? extends Component> decoder;

        Serializer(ComponentDecoder<String, ? extends Component> decoder) {
            this.decoder = decoder;
        }

        private static @NotNull LegacyComponentSerializer fixup(@NotNull LegacyComponentSerializer serializer) {
            return serializer.toBuilder()
                    .extractUrls()
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();
        }

        public @NotNull ComponentDecoder<String, ? extends Component> decoder() {
            return decoder;
        }

        public static @Nullable Serializer byName(@NotNull String name) {
            return switch (name.toUpperCase(Locale.ROOT)) {
                case "MINIMESSAGE", "MINI_MESSAGE" -> MINIMESSAGE;
                case "LEGACY" -> LEGACY;
                case "LEGACY_SECTION" -> LEGACY_SECTION;
                case "LEGACY_AMPERSAND" -> LEGACY_AMPERSAND;
                default -> null;
            };
        }
    }
}
