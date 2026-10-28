package com.example.demo.zombie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Ported from the standalone zombie-escape-room project. */
@SpringBootTest
class ZombieFlowTest {

    @Autowired
    WebApplicationContext ctx;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    private ResultActions submit(MockHttpSession s, String scenario, String answer) throws Exception {
        return mvc.perform(post("/zombie/api/submit").session(s).contentType(MediaType.APPLICATION_JSON)
                .content("{\"scenario\":\"" + scenario + "\",\"answer\":\"" + answer + "\"}"));
    }

    private String body(String url) throws Exception {
        return mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void pagesRender() throws Exception {
        assertThat(body("/zombie")).contains("Choose your scenario").contains("2026 Digital Escape Room Team");
        assertThat(body("/zombie/school")).contains("data-scenario=\"school\"");
        assertThat(body("/zombie/hospital")).contains("data-scenario=\"hospital\"");
    }

    @Test
    void mainMenuIsTheSharedHubNotAZombieRedirect() throws Exception {
        // "/" used to redirect to /zombie in the standalone project; it must now be the real hub.
        mvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void zombieAssetsServedFromRoot() throws Exception {
        for (String p : new String[] {"/css/zombie-base.css", "/css/zombie.css",
                "/js/zombie-game.js", "/js/zombie-audio.js",
                "/sounds/common/click.mp3", "/sounds/common/correct.mp3",
                "/sounds/common/wrong.mp3", "/sounds/common/failure.mp3",
                "/sounds/common/success.mp3", "/sounds/zombie/hub-ambient.mp3",
                "/sounds/common/hub-ambient.mp3", "/js/bgm.js",
                "/images/zombie/city.svg"}) {
            mvc.perform(get(p)).andExpect(status().isOk());
        }
    }

    @Test
    void schoolVictoryPath() throws Exception {
        MockHttpSession s = new MockHttpSession();
        mvc.perform(get("/zombie/api/school/state").session(s)).andExpect(jsonPath("$.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.lives").value(3));
        mvc.perform(post("/zombie/api/school/start").session(s)).andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.puzzle.number").value(1));
        submit(s, "school", "31").andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.lives").value(2))
                .andExpect(jsonPath("$.sound").value("/sounds/common/wrong.mp3"))
                .andExpect(jsonPath("$.feedbackColor").value("#FF4D5A"));
        submit(s, "school", " 32 ").andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.puzzle.number").value(2))
                .andExpect(jsonPath("$.sound").value("/sounds/common/correct.mp3"))
                .andExpect(jsonPath("$.feedbackColor").value("#35E08A"));
        submit(s, "school", "library").andExpect(jsonPath("$.puzzle.number").value(3));
        submit(s, "school", "22").andExpect(jsonPath("$.status").value("VICTORY"))
                .andExpect(jsonPath("$.sound").value("/sounds/common/success.mp3"));
        // scenarios are independent: hospital is untouched
        mvc.perform(get("/zombie/api/hospital/state").session(s))
                .andExpect(jsonPath("$.status").value("NOT_STARTED"));
    }

    @Test
    void threeWrongAnswersFailTheMission() throws Exception {
        MockHttpSession s = new MockHttpSession();
        mvc.perform(post("/zombie/api/hospital/start").session(s));
        submit(s, "hospital", "1");
        submit(s, "hospital", "2");
        submit(s, "hospital", "3").andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.lives").value(0)).andExpect(jsonPath("$.failReason").value("NO_LIVES"))
                .andExpect(jsonPath("$.sound").value("/sounds/common/failure.mp3"));
        submit(s, "hospital", "15").andExpect(status().isConflict());
        mvc.perform(post("/zombie/api/hospital/restart").session(s)).andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.lives").value(3));
    }

    @Test
    void emptyAnswerCostsNothingAndHintsCostTime() throws Exception {
        MockHttpSession s = new MockHttpSession();
        mvc.perform(post("/zombie/api/school/start").session(s));
        submit(s, "school", "   ").andExpect(jsonPath("$.lives").value(3)).andExpect(jsonPath("$.success").value(false));
        String json = mvc.perform(post("/zombie/api/school/hint").session(s))
                .andExpect(jsonPath("$.hint").exists()).andExpect(jsonPath("$.hintsRemaining").value(2))
                .andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("\"remainingSeconds\":(\\d+)").matcher(json);
        assertThat(m.find()).isTrue();
        assertThat(Long.parseLong(m.group(1))).isLessThanOrEqualTo(280L); // 300 s limit - 20 s hint penalty
        mvc.perform(get("/zombie/api/school/state").session(s))
                .andExpect(jsonPath("$.puzzle.hintsShown.length()").value(1));
    }

    @Test
    void unknownScenarioIs404() throws Exception {
        submit(new MockHttpSession(), "castle", "x").andExpect(status().isNotFound());
    }
}
