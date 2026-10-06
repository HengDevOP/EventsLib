package org.khmc.eventslib.collections.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.khmc.eventslib.EventsLibPlugin;
import org.khmc.eventslib.collections.manager.CollectionManager;
import org.khmc.eventslib.collections.model.CollectionSkin;
import org.khmc.eventslib.collections.model.CollectionType;
import org.khmc.eventslib.util.ColorUtil;
import org.khmc.eventslib.util.SoundUtil;

import java.util.*;
import java.util.regex.Pattern;

public class CollectionAdminCommand implements CommandExecutor, TabCompleter {

    private final EventsLibPlugin plugin;
    private final CollectionManager manager;
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    public CollectionAdminCommand(EventsLibPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getCollectionManager();
    }

    private String getPrefix() {
        return plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ");
    }

    private boolean checkPermission(CommandSender sender, String perm) {
        if (!sender.hasPermission("collections.admin") && !sender.hasPermission(perm)) {
            sender.sendMessage(ColorUtil.parse(getPrefix() + "&cYou do not have permission to execute this command."));
            if (sender instanceof Player p) SoundUtil.playError(p);
            return false;
        }
        return true;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!checkPermission(sender, "collections.admin")) {
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            // ── /cadmin create <id> <type> <material> <custom_model> ──────
            case "create" -> {
                if (!checkPermission(sender, "collections.admin.create")) return true;
                if (args.length < 5) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /" + label + " create <id> <type> <material> <custom_model>"));
                    sender.sendMessage(ColorUtil.parse("&7Example: &e/" + label + " create sword_crimson SWORD NETHERITE_SWORD khmc:sword/crimson"));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String id = args[1].trim().toLowerCase(Locale.ROOT);
                if (!ID_PATTERN.matcher(id).matches() || id.length() > 32) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cInvalid skin ID! Use only alphanumeric characters, underscores, and hyphens (max 32 chars)."));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                if (manager.getSkin(id) != null) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cA skin with ID &e'" + id + "' &calready exists."));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                CollectionType type = CollectionType.fromString(args[2]);
                if (type == null) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cInvalid collection type &e" + args[2] + "&c!"));
                    sender.sendMessage(ColorUtil.parse("&7Available types: &f" + Arrays.toString(CollectionType.values())));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                Material mat = Material.matchMaterial(args[3].toUpperCase(Locale.ROOT));
                if (mat == null) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cInvalid material &e" + args[3] + "&c!"));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String model = args[4].trim();
                if (model.isEmpty()) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cCustom model identifier cannot be empty!"));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                // Default auto-generated name from id
                String name = id.replace("_", " ").replace("-", " ");
                name = capitalizeWords(name);

                boolean created = manager.createSkin(id, name, type, mat, model, "Exclusive " + name + " cosmetic skin.");
                if (created) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&aCosmetic skin &e" + id + " &a(&b" + name + "&a) created successfully!"));
                    sender.sendMessage(ColorUtil.parse("&7Type: &f" + type.name() + " &8| &7Material: &f" + mat.name() + " &8| &7Model: &d" + model));
                    sender.sendMessage(ColorUtil.parse("&7Use &e/" + label + " edit " + id + " name \"New Name\" &7to customize its display name."));
                    if (sender instanceof Player p) SoundUtil.playSuccess(p);
                } else {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cFailed to create skin &e" + id + "&c."));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                }
            }

            // ── /cadmin delete <id> ──────────────────────────────────────────
            case "delete" -> {
                if (!checkPermission(sender, "collections.admin.delete")) return true;
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /" + label + " delete <id>"));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String id = args[1].trim().toLowerCase(Locale.ROOT);
                CollectionSkin skin = manager.getSkin(id);
                if (skin == null) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cSkin &e" + id + " &cwas not found."));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                boolean deleted = manager.deleteSkin(id);
                if (deleted) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&aCosmetic skin &e" + id + " &adeleted successfully."));
                    if (sender instanceof Player p) SoundUtil.playSuccess(p);
                } else {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cFailed to delete skin &e" + id + "&c."));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                }
            }

            // ── /cadmin edit <id> <property> <value...> ─────────────────────
            case "edit" -> {
                if (!checkPermission(sender, "collections.admin.edit")) return true;
                if (args.length < 4) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /" + label + " edit <id> <name/model/desc/enabled/material> <value...>"));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String id = args[1].trim().toLowerCase(Locale.ROOT);
                CollectionSkin skin = manager.getSkin(id);
                if (skin == null) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cSkin &e" + id + " &cwas not found."));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String prop = args[2].toLowerCase(Locale.ROOT);

                // Join remaining arguments
                StringBuilder sb = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    if (i > 3) sb.append(" ");
                    sb.append(args[i]);
                }
                String valStr = sb.toString().trim();
                if (valStr.startsWith("\"") && valStr.endsWith("\"") && valStr.length() >= 2) {
                    valStr = valStr.substring(1, valStr.length() - 1);
                }

                Object value = valStr;
                if (prop.equals("enabled")) {
                    value = Boolean.parseBoolean(valStr);
                }

                boolean updated = manager.editSkin(id, prop, value);
                if (updated) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&aUpdated property &e" + prop + " &aof skin &b" + id + " &ato &f" + valStr + "&a."));
                    if (sender instanceof Player p) SoundUtil.playSuccess(p);
                } else {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cFailed to update property &e" + prop + " &cfor skin &b" + id + "&c."));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                }
            }

            // ── /cadmin give <player> <skin_id> ──────────────────────────────
            case "give" -> {
                if (!checkPermission(sender, "collections.admin.give")) return true;
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /" + label + " give <player> <skin_id>"));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String playerName = args[1].trim();
                Player target = Bukkit.getPlayer(playerName);
                UUID targetUuid;
                String targetDisplay;

                if (target != null) {
                    targetUuid = target.getUniqueId();
                    targetDisplay = target.getName();
                } else {
                    @SuppressWarnings("deprecation")
                    OfflinePlayer off = Bukkit.getOfflinePlayer(playerName);
                    targetUuid = off.getUniqueId();
                    targetDisplay = off.getName() != null ? off.getName() : playerName;
                }

                String skinId = args[2].trim().toLowerCase(Locale.ROOT);
                CollectionSkin skin = manager.getSkin(skinId);
                if (skin == null) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cSkin &e" + skinId + " &cwas not found."));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                boolean unlocked = manager.unlockSkin(targetUuid, skinId);
                if (unlocked) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&aGave skin &b" + skin.getName() + " &a(&e" + skinId + "&a) to &f" + targetDisplay + "&a."));
                    if (target != null && target.isOnline()) {
                        target.sendMessage(ColorUtil.parse(getPrefix() + "&a✦ You unlocked the &b" + skin.getName() + " &acosmetic skin! Open &e/collections &ato equip it."));
                        SoundUtil.playSuccess(target);
                    }
                    if (sender instanceof Player p) SoundUtil.playSuccess(p);
                } else {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cFailed to give skin &e" + skinId + " &cto &f" + targetDisplay + "&c."));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                }
            }

            // ── /cadmin remove <player> <skin_id> ────────────────────────────
            case "remove" -> {
                if (!checkPermission(sender, "collections.admin.remove")) return true;
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /" + label + " remove <player> <skin_id>"));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String playerName = args[1].trim();
                Player target = Bukkit.getPlayer(playerName);
                UUID targetUuid;
                String targetDisplay;

                if (target != null) {
                    targetUuid = target.getUniqueId();
                    targetDisplay = target.getName();
                } else {
                    @SuppressWarnings("deprecation")
                    OfflinePlayer off = Bukkit.getOfflinePlayer(playerName);
                    targetUuid = off.getUniqueId();
                    targetDisplay = off.getName() != null ? off.getName() : playerName;
                }

                String skinId = args[2].trim().toLowerCase(Locale.ROOT);
                CollectionSkin skin = manager.getSkin(skinId);
                if (skin == null) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cSkin &e" + skinId + " &cwas not found."));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                boolean removed = manager.removeSkin(targetUuid, skinId);
                if (removed) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&aRemoved skin &b" + skin.getName() + " &afrom &f" + targetDisplay + "&a."));
                    if (target != null && target.isOnline()) {
                        target.sendMessage(ColorUtil.parse(getPrefix() + "&cYour access to cosmetic skin &e" + skin.getName() + " &chas been removed."));
                        SoundUtil.playError(target);
                    }
                    if (sender instanceof Player p) SoundUtil.playSuccess(p);
                } else {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cFailed to remove skin &e" + skinId + " &cfrom &f" + targetDisplay + "&c."));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                }
            }

            // ── /cadmin list [type] ──────────────────────────────────────────
            case "list" -> {
                CollectionType filterType = null;
                if (args.length >= 2) {
                    filterType = CollectionType.fromString(args[1]);
                }

                Collection<CollectionSkin> list = (filterType != null) ? manager.getSkinsForType(filterType) : manager.getAllSkins();
                sender.sendMessage(ColorUtil.parse(getPrefix() + "&aRegistered Cosmetic Skins &8(" + list.size() + "&8):"));
                for (CollectionSkin s : list) {
                    String status = s.isEnabled() ? "&a[ENABLED]" : "&c[DISABLED]";
                    sender.sendMessage(ColorUtil.parse("&8▪ &b" + s.getId() + " &7- &f" + s.getName() +
                            " &8(&e" + s.getType().name() + "&8, &d" + s.getCustomModel() + "&8) " + status));
                }
            }

            // ── /cadmin reload ───────────────────────────────────────────────
            case "reload" -> {
                if (!checkPermission(sender, "collections.admin.reload")) return true;
                manager.reload();
                sender.sendMessage(ColorUtil.parse(getPrefix() + "&aCollections database and skin definitions reloaded successfully."));
                if (sender instanceof Player p) SoundUtil.playSuccess(p);
            }

            default -> sendHelp(sender, label);
        }

        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ColorUtil.parse("&8======= &dEventsLib Cosmetic Collections Admin &8======="));
        sender.sendMessage(ColorUtil.parse("&b/" + label + " create <id> <type> <material> <model> &7- Create new skin"));
        sender.sendMessage(ColorUtil.parse("&b/" + label + " delete <id> &7- Delete a skin"));
        sender.sendMessage(ColorUtil.parse("&b/" + label + " edit <id> <name/model/desc/enabled/material> <val> &7- Edit skin"));
        sender.sendMessage(ColorUtil.parse("&b/" + label + " give <player> <skin_id> &7- Give skin to player"));
        sender.sendMessage(ColorUtil.parse("&b/" + label + " remove <player> <skin_id> &7- Revoke skin from player"));
        sender.sendMessage(ColorUtil.parse("&b/" + label + " list [type] &7- List registered skins"));
        sender.sendMessage(ColorUtil.parse("&b/" + label + " reload &7- Reload skins from database"));
        sender.sendMessage(ColorUtil.parse("&8================================================="));
    }

    private String capitalizeWords(String str) {
        String[] words = str.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) {
                    sb.append(w.substring(1).toLowerCase());
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("collections.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = List.of("create", "delete", "edit", "give", "remove", "list", "reload");
            for (String s : subs) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(s);
                }
            }
            return completions;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("create")) {
            if (args.length == 3) {
                for (CollectionType t : CollectionType.values()) {
                    if (t.name().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(t.name());
                    }
                }
            } else if (args.length == 4) {
                CollectionType type = CollectionType.fromString(args[2]);
                if (type != null) {
                    completions.add(type.getBaseMaterial().name());
                } else {
                    for (Material m : Material.values()) {
                        if (m.name().toLowerCase().startsWith(args[3].toLowerCase())) {
                            completions.add(m.name());
                            if (completions.size() >= 30) break;
                        }
                    }
                }
            } else if (args.length == 5) {
                completions.add("khmc:sword/custom_skin");
            }
        } else if (sub.equals("delete") || sub.equals("edit")) {
            if (args.length == 2) {
                for (CollectionSkin s : manager.getAllSkins()) {
                    if (s.getId().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(s.getId());
                    }
                }
            } else if (sub.equals("edit") && args.length == 3) {
                for (String p : List.of("name", "model", "description", "enabled", "material")) {
                    if (p.startsWith(args[2].toLowerCase())) completions.add(p);
                }
            }
        } else if (sub.equals("give") || sub.equals("remove")) {
            if (args.length == 2) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            } else if (args.length == 3) {
                for (CollectionSkin s : manager.getAllSkins()) {
                    if (s.getId().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(s.getId());
                    }
                }
            }
        } else if (sub.equals("list")) {
            if (args.length == 2) {
                for (CollectionType t : CollectionType.values()) {
                    if (t.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(t.name());
                    }
                }
            }
        }

        return completions;
    }
}
