package com.example.demo.game;

import com.example.demo.model.BattleResult;
import com.example.demo.model.BattleActionRequest;
import com.example.demo.model.BattleSession;
import com.example.demo.model.CreateGameRequest;
import com.example.demo.model.GameSave;
import com.example.demo.model.PartyRequest;
import com.example.demo.model.SimpleRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/saves")
    public List<GameSave> saves() {
        return gameService.listSaves();
    }

    @PostMapping("/new")
    public GameSave newGame(@RequestBody CreateGameRequest request) {
        return gameService.createGame(request.name, request.gender, request.element);
    }

    @GetMapping("/{id}")
    public GameSave save(@PathVariable String id) {
        return gameService.getSave(id);
    }

    @DeleteMapping("/{id}")
    public List<GameSave> deleteSave(@PathVariable String id) {
        return gameService.deleteSave(id);
    }

    @GetMapping("/shop")
    public Map<String, Object> shop() {
        return gameService.shop();
    }

    @GetMapping("/catalog")
    public Map<String, Object> catalog() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("skills", Catalog.SKILLS);
        data.put("items", Catalog.ITEMS);
        data.put("equipment", Catalog.EQUIPMENT);
        data.put("treasures", Catalog.TREASURES);
        data.put("elements", Catalog.ELEMENTS);
        data.put("maps", gameService.adventureRegions());
        return data;
    }

    @PostMapping("/{id}/shop/buy")
    public GameSave buy(@PathVariable String id, @RequestBody SimpleRequest request) {
        return gameService.buy(id, request.itemId, request.quantity);
    }

    @PostMapping("/{id}/inn")
    public GameSave inn(@PathVariable String id) {
        return gameService.restAtInn(id);
    }

    @PostMapping("/{id}/pill/use")
    public GameSave usePill(@PathVariable String id, @RequestBody SimpleRequest request) {
        return gameService.usePill(id, request.characterId, request.itemId);
    }

    @PostMapping("/{id}/equip")
    public GameSave equip(@PathVariable String id, @RequestBody SimpleRequest request) {
        return gameService.equip(id, request.characterId, request.equipmentId);
    }

    @PostMapping("/{id}/equip/unequip")
    public GameSave unequip(@PathVariable String id, @RequestBody SimpleRequest request) {
        return gameService.unequip(id, request.characterId, request.slot);
    }

    @PostMapping("/{id}/equip/decompose")
    public GameSave decomposeEquipment(@PathVariable String id, @RequestBody SimpleRequest request) {
        return gameService.decomposeEquipment(id, request.equipmentId, request.quantity);
    }

    @PostMapping("/{id}/treasure/equip")
    public GameSave equipTreasure(@PathVariable String id, @RequestBody SimpleRequest request) {
        return gameService.equipTreasure(id, request.characterId, request.treasureId);
    }

    @PostMapping("/{id}/treasure/unequip")
    public GameSave unequipTreasure(@PathVariable String id, @RequestBody SimpleRequest request) {
        return gameService.unequipTreasure(id, request.characterId);
    }

    @PostMapping("/{id}/treasure/decompose")
    public GameSave decomposeTreasure(@PathVariable String id, @RequestBody SimpleRequest request) {
        return gameService.decomposeTreasure(id, request.treasureId, request.quantity);
    }

    @PostMapping("/{id}/manual/learn")
    public GameSave learnManual(@PathVariable String id, @RequestBody SimpleRequest request) {
        return gameService.learnManual(id, request.characterId, request.itemId);
    }

    @PostMapping("/{id}/party")
    public GameSave updateParty(@PathVariable String id, @RequestBody PartyRequest request) {
        return gameService.updateParty(id, request);
    }

    @PostMapping("/{id}/breakthrough")
    public GameSave breakthrough(@PathVariable String id, @RequestBody SimpleRequest request) {
        return gameService.breakthrough(id, request.characterId);
    }

    @PostMapping("/{id}/explore")
    public Map<String, Object> explore(@PathVariable String id) {
        return gameService.explore(id);
    }

    @PostMapping("/{id}/adventure")
    public BattleSession adventure(@PathVariable String id, @RequestBody(required = false) SimpleRequest request) {
        return gameService.startAdventure(id, request == null ? null : request.regionId);
    }

    @PostMapping("/{id}/battle/tower/start")
    public BattleSession startTowerBattle(@PathVariable String id) {
        return gameService.startTowerBattle(id);
    }

    @PostMapping("/{id}/trial/{element}")
    public BattleSession startElementTrial(@PathVariable String id, @PathVariable String element) {
        return gameService.startElementTrial(id, element);
    }

    @PostMapping("/{id}/boss/{bossId}/{difficulty}")
    public BattleSession startWorldBoss(@PathVariable String id, @PathVariable String bossId, @PathVariable String difficulty) {
        return gameService.startWorldBoss(id, bossId, difficulty);
    }

    @PostMapping("/{id}/codex/reward")
    public GameSave claimCodexReward(@PathVariable String id, @RequestBody SimpleRequest request) {
        return gameService.claimCodexReward(id, request.rewardId);
    }

    @GetMapping("/battle/{battleId}")
    public BattleSession battle(@PathVariable String battleId) {
        return gameService.getBattle(battleId);
    }

    @PostMapping("/battle/{battleId}/action")
    public BattleSession battleAction(@PathVariable String battleId, @RequestBody BattleActionRequest request) {
        return gameService.battleAction(battleId, request);
    }

    @PostMapping("/{id}/battle/tower")
    public BattleResult towerBattle(@PathVariable String id) {
        return gameService.towerBattle(id);
    }

    @PostMapping("/{id}/battle/wild")
    public BattleResult wildBattle(@PathVariable String id) {
        return gameService.wildBattle(id);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleGameError(RuntimeException ex) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }
}
