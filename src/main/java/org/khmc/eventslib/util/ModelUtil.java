package org.khmc.eventslib.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModelUtil {

    private static Method getCustomModelDataComponentMethod = null;
    private static Method setCustomModelDataComponentMethod = null;
    private static Method hasCustomModelDataComponentMethod = null;
    private static Method setStringsMethod = null;
    private static Method getStringsMethod = null;
    private static Method setFloatsMethod = null;
    private static Method getFloatsMethod = null;
    private static Method setItemModelMethod = null;
    private static Method hasItemModelMethod = null;
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
                } else if (m.getName().equals("hasCustomModelDataComponent") && m.getParameterCount() == 0) {
                    hasCustomModelDataComponentMethod = m;
                    m.setAccessible(true);
                } else if (m.getName().equals("setItemModel") && m.getParameterCount() == 1) {
                    setItemModelMethod = m;
                    m.setAccessible(true);
                } else if (m.getName().equals("hasItemModel") && m.getParameterCount() == 0) {
                    hasItemModelMethod = m;
                    m.setAccessible(true);
                }
            }

            if (getCustomModelDataComponentMethod != null) {
                Class<?> compClass = getCustomModelDataComponentMethod.getReturnType();
                for (Method m : compClass.getMethods()) {
                    if (m.getName().equals("setStrings") && m.getParameterCount() == 1) {
                        setStringsMethod = m;
                        m.setAccessible(true);
                    } else if (m.getName().equals("getStrings") && m.getParameterCount() == 0) {
                        getStringsMethod = m;
                        m.setAccessible(true);
                    } else if (m.getName().equals("setFloats") && m.getParameterCount() == 1) {
                        setFloatsMethod = m;
                        m.setAccessible(true);
                    } else if (m.getName().equals("getFloats") && m.getParameterCount() == 0) {
                        getFloatsMethod = m;
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
     * Guaranteed to use Minecraft 1.21.4+ / 1.21.11 custom_model_data.strings:
     * e.g. custom_model_data={strings:["khmc:mace/inferno"]}
     * Clears conflicting item_model so the client uses the base item's model selector.
     */
    public static void applyModel(ItemMeta meta, String modelInput, Plugin plugin) {
        if (meta == null) return;
        init(meta);

        if (modelInput == null || modelInput.trim().isEmpty() || modelInput.equalsIgnoreCase("none") || modelInput.equalsIgnoreCase("reset")) {
            resetModel(meta, plugin);
            return;
        }

        String trimmed = modelInput.trim();

        // 1. Check if numeric
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
                        List<String> stringList = new ArrayList<>();
                        if (trimmed.contains(",")) {
                            for (String s : trimmed.split(",")) {
                                if (!s.trim().isEmpty()) stringList.add(s.trim());
                            }
                        } else {
                            stringList.add(trimmed);
                        }
                        setStringsMethod.invoke(comp, stringList);
                    }

                    if (isNumeric && setFloatsMethod != null) {
                        setFloatsMethod.invoke(comp, Collections.singletonList((float) numericVal));
                    } else if (!isNumeric) {
                        if (setFloatsMethod != null) {
                            setFloatsMethod.invoke(comp, Collections.emptyList());
                        }
                        try {
                            meta.setCustomModelData(null);
                        } catch (Throwable ignored) {}
                    }

                    setCustomModelDataComponentMethod.invoke(meta, comp);
                }
            }
        } catch (Throwable ignored) {}

        // 3. Clear any conflicting item_model!
        // In Minecraft 1.21.2+, if item_model is present, Minecraft will bypass the base item's custom_model_data strings.
        // We MUST ensure item_model is null so the client uses the base item's custom model strings selector!
        try {
            if (setItemModelMethod != null) {
                setItemModelMethod.invoke(meta, (NamespacedKey) null);
            }
        } catch (Throwable ignored) {}

        // 4. Store in PersistentDataContainer
        if (plugin != null) {
            try {
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "custom_model_data"), PersistentDataType.STRING, trimmed);
                meta.getPersistentDataContainer().remove(new NamespacedKey(plugin, "item_model"));
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Checks if the ItemMeta already has the expected custom model string applied and no conflicting item_model.
     */
    public static boolean hasCustomModelString(ItemMeta meta, String expectedString) {
        if (meta == null || expectedString == null) return false;
        init(meta);
        try {
            // If item_model is set, it conflicts with custom model strings
            if (hasItemModelMethod != null) {
                boolean hasItemModel = (boolean) hasItemModelMethod.invoke(meta);
                if (hasItemModel) return false;
            }

            if (getCustomModelDataComponentMethod != null && getStringsMethod != null) {
                Object comp = getCustomModelDataComponentMethod.invoke(meta);
                if (comp != null) {
                    @SuppressWarnings("unchecked")
                    List<String> strings = (List<String>) getStringsMethod.invoke(comp);
                    if (strings != null && !strings.isEmpty()) {
                        String clean = expectedString.trim();
                        return strings.contains(clean);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * Checks if the ItemMeta has any custom model data or component attached.
     */
    public static boolean hasAnyCustomModel(ItemMeta meta) {
        if (meta == null) return false;
        init(meta);
        try {
            if (meta.hasCustomModelData()) return true;
            if (hasCustomModelDataComponentMethod != null) {
                boolean hasComp = (boolean) hasCustomModelDataComponentMethod.invoke(meta);
                if (hasComp) return true;
            }
            if (hasItemModelMethod != null) {
                boolean hasItemModel = (boolean) hasItemModelMethod.invoke(meta);
                if (hasItemModel) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * Resets any custom model data and item_model on an ItemMeta back to default.
     */
    public static void resetModel(ItemMeta meta, Plugin plugin) {
        if (meta == null) return;
        init(meta);

        try {
            meta.setCustomModelData(null);
        } catch (Throwable ignored) {}

        try {
            if (setItemModelMethod != null) {
                setItemModelMethod.invoke(meta, (NamespacedKey) null);
            }
        } catch (Throwable ignored) {}

        try {
            if (setCustomModelDataComponentMethod != null) {
                setCustomModelDataComponentMethod.invoke(meta, (Object) null);
            }
        } catch (Throwable ignored) {}

        if (plugin != null) {
            try {
                meta.getPersistentDataContainer().remove(new NamespacedKey(plugin, "custom_model_data"));
                meta.getPersistentDataContainer().remove(new NamespacedKey(plugin, "item_model"));
                meta.getPersistentDataContainer().remove(new NamespacedKey(plugin, "collections_skin_id"));
                meta.getPersistentDataContainer().remove(new NamespacedKey(plugin, "collections_managed"));
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Resets any custom model data on an ItemStack back to default.
     */
    public static ItemStack resetModel(ItemStack item, Plugin plugin) {
        if (item == null || item.getType().isAir()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            resetModel(meta, plugin);
            item.setItemMeta(meta);
        }
        return item;
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
