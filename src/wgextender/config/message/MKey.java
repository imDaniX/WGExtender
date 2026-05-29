package wgextender.config.message;

import java.util.Locale;

public enum MKey {
    COMMON__ERROR__PLAYER_ONLY("Эта команда только для игроков."),
    COMMON__ERROR__NO_PERMISSION("&4Недостаточно прав."),
    COMMON__ERROR__PLAYER_NOT_FOUND("&4Игрока {player} не существует!", "player"),

    CLAIM__ERROR__GLOBAL("Нельзя заприватить __global__."),
    CLAIM__ERROR__FORMAT("&4{error}", "error"),
    CLAIM__ERROR__RESTRICTED_SYMBOLS("Имя региона '{id}' содержит запрещённые символы.", "id"),
    CLAIM__ERROR__CONFIG_TOO_BIG(
            "The maximum claim volume get in the configuration is higher than is supported. " +
            "Currently, it must be " + Integer.MAX_VALUE + " or smaller. Please contact a server administrator."
    ),
    CLAIM__ERROR__ALREADY_EXISTS("Регион с таким именем уже существует, выберите другое."),
    CLAIM__ERROR__TOO_MANY("У вас слишком много регионов, удалите один из них перед тем как заприватить новый."),
    CLAIM__ERROR__TOO_BIG(
            "Размер региона слишком большой. Максимальный размер: {maxVolume}, ваш размер: {volume}",
            "maxVolume", "volume"
    ),
    CLAIM__ERROR__OVERLAP("Это регион перекрывает чужой регион."),
    CLAIM__ERROR__ONLY_INSIDE_OWN("Вы можете приватить только внутри своих регионов."),
    CLAIM__ERROR__ONLY_CUBOID("Вы можете использовать только кубическую территорию."),
    CLAIM__ERROR__EXCEPTION("&eПроизошла ошибка при привате региона {id}.", "id"),
    CLAIM__ERROR__INCOMPLETE("Сначала выделите территорию. Используйте WorldEdit для выделения (wiki: https://worldedit.enginehub.org/)."),
    CLAIM__ERROR__DENY_MAX_VOLUME(
            "&4Вы не можете заприватить такой большой регион\n" +
            "Ваш лимит: {limit}, вы попытались заприватить: {volume}",
            "limit", "volume"
    ),
    CLAIM__ERROR__DENY_MIN_VOLUME(
            "&4Вы не можете заприватить такой маленький регион\n" +
            "Минимальный объем: {limit}, вы попытались заприватить: {volume}",
            "limit", "volume"
    ),
    CLAIM__ERROR__DENY_HORIZONTAL(
            "&4Вы не можете заприватить такой узкий регион\n" +
            "Минимальная ширина: {limit}, вы попытались заприватить: {volume}",
            "limit", "volume"
    ),
    CLAIM__ERROR__DENY_VERTICAL(
            "&4Вы не можете заприватить такой низкий регион\n" +
            "Минимальная высота: {limit}, вы попытались заприватить: {volume}",
            "limit", "volume"
    ),
    CLAIM__SUCCESS("&eВы заприватили регион {id}.", "id"),
    CLAIM__AUTO_VERT("&eРегион автоматически расширен по вертикали."),

    WAND__GIVEN("&dВыдан предмет для выделения территории."),
    WAND__ITEM_NAME("&dВыделение территории"),

    FLAGS__CHORUS_RESTRICTED("&4Вы не можете использовать хорус в этом регионе."),
    FLAGS__CONSUME_RESTRICTED("&4Вы не можете употреблять предметы в этом регионе."),

    DAMAGE__MOB("бить это"),
    DAMAGE__VEHICLE("ломать это"),
    DAMAGE__DECORATION("менять это"),
    DAMAGE__PVP("драться"),
    DAMAGE__PLAYER("наносить урон этому"),
    DAMAGE__NON_HOSTILE("вредить этому"),
    DAMAGE__OTHER("бить это"),

    RESTRICTED_COMMAND("&4Вы не можете использовать эту команду в чужом регионе."),

    WGEX_COMMAND__UNKNOWN_SUBCOMMAND("&bНеизвестная подкоманда '{subcommand}'.", "subcommand"),
    WGEX_COMMAND__RELOAD__HELP("&bwgex reload - перезагрузить конфиг"),
    WGEX_COMMAND__SEARCH__HELP("&bwgex search - ищет регионы в выделенной области"),
    WGEX_COMMAND__SETFLAG__HELP("&bwgex setflag <мир> <флаг> <значение> - устанавливает флаг <флаг> со значением <значение> на все регионы в мире <мир>"),
    WGEX_COMMAND__REMOVEOWNER__HELP("&bwgex removeowner <имя> - удаляет игрока из списков владельцев всех регионов"),
    WGEX_COMMAND__REMOVEMEMBER__HELP("&bwgex removemember <имя> - удаляет игрока из списков членов всех регионов"),
    WGEX_COMMAND__LIMITS__HELP("&bwgex limit - управление кэшем лимитов блоков"),
    WGEX_COMMAND__LIMITS__REFRESH__HELP("&bwgex limits refresh <имя> - обновить кэш для игрока"),
    WGEX_COMMAND__LIMITS__CLEAR__HELP("&bwgex limits clear - очистить текущий кэш"),

    WGEX_COMMAND__RELOAD__SUCCESS("&bКонфиг перезагружен"),

    WGEX_COMMAND__SEARCH__NOT_FOUND("&bРегионов пересекающихся с выделенной зоной не найдено"),
    WGEX_COMMAND__SEARCH__FOUND("&bНайдены регионы пересекающиеся с выделенной зоной: {regions}", "regions"),
    WGEX_COMMAND__SEARCH__INCOMPLETE_SELECTION("&bСначала выделите зону поиска"),

    WGEX_COMMAND__SETFLAG__WORLD_NOT_FOUND("&bМир не найден"),
    WGEX_COMMAND__SETFLAG__FLAG_NOT_FOUND("&bФлаг не найден"),
    WGEX_COMMAND__SETFLAG__SUCCESS("&bФлаги установлены"),
    WGEX_COMMAND__SETFLAG__INVALID_FORMAT("&bНеправильный формат флага {flag}: {error}", "flag", "error"),

    WGEX_COMMAND__REMOVEOWNER__SUCCESS("&bИгрок удалён из списков владельцев всех регионов"),
    WGEX_COMMAND__REMOVEMEMBER__SUCCESS("&bИгрок удалён из списков участников всех регионов"),

    WGEX_COMMAND__LIMITS__REFRESH__SUCCESS("&bЛимит игрока {player} обновлён: {limit}.", "player", "limit"),
    WGEX_COMMAND__LIMITS__CLEAR__SUCCESS("&bКэш лимитов сброшен.")
    ;

    final String def;
    final String[] placeholders;

    MKey(String def) {
        this.def = def;
        this.placeholders = new String[0];
    }

    MKey(String def, String... placeholders) {
        this.def = def;
        this.placeholders = new String[placeholders.length];
        for (int i = 0; i < placeholders.length; i++) {
            this.placeholders[i] = '{' + placeholders[i] + '}';
        }
    }

    public String asConfigurationKey() {
        return name()
                .replace("__", ".")
                .replace('_', '-')
                .toLowerCase(Locale.ROOT);
    }
}
