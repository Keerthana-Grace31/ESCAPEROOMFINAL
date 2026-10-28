package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Doctor scenario: GET /api/pandemic/doctor/scenario and POST /api/pandemic/doctor/action. */
@SpringBootTest
class PandemicDoctorApiTest {

    private static final String SCENARIO = "/api/pandemic/doctor/scenario";
    private static final String ACTION = "/api/pandemic/doctor/action";

    @Autowired
    WebApplicationContext ctx;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    private String answer(MockHttpSession s, String caseId, String answer, int expectedStatus) throws Exception {
        return mvc.perform(post(ACTION).session(s).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caseId\":\"" + caseId + "\",\"answer\":\"" + answer + "\"}"))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void scenarioStartsFreshAndNeverLeaksTheAnswerKey() throws Exception {
        String json = mvc.perform(get(SCENARIO).session(new MockHttpSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.stats.lives").value(3))
                .andExpect(jsonPath("$.stats.infectionRate").value(60))
                .andExpect(jsonPath("$.stats.recoveryRate").value(20))
                .andExpect(jsonPath("$.activeCase.id").value("medical"))
                .andExpect(jsonPath("$.activeCase.options.length()").value(4))
                .andReturn().getResponse().getContentAsString();
        assertThat(json.toLowerCase()).doesNotContain("\"answer\"");
    }

    @Test
    void wrongAnswerCostsALifeAndRightAnswersWinTheMission() throws Exception {
        MockHttpSession s = new MockHttpSession();
        mvc.perform(get(SCENARIO).session(s)).andExpect(status().isOk());

        assertThat(answer(s, "medical", "B", 200)).contains("\"correct\":false").contains("\"lives\":2");
        assertThat(answer(s, "medical", "A", 200)).contains("\"correct\":true");
        assertThat(answer(s, "research", "C", 200)).contains("\"solved\":2");
        assertThat(answer(s, "emergency", "C", 200)).contains("\"status\":\"WON\"");

        // the run is over: further answers are refused
        answer(s, "emergency", "C", 409);
    }

    @Test
    void roomsMustBeClearedInOrderAndInputIsValidated() throws Exception {
        MockHttpSession s = new MockHttpSession();
        mvc.perform(get(SCENARIO).session(s)).andExpect(status().isOk());

        assertThat(answer(s, "emergency", "C", 409)).contains("\"lives\":3");   // skipping ahead costs nothing
        answer(s, "nope", "A", 400);
        answer(s, "medical", "Z", 400);
    }

    @Test
    void threeWrongAnswersLoseTheMission() throws Exception {
        MockHttpSession s = new MockHttpSession();
        mvc.perform(get(SCENARIO).session(s)).andExpect(status().isOk());
        answer(s, "medical", "B", 200);
        answer(s, "medical", "C", 200);
        assertThat(answer(s, "medical", "D", 200)).contains("\"status\":\"LOST\"").contains("\"lives\":0");
    }
}
