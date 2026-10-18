package wgextender.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import wgextender.config.Pointer;
import wgextender.config.Snapshot;

import static wgextender.config.section.Sections.at;

public record Explosion(boolean block, boolean entity, boolean creeperTarget, boolean tntPrime, boolean endCrystalDamager) {
    public static final Pointer.Scoped<Explosion> POINTER = Pointer.scoped(Snapshot.Scope::explosion);

    public static @NotNull Explosion load(@NotNull ConfigurationSection config) {
        return at(config, "regionprotect", protectionSection -> at(protectionSection, "explosion", explosionSection -> new Explosion(
                explosionSection.getBoolean("block", false),
                explosionSection.getBoolean("entity", false),
                explosionSection.getBoolean("source-detection.creeper-target", false),
                explosionSection.getBoolean("source-detection.tnt-prime", false),
                explosionSection.getBoolean("source-detection.end-crystal-damager", false)
        )));
    }
}
