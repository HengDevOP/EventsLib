package org.khmc.eventslib.util;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoundUtil {

    private static JavaPlugin plugin;

    public static void initialize(JavaPlugin instance) {
        plugin = instance;
    }

    public static void playSound(Player player, String path, Sound defaultSound, float defaultVolume, float defaultPitch) {
        if (player == null) return;
        if (plugin == null) {
            player.playSound(player.getLocation(), defaultSound, defaultVolume, defaultPitch);
            return;
        }
        FileConfiguration config = plugin.getConfig();
        String soundName = config.getString("sounds." + path + ".sound", defaultSound.name());
        float volume = (float) config.getDouble("sounds." + path + ".volume", defaultVolume);
        float pitch = (float) config.getDouble("sounds." + path + ".pitch", defaultPitch);
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception e) {
            player.playSound(player.getLocation(), defaultSound, volume, pitch);
        }
    }

    public static void playSuccess(Player player) {
        if (player == null) return;
        playSound(player, "success", Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
    }

    public static void playClick(Player player) {
        if (player == null) return;
        playSound(player, "click", Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    public static void playToggle(Player player) {
        if (player == null) return;
        playSound(player, "toggle", Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
    }

    public static void playError(Player player) {
        if (player == null) return;
        playSound(player, "error", Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
    }
}
