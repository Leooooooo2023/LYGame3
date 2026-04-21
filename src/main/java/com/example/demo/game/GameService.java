package com.example.demo.game;

import com.example.demo.model.BattleResult;
import com.example.demo.model.BattleActionRequest;
import com.example.demo.model.BattleSession;
import com.example.demo.model.CharacterState;
import com.example.demo.model.EquipmentDef;
import com.example.demo.model.GameSave;
import com.example.demo.model.ItemDef;
import com.example.demo.model.MagicTreasureDef;
import com.example.demo.model.PartyRequest;
import com.example.demo.model.SkillDef;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {
    private final ObjectMapper objectMapper;
    private final Path savePath = Path.of("data", "saves.json");
    private final Map<String, GameSave> saves = new ConcurrentHashMap<>();
    private final Map<String, BattleSession> battles = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public GameService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        loadFromDisk();
    }

    public List<GameSave> listSaves() {
        return saves.values().stream()
                .sorted(Comparator.comparing((GameSave s) -> s.updatedAt).reversed())
                .toList();
    }

    public GameSave getSave(String id) {
        GameSave save = saves.get(id);
        if (save == null) {
            throw new IllegalArgumentException("存档不存在");
        }
        normalizeSave(save);
        return save;
    }

    public GameSave createGame(String name, String gender, String element) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 8) {
            throw new IllegalArgumentException("角色名称需要 1-8 个字");
        }
        if (!List.of("男", "女").contains(gender)) {
            throw new IllegalArgumentException("请选择性别");
        }
        if (!Catalog.ELEMENTS.contains(element)) {
            throw new IllegalArgumentException("请选择五行属性");
        }

        GameSave save = new GameSave();
        save.id = UUID.randomUUID().toString();
        save.createdAt = now();
        save.updatedAt = save.createdAt;
        CharacterState player = createPlayer(trimmed, gender, element);
        save.party.add(player);

        save.companions.add(createCompanion("companion_lifeiyu", "厉飞雨", "男", "金", false));
        save.companions.add(createCompanion("companion_chenqiaoqian", "陈巧倩", "女", "木", false));
        save.companions.add(createCompanion("companion_yuanyao", "元瑶", "女", "水", false));
        save.companions.add(createCompanion("companion_ziling", "紫灵", "女", "火", false));
        save.companions.add(createCompanion("companion_xinruyin", "辛如音", "女", "土", false));

        save.inventory.put("huanglong_pill", 5);
        save.inventory.put("heqi_pill", 5);
        save.treasureBag.put("binding_charm", 1);
        save.party.get(0).equipment.put("weapon", "bamboo_sword");
        save.party.get(0).equipment.put("armor", "cloth_robe");
        addCodex(save, "主角");
        for (String skillId : player.skills) {
            addCodex(save, Catalog.SKILLS.get(skillId).name);
        }
        addCodex(save, "黄龙丹");
        addCodex(save, "合气丹");
        addCodex(save, "青竹剑");
        addCodex(save, "灰布道袍");
        addCodex(save, "定身符");

        saves.put(save.id, save);
        persist(save);
        return save;
    }

    public Map<String, Object> shop() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", Catalog.shopItems());
        data.put("equipment", Catalog.shopEquipment());
        data.put("treasures", Catalog.shopTreasures());
        return data;
    }

    public GameSave buy(String saveId, String itemId, int quantity) {
        GameSave save = getSave(saveId);
        int count = Math.max(quantity, 1);
        ItemDef item = Catalog.ITEMS.get(itemId);
        EquipmentDef equipment = Catalog.EQUIPMENT.get(itemId);
        MagicTreasureDef treasure = Catalog.TREASURES.get(itemId);
        int price;
        if (item != null) {
            price = item.price * count;
        } else if (equipment != null) {
            price = equipment.price * count;
        } else if (treasure != null) {
            price = treasure.price * count;
        } else {
            throw new IllegalArgumentException("商品不存在");
        }
        if (save.resources.spiritStones < price) {
            throw new IllegalArgumentException("灵石不足");
        }
        save.resources.spiritStones -= price;
        if (item != null) {
            if ("breakthrough".equals(item.category)) {
                save.resources.breakthroughPill += count;
            } else {
                addCount(save.inventory, item.id, count);
            }
            addCodex(save, item.name);
        } else {
            if (treasure != null) {
                addCount(save.treasureBag, treasure.id, count);
                addCodex(save, treasure.name);
                persist(save);
                return save;
            }
            addCount(save.equipmentBag, equipment.id, count);
            addCodex(save, equipment.name);
        }
        persist(save);
        return save;
    }

    public GameSave restAtInn(String saveId) {
        GameSave save = getSave(saveId);
        int cost = save.party.size() * 100;
        if (save.resources.spiritStones < cost) {
            throw new IllegalArgumentException("灵石不足，无法入住客栈");
        }
        save.resources.spiritStones -= cost;
        for (CharacterState character : save.party) {
            character.hp = character.maxHp;
            character.mp = character.maxMp;
            character.statuses.clear();
        }
        persist(save);
        return save;
    }

    public GameSave usePill(String saveId, String characterId, String itemId) {
        GameSave save = getSave(saveId);
        ItemDef item = Catalog.ITEMS.get(itemId);
        if (item == null || !"pill".equals(item.category)) {
            throw new IllegalArgumentException("丹药不存在");
        }
        if (save.inventory.getOrDefault(itemId, 0) <= 0) {
            throw new IllegalArgumentException("丹药数量不足");
        }
        CharacterState target = findCharacter(save, characterId);
        target.hp = Math.min(target.maxHp, target.hp + target.maxHp * item.hpPercent / 100);
        target.mp = Math.min(target.maxMp, target.mp + target.maxMp * item.mpPercent / 100);
        addCount(save.inventory, itemId, -1);
        persist(save);
        return save;
    }

    public GameSave equip(String saveId, String characterId, String equipmentId) {
        GameSave save = getSave(saveId);
        EquipmentDef equipment = Catalog.EQUIPMENT.get(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("装备不存在");
        }
        if (save.equipmentBag.getOrDefault(equipmentId, 0) <= 0) {
            throw new IllegalArgumentException("背包中没有这件装备");
        }
        CharacterState character = findCharacter(save, characterId);
        String old = character.equipment.put(equipment.slot, equipment.id);
        addCount(save.equipmentBag, equipmentId, -1);
        if (old != null) {
            addCount(save.equipmentBag, old, 1);
        }
        persist(save);
        return save;
    }

    public GameSave unequip(String saveId, String characterId, String slot) {
        GameSave save = getSave(saveId);
        CharacterState character = findCharacter(save, characterId);
        if (slot == null || slot.isBlank()) {
            throw new IllegalArgumentException("请选择要卸下的装备位");
        }
        String old = character.equipment.remove(slot);
        if (old == null) {
            throw new IllegalArgumentException("该装备位没有装备");
        }
        addCount(save.equipmentBag, old, 1);
        persist(save);
        return save;
    }

    public GameSave equipTreasure(String saveId, String characterId, String treasureId) {
        GameSave save = getSave(saveId);
        MagicTreasureDef treasure = Catalog.TREASURES.get(treasureId);
        if (treasure == null) {
            throw new IllegalArgumentException("法宝不存在");
        }
        if (save.treasureBag.getOrDefault(treasureId, 0) <= 0) {
            throw new IllegalArgumentException("背包中没有这件法宝");
        }
        CharacterState character = findCharacter(save, characterId);
        String old = character.treasure;
        character.treasure = treasureId;
        addCount(save.treasureBag, treasureId, -1);
        if (old != null && !old.isBlank()) {
            addCount(save.treasureBag, old, 1);
        }
        persist(save);
        return save;
    }

    public GameSave unequipTreasure(String saveId, String characterId) {
        GameSave save = getSave(saveId);
        CharacterState character = findCharacter(save, characterId);
        if (character.treasure == null || character.treasure.isBlank()) {
            throw new IllegalArgumentException("该角色没有装备法宝");
        }
        addCount(save.treasureBag, character.treasure, 1);
        character.treasure = null;
        persist(save);
        return save;
    }

    public GameSave learnManual(String saveId, String characterId, String itemId) {
        GameSave save = getSave(saveId);
        ItemDef manual = Catalog.ITEMS.get(itemId);
        if (manual == null || !"manual".equals(manual.category)) {
            throw new IllegalArgumentException("秘籍不存在");
        }
        if (save.inventory.getOrDefault(itemId, 0) <= 0) {
            throw new IllegalArgumentException("秘籍数量不足");
        }
        CharacterState character = findCharacter(save, characterId);
        SkillDef skill = Catalog.SKILLS.get(manual.skillId);
        if (skill == null) {
            throw new IllegalArgumentException("秘籍对应技能不存在");
        }
        if (!character.element.equals(skill.element)) {
            throw new IllegalArgumentException("五行属性不符，无法学习");
        }
        if (character.skills.contains(skill.id)) {
            throw new IllegalArgumentException("角色已经掌握该技能");
        }
        character.skills.add(skill.id);
        addCount(save.inventory, itemId, -1);
        addCodex(save, skill.name);
        persist(save);
        return save;
    }

    public GameSave updateParty(String saveId, PartyRequest request) {
        GameSave save = getSave(saveId);
        CharacterState player = save.party.stream()
                .filter(c -> !c.companion)
                .findFirst()
                .orElse(save.party.get(0));
        List<CharacterState> newParty = new ArrayList<>();
        newParty.add(player);
        for (String companionId : request.companionIds) {
            if (newParty.size() >= 3) {
                break;
            }
            CharacterState companion = save.companions.stream()
                    .filter(c -> c.id.equals(companionId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("道友不存在"));
            if (!companion.unlocked) {
                throw new IllegalArgumentException("该道友尚未结识");
            }
            if (newParty.stream().noneMatch(c -> c.id.equals(companion.id))) {
                newParty.add(companion);
            }
        }
        save.party = newParty;
        persist(save);
        return save;
    }

    public GameSave breakthrough(String saveId, String characterId) {
        GameSave save = getSave(saveId);
        CharacterState character = findCharacter(save, characterId);
        if (character.level < 5) {
            throw new IllegalArgumentException("角色达到 5 级后才能突破到筑基期");
        }
        if (save.resources.breakthroughPill <= 0) {
            throw new IllegalArgumentException("需要突破丹 ×1");
        }
        if ("筑基期".equals(character.realm)) {
            throw new IllegalArgumentException("v0.1 暂时只开放到筑基期");
        }
        save.resources.breakthroughPill -= 1;
        character.realm = "筑基期";
        character.realmLevel = 1;
        character.maxHp = character.maxHp * 3 / 2;
        character.maxMp = character.maxMp * 3 / 2;
        character.attack = character.attack * 3 / 2;
        character.magic = character.magic * 3 / 2;
        character.defense = character.defense * 3 / 2;
        character.agility = character.agility * 3 / 2;
        character.hp = character.maxHp;
        character.mp = character.maxMp;
        addCodex(save, "筑基期");
        persist(save);
        return save;
    }

    public Map<String, Object> explore(String saveId) {
        GameSave save = getSave(saveId);
        Map<String, Object> result = new LinkedHashMap<>();
        int roll = random.nextInt(100);
        if (roll < 45) {
            int stones = 80 + random.nextInt(90);
            save.resources.spiritStones += stones;
            result.put("message", "你在太南山溪涧旁发现散落灵石，获得 " + stones + " 灵石。");
        } else if (roll < 70) {
            addCount(save.inventory, "huanglong_pill", 1);
            result.put("message", "你遇到云游散修，获赠黄龙丹 ×1。");
        } else if (roll < 90) {
            save.resources.fiveElementEssence += 1;
            result.put("message", "你炼化一缕天地灵气，获得五行精华 ×1。");
        } else {
            Optional<CharacterState> locked = save.companions.stream().filter(c -> !c.unlocked).findFirst();
            if (locked.isPresent()) {
                CharacterState companion = locked.get();
                companion.unlocked = true;
                if (save.party.size() < 3) {
                    save.party.add(companion);
                }
                addCodex(save, companion.name);
                result.put("message", "山道偶遇道友 " + companion.name + "，对方决定与你同行。");
            } else {
                save.resources.breakthroughPill += 1;
                result.put("message", "你在古修士洞府残壁中找到突破丹 ×1。");
            }
        }
        persist(save);
        result.put("save", save);
        return result;
    }

    public BattleSession startAdventure(String saveId) {
        GameSave save = getSave(saveId);
        return startBattle(save, "太南山探索", "adventure", Math.max(1, save.towerFloor - 1), false);
    }

    public BattleSession startTowerBattle(String saveId) {
        GameSave save = getSave(saveId);
        return startBattle(save, "锁妖塔第 " + save.towerFloor + " 层", "tower", save.towerFloor, true);
    }

    public BattleSession startElementTrial(String saveId, String element) {
        if (!Catalog.ELEMENTS.contains(element)) {
            throw new IllegalArgumentException("五行属性不存在");
        }
        GameSave save = getSave(saveId);
        int difficulty = Math.max(1, Math.min(30, save.highestTowerFloor + 1));
        return startBattle(save, element + "行试炼洞", "trial_" + element, difficulty, false);
    }

    public BattleSession getBattle(String battleId) {
        BattleSession battle = battles.get(battleId);
        if (battle == null) {
            throw new IllegalArgumentException("战斗不存在或已经结束");
        }
        return battle;
    }

    public BattleSession battleAction(String battleId, BattleActionRequest request) {
        BattleSession battle = getBattle(battleId);
        if (battle.finished) {
            return battle;
        }
        if (!battle.waitingForPlayer) {
            advanceBattle(battle);
            return battle;
        }
        GameSave save = getSave(battle.saveId);
        CharacterState actor = findLivingById(save.party, battle.currentActorId);
        if (actor == null) {
            battle.waitingForPlayer = false;
            battle.turnIndex++;
            advanceBattle(battle);
            return battle;
        }

        String action = request.action == null ? "" : request.action;
        switch (action) {
            case "attack" -> {
                CharacterState target = findLivingById(battle.enemies, request.targetId);
                if (target == null) {
                    throw new IllegalArgumentException("请选择可攻击的敌人");
                }
                SkillDef basic = Catalog.SKILLS.get(actor.skills.get(0));
                performSkill(actor, target, basic, battle.logs);
            }
            case "skill" -> {
                SkillDef skill = Catalog.SKILLS.get(request.skillId);
                if (skill == null || !actor.skills.contains(request.skillId)) {
                    throw new IllegalArgumentException("技能不存在");
                }
                if (actor.mp < skill.mpCost) {
                    throw new IllegalArgumentException("灵力不足，无法施展该技能");
                }
                if ("heal".equals(skill.type)) {
                    CharacterState target = findLivingById(save.party, request.targetId);
                    if (target == null) {
                        throw new IllegalArgumentException("请选择要恢复的队友");
                    }
                    performHeal(actor, target, skill, battle.logs);
                } else {
                    CharacterState target = findLivingById(battle.enemies, request.targetId);
                    if (target == null) {
                        throw new IllegalArgumentException("请选择可攻击的敌人");
                    }
                    performSkill(actor, target, skill, battle.logs);
                }
            }
            case "item" -> {
                ItemDef item = Catalog.ITEMS.get(request.itemId);
                if (item == null || !"pill".equals(item.category)) {
                    throw new IllegalArgumentException("只能在战斗中使用恢复丹药");
                }
                if (save.inventory.getOrDefault(request.itemId, 0) <= 0) {
                    throw new IllegalArgumentException("丹药数量不足");
                }
                CharacterState target = findCharacter(save, request.targetId);
                target.hp = Math.min(target.maxHp, target.hp + target.maxHp * item.hpPercent / 100);
                target.mp = Math.min(target.maxMp, target.mp + target.maxMp * item.mpPercent / 100);
                addCount(save.inventory, request.itemId, -1);
                battle.logs.add(actor.name + " 使用 " + item.name + "，为 " + target.name + " 恢复状态。");
            }
            case "treasure" -> {
                if (actor.treasure == null || actor.treasure.isBlank()) {
                    throw new IllegalArgumentException("当前角色没有装备法宝");
                }
                if (battle.usedTreasures.getOrDefault(actor.id, false)) {
                    throw new IllegalArgumentException("该角色本场战斗已经使用过法宝");
                }
                MagicTreasureDef treasure = Catalog.TREASURES.get(actor.treasure);
                if (treasure == null) {
                    throw new IllegalArgumentException("法宝不存在");
                }
                if (actor.mp < treasure.mpCost) {
                    throw new IllegalArgumentException("灵力不足，无法使用法宝");
                }
                CharacterState target = "ally".equals(treasure.targetType)
                        ? findLivingById(save.party, request.targetId)
                        : findLivingById(battle.enemies, request.targetId);
                if (target == null) {
                    throw new IllegalArgumentException("请选择可用目标");
                }
                performTreasure(actor, target, treasure, battle.logs);
                battle.usedTreasures.put(actor.id, true);
            }
            case "escape" -> {
                battle.finished = true;
                battle.escaped = true;
                battle.victory = false;
                battle.waitingForPlayer = false;
                battle.logs.add(actor.name + " 选择撤退，脱离了战斗。");
                finishBattle(battle);
                return battle;
            }
            default -> throw new IllegalArgumentException("未知战斗行动");
        }

        battle.waitingForPlayer = false;
        battle.turnIndex++;
        checkBattleEnd(battle);
        if (!battle.finished) {
            advanceBattle(battle);
        } else {
            finishBattle(battle);
        }
        persist(save);
        battle.save = save;
        return battle;
    }

    public BattleResult towerBattle(String saveId) {
        GameSave save = getSave(saveId);
        return runBattle(save, "锁妖塔第 " + save.towerFloor + " 层", save.towerFloor, true);
    }

    public BattleResult wildBattle(String saveId) {
        GameSave save = getSave(saveId);
        return runBattle(save, "太南山野外", Math.max(1, save.towerFloor - 1), false);
    }

    private BattleSession startBattle(GameSave save, String title, String type, int difficulty, boolean tower) {
        if (save.party.stream().noneMatch(this::alive)) {
            throw new IllegalArgumentException("队伍已经无力再战，请先前往客栈恢复");
        }
        BattleSession battle = new BattleSession();
        battle.id = UUID.randomUUID().toString();
        battle.saveId = save.id;
        battle.title = title;
        battle.type = type;
        battle.difficulty = difficulty;
        battle.tower = tower;
        battle.enemies = createEnemies(difficulty, Math.max(1, save.party.size()));
        for (CharacterState enemy : battle.enemies) {
            addCodex(save, enemy.name);
        }
        battle.logs.add("进入" + title + "，敌方出现：" + names(battle.enemies));
        battle.save = save;
        battles.put(battle.id, battle);
        advanceBattle(battle);
        persist(save);
        return battle;
    }

    private void advanceBattle(BattleSession battle) {
        GameSave save = getSave(battle.saveId);
        battle.save = save;
        while (!battle.finished) {
            checkBattleEnd(battle);
            if (battle.finished) {
                finishBattle(battle);
                return;
            }
            if (battle.turnOrder.isEmpty() || battle.turnIndex >= battle.turnOrder.size()) {
                prepareRound(battle, save);
            }

            CharacterState actor = findAnyActor(save, battle, battle.turnOrder.get(battle.turnIndex));
            if (actor == null || !alive(actor)) {
                battle.turnIndex++;
                continue;
            }
            applyStatuses(actor, battle.logs);
            if (!alive(actor)) {
                battle.turnIndex++;
                continue;
            }
            if (actor.statuses.getOrDefault("眩晕", 0) > 0 || actor.statuses.getOrDefault("束缚", 0) > 0) {
                battle.logs.add(actor.name + " 被控制，无法行动。");
                tickStatus(actor, "眩晕");
                tickStatus(actor, "束缚");
                battle.turnIndex++;
                continue;
            }

            if (save.party.contains(actor)) {
                battle.currentActorId = actor.id;
                battle.waitingForPlayer = true;
                return;
            }

            takeAction(actor, battle.enemies, save.party, battle.logs);
            battle.turnIndex++;
        }
    }

    private void prepareRound(BattleSession battle, GameSave save) {
        battle.round++;
        battle.turnIndex = 0;
        battle.turnOrder = new ArrayList<>();
        List<CharacterState> order = new ArrayList<>();
        order.addAll(save.party.stream().filter(this::alive).toList());
        order.addAll(battle.enemies.stream().filter(this::alive).toList());
        order.sort(Comparator.comparingInt(this::totalAgility).reversed());
        battle.turnOrder.addAll(order.stream().map(c -> c.id).toList());
        battle.logs.add("第 " + battle.round + " 回合");
    }

    private void checkBattleEnd(BattleSession battle) {
        GameSave save = getSave(battle.saveId);
        if (battle.enemies.stream().noneMatch(this::alive)) {
            battle.finished = true;
            battle.victory = true;
        } else if (save.party.stream().noneMatch(this::alive)) {
            battle.finished = true;
            battle.victory = false;
        }
    }

    private void finishBattle(BattleSession battle) {
        GameSave save = getSave(battle.saveId);
        battle.waitingForPlayer = false;
        battle.currentActorId = null;
        if (battle.escaped) {
            battle.logs.add("战斗已结束，未获得奖励。");
        } else if (battle.victory) {
            applyBattleRewards(save, battle);
            if ("adventure".equals(battle.type)) {
                battle.postBattleEvent = triggerPostBattleEvent(save);
                battle.logs.add("战后事件：" + battle.postBattleEvent);
            }
        } else {
            battle.logs.add("队伍败退，保留当前进度。");
        }
        persist(save);
        battle.save = save;
    }

    private void applyBattleRewards(GameSave save, BattleSession battle) {
        if (battle.spiritStones > 0 || battle.exp > 0) {
            return;
        }
        battle.spiritStones = 120 + battle.difficulty * 40;
        battle.essence = battle.tower ? Math.max(1, battle.difficulty / 3) : 1;
        battle.exp = 80 + battle.difficulty * 35;
        save.resources.spiritStones += battle.spiritStones;
        save.resources.fiveElementEssence += battle.essence;
        for (CharacterState character : save.party) {
            gainExp(character, battle.exp, battle.logs);
            if (character.companion) {
                character.affection += 1;
            }
        }
        if (random.nextInt(100) < 35) {
            addCount(save.inventory, "heqi_pill", 1);
            battle.drops.add("合气丹 ×1");
            addCodex(save, "合气丹");
        }
        if (battle.tower && battle.difficulty % 5 == 0) {
            save.resources.breakthroughPill += 1;
            battle.drops.add("突破丹 ×1");
            addCodex(save, "突破丹");
        }
        if (battle.tower) {
            save.highestTowerFloor = Math.max(save.highestTowerFloor, save.towerFloor);
            save.towerFloor = Math.min(30, save.towerFloor + 1);
        }
        if (battle.type != null && battle.type.startsWith("trial_")) {
            String element = battle.type.substring("trial_".length());
            save.resources.fiveElementEssence += 2;
            battle.essence += 2;
            ItemDef manual = trialManual(element);
            if (manual != null && random.nextInt(100) < 50) {
                addCount(save.inventory, manual.id, 1);
                addCodex(save, manual.name);
                battle.drops.add(manual.name + " ×1");
            }
        }
        battle.logs.add("战斗胜利，获得灵石 " + battle.spiritStones + "、五行精华 " + battle.essence + "、经验 " + battle.exp + "。");
    }

    private String triggerPostBattleEvent(GameSave save) {
        int roll = random.nextInt(100);
        if (roll < 45) {
            int stones = 80 + random.nextInt(90);
            save.resources.spiritStones += stones;
            return "你在战场附近发现散落灵石，获得 " + stones + " 灵石。";
        }
        if (roll < 70) {
            addCount(save.inventory, "huanglong_pill", 1);
            addCodex(save, "黄龙丹");
            return "你遇到云游散修，获赠黄龙丹 ×1。";
        }
        if (roll < 90) {
            save.resources.fiveElementEssence += 1;
            return "你炼化一缕天地灵气，获得五行精华 ×1。";
        }
        Optional<CharacterState> locked = save.companions.stream().filter(c -> !c.unlocked).findFirst();
        if (locked.isPresent()) {
            CharacterState companion = locked.get();
            companion.unlocked = true;
            if (save.party.size() < 3) {
                save.party.add(companion);
            }
            addCodex(save, companion.name);
            return "山道偶遇道友 " + companion.name + "，对方决定与你同行。";
        }
        save.resources.breakthroughPill += 1;
        addCodex(save, "突破丹");
        return "你在古修士洞府残壁中找到突破丹 ×1。";
    }

    private void performHeal(CharacterState actor, CharacterState target, SkillDef skill, List<String> logs) {
        actor.mp -= skill.mpCost;
        int amount = target.maxHp * skill.power / 100;
        target.hp = Math.min(target.maxHp, target.hp + amount);
        logs.add(actor.name + " 施展 " + skill.name + "，为 " + target.name + " 恢复 " + amount + " 体力。");
    }

    private void performSkill(CharacterState actor, CharacterState target, SkillDef skill, List<String> logs) {
        actor.mp -= skill.mpCost;
        int source = "physical".equals(skill.type) ? totalAttack(actor) : totalMagic(actor);
        int damage = Math.max(1, source * skill.power / 100 - totalDefense(target) / 2);
        if (hasAdvantage(actor.element, target.element)) {
            damage = damage * 6 / 5;
        }
        target.hp = Math.max(0, target.hp - damage);
        logs.add(actor.name + " 使用 " + skill.name + " 攻击 " + target.name + "，造成 " + damage + " 伤害。");
        if (!skill.status.isBlank() && alive(target) && random.nextInt(100) < 45) {
            target.statuses.put(skill.status, skill.statusTurns);
            logs.add(target.name + " 陷入" + skill.status + "状态。");
        }
    }

    private void performTreasure(CharacterState actor, CharacterState target, MagicTreasureDef treasure, List<String> logs) {
        actor.mp -= treasure.mpCost;
        switch (treasure.effectType) {
            case "stun" -> {
                target.statuses.put("眩晕", Math.max(1, treasure.power));
                logs.add(actor.name + " 祭出 " + treasure.name + "，使 " + target.name + " 眩晕" + Math.max(1, treasure.power) + "回合。");
            }
            case "heal" -> {
                int amount = target.maxHp * treasure.power / 100;
                target.hp = Math.min(target.maxHp, target.hp + amount);
                logs.add(actor.name + " 使用 " + treasure.name + "，为 " + target.name + " 恢复 " + amount + " 体力。");
            }
            case "mp" -> {
                int amount = target.maxMp * treasure.power / 100;
                target.mp = Math.min(target.maxMp, target.mp + amount);
                logs.add(actor.name + " 使用 " + treasure.name + "，为 " + target.name + " 恢复 " + amount + " 灵力。");
            }
            default -> logs.add(actor.name + " 使用 " + treasure.name + "，但法宝没有生效。");
        }
    }

    private ItemDef trialManual(String element) {
        return Catalog.ITEMS.values().stream()
                .filter(item -> "manual".equals(item.category))
                .filter(item -> {
                    SkillDef skill = Catalog.SKILLS.get(item.skillId);
                    return skill != null && element.equals(skill.element);
                })
                .findFirst()
                .orElse(null);
    }

    private CharacterState findAnyActor(GameSave save, BattleSession battle, String actorId) {
        CharacterState actor = save.party.stream().filter(c -> c.id.equals(actorId)).findFirst().orElse(null);
        if (actor != null) {
            return actor;
        }
        return battle.enemies.stream().filter(c -> c.id.equals(actorId)).findFirst().orElse(null);
    }

    private CharacterState findLivingById(List<CharacterState> characters, String id) {
        return characters.stream().filter(c -> c.id.equals(id)).filter(this::alive).findFirst().orElse(null);
    }

    private BattleResult runBattle(GameSave save, String title, int difficulty, boolean tower) {
        if (save.party.stream().noneMatch(this::alive)) {
            throw new IllegalArgumentException("队伍已经无力再战，请先前往客栈恢复");
        }
        BattleResult result = new BattleResult();
        result.title = title;
        List<CharacterState> enemies = createEnemies(difficulty, Math.max(1, save.party.size()));
        for (CharacterState enemy : enemies) {
            addCodex(save, enemy.name);
        }
        result.logs.add("进入" + title + "，敌方出现：" + names(enemies));

        for (int round = 1; round <= 30; round++) {
            result.logs.add("第 " + round + " 回合");
            List<CharacterState> order = new ArrayList<>();
            order.addAll(save.party.stream().filter(this::alive).toList());
            order.addAll(enemies.stream().filter(this::alive).toList());
            order.sort(Comparator.comparingInt(this::totalAgility).reversed());

            for (CharacterState actor : order) {
                if (!alive(actor)) {
                    continue;
                }
                applyStatuses(actor, result.logs);
                if (!alive(actor)) {
                    continue;
                }
                if (actor.statuses.getOrDefault("眩晕", 0) > 0 || actor.statuses.getOrDefault("束缚", 0) > 0) {
                    result.logs.add(actor.name + " 被控制，无法行动。");
                    tickStatus(actor, "眩晕");
                    tickStatus(actor, "束缚");
                    continue;
                }
                boolean actorIsPlayer = save.party.contains(actor);
                List<CharacterState> allies = actorIsPlayer ? save.party : enemies;
                List<CharacterState> opponents = actorIsPlayer ? enemies : save.party;
                takeAction(actor, allies, opponents, result.logs);
                if (save.party.stream().noneMatch(this::alive) || enemies.stream().noneMatch(this::alive)) {
                    break;
                }
            }
            if (save.party.stream().noneMatch(this::alive) || enemies.stream().noneMatch(this::alive)) {
                break;
            }
        }

        result.victory = enemies.stream().noneMatch(this::alive);
        if (result.victory) {
            result.spiritStones = 120 + difficulty * 40;
            result.essence = tower ? Math.max(1, difficulty / 3) : 1;
            result.exp = 80 + difficulty * 35;
            save.resources.spiritStones += result.spiritStones;
            save.resources.fiveElementEssence += result.essence;
            for (CharacterState character : save.party) {
                gainExp(character, result.exp, result.logs);
                if (character.companion) {
                    character.affection += 1;
                }
            }
            if (random.nextInt(100) < 35) {
                addCount(save.inventory, "heqi_pill", 1);
                result.drops.add("合气丹 ×1");
            }
            if (tower && difficulty % 5 == 0) {
                save.resources.breakthroughPill += 1;
                result.drops.add("突破丹 ×1");
            }
            if (tower) {
                save.highestTowerFloor = Math.max(save.highestTowerFloor, save.towerFloor);
                save.towerFloor = Math.min(30, save.towerFloor + 1);
            }
            result.logs.add("战斗胜利，获得灵石 " + result.spiritStones + "、经验 " + result.exp + "。");
        } else {
            result.logs.add("队伍败退，保留当前进度。");
        }
        result.save = save;
        persist(save);
        return result;
    }

    private void takeAction(CharacterState actor, List<CharacterState> allies, List<CharacterState> opponents, List<String> logs) {
        CharacterState weakAlly = allies.stream()
                .filter(this::alive)
                .filter(c -> c.hp * 100 / c.maxHp <= 45)
                .findFirst()
                .orElse(null);
        SkillDef skill = chooseSkill(actor, weakAlly != null);
        if ("heal".equals(skill.type) && weakAlly != null) {
            actor.mp -= skill.mpCost;
            int amount = weakAlly.maxHp * skill.power / 100;
            weakAlly.hp = Math.min(weakAlly.maxHp, weakAlly.hp + amount);
            logs.add(actor.name + " 施展 " + skill.name + "，为 " + weakAlly.name + " 恢复 " + amount + " 体力。");
            return;
        }
        CharacterState target = opponents.stream().filter(this::alive).min(Comparator.comparingInt(c -> c.hp)).orElse(null);
        if (target == null) {
            return;
        }
        actor.mp -= skill.mpCost;
        int source = "physical".equals(skill.type) ? totalAttack(actor) : totalMagic(actor);
        int damage = Math.max(1, source * skill.power / 100 - totalDefense(target) / 2);
        if (hasAdvantage(actor.element, target.element)) {
            damage = damage * 6 / 5;
        }
        target.hp = Math.max(0, target.hp - damage);
        logs.add(actor.name + " 使用 " + skill.name + " 攻击 " + target.name + "，造成 " + damage + " 伤害。");
        if (!skill.status.isBlank() && alive(target) && random.nextInt(100) < 45) {
            target.statuses.put(skill.status, skill.statusTurns);
            logs.add(target.name + " 陷入" + skill.status + "状态。");
        }
    }

    private SkillDef chooseSkill(CharacterState actor, boolean shouldHeal) {
        List<SkillDef> usable = actor.skills.stream()
                .map(Catalog.SKILLS::get)
                .filter(skill -> skill != null && actor.mp >= skill.mpCost)
                .filter(skill -> shouldHeal || !"heal".equals(skill.type))
                .toList();
        if (usable.isEmpty()) {
            return Catalog.SKILLS.get(actor.skills.get(0));
        }
        return usable.get(random.nextInt(usable.size()));
    }

    private void applyStatuses(CharacterState actor, List<String> logs) {
        int poison = actor.statuses.getOrDefault("中毒", 0);
        int burn = actor.statuses.getOrDefault("灼烧", 0);
        if (poison > 0 || burn > 0) {
            int damage = Math.max(3, actor.maxHp / 20);
            actor.hp = Math.max(0, actor.hp - damage);
            logs.add(actor.name + " 受到持续伤害 " + damage + "。");
            tickStatus(actor, "中毒");
            tickStatus(actor, "灼烧");
        }
        tickStatus(actor, "减速");
    }

    private void tickStatus(CharacterState actor, String status) {
        int turns = actor.statuses.getOrDefault(status, 0);
        if (turns <= 1) {
            actor.statuses.remove(status);
        } else {
            actor.statuses.put(status, turns - 1);
        }
    }

    private List<CharacterState> createEnemies(int difficulty, int count) {
        List<String> names = List.of("黑风寨匪徒", "银月妖狼", "鬼灵门邪修", "赤炎虎王", "石巨人");
        List<CharacterState> enemies = new ArrayList<>();
        for (int i = 0; i < Math.min(3, count); i++) {
            String element = Catalog.ELEMENTS.get((difficulty + i) % Catalog.ELEMENTS.size());
            CharacterState enemy = new CharacterState();
            enemy.id = "enemy_" + i + "_" + random.nextInt(9999);
            enemy.name = names.get((difficulty + i) % names.size());
            enemy.gender = "未知";
            enemy.element = element;
            enemy.level = difficulty;
            enemy.maxHp = 70 + difficulty * 22;
            enemy.hp = enemy.maxHp;
            enemy.maxMp = 35 + difficulty * 8;
            enemy.mp = enemy.maxMp;
            enemy.attack = 14 + difficulty * 4;
            enemy.magic = 14 + difficulty * 4;
            enemy.defense = 8 + difficulty * 3;
            enemy.agility = 8 + difficulty * 2 + i;
            enemy.skills.addAll(Catalog.initialSkills(element));
            enemies.add(enemy);
        }
        return enemies;
    }

    private CharacterState createPlayer(String name, String gender, String element) {
        CharacterState player = new CharacterState();
        player.id = "player";
        player.name = name;
        player.gender = gender;
        player.element = element;
        player.skills.addAll(Catalog.initialSkills(element));
        return player;
    }

    private CharacterState createCompanion(String id, String name, String gender, String element, boolean unlocked) {
        CharacterState companion = new CharacterState();
        companion.id = id;
        companion.name = name;
        companion.gender = gender;
        companion.element = element;
        companion.companion = true;
        companion.unlocked = unlocked;
        companion.maxHp = 90;
        companion.hp = 90;
        companion.maxMp = 45;
        companion.mp = 45;
        companion.attack = 18;
        companion.magic = 18;
        companion.defense = 13;
        companion.agility = 11;
        companion.skills.addAll(Catalog.initialSkills(element));
        return companion;
    }

    private void gainExp(CharacterState character, int exp, List<String> logs) {
        character.exp += exp;
        while (character.exp >= character.nextExp) {
            character.exp -= character.nextExp;
            character.level += 1;
            character.nextExp += 60;
            character.realmLevel = Math.min(9, character.realmLevel + 1);
            character.maxHp += 18;
            character.maxMp += 9;
            character.attack += 4;
            character.magic += 4;
            character.defense += 3;
            character.agility += 1;
            character.hp = character.maxHp;
            character.mp = character.maxMp;
            logs.add(character.name + " 提升到 " + character.level + " 级。");
        }
    }

    private CharacterState findCharacter(GameSave save, String characterId) {
        return save.party.stream()
                .filter(c -> c.id.equals(characterId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("角色不存在或未出战"));
    }

    private int totalAttack(CharacterState character) {
        return character.attack + affectionBonus(character) + character.equipment.values().stream().map(Catalog.EQUIPMENT::get).filter(e -> e != null).mapToInt(e -> e.attack).sum();
    }

    private int totalMagic(CharacterState character) {
        return character.magic + affectionBonus(character) + character.equipment.values().stream().map(Catalog.EQUIPMENT::get).filter(e -> e != null).mapToInt(e -> e.magic).sum();
    }

    private int totalDefense(CharacterState character) {
        return character.defense + affectionBonus(character) + character.equipment.values().stream().map(Catalog.EQUIPMENT::get).filter(e -> e != null).mapToInt(e -> e.defense).sum();
    }

    private int totalAgility(CharacterState character) {
        int value = character.agility + affectionBonus(character) + character.equipment.values().stream().map(Catalog.EQUIPMENT::get).filter(e -> e != null).mapToInt(e -> e.agility).sum();
        if (character.statuses.getOrDefault("减速", 0) > 0) {
            value /= 2;
        }
        return value;
    }

    private boolean hasAdvantage(String attacker, String defender) {
        return ("金".equals(attacker) && "木".equals(defender))
                || ("木".equals(attacker) && "土".equals(defender))
                || ("土".equals(attacker) && "水".equals(defender))
                || ("水".equals(attacker) && "火".equals(defender))
                || ("火".equals(attacker) && "金".equals(defender));
    }

    private int affectionBonus(CharacterState character) {
        return character.companion ? character.affection / 5 : 0;
    }

    private void normalizeSave(GameSave save) {
        if (save.treasureBag == null) {
            save.treasureBag = new LinkedHashMap<>();
        }
        if (save.inventory == null) {
            save.inventory = new LinkedHashMap<>();
        }
        if (save.equipmentBag == null) {
            save.equipmentBag = new LinkedHashMap<>();
        }
        for (CharacterState character : save.party) {
            normalizeCharacter(character);
        }
        for (CharacterState character : save.companions) {
            normalizeCharacter(character);
        }
    }

    private void normalizeCharacter(CharacterState character) {
        if (character.skills == null) {
            character.skills = new ArrayList<>();
        }
        if (character.equipment == null) {
            character.equipment = new java.util.HashMap<>();
        }
        if (character.statuses == null) {
            character.statuses = new java.util.HashMap<>();
        }
    }

    private boolean alive(CharacterState character) {
        return character.hp > 0;
    }

    private String names(List<CharacterState> characters) {
        return String.join("、", characters.stream().map(c -> c.name + "(" + c.element + ")").toList());
    }

    private void addCount(Map<String, Integer> map, String key, int count) {
        int value = map.getOrDefault(key, 0) + count;
        if (value <= 0) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }

    private void addCodex(GameSave save, String entry) {
        if (!save.codex.contains(entry)) {
            save.codex.add(entry);
        }
    }

    private void persist(GameSave save) {
        save.updatedAt = now();
        saveToDisk();
    }

    private void loadFromDisk() {
        if (!Files.exists(savePath)) {
            return;
        }
        try {
            List<GameSave> loaded = objectMapper.readValue(savePath.toFile(), new TypeReference<>() {
            });
            for (GameSave save : loaded) {
                saves.put(save.id, save);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("读取存档失败", ex);
        }
    }

    private void saveToDisk() {
        try {
            Files.createDirectories(savePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(savePath.toFile(), listSaves());
        } catch (Exception ex) {
            throw new IllegalStateException("保存存档失败", ex);
        }
    }

    private String now() {
        return Instant.now().toString();
    }
}
