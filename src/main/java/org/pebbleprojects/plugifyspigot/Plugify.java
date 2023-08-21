package org.pebbleprojects.plugifyspigot;

import org.bukkit.plugin.java.JavaPlugin;

public final class Plugify extends JavaPlugin {

    @Override
    public void onEnable() {
        getCommand("plugify").setExecutor(new Command());

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
