package org.khmc.eventslib.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ColorUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .build();

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String converted = translateLegacyToMiniMessage(input);
        try {
            return MINI_MESSAGE.deserialize(converted);
        } catch (Exception ignored) {
            return LEGACY_SERIALIZER.deserialize(input);
        }
    }

    private static String translateLegacyToMiniMessage(String text) {
        if (text == null) return "";
        return text
                .replace("&0", "<black>").replace("§0", "<black>")
                .replace("&1", "<dark_blue>").replace("§1", "<dark_blue>")
                .replace("&2", "<dark_green>").replace("§2", "<dark_green>")
                .replace("&3", "<dark_aqua>").replace("§3", "<dark_aqua>")
                .replace("&4", "<dark_red>").replace("§4", "<dark_red>")
                .replace("&5", "<dark_purple>").replace("§5", "<dark_purple>")
                .replace("&6", "<gold>").replace("§6", "<gold>")
                .replace("&7", "<gray>").replace("§7", "<gray>")
                .replace("&8", "<dark_gray>").replace("§8", "<dark_gray>")
                .replace("&9", "<blue>").replace("§9", "<blue>")
                .replace("&a", "<green>").replace("§a", "<green>")
                .replace("&b", "<aqua>").replace("§b", "<aqua>")
                .replace("&c", "<red>").replace("§c", "<red>")
                .replace("&d", "<light_purple>").replace("§d", "<light_purple>")
                .replace("&e", "<yellow>").replace("§e", "<yellow>")
                .replace("&f", "<white>").replace("§f", "<white>")
                .replace("&l", "<bold>").replace("§l", "<bold>")
                .replace("&m", "<strikethrough>").replace("§m", "<strikethrough>")
                .replace("&n", "<underlined>").replace("§n", "<underlined>")
                .replace("&o", "<italic>").replace("§o", "<italic>")
                .replace("&r", "<reset>").replace("§r", "<reset>");
    }

    public static String toSmallCaps(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            char lower = Character.toLowerCase(c);
            switch (lower) {
                case 'a' -> sb.append('ᴀ');
                case 'b' -> sb.append('ʙ');
                case 'c' -> sb.append('ᴄ');
                case 'd' -> sb.append('ᴅ');
                case 'e' -> sb.append('ᴇ');
                case 'f' -> sb.append('ғ');
                case 'g' -> sb.append('ɢ');
                case 'h' -> sb.append('ʜ');
                case 'i' -> sb.append('ɪ');
                case 'j' -> sb.append('ᴊ');
                case 'k' -> sb.append('ᴋ');
                case 'l' -> sb.append('ʟ');
                case 'm' -> sb.append('ᴍ');
                case 'n' -> sb.append('ɴ');
                case 'o' -> sb.append('ᴏ');
                case 'p' -> sb.append('ᴘ');
                case 'q' -> sb.append('ǫ');
                case 'r' -> sb.append('ʀ');
                case 's' -> sb.append('s');
                case 't' -> sb.append('ᴛ');
                case 'u' -> sb.append('ᴜ');
                case 'v' -> sb.append('ᴠ');
                case 'w' -> sb.append('ᴡ');
                case 'x' -> sb.append('x');
                case 'y' -> sb.append('ʏ');
                case 'z' -> sb.append('ᴢ');
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
