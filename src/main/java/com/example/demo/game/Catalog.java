package com.example.demo.game;

import com.example.demo.model.EquipmentDef;
import com.example.demo.model.ItemDef;
import com.example.demo.model.MagicTreasureDef;
import com.example.demo.model.SkillDef;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Catalog {
    public static final Map<String, SkillDef> SKILLS = new LinkedHashMap<>();
    public static final Map<String, EquipmentDef> EQUIPMENT = new LinkedHashMap<>();
    public static final Map<String, ItemDef> ITEMS = new LinkedHashMap<>();
    public static final Map<String, MagicTreasureDef> TREASURES = new LinkedHashMap<>();
    public static final List<String> ELEMENTS = List.of("金", "木", "水", "火", "土");

    static {
        addSkill("metal_sword", "青元剑诀", "金", "physical", 100, 0, "", 0);
        addSkill("metal_slash", "金光斩", "金", "magical", 150, 20, "", 0);
        addSkill("metal_finger", "裂金指", "金", "physical", 120, 0, "", 0);
        addSkill("metal_poison", "金毒针", "金", "magical", 100, 30, "中毒", 3);

        addSkill("wood_vine", "青藤术", "木", "physical", 100, 0, "", 0);
        addSkill("wood_heal", "长春功", "木", "heal", 30, 25, "", 0);
        addSkill("wood_whip", "毒藤鞭", "木", "physical", 110, 0, "", 0);
        addSkill("wood_poison", "腐毒术", "木", "magical", 90, 25, "中毒", 3);

        addSkill("water_arrow", "水箭术", "水", "magical", 100, 0, "", 0);
        addSkill("water_ice", "玄冰刺", "水", "magical", 140, 25, "", 0);
        addSkill("water_dragon", "水龙卷", "水", "magical", 130, 30, "", 0);
        addSkill("water_slow", "寒毒咒", "水", "magical", 100, 35, "减速", 2);

        addSkill("fire_bolt", "火弹术", "火", "magical", 100, 0, "", 0);
        addSkill("fire_flame", "烈焰术", "火", "magical", 180, 30, "", 0);
        addSkill("fire_rain", "火雨术", "火", "magical", 150, 35, "", 0);
        addSkill("fire_burn", "炎毒术", "火", "magical", 120, 40, "灼烧", 3);

        addSkill("earth_stone", "落石术", "土", "physical", 100, 0, "", 0);
        addSkill("earth_spike", "地刺术", "土", "magical", 130, 20, "", 0);
        addSkill("earth_rock", "飞岩术", "土", "physical", 140, 0, "", 0);
        addSkill("earth_poison", "土毒咒", "土", "magical", 110, 30, "中毒", 3);

        addEquipment("bamboo_sword", "青竹剑", "weapon", "凡品", "以灵竹削成的轻剑，锋利不足，却适合初入仙途的散修练手。", 6, 0, 0, 0, 80);
        addEquipment("steel_sword", "精钢剑", "weapon", "良品", "凡铁百炼而成，剑身沉稳，可明显提升武力。", 14, 0, 0, 0, 280);
        addEquipment("spirit_pearl", "聚灵珠", "focus", "凡品", "能微弱聚拢灵气的小珠，适合法术入门者佩戴。", 0, 6, 0, 0, 80);
        addEquipment("jade_pearl", "玄玉珠", "focus", "良品", "温润玄玉炼成的法器，能让灵力运转更顺畅。", 0, 14, 0, 0, 280);
        addEquipment("cloth_robe", "灰布道袍", "armor", "凡品", "旧布缝成的道袍，虽不华贵，却能挡去些许风霜。", 0, 0, 5, 0, 80);
        addEquipment("bronze_armor", "青铜铠甲", "armor", "良品", "刻有简易护身纹的青铜甲，适合早期硬抗妖兽利爪。", 0, 0, 13, 0, 260);
        addEquipment("straw_shoes", "草鞋", "boots", "凡品", "山路常见的草鞋，轻便但不耐久。", 0, 0, 0, 3, 50);
        addEquipment("cloth_boots", "布靴", "boots", "良品", "鞋底缝入轻身符线，行动更灵活。", 0, 0, 0, 8, 180);

        addItem("huanglong_pill", "黄龙丹", "pill", "凡品", "入门修士常备的疗伤丹药，使用后恢复30%体力。", 40, 30, 0);
        addItem("heqi_pill", "合气丹", "pill", "凡品", "调和气海的小丹，使用后恢复30%灵力。", 40, 0, 30);
        addItem("huiyuan_pill", "回元丹", "pill", "良品", "药力温和稳定，使用后同时恢复30%体力与灵力。", 120, 30, 30);
        addItem("breakthrough_pill", "突破丹", "breakthrough", "良品", "冲击筑基期所需丹药，当前版本突破消耗1枚。", 300, 0, 0);
        addManual("manual_metal_poison", "金毒针秘籍", "良品", "记载金毒针的秘籍，可让金属性角色学习中毒法术。", 360, "metal_poison");
        addManual("manual_wood_poison", "腐毒术秘籍", "良品", "记载腐毒术的秘籍，可让木属性角色学习中毒法术。", 360, "wood_poison");
        addManual("manual_water_slow", "寒毒咒秘籍", "良品", "记载寒毒咒的秘籍，可让水属性角色学习减速法术。", 360, "water_slow");
        addManual("manual_fire_burn", "炎毒术秘籍", "良品", "记载炎毒术的秘籍，可让火属性角色学习灼烧法术。", 360, "fire_burn");
        addManual("manual_earth_poison", "土毒咒秘籍", "良品", "记载土毒咒的秘籍，可让土属性角色学习中毒法术。", 360, "earth_poison");

        addTreasure("binding_charm", "定身符", "凡品", "stun", "enemy", "战斗中使一名敌人眩晕1回合，每场每名角色只能使用一次法宝。", 30, 1, 260);
        addTreasure("spring_charm", "回春符", "良品", "heal", "ally", "战斗中恢复一名队友50%最大体力。", 40, 50, 420);
        addTreasure("spirit_charm", "聚灵符", "良品", "mp", "ally", "战斗中恢复一名队友50%最大灵力。", 40, 50, 420);
    }

    private Catalog() {
    }

    public static Collection<EquipmentDef> shopEquipment() {
        return EQUIPMENT.values().stream().filter(e -> e.price > 0 && !"凡品".equals(e.quality)).toList();
    }

    public static Collection<ItemDef> shopItems() {
        return ITEMS.values();
    }

    public static Collection<MagicTreasureDef> shopTreasures() {
        return TREASURES.values();
    }

    public static List<String> initialSkills(String element) {
        return switch (element) {
            case "金" -> List.of("metal_sword", "metal_slash", "metal_finger");
            case "木" -> List.of("wood_vine", "wood_heal", "wood_whip");
            case "水" -> List.of("water_arrow", "water_ice", "water_dragon");
            case "火" -> List.of("fire_bolt", "fire_flame", "fire_rain");
            case "土" -> List.of("earth_stone", "earth_spike", "earth_rock");
            default -> List.of("metal_sword", "metal_slash", "metal_finger");
        };
    }

    private static void addSkill(String id, String name, String element, String type, int power, int mpCost, String status, int statusTurns) {
        SKILLS.put(id, new SkillDef(id, name, element, type, power, mpCost, status, statusTurns));
    }

    private static void addEquipment(String id, String name, String slot, String quality, String description, int attack, int magic, int defense, int agility, int price) {
        EQUIPMENT.put(id, new EquipmentDef(id, name, slot, quality, description, attack, magic, defense, agility, price));
    }

    private static void addItem(String id, String name, String category, String quality, String description, int price, int hpPercent, int mpPercent) {
        ITEMS.put(id, new ItemDef(id, name, category, quality, description, price, hpPercent, mpPercent));
    }

    private static void addManual(String id, String name, String quality, String description, int price, String skillId) {
        ITEMS.put(id, new ItemDef(id, name, "manual", quality, description, price, 0, 0, skillId));
    }

    private static void addTreasure(String id, String name, String quality, String effectType, String targetType, String description, int mpCost, int power, int price) {
        TREASURES.put(id, new MagicTreasureDef(id, name, quality, effectType, targetType, description, mpCost, power, price));
    }
}
