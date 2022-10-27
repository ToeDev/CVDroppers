package org.cubeville.cvdroppers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class CVDroppers extends JavaPlugin implements Listener {

    HashMap<String, List<String>> dropperRegions;

    public void onEnable() {
        saveConfig();
        dropperRegions = new HashMap<>();

        for(World world : Bukkit.getWorlds()) {
            if(getConfig().getConfigurationSection("droppers") != null && getConfig().getConfigurationSection("droppers").get(world.getName()) != null) {
                dropperRegions.put(world.getName(), (List<String>) getConfig().getConfigurationSection("droppers").get(world.getName()));
            }
        }

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game!");
            return true;
        }
        if(command.getName().equalsIgnoreCase("cvdroppers")) {
            if(args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Incorrect usage! Try /droppers list|add|remove");
            } else if((args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) && args.length != 2) {
                sender.sendMessage(ChatColor.RED + "Incorrect usage! Try /droppers add|remove <region>");
            } else if(args[0].equalsIgnoreCase("list")) {
                if(args.length != 1) {
                    sender.sendMessage(ChatColor.RED + "Incorrect usage! Try /droppers list");
                } else {
                    listDroppers((Player) sender);
                }
            } else if(args[0].equalsIgnoreCase("add")) {
                addDropper((Player) sender, args[1]);
            } else if(args[0].equalsIgnoreCase("remove")) {
                removeDropper((Player) sender, args[1]);
            } else {
                sender.sendMessage(ChatColor.RED + "Incorrect usage! Try /droppers list|add|remove");
            }
            return true;
        }
        return false;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if(event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if(event.getEntityType() != EntityType.PLAYER) return;
        Player player = (Player) event.getEntity();
        if(!dropperRegions.containsKey(player.getWorld().getName())) return;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
        for(ProtectedRegion region : set) {
            if(dropperRegions.get(player.getWorld().getName()).contains(region.getId().toLowerCase())) {
                player.setHealth(0);
                return;
            }
        }
    }

    public void listDroppers(Player player) {
        if(!dropperRegions.containsKey(player.getWorld().getName())) {
            player.sendMessage(ChatColor.RED + "There are no dropper regions in this world!");
            return;
        }
        player.sendMessage(ChatColor.BLUE + "----------Dropper Regions----------");
        String out = "";
        int i = dropperRegions.get(player.getWorld().getName()).size();
        for(String region : dropperRegions.get(player.getWorld().getName())) {
            out = out.concat(ChatColor.GOLD + region);
            i--;
            if(i != 0) {
                out = out.concat(ChatColor.BLUE + " | ");
            }
        }
        player.sendMessage(out);
    }

    public void addDropper(Player player, String region) {
        if(dropperRegions.containsKey(player.getWorld().getName())) {
            List<String> tempWorldRegions = dropperRegions.get(player.getWorld().getName());
            if(tempWorldRegions.contains(region.toLowerCase())) {
                player.sendMessage(ChatColor.RED + "The region " + ChatColor.GOLD + region + ChatColor.RED + " is already on the dropper list for this world!");
                return;
            }
            tempWorldRegions.add(region.toLowerCase());
            dropperRegions.put(player.getWorld().getName(), tempWorldRegions);
        } else {
            List<String> tempWorldRegions = new ArrayList<>();
            tempWorldRegions.add(region);
            dropperRegions.put(player.getWorld().getName(), tempWorldRegions);
        }
        writeDroppersToConfig();
        player.sendMessage(ChatColor.GREEN + "The region " + ChatColor.GOLD + region + ChatColor.GREEN + " has been added to the dropper list.");
    }

    public void removeDropper(Player player, String region) {
        if(dropperRegions.containsKey(player.getWorld().getName())) {
            List<String> tempWorldRegions = dropperRegions.get(player.getWorld().getName());
            if(!tempWorldRegions.contains(region.toLowerCase())) {
                player.sendMessage(ChatColor.RED + "The region " + ChatColor.GOLD + region + ChatColor.RED + " is not on the dropper list for this world!");
                return;
            }
            tempWorldRegions.remove(region.toLowerCase());
            dropperRegions.put(player.getWorld().getName(), tempWorldRegions);
        } else {
            player.sendMessage(ChatColor.RED + "The region " + ChatColor.GOLD + region + ChatColor.RED + " is not on the dropper list for this world!");
        }
        writeDroppersToConfig();
        player.sendMessage(ChatColor.GREEN + "The region " + ChatColor.GOLD + region + ChatColor.GREEN + " has been removed from the dropper list.");
    }

    public void writeDroppersToConfig() {
        if(getConfig().getConfigurationSection("droppers") == null) {
            getConfig().createSection("droppers");
        }
        ConfigurationSection droppers = getConfig().getConfigurationSection("droppers");
        for(String world : dropperRegions.keySet()) {
            List<String> regions = dropperRegions.get(world);
            droppers.set(world, regions);
        }
        saveConfig();
    }

    public void onDisable() {

    }
}
