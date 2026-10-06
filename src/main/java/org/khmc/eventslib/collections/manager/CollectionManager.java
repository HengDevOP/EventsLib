package org.khmc.eventslib.collections.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.khmc.eventslib.EventsLibPlugin;
import org.khmc.eventslib.collections.database.CollectionDatabaseManager;
import org.khmc.eventslib.collections.model.CollectionSkin;
import org.khmc.eventslib.collections.model.CollectionType;
import org.khmc.eventslib.collections.model.PlayerCollectionProfile;
import org.khmc.eventslib.util.ModelUtil;
import org.khmc.eventslib.util.SchedulerUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class CollectionManager {

    private final EventsLibPlugin plugin;
    private final CollectionDatabaseManager databaseManager;

    private final Map<String, CollectionSkin> skins = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerCollectionProfile> profiles = new ConcurrentHashMap<>();

    private final NamespacedKey skinIdKey;
    private final NamespacedKey managedKey;

    public CollectionManager(EventsLibPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = new CollectionDatabaseManager(plugin);
        this.skinIdKey = new NamespacedKey(plugin, "collections_skin_id");
        this.managedKey = new NamespacedKey(plugin, "collections_managed");
        reload();
    }

    public void reload() {
        skins.clear();
        Map<String, CollectionSkin> loaded = databaseManager.loadAllSkins();
        skins.putAll(loaded);
        plugin.getLogger().info("Loaded " + skins.size() + " cosmetic collection skins from collections.db");

        // Reload online players profiles
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadProfileAsync(player.getUniqueId(), profile -> {
                SchedulerUtil.runTask(plugin, () -> syncPlayerItems(player));
            });
        }
    }

    public CollectionDatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public CollectionSkin getSkin(String id) {
        if (id == null) return null;
        return skins.get(id.toLowerCase());
    }

    public Collection<CollectionSkin> getAllSkins() {
        return Collections.unmodifiableCollection(skins.values());
    }

    public List<CollectionSkin> getSkinsForType(CollectionType type) {
        if (type == null) return Collections.emptyList();
        List<CollectionSkin> list = new ArrayList<>();
        for (CollectionSkin skin : skins.values()) {
            if (skin.getType() == type && skin.isEnabled()) {
                list.add(skin);
            }
        }
        list.sort(Comparator.comparing(CollectionSkin::getName));
        return list;
    }

    public int getTotalSkinsCount() {
        int count = 0;
        for (CollectionSkin s : skins.values()) {
            if (s.isEnabled()) count++;
        }
        return count;
    }

    public int getTotalSkinsCount(CollectionType type) {
        if (type == null) return 0;
        int count = 0;
        for (CollectionSkin s : skins.values()) {
            if (s.getType() == type && s.isEnabled()) count++;
        }
        return count;
    }

    public PlayerCollectionProfile getProfile(Player player) {
        if (player == null) return null;
        return getProfile(player.getUniqueId());
    }

    public PlayerCollectionProfile getProfile(UUID uuid) {
        if (uuid == null) return null;
        return profiles.computeIfAbsent(uuid, id -> databaseManager.loadPlayerProfile(id));
    }

    public void loadProfileAsync(UUID uuid, Consumer<PlayerCollectionProfile> callback) {
        if (uuid == null) return;
        SchedulerUtil.runTaskAsync(plugin, () -> {
            PlayerCollectionProfile profile = databaseManager.loadPlayerProfile(uuid);
            profiles.put(uuid, profile);
            if (callback != null) {
                SchedulerUtil.runTask(plugin, () -> callback.accept(profile));
            }
        });
    }

    public void unloadProfile(UUID uuid) {
        if (uuid != null) {
            profiles.remove(uuid);
        }
    }

    public boolean createSkin(String id, String name, CollectionType type, Material material, String customModel, String description) {
        if (id == null || skins.containsKey(id.toLowerCase())) return false;
        CollectionSkin skin = new CollectionSkin(id.toLowerCase(), name, type, material, customModel, description, true, System.currentTimeMillis());
        skins.put(skin.getId(), skin);
        SchedulerUtil.runTaskAsync(plugin, () -> databaseManager.insertSkin(skin));
        return true;
    }

    public boolean deleteSkin(String id) {
        if (id == null) return false;
        CollectionSkin removed = skins.remove(id.toLowerCase());
        if (removed == null) return false;

        SchedulerUtil.runTaskAsync(plugin, () -> databaseManager.deleteSkin(id));

        // Unequip from online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerCollectionProfile profile = profiles.get(player.getUniqueId());
            if (profile != null) {
                String equipped = profile.getEquippedSkin(removed.getType());
                if (id.equalsIgnoreCase(equipped)) {
                    profile.setEquippedSkin(removed.getType(), null);
                    syncPlayerItems(player);
                }
            }
        }
        return true;
    }

    public boolean editSkin(String id, String property, Object value) {
        CollectionSkin skin = getSkin(id);
        if (skin == null) return false;

        switch (property.toLowerCase()) {
            case "name" -> skin.setName(String.valueOf(value));
            case "model", "custom_model" -> skin.setCustomModel(String.valueOf(value));
            case "desc", "description" -> skin.setDescription(String.valueOf(value));
            case "enabled" -> {
                if (value instanceof Boolean b) {
                    skin.setEnabled(b);
                } else {
                    skin.setEnabled(Boolean.parseBoolean(String.valueOf(value)));
                }
            }
            case "material", "base_material" -> {
                Material mat = Material.matchMaterial(String.valueOf(value).toUpperCase(Locale.ROOT));
                if (mat != null) {
                    skin.setBaseMaterial(mat);
                } else {
                    return false;
                }
            }
            default -> {
                return false;
            }
        }

        SchedulerUtil.runTaskAsync(plugin, () -> databaseManager.updateSkinField(id, property, value));

        // Resync for online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerCollectionProfile profile = profiles.get(player.getUniqueId());
            if (profile != null && id.equalsIgnoreCase(profile.getEquippedSkin(skin.getType()))) {
                syncPlayerItems(player);
            }
        }
        return true;
    }

    public boolean unlockSkin(UUID uuid, String skinId) {
        CollectionSkin skin = getSkin(skinId);
        if (skin == null) return false;

        PlayerCollectionProfile profile = profiles.get(uuid);
        if (profile != null) {
            profile.unlock(skin.getId());
        }

        SchedulerUtil.runTaskAsync(plugin, () -> databaseManager.addPlayerUnlock(uuid, skin.getId()));
        return true;
    }

    public boolean removeSkin(UUID uuid, String skinId) {
        CollectionSkin skin = getSkin(skinId);
        if (skin == null) return false;

        PlayerCollectionProfile profile = profiles.get(uuid);
        if (profile != null) {
            profile.lock(skin.getId());
            if (skin.getId().equalsIgnoreCase(profile.getEquippedSkin(skin.getType()))) {
                profile.setEquippedSkin(skin.getType(), null);
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    syncPlayerItems(p);
                }
            }
        }

        SchedulerUtil.runTaskAsync(plugin, () -> databaseManager.removePlayerUnlock(uuid, skin.getId()));
        return true;
    }

    public boolean equipSkin(Player player, CollectionType type, String skinId) {
        if (player == null || type == null) return false;
        PlayerCollectionProfile profile = getProfile(player);
        if (profile == null) return false;

        // Unequip / default
        if (skinId == null || skinId.trim().isEmpty() || skinId.equalsIgnoreCase("default") || skinId.equalsIgnoreCase("none")) {
            profile.setEquippedSkin(type, null);
            SchedulerUtil.runTaskAsync(plugin, () -> databaseManager.setPlayerEquipped(player.getUniqueId(), type, null));
            syncPlayerItems(player);
            return true;
        }

        CollectionSkin skin = getSkin(skinId);
        if (skin == null || !skin.isEnabled()) return false;

        if (!profile.hasUnlocked(skin.getId()) && !player.hasPermission("collections.admin")) {
            return false;
        }

        profile.setEquippedSkin(type, skin.getId());
        SchedulerUtil.runTaskAsync(plugin, () -> databaseManager.setPlayerEquipped(player.getUniqueId(), type, skin.getId()));
        syncPlayerItems(player);
        return true;
    }

    /**
     * Scans and synchronizes all items in player's inventory, armor, and hands.
     * Preserves all enchantments, durability, custom name, lore, attributes, and PDC tags!
     */
    public void syncPlayerItems(Player player) {
        if (player == null || !player.isOnline()) return;
        PlayerCollectionProfile profile = getProfile(player);
        if (profile == null) return;

        // 1. Storage contents (0 to 35)
        ItemStack[] storage = player.getInventory().getStorageContents();
        boolean changedStorage = false;
        for (int i = 0; i < storage.length; i++) {
            ItemStack item = storage[i];
            if (item != null && !item.getType().isAir()) {
                if (syncItem(item, profile)) {
                    changedStorage = true;
                }
            }
        }
        if (changedStorage) {
            player.getInventory().setStorageContents(storage);
        }

        // 2. Armor contents
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean changedArmor = false;
        for (int i = 0; i < armor.length; i++) {
            ItemStack item = armor[i];
            if (item != null && !item.getType().isAir()) {
                if (syncItem(item, profile)) {
                    changedArmor = true;
                }
            }
        }
        if (changedArmor) {
            player.getInventory().setArmorContents(armor);
        }

        // 3. Extra contents (Off-hand)
        ItemStack[] extra = player.getInventory().getExtraContents();
        boolean changedExtra = false;
        for (int i = 0; i < extra.length; i++) {
            ItemStack item = extra[i];
            if (item != null && !item.getType().isAir()) {
                if (syncItem(item, profile)) {
                    changedExtra = true;
                }
            }
        }
        if (changedExtra) {
            player.getInventory().setExtraContents(extra);
        }

        if (changedStorage || changedArmor || changedExtra) {
            player.updateInventory();
        }
    }

    /**
     * Synchronizes a single ItemStack against the player's active equipped cosmetic skin.
     * Returns true if the ItemStack's ItemMeta was modified.
     */
    public boolean syncItem(ItemStack item, PlayerCollectionProfile profile) {
        if (item == null || item.getType().isAir()) return false;

        CollectionType matchingType = matchType(item.getType());
        if (matchingType == null) return false;

        String equippedSkinId = profile != null ? profile.getEquippedSkin(matchingType) : null;
        CollectionSkin targetSkin = (equippedSkinId != null) ? getSkin(equippedSkinId) : null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String currentSkinId = pdc.get(skinIdKey, PersistentDataType.STRING);

        if (targetSkin != null && targetSkin.isEnabled()) {
            String targetModel = targetSkin.getCustomModel();
            // Verify if truly up to date (both ID and custom model strings present, with no conflicting item_model)
            if (targetSkin.getId().equalsIgnoreCase(currentSkinId) && ModelUtil.hasCustomModelString(meta, targetModel)) {
                return false;
            }
            ModelUtil.applyModel(meta, targetModel, plugin);
            pdc.set(skinIdKey, PersistentDataType.STRING, targetSkin.getId());
            pdc.set(managedKey, PersistentDataType.STRING, "true");
            item.setItemMeta(meta);
            return true;
        } else {
            // Should be default
            if (currentSkinId != null || pdc.has(managedKey, PersistentDataType.STRING) || ModelUtil.hasAnyCustomModel(meta)) {
                ModelUtil.resetModel(meta, plugin);
                pdc.remove(skinIdKey);
                pdc.remove(managedKey);
                item.setItemMeta(meta);
                return true;
            }
            return false;
        }
    }

    public CollectionType matchType(Material material) {
        if (material == null) return null;
        for (CollectionType type : CollectionType.values()) {
            if (type.getBaseMaterial() == material) {
                return type;
            }
        }
        // Also check if any custom skin has this as base material
        for (CollectionSkin skin : skins.values()) {
            if (skin.getBaseMaterial() == material) {
                return skin.getType();
            }
        }
        return null;
    }

    public void close() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        profiles.clear();
        skins.clear();
    }
}
