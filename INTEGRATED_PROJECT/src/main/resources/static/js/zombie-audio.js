/*
 * zombie-audio.js - gameplay sounds for the Zombie scenario.
 *
 *  - plays the shared click sound for every standard button / button-link
 *  - EscapeRoom.play(path, volume): one-shot gameplay sound (correct / wrong / success / failure)
 *
 * Background music and the SOUND ON/OFF toggle are handled for every page by bgm.js
 * (<audio id="bgm-audio">, localStorage "escaperoom.bgm.enabled"). The Zombie "ambient" tracks were
 * the same audio file as the shared hub-ambient.mp3, so playing them as well would only layer the
 * same loop twice. EscapeRoom.ambient is kept so zombie-game.js keeps working:
 *    ambient.stop()   briefly ducks the music so the success / failure jingle is heard clearly
 *    ambient.start()  nothing to do (the music was never stopped)
 */
(function () {
    'use strict';

    var CLICK_SOUND = '/sounds/common/click.mp3';

    function play(path, volume) {
        if (!path) return;
        var shared = window.BGM && window.BGM.sfxAllowed;       // levels + toggle live in bgm.js
        if (shared && !window.BGM.sfxAllowed()) return;
        try {
            var a = new Audio(path);
            a.volume = shared ? window.BGM.sfxVolume : (typeof volume === 'number' ? volume : 0.8);
            var p = a.play();
            if (p && p.catch) p.catch(function () { /* blocked or missing file: ignore */ });
        } catch (e) { /* ignore */ }
    }

    window.EscapeRoom = {
        play: play,
        ambient: {
            start: function () { /* music keeps playing */ },
            stop: function () { if (window.BGM && window.BGM.duck) window.BGM.duck(3000); }
        }
    };

    // Shared click sound. Links styled as buttons wait a moment so the click is actually heard.
    document.addEventListener('click', function (e) {
        var el = e.target.closest ? e.target.closest('button, a.btn, a.scenario-card') : null;
        if (!el || el.disabled) return;
        play(CLICK_SOUND);
        var isNav = el.tagName === 'A' && el.href && el.target !== '_blank' &&
            !e.metaKey && !e.ctrlKey && !e.shiftKey && !e.defaultPrevented;
        if (isNav) {
            e.preventDefault();
            setTimeout(function () { window.location.href = el.href; }, 150);
        }
    });
})();
