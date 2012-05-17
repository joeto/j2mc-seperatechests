/*
 * Copyright (c) 2011, The Multiverse Team All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. Neither the name of the The Multiverse Team nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package to.joe.j2mc.seperatechests;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedList;
import java.util.List;

/**
 * This class handles the formatting of strings for data i/o.
 */
public class DataStrings {

    /**
     * General delimiter to separate data items.
     */
    public static final String GENERAL_DELIMITER = ";";
    /**
     * Secondary delimiter to separate data items where general delimiter is used in a broader purpose.
     */
    public static final String SECONDARY_DELIMITER = ",";
    /**
     * Special delimiter to separate items since they use both the general and secondary delimiters already.
     */
    public static final String ITEM_DELIMITER = "/";
    /**
     * Delimiter to separate a key and it's value.
     */
    public static final String VALUE_DELIMITER = ":";
    /**
     * Item type identifier.
     */
    public static final String ITEM_TYPE_ID = "t";
    /**
     * Item durability identifier.
     */
    public static final String ITEM_DURABILITY = "d";
    /**
     * Item amount identifier.
     */
    public static final String ITEM_AMOUNT = "#";
    /**
     * Item enchantments identifier.
     */
    public static final String ITEM_ENCHANTS = "e";
    /**
     * Player stats identifier.
     */
    public static final String PLAYER_STATS = "stats";
    /**
     * Player inventory contents identifier.
     */
    public static final String PLAYER_INVENTORY_CONTENTS = "inventoryContents";
    /**
     * Player armor contents identifier.
     */
    public static final String PLAYER_ARMOR_CONTENTS = "armorContents";
    /**
     * Player bed spawn location identifier.
     */
    public static final String PLAYER_BED_SPAWN_LOCATION = "bedSpawnLocation";
    /**
     * Player bed spawn location identifier.
     */
    public static final String PLAYER_LAST_LOCATION = "lastLocation";
    /**
     * Player health identifier.
     */
    public static final String PLAYER_HEALTH = "hp";
    /**
     * Player exp identifier.
     */
    public static final String PLAYER_EXPERIENCE = "xp";
    /**
     * Player total exp identifier.
     */
    public static final String PLAYER_TOTAL_EXPERIENCE = "txp";
    /**
     * Player exp identifier.
     */
    public static final String PLAYER_LEVEL = "el";
    /**
     * Player food level identifier.
     */
    public static final String PLAYER_FOOD_LEVEL = "fl";
    /**
     * Player exhaustion identifier.
     */
    public static final String PLAYER_EXHAUSTION = "ex";
    /**
     * Player saturation identifier.
     */
    public static final String PLAYER_SATURATION = "sa";
    /**
     * Player saturation identifier.
     */
    public static final String PLAYER_FALL_DISTANCE = "fd";
    /**
     * Player saturation identifier.
     */
    public static final String PLAYER_FIRE_TICKS = "ft";
    /**
     * Player saturation identifier.
     */
    public static final String PLAYER_REMAINING_AIR = "ra";
    /**
     * Player saturation identifier.
     */
    public static final String PLAYER_MAX_AIR = "ma";
    /**
     * Location x identifier.
     */
    public static final String LOCATION_X = "x";
    /**
     * Location y identifier.
     */
    public static final String LOCATION_Y = "y";
    /**
     * Location z identifier.
     */
    public static final String LOCATION_Z = "z";
    /**
     * Location world identifier.
     */
    public static final String LOCATION_WORLD = "wo";
    /**
     * Location pitch identifier.
     */
    public static final String LOCATION_PITCH = "pi";
    /**
     * Location yaw identifier.
     */
    public static final String LOCATION_YAW = "ya";
    /**
     * Potion type identifier.
     */
    public static final String POTION_TYPE = "pt";
    /**
     * Potion duration identifier.
     */
    public static final String POTION_DURATION = "pd";
    /**
     * Potion amplifier identifier.
     */
    public static final String POTION_AMPLIFIER = "pa";

    private DataStrings() {
        throw new AssertionError();
    }

    /**
     * Splits a key:value string into a String[2] where string[0] == key and string[1] == value.
     *
     * @param valueString A key:value string.
     * @return A string array split on the {@link #VALUE_DELIMITER}.
     */
    public static String[] splitEntry(String valueString) {
        return valueString.split(VALUE_DELIMITER, 2);
    }

    /**
     * Creates a key:value string from the string form of the key object and value object.
     *
     * @param key   Object that is to be the key.
     * @param value Object that is to be the value.
     * @return String of key and value joined with the {@link #VALUE_DELIMITER}.
     */
    public static String createEntry(Object key, Object value) {
        return key + VALUE_DELIMITER + value;
    }

    /**
     * @param inventoryString An inventory in string form to be parsed into an ItemStack array.
     * @param inventorySize The number of item slots in the inventory.
     * @return an ItemStack array containing the inventory contents parsed from inventoryString.
     */
    public static ItemStack[] parseInventory(String inventoryString, int inventorySize) {
        String[] inventoryArray = inventoryString.split(DataStrings.ITEM_DELIMITER);
        ItemStack[] invContents = new ItemStack[inventorySize];
        for (int i = 0; i < inventorySize; i++) {
            invContents[i] = new ItemStack(Material.AIR);
        }
        for (String itemString : inventoryArray) {
            String[] itemValues = DataStrings.splitEntry(itemString);
            try {
                ItemWrapper itemWrapper = ItemWrapper.wrap(itemValues[1]);
                invContents[Integer.valueOf(itemValues[0])] = itemWrapper.getItem();
                //Logging.debug("ItemString '" + itemString + "' unwrapped as: " + itemWrapper.getItem().toString());
            } catch (Exception e) {
                if (!itemString.isEmpty()) {
                    Bukkit.getLogger().info("Could not parse item string: " + itemString);
                    Bukkit.getLogger().info(e.getMessage());
                }
            }
        }
        return invContents;
    }

    /**
     * Converts an ItemStack array into a String for easy persistence.
     *
     * @param items The items you wish to "string-i-tize".
     * @return A string representation of an inventory.
     */
    public static String valueOf(ItemStack[] items) {
        StringBuilder builder = new StringBuilder();
        for (Integer i = 0; i < items.length; i++) {
            if (items[i] != null && items[i].getTypeId() != 0) {
                if (!builder.toString().isEmpty()) {
                    builder.append(DataStrings.ITEM_DELIMITER);
                }
                builder.append(DataStrings.createEntry(i, ItemWrapper.wrap(items[i]).toString()));
            }
        }
        return builder.toString();
    }
}
