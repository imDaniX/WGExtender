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

package wgextender.features.extendedwand;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import wgextender.utils.WEUtils;

import java.util.Objects;

public final class WEWand {
	public static final NamespacedKey WAND_KEY = Objects.requireNonNull(NamespacedKey.fromString("wgextender:wand"));

	private static String cachedName;
	private static Material cachedMaterial;

	private WEWand() { }

	@SuppressWarnings("PatternValidation")
    private static Material getWandMaterial() { // TODO Better handle registry?
		String weName = WEUtils.getWorldEditPlugin().getLocalConfiguration().wandItem;
		if (!weName.equals(cachedName)) {
			if (!Key.parseable(weName)) {
				cachedMaterial = Material.WOODEN_AXE;
				// TODO Log
			} else {
				cachedMaterial = Registry.MATERIAL.get(Key.key(weName));
				cachedName = weName;
			}
		}
		return cachedMaterial;
	}

	public static @NotNull ItemStack getWand(@NotNull Component name) {
		ItemStack wandItem = ItemStack.of(getWandMaterial());
		wandItem.editMeta(meta -> {
			meta.getPersistentDataContainer().set(WAND_KEY, PersistentDataType.BOOLEAN, true);
			meta.displayName(name);
		});
		return wandItem;
	}

	public static boolean isWand(@NotNull ItemStack item) {
        return item.getType().equals(getWandMaterial()) && item.getPersistentDataContainer().getOrDefault(WAND_KEY, PersistentDataType.BOOLEAN, false);
    }

}
