package org.khmc.eventslib.collections.model;

import org.bukkit.Material;

public class CollectionSkin {
    private final String id;
    private String name;
    private final CollectionType type;
    private Material baseMaterial;
    private String customModel;
    private String description;
    private boolean enabled;
    private final long createdAt;

    public CollectionSkin(String id, String name, CollectionType type, Material baseMaterial, String customModel, String description, boolean enabled, long createdAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.baseMaterial = baseMaterial != null ? baseMaterial : (type != null ? type.getBaseMaterial() : Material.NETHERITE_SWORD);
        this.customModel = customModel != null ? customModel : "";
        this.description = description != null ? description : "";
        this.enabled = enabled;
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CollectionType getType() {
        return type;
    }

    public Material getBaseMaterial() {
        return baseMaterial;
    }

    public void setBaseMaterial(Material baseMaterial) {
        this.baseMaterial = baseMaterial;
    }

    public String getCustomModel() {
        return customModel;
    }

    public void setCustomModel(String customModel) {
        this.customModel = customModel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
