package wgextender.config.message;

import java.util.Arrays;
import java.util.Locale;

public enum MKey {
    COMMON__ERROR__PLAYER_ONLY,
    COMMON__ERROR__NO_PERMISSION,
    COMMON__ERROR__PLAYER_NOT_FOUND("player"),

    CLAIM__ERROR__GLOBAL,
    CLAIM__ERROR__FORMAT("error"),
    CLAIM__ERROR__RESTRICTED_SYMBOLS("id"),
    CLAIM__ERROR__CONFIG_TOO_BIG,
    CLAIM__ERROR__ALREADY_EXISTS,
    CLAIM__ERROR__TOO_MANY,
    CLAIM__ERROR__TOO_BIG("maxVolume", "volume"),
    CLAIM__ERROR__OVERLAP,
    CLAIM__ERROR__ONLY_INSIDE_OWN,
    CLAIM__ERROR__ONLY_CUBOID,
    CLAIM__ERROR__EXCEPTION("id"),
    CLAIM__ERROR__INCOMPLETE,
    CLAIM__ERROR__DENY_MAX_VOLUME("limit", "volume"),
    CLAIM__ERROR__DENY_MIN_VOLUME("limit", "volume"),
    CLAIM__ERROR__DENY_HORIZONTAL("limit", "volume"),
    CLAIM__ERROR__DENY_VERTICAL("limit", "volume"),
    CLAIM__SUCCESS("id"),
    CLAIM__AUTO_VERT,

    WAND__GIVEN,
    WAND__ITEM_NAME,

    FLAGS__CHORUS_RESTRICTED,
    FLAGS__CONSUME_RESTRICTED,
    FLAGS__RENAME_RESTRICTED,

    DAMAGE__MOB,
    DAMAGE__VEHICLE,
    DAMAGE__DECORATION,
    DAMAGE__PVP,
    DAMAGE__PLAYER,
    DAMAGE__NON_HOSTILE,
    DAMAGE__OTHER,

    RESTRICTED_COMMAND,

    WGEX_COMMAND__RELOAD__HELP,
    WGEX_COMMAND__SEARCH__HELP,
    WGEX_COMMAND__SETFLAG__HELP,
    WGEX_COMMAND__REMOVEOWNER__HELP,
    WGEX_COMMAND__REMOVEMEMBER__HELP,
    WGEX_COMMAND__LIMITS__HELP,
    WGEX_COMMAND__LIMITS__REFRESH__HELP,
    WGEX_COMMAND__LIMITS__CLEAR__HELP,
    WGEX_COMMAND__UPDATE__HELP,

    WGEX_COMMAND__RELOAD__SUCCESS,

    WGEX_COMMAND__SEARCH__NOT_FOUND,
    WGEX_COMMAND__SEARCH__FOUND("regions"),
    WGEX_COMMAND__SEARCH__INCOMPLETE_SELECTION,

    WGEX_COMMAND__SETFLAG__WORLD_NOT_FOUND,
    WGEX_COMMAND__SETFLAG__FLAG_NOT_FOUND,
    WGEX_COMMAND__SETFLAG__SUCCESS,
    WGEX_COMMAND__SETFLAG__INVALID_FORMAT("flag", "error"),

    WGEX_COMMAND__REMOVEOWNER__SUCCESS,
    WGEX_COMMAND__REMOVEMEMBER__SUCCESS,

    WGEX_COMMAND__LIMITS__REFRESH__SUCCESS("player", "limit"),
    WGEX_COMMAND__LIMITS__CLEAR__SUCCESS,

    WGEX_COMMAND__UPDATE__START,
    WGEX_COMMAND__UPDATE__AVAILABLE("current", "remote", "versionType"),
    WGEX_COMMAND__UPDATE__AHEAD("current", "remote"),
    WGEX_COMMAND__UPDATE__UP_TO_DATE("current"),
    WGEX_COMMAND__UPDATE__FAILURE("exception"),
    ;

    final String configurationKey;
    final String[] placeholders;
    final String fallback;

    MKey() {
        this(new String[0]);
    }

    MKey(String... placeholders) {
        this.configurationKey = name()
                .replace("__", ".")
                .replace('_', '-')
                .toLowerCase(Locale.ROOT);

        this.placeholders = new String[placeholders.length];
        for (int i = 0; i < placeholders.length; i++) {
            this.placeholders[i] = '{' + placeholders[i] + '}';
        }

        this.fallback = this.placeholders.length > 0
                ? this.configurationKey + " " + Arrays.toString(this.placeholders)
                : this.configurationKey;
    }
}