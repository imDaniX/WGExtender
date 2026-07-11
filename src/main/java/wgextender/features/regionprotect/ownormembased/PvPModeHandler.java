package wgextender.features.regionprotect.ownormembased;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.event.entity.DamageEntityEvent;
import com.sk89q.worldguard.bukkit.internal.WGMetadata;
import com.sk89q.worldguard.bukkit.protection.events.DisallowedPVPEvent;
import com.sk89q.worldguard.bukkit.util.Entities;
import com.sk89q.worldguard.bukkit.util.Events;
import com.sk89q.worldguard.bukkit.util.InteropUtils;
import com.sk89q.worldguard.commands.CommandUtils;
import com.sk89q.worldguard.config.WorldConfiguration;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import wgextender.config.ConfigurationProvider;
import wgextender.config.message.MKey;
import wgextender.features.ConfigurableListenerBase;
import wgextender.utils.WGUtils;

import static wgextender.utils.WEUtils.weLocation;
import static wgextender.utils.WGUtils.getWorldConfig;

public final class PvPModeHandler extends ConfigurableListenerBase<ConfigurationProvider.Misc> {
    private static final String DENY_MESSAGE_KEY = "worldguard.region.lastMessage";
    private static final int LAST_MESSAGE_DELAY = 500;

    public PvPModeHandler(@NotNull ConfigurationProvider cfgProvider) {
        super(cfgProvider, ConfigurationProvider.Misc.SECTION);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onDisallowedPVP(DisallowedPVPEvent event) {
        if (config.pvpMode() != State.ALLOW) return;
        // Entity is protected but the mode is pvp = ALLOW

        Player attacker = event.getAttacker();
        Player defender = event.getDefender();

        LocalPlayer localAttacker = WGUtils.wgPlayer(attacker);
        var attackerLocation = weLocation(attacker.getLocation());
        var targetLocation = weLocation(defender.getLocation());

        // Both regions are not disallowing pvp
        boolean allow = WGUtils.REGION_QUERY.queryState(attackerLocation, localAttacker, Flags.PVP) != State.DENY &&
                        WGUtils.REGION_QUERY.queryState(targetLocation, localAttacker, Flags.PVP) != State.DENY;

        if (allow) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onDamageEntity(DamageEntityEvent event) {
        if (config.pvpMode() != State.DENY) return;
        // Entity is damaged but the mode is pvp = DENY

        if (!getWorldConfig(event.getWorld()).useRegions) return;

        Player attacker = event.getCause().getFirstPlayer();
        if (!(event.getEntity() instanceof Player defender) || attacker == null || attacker.equals(defender)) {
            return;
        }
        if (Entities.isNPC(defender)) return;

        WorldConfiguration worldConfig = getWorldConfig(event.getWorld());
        if (worldConfig.fakePlayerBuildOverride && InteropUtils.isFakePlayer(attacker)) return;

        LocalPlayer localAttacker = WGUtils.wgPlayer(attacker);
        var attackerLocation = weLocation(attacker.getLocation());
        var targetLocation = weLocation(event.getTarget());

        boolean allow;
        if (!WGUtils.isInRegion(attacker.getLocation()) && !WGUtils.isInRegion(event.getTarget())) {
            // Both are outside of regions
            allow = true;
        } else {
            // Both have pvp allowed
            allow = WGUtils.REGION_QUERY.queryState(attackerLocation, localAttacker, Flags.PVP) == State.ALLOW &&
                    WGUtils.REGION_QUERY.queryState(targetLocation, localAttacker, Flags.PVP) == State.ALLOW;
        }

        if (!allow) {
            if (Events.fireAndTestCancel(new DisallowedPVPEvent(attacker, defender, event.getOriginalEvent()))) {
                allow = true;
            } else if (!event.isSilent() && !event.getCause().isIndirect()) {
                long now = System.currentTimeMillis();
                Long lastTime = WGMetadata.getIfPresent(attacker, DENY_MESSAGE_KEY, Long.class);
                if (lastTime == null || now - lastTime >= LAST_MESSAGE_DELAY) {
                    String message = WGUtils.REGION_QUERY.queryValue(targetLocation, localAttacker, Flags.DENY_MESSAGE);
                    if (message != null && !message.isEmpty()) {
                        message = WorldGuard.getInstance().getPlatform().getMatcher().replaceMacros(localAttacker, message);
                        message = CommandUtils.replaceColorMacros(message);
                        localAttacker.printRaw(message.replace("%what%", msg.get(MKey.DAMAGE__PVP)));
                    }
                    WGMetadata.put(attacker, DENY_MESSAGE_KEY, now);
                }
            }
        }

        event.setCancelled(!allow);
    }
}