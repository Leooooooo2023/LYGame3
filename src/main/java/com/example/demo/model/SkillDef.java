package com.example.demo.model;

public class SkillDef {
    public String id;
    public String name;
    public String element;
    public String type;
    public int power;
    public int mpCost;
    public String status;
    public int statusTurns;

    public SkillDef() {
    }

    public SkillDef(String id, String name, String element, String type, int power, int mpCost, String status, int statusTurns) {
        this.id = id;
        this.name = name;
        this.element = element;
        this.type = type;
        this.power = power;
        this.mpCost = mpCost;
        this.status = status;
        this.statusTurns = statusTurns;
    }
}
