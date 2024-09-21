package wgextender.features.regionprotect.ownormembased;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.cause.Cause;
import com.sk89q.worldguard.bukkit.event.DelegateEvent;
import com.sk89q.worldguard.bukkit.event.entity.DamageEntityEvent;
import com.sk89q.worldguard.bukkit.internal.WGMetadata;
import com.sk89q.worldguard.bukkit.listener.RegionProtectionListener;
import com.sk89q.worldguard.bukkit.protection.events.DisallowedPVPEvent;
import com.sk89q.worldguard.bukkit.util.Entities;
import com.sk89q.worldguard.bukkit.util.Events;
import com.sk89q.worldguard.bukkit.util.InteropUtils;
import com.sk89q.worldguard.commands.CommandUtils;
import com.sk89q.worldguard.config.WorldConfiguration;
import com.sk89q.worldguard.domains.Association;
import com.sk89q.worldguard.protection.association.Associables;
import com.sk89q.worldguard.protection.association.DelayedRegionOverlapAssociation;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import wgextender.Config;
import wgextender.utils.WGRegionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PvPHandlingListener implements Listener {

	private final Config config;

	private static final String DENY_MESSAGE_KEY = "worldguard.region.lastMessage";
	private static final int LAST_MESSAGE_DELAY = 500;

	private RegisteredListener origin;

	public PvPHandlingListener(Config config) {
		this.config = config;
	}

	public void inject(Plugin plugin) {
		HandlerList handlers = DamageEntityEvent.getHandlerList();
		for (var listener : handlers.getRegisteredListeners()) {
			if (listener.getListener().getClass().equals(RegionProtectionListener.class)) {
				origin = listener;
				break;
			}
		}
		Objects.requireNonNull(origin, "Couldn't find the original RegionProtectionListener");
		handlers.unregister(origin);
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void uninject() {
        if (origin == null) return;
        HandlerList handlers = DamageEntityEvent.getHandlerList();
        handlers.unregister(this);
        handlers.register(origin);
        origin = null;
    }

	@EventHandler(ignoreCancelled = true)
	public void onDamageEntity(DamageEntityEvent event) {
		if (event.getResult() == Result.ALLOW) {
			return;
		}
		if (!WGRegionUtils.getWorldConfig(event.getWorld()).useRegions) {
			return;
		}

		Player playerAttacker = event.getCause().getFirstPlayer();
		if (playerAttacker == null) {
			return;
		}

		// Block PvP like normal even if the player has an override permission
		// because (1) this is a frequent source of confusion and
		// (2) some users want to block PvP even with the bypass permission
		boolean pvp = event.getEntity() instanceof Player && !playerAttacker.equals(event.getEntity());
		if (isWhitelisted(event.getCause(), event.getWorld(), pvp)) {
			return;
		}

		RegionQuery query = WGRegionUtils.REGION_QUERY;

		boolean canDamage;
		String what;

		Location target = event.getTarget();
		RegionAssociable associable = createRegionAssociable(event.getCause());
		com.sk89q.worldedit.util.Location weTarget = BukkitAdapter.adapt(target);
		com.sk89q.worldedit.util.Location weAttacker = BukkitAdapter.adapt(playerAttacker.getLocation());

		/* Hostile / ambient mob override */
		if (
			Entities.isHostile(event.getEntity()) ||
			Entities.isAmbient(event.getEntity()) ||
			Entities.isVehicle(event.getEntity().getType())
		) {
			canDamage = event.getRelevantFlags().isEmpty() || (query.queryState(weTarget, associable, getFlags(event)) != State.DENY);
			what = "hit that";

			/* Paintings, item frames, etc. */
		} else if (Entities.isConsideredBuildingIfUsed(event.getEntity())) {
			canDamage = query.testBuild(weTarget, associable, getFlags(event));
			what = "change that";

			/* PVP */
		} else if (pvp) {
			Player defender = (Player) event.getEntity();
			LocalPlayer localPlayerAttacker = WorldGuardPlugin.inst().wrapPlayer(playerAttacker);

			// add possibility to change how pvp none flag works
			// null - default wg pvp logic
			// true - allow pvp when flag not set
			// false - disallow pvp when flag not set
			if (config.miscDefaultPvPFlagOperationMode == null) {
				canDamage =
					query.testBuild(weTarget, associable, getFlags(event, Flags.PVP)) &&
					(query.queryState(weAttacker, localPlayerAttacker, getFlags(event, Flags.PVP)) != State.DENY) &&
					(query.queryState(weTarget, localPlayerAttacker, getFlags(event, Flags.PVP)) != State.DENY);
			} else if (config.miscDefaultPvPFlagOperationMode) {
				canDamage =
					(query.queryState(weAttacker, localPlayerAttacker, getFlags(event, Flags.PVP)) != State.DENY) &&
					(query.queryState(weTarget, localPlayerAttacker, getFlags(event, Flags.PVP)) != State.DENY);
			} else {
				if (!WGRegionUtils.isInWGRegion(playerAttacker.getLocation()) && !WGRegionUtils.isInWGRegion(target)) {
					canDamage = true;
				} else {
					canDamage = (query.queryState(weAttacker, localPlayerAttacker, getFlags(event, Flags.PVP)) == State.ALLOW) && (query.queryState(weTarget, localPlayerAttacker, getFlags(event, Flags.PVP)) == State.ALLOW);
				}

			}

			// Fire disallow PVP event
			if (!canDamage && Events.fireAndTestCancel(new DisallowedPVPEvent(playerAttacker, defender, event.getOriginalEvent()))) {
				canDamage = true;
			}

			what = "PvP";

			/* Player damage not caused by another player */
		} else if (event.getEntity() instanceof Player) {
			canDamage = event.getRelevantFlags().isEmpty() || (query.queryState(weTarget, associable, getFlags(event)) != State.DENY);
			what = "damage that";

			/* damage to non-hostile mobs (e.g. animals) */
		} else if (Entities.isNonHostile(event.getEntity())) {
			canDamage = query.testBuild(weTarget, associable, getFlags(event, Flags.DAMAGE_ANIMALS));
			what = "harm that";

			/* Everything else */
		} else {
			canDamage = query.testBuild(weTarget, associable, getFlags(event, Flags.INTERACT));
			what = "hit that";
		}

		if (!canDamage) {
			tellErrorMessage(event, event.getCause(), target, what);
			event.setCancelled(true);
		}
	}

	private static RegionAssociable createRegionAssociable(Cause cause) {
		if (!cause.isKnown()) {
			return Associables.constant(Association.NON_MEMBER);
		}

        return switch (cause.getRootCause()) {
            case Player player -> WorldGuardPlugin.inst().wrapPlayer(player);
            case OfflinePlayer offlinePlayer -> WorldGuardPlugin.inst().wrapOfflinePlayer(offlinePlayer);
            case Entity entity -> new DelayedRegionOverlapAssociation(
					WGRegionUtils.REGION_QUERY, BukkitAdapter.adapt(entity.getLocation())
			);
            case Block block -> new DelayedRegionOverlapAssociation(
					WGRegionUtils.REGION_QUERY, BukkitAdapter.adapt(block.getLocation())
			);
            case null, default -> Associables.constant(Association.NON_MEMBER);
        };
	}

	private boolean isWhitelisted(Cause cause, World world, boolean pvp) {
		Object rootCause = cause.getRootCause();

		if (rootCause instanceof Block block) {
			Material type = block.getType();
			return (type == Material.HOPPER) || (type == Material.DROPPER);
		} else if (rootCause instanceof Player player) {
            WorldConfiguration config = WGRegionUtils.getWorldConfig(world);

			if (config.fakePlayerBuildOverride && InteropUtils.isFakePlayer(player)) {
				return true;
			}

			LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
			return !pvp && WGRegionUtils.getPlatform().getSessionManager().hasBypass(localPlayer, localPlayer.getWorld());
		} else {
			return false;
		}
	}

	private void tellErrorMessage(DelegateEvent event, Cause cause, Location location, String what) {
		if (event.isSilent() || cause.isIndirect()) {
			return;
		}

		if (cause.getRootCause() instanceof Player player) {
			long now = System.currentTimeMillis();
			Long lastTime = WGMetadata.getIfPresent(player, DENY_MESSAGE_KEY, Long.class);
			if (lastTime == null || now - lastTime >= LAST_MESSAGE_DELAY) {
				RegionQuery query = WGRegionUtils.getPlatform().getRegionContainer().createQuery();
				LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
				String message = query.queryValue(BukkitAdapter.adapt(location), localPlayer, Flags.DENY_MESSAGE);
				formatAndSendDenyMessage(what, localPlayer, message);
				WGMetadata.put(player, DENY_MESSAGE_KEY, now);
			}
		}
	}

    private static void formatAndSendDenyMessage(String what, LocalPlayer localPlayer, String message) {
        if (message == null || message.isEmpty()) return;
        message = WGRegionUtils.getPlatform().getMatcher().replaceMacros(localPlayer, message);
        message = CommandUtils.replaceColorMacros(message);
        localPlayer.printRaw(message.replace("%what%", what));
    }

	private static StateFlag[] getFlags(DelegateEvent event) {
		return event.getRelevantFlags().toArray(new StateFlag[0]);
	}

	private static StateFlag[] getFlags(DelegateEvent event, StateFlag flag) {
		List<StateFlag> extra = event.getRelevantFlags();
		StateFlag[] result = Arrays.copyOf(getFlags(event), extra.size() + 1);
		result[extra.size()] = flag;
		return result;
	}
}
