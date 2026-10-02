package org.khmc.eventslib.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.khmc.eventslib.EventsLibPlugin;
import org.khmc.eventslib.gui.framework.MarketGUI;
import org.khmc.eventslib.hook.ShardsHook;
import org.khmc.eventslib.model.EventModel;
import org.khmc.eventslib.model.EventSpinReward;
import org.khmc.eventslib.util.ColorUtil;
import org.khmc.eventslib.util.SoundUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class EventSpinGUI {

    private final EventsLibPlugin plugin;
    private final EventModel event;
    private boolean isSpinning = false;
    private MarketGUI currentGui;

    public EventSpinGUI(EventsLibPlugin plugin, EventModel event) {
        this.plugin = plugin;
        this.event = event;
    }

    public void open(Player player) {
        if (player == null || event == null) return;

        if (!plugin.getEventManager().canAccess(player, event)) {
            String msg = plugin.getConfig().getString("messages.event-private", "&cThis event is currently private.");
            player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") + msg));
            SoundUtil.playError(player);
            return;
        }

        boolean isAdmin = player.isOp() || player.hasPermission("eventslib.admin");
        String title = "&f" + ColorUtil.toSmallCaps("server events - " + event.getId());
        MarketGUI gui = new MarketGUI(title, 3);
        this.currentGui = gui;

        ItemStack borderGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bMeta = borderGlass.getItemMeta();
        if (bMeta != null) {
            bMeta.displayName(Component.empty());
            borderGlass.setItemMeta(bMeta);
        }

        // Fill border
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18) {
                gui.setButton(i, borderGlass, e -> e.setCancelled(true));
            }
        }

        // Slot 4: Winning Slot Indicator
        ItemStack indicator = new ItemStack(Material.HOPPER);
        ItemMeta indMeta = indicator.getItemMeta();
        if (indMeta != null) {
            indMeta.displayName(ColorUtil.parse("&a▼ &e&l" + ColorUtil.toSmallCaps("winning slot") + " &a▼").decoration(TextDecoration.ITALIC, false));
            List<Component> iLore = new ArrayList<>();
            iLore.add(ColorUtil.parse("&7The item that lands here is your prize!").decoration(TextDecoration.ITALIC, false));
            indMeta.lore(iLore);
            indicator.setItemMeta(indMeta);
        }
        gui.setButton(4, indicator, e -> e.setCancelled(true));

        // Initial items in reel (slots 9 to 17)
        List<EventSpinReward> rewards = event.getSpinRewards();
        for (int slot = 9; slot <= 17; slot++) {
            if (!rewards.isEmpty()) {
                EventSpinReward sample = rewards.get((slot - 9) % rewards.size());
                ItemStack stack = sample.getItem().clone();
                ItemMeta sm = stack.getItemMeta();
                if (sm != null) {
                    sm.addItemFlags(ItemFlag.values());
                    stack.setItemMeta(sm);
                }
                gui.setButton(slot, stack, e -> e.setCancelled(true));
            } else {
                ItemStack emptySlot = new ItemStack(Material.STRUCTURE_VOID);
                ItemMeta em = emptySlot.getItemMeta();
                if (em != null) {
                    em.displayName(ColorUtil.parse("&7" + ColorUtil.toSmallCaps("no rewards")).decoration(TextDecoration.ITALIC, false));
                    emptySlot.setItemMeta(em);
                }
                gui.setButton(slot, emptySlot, e -> e.setCancelled(true));
            }
        }

        // Slot 18: Back Button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta bckMeta = backItem.getItemMeta();
        if (bckMeta != null) {
            bckMeta.displayName(ColorUtil.parse("&c« " + ColorUtil.toSmallCaps("back to event")).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(ColorUtil.parse("&7Return to event hub.").decoration(TextDecoration.ITALIC, false));
            bckMeta.lore(lore);
            backItem.setItemMeta(bckMeta);
        }
        gui.setButton(18, backItem, e -> {
            e.setCancelled(true);
            if (isSpinning) return;
            new EventHubGUI(plugin, event).open(player);
        });

        // Slot 20: View Rewards & Odds
        ItemStack oddsItem = new ItemStack(Material.BOOK);
        ItemMeta oMeta = oddsItem.getItemMeta();
        if (oMeta != null) {
            oMeta.displayName(ColorUtil.parse("&b✦ " + ColorUtil.toSmallCaps("rewards & odds")).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(ColorUtil.parse("&7Possible rewards and their drop chances:").decoration(TextDecoration.ITALIC, false));
            lore.add(ColorUtil.parse("&8-----------------------------").decoration(TextDecoration.ITALIC, false));

            int totalWeight = event.getTotalSpinWeight();
            if (rewards.isEmpty()) {
                lore.add(ColorUtil.parse("&cNo rewards configured yet.").decoration(TextDecoration.ITALIC, false));
            } else {
                for (EventSpinReward r : rewards) {
                    double chance = r.getChancePercent(totalWeight);
                    String name = getItemDisplayName(r.getItem());
                    lore.add(ColorUtil.parse("&7- &f" + name + " &8(&e" + String.format(Locale.US, "%.1f%%", chance) + "&8)").decoration(TextDecoration.ITALIC, false));
                }
            }
            oMeta.lore(lore);
            oddsItem.setItemMeta(oMeta);
        }
        gui.setButton(20, oddsItem, e -> e.setCancelled(true));

        // Slot 22: SPIN BUTTON
        double balance = ShardsHook.getBalance(player);
        boolean canAfford = balance >= event.getSpinCost();
        ItemStack spinButton = new ItemStack(Material.NETHER_STAR);
        ItemMeta sbMeta = spinButton.getItemMeta();
        if (sbMeta != null) {
            sbMeta.displayName(ColorUtil.parse("&e✦ &6&l" + ColorUtil.toSmallCaps("spin wheel") + " &e✦").decoration(TextDecoration.ITALIC, false));
            List<Component> sLore = new ArrayList<>();
            sLore.add(ColorUtil.parse("&7Spin the wheel for exclusive rewards!").decoration(TextDecoration.ITALIC, false));
            sLore.add(ColorUtil.parse("&8-----------------------------").decoration(TextDecoration.ITALIC, false));
            sLore.add(ColorUtil.parse("&7Spin Cost: &d" + ShardsHook.format(event.getSpinCost())).decoration(TextDecoration.ITALIC, false));
            sLore.add(ColorUtil.parse("&7Your Shards: &d" + ShardsHook.format(balance)).decoration(TextDecoration.ITALIC, false));

            if (rewards.isEmpty()) {
                sLore.add(ColorUtil.parse("&c✕ No rewards configured!").decoration(TextDecoration.ITALIC, false));
            } else if (canAfford) {
                sLore.add(ColorUtil.parse("&a✦ Click to Spin!").decoration(TextDecoration.ITALIC, false));
            } else {
                sLore.add(ColorUtil.parse("&c✕ Not enough shards!").decoration(TextDecoration.ITALIC, false));
            }

            sbMeta.lore(sLore);
            spinButton.setItemMeta(sbMeta);
        }

        gui.setButton(22, spinButton, e -> {
            e.setCancelled(true);
            if (isSpinning) return;
            handleSpin(player, gui);
        });

        // Slot 24: Shards Balance
        ItemStack shardsDisplay = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta shMeta = shardsDisplay.getItemMeta();
        if (shMeta != null) {
            shMeta.displayName(ColorUtil.parse("&d✦ " + ColorUtil.toSmallCaps("your shards")).decoration(TextDecoration.ITALIC, false));
            List<Component> sLore = new ArrayList<>();
            sLore.add(ColorUtil.parse("&7Current Balance: &d" + ShardsHook.format(balance)).decoration(TextDecoration.ITALIC, false));
            sLore.add(ColorUtil.parse("&7Used for spinning event wheels.").decoration(TextDecoration.ITALIC, false));
            shMeta.lore(sLore);
            shardsDisplay.setItemMeta(shMeta);
        }
        gui.setButton(24, shardsDisplay, e -> e.setCancelled(true));

        // Slot 26: Admin Controls
        if (isAdmin) {
            ItemStack adminDisplay = new ItemStack(Material.COMPARATOR);
            ItemMeta aMeta = adminDisplay.getItemMeta();
            if (aMeta != null) {
                aMeta.displayName(ColorUtil.parse("&d✦ " + ColorUtil.toSmallCaps("spin admin commands")).decoration(TextDecoration.ITALIC, false));
                List<Component> aLore = new ArrayList<>();
                aLore.add(ColorUtil.parse("&7Add reward: &e/el spin add " + event.getId() + " [weight]").decoration(TextDecoration.ITALIC, false));
                aLore.add(ColorUtil.parse("&7Remove reward: &e/el spin remove " + event.getId() + " <id>").decoration(TextDecoration.ITALIC, false));
                aLore.add(ColorUtil.parse("&7Set cost: &e/el spincost " + event.getId() + " <shards>").decoration(TextDecoration.ITALIC, false));
                aLore.add(ColorUtil.parse("&7List rewards: &e/el spin list " + event.getId()).decoration(TextDecoration.ITALIC, false));
                aMeta.lore(aLore);
                adminDisplay.setItemMeta(aMeta);
            }
            gui.setButton(26, adminDisplay, e -> e.setCancelled(true));
        }

        gui.open(player);
    }

    private void handleSpin(Player player, MarketGUI gui) {
        if (event.getSpinRewards().isEmpty()) {
            SoundUtil.playError(player);
            player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                    "&cThis event has no spin rewards configured!"));
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            SoundUtil.playError(player);
            player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                    "&cYour inventory is full! Please make space before spinning."));
            return;
        }

        double balance = ShardsHook.getBalance(player);
        if (balance < event.getSpinCost()) {
            SoundUtil.playError(player);
            player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                    "&cYou do not have enough shards! Cost: &d" + ShardsHook.format(event.getSpinCost()) +
                    "&c, Balance: &d" + ShardsHook.format(balance)));
            return;
        }

        boolean paid = ShardsHook.withdraw(player, event.getSpinCost());
        if (!paid) {
            SoundUtil.playError(player);
            player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                    "&cTransaction failed. Please try again."));
            return;
        }

        // Pick winning reward based on weights
        EventSpinReward winner = event.getRandomSpinReward();
        if (winner == null) {
            SoundUtil.playError(player);
            return;
        }

        this.isSpinning = true;

        // Update Spin Button to indicate rolling
        ItemStack spinningBtn = new ItemStack(Material.CLOCK);
        ItemMeta sbm = spinningBtn.getItemMeta();
        if (sbm != null) {
            sbm.displayName(ColorUtil.parse("&e&l" + ColorUtil.toSmallCaps("spinning...")).decoration(TextDecoration.ITALIC, false));
            spinningBtn.setItemMeta(sbm);
        }
        gui.setButton(22, spinningBtn, e -> e.setCancelled(true));

        // Generate conveyor strip
        int totalSteps = 26;
        List<ItemStack> reelItems = new ArrayList<>();
        List<EventSpinReward> allRewards = event.getSpinRewards();

        for (int i = 0; i < totalSteps + 9; i++) {
            reelItems.add(allRewards.get(ThreadLocalRandom.current().nextInt(allRewards.size())).getItem().clone());
        }
        // Slot 13 is center: index = step + 4. At final step (totalSteps - 1), center is (totalSteps - 1 + 4)
        reelItems.set(totalSteps - 1 + 4, winner.getItem().clone());

        // Run animated horizontal rolling
        new BukkitRunnable() {
            int step = 0;
            long lastTick = 0;
            int delay = 1;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    isSpinning = false;
                    player.getInventory().addItem(winner.getItem().clone());
                    return;
                }

                // Render current 9 items in reel
                for (int col = 0; col < 9; col++) {
                    int itemIdx = step + col;
                    ItemStack is = reelItems.get(itemIdx).clone();
                    ItemMeta meta = is.getItemMeta();
                    if (meta != null) {
                        meta.addItemFlags(ItemFlag.values());
                        is.setItemMeta(meta);
                    }
                    gui.setButton(9 + col, is, e -> e.setCancelled(true));
                }

                // Sound effect
                float pitch = 0.8f + ((float) step / totalSteps) * 0.6f;
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, pitch);

                step++;
                if (step >= totalSteps) {
                    cancel();
                    finishSpin(player, winner, gui);
                }
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    private void finishSpin(Player player, EventSpinReward winner, MarketGUI gui) {
        this.isSpinning = false;

        // Deliver prize
        player.getInventory().addItem(winner.getItem().clone());

        // Victory sound and particles
        SoundUtil.playSuccess(player);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.1);

        String winnerName = getItemDisplayName(winner.getItem());
        player.sendTitle("§a§lWINNER!", "§f" + winnerName, 10, 40, 15);
        player.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.prefix", "&8[&dEventsLib&8] ") +
                "&a✦ Congratulations! You won &f" + winnerName + " &afrom the &e" + event.getId() + " &aspin!"));

        // Refresh GUI
        open(player);
    }

    private String getItemDisplayName(ItemStack item) {
        if (item == null) return "Item";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return LegacyComponentSerializer.legacyAmpersand().serialize(item.getItemMeta().displayName());
        }
        String name = item.getType().name().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
