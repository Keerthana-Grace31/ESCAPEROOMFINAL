# Digital Escape Room: integrated project

One Spring Boot 4.1.1 / Java 21 application containing all four team projects.

```
./mvnw spring-boot:run        # then open http://localhost:8080
./mvnw test
```

| Scenario    | Entry URL      | Source project | Notes |
|-------------|----------------|----------------|-------|
| Main menu   | `/`            | B (hub)        | all four cards live |
| Spacecraft  | `/spacecraft`  | A = B          | Mars + Lunar missions, server-side answers |
| Pandemic    | `/pandemic`    | A              | Student + Doctor roles (`/student`, `/doctor`); Doctor is server-driven (see below); console version kept as `PandemicConsole` |
| Time Travel | `/timetravel`  | C              | Mission 1 story game `/timetravel/game`; quiz missions `/timetravel/mission/{1,2}` |
| Zombie      | `/zombie`      | D              | School + Hospital, lives/timer/hints, session stats |

## Layout

```
com.example.demo
  DemoApplication
  hub/HomeController                       main menu (the only mapping of "/")
  spacecraft/ pandemic/ timetravel/ zombie/   one package per game
static/css   style.css (shared), spacecraft.css, pandemic-base.css, pandemic.css, timetravel.css, zombie-base.css, zombie.css
static/js    bgm.js (background music, every page), game.js (hub + Spacecraft SFX), spacecraft.js, pandemic-sound.js, pandemic.js, student.js, doctor.js,
             timetravel-quiz.js, zombie-audio.js, zombie-game.js
static/sounds/common   5 shared UI sounds + hub-ambient.mp3 (background music)
```

## Defects found in the originals and fixed

1. **App could not start**: `/` was mapped in both `Escapecontroller` and `SpacecraftPageController` (A), and `SpacecraftPageController` also collided with D's `/` redirect. Now only `HomeController` maps `/`.
2. **App could not start**: two `@Service GameService` beans (spacecraft in A, time travel in C). Time travel's is now `TimeTravelService`.
3. **Zombie was never component-scanned** (`com.digitalescaperoom.zombie` is outside `com.example.demo`); moved to `com.example.demo.zombie`. Its Boot 3.3.5 / Java 17 pom is replaced by the Boot 4.1.1 / Java 21 stack.
4. **A overwrote the shared `style.css`** with Pandemic-only rules, which un-styled the Spacecraft pages (no `.btn`, `.container`, `.scenario-card`). Each game's stylesheet is now namespaced; `style.css` is B's design system again. Same for the two different `game.js` files.
5. **Pandemic Student puzzle 3 was unwinnable as designed**: clues say 4-7-2 (option A, 472) but the server accepted B (427) and printed "311". Fixed to A / 472.
6. **Time Travel API leaked answers** (`GET /game/puzzle` serialised the answer; `GET /game/answer` used GET for a check). Now `GET /api/timetravel/puzzle` (no answer) and `POST /api/timetravel/answer`.
7. **Time Travel mission 2 had no UI** and both mission buttons opened the same page; mission 2 and a mission 1 practice quiz now use the quiz page. `.mission-button` had no CSS; added. No way back to the menu; added.
8. **`failure.mp3` in A was a 0-byte file**; A/C sounds were also very long (wrong = 3.3 s). B's short sound set is the shared one.
9. Smaller: Pandemic home page had broken header markup and a SOUND button the toggle could not find; `index.js` (stray `javascript` first line, targets nonexistent `main.html`, never loaded) was dropped; the Pandemic ambient file never existed (now optional, see `AMBIENT_SRC` in `pandemic-sound.js`); unhandled audio-autoplay promise rejections in `timegame.html`; empty `Room.java` stub removed; build output (`target/`, `.git`, `.settings`) not carried over.

## Asset paths

All asset URLs are root-relative to Spring Boot's static folder: `/css/...`, `/js/...`, `/sounds/...`,
`/images/...`. There is no `/static/` prefix anywhere (HTML, CSS, JS, or the `ZombieController` sound
constants), so the old `/static/**` resource handler (`WebConfig`) was removed. Two tests guard this
(`noSourceFileUsesTheStaticPrefix`, `everyReferencedAssetExists`).

## Pandemic Doctor API

`doctor.js` talks to `PandemicController`; the server owns lives, room order, infection / recovery rates and the
10-minute clock (kept in the `HttpSession`, so a refresh resumes the run; a finished or expired run restarts on the next page load).

| Endpoint | Purpose |
|----------|---------|
| `GET  /api/pandemic/doctor/scenario` | `{status, stats, cases[], activeCase}`; `status` is `IN_PROGRESS`, `WON` or `LOST`; the answer key is never sent |
| `POST /api/pandemic/doctor/action`   | body `{"caseId":"medical","answer":"A"}` -> same shape plus `correct` and `message`; 409 if the room is locked / the run is over, 400 on bad input |

Rooms are cleared in order (medical -> research -> emergency). Start 60% infection / 20% recovery; a right answer lowers
infection and raises recovery, a wrong answer costs a life and moves both the other way. The old stateless
`/api/doctor/*` endpoints are still there. `PandemicConsole` is a separate command-line program with different puzzles;
nothing in the web flow calls it.

## Background music and sound effects

**Levels** (one place, top of `bgm.js`): music `0.30`, effects `0.38`. The five effect files were recorded very quietly
(click peak -24.5 dBFS vs -5.4 for the loop), so they were raised by one uniform +16 dB; at these volumes a click peaks
about level with the music and ~11 dB above its average. **SOUND OFF now silences music and effects** (`MUTE_SFX_WITH_TOGGLE`
in `bgm.js`; set it to `false` to keep effects on). Every scenario's effects go through `BGM.sfxVolume` / `BGM.sfxAllowed()`.


* One loop, `/sounds/common/hub-ambient.mp3`, plays on an `<audio id="bgm-audio">` element on every page
  (hub, Spacecraft, Pandemic, Zombie, Time Travel). `static/js/bgm.js` drives it.
* The header SOUND button (any element with `data-bgm-toggle`) switches it on/off. The choice is stored in
  `localStorage` as `escaperoom.bgm.enabled` = `on` | `off` (default `on`). A mute made with the old
  per-scenario toggles is respected until the player uses the new one.
* Browsers block autoplay, so if the first `play()` is refused the music starts on the first click/key/touch.
  The playback position is kept in `sessionStorage`, so the track carries on across page navigations.
* Sound effects (`click`, `correct`, `wrong`, `success`, `failure`) are separate `Audio` objects created by
  `game.js`, `zombie-audio.js`, `pandemic-sound.js`, `timetravel-quiz.js` and `timegame.html`. They never touch
  `#bgm-audio`, so the two never interrupt each other. Since the levels update, the SOUND toggle mutes both
  (see the levels note above).
* The per-scenario ambient loops are no longer played (they would layer on top of the shared loop; the three
  Zombie tracks were the same file). The files and the `data-ambient` attributes are still there but inert.
  To give one page its own track, put `data-bgm-src="/sounds/..."` on its `<body>`.
* There is no `hover.mp3` in any source project and nothing plays a hover sound, so none was added.

## Responsive audit (static review, no browser available when this was done)

Viewport: every page has `<meta name="viewport" content="width=device-width, initial-scale=1.0">`
(the four hub/Spacecraft pages get it from `fragments/layout.html`).
Fixed: Pandemic role grid (`minmax(350px, ...)` declared after the mobile rule, so it never collapsed),
a stray `cd` that disabled the Pandemic multiple-choice styles, Zombie scenario cards (300px minimum wider
than a 320px screen), headers whose nav couldn't fit 320px (hub/Spacecraft, Pandemic, Zombie), the Time Travel
nav, and 44px minimum touch targets on phones/coarse pointers.

## Known limitations (content, not bugs)

- Pandemic's console version has a third "Scientist" role and uses different codes (312/321) than the web version; it is not wired into the website.
- Time Travel has two independent content sets (the story game and the quiz bank). They were kept as-is.
- Zombie's three ambient tracks are the same audio file; Pandemic has no ambient track.
