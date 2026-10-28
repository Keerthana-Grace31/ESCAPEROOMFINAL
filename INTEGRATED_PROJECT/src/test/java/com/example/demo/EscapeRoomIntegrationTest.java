package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Whole-application checks: the four games coexist, every route works, no asset link is dead. */
@SpringBootTest
class EscapeRoomIntegrationTest {

    @Autowired
    WebApplicationContext ctx;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    // ------------------------------------------------------------------ hub & pages

    @Test
    void hubLinksToAllFourScenarios() throws Exception {
        String html = mvc.perform(get("/")).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        for (String href : new String[] {"/pandemic", "/zombie", "/timetravel", "/spacecraft"}) {
            assertThat(html).contains("href=\"" + href + "\"");
        }
        assertThat(html).doesNotContain("Coming soon");
    }

    @Test
    void everyPageRenders() throws Exception {
        String[] pages = {
                "/pandemic", "/student", "/doctor",
                "/zombie", "/zombie/school", "/zombie/hospital",
                "/timetravel", "/timetravel/game", "/timetravel/mission/1", "/timetravel/mission/2",
                "/spacecraft", "/spacecraft/mars", "/spacecraft/lunar"};
        for (String p : pages) {
            mvc.perform(get(p)).andExpect(status().isOk());
        }
        mvc.perform(get("/timetravel/mission/3")).andExpect(status().isNotFound());
    }

    @Test
    void pandemicStoryOffersBothRoles() throws Exception {
        String html = mvc.perform(get("/pandemic")).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        assertThat(html).contains("/student").contains("/doctor");
    }

    // ------------------------------------------------------------------ pandemic

    @Test
    void pandemicAnswersMatchTheOnScreenClues() throws Exception {
        String[][] cases = {
                {"/api/student/laboratory", "A"}, {"/api/student/computer", "B"},
                {"/api/student/security", "A"},   // clues spell 4-7-2 = option A (472)
                {"/api/doctor/medical", "A"}, {"/api/doctor/research", "C"}, {"/api/doctor/emergency", "C"}};
        for (String[] c : cases) {
            mvc.perform(post(c[0]).contentType(MediaType.APPLICATION_FORM_URLENCODED).param("answer", c[1]))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.correct").value(true));
            mvc.perform(post(c[0]).contentType(MediaType.APPLICATION_FORM_URLENCODED).param("answer", "D"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.correct").value(false));
        }
        // the old (wrong) key for the security puzzle must now be rejected
        mvc.perform(post("/api/student/security").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("answer", "B")).andExpect(jsonPath("$.correct").value(false));
    }

    // ------------------------------------------------------------------ time travel

    @Test
    void timeTravelNeverLeaksAnswers() throws Exception {
        String json = mvc.perform(get("/api/timetravel/puzzle")
                        .param("scenario", "1").param("period", "PAST").param("question", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andReturn().getResponse().getContentAsString();
        assertThat(json).doesNotContain("answer").doesNotContain("1985");
        mvc.perform(get("/api/timetravel/puzzle")
                .param("scenario", "9").param("period", "PAST").param("question", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void timeTravelChecksAnswers() throws Exception {
        String ok = "{\"scenario\":1,\"period\":\"PRESENT\",\"question\":1,\"answer\":\"1985, 2026, 2080\"}";
        String bad = "{\"scenario\":1,\"period\":\"PRESENT\",\"question\":1,\"answer\":\"2080,2026,1985\"}";
        mvc.perform(post("/api/timetravel/answer").contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(jsonPath("$.correct").value(true));
        mvc.perform(post("/api/timetravel/answer").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(jsonPath("$.correct").value(false));
    }

    // ------------------------------------------------------------------ spacecraft

    @Test
    void spacecraftStillWorksAndHidesTheAnswerKey() throws Exception {
        MockHttpSession s = new MockHttpSession();
        String json = mvc.perform(post("/api/spacecraft/mars/start").session(s))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.lives").value(3))
                .andReturn().getResponse().getContentAsString();
        assertThat(json).doesNotContain("correctIndex");
        mvc.perform(post("/api/spacecraft/pluto/start")).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ no dead links

    private static final Pattern ASSET = Pattern.compile(
            "[\"'(](/(?:css|js|sounds|images)/[A-Za-z0-9_./-]+\\.(?:css|js|mp3|svg|png|jpg))[\"')?]");
    private static final Pattern TH_ASSET = Pattern.compile(
            "@\\{(/(?:css|js|sounds|images)/[A-Za-z0-9_./-]+\\.(?:css|js|mp3|svg|png|jpg))");

    @Test
    void everyReferencedAssetExists() throws IOException {
        Path root = Paths.get("src/main/resources");
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().matches(".*\\.(html|css|js)$"))::iterator) {
                String text = Files.readString(f, StandardCharsets.UTF_8);
                for (Pattern pat : new Pattern[] {ASSET, TH_ASSET}) {
                    Matcher m = pat.matcher(text);
                    while (m.find()) {
                        String url = m.group(1);
                        if (!Files.exists(root.resolve("static" + url))) {
                            missing.add(root.relativize(f) + " -> " + m.group(1));
                        }
                    }
                }
            }
        }
        assertThat(missing).as("dead asset references").isEmpty();
    }

    // ------------------------------------------------------------------ viewport, music layer, asset paths

    private static final String VIEWPORT =
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">";

    @Test
    void everyPageHasViewportAndBackgroundMusic() throws Exception {
        String[] pages = {
                "/", "/pandemic", "/student", "/doctor",
                "/zombie", "/zombie/school", "/zombie/hospital",
                "/timetravel", "/timetravel/game", "/timetravel/mission/1", "/timetravel/mission/2",
                "/spacecraft", "/spacecraft/mars", "/spacecraft/lunar"};
        for (String p : pages) {
            String html = mvc.perform(get(p)).andExpect(status().isOk()).andReturn()
                    .getResponse().getContentAsString();
            assertThat(html).as(p + " viewport").contains(VIEWPORT);
            assertThat(html).as(p + " bgm audio element").contains("id=\"bgm-audio\"")
                    .contains("/sounds/common/hub-ambient.mp3");
            assertThat(html).as(p + " bgm toggle").contains("data-bgm-toggle");
            assertThat(html).as(p + " bgm script").contains("/js/bgm.js");
            assertThat(html).as(p + " no /static/ prefix").doesNotContain("/static/");
        }
    }

    @Test
    void noSourceFileUsesTheStaticPrefix() throws IOException {
        Path root = Paths.get("src/main");
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().matches(".*\\.(html|css|js|java)$"))::iterator) {
                if (Files.readString(f, StandardCharsets.UTF_8).matches("(?s).*/static/(css|js|sounds|images)/.*")) {
                    offenders.add(root.relativize(f).toString());
                }
            }
        }
        assertThat(offenders).as("files still using /static/ asset URLs").isEmpty();
    }
}
