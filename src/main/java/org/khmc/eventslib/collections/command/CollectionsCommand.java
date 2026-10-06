package org.khmc.eventslib.collections.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.khmc.eventslib.EventsLibPlugin;
import org.khmc.eventslib.collections.gui.CollectionsCategoryGUI;
import org.khmc.eventslib.util.ColorUtil;
import org.khmc.eventslib.util.SoundUtil;

public class CollectionsCommand implements CommandExecutor {

    private final EventsLibPlugin plugin;

    public CollectionsCommand(EventsLibPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse("&cOnly players can open the collections menu."));
            return true;
        }

        if (!player.hasPermission("collections.use")) {
            player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                    plugin.getConfig().getString("messages.no-permission", "&cYou do not have permission to execute this command.")));
            SoundUtil.playError(player);
            return true;
        }

        SoundUtil.playClick(player);
        new CollectionsCategoryGUI(plugin).open(player);
        return true;
    }
}
