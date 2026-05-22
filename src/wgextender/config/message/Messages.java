package wgextender.config.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentDecoder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public final class Messages {
    private final Map<MKey, String> messages = new EnumMap<>(MKey.class);
    private final File messagesFile;

    private ComponentDecoder<String, ? extends Component> decoder;

    public Messages(File dataFolder) {
        messagesFile = new File(dataFolder, "messages.yml");
        decoder = LegacyComponentSerializer.legacyAmpersand();
    }

    public void loadMessages() {
        if (!messagesFile.exists()) {
            try {
                messagesFile.createNewFile();
                FileConfiguration config = YamlConfiguration.loadConfiguration(messagesFile);
                for (MKey msg : MKey.values()) {
                    config.set(msg.asConfigurationKey(), msg.def);
                    messages.put(msg, msg.def);
                }
                config.save(messagesFile);
            } catch (IOException e) {
                throw new IllegalStateException("Couldn't create messages.yml, can't customize messages.");
            }
        } else {
            FileConfiguration config = YamlConfiguration.loadConfiguration(messagesFile);
            for (MKey msg : MKey.values()) {
                messages.put(msg, config.getString(msg.asConfigurationKey(), msg.def));
            }
        }
    }

    public void setDecoder(ComponentDecoder<String, ? extends Component> decoder) {
        this.decoder = decoder;
    }

    public Component decode(String msg) {
        return decoder.deserialize(msg);
    }

    public String get(@NotNull MKey msg) {
        return messages.get(msg);
    }

    public String get(@NotNull MKey msg, @NotNull Object ph) {
        return messages.get(msg).replace(msg.placeholders[0], ph.toString());
    }

    public String get(@NotNull MKey msg, @NotNull Object ph1, @NotNull Object ph2) {
        return messages.get(msg)
                .replace(msg.placeholders[0], ph1.toString())
                .replace(msg.placeholders[1], ph2.toString());
    }

    public String get(@NotNull MKey msg, @NotNull Object... phs) {
        String result = messages.get(msg);
        for (int i = 0; i < phs.length; i++) {
            result = result.replace(msg.placeholders[i], phs[i].toString());
        }
        return result;
    }

    public Component rich(@NotNull MKey msg) {
        return decode(get(msg));
    }

    public Component rich(@NotNull MKey msg, @NotNull Object ph) {
        return decode(get(msg, ph));
    }

    public Component rich(@NotNull MKey msg, @NotNull Object ph1, @NotNull Object ph2) {
        return decode(get(msg, ph1, ph2));
    }

    public Component rich(@NotNull MKey msg, @NotNull Object... phs) {
        return decode(get(msg, phs));
    }
}
