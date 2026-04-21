package com.example.demo.model;

public class ItemDef {
    public String id;
    public String name;
    public String category;
    public String quality;
    public String description;
    public int price;
    public int hpPercent;
    public int mpPercent;
    public String skillId;

    public ItemDef() {
    }

    public ItemDef(String id, String name, String category, String quality, String description, int price, int hpPercent, int mpPercent) {
        this(id, name, category, quality, description, price, hpPercent, mpPercent, "");
    }

    public ItemDef(String id, String name, String category, String quality, String description, int price, int hpPercent, int mpPercent, String skillId) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.quality = quality;
        this.description = description;
        this.price = price;
        this.hpPercent = hpPercent;
        this.mpPercent = mpPercent;
        this.skillId = skillId;
    }
}
