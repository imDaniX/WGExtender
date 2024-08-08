package wgextender.features.flags;

import com.google.common.base.Function;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import wgextender.WGExtender;
import wgextender.utils.WGRegionUtils;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@Deprecated
public class OldPVPFlagsHandler implements Listener {
	private static final Set<EntityDamageEvent.DamageModifier> PVP_MODIFIERS = EnumSet.of(
			DamageModifier.ARMOR, DamageModifier.RESISTANCE, DamageModifier.MAGIC, DamageModifier.ABSORPTION
	);
	private final Map<UUID, Double> oldValues = new ConcurrentHashMap<>();
	private Field functionsField;

	public void start(Plugin plugin) {
        try {
            functionsField = EntityDamageEvent.class.getDeclaredField("modifierFunctions");
			functionsField.setAccessible(true);
        } catch (Exception ex) {
            plugin.getLogger().log(
					Level.SEVERE,
					"Couldn't get access to 'modifierFunctions' field. Old PvP flags will not be enabled",
					ex
			);
			return;
        }

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void stop(Plugin plugin) {
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			reset(player);
		}
	}

	private void handlePlayer(Player player) {
		if (WGRegionUtils.isFlagTrue(player.getLocation(), WGExtenderFlags.OLDPVP_ATTACKSPEED)) {
			if (oldValues.containsKey(player.getUniqueId())) return;
			AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
			oldValues.put(player.getUniqueId(), attribute.getBaseValue());
			attribute.setBaseValue(16.0);
		} else {
			reset(player);
		}
	}

	private void reset(Player player) {
		Double oldValue = oldValues.remove(player.getUniqueId());
		if (oldValue != null) {
			player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(oldValue);
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		handlePlayer(player);
		player.getScheduler().runAtFixedRate(WGExtender.getInstance(),
				(task) -> handlePlayer(player),
				() -> reset(player),
				1, 1
		);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onQuit(PlayerQuitEvent event) {
		reset(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof Player player) ||
				!player.isBlocking() ||
				!WGRegionUtils.isFlagTrue(entity.getLocation(), WGExtenderFlags.OLDPVP_NOSHIELDBLOCK)) {
			return;
		}
		Map<DamageModifier, Function<Double, Double>> func;
		try {
            //noinspection unchecked
            func = (Map<DamageModifier, Function<Double, Double>>) functionsField.get(event);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			WGExtender.getInstance().getLogger().log(Level.SEVERE, "Unable to recalculate blocking damage", e);
			return;
		}
		double totalDamage = event.getDamage() + event.getDamage(DamageModifier.HARD_HAT);
		// Reset blocking modifier
		event.setDamage(DamageModifier.BLOCKING, 0);
		// Recalculate other modifiers
		for (var modifier : PVP_MODIFIERS) {
			double damage = func.get(modifier).apply(totalDamage);
			event.setDamage(modifier, damage);
			totalDamage += damage;
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction().isRightClick() &&
				event.getHand() == EquipmentSlot.OFF_HAND &&
				event.getItem() != null &&
				event.getItem().getType() == Material.BOW &&
				WGRegionUtils.isFlagTrue(event.getPlayer().getLocation(), WGExtenderFlags.OLDPVP_NOBOW)) {
			event.setCancelled(true);
		}
	}
}
