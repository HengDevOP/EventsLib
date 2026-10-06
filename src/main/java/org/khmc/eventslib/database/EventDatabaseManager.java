package org.khmc.eventslib.database;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.khmc.eventslib.model.EventModel;
import org.khmc.eventslib.model.EventPrivacy;
import org.khmc.eventslib.model.EventShopItem;
import org.khmc.eventslib.model.EventSpinReward;
import org.khmc.eventslib.util.ItemSerializer;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class EventDatabaseManager {

    private final JavaPlugin plugin;
    private final Object lock = new Object();
    private Connection connection;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {}
    }

    public EventDatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    private Connection getConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return connection;
        }
        File dbFolder = new File(plugin.getDataFolder(), "database");
        if (!dbFolder.exists()) {
            dbFolder.mkdirs();
        }
        String fileName = plugin.getConfig().getString("database.filename", "events.db");
        File dbFile = new File(dbFolder, fileName);
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        connection = DriverManager.getConnection(url);
        return connection;
    }

    private void initDatabase() {
        synchronized (lock) {
            try {
                Connection conn = getConnection();
                try (Statement stmt = conn.createStatement()) {
                    // 1. Events table
                    String eventsTable = "CREATE TABLE IF NOT EXISTS events (" +
                            "id VARCHAR(64) PRIMARY KEY NOT NULL, " +
                            "title VARCHAR(128) NOT NULL, " +
                            "privacy VARCHAR(16) NOT NULL DEFAULT 'PRIVATE', " +
                            "creator_uuid VARCHAR(36), " +
                            "creator_name VARCHAR(32), " +
                            "created_at BIGINT NOT NULL, " +
                            "updated_at BIGINT NOT NULL, " +
                            "custom_model_data TEXT NOT NULL DEFAULT '', " +
                            "spin_cost DOUBLE NOT NULL DEFAULT 10.0" +
                            ");";
                    stmt.execute(eventsTable);

                    // Migration safe-checks
                    try {
                        stmt.execute("ALTER TABLE events ADD COLUMN custom_model_data TEXT NOT NULL DEFAULT '';");
                    } catch (SQLException ignored) {}
                    try {
                        stmt.execute("ALTER TABLE events ADD COLUMN spin_cost DOUBLE NOT NULL DEFAULT 10.0;");
                    } catch (SQLException ignored) {}

                    // 2. 54-Slot layout items
                    String slotsTable = "CREATE TABLE IF NOT EXISTS event_slots (" +
                            "event_id VARCHAR(64) NOT NULL, " +
                            "slot INTEGER NOT NULL, " +
                            "item_bytes BLOB, " +
                            "PRIMARY KEY (event_id, slot), " +
                            "FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE" +
                            ");";
                    stmt.execute(slotsTable);

                    // 3. Event Collectible Item & Daily Amount
                    String itemTable = "CREATE TABLE IF NOT EXISTS event_items (" +
                            "event_id VARCHAR(64) PRIMARY KEY NOT NULL, " +
                            "item_bytes BLOB NOT NULL, " +
                            "daily_amount INTEGER NOT NULL DEFAULT 1, " +
                            "FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE" +
                            ");";
                    stmt.execute(itemTable);

                    // 4. Event Shop Items
                    String shopTable = "CREATE TABLE IF NOT EXISTS event_shop_items (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "event_id VARCHAR(64) NOT NULL, " +
                            "price INTEGER NOT NULL, " +
                            "item_bytes BLOB NOT NULL, " +
                            "created_at BIGINT NOT NULL, " +
                            "FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE" +
                            ");";
                    stmt.execute(shopTable);

                    // 5. Daily Claims Tracking (Singapore 3:00 PM cycle)
                    String claimsTable = "CREATE TABLE IF NOT EXISTS event_daily_claims (" +
                            "player_uuid VARCHAR(36) NOT NULL, " +
                            "event_id VARCHAR(64) NOT NULL, " +
                            "cycle_key VARCHAR(16) NOT NULL, " +
                            "claimed_at BIGINT NOT NULL, " +
                            "PRIMARY KEY (player_uuid, event_id, cycle_key)" +
                            ");";
                    stmt.execute(claimsTable);

                    // 6. Event Spin Rewards (item + weight)
                    String spinTable = "CREATE TABLE IF NOT EXISTS event_spin_rewards (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "event_id VARCHAR(64) NOT NULL, " +
                            "weight INTEGER NOT NULL DEFAULT 10, " +
                            "item_bytes BLOB NOT NULL, " +
                            "created_at BIGINT NOT NULL, " +
                            "FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE" +
                            ");";
                    stmt.execute(spinTable);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not initialize EventsLib SQLite database", e);
            }
        }
    }

    public Map<String, EventModel> loadAllEvents() {
        Map<String, EventModel> eventsMap = new HashMap<>();
        synchronized (lock) {
            try {
                Connection conn = getConnection();
                String queryEvents = "SELECT id, title, privacy, creator_uuid, creator_name, created_at, updated_at, custom_model_data, spin_cost FROM events;";
                try (PreparedStatement ps = conn.prepareStatement(queryEvents);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String id = rs.getString("id");
                        String title = rs.getString("title");
                        EventPrivacy privacy = EventPrivacy.fromString(rs.getString("privacy"));
                        String uuidStr = rs.getString("creator_uuid");
                        UUID creatorUuid = (uuidStr != null && !uuidStr.isEmpty()) ? UUID.fromString(uuidStr) : null;
                        String creatorName = rs.getString("creator_name");
                        long createdAt = rs.getLong("created_at");
                        long updatedAt = rs.getLong("updated_at");
                        String customModelData = rs.getString("custom_model_data");
                        double spinCost = rs.getDouble("spin_cost");

                        EventModel event = new EventModel(id, title, privacy, creatorUuid, creatorName, createdAt, updatedAt, customModelData, spinCost);
                        eventsMap.put(id.toLowerCase(Locale.ROOT), event);
                    }
                }

                // Load 54 slots for all events
                String querySlots = "SELECT event_id, slot, item_bytes FROM event_slots;";
                try (PreparedStatement ps = conn.prepareStatement(querySlots);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String eventId = rs.getString("event_id").toLowerCase(Locale.ROOT);
                        int slot = rs.getInt("slot");
                        byte[] bytes = rs.getBytes("item_bytes");
                        EventModel event = eventsMap.get(eventId);
                        if (event != null && slot >= 0 && slot < EventModel.TOTAL_SLOTS) {
                            ItemStack item = ItemSerializer.fromBytes(bytes);
                            event.setItem(slot, item);
                        }
                    }
                }

                // Load event collectible items
                String queryItems = "SELECT event_id, item_bytes, daily_amount FROM event_items;";
                try (PreparedStatement ps = conn.prepareStatement(queryItems);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String eventId = rs.getString("event_id").toLowerCase(Locale.ROOT);
                        byte[] bytes = rs.getBytes("item_bytes");
                        int dailyAmount = rs.getInt("daily_amount");
                        EventModel event = eventsMap.get(eventId);
                        if (event != null) {
                            ItemStack item = ItemSerializer.fromBytes(bytes);
                            event.setEventItem(item, dailyAmount);
                        }
                    }
                }

                // Load event shop items
                String queryShop = "SELECT id, event_id, price, item_bytes, created_at FROM event_shop_items ORDER BY id ASC;";
                try (PreparedStatement ps = conn.prepareStatement(queryShop);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String eventId = rs.getString("event_id").toLowerCase(Locale.ROOT);
                        int price = rs.getInt("price");
                        byte[] bytes = rs.getBytes("item_bytes");
                        long createdAt = rs.getLong("created_at");

                        EventModel event = eventsMap.get(eventId);
                        if (event != null) {
                            ItemStack item = ItemSerializer.fromBytes(bytes);
                            if (item != null) {
                                event.addShopItem(new EventShopItem(id, eventId, price, item, createdAt));
                            }
                        }
                    }
                }

                // Load event spin rewards
                String querySpin = "SELECT id, event_id, weight, item_bytes, created_at FROM event_spin_rewards ORDER BY id ASC;";
                try (PreparedStatement ps = conn.prepareStatement(querySpin);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String eventId = rs.getString("event_id").toLowerCase(Locale.ROOT);
                        int weight = rs.getInt("weight");
                        byte[] bytes = rs.getBytes("item_bytes");
                        long createdAt = rs.getLong("created_at");

                        EventModel event = eventsMap.get(eventId);
                        if (event != null) {
                            ItemStack item = ItemSerializer.fromBytes(bytes);
                            if (item != null) {
                                event.addSpinReward(new EventSpinReward(id, eventId, weight, item, createdAt));
                            }
                        }
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading events from SQLite database", e);
            }
        }
        return eventsMap;
    }

    public void saveEvent(EventModel event) {
        if (event == null || event.getId() == null) return;
        synchronized (lock) {
            try {
                Connection conn = getConnection();
                boolean prevAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);
                try {
                    String upsertEvent = "INSERT INTO events (id, title, privacy, creator_uuid, creator_name, created_at, updated_at, custom_model_data, spin_cost) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                            "ON CONFLICT(id) DO UPDATE SET " +
                            "title = excluded.title, " +
                            "privacy = excluded.privacy, " +
                            "creator_uuid = excluded.creator_uuid, " +
                            "creator_name = excluded.creator_name, " +
                            "updated_at = excluded.updated_at, " +
                            "custom_model_data = excluded.custom_model_data, " +
                            "spin_cost = excluded.spin_cost;";
                    try (PreparedStatement ps = conn.prepareStatement(upsertEvent)) {
                        ps.setString(1, event.getId());
                        ps.setString(2, event.getTitle());
                        ps.setString(3, event.getPrivacy().name());
                        ps.setString(4, event.getCreatorUuid() != null ? event.getCreatorUuid().toString() : null);
                        ps.setString(5, event.getCreatorName());
                        ps.setLong(6, event.getCreatedAt());
                        ps.setLong(7, event.getUpdatedAt());
                        ps.setString(8, event.getCustomModelData());
                        ps.setDouble(9, event.getSpinCost());
                        ps.executeUpdate();
                    }

                    // Delete existing slots
                    String deleteSlots = "DELETE FROM event_slots WHERE event_id = ?;";
                    try (PreparedStatement ps = conn.prepareStatement(deleteSlots)) {
                        ps.setString(1, event.getId());
                        ps.executeUpdate();
                    }

                    // Insert occupied slots
                    String insertSlot = "INSERT INTO event_slots (event_id, slot, item_bytes) VALUES (?, ?, ?);";
                    try (PreparedStatement ps = conn.prepareStatement(insertSlot)) {
                        ItemStack[] slots = event.getSlots();
                        for (int i = 0; i < slots.length; i++) {
                            ItemStack is = slots[i];
                            if (is != null && !is.getType().isAir()) {
                                byte[] bytes = ItemSerializer.toBytes(is);
                                if (bytes != null) {
                                    ps.setString(1, event.getId());
                                    ps.setInt(2, i);
                                    ps.setBytes(3, bytes);
                                    ps.addBatch();
                                }
                            }
                        }
                        ps.executeBatch();
                    }

                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(prevAutoCommit);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save event " + event.getId() + " to SQLite", e);
            }
        }
    }

    public void updateEventPrivacy(String eventId, EventPrivacy privacy) {
        if (eventId == null || privacy == null) return;
        synchronized (lock) {
            String sql = "UPDATE events SET privacy = ?, updated_at = ? WHERE id = ?;";
            try {
                Connection conn = getConnection();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, privacy.name());
                    ps.setLong(2, System.currentTimeMillis());
                    ps.setString(3, eventId);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update privacy for event " + eventId, e);
            }
        }
    }

    public void updateEventCustomModelData(String eventId, String customModelData) {
        if (eventId == null) return;
        synchronized (lock) {
            String sql = "UPDATE events SET custom_model_data = ?, updated_at = ? WHERE id = ?;";
            try {
                Connection conn = getConnection();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, customModelData != null ? customModelData.trim() : "");
                    ps.setLong(2, System.currentTimeMillis());
                    ps.setString(3, eventId);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update custom model data for event " + eventId, e);
            }
        }
    }

    public void updateEventSpinCost(String eventId, double spinCost) {
        if (eventId == null) return;
        synchronized (lock) {
            String sql = "UPDATE events SET spin_cost = ?, updated_at = ? WHERE id = ?;";
            try {
                Connection conn = getConnection();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setDouble(1, Math.max(0.0, spinCost));
                    ps.setLong(2, System.currentTimeMillis());
                    ps.setString(3, eventId);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update spin cost for event " + eventId, e);
            }
        }
    }

    public void saveEventItem(String eventId, ItemStack item, int dailyAmount) {
        if (eventId == null) return;
        synchronized (lock) {
            try {
                Connection conn = getConnection();
                if (item == null || item.getType().isAir()) {
                    String deleteSql = "DELETE FROM event_items WHERE event_id = ?;";
                    try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                        ps.setString(1, eventId);
                        ps.executeUpdate();
                    }
                    return;
                }

                byte[] bytes = ItemSerializer.toBytes(item);
                if (bytes == null) return;

                String upsertSql = "INSERT INTO event_items (event_id, item_bytes, daily_amount) VALUES (?, ?, ?) " +
                        "ON CONFLICT(event_id) DO UPDATE SET item_bytes = excluded.item_bytes, daily_amount = excluded.daily_amount;";
                try (PreparedStatement ps = conn.prepareStatement(upsertSql)) {
                    ps.setString(1, eventId);
                    ps.setBytes(2, bytes);
                    ps.setInt(3, Math.max(1, dailyAmount));
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save event item for event " + eventId, e);
            }
        }
    }

    public int addShopItem(String eventId, int price, ItemStack item) {
        if (eventId == null || item == null) return -1;
        synchronized (lock) {
            String sql = "INSERT INTO event_shop_items (event_id, price, item_bytes, created_at) VALUES (?, ?, ?, ?);";
            try {
                Connection conn = getConnection();
                byte[] bytes = ItemSerializer.toBytes(item);
                if (bytes == null) return -1;

                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, eventId);
                    ps.setInt(2, Math.max(1, price));
                    ps.setBytes(3, bytes);
                    ps.setLong(4, System.currentTimeMillis());
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to add shop item for event " + eventId, e);
            }
        }
        return -1;
    }

    public boolean deleteShopItem(int shopItemId) {
        synchronized (lock) {
            String sql = "DELETE FROM event_shop_items WHERE id = ?;";
            try {
                Connection conn = getConnection();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, shopItemId);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete shop item " + shopItemId, e);
                return false;
            }
        }
    }

    public int addSpinReward(String eventId, int weight, ItemStack item) {
        if (eventId == null || item == null) return -1;
        synchronized (lock) {
            String sql = "INSERT INTO event_spin_rewards (event_id, weight, item_bytes, created_at) VALUES (?, ?, ?, ?);";
            try {
                Connection conn = getConnection();
                byte[] bytes = ItemSerializer.toBytes(item);
                if (bytes == null) return -1;

                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, eventId);
                    ps.setInt(2, Math.max(1, weight));
                    ps.setBytes(3, bytes);
                    ps.setLong(4, System.currentTimeMillis());
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to add spin reward for event " + eventId, e);
            }
        }
        return -1;
    }

    public boolean deleteSpinReward(int rewardId) {
        synchronized (lock) {
            String sql = "DELETE FROM event_spin_rewards WHERE id = ?;";
            try {
                Connection conn = getConnection();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, rewardId);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete spin reward " + rewardId, e);
                return false;
            }
        }
    }

    public boolean hasClaimedDaily(UUID playerUuid, String eventId, String cycleKey) {
        if (playerUuid == null || eventId == null || cycleKey == null) return false;
        synchronized (lock) {
            String sql = "SELECT 1 FROM event_daily_claims WHERE player_uuid = ? AND event_id = ? AND cycle_key = ? LIMIT 1;";
            try {
                Connection conn = getConnection();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, playerUuid.toString());
                    ps.setString(2, eventId);
                    ps.setString(3, cycleKey);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to check daily claim for " + playerUuid, e);
                return false;
            }
        }
    }

    public boolean recordDailyClaim(UUID playerUuid, String eventId, String cycleKey) {
        if (playerUuid == null || eventId == null || cycleKey == null) return false;
        synchronized (lock) {
            String sql = "INSERT OR IGNORE INTO event_daily_claims (player_uuid, event_id, cycle_key, claimed_at) VALUES (?, ?, ?, ?);";
            try {
                Connection conn = getConnection();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, playerUuid.toString());
                    ps.setString(2, eventId);
                    ps.setString(3, cycleKey);
                    ps.setLong(4, System.currentTimeMillis());
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to record daily claim for " + playerUuid, e);
                return false;
            }
        }
    }

    public boolean deleteEvent(String eventId) {
        if (eventId == null) return false;
        synchronized (lock) {
            try {
                Connection conn = getConnection();

                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM event_slots WHERE event_id = ?;")) {
                    ps.setString(1, eventId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM event_items WHERE event_id = ?;")) {
                    ps.setString(1, eventId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM event_shop_items WHERE event_id = ?;")) {
                    ps.setString(1, eventId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM event_daily_claims WHERE event_id = ?;")) {
                    ps.setString(1, eventId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM event_spin_rewards WHERE event_id = ?;")) {
                    ps.setString(1, eventId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM events WHERE id = ?;")) {
                    ps.setString(1, eventId);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete event " + eventId, e);
                return false;
            }
        }
    }

    public void close() {
        synchronized (lock) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ignored) {}
        }
    }
}
