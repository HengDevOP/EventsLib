package org.khmc.eventslib.collections.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCollectionProfile {
    private final UUID uuid;
    private final Set<String> unlockedSkins = ConcurrentHashMap.newKeySet();
    private final Map<CollectionType, String> equippedSkins = new ConcurrentHashMap<>();

    public PlayerCollectionProfile(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean hasUnlocked(String skinId) {
        if (skinId == null) return false;
        return unlockedSkins.contains(skinId.toLowerCase());
    }

    public void unlock(String skinId) {
        if (skinId != null) {
            unlockedSkins.add(skinId.toLowerCase());
        }
    }

    public void lock(String skinId) {
        if (skinId != null) {
            unlockedSkins.remove(skinId.toLowerCase());
        }
    }

    public Set<String> getUnlockedSkins() {
        return Collections.unmodifiableSet(unlockedSkins);
    }

    public String getEquippedSkin(CollectionType type) {
        if (type == null) return null;
        return equippedSkins.get(type);
    }

    public void setEquippedSkin(CollectionType type, String skinId) {
        if (type == null) return;
        if (skinId == null || skinId.trim().isEmpty() || skinId.equalsIgnoreCase("default") || skinId.equalsIgnoreCase("none")) {
            equippedSkins.remove(type);
        } else {
            equippedSkins.put(type, skinId.toLowerCase());
        }
    }

    public Map<CollectionType, String> getEquippedSkins() {
        return Collections.unmodifiableMap(equippedSkins);
    }

    public int getUnlockedCount(CollectionType type, Map<String, CollectionSkin> allSkins) {
        if (type == null || allSkins == null) return 0;
        int count = 0;
        for (String skinId : unlockedSkins) {
            CollectionSkin skin = allSkins.get(skinId);
            if (skin != null && skin.getType() == type && skin.isEnabled()) {
                count++;
            }
        }
        return count;
    }
}
