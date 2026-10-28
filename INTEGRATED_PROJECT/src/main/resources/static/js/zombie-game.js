/*
 * zombie-game.js - client for the Zombie School and Zombie Hospital scenarios.
 *
 * The SERVER owns the lives, the clock and the puzzle progress (HttpSession in ZombieController).
 * This script only displays that state, counts the timer down smoothly and plays the sounds.
 *
 *   GET  /zombie/api/{slug}/state      on page load / refresh
 *   POST /zombie/api/{slug}/start      Start Mission
 *   POST /zombie/api/submit            {scenario, answer}  -> success, message, lives, status, ...
 *   POST /zombie/api/{slug}/hint       costs time, not a life
 *   POST /zombie/api/{slug}/timeout    countdown reached 0 (server re-verifies)
 *   POST /zombie/api/{slug}/restart    Try Again / Play Again
 *
 * Every endpoint returns the same JSON shape, so one render(data) updates the whole page:
 *   { success, message, feedbackColor, sound, status, failReason, lives, maxLives,
 *     remainingSeconds, solved, totalPuzzles, hint, hintsRemaining, puzzle }
 *
 * Sounds: click.mp3 is played by game.js for every button. correct / wrong / success / failure
 * come back from the server in data.sound and are played here.
 *
 * Page attributes used: <body data-scenario="school" data-time-limit="300"
 *   data-fail-time="..." data-fail-lives="..." data-victory="...">
 */
(function () {
    'use strict';

    var body = document.body;
    var slug = body.getAttribute('data-scenario');
    var API = '/zombie/api';
    var TIME_LIMIT = parseInt(body.getAttribute('data-time-limit'), 10) || 300;
    var COLORS = { success: '#35E08A', error: '#FF4D5A', warning: '#FFB547' };
    var ER = window.EscapeRoom || { play: function () {}, ambient: { start: function () {}, stop: function () {} } };

    var status = 'NOT_STARTED';
    var deadline = 0;               // ms timestamp when the countdown reaches zero
    var tickId = null;
    var timeoutSent = false;
    var busy = false;
    var shownPuzzle = 0;            // number of the puzzle currently on screen
    var pendingPuzzle = null;       // next puzzle, held back while the CORRECT panel is open
    var lastRemaining = TIME_LIMIT;

    function $(id) { return document.getElementById(id); }
    function show(id) { var el = $(id); if (el) el.hidden = false; }
    function hide(id) { var el = $(id); if (el) el.hidden = true; }
    function pad(n) { return (n < 10 ? '0' : '') + n; }

    // ------------------------------------------------------------------ network
    function request(method, path, payload) {
        var opts = { method: method, headers: { 'Content-Type': 'application/json' }, credentials: 'same-origin' };
        if (payload !== undefined) opts.body = JSON.stringify(payload);
        // The body is JSON even for 404 / 409 answers, so always parse it.
        return fetch(API + path, opts).then(function (res) { return res.json(); });
    }
    function connectionLost() { setStatus('Connection lost. Check the server and try again.', 'warn'); }

    // ------------------------------------------------------------------ rendering
    function setStatus(message, kind) {
        var el = $('status');
        if (!el) return;
        el.textContent = message || '';
        el.className = 'status-line' + (kind ? ' ' + kind : '');
    }

    function kindFor(color) {
        if (color === COLORS.success) return 'ok';
        if (color === COLORS.error) return 'err';
        return 'warn';
    }

    function renderHearts(lives, maxLives) {
        var box = $('hearts');
        if (!box) return;
        box.textContent = '';
        for (var i = 0; i < maxLives; i++) {
            var span = document.createElement('span');
            span.className = i < lives ? 'heart' : 'heart lost';
            span.textContent = '❤️';
            box.appendChild(span);
        }
        box.setAttribute('aria-label', lives + ' of ' + maxLives + ' lives left');
    }

    function renderTimer(seconds) {
        lastRemaining = seconds;
        var el = $('timer');
        if (!el) return;
        el.textContent = pad(Math.floor(seconds / 60)) + ':' + pad(seconds % 60);
        // normal = cyan, low = orange, critical = red (thresholds scale with the scenario's time limit)
        var cls = 'timer-value';
        if (status === 'ACTIVE' || status === 'NOT_STARTED') {
            if (seconds <= TIME_LIMIT * 0.1) cls += ' critical';
            else if (seconds <= TIME_LIMIT * 0.2) cls += ' low';
        }
        el.className = cls;
    }

    function renderProgress(solved, total) {
        var bar = $('progress-bar');
        if (bar) bar.style.width = (total ? Math.round(100 * solved / total) : 0) + '%';
        var txt = $('puzzle-progress');
        if (txt && status !== 'ACTIVE') txt.textContent = solved + ' / ' + total + ' solved';
    }

    function renderHints(list) {
        var box = $('hint-box');
        var holder = $('hint-items');
        if (!box || !holder) return;
        holder.textContent = '';
        (list || []).forEach(function (text) {
            var p = document.createElement('p');
            p.textContent = text;
            holder.appendChild(p);
        });
        box.hidden = !(list && list.length);
    }

    function renderPuzzle(p) {
        if (!p) return;
        $('puzzle-title').textContent = p.title;
        $('puzzle-story').textContent = p.story;
        $('puzzle-question').textContent = p.question;
        $('puzzle-visual').textContent = p.visual;
        $('puzzle-progress').textContent = 'PUZZLE ' + p.number + ' / ' + p.total;
        renderHints(p.hintsShown);
        if (p.number !== shownPuzzle) {          // a new puzzle: clear the answer box
            shownPuzzle = p.number;
            $('answer').value = '';
        }
        pendingPuzzle = null;
    }

    function setControls(enabled) {
        ['answer', 'btn-submit', 'btn-hint'].forEach(function (id) {
            var el = $(id);
            if (el) el.disabled = !enabled;
        });
    }

    function startCountdown(seconds) {
        deadline = Date.now() + seconds * 1000;
        timeoutSent = false;
        clearInterval(tickId);
        tickId = setInterval(tick, 250);
        renderTimer(seconds);
    }

    /** Single place that applies any server response to the page. */
    function render(d, opts) {
        opts = opts || {};
        status = d.status;
        renderHearts(d.lives, d.maxLives);
        renderProgress(d.solved, d.totalPuzzles);
        if (d.puzzle) {
            if (opts.holdPuzzle) pendingPuzzle = d.puzzle; else renderPuzzle(d.puzzle);
        }
        if (typeof d.hintsRemaining === 'number') {
            $('hints-left').textContent = d.hintsRemaining;
            $('btn-hint').disabled = d.hintsRemaining <= 0 || status !== 'ACTIVE';
        }

        hide('overlay-start'); hide('overlay-failed'); hide('overlay-victory');
        clearInterval(tickId);

        if (status === 'NOT_STARTED') {
            renderTimer(d.remainingSeconds);
            setControls(false);
            show('overlay-start');
        } else if (status === 'ACTIVE') {
            startCountdown(d.remainingSeconds);
            if (!feedbackOpen()) setControls(true);
            if (typeof d.hintsRemaining === 'number' && d.hintsRemaining <= 0) $('btn-hint').disabled = true;
            ER.ambient.start();
        } else {                                    // FAILED or VICTORY
            renderTimer(d.remainingSeconds);
            setControls(false);
            closeFeedback();
            ER.ambient.stop();                      // let success / failure sound be heard clearly
            if (status === 'FAILED') {
                $('failed-msg').textContent = d.failReason === 'TIME_UP'
                    ? body.getAttribute('data-fail-time')
                    : body.getAttribute('data-fail-lives');
                $('failed-solved').textContent = d.solved + ' / ' + d.totalPuzzles;
                show('overlay-failed');
                var retry = document.querySelector('#overlay-failed .js-restart');
                if (retry) retry.focus();
            } else {
                $('victory-msg').textContent = body.getAttribute('data-victory');
                $('victory-lives').textContent = d.lives + ' / ' + d.maxLives;
                $('victory-time').textContent = pad(Math.floor((TIME_LIMIT - d.remainingSeconds) / 60)) + ':' +
                    pad((TIME_LIMIT - d.remainingSeconds) % 60);
                show('overlay-victory');
                var again = document.querySelector('#overlay-victory .js-restart');
                if (again) again.focus();
            }
        }
    }

    // ------------------------------------------------------------------ correct / wrong panels
    function feedbackOpen() { var f = $('feedback'); return !!f && !f.hidden; }

    function openFeedback(kind, title, message, buttonLabel) {
        var f = $('feedback');
        f.className = 'feedback ' + kind;
        $('feedback-title').textContent = title;
        $('feedback-msg').textContent = message;
        $('btn-feedback').textContent = buttonLabel;
        f.hidden = false;
        hide('answer-form');
        setControls(false);
        $('btn-feedback').focus();
    }

    function closeFeedback() {
        if (!$('feedback')) return;
        $('feedback').hidden = true;
        show('answer-form');
    }

    function dismissFeedback() {
        closeFeedback();
        if (pendingPuzzle) renderPuzzle(pendingPuzzle);
        if (status === 'ACTIVE') {
            setControls(true);
            var left = parseInt($('hints-left').textContent, 10);
            $('btn-hint').disabled = !(left > 0);
            $('answer').focus();
            $('answer').select();
            setStatus('', '');
        }
    }

    function damageEffect() {
        body.classList.remove('damage');
        void body.offsetWidth;                      // restart the CSS animation
        body.classList.add('damage');
        var card = $('puzzle-card');
        if (card) { card.classList.remove('shake'); void card.offsetWidth; card.classList.add('shake'); }
    }

    // ------------------------------------------------------------------ game loop
    function tick() {
        if (status !== 'ACTIVE') return;
        var rem = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
        if (rem !== lastRemaining) renderTimer(rem);
        if (rem === 0 && !timeoutSent) {
            timeoutSent = true;
            // The server re-checks its own clock; it only fails the run if time is really up.
            request('POST', '/' + slug + '/timeout').then(function (d) {
                if (d.sound) ER.play(d.sound);      // failure.mp3
                if (d.message && d.status === 'FAILED') setStatus(d.message, 'err');
                render(d);
            }).catch(function () { timeoutSent = false; connectionLost(); });
        }
    }

    // ------------------------------------------------------------------ actions
    function startMission() {
        request('POST', '/' + slug + '/start').then(function (d) {
            setStatus('', '');
            render(d);
            var a = $('answer');
            if (a && status === 'ACTIVE') a.focus();
        }).catch(connectionLost);
    }

    function submitAnswer() {
        if (busy || status !== 'ACTIVE' || feedbackOpen()) return;
        busy = true;
        request('POST', '/submit', { scenario: slug, answer: $('answer').value }).then(function (d) {
            if (d.sound) ER.play(d.sound);          // correct / wrong / success / failure

            if (d.status === 'ACTIVE' && d.success === true) {              // correct, mission continues
                setStatus('', '');
                render(d, { holdPuzzle: true });
                openFeedback('correct', '✓ CORRECT', 'Puzzle solved. ' + d.message.replace(/^Correct!\s*/, ''), 'CONTINUE');
            } else if (d.status === 'ACTIVE' && d.feedbackColor === COLORS.error) {   // wrong, still alive
                damageEffect();
                render(d, { holdPuzzle: true });
                openFeedback('wrong', '✕ INCORRECT', "That wasn't the right solution. " + livesText(d.lives) + ' Try again.', 'TRY AGAIN');
            } else {
                if (d.status === 'FAILED') damageEffect();
                setStatus(d.message, kindFor(d.feedbackColor));
                render(d);                          // empty answer, victory, failure, conflicts
                if (d.status === 'ACTIVE') $('answer').focus();
            }
        }).catch(connectionLost).then(function () { busy = false; });
    }

    function livesText(n) { return n === 1 ? 'One life left.' : n + ' lives left.'; }

    function askHint() {
        if (busy || status !== 'ACTIVE' || feedbackOpen()) return;
        busy = true;
        request('POST', '/' + slug + '/hint').then(function (d) {
            if (d.sound) ER.play(d.sound);          // only present if the penalty ended the run
            setStatus(d.message, kindFor(d.feedbackColor));
            render(d);
        }).catch(connectionLost).then(function () { busy = false; });
    }

    function restart() {
        shownPuzzle = 0;
        pendingPuzzle = null;
        closeFeedback();
        request('POST', '/' + slug + '/restart').then(function (d) {
            setStatus('', '');
            render(d);
            if (status === 'ACTIVE') $('answer').focus();
        }).catch(connectionLost);
    }

    // ------------------------------------------------------------------ init
    document.addEventListener('DOMContentLoaded', function () {
        $('btn-start').addEventListener('click', startMission);
        $('btn-submit').addEventListener('click', submitAnswer);
        $('btn-hint').addEventListener('click', askHint);
        $('btn-feedback').addEventListener('click', dismissFeedback);
        $('answer').addEventListener('keydown', function (e) { if (e.key === 'Enter') submitAnswer(); });
        Array.prototype.forEach.call(document.querySelectorAll('.js-restart'), function (b) {
            b.addEventListener('click', restart);
        });

        // Page load / refresh: the server is the source of truth for lives, step and time.
        request('GET', '/' + slug + '/state').then(function (d) { render(d); }).catch(connectionLost);
    });
})();
