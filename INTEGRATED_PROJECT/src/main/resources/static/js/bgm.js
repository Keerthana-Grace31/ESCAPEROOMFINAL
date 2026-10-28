/* ==========================================================
   DIGITAL ESCAPE ROOM: background music (one layer, every page)

   - Plays /sounds/common/hub-ambient.mp3 on a dedicated
     <audio id="bgm-audio"> element, in a loop.
   - Gameplay sound effects (click / correct / wrong / success /
     failure) are created with `new Audio(...)` by game.js,
     zombie-audio.js, pandemic-sound.js and the time-travel pages.
     They never touch #bgm-audio, so muting or ducking the music
     can't interrupt them and they can't interrupt the music.
   - Any element with [data-bgm-toggle] switches the music on/off.
     The choice is stored in localStorage under
     "escaperoom.bgm.enabled" ("on" | "off"; default "on").
   - Browsers block autoplay until the first user gesture, so if the
     first play() is rejected the music starts on the first
     pointer/key/touch interaction.
   - Pages are separate documents, so the playback position is kept
     in sessionStorage and restored on the next page: the track
     carries on instead of restarting at every navigation.
   - Optional: <body data-bgm-src="/sounds/..."> plays another track.

   LEVELS (one place to tune them)
     BGM_VOLUME 0.30   background loop
     SFX_VOLUME 0.38   click / correct / wrong / success / failure
   The effect files in /sounds/common were recorded very quietly (click peaked at
   -24.5 dBFS, the loop at -5.4 dBFS), so they were lifted by one uniform +16 dB
   in the files themselves; with the values above a click lands about level with
   the music's peaks (~ -17 dBFS) and ~11 dB over its average.

   MASTER SWITCH: SOUND OFF silences the music AND the effects (MUTE_SFX_WITH_TOGGLE).
   Set it to false to keep effects audible while the music is off.

   Public API: window.BGM = { isEnabled, enable, disable, toggle, duck, audio,
                              volume, sfxVolume, sfx(name), sfxAllowed() }
   ========================================================== */
(function () {
  'use strict';

  var KEY = 'escaperoom.bgm.enabled';         // localStorage: 'on' | 'off'
  var POS_KEY = 'escaperoom.bgm.position';    // sessionStorage: {src, t}
  var LEGACY_KEYS = ['der.ambient', 'escaperoom.ambient']; // old per-scenario mute flags
  var DEFAULT_SRC = '/sounds/common/hub-ambient.mp3';
  var VOLUME = 0.30;         // background loop (30%)
  var SFX_VOLUME = 0.38;     // interaction effects (35-40%)
  var DUCKED_VOLUME = 0.10;  // loop level while a success / failure jingle plays
  var MUTE_SFX_WITH_TOGGLE = true;
  var SFX_BASE = ((document.body && document.body.getAttribute('data-sound-base')) || '/sounds') + '/common/';

  function lsGet(k) { try { return window.localStorage.getItem(k); } catch (e) { return null; } }
  function lsSet(k, v) { try { window.localStorage.setItem(k, v); } catch (e) { /* storage blocked */ } }
  function ssGet(k) { try { return window.sessionStorage.getItem(k); } catch (e) { return null; } }
  function ssSet(k, v) { try { window.sessionStorage.setItem(k, v); } catch (e) { /* storage blocked */ } }

  function readEnabled() {
    var v = lsGet(KEY);
    if (v === 'on') return true;
    if (v === 'off') return false;
    // No choice stored yet: respect a mute made with the old per-scenario toggles.
    for (var i = 0; i < LEGACY_KEYS.length; i++) {
      if (lsGet(LEGACY_KEYS[i]) === 'off') return false;
    }
    return true;
  }

  /* ---------- the audio element ---------- */
  var audio = document.getElementById('bgm-audio');
  if (!audio) {
    audio = document.createElement('audio');
    audio.id = 'bgm-audio';
    audio.setAttribute('src', DEFAULT_SRC);
    document.body.appendChild(audio);
  }
  var override = document.body && document.body.getAttribute('data-bgm-src');
  if (override) audio.setAttribute('src', override);
  if (!audio.getAttribute('src')) audio.setAttribute('src', DEFAULT_SRC);
  audio.loop = true;
  audio.preload = 'auto';
  audio.volume = VOLUME;

  var enabled = readEnabled();

  /* ---------- resume position across pages ---------- */
  var resumeAt = 0;
  (function () {
    var raw = ssGet(POS_KEY);
    if (!raw) return;
    try {
      var saved = JSON.parse(raw);
      if (saved && saved.src === audio.getAttribute('src') && saved.t > 0) resumeAt = saved.t;
    } catch (e) { /* ignore corrupt value */ }
  })();

  function applyResume() {
    if (resumeAt > 0 && isFinite(audio.duration) && audio.duration > 0) {
      try { audio.currentTime = resumeAt % audio.duration; } catch (e) { /* ignore */ }
      resumeAt = 0;
    }
  }
  if (audio.readyState >= 1) applyResume();
  else audio.addEventListener('loadedmetadata', applyResume);

  var lastSaved = 0;
  function savePosition(force) {
    if (resumeAt > 0) return;                  // metadata not loaded yet: don't overwrite with 0
    var t = audio.currentTime || 0;
    if (!force && Math.abs(t - lastSaved) < 1) return;
    lastSaved = t;
    ssSet(POS_KEY, JSON.stringify({ src: audio.getAttribute('src'), t: t }));
  }
  audio.addEventListener('timeupdate', function () { savePosition(false); });
  window.addEventListener('pagehide', function () { savePosition(true); });

  /* ---------- play / pause with autoplay-policy fallback ---------- */
  var gestureArmed = false;
  var GESTURES = ['pointerdown', 'keydown', 'touchstart'];

  function onGesture(e) {
    // pressing the toggle itself must not start the music a split second before it is switched off
    if (e && e.target && e.target.closest && e.target.closest('[data-bgm-toggle]')) return;
    disarmGesture();
    if (enabled) tryPlay();
  }
  function armGesture() {
    if (gestureArmed) return;
    gestureArmed = true;
    GESTURES.forEach(function (t) { document.addEventListener(t, onGesture, true); });
  }
  function disarmGesture() {
    if (!gestureArmed) return;
    gestureArmed = false;
    GESTURES.forEach(function (t) { document.removeEventListener(t, onGesture, true); });
  }
  function tryPlay() {
    if (!enabled) return;
    var p;
    try { p = audio.play(); } catch (e) { p = null; }
    if (p && p.catch) p.catch(function () { armGesture(); });   // blocked: wait for a gesture
  }
  audio.addEventListener('playing', disarmGesture);

  /* ---------- toggle buttons ---------- */
  function render() {
    var nodes = document.querySelectorAll('[data-bgm-toggle]');
    for (var i = 0; i < nodes.length; i++) {
      var btn = nodes[i];
      btn.setAttribute('aria-pressed', String(enabled));
      btn.setAttribute('title', enabled ? 'Turn background music off' : 'Turn background music on');
      var icon = btn.querySelector('.sound-icon');
      var label = btn.querySelector('.sound-label');
      if (icon || label) {                                   // header toggle: icon + "Sound On"
        if (icon) icon.textContent = enabled ? '🔊' : '🔇';
        if (label) label.textContent = enabled ? 'Sound On' : 'Sound Off';
      } else {                                               // plain-text toggle: "🔊 SOUND ON"
        btn.textContent = enabled ? '🔊 SOUND ON' : '🔇 SOUND OFF';
      }
    }
  }

  function renderStatus() {
    var st = document.querySelectorAll('[data-bgm-status]');
    for (var i = 0; i < st.length; i++) {
      st[i].textContent = enabled ? 'AUDIO ON' : 'AUDIO OFF';
      st[i].classList.toggle('is-off', !enabled);
    }
    var vol = document.querySelectorAll('[data-bgm-volume]');
    var text = 'MUSIC ' + Math.round(VOLUME * 100) + '% \u00B7 FX ' + Math.round(SFX_VOLUME * 100) + '%';
    for (var j = 0; j < vol.length; j++) vol[j].textContent = enabled ? text : 'MUTED';
  }

  function setEnabled(value) {
    enabled = !!value;
    lsSet(KEY, enabled ? 'on' : 'off');
    render();
    renderStatus();
    if (enabled) tryPlay(); else audio.pause();
  }

  document.addEventListener('click', function (e) {
    var t = e.target && e.target.closest ? e.target.closest('[data-bgm-toggle]') : null;
    if (t) setEnabled(!enabled);
  });

  // keep several open tabs in step
  window.addEventListener('storage', function (e) {
    if (e.key !== KEY) return;
    enabled = readEnabled();
    render();
    renderStatus();
    if (enabled) tryPlay(); else audio.pause();
  });

  // don't play in a hidden tab
  document.addEventListener('visibilitychange', function () {
    if (document.hidden) audio.pause();
    else if (enabled) tryPlay();
  });

  /* ---------- brief ducking (e.g. so the success/failure jingle is heard) ---------- */
  var duckTimer = null;
  function duck(ms) {
    audio.volume = DUCKED_VOLUME;
    clearTimeout(duckTimer);
    duckTimer = setTimeout(function () { audio.volume = VOLUME; }, ms || 2500);
  }

  /* ---------- interaction effects ---------- */
  var sfxCache = {};
  function sfxAllowed() { return enabled || !MUTE_SFX_WITH_TOGGLE; }
  function sfx(name) {
    if (!sfxAllowed()) return;
    try {
      if (!sfxCache[name]) {
        sfxCache[name] = new Audio(SFX_BASE + name + '.mp3');
        sfxCache[name].preload = 'auto';
      }
      var a = sfxCache[name].cloneNode();      // clone: rapid clicks may overlap instead of cutting off
      a.volume = SFX_VOLUME;
      var p = a.play();
      if (p && p.catch) p.catch(function () { /* autoplay blocked or file missing */ });
      if (name === 'success' || name === 'failure') duck(Math.max(1500, (sfxCache[name].duration || 1.2) * 1000 + 400));
    } catch (e) { /* ignore */ }
  }

  window.BGM = {
    volume: VOLUME,
    sfxVolume: SFX_VOLUME,
    sfx: sfx,
    sfxAllowed: sfxAllowed,
    audio: audio,
    isEnabled: function () { return enabled; },
    enable: function () { setEnabled(true); },
    disable: function () { setEnabled(false); },
    toggle: function () { setEnabled(!enabled); },
    duck: duck
  };

  render();
  renderStatus();
  tryPlay();
})();
