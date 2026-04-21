package com.example.demo.model;

public class EquipmentDef {
    public String id;
    public String name;
    public String slot;
    public String quality;
    public String description;
    public int attack;
    public int magic;
    public int defense;
    public int agility;
    public int price;

    public EquipmentDef() {
    }

    public EquipmentDef(String id, String name, String slot, String quality, String description, int attack, int magic, int defense, int agility, int price) {
        this.id = id;
        this.name = name;
        this.slot = slot;
        this.quality = quality;
        this.description = description;
        this.attack = attack;
        this.magic = magic;
        this.defense = defense;
        this.agility = agility;
        this.price = price;
    }
}
