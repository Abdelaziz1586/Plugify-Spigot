package org.pebbleprojects.plugifyspigot;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Command implements CommandExecutor {

    @Override
    public boolean onCommand(final CommandSender sender, final org.bukkit.command.Command command, final String label, final String[] args) {
        new Thread(() -> {
            if (sender.hasPermission("plugify.search")) {
                if (args.length < 2) {
                    sender.sendMessage("/plugify <search/download> <resource name>");
                    return;
                }

                final StringBuilder query = new StringBuilder();
                
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) query.append(args[i]).append(" ");
                }

                query.deleteCharAt(query.length() - 1);

                if (args[0].equalsIgnoreCase("search")) {

                    final String response = sendAndGet("https://api.spiget.org/v2/search/resources/" + query + "?size=1");

                    if (response == null) {
                        sender.sendMessage("An error has occurred, please check the console.");
                        return;
                    }

                    if (response.equals("")) {
                        sender.sendMessage("Couldn't find resource " + query);
                        return;
                    }

                    sender.sendMessage("Plugin found -> " + response.split("name\": \"")[1].split("\",")[0]);
                    return;
                }

                if (args[0].equalsIgnoreCase("download")) {
                    final String response = sendAndGet("https://api.spiget.org/v2/search/resources/" + query + "?size=1");

                    if (response == null) {
                        sender.sendMessage("An error has occurred, please check the console.");
                        return;
                    }

                    if (response.equals("")) {
                        sender.sendMessage("Couldn't find resource " + query);
                        return;
                    }

                    final boolean b = Boolean.parseBoolean(response.split("external\": ")[1].split(",")[0]);

                    sender.sendMessage((b ? "External" : "SpigotMC") + " download detected, fetching data...");

                    final String id = response.split("\"id\": ")[4].replace(" ", "").replace("}", "").replace("]", "");

                    try {
                        Files.copy(new URL(!b ? "https://api.spiget.org/v2/resources/" + id + "/download/" : response.split("externalUrl\": \"")[1].split("\"")[0]).openStream(), Paths.get("plugins/" + id + ".jar"), StandardCopyOption.REPLACE_EXISTING);
                    } catch (final FileSystemException ignored) {
                        sender.sendMessage("Plugin already downloaded.");
                        return;
                    } catch (final FileNotFoundException ignored) {
                        sender.sendMessage("Failed to download the plugin, perhaps it's premium?");
                        return;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        loadPlugin(id);
                    } catch (final Exception ignored) {
                        sender.sendMessage("Failed to enable plugin, perhaps it failed to download?");
                    }
                }
            }
        }).start();
        return false;
    }

    public void loadPlugin(final String name) {
        Plugin target = null;

        File pluginDir = new File("plugins");

        File pluginFile = new File(pluginDir, name + ".jar");


        try {
            target = Bukkit.getPluginManager().loadPlugin(pluginFile);
        } catch (InvalidDescriptionException | InvalidPluginException e) {
            e.printStackTrace();
        }

        assert target != null;

        target.onLoad();
        Bukkit.getPluginManager().enablePlugin(target);
    }

    private String sendAndGet(final String urlName) {
        try {
            final URL url = new URL(urlName);

            final HttpURLConnection http = (HttpURLConnection) url.openConnection();

            http.setConnectTimeout(1000);
            http.setReadTimeout(1000);

            http.setRequestProperty("UserAgent", "Plugify");
            http.setRequestProperty("Accept", "application/json");

            final int responseCode = http.getResponseCode();

            if (responseCode == 404) return "";

            final InputStream inputStream = 200 <= responseCode && responseCode <= 299 ? http.getInputStream() : http.getErrorStream();

            final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            final StringBuilder response = new StringBuilder();

            String currentLine;

            while ((currentLine = in.readLine()) != null) response.append(currentLine);

            in.close();

            return response.toString();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
