package com.example.demo.model;

public class MagicTreasureDef {
    public String id;
    public String name;
    public String quality;
    public String effectType;
    public String targetType;
    public String description;
    public int mpCost;
    public int power;
    public int price;

    public MagicTreasureDef() {
    }

    public MagicTreasureDef(String id, String name, String quality, String effectType, String targetType, String description, int mpCost, int power, int price) {
        this.id = id;
        this.name = name;
        this.quality = quality;
        this.effectType = effectType;
        this.targetType = targetType;
        this.description = description;
        this.mpCost = mpCost;
        this.power = power;
        this.price = price;
    }
}
