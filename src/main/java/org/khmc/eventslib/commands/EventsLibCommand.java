package org.khmc.eventslib.commands;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.khmc.eventslib.EventsLibPlugin;
import org.khmc.eventslib.gui.EventEditorGUI;
import org.khmc.eventslib.gui.EventHubGUI;
import org.khmc.eventslib.gui.EventShopGUI;
import org.khmc.eventslib.gui.EventViewerGUI;
import org.khmc.eventslib.gui.EventsDirectoryGUI;
import org.khmc.eventslib.model.EventModel;
import org.khmc.eventslib.model.EventPrivacy;
import org.khmc.eventslib.util.ColorUtil;
import org.khmc.eventslib.util.SoundUtil;
import org.khmc.eventslib.util.TimeResetUtil;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

public class EventsLibCommand implements CommandExecutor, TabCompleter {

    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,32}$");
    private final EventsLibPlugin plugin;

    public EventsLibCommand(EventsLibPlugin plugin) {
        this.plugin = plugin;
    }

    private String getPrefix() {
        return plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ");
    }

    private boolean checkAdmin(CommandSender sender) {
        if (sender.isOp() || sender.hasPermission("eventslib.admin")) {
            return true;
        }
        sender.sendMessage(ColorUtil.parse(getPrefix() + plugin.getConfig().getString("messages.no-permission", "&cYou do not have permission to execute this command.")));
        if (sender instanceof Player p) {
            SoundUtil.playError(p);
        }
        return false;
    }

    private String getItemDisplayName(ItemStack item) {
        if (item == null) return "Item";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return LegacyComponentSerializer.legacyAmpersand().serialize(item.getItemMeta().displayName());
        }
        String name = item.getType().name().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                new EventsDirectoryGUI(plugin).open(player);
            } else {
                sendHelp(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {

            // ── /el create <id> ──────────────────────────────────────────────
            case "create" -> {
                if (!checkAdmin(sender)) return true;

                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el create <id>"));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String rawId = args[1].trim();
                if (!ID_PATTERN.matcher(rawId).matches()) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + plugin.getConfig().getString("messages.invalid-id", "&cEvent ID may only contain alphanumeric characters, underscores, and hyphens (max 32 chars).")));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String id = rawId.toLowerCase(Locale.ROOT);
                if (plugin.getEventManager().hasEvent(id)) {
                    String msg = plugin.getConfig().getString("messages.event-already-exists", "&cAn event with ID &e{id} &calready exists!")
                            .replace("{id}", id);
                    sender.sendMessage(ColorUtil.parse(getPrefix() + msg));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                Player creator = (sender instanceof Player p) ? p : null;
                EventModel createdEvent = plugin.getEventManager().createEvent(id, creator);

                if (createdEvent != null) {
                    String msg = plugin.getConfig().getString("messages.event-created", "&aEvent &e{id} &asuccessfully created! Default privacy: &cPRIVATE&a.")
                            .replace("{id}", id);
                    sender.sendMessage(ColorUtil.parse(getPrefix() + msg));

                    if (sender instanceof Player player) {
                        SoundUtil.playSuccess(player);
                        new EventHubGUI(plugin, createdEvent).open(player);
                    }
                } else {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cFailed to create event."));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                }
            }

            // ── /el privacy <id> <private/public> ──────────────────────────────
            case "privacy" -> {
                if (!checkAdmin(sender)) return true;

                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el privacy <id> <private/public>"));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String id = args[1].trim().toLowerCase(Locale.ROOT);
                EventModel event = plugin.getEventManager().getEvent(id);
                if (event == null) {
                    String msg = plugin.getConfig().getString("messages.event-not-found", "&cEvent &e{id} &cwas not found.")
                            .replace("{id}", id);
                    sender.sendMessage(ColorUtil.parse(getPrefix() + msg));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String privacyArg = args[2].trim().toLowerCase(Locale.ROOT);
                EventPrivacy newPrivacy;
                if (privacyArg.equals("private") || privacyArg.equals("priv")) {
                    newPrivacy = EventPrivacy.PRIVATE;
                } else if (privacyArg.equals("public") || privacyArg.equals("pub")) {
                    newPrivacy = EventPrivacy.PUBLIC;
                } else {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + plugin.getConfig().getString("messages.invalid-privacy", "&cInvalid privacy type! Available choices: &eprivate &cor &epublic&c.")));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                plugin.getEventManager().setEventPrivacy(id, newPrivacy);
                String msg = plugin.getConfig().getString("messages.privacy-updated", "&aEvent &e{id} &aprivacy updated to &b{privacy}&a.")
                        .replace("{id}", id)
                        .replace("{privacy}", newPrivacy.getFormattedDisplay());
                sender.sendMessage(ColorUtil.parse(getPrefix() + msg));

                if (sender instanceof Player p) {
                    SoundUtil.playToggle(p);
                }
            }

            // ── /el model <id> <string> ───────────────────────────────────────
            case "model", "cmd", "custommodel" -> {
                if (!checkAdmin(sender)) return true;

                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el model <id> <string> (use 'reset' or '0' to clear)"));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String id = args[1].trim().toLowerCase(Locale.ROOT);
                EventModel event = plugin.getEventManager().getEvent(id);
                if (event == null) {
                    String msg = plugin.getConfig().getString("messages.event-not-found", "&cEvent &e{id} &cwas not found.")
                            .replace("{id}", id);
                    sender.sendMessage(ColorUtil.parse(getPrefix() + msg));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String modelStr = args[2].trim();
                plugin.getEventManager().setEventCustomModelData(id, modelStr);

                if (modelStr.equalsIgnoreCase("reset") || modelStr.equalsIgnoreCase("0") || modelStr.equalsIgnoreCase("none")) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&aEvent &e" + id + " &acustom model data has been reset to default."));
                } else {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&aEvent &e" + id + " &acustom model data set to &b" + modelStr + "&a."));
                }

                if (sender instanceof Player p) {
                    SoundUtil.playToggle(p);
                }
            }

            // ── /el setitem <id> [daily_amount] ───────────────────────────────
            case "setitem", "item" -> {
                if (!checkAdmin(sender)) return true;
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cOnly players can hold items to set as event items."));
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el setitem <id> [daily_amount]"));
                    SoundUtil.playError(player);
                    return true;
                }

                String id = args[1].trim().toLowerCase(Locale.ROOT);
                EventModel event = plugin.getEventManager().getEvent(id);
                if (event == null) {
                    String msg = plugin.getConfig().getString("messages.event-not-found", "&cEvent &e{id} &cwas not found.")
                            .replace("{id}", id);
                    player.sendMessage(ColorUtil.parse(getPrefix() + msg));
                    SoundUtil.playError(player);
                    return true;
                }

                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || held.getType().isAir()) {
                    player.sendMessage(ColorUtil.parse(getPrefix() + "&cYou must hold an item in your main hand to set as the event item!"));
                    SoundUtil.playError(player);
                    return true;
                }

                int dailyAmount = 1;
                if (args.length >= 3) {
                    try {
                        dailyAmount = Math.max(1, Integer.parseInt(args[2].trim()));
                    } catch (NumberFormatException e) {
                        player.sendMessage(ColorUtil.parse(getPrefix() + "&cDaily amount must be a positive number!"));
                        SoundUtil.playError(player);
                        return true;
                    }
                }

                plugin.getEventManager().setEventItem(id, held, dailyAmount);
                String itemName = getItemDisplayName(held);
                player.sendMessage(ColorUtil.parse(getPrefix() + "&aEvent &e" + id + " &acollectible item set to &f" + itemName +
                        " &a(Daily claim amount: &e" + dailyAmount + "x&a)."));
                SoundUtil.playSuccess(player);
            }

            // ── /el shop <add/remove/id> ───────────────────────────────────────
            case "shop" -> {
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el shop <id> OR /el shop add <id> <price> OR /el shop remove <id> <item_id>"));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String secondArg = args[1].toLowerCase(Locale.ROOT);

                // /el shop add <id> <price>
                if (secondArg.equals("add")) {
                    if (!checkAdmin(sender)) return true;
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(ColorUtil.parse(getPrefix() + "&cOnly players can hold items to add to the shop."));
                        return true;
                    }

                    if (args.length < 4) {
                        player.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el shop add <id> <price>"));
                        SoundUtil.playError(player);
                        return true;
                    }

                    String id = args[2].trim().toLowerCase(Locale.ROOT);
                    EventModel event = plugin.getEventManager().getEvent(id);
                    if (event == null) {
                        player.sendMessage(ColorUtil.parse(getPrefix() + "&cEvent &e" + id + " &cwas not found."));
                        SoundUtil.playError(player);
                        return true;
                    }

                    int price;
                    try {
                        price = Integer.parseInt(args[3].trim());
                        if (price <= 0) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        player.sendMessage(ColorUtil.parse(getPrefix() + "&cPrice must be a positive number!"));
                        SoundUtil.playError(player);
                        return true;
                    }

                    ItemStack held = player.getInventory().getItemInMainHand();
                    if (held == null || held.getType().isAir()) {
                        player.sendMessage(ColorUtil.parse(getPrefix() + "&cYou must hold an item in your main hand to add to the shop!"));
                        SoundUtil.playError(player);
                        return true;
                    }

                    boolean added = plugin.getEventManager().addShopItem(id, price, held);
                    if (added) {
                        String itemName = getItemDisplayName(held);
                        player.sendMessage(ColorUtil.parse(getPrefix() + "&aAdded &f" + itemName + " &ato event &e" + id +
                                " &ashop for &e" + price + "x &aevent items!"));
                        SoundUtil.playSuccess(player);
                    } else {
                        player.sendMessage(ColorUtil.parse(getPrefix() + "&cFailed to add item to shop."));
                        SoundUtil.playError(player);
                    }
                    return true;
                }

                // /el shop remove <id> <item_id>
                if (secondArg.equals("remove") || secondArg.equals("del") || secondArg.equals("delete")) {
                    if (!checkAdmin(sender)) return true;

                    if (args.length < 4) {
                        sender.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el shop remove <id> <item_id>"));
                        if (sender instanceof Player p) SoundUtil.playError(p);
                        return true;
                    }

                    String id = args[2].trim().toLowerCase(Locale.ROOT);
                    EventModel event = plugin.getEventManager().getEvent(id);
                    if (event == null) {
                        sender.sendMessage(ColorUtil.parse(getPrefix() + "&cEvent &e" + id + " &cwas not found."));
                        if (sender instanceof Player p) SoundUtil.playError(p);
                        return true;
                    }

                    int shopItemId;
                    try {
                        shopItemId = Integer.parseInt(args[3].trim());
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ColorUtil.parse(getPrefix() + "&cItem ID must be a valid number!"));
                        if (sender instanceof Player p) SoundUtil.playError(p);
                        return true;
                    }

                    boolean removed = plugin.getEventManager().removeShopItem(id, shopItemId);
                    if (removed) {
                        sender.sendMessage(ColorUtil.parse(getPrefix() + "&aRemoved shop item #" + shopItemId + " from event &e" + id + "&a."));
                        if (sender instanceof Player p) SoundUtil.playSuccess(p);
                    } else {
                        sender.sendMessage(ColorUtil.parse(getPrefix() + "&cShop item #" + shopItemId + " not found in event &e" + id + "&c."));
                        if (sender instanceof Player p) SoundUtil.playError(p);
                    }
                    return true;
                }

                // /el shop <id> (open shop)
                if (sender instanceof Player player) {
                    String id = secondArg;
                    EventModel event = plugin.getEventManager().getEvent(id);
                    if (event == null) {
                        player.sendMessage(ColorUtil.parse(getPrefix() + "&cEvent &e" + id + " &cwas not found."));
                        SoundUtil.playError(player);
                        return true;
                    }
                    new EventShopGUI(plugin, event).open(player);
                } else {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cOnly players can open the event shop GUI."));
                }
            }

            // ── /el spincost <id> <cost> ─────────────────────────────────────
            case "spincost", "setspincost" -> {
                if (!checkAdmin(sender)) return true;

                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el spincost <id> <cost>"));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String id = args[1].trim().toLowerCase(Locale.ROOT);
                EventModel event = plugin.getEventManager().getEvent(id);
                if (event == null) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cEvent &e" + id + " &cwas not found."));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                double cost;
                try {
                    cost = Double.parseDouble(args[2].trim());
                    if (cost < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cCost must be a valid positive number!"));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                plugin.getEventManager().setEventSpinCost(id, cost);
                sender.sendMessage(ColorUtil.parse(getPrefix() + "&aEvent &e" + id + " &aspin cost set to &d" +
                        org.khmc.eventslib.hook.ShardsHook.format(cost) + "&a."));
                if (sender instanceof Player p) SoundUtil.playSuccess(p);
            }

            // ── /el spin <add/remove/list/id> ─────────────────────────────────
            case "spin", "wheel" -> {
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el spin <id> OR /el spin add <id> [weight] OR /el spin remove <id> <id> OR /el spin list <id>"));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String secondArg = args[1].toLowerCase(Locale.ROOT);

                // /el spin add <id> [weight]
                if (secondArg.equals("add")) {
                    if (!checkAdmin(sender)) return true;
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(ColorUtil.parse(getPrefix() + "&cOnly players can hold items to add spin rewards."));
                        return true;
                    }

                    if (args.length < 3) {
                        player.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el spin add <id> [weight]"));
                        SoundUtil.playError(player);
                        return true;
                    }

                    String id = args[2].trim().toLowerCase(Locale.ROOT);
                    EventModel event = plugin.getEventManager().getEvent(id);
                    if (event == null) {
                        player.sendMessage(ColorUtil.parse(getPrefix() + "&cEvent &e" + id + " &cwas not found."));
                        SoundUtil.playError(player);
                        return true;
                    }

                    int weight = 10;
                    if (args.length >= 4) {
                        try {
                            weight = Math.max(1, Integer.parseInt(args[3].trim()));
                        } catch (NumberFormatException e) {
                            player.sendMessage(ColorUtil.parse(getPrefix() + "&cWeight must be a positive integer!"));
                            SoundUtil.playError(player);
                            return true;
                        }
                    }

                    ItemStack held = player.getInventory().getItemInMainHand();
                    if (held == null || held.getType().isAir()) {
                        player.sendMessage(ColorUtil.parse(getPrefix() + "&cYou must hold an item in your main hand to add as a spin reward!"));
                        SoundUtil.playError(player);
                        return true;
                    }

                    boolean added = plugin.getEventManager().addSpinReward(id, weight, held);
                    if (added) {
                        String itemName = getItemDisplayName(held);
                        player.sendMessage(ColorUtil.parse(getPrefix() + "&aAdded &f" + itemName + " &ato event &e" + id +
                                " &aspin pool with weight &b" + weight + "&a!"));
                        SoundUtil.playSuccess(player);
                    } else {
                        player.sendMessage(ColorUtil.parse(getPrefix() + "&cFailed to add spin reward."));
                        SoundUtil.playError(player);
                    }
                    return true;
                }

                // /el spin remove <id> <reward_id>
                if (secondArg.equals("remove") || secondArg.equals("del") || secondArg.equals("delete")) {
                    if (!checkAdmin(sender)) return true;

                    if (args.length < 4) {
                        sender.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el spin remove <id> <reward_id>"));
                        if (sender instanceof Player p) SoundUtil.playError(p);
                        return true;
                    }

                    String id = args[2].trim().toLowerCase(Locale.ROOT);
                    EventModel event = plugin.getEventManager().getEvent(id);
                    if (event == null) {
                        sender.sendMessage(ColorUtil.parse(getPrefix() + "&cEvent &e" + id + " &cwas not found."));
                        if (sender instanceof Player p) SoundUtil.playError(p);
                        return true;
                    }

                    int rewardId;
                    try {
                        rewardId = Integer.parseInt(args[3].trim());
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ColorUtil.parse(getPrefix() + "&cReward ID must be a valid number!"));
                        if (sender instanceof Player p) SoundUtil.playError(p);
                        return true;
                    }

                    boolean removed = plugin.getEventManager().removeSpinReward(id, rewardId);
                    if (removed) {
                        sender.sendMessage(ColorUtil.parse(getPrefix() + "&aRemoved spin reward #" + rewardId + " from event &e" + id + "&a."));
                        if (sender instanceof Player p) SoundUtil.playSuccess(p);
                    } else {
                        sender.sendMessage(ColorUtil.parse(getPrefix() + "&cSpin reward #" + rewardId + " not found in event &e" + id + "&c."));
                        if (sender instanceof Player p) SoundUtil.playError(p);
                    }
                    return true;
                }

                // /el spin list <id>
                if (secondArg.equals("list")) {
                    if (args.length < 3) {
                        sender.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el spin list <id>"));
                        if (sender instanceof Player p) SoundUtil.playError(p);
                        return true;
                    }

                    String id = args[2].trim().toLowerCase(Locale.ROOT);
                    EventModel event = plugin.getEventManager().getEvent(id);
                    if (event == null) {
                        sender.sendMessage(ColorUtil.parse(getPrefix() + "&cEvent &e" + id + " &cwas not found."));
                        if (sender instanceof Player p) SoundUtil.playError(p);
                        return true;
                    }

                    int totalWeight = event.getTotalSpinWeight();
                    sender.sendMessage(ColorUtil.parse("&8================= [&dEvent Spin Rewards: " + event.getId() + "&8] ================="));
                    sender.sendMessage(ColorUtil.parse("&7Spin Cost: &d" + org.khmc.eventslib.hook.ShardsHook.format(event.getSpinCost())));
                    sender.sendMessage(ColorUtil.parse("&7Total Weight: &e" + totalWeight));
                    if (event.getSpinRewards().isEmpty()) {
                        sender.sendMessage(ColorUtil.parse("&7No rewards configured yet."));
                    } else {
                        for (org.khmc.eventslib.model.EventSpinReward r : event.getSpinRewards()) {
                            double chance = r.getChancePercent(totalWeight);
                            String itemName = getItemDisplayName(r.getItem());
                            sender.sendMessage(ColorUtil.parse("&7- &e#" + r.getId() + " &f" + itemName +
                                    " &8| &7Weight: &b" + r.getWeight() + " &8(&e" + String.format(Locale.US, "%.1f%%", chance) + "&8)"));
                        }
                    }
                    sender.sendMessage(ColorUtil.parse("&8=========================================================="));
                    return true;
                }

                // /el spin <id> (open spin gui)
                if (sender instanceof Player player) {
                    String id = secondArg;
                    EventModel event = plugin.getEventManager().getEvent(id);
                    if (event == null) {
                        player.sendMessage(ColorUtil.parse(getPrefix() + "&cEvent &e" + id + " &cwas not found."));
                        SoundUtil.playError(player);
                        return true;
                    }
                    new org.khmc.eventslib.gui.EventSpinGUI(plugin, event).open(player);
                } else {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cOnly players can open the event spin GUI."));
                }
            }

            // ── /el claim <id> ────────────────────────────────────────────────
            case "claim" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cOnly players can claim daily event items."));
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el claim <id>"));
                    SoundUtil.playError(player);
                    return true;
                }

                String id = args[1].trim().toLowerCase(Locale.ROOT);
                EventModel event = plugin.getEventManager().getEvent(id);
                if (event == null) {
                    player.sendMessage(ColorUtil.parse(getPrefix() + "&cEvent &e" + id + " &cwas not found."));
                    SoundUtil.playError(player);
                    return true;
                }

                if (event.getEventItem() == null) {
                    player.sendMessage(ColorUtil.parse(getPrefix() + "&cNo daily login item configured for event &e" + id + "&c."));
                    SoundUtil.playError(player);
                    return true;
                }

                if (plugin.getEventManager().hasClaimedToday(player.getUniqueId(), event.getId())) {
                    Duration left = TimeResetUtil.getTimeUntilNextReset();
                    player.sendMessage(ColorUtil.parse(getPrefix() + "&cYou have already claimed today's daily reward for &e" + id +
                            "&c! Next reset is at 3:00 PM SGT (in &e" + TimeResetUtil.formatDuration(left) + "&c)."));
                    SoundUtil.playError(player);
                    return true;
                }

                boolean claimed = plugin.getEventManager().claimDailyReward(player, event);
                if (claimed) {
                    SoundUtil.playSuccess(player);
                    String itemName = getItemDisplayName(event.getEventItem());
                    player.sendMessage(ColorUtil.parse(getPrefix() + "&aClaimed daily reward: &e" + event.getDailyClaimAmount() + "x &f" + itemName + "&a!"));
                } else {
                    SoundUtil.playError(player);
                }
            }

            // ── /el open <id> / /el hub <id> ──────────────────────────────────
            case "open", "hub" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cOnly players can open event GUIs."));
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el open <id>"));
                    SoundUtil.playError(player);
                    return true;
                }

                String id = args[1].trim().toLowerCase(Locale.ROOT);
                EventModel event = plugin.getEventManager().getEvent(id);
                if (event == null) {
                    String msg = plugin.getConfig().getString("messages.event-not-found", "&cEvent &e{id} &cwas not found.")
                            .replace("{id}", id);
                    player.sendMessage(ColorUtil.parse(getPrefix() + msg));
                    SoundUtil.playError(player);
                    return true;
                }

                new EventHubGUI(plugin, event).open(player);
            }

            // ── /el view <id> ─────────────────────────────────────────────────
            case "view", "layout" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cOnly players can open event GUIs."));
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el view <id>"));
                    SoundUtil.playError(player);
                    return true;
                }

                String id = args[1].trim().toLowerCase(Locale.ROOT);
                EventModel event = plugin.getEventManager().getEvent(id);
                if (event == null) {
                    String msg = plugin.getConfig().getString("messages.event-not-found", "&cEvent &e{id} &cwas not found.")
                            .replace("{id}", id);
                    player.sendMessage(ColorUtil.parse(getPrefix() + msg));
                    SoundUtil.playError(player);
                    return true;
                }

                new EventViewerGUI(plugin, event).open(player);
            }

            // ── /el edit <id> ─────────────────────────────────────────────────
            case "edit" -> {
                if (!checkAdmin(sender)) return true;
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cOnly players can open the event editor GUI."));
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el edit <id>"));
                    SoundUtil.playError(player);
                    return true;
                }

                String id = args[1].trim().toLowerCase(Locale.ROOT);
                EventModel event = plugin.getEventManager().getEvent(id);
                if (event == null) {
                    String msg = plugin.getConfig().getString("messages.event-not-found", "&cEvent &e{id} &cwas not found.")
                            .replace("{id}", id);
                    player.sendMessage(ColorUtil.parse(getPrefix() + msg));
                    SoundUtil.playError(player);
                    return true;
                }

                new EventEditorGUI(plugin, event).open(player);
            }

            // ── /el list ──────────────────────────────────────────────────────
            case "list" -> {
                boolean isAdmin = sender.isOp() || sender.hasPermission("eventslib.admin");
                Collection<EventModel> list = isAdmin ?
                        plugin.getEventManager().getAllEvents() :
                        plugin.getEventManager().getPublicEvents();

                sender.sendMessage(ColorUtil.parse("&8================= [&dEventsLib&8] ================="));
                if (list.isEmpty()) {
                    sender.sendMessage(ColorUtil.parse("&7No events available."));
                } else {
                    for (EventModel ev : list) {
                        String privacyStr = ev.getPrivacy().getFormattedDisplay();
                        sender.sendMessage(ColorUtil.parse("&7- &e" + ev.getId() + " &8| &7Privacy: " + privacyStr +
                                " &8| &7Items: &f" + ev.getOccupiedSlotsCount() + "/54" +
                                " &8| &7Shop: &e" + ev.getShopItems().size()));
                    }
                }
                sender.sendMessage(ColorUtil.parse("&8================================================"));
            }

            // ── /el delete <id> ───────────────────────────────────────────────
            case "delete" -> {
                if (!checkAdmin(sender)) return true;

                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el delete <id>"));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String id = args[1].trim().toLowerCase(Locale.ROOT);
                boolean deleted = plugin.getEventManager().deleteEvent(id);
                if (deleted) {
                    String msg = plugin.getConfig().getString("messages.event-deleted", "&cEvent &e{id} &cdeleted successfully.")
                            .replace("{id}", id);
                    sender.sendMessage(ColorUtil.parse(getPrefix() + msg));
                    if (sender instanceof Player p) SoundUtil.playSuccess(p);
                } else {
                    String msg = plugin.getConfig().getString("messages.event-not-found", "&cEvent &e{id} &cwas not found.")
                            .replace("{id}", id);
                    sender.sendMessage(ColorUtil.parse(getPrefix() + msg));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                }
            }

            // ── /el info <id> ─────────────────────────────────────────────────
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + "&cUsage: /el info <id>"));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                String id = args[1].trim().toLowerCase(Locale.ROOT);
                EventModel event = plugin.getEventManager().getEvent(id);
                if (event == null) {
                    String msg = plugin.getConfig().getString("messages.event-not-found", "&cEvent &e{id} &cwas not found.")
                            .replace("{id}", id);
                    sender.sendMessage(ColorUtil.parse(getPrefix() + msg));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                if (!plugin.getEventManager().canAccess((sender instanceof Player p ? p : null), event)) {
                    sender.sendMessage(ColorUtil.parse(getPrefix() + plugin.getConfig().getString("messages.event-private", "&cThis event is currently private.")));
                    if (sender instanceof Player p) SoundUtil.playError(p);
                    return true;
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Duration untilReset = TimeResetUtil.getTimeUntilNextReset();
                sender.sendMessage(ColorUtil.parse("&8================= [&dEvent Info: " + event.getId() + "&8] ================="));
                sender.sendMessage(ColorUtil.parse("&7Title: &f" + event.getTitle()));
                sender.sendMessage(ColorUtil.parse("&7Privacy: " + event.getPrivacy().getFormattedDisplay()));
                sender.sendMessage(ColorUtil.parse("&7Model Data: &b" + (event.hasCustomModelData() ? event.getCustomModelData() : "None (Default)")));
                sender.sendMessage(ColorUtil.parse("&7Event Item: &e" + (event.getEventItem() != null ? getItemDisplayName(event.getEventItem()) : "Not set") +
                        " &7(Daily amount: &f" + event.getDailyClaimAmount() + "&7)"));
                sender.sendMessage(ColorUtil.parse("&7Next Daily Reset: &e" + TimeResetUtil.formatDuration(untilReset) + " &7(3:00 PM SGT)"));
                sender.sendMessage(ColorUtil.parse("&7Shop Items: &e" + event.getShopItems().size()));
                sender.sendMessage(ColorUtil.parse("&7Occupied Slots: &e" + event.getOccupiedSlotsCount() + "/54"));
                sender.sendMessage(ColorUtil.parse("&7Creator: &f" + event.getCreatorName()));
                sender.sendMessage(ColorUtil.parse("&7Created At: &f" + sdf.format(new Date(event.getCreatedAt()))));
                sender.sendMessage(ColorUtil.parse("&8=========================================================="));
            }

            // ── /el reload ────────────────────────────────────────────────────
            case "reload" -> {
                if (!checkAdmin(sender)) return true;
                plugin.reloadConfig();
                plugin.getEventManager().loadAll();
                sender.sendMessage(ColorUtil.parse(getPrefix() + plugin.getConfig().getString("messages.reloaded", "&aConfiguration and events reloaded successfully.")));
                if (sender instanceof Player p) SoundUtil.playSuccess(p);
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        boolean isAdmin = sender.isOp() || sender.hasPermission("eventslib.admin");
        sender.sendMessage(ColorUtil.parse("&8================= [&dEventsLib Commands&8] ================="));
        sender.sendMessage(ColorUtil.parse("&e/el &7- Open Events Directory GUI"));
        sender.sendMessage(ColorUtil.parse("&e/el open <id> &7- Open Event Hub (Items, Shop, Layout, Spin)"));
        sender.sendMessage(ColorUtil.parse("&e/el spin <id> &7- Open Event Spin Wheel (Shards)"));
        sender.sendMessage(ColorUtil.parse("&e/el shop <id> &7- Open Event Shop"));
        sender.sendMessage(ColorUtil.parse("&e/el claim <id> &7- Claim daily login event reward (3 PM SGT)"));
        sender.sendMessage(ColorUtil.parse("&e/el view <id> &7- View 54-slot showcase layout"));
        sender.sendMessage(ColorUtil.parse("&e/el list &7- List active events"));
        sender.sendMessage(ColorUtil.parse("&e/el info <id> &7- Show event details"));
        if (isAdmin) {
            sender.sendMessage(ColorUtil.parse("&dAdmin Management:"));
            sender.sendMessage(ColorUtil.parse("&b/el create <id> &7- Create new event"));
            sender.sendMessage(ColorUtil.parse("&b/el setitem <id> [daily_amount] &7- Set held item as event collectible item"));
            sender.sendMessage(ColorUtil.parse("&b/el spincost <id> <cost> &7- Set spin cost in Shards"));
            sender.sendMessage(ColorUtil.parse("&b/el spin add <id> [weight] &7- Add held item to spin wheel rewards"));
            sender.sendMessage(ColorUtil.parse("&b/el spin remove <id> <reward_id> &7- Remove reward from spin pool"));
            sender.sendMessage(ColorUtil.parse("&b/el spin list <id> &7- List all spin rewards and weights"));
            sender.sendMessage(ColorUtil.parse("&b/el shop add <id> <price> &7- Add held item to event shop"));
            sender.sendMessage(ColorUtil.parse("&b/el shop remove <id> <item_id> &7- Remove item from event shop"));
            sender.sendMessage(ColorUtil.parse("&b/el model <id> <string> &7- Set icon custom model data string"));
            sender.sendMessage(ColorUtil.parse("&b/el privacy <id> <private/public> &7- Set event privacy"));
            sender.sendMessage(ColorUtil.parse("&b/el edit <id> &7- Open 54-slot editor"));
            sender.sendMessage(ColorUtil.parse("&b/el delete <id> &7- Delete an event"));
            sender.sendMessage(ColorUtil.parse("&b/el reload &7- Reload config & database"));
        }
        sender.sendMessage(ColorUtil.parse("&8======================================================"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean isAdmin = sender.isOp() || sender.hasPermission("eventslib.admin");

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase(Locale.ROOT);
            List<String> options = new ArrayList<>(Arrays.asList("open", "spin", "shop", "claim", "view", "list", "info"));
            if (isAdmin) {
                options.addAll(Arrays.asList("create", "privacy", "model", "setitem", "spincost", "edit", "delete", "reload"));
            }
            for (String opt : options) {
                if (opt.startsWith(input)) {
                    completions.add(opt);
                }
            }
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("open") || sub.equals("view") || sub.equals("info") || sub.equals("claim") ||
                    (isAdmin && (sub.equals("privacy") || sub.equals("model") || sub.equals("cmd") || sub.equals("setitem") || sub.equals("spincost") || sub.equals("edit") || sub.equals("delete")))) {
                List<String> completions = new ArrayList<>();
                String input = args[1].toLowerCase(Locale.ROOT);
                Collection<EventModel> events = isAdmin ?
                        plugin.getEventManager().getAllEvents() :
                        plugin.getEventManager().getPublicEvents();
                for (EventModel ev : events) {
                    if (ev.getId().startsWith(input)) {
                        completions.add(ev.getId());
                    }
                }
                return completions;
            }

            if (sub.equals("shop")) {
                List<String> completions = new ArrayList<>();
                String input = args[1].toLowerCase(Locale.ROOT);
                if (isAdmin) {
                    for (String opt : Arrays.asList("add", "remove")) {
                        if (opt.startsWith(input)) completions.add(opt);
                    }
                }
                Collection<EventModel> events = isAdmin ?
                        plugin.getEventManager().getAllEvents() :
                        plugin.getEventManager().getPublicEvents();
                for (EventModel ev : events) {
                    if (ev.getId().startsWith(input)) completions.add(ev.getId());
                }
                return completions;
            }

            if (sub.equals("spin") || sub.equals("wheel")) {
                List<String> completions = new ArrayList<>();
                String input = args[1].toLowerCase(Locale.ROOT);
                if (isAdmin) {
                    for (String opt : Arrays.asList("add", "remove", "list")) {
                        if (opt.startsWith(input)) completions.add(opt);
                    }
                }
                Collection<EventModel> events = isAdmin ?
                        plugin.getEventManager().getAllEvents() :
                        plugin.getEventManager().getPublicEvents();
                for (EventModel ev : events) {
                    if (ev.getId().startsWith(input)) completions.add(ev.getId());
                }
                return completions;
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (isAdmin && sub.equals("privacy")) {
                List<String> completions = new ArrayList<>();
                String input = args[2].toLowerCase(Locale.ROOT);
                for (String choice : Arrays.asList("private", "public")) {
                    if (choice.startsWith(input)) {
                        completions.add(choice);
                    }
                }
                return completions;
            }
            if (isAdmin && (sub.equals("model") || sub.equals("cmd") || sub.equals("custommodel"))) {
                List<String> completions = new ArrayList<>();
                String input = args[2].toLowerCase(Locale.ROOT);
                for (String choice : Arrays.asList("0", "reset", "1001", "1002", "summer_event_icon")) {
                    if (choice.startsWith(input)) completions.add(choice);
                }
                return completions;
            }
            if (isAdmin && sub.equals("setitem")) {
                List<String> completions = new ArrayList<>();
                String input = args[2].toLowerCase(Locale.ROOT);
                for (String choice : Arrays.asList("1", "2", "5", "10")) {
                    if (choice.startsWith(input)) completions.add(choice);
                }
                return completions;
            }
            if (isAdmin && sub.equals("spincost")) {
                List<String> completions = new ArrayList<>();
                String input = args[2].toLowerCase(Locale.ROOT);
                for (String choice : Arrays.asList("5", "10", "20", "50", "100")) {
                    if (choice.startsWith(input)) completions.add(choice);
                }
                return completions;
            }
            if (isAdmin && sub.equals("shop") && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
                List<String> completions = new ArrayList<>();
                String input = args[2].toLowerCase(Locale.ROOT);
                for (EventModel ev : plugin.getEventManager().getAllEvents()) {
                    if (ev.getId().startsWith(input)) completions.add(ev.getId());
                }
                return completions;
            }
            if ((sub.equals("spin") || sub.equals("wheel")) && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("list"))) {
                List<String> completions = new ArrayList<>();
                String input = args[2].toLowerCase(Locale.ROOT);
                Collection<EventModel> evs = isAdmin ? plugin.getEventManager().getAllEvents() : plugin.getEventManager().getPublicEvents();
                for (EventModel ev : evs) {
                    if (ev.getId().startsWith(input)) completions.add(ev.getId());
                }
                return completions;
            }
        }

        if (args.length == 4 && isAdmin) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("shop") && args[1].equalsIgnoreCase("add")) {
                List<String> completions = new ArrayList<>();
                String input = args[3].toLowerCase(Locale.ROOT);
                for (String choice : Arrays.asList("1", "5", "10", "20", "50", "100")) {
                    if (choice.startsWith(input)) completions.add(choice);
                }
                return completions;
            }
            if (sub.equals("shop") && args[1].equalsIgnoreCase("remove")) {
                String eventId = args[2].toLowerCase(Locale.ROOT);
                EventModel ev = plugin.getEventManager().getEvent(eventId);
                if (ev != null) {
                    List<String> completions = new ArrayList<>();
                    String input = args[3].toLowerCase(Locale.ROOT);
                    for (var item : ev.getShopItems()) {
                        String idStr = String.valueOf(item.getId());
                        if (idStr.startsWith(input)) completions.add(idStr);
                    }
                    return completions;
                }
            }
            if ((sub.equals("spin") || sub.equals("wheel")) && args[1].equalsIgnoreCase("add")) {
                List<String> completions = new ArrayList<>();
                String input = args[3].toLowerCase(Locale.ROOT);
                for (String choice : Arrays.asList("1", "5", "10", "25", "50", "100")) {
                    if (choice.startsWith(input)) completions.add(choice);
                }
                return completions;
            }
            if ((sub.equals("spin") || sub.equals("wheel")) && args[1].equalsIgnoreCase("remove")) {
                String eventId = args[2].toLowerCase(Locale.ROOT);
                EventModel ev = plugin.getEventManager().getEvent(eventId);
                if (ev != null) {
                    List<String> completions = new ArrayList<>();
                    String input = args[3].toLowerCase(Locale.ROOT);
                    for (var item : ev.getSpinRewards()) {
                        String idStr = String.valueOf(item.getId());
                        if (idStr.startsWith(input)) completions.add(idStr);
                    }
                    return completions;
                }
            }
        }

        return Collections.emptyList();
    }
}
