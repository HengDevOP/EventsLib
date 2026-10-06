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

public class CollectionsCommand implements CommandExecutor, org.bukkit.command.TabCompleter {

    private final EventsLibPlugin plugin;

    public CollectionsCommand(EventsLibPlugin plugin) {
        this.plugin = plugin;
    }

    private String getPrefix() {
        return plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse("&cOnly players can open the collections menu."));
            return true;
        }

        if (!player.hasPermission("collections.use")) {
            player.sendMessage(ColorUtil.parse(getPrefix() +
                    plugin.getConfig().getString("messages.no-permission", "&cYou do not have permission to execute this command.")));
            SoundUtil.playError(player);
            return true;
        }

        // Handle /collections list [category]
        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            var manager = plugin.getCollectionManager();
            var profile = manager.getProfile(player);

            if (args.length >= 2) {
                org.khmc.eventslib.collections.model.CollectionType type = org.khmc.eventslib.collections.model.CollectionType.fromString(args[1]);
                if (type == null) {
                    player.sendMessage(ColorUtil.parse(getPrefix() + "&cInvalid category &e" + args[1] + "&c."));
                    return true;
                }
                var skins = manager.getSkinsForType(type);
                player.sendMessage(ColorUtil.parse("&8======= &d" + type.getDisplayName() + " Skins &8(" + skins.size() + ") ======="));
                for (var s : skins) {
                    boolean owned = profile != null && profile.hasUnlocked(s.getId());
                    String status = owned ? "&a[UNLOCKED]" : "&c[LOCKED]";
                    player.sendMessage(ColorUtil.parse("&8▪ &b" + s.getName() + " &7(&e" + s.getId() + "&7) " + status));
                }
                return true;
            }

            player.sendMessage(ColorUtil.parse("&8======= &dCosmetic Collections &8(" + manager.getTotalSkinsCount() + " total skins) ======="));
            for (org.khmc.eventslib.collections.model.CollectionType type : org.khmc.eventslib.collections.model.CollectionType.values()) {
                var skins = manager.getSkinsForType(type);
                int count = skins.size();
                String countColor = count > 0 ? "&a" : "&7";
                player.sendMessage(ColorUtil.parse("&e✦ &f" + type.getDisplayName() + " &8[" + countColor + count + " skins&8]"));
            }
            player.sendMessage(ColorUtil.parse("&7Use &e/collections &7to open the interactive GUI!"));
            return true;
        }

        SoundUtil.playClick(player);
        new CollectionsCategoryGUI(plugin).open(player);
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            if ("list".startsWith(args[0].toLowerCase())) {
                return java.util.List.of("list");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            java.util.List<String> completions = new java.util.ArrayList<>();
            for (var t : org.khmc.eventslib.collections.model.CollectionType.values()) {
                if (t.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(t.name());
                }
            }
            return completions;
        }
        return java.util.Collections.emptyList();
    }
}
