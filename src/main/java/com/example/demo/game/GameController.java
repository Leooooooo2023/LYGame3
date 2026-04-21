package com.example.demo.game;

import com.example.demo.model.BattleResult;
import com.example.demo.model.BattleActionRequest;
import com.example.demo.model.BattleSession;
import com.example.demo.model.CreateGameRequest;
import com.example.demo.model.GameSave;
import com.example.demo.model.SimpleRequest;
import org.springframework.http.ResponseEntity;
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
        data.put("elements", Catalog.ELEMENTS);
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

    @PostMapping("/{id}/breakthrough")
    public GameSave breakthrough(@PathVariable String id, @RequestBody SimpleRequest request) {
        return gameService.breakthrough(id, request.characterId);
    }

    @PostMapping("/{id}/explore")
    public Map<String, Object> explore(@PathVariable String id) {
        return gameService.explore(id);
    }

    @PostMapping("/{id}/adventure")
    public BattleSession adventure(@PathVariable String id) {
        return gameService.startAdventure(id);
    }

    @PostMapping("/{id}/battle/tower/start")
    public BattleSession startTowerBattle(@PathVariable String id) {
        return gameService.startTowerBattle(id);
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
