package org.khmc.eventslib;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.khmc.eventslib.commands.EventsLibCommand;
import org.khmc.eventslib.database.EventDatabaseManager;
import org.khmc.eventslib.listener.EventListener;
import org.khmc.eventslib.manager.EventManager;
import org.khmc.eventslib.util.SoundUtil;

public class EventsLibPlugin extends JavaPlugin {

    private static EventsLibPlugin instance;
    private EventDatabaseManager databaseManager;
    private EventManager eventManager;

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

        // 4. Manager
        this.eventManager = new EventManager(this, databaseManager);

        // 5. Listener
        new EventListener(this);

        // 6. Command
        PluginCommand cmd = getCommand("el");
        if (cmd != null) {
            EventsLibCommand executor = new EventsLibCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        } else {
            getLogger().warning("Command /el could not be registered!");
        }

        getLogger().info("EventsLib v" + getDescription().getVersion() + " has been enabled successfully!");
    }

    @Override
    public void onDisable() {
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
}
