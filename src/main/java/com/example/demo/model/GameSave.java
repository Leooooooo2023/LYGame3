package com.example.demo.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameSave {
    public String id;
    public String createdAt;
    public String updatedAt;
    public String currentArea = "太南山";
    public int towerFloor = 1;
    public int highestTowerFloor = 0;
    public Resources resources = new Resources();
    public List<CharacterState> party = new ArrayList<>();
    public List<CharacterState> companions = new ArrayList<>();
    public Map<String, Integer> inventory = new LinkedHashMap<>();
    public Map<String, Integer> equipmentBag = new LinkedHashMap<>();
    public List<String> codex = new ArrayList<>();
}
