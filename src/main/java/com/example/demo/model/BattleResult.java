package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

public class BattleResult {
    public boolean victory;
    public String title;
    public int spiritStones;
    public int essence;
    public int exp;
    public List<String> drops = new ArrayList<>();
    public List<String> logs = new ArrayList<>();
    public GameSave save;
}
