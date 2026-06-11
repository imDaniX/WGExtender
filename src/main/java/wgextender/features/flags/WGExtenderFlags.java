package wgextender.features.flags;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class WGExtenderFlags {
    private WGExtenderFlags() { }

    private static final List<Flag<?>> FLAGS = new ArrayList<>();

    public static final StateFlag CHORUS_FRUIT_USE_FLAG = cache(new StateFlag("chorus-fruit-use", true));
    public static final StateFlag ITEM_CONSUME_FLAG = cache(new StateFlag("item-consume", true));
    public static final StateFlag MOB_RENAME_FLAG = cache(new StateFlag("mob-rename", true));

    public static final StringFlag CONTEXT_HELPER_FLAG = cache(new StringFlag("context-helper"));

    public static final BooleanFlag OLDPVP_ATTACKSPEED = cache(new BooleanFlag("oldpvp-attackspeed"));
    public static final BooleanFlag OLDPVP_NOBOW = cache(new BooleanFlag("oldpvp-nobow"));
    public static final BooleanFlag OLDPVP_NOSHIELDBLOCK = cache(new BooleanFlag("oldpvp-noshieldblock"));

    private static <T, F extends Flag<T>> F cache(F flag) {
        FLAGS.add(flag);
        return flag;
    }

    public static void registerFlags(Logger log) {
        FlagRegistry flagRegistry = WorldGuard.getInstance().getFlagRegistry();
        for (var flag : FLAGS) {
            try {
                flagRegistry.register(flag);
            } catch (FlagConflictException ex) {
                log.log(Level.SEVERE, "Unable to register '" + flag.getName() + "' flag - already registered", ex);
            }
        }
    }
}
