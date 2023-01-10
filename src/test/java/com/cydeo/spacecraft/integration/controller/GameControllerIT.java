package com.cydeo.spacecraft.integration.controller;


import com.cydeo.spacecraft.entity.Game;
import com.cydeo.spacecraft.entity.Player;
import com.cydeo.spacecraft.entity.Target;
import com.cydeo.spacecraft.enumtype.AttackType;
import com.cydeo.spacecraft.enumtype.Boost;
import com.cydeo.spacecraft.enumtype.Level;
import com.cydeo.spacecraft.model.request.CreateGameRequest;
import com.cydeo.spacecraft.model.request.CreateHitRequest;
import com.cydeo.spacecraft.model.response.CreateGameResponse;
import com.cydeo.spacecraft.model.response.CreateHitResponse;
import com.cydeo.spacecraft.repository.GameRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@Transactional
public class GameControllerIT{
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private GameRepository gameRepository;

    @Test
    public void should_create_game_successfully() throws Exception {
        CreateGameRequest createGameRequest = new CreateGameRequest();
        createGameRequest.setUsername("username");
        createGameRequest.setBoost(Boost.BIG_BOMB);
        createGameRequest.setLevel(Level.EASY);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/game/createGame")
                        .contentType(MediaType.APPLICATION_JSON)
                //createGameRequest is the body that needs to be sent in the API
                        .content(objectMapper.writeValueAsString(createGameRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").exists())
                .andExpect(jsonPath("$.responseMessage").value("SUCCESS")).andReturn();

        String json = result.getResponse().getContentAsString();
        System.out.println(json);
        CreateGameResponse createGameResponse = objectMapper.readValue(json, CreateGameResponse.class);

        Game game = gameRepository.findById(createGameResponse.getGameId()).orElseThrow();
        assertEquals(game.getIsEnded(), false);
        assertEquals(game.getBoost(), createGameRequest.getBoost());
        assertEquals(game.getLevel(), createGameRequest.getLevel());
    }

    @Test
    @Sql(scripts = "/sql/hit_and_player_win.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/remove_data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void should_player_win_if_player_attack_to_target() throws Exception{
        CreateHitRequest createHitRequest = new CreateHitRequest();
        createHitRequest.setAttackType(AttackType.PLAYER_TO_TARGET);
        createHitRequest.setGameId(1L);
        // make a http request to specific
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/game/createHit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createHitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseMessage").value("SUCCESS"))
                .andExpect(jsonPath("$.isWin").value(true))
                .andExpect(jsonPath("$.isEnded").value(true))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        CreateHitResponse createHitResponse = objectMapper.readValue(json, CreateHitResponse.class);


        Game game = gameRepository.findById(createHitResponse.getGameId()).orElseThrow();

        assertEquals(game.getIsEnded(), true);
        assertEquals(game.getIsWin(), true);

        Set<Target> targetSet = game.getTargets();
        Target target = targetSet.stream().findAny().get();

        int targetHealth = target.getHealth();

        if (targetHealth >= 0){
            Assertions.fail();
        }
    }

    @Test
    @Sql(scripts = "/sql/hit_and_player_lose.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/remove_data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void should_player_lose_when_attack_type_is_target_to_player_and_movable_false() throws Exception {
        CreateHitRequest createHitRequest = new CreateHitRequest();
        createHitRequest.setAttackType(AttackType.TARGET_TO_PLAYER);
        createHitRequest.setGameId(1L);
        // make a http request to specific
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/game/createHit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createHitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseMessage").value("SUCCESS"))
                .andExpect(jsonPath("$.isWin").value(false))
                .andExpect(jsonPath("$.isEnded").value(true))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        CreateHitResponse createHitResponse = objectMapper.readValue(json, CreateHitResponse.class);


        Game game = gameRepository.findById(createHitResponse.getGameId()).orElseThrow();

        assertEquals(game.getIsEnded(), true);
        assertEquals(game.getIsWin(), false);

        Player player = game.getPlayer();
        assertTrue(player.getHealth() < 0);
        assertEquals(player.getHealth(),-99);
        assertTrue(!game.getPlayer().isMovable());
    }

    @Test
    @Sql(scripts = "/sql/hit_and_game_continue.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/remove_data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void should_game_continue_when_attack_type_is_target_to_player_and_movable_is_false() throws Exception {
        CreateHitRequest createHitRequest = new CreateHitRequest();
        createHitRequest.setAttackType(AttackType.TARGET_TO_PLAYER);
        createHitRequest.setGameId(1L);
        // make a http request to specific
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/game/createHit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createHitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseMessage").value("SUCCESS"))
                .andExpect(jsonPath("$.isWin").value(false))
                .andExpect(jsonPath("$.isEnded").value(false))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        CreateHitResponse createHitResponse = objectMapper.readValue(json, CreateHitResponse.class);
        Game game = gameRepository.findById(createHitResponse.getGameId()).orElseThrow();
        assertEquals(game.getIsEnded(), false);
        assertEquals(game.getIsWin(), false);
        assertTrue(!game.getPlayer().isMovable());

    }

    @Test
    @Sql(scripts = "/sql/can_not_hit_game_ended.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/remove_data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void should_not_hit_because_game_is_ended() throws Exception {
        CreateHitRequest createHitRequest = new CreateHitRequest();
        createHitRequest.setAttackType(AttackType.TARGET_TO_PLAYER);
        createHitRequest.setGameId(1L);
        // make a http request to specific
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/game/createHit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createHitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseMessage").value("FAILURE"))
                .andReturn();
    }

    @Test
    @Sql(scripts = "/sql/hit_and_game_continue_movable_true.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/remove_data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void should_game_continue_when_attack_type_is_target_to_player_and_movable_is_true_health_remains_unchanged() throws Exception {
        CreateHitRequest createHitRequest = new CreateHitRequest();
        createHitRequest.setAttackType(AttackType.TARGET_TO_PLAYER);
        createHitRequest.setGameId(1L);
        // make a http request to specific
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/game/createHit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createHitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseMessage").value("SUCCESS"))
                .andExpect(jsonPath("$.isWin").value(false))
                .andExpect(jsonPath("$.isEnded").value(false))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        CreateHitResponse createHitResponse = objectMapper.readValue(json, CreateHitResponse.class);
        Game game = gameRepository.findById(createHitResponse.getGameId()).orElseThrow();
        assertEquals(game.getIsEnded(), false);
        assertEquals(game.getIsWin(), false);
        assertTrue(game.getPlayer().isMovable());
        assertEquals(game.getPlayer().getHealth(), 2000);

    }

    @Test
    @Sql(scripts = "/sql/hit_and_player_lose_movable_true.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/remove_data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void should_player_lose_when_attack_type_is_target_to_player_and_movable_true_health_remains_same() throws Exception {
        CreateHitRequest createHitRequest = new CreateHitRequest();
        createHitRequest.setAttackType(AttackType.TARGET_TO_PLAYER);
        createHitRequest.setGameId(1L);
        // make a http request to specific
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/game/createHit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createHitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseMessage").value("SUCCESS"))
                .andExpect(jsonPath("$.isWin").value(false))
                .andExpect(jsonPath("$.isEnded").value(false))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        CreateHitResponse createHitResponse = objectMapper.readValue(json, CreateHitResponse.class);


        Game game = gameRepository.findById(createHitResponse.getGameId()).orElseThrow();

        assertEquals(game.getIsEnded(), false);
        assertEquals(game.getIsWin(), false);

        Player player = game.getPlayer();
        assertEquals(player.getHealth(),1);
        assertTrue(game.getPlayer().isMovable());
    }
}
