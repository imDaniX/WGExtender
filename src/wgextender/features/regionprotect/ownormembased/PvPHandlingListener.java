package wgextender.features.regionprotect.ownormembased;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.BukkitWorldConfiguration;
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
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import wgextender.config.Config;
import wgextender.config.message.MKey;
import wgextender.features.ConfigurableListenerBase;
import wgextender.utils.WGUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static wgextender.utils.WGUtils.getWorldConfig;

@SuppressWarnings("all")
@Deprecated
/**
 * Most of this class is just a copy-paste of WG code.
 * This is a bad pattern, and this should either be refactored or removed entierly
 */
public final class PvPHandlingListener extends ConfigurableListenerBase {

	private static final String DENY_MESSAGE_KEY = "worldguard.region.lastMessage";
	private static final int LAST_MESSAGE_DELAY = 500;

	private RegisteredListener origin;

	public PvPHandlingListener(Config config) {
		super(config);
	}

	public void inject(Plugin plugin) {
        if (config.miscDefaultPvPFlagOperationMode == null) {
            plugin.getLogger().info(
                    "misc.pvpmode is set to default. Changing it post-initialization will require server " +
                    "restart, but it's recommended to not this feature because of possible inconsistencies."
            );
            return;
        } else {
            plugin.getLogger().warning(
                    "misc.pvpmode is set to '" + config.miscDefaultPvPFlagOperationMode + "'. This may or " +
                    "may not result in inconsistency with the default WG behavior, since the feature requires " +
                    "manual copying of WG code."
            );
        }
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
		if (event.getResult() == Result.ALLOW) return; // Don't care about events that have been pre-allowed
		if (!getWorldConfig(event.getWorld()).useRegions) return; // Region support disabled
		// Whitelist check is below

		com.sk89q.worldedit.util.Location target = BukkitAdapter.adapt(event.getTarget());
		RegionAssociable associable = createRegionAssociable(event.getCause());

		RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
		Player playerAttacker = event.getCause().getFirstPlayer();
		boolean canDamage;
		String what;

		// Block PvP like normal even if the player has an override permission
		// because (1) this is a frequent source of confusion and
		// (2) some users want to block PvP even with the bypass permission
		boolean pvp = event.getEntity() instanceof Player && playerAttacker != null && !playerAttacker.equals(event.getEntity());
		if (isWhitelisted(event.getCause(), event.getWorld(), pvp)) {
			return;
		}

		/* Hostile / ambient mob override */
		if (Entities.isHostile(event.getEntity()) || Entities.isAmbient(event.getEntity())) {
			canDamage = event.getRelevantFlags().isEmpty() || query.queryState(target, associable, combine(event)) != State.DENY;
			what = msg.get(MKey.DAMAGE__MOB);
		} else if (Entities.isVehicle(event.getEntity().getType())) {
			canDamage = query.testBuild(target, associable, combine(event, Flags.DESTROY_VEHICLE));
			what = msg.get(MKey.DAMAGE__VEHICLE);
			/* Paintings, item frames, etc. */
		} else if (Entities.isConsideredBuildingIfUsed(event.getEntity())) {
			canDamage = query.testBuild(target, associable, combine(event));
			what = msg.get(MKey.DAMAGE__DECORATION);

			/* PVP */
		}  else if (pvp) {
			LocalPlayer localAttacker = WorldGuardPlugin.inst().wrapPlayer(playerAttacker);
			Player defender = (Player) event.getEntity();
			com.sk89q.worldedit.util.Location attackerLocation = BukkitAdapter.adapt(playerAttacker.getLocation());

			// if defender is an NPC
			if (Entities.isNPC(defender)) {
				return;
			}

			// add possibility to change how pvp none flag works
			// null - default wg pvp logic
			// true - allow pvp when flag not set
			// false - disallow pvp when flag not set
			if (config.miscDefaultPvPFlagOperationMode == null) {
				canDamage =
					query.testBuild(target, associable, combine(event, Flags.PVP)) &&
					(query.queryState(attackerLocation, localAttacker, combine(event, Flags.PVP)) != State.DENY) &&
					(query.queryState(target, localAttacker, combine(event, Flags.PVP)) != State.DENY);
			} else if (config.miscDefaultPvPFlagOperationMode) {
				canDamage =
					(query.queryState(attackerLocation, localAttacker, combine(event, Flags.PVP)) != State.DENY) &&
					(query.queryState(target, localAttacker, combine(event, Flags.PVP)) != State.DENY);
			} else {
				if (!WGUtils.isInRegion(playerAttacker.getLocation()) && !WGUtils.isInRegion(event.getTarget())) {
					canDamage = true;
				} else {
					canDamage =
							(query.queryState(attackerLocation, localAttacker, combine(event, Flags.PVP)) == State.ALLOW) &&
							(query.queryState(target, localAttacker, combine(event, Flags.PVP)) == State.ALLOW);
				}
			}

			// Fire disallow PVP event
			if (!canDamage && Events.fireAndTestCancel(new DisallowedPVPEvent(playerAttacker, defender, event.getOriginalEvent()))) {
				canDamage = true;
			}

			what = msg.get(MKey.DAMAGE__PVP);

		/* Player damage not caused  by another player */
		} else if (event.getEntity() instanceof Player) {
			canDamage = event.getRelevantFlags().isEmpty() || query.queryState(target, associable, combine(event)) != State.DENY;
			what = msg.get(MKey.DAMAGE__PLAYER);

			/* damage to non-hostile mobs (e.g. animals) */
		} else if (Entities.isNonHostile(event.getEntity())) {
			canDamage = query.testBuild(target, associable, combine(event, Flags.DAMAGE_ANIMALS));
			what = msg.get(MKey.DAMAGE__NON_HOSTILE);

			/* Everything else */
		} else {
			canDamage = query.testBuild(target, associable, combine(event, Flags.INTERACT));
			what = msg.get(MKey.DAMAGE__OTHER);
		}

		if (!canDamage) {
			tellErrorMessage(event, event.getCause(), event.getTarget(), what);
			event.setCancelled(true);
		}
	}

	protected RegionAssociable createRegionAssociable(Cause cause) {
		Object rootCause = cause.getRootCause();

		if (!cause.isKnown()) {
			return Associables.constant(Association.NON_MEMBER);
		} else if (rootCause instanceof Player player && !Entities.isNPC(player)) {
			return WorldGuardPlugin.inst().wrapPlayer(player);
		} else if (rootCause instanceof OfflinePlayer offlinePlayer) {
			return WorldGuardPlugin.inst().wrapOfflinePlayer(offlinePlayer);
		} else if (rootCause instanceof Entity entity) {
			RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
			BukkitWorldConfiguration config = getWorldConfig(entity.getWorld());
			Location loc;
			if (config.usePaperEntityOrigin) {
				loc = entity.getOrigin();
				// Origin world may be null, and thus a Location with a null world created, which cannot be adapted to a WorldEdit location
				if (loc == null || loc.getWorld() == null) {
					loc = entity.getLocation();
				}
			} else {
				loc = entity.getLocation();
			}
			return new DelayedRegionOverlapAssociation(query, BukkitAdapter.adapt(loc),
					config.useMaxPriorityAssociation);
		} else if (rootCause instanceof Block block) {
			RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
			Location loc = block.getLocation();
			return new DelayedRegionOverlapAssociation(query, BukkitAdapter.adapt(loc),
					getWorldConfig(loc.getWorld()).useMaxPriorityAssociation);
		} else {
			return Associables.constant(Association.NON_MEMBER);
		}
	}

	/**
	 * Return whether the given cause is whitelist (should be ignored).
	 *
	 * @param cause the cause
	 * @param world the world
	 * @param pvp whether the event in question is PvP combat
	 * @return true if whitelisted
	 */
	private boolean isWhitelisted(Cause cause, World world, boolean pvp) {
		Object rootCause = cause.getRootCause();

		if (rootCause instanceof Player) {
			Player player = (Player) rootCause;
			WorldConfiguration config = getWorldConfig(world);

			if (config.fakePlayerBuildOverride && InteropUtils.isFakePlayer(player)) {
				return true;
			}

			LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
			return !pvp && WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, localPlayer.getWorld());
		} else {
			return false;
		}
	}

	/**
	 * Tell a sender that s/he cannot do something 'here'.
	 *
	 * @param event the event
	 * @param cause the cause
	 * @param location the location
	 * @param what what was done
	 */
	private void tellErrorMessage(DelegateEvent event, Cause cause, Location location, String what) {
		if (event.isSilent() || cause.isIndirect()) {
			return;
		}

		Object rootCause = cause.getRootCause();

		if (rootCause instanceof Player) {
			Player player = (Player) rootCause;

			long now = System.currentTimeMillis();
			Long lastTime = WGMetadata.getIfPresent(player, DENY_MESSAGE_KEY, Long.class);
			if (lastTime == null || now - lastTime >= LAST_MESSAGE_DELAY) {
				RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
				LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
				String message = query.queryValue(BukkitAdapter.adapt(location), localPlayer, Flags.DENY_MESSAGE);
				formatAndSendDenyMessage(what, localPlayer, message);
				WGMetadata.put(player, DENY_MESSAGE_KEY, now);
			}
		}
	}

	static void formatAndSendDenyMessage(String what, LocalPlayer localPlayer, String message) {
		if (message == null || message.isEmpty()) return;
		message = WorldGuard.getInstance().getPlatform().getMatcher().replaceMacros(localPlayer, message);
		message = CommandUtils.replaceColorMacros(message);
		localPlayer.printRaw(message.replace("%what%", what));
	}

	/**
	 * Combine the flags from a delegate event with an array of flags.
	 *
	 * <p>The delegate event's flags appear at the end.</p>
	 *
	 * @param event The event
	 * @param flag An array of flags
	 * @return An array of flags
	 */
	private static StateFlag[] combine(DelegateEvent event, StateFlag... flag) {
		List<StateFlag> extra = event.getRelevantFlags();
		StateFlag[] flags = Arrays.copyOf(flag, flag.length + extra.size());
		for (int i = 0; i < extra.size(); i++) {
			flags[flag.length + i] = extra.get(i);
		}
		return flags;
	}
}
