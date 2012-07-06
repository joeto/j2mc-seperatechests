package to.joe.j2mc.seperatechests;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import to.joe.j2mc.core.J2MC_Manager;
import to.joe.j2mc.seperatechests.commands.SeperateChestsCommand;

public class J2MC_seperatechests extends JavaPlugin implements Listener {

    public static J2MC_seperatechests instance;
    public ArrayList<String> pendingSmackers = new ArrayList<String>();
    public ArrayList<String> pendingDeletion = new ArrayList<String>();
    public ArrayList<String> pendingInventoryChanges = new ArrayList<String>();
    public Map<String, Integer> chestUsers = new ConcurrentHashMap<String, Integer>();
    public ArrayList<Location> chestLocations = new ArrayList<Location>();

    public void onEnable() {
        instance = this;

        this.getCommand("seperatechests").setExecutor(new SeperateChestsCommand(this));

        this.getServer().getPluginManager().registerEvents(this, this);

        try {
            PreparedStatement ps = J2MC_Manager.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT * FROM seperateChests_chests");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                chestLocations.add(new Location(this.getServer().getWorld(rs.getString("world")), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.getLogger().severe("Loading chests from sql failed. Nope. shutting down.");
            this.getServer().getPluginManager().disablePlugin(this);
        }

        this.getLogger().info("Seperate Chests enabled");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.CHEST && pendingSmackers.contains(event.getPlayer().getName())) {
            event.getPlayer().sendMessage(ChatColor.RED + "Verifying that this chest can be altered!");
            this.pendingSmackers.remove(event.getPlayer().getName());
            this.pendingInventoryChanges.add(event.getPlayer().getName());
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.CHEST && pendingDeletion.contains(event.getPlayer().getName())) {
            try {
                Location loc = event.getClickedBlock().getLocation();
                PreparedStatement isItAChest = J2MC_Manager.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT `ChestID` FROM seperateChests_chests WHERE world=? AND x=? AND y=? AND z=?");
                isItAChest.setString(1, loc.getWorld().getName());
                isItAChest.setDouble(2, loc.getX());
                isItAChest.setDouble(3, loc.getY());
                isItAChest.setDouble(4, loc.getZ());
                ResultSet rs = isItAChest.executeQuery();
                if (rs.first()) {
                    PreparedStatement ps = J2MC_Manager.getMySQL().getFreshPreparedStatementHotFromTheOven("DELETE FROM seperateChests_chests WHERE world=? AND x=? AND y=? AND z=?");
                    ps.setString(1, loc.getWorld().getName());
                    ps.setDouble(2, loc.getX());
                    ps.setDouble(3, loc.getY());
                    ps.setDouble(4, loc.getZ());
                    ps.executeUpdate();
                    event.getPlayer().sendMessage(ChatColor.RED + "Chest deleted from SeperateChests database");
                    this.getLogger().info(event.getPlayer().getName() + " deleted chest #" + rs.getString("ChestID") + " from seperate chests database");
                    this.chestLocations.remove(loc);
                    this.pendingDeletion.remove(event.getPlayer().getName());
                } else {
                    event.getPlayer().sendMessage(ChatColor.RED + "That chest isn't present in the SeperateChests database");
                }
            } catch (Exception e) {
                e.printStackTrace();
                event.setCancelled(true);
            }
        }
        if ((event.getAction() == Action.RIGHT_CLICK_BLOCK) && (event.getClickedBlock().getType() == Material.CHEST) && this.chestLocations.contains(event.getClickedBlock().getLocation())) {
            try {
                Location loc = event.getClickedBlock().getLocation();
                PreparedStatement ps = J2MC_Manager.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT `ChestID`, `inventory` FROM seperateChests_chests WHERE `world`=? AND `x`=? AND `y`=? AND `z`=?");
                ps.setString(1, loc.getWorld().getName());
                ps.setDouble(2, loc.getX());
                ps.setDouble(3, loc.getY());
                ps.setDouble(4, loc.getZ());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    /*
                     * if(this.chestUsers.containsValue(rs.getInt("ChestID")) ){
                     * ((Player) event.getPlayer()).sendMessage(ChatColor.RED +
                     * "This chest is currently in use! Please wait.");
                     * event.setCancelled(true); return; }
                     */
                    PreparedStatement ps2 = J2MC_Manager.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT `inventory` FROM seperateChests_users WHERE `chestID`=? AND `player`=?");
                    ps2.setInt(1, rs.getInt("ChestID"));
                    ps2.setString(2, event.getPlayer().getName());
                    ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) {
                        ItemStack[] items = DataStrings.parseInventory(rs2.getString("inventory"), 27);
                        Inventory playerInventory = this.getServer().createInventory(event.getPlayer(), 27);
                        playerInventory.setContents(items);
                        event.getPlayer().openInventory(playerInventory);
                        this.chestUsers.put(event.getPlayer().getName(), rs.getInt("ChestID"));
                        this.getLogger().info(event.getPlayer().getName() + " opened previously opened chest #" + rs.getInt("ChestID"));
                    } else {
                        PreparedStatement ps3 = J2MC_Manager.getMySQL().getFreshPreparedStatementHotFromTheOven("INSERT INTO seperateChests_users (`chestID`, `inventory`, `player`) VALUES (?,?,?)");
                        ps3.setInt(1, rs.getInt("ChestID"));
                        ps3.setString(2, rs.getString("inventory"));
                        ps3.setString(3, event.getPlayer().getName());
                        ps3.executeUpdate();
                        ItemStack[] items = DataStrings.parseInventory(rs.getString("inventory"), 27);
                        Inventory playerInventory = this.getServer().createInventory(event.getPlayer(), 27);
                        playerInventory.setContents(items);
                        event.getPlayer().openInventory(playerInventory);
                        this.chestUsers.put(event.getPlayer().getName(), rs.getInt("ChestID"));
                        this.getLogger().info(event.getPlayer().getName() + " opened chest #" + rs.getInt("ChestID") + " for the first time, inserted row for his unique inventory");
                    }
                    event.setCancelled(true);
                } else {
                    ((Player) event.getPlayer()).sendMessage(ChatColor.RED + "Sorry, chest currently out of order D:");
                    event.setCancelled(true);
                }
            } catch (Exception e) {
                event.setCancelled(true);
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (this.pendingInventoryChanges.contains(event.getPlayer().getName())) {
            if (event.getInventory().getType() == InventoryType.CHEST && !(event.getInventory() instanceof DoubleChestInventory)) {
                ((Player) event.getPlayer()).sendMessage(ChatColor.RED + "Upon exiting this inventory, your changes will be saved!");
                return;
            } else {
                ((Player) event.getPlayer()).sendMessage(ChatColor.RED + "Invalid block type, this plugin only accepts single chests! (you'll have to re-enter /seperatechests");
                event.setCancelled(true);
                this.pendingInventoryChanges.remove(event.getPlayer().getName());
                return;
            }
        }
        /*
         * if (event.getInventory().getHolder() instanceof Chest &&
         * !(event.getInventory() instanceof DoubleChestInventory)) { Location
         * loc = ((Chest)
         * event.getInventory().getHolder()).getBlock().getLocation(); if
         * (chestLocations.contains(loc)) {
         * 
         * } }
         */
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (this.chestUsers.containsKey(event.getWhoClicked().getName())) {
            // Schedule a task to grab the inventory data and turn it into a
            // string
            this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    final String inventory = DataStrings.valueOf(event.getInventory().getContents());
                    final String player = event.getWhoClicked().getName();
                    // Schedule a threaded task to update the sql table with the
                    // new inventory data
                    getServer().getScheduler().scheduleAsyncDelayedTask(J2MC_seperatechests.instance, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                PreparedStatement ps = J2MC_Manager.getMySQL().getFreshPreparedStatementHotFromTheOven("UPDATE seperateChests_users SET `inventory`=? WHERE `chestID`=? AND `player`=?");
                                ps.setString(1, inventory);
                                ps.setInt(2, chestUsers.get(player));
                                ps.setString(3, player);
                                ps.executeUpdate();
                            } catch (Exception e) {
                                event.setCancelled(true);
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }, 1);
        }
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (this.chestUsers.containsKey(event.getPlayer().getName())) {
            final String inventory = DataStrings.valueOf(event.getInventory().getContents());
            final String player = event.getPlayer().getName();
            getServer().getScheduler().scheduleAsyncDelayedTask(J2MC_seperatechests.instance, new Runnable() {
                @Override
                public void run() {
                    try {
                        PreparedStatement ps = J2MC_Manager.getMySQL().getFreshPreparedStatementHotFromTheOven("UPDATE seperateChests_users SET `inventory`=? WHERE `chestID`=? AND `player`=?");
                        ps.setString(1, inventory);
                        ps.setInt(2, chestUsers.get(player));
                        ps.setString(3, player);
                        ps.executeUpdate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            this.getLogger().info(event.getPlayer().getName() + " closed chest #" + this.chestUsers.get(event.getPlayer().getName()));
            this.chestUsers.remove(event.getPlayer().getName());
        }
        if (this.pendingInventoryChanges.contains(event.getPlayer().getName())) {
            Player player = (Player) event.getPlayer();
            String inventory = DataStrings.valueOf(event.getInventory().getContents());
            Chest chest = (Chest) event.getInventory().getHolder();
            Location loc = chest.getBlock().getLocation();
            try {
                PreparedStatement alreadyExists = J2MC_Manager.getMySQL().getFreshPreparedStatementHotFromTheOven("SELECT `ChestID` FROM  seperateChests_chests WHERE `x`=? AND `y`=? AND `z`=? AND `world`=?");
                alreadyExists.setDouble(1, loc.getX());
                alreadyExists.setDouble(2, loc.getY());
                alreadyExists.setDouble(3, loc.getZ());
                alreadyExists.setString(4, loc.getWorld().getName());
                ResultSet rs = alreadyExists.executeQuery();
                if (rs.first()) {
                    PreparedStatement ps = J2MC_Manager.getMySQL().getFreshPreparedStatementHotFromTheOven("UPDATE seperateChests_chests SET `inventory`=? WHERE `x`=? AND `y`=? AND `z`=? AND `world`=?");
                    ps.setString(1, inventory);
                    ps.setDouble(2, loc.getX());
                    ps.setDouble(3, loc.getY());
                    ps.setDouble(4, loc.getZ());
                    ps.setString(5, loc.getWorld().getName());
                    ps.executeUpdate();
                    player.sendMessage(ChatColor.RED + "Default chest updated successfully");
                    this.getLogger().info(event.getPlayer().getName() + " altered seperate chest #" + rs.getInt("ChestID"));
                    this.pendingInventoryChanges.remove(player.getName());
                    return;
                } else {
                    PreparedStatement ps = J2MC_Manager.getMySQL().getFreshPreparedStatementHotFromTheOven("INSERT INTO seperateChests_chests (`world`,`x`,`y`,`z`,`inventory`) VALUES(?,?,?,?,?)");
                    ps.setString(1, loc.getWorld().getName());
                    ps.setDouble(2, loc.getX());
                    ps.setDouble(3, loc.getY());
                    ps.setDouble(4, loc.getZ());
                    ps.setString(5, inventory);
                    ps.executeUpdate();
                    this.chestLocations.add(loc);
                    player.sendMessage(ChatColor.RED + "Default chest added and inventory saved");
                    this.getLogger().info(event.getPlayer().getName() + " created seperate chest @" + loc.getWorld().getName() + ": " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ());
                    this.pendingInventoryChanges.remove(player.getName());
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            this.pendingInventoryChanges.remove(player.getName());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String player = event.getPlayer().getName();
        if (pendingSmackers.contains(player)) {
            pendingSmackers.remove(player);
        }
        if (pendingInventoryChanges.contains(player)) {
            pendingInventoryChanges.remove(player);
        }
        if (chestUsers.containsKey(player)) {
            chestUsers.remove(player);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (chestLocations.contains(event.getBlock().getLocation())) {
            if (event.getPlayer().hasPermission("j2mc.seperatechest.command")) {
                event.getPlayer().sendMessage("Thats a seperate type chest, you have to remove it from the database first (/sepchests delete)");
            }
            event.setCancelled(true);
        }
    }

}
