package com.example.demo.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CharacterState {
    public String id;
    public String name;
    public String gender;
    public String element;
    public String avatar;
    public boolean companion;
    public boolean unlocked = true;
    public int level = 1;
    public int exp = 0;
    public int nextExp = 100;
    public String realm = "炼气期";
    public int realmLevel = 1;
    public int hp = 100;
    public int maxHp = 100;
    public int mp = 50;
    public int maxMp = 50;
    public int attack = 20;
    public int magic = 20;
    public int defense = 15;
    public int agility = 10;
    public int affection = 0;
    public String treasure;
    public List<String> skills = new ArrayList<>();
    public Map<String, String> equipment = new HashMap<>();
    public Map<String, Integer> statuses = new HashMap<>();
}
