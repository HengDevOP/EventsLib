package org.khmc.eventslib;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.khmc.eventslib.collections.command.CollectionAdminCommand;
import org.khmc.eventslib.collections.command.CollectionsCommand;
import org.khmc.eventslib.collections.listener.CollectionItemListener;
import org.khmc.eventslib.collections.manager.CollectionManager;
import org.khmc.eventslib.commands.EventsLibCommand;
import org.khmc.eventslib.database.EventDatabaseManager;
import org.khmc.eventslib.listener.EventListener;
import org.khmc.eventslib.manager.EventManager;
import org.khmc.eventslib.util.SoundUtil;

public class EventsLibPlugin extends JavaPlugin {

    private static EventsLibPlugin instance;
    private EventDatabaseManager databaseManager;
    private EventManager eventManager;
    private CollectionManager collectionManager;

    public static EventsLibPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // 1. Config
        saveDefaultConfig();

        // 2. SoundUtil
        SoundUtil.initialize(this);

        // 3. Database
        this.databaseManager = new EventDatabaseManager(this);

        // 4. Managers
        this.eventManager = new EventManager(this, databaseManager);
        this.collectionManager = new CollectionManager(this);

        // 5. Listeners
        new EventListener(this);
        new CollectionItemListener(this);

        // 6. Commands
        PluginCommand elCmd = getCommand("el");
        if (elCmd != null) {
            EventsLibCommand executor = new EventsLibCommand(this);
            elCmd.setExecutor(executor);
            elCmd.setTabCompleter(executor);
        } else {
            getLogger().warning("Command /el could not be registered!");
        }

        PluginCommand collectionsCmd = getCommand("collections");
        if (collectionsCmd != null) {
            collectionsCmd.setExecutor(new CollectionsCommand(this));
        }

        PluginCommand cadminCmd = getCommand("collectionadmin");
        if (cadminCmd != null) {
            CollectionAdminCommand cadminExecutor = new CollectionAdminCommand(this);
            cadminCmd.setExecutor(cadminExecutor);
            cadminCmd.setTabCompleter(cadminExecutor);
        }

        getLogger().info("EventsLib v" + getDescription().getVersion() + " (with Collections module) enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (collectionManager != null) {
            collectionManager.close();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("EventsLib has been disabled.");
    }

    public EventDatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public CollectionManager getCollectionManager() {
        return collectionManager;
    }
}
