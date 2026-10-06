package org.khmc.eventslib.collections.database;

import org.bukkit.Material;
import org.khmc.eventslib.EventsLibPlugin;
import org.khmc.eventslib.collections.model.CollectionSkin;
import org.khmc.eventslib.collections.model.CollectionType;
import org.khmc.eventslib.collections.model.PlayerCollectionProfile;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class CollectionDatabaseManager {

    private final EventsLibPlugin plugin;
    private final File dbFile;
    private Connection connection;

    public CollectionDatabaseManager(EventsLibPlugin plugin) {
        this.plugin = plugin;
        File dbDir = new File(plugin.getDataFolder(), "database");
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        this.dbFile = new File(dbDir, "collections.db");
        initDatabase();
    }

    private synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            try (Statement s = connection.createStatement()) {
                s.execute("PRAGMA journal_mode = WAL;");
                s.execute("PRAGMA synchronous = NORMAL;");
                s.execute("PRAGMA busy_timeout = 5000;");
            }
        }
        return connection;
    }

    private void initDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS skins (" +
                    "id TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "base_material TEXT NOT NULL, " +
                    "custom_model TEXT NOT NULL, " +
                    "description TEXT, " +
                    "enabled INTEGER DEFAULT 1, " +
                    "created_at INTEGER" +
                    ");");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_collections (" +
                    "uuid TEXT NOT NULL, " +
                    "skin_id TEXT NOT NULL, " +
                    "unlocked_at INTEGER, " +
                    "PRIMARY KEY (uuid, skin_id)" +
                    ");");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_equipped (" +
                    "uuid TEXT NOT NULL, " +
                    "item_type TEXT NOT NULL, " +
                    "skin_id TEXT, " +
                    "PRIMARY KEY (uuid, item_type)" +
                    ");");

            // Seed initial sample skins if empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM skins;");
            if (rs.next() && rs.getInt("total") == 0) {
                seedDefaultSkins(conn);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize collections.db database", e);
        }
    }

    private void seedDefaultSkins(Connection conn) {
        String sql = "INSERT OR IGNORE INTO skins (id, name, type, base_material, custom_model, description, enabled, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            List<CollectionSkin> defaults = List.of(
                    new CollectionSkin("sword_crimson", "Crimson Sword", CollectionType.SWORD, Material.NETHERITE_SWORD, "khmc:sword/crimson", "A powerful crimson blade", true, System.currentTimeMillis()),
                    new CollectionSkin("sword_shadow", "Shadow Sword", CollectionType.SWORD, Material.NETHERITE_SWORD, "khmc:sword/shadow", "Forged in the deepest shadows", true, System.currentTimeMillis()),
                    new CollectionSkin("sword_dragon", "Dragon Sword", CollectionType.SWORD, Material.NETHERITE_SWORD, "khmc:sword/dragon", "Infused with fiery dragon breath", true, System.currentTimeMillis()),
                    new CollectionSkin("pickaxe_shadow", "Shadow Pickaxe", CollectionType.PICKAXE, Material.NETHERITE_PICKAXE, "khmc:pickaxe/shadow", "Mines through ancient depths", true, System.currentTimeMillis()),
                    new CollectionSkin("axe_dragon", "Dragon Axe", CollectionType.AXE, Material.NETHERITE_AXE, "khmc:axe/dragon", "Cleaves through enchanted woods", true, System.currentTimeMillis()),
                    new CollectionSkin("spear_netherite", "Netherite Spear", CollectionType.SPEAR, CollectionType.SPEAR.getBaseMaterial(), "netherite_spear", "Forged from pure netherite alloy and ancient debris", true, System.currentTimeMillis()),
                    new CollectionSkin("spear_inferno", "Inferno Spear", CollectionType.SPEAR, CollectionType.SPEAR.getBaseMaterial(), "inferno_spear", "Incandescent molten flame blade forged in the nether core", true, System.currentTimeMillis()),
                    new CollectionSkin("totem_celestial", "Celestial Totem", CollectionType.TOTEM, Material.TOTEM_OF_UNDYING, "khmc:totem/celestial", "Radiates eternal protective starlight.", true, System.currentTimeMillis()),
                    new CollectionSkin("exp_starlight", "Starlight Bottle", CollectionType.EXPERIENCE_BOTTLE, Material.EXPERIENCE_BOTTLE, "khmc:exp/starlight", "Glowing with condensed cosmic starlight knowledge.", true, System.currentTimeMillis()),
                    new CollectionSkin("apple_cosmic", "Cosmic Apple", CollectionType.GOLDEN_APPLE, Material.GOLDEN_APPLE, "khmc:apple/cosmic", "Infused with ancient cosmic celestial nectar.", true, System.currentTimeMillis()),
                    new CollectionSkin("naruto_helmet", "Naruto Headband", CollectionType.HELMET, Material.NETHERITE_HELMET, "naruto_helmet", "Hidden Leaf Village forehead protector.", true, System.currentTimeMillis()),
                    new CollectionSkin("naruto_chestplate", "Naruto Jacket", CollectionType.CHESTPLATE, Material.NETHERITE_CHESTPLATE, "naruto_chestplate", "Iconic orange and black shinobi jacket.", true, System.currentTimeMillis()),
                    new CollectionSkin("naruto_leggings", "Naruto Pants", CollectionType.LEGGINGS, Material.NETHERITE_LEGGINGS, "naruto_leggings", "Orange ninja trousers with leg holster bandage.", true, System.currentTimeMillis()),
                    new CollectionSkin("naruto_boots", "Naruto Sandals", CollectionType.BOOTS, Material.NETHERITE_BOOTS, "naruto_boots", "Traditional open-toe shinobi footwear.", true, System.currentTimeMillis())
            );

            for (CollectionSkin skin : defaults) {
                ps.setString(1, skin.getId());
                ps.setString(2, skin.getName());
                ps.setString(3, skin.getType().name());
                ps.setString(4, skin.getBaseMaterial().name());
                ps.setString(5, skin.getCustomModel());
                ps.setString(6, skin.getDescription());
                ps.setInt(7, skin.isEnabled() ? 1 : 0);
                ps.setLong(8, skin.getCreatedAt());
                ps.addBatch();
            }
            ps.executeBatch();
            plugin.getLogger().info("Seeded initial example cosmetic skins in collections.db");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error seeding default skins", e);
        }
    }

    public synchronized Map<String, CollectionSkin> loadAllSkins() {
        initDatabase();
        Map<String, CollectionSkin> result = new ConcurrentHashMap<>();
        String sql = "SELECT id, name, type, base_material, custom_model, description, enabled, created_at FROM skins;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String typeStr = rs.getString("type");
                String matStr = rs.getString("base_material");
                String model = rs.getString("custom_model");
                String desc = rs.getString("description");
                boolean enabled = rs.getInt("enabled") == 1;
                long createdAt = rs.getLong("created_at");

                CollectionType type = CollectionType.fromString(typeStr);
                if (type == null) continue;

                Material mat = Material.matchMaterial(matStr);
                if (mat == null) mat = type.getBaseMaterial();

                CollectionSkin skin = new CollectionSkin(id, name, type, mat, model, desc, enabled, createdAt);
                result.put(id.toLowerCase(), skin);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading skins from collections.db", e);
        }
        return result;
    }

    public synchronized boolean insertSkin(CollectionSkin skin) {
        String sql = "INSERT OR REPLACE INTO skins (id, name, type, base_material, custom_model, description, enabled, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, skin.getId().toLowerCase());
            ps.setString(2, skin.getName());
            ps.setString(3, skin.getType().name());
            ps.setString(4, skin.getBaseMaterial().name());
            ps.setString(5, skin.getCustomModel());
            ps.setString(6, skin.getDescription());
            ps.setInt(7, skin.isEnabled() ? 1 : 0);
            ps.setLong(8, skin.getCreatedAt());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error inserting skin " + skin.getId(), e);
            return false;
        }
    }

    public synchronized boolean deleteSkin(String skinId) {
        String sql = "DELETE FROM skins WHERE id = ?;";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, skinId.toLowerCase());
            int affected = ps.executeUpdate();

            // Also clean up player_collections & player_equipped
            try (PreparedStatement pc = conn.prepareStatement("DELETE FROM player_collections WHERE skin_id = ?;");
                 PreparedStatement pe = conn.prepareStatement("DELETE FROM player_equipped WHERE skin_id = ?;")) {
                pc.setString(1, skinId.toLowerCase());
                pe.setString(1, skinId.toLowerCase());
                pc.executeUpdate();
                pe.executeUpdate();
            }
            return affected > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error deleting skin " + skinId, e);
            return false;
        }
    }

    public synchronized boolean updateSkinField(String skinId, String fieldName, Object value) {
        String col;
        switch (fieldName.toLowerCase()) {
            case "name" -> col = "name";
            case "model", "custom_model" -> col = "custom_model";
            case "desc", "description" -> col = "description";
            case "enabled" -> col = "enabled";
            case "material", "base_material" -> col = "base_material";
            default -> {
                return false;
            }
        }

        String sql = "UPDATE skins SET " + col + " = ? WHERE id = ?;";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (value instanceof Boolean b) {
                ps.setInt(1, b ? 1 : 0);
            } else if (value instanceof Number n) {
                ps.setLong(1, n.longValue());
            } else {
                ps.setString(1, String.valueOf(value));
            }
            ps.setString(2, skinId.toLowerCase());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating skin field " + fieldName + " for " + skinId, e);
            return false;
        }
    }

    public synchronized PlayerCollectionProfile loadPlayerProfile(UUID uuid) {
        initDatabase();
        PlayerCollectionProfile profile = new PlayerCollectionProfile(uuid);
        String uuidStr = uuid.toString();

        try (Connection conn = getConnection()) {
            // 1. Unlocked skins
            try (PreparedStatement ps = conn.prepareStatement("SELECT skin_id FROM player_collections WHERE uuid = ?;")) {
                ps.setString(1, uuidStr);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        profile.unlock(rs.getString("skin_id"));
                    }
                }
            }

            // 2. Equipped skins
            try (PreparedStatement ps = conn.prepareStatement("SELECT item_type, skin_id FROM player_equipped WHERE uuid = ?;")) {
                ps.setString(1, uuidStr);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        CollectionType type = CollectionType.fromString(rs.getString("item_type"));
                        if (type != null) {
                            profile.setEquippedSkin(type, rs.getString("skin_id"));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading player profile for " + uuid, e);
        }

        return profile;
    }

    public synchronized boolean addPlayerUnlock(UUID uuid, String skinId) {
        String sql = "INSERT OR IGNORE INTO player_collections (uuid, skin_id, unlocked_at) VALUES (?, ?, ?);";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, skinId.toLowerCase());
            ps.setLong(3, System.currentTimeMillis());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error unlocking skin " + skinId + " for player " + uuid, e);
            return false;
        }
    }

    public synchronized boolean removePlayerUnlock(UUID uuid, String skinId) {
        String sql = "DELETE FROM player_collections WHERE uuid = ? AND skin_id = ?;";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, skinId.toLowerCase());
            int affected = ps.executeUpdate();

            // If equipped, unequip it
            try (PreparedStatement pe = conn.prepareStatement("DELETE FROM player_equipped WHERE uuid = ? AND skin_id = ?;")) {
                pe.setString(1, uuid.toString());
                pe.setString(2, skinId.toLowerCase());
                pe.executeUpdate();
            }

            return affected > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error removing unlocked skin " + skinId + " from player " + uuid, e);
            return false;
        }
    }

    public synchronized boolean setPlayerEquipped(UUID uuid, CollectionType type, String skinId) {
        String uuidStr = uuid.toString();
        try (Connection conn = getConnection()) {
            if (skinId == null || skinId.trim().isEmpty() || skinId.equalsIgnoreCase("default") || skinId.equalsIgnoreCase("none")) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM player_equipped WHERE uuid = ? AND item_type = ?;")) {
                    ps.setString(1, uuidStr);
                    ps.setString(2, type.name());
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO player_equipped (uuid, item_type, skin_id) VALUES (?, ?, ?);")) {
                    ps.setString(1, uuidStr);
                    ps.setString(2, type.name());
                    ps.setString(3, skinId.toLowerCase());
                    ps.executeUpdate();
                }
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving equipped skin " + skinId + " for " + uuid, e);
            return false;
        }
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
    }
}
