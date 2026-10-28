/* ==========================================================
   DIGITAL ESCAPE ROOM: shared interactions (hub + Spacecraft)
   - common gameplay sounds (click/correct/wrong/success/failure)
   - every .btn / .option click plays the shared click sound
   Background music and the SOUND toggle live in bgm.js, on a
   separate <audio id="bgm-audio"> element, so muting the music
   never removes gameplay feedback and the two never interrupt
   each other.
   ========================================================== */
(function () {
  'use strict';

  const base = document.body.dataset.soundBase || '/sounds';

  /* ---------- Gameplay sounds ---------- */
  const cache = {};
  function sfx(name) {
    // Levels and the SOUND toggle live in bgm.js (BGM.sfx); the code below is only a fallback.
    if (window.BGM && window.BGM.sfx) { window.BGM.sfx(name); return; }
    try {
      if (!cache[name]) {
        cache[name] = new Audio(base + '/common/' + name + '.mp3');
        cache[name].preload = 'auto';
      }
      const a = cache[name];
      a.currentTime = 0;
      const p = a.play();
      if (p && p.catch) p.catch(function () { /* autoplay blocked, ignore */ });
    } catch (e) { /* ignore */ }
  }

  /* every standard button / link-button uses the shared click sound
     (this includes the SOUND toggle, which has the .btn class) */
  document.addEventListener('click', function (e) {
    const btn = e.target.closest('.btn:not([data-no-click]), .option, .world-modes a');
    if (btn && !btn.disabled) sfx('click');

    /* <a data-delay-nav>: let the click sound start before the browser leaves the page */
    const link = e.target.closest('a[data-delay-nav][href]');
    if (link && !e.defaultPrevented && !e.metaKey && !e.ctrlKey && !e.shiftKey && link.target !== '_blank') {
      e.preventDefault();
      setTimeout(function () { window.location.href = link.href; }, 150);
    }
  });

  /* hub footer: how many mission cards are actually on the page */
  const worlds = document.querySelectorAll('.world-card').length;
  document.querySelectorAll('[data-world-count]').forEach(function (n) { n.textContent = String(worlds); });

  window.DER = { sfx: sfx };
})();
