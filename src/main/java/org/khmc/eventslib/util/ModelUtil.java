package org.khmc.eventslib.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ModelUtil {

    private static Method getCustomModelDataComponentMethod = null;
    private static Method setCustomModelDataComponentMethod = null;
    private static Method setStringsMethod = null;
    private static Method setFloatsMethod = null;
    private static Method setItemModelMethod = null;
    private static boolean initialized = false;

    private ModelUtil() {}

    private static synchronized void init(ItemMeta meta) {
        if (initialized) return;
        try {
            Class<?> metaClass = meta.getClass();
            for (Method m : metaClass.getMethods()) {
                if (m.getName().equals("getCustomModelDataComponent") && m.getParameterCount() == 0) {
                    getCustomModelDataComponentMethod = m;
                    m.setAccessible(true);
                } else if (m.getName().equals("setCustomModelDataComponent") && m.getParameterCount() == 1) {
                    setCustomModelDataComponentMethod = m;
                    m.setAccessible(true);
                } else if (m.getName().equals("setItemModel") && m.getParameterCount() == 1) {
                    setItemModelMethod = m;
                    m.setAccessible(true);
                }
            }

            if (getCustomModelDataComponentMethod != null) {
                Class<?> compClass = getCustomModelDataComponentMethod.getReturnType();
                for (Method m : compClass.getMethods()) {
                    if (m.getName().equals("setStrings") && m.getParameterCount() == 1) {
                        setStringsMethod = m;
                        m.setAccessible(true);
                    } else if (m.getName().equals("setFloats") && m.getParameterCount() == 1) {
                        setFloatsMethod = m;
                        m.setAccessible(true);
                    }
                }
            }
        } catch (Throwable ignored) {
        } finally {
            initialized = true;
        }
    }

    /**
     * Applies custom model data to an ItemMeta.
     * Supports:
     * 1. Minecraft 1.21.4+ / 1.21.11 CustomModelDataComponent strings (e.g. "summer_event_icon")
     * 2. Minecraft 1.21.2+ item_model NamespacedKey (e.g. "summer_event_icon" -> "minecraft:summer_event_icon")
     * 3. Legacy numeric custom model data (e.g. "10001")
     * 4. PersistentDataContainer ("custom_model_data" and "item_model")
     */
    public static void applyModel(ItemMeta meta, String modelInput, Plugin plugin) {
        if (meta == null) return;
        init(meta);

        if (modelInput == null || modelInput.trim().isEmpty() || modelInput.equalsIgnoreCase("none") || modelInput.equalsIgnoreCase("reset")) {
            return;
        }

        String trimmed = modelInput.trim();

        // 1. Try legacy numeric CustomModelData
        boolean isNumeric = false;
        int numericVal = 0;
        try {
            numericVal = Integer.parseInt(trimmed);
            if (numericVal > 0) {
                isNumeric = true;
                meta.setCustomModelData(numericVal);
            }
        } catch (NumberFormatException ignored) {}

        // 2. Modern 1.21.4+ CustomModelDataComponent (strings / floats)
        try {
            if (getCustomModelDataComponentMethod != null && setCustomModelDataComponentMethod != null) {
                Object comp = getCustomModelDataComponentMethod.invoke(meta);
                if (comp != null) {
                    if (setStringsMethod != null) {
                        if (trimmed.contains(":")) {
                            String path = trimmed.substring(trimmed.indexOf(':') + 1);
                            setStringsMethod.invoke(comp, List.of(trimmed, path));
                        } else {
                            setStringsMethod.invoke(comp, Collections.singletonList(trimmed));
                        }
                    }
                    if (isNumeric && setFloatsMethod != null) {
                        setFloatsMethod.invoke(comp, Collections.singletonList((float) numericVal));
                    }
                    setCustomModelDataComponentMethod.invoke(meta, comp);
                }
            }
        } catch (Throwable ignored) {}

        // 3. Modern 1.21.2+ item_model component
        try {
            if (setItemModelMethod != null) {
                String keyStr = trimmed.toLowerCase(Locale.ROOT).replace(" ", "_");
                NamespacedKey modelKey = null;
                if (keyStr.contains(":")) {
                    modelKey = NamespacedKey.fromString(keyStr);
                } else {
                    modelKey = NamespacedKey.minecraft(keyStr);
                }
                if (modelKey != null) {
                    setItemModelMethod.invoke(meta, modelKey);
                }
            }
        } catch (Throwable ignored) {}

        // 4. Store in PersistentDataContainer
        if (plugin != null) {
            try {
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "custom_model_data"), PersistentDataType.STRING, trimmed);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_model"), PersistentDataType.STRING, trimmed);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Applies custom model data to an ItemStack and updates its ItemMeta.
     */
    public static ItemStack applyModel(ItemStack item, String modelInput, Plugin plugin) {
        if (item == null || item.getType().isAir()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            applyModel(meta, modelInput, plugin);
            item.setItemMeta(meta);
        }
        return item;
    }
}
