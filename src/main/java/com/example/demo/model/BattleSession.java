package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class BattleSession {
    public String id;
    public String saveId;
    public String title;
    public String type;
    public int difficulty;
    public boolean tower;
    public int round = 0;
    public int turnIndex = 0;
    public List<String> turnOrder = new ArrayList<>();
    public String currentActorId;
    public boolean waitingForPlayer;
    public boolean finished;
    public boolean victory;
    public boolean escaped;
    public String postBattleEvent;
    public Map<String, Boolean> usedTreasures = new HashMap<>();
    public int spiritStones;
    public int essence;
    public int exp;
    public List<String> drops = new ArrayList<>();
    public List<String> logs = new ArrayList<>();
    public List<CharacterState> enemies = new ArrayList<>();
    public GameSave save;
}
