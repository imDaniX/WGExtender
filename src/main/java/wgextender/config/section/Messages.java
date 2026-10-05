package wgextender.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wgextender.config.Pointer;
import wgextender.config.Snapshot;
import wgextender.config.message.MessagesProvider;

import static wgextender.config.section.Sections.at;

public record Messages(@Nullable MessagesProvider.Serializer serializer, @NotNull String locale) {
    public static final Pointer.Global<Messages> POINTER = Pointer.global(Snapshot::messages);

    public static @NotNull Messages load(@NotNull ConfigurationSection config) {
        return at(config, "messages", messagesSection -> new Messages(
                MessagesProvider.Serializer.byName(messagesSection.getString("serializer", "LEGACY")),
                messagesSection.getString("locale", "en")
        ));
    }
}
