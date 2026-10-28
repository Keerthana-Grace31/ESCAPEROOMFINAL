/* ==========================================================
   PANDEMIC ESCAPE: DOCTOR MISSION (front end)

   The server (PandemicController) owns the game: lives, room order,
   infection / recovery rates, the clock and the answer key. This file
   only asks it what to show and tells it what the player chose.

     GET  /api/pandemic/doctor/scenario
          -> { status, stats, cases[], activeCase }
     POST /api/pandemic/doctor/action   { caseId, answer }
          -> { status, correct, message, stats, cases[], activeCase }

   status: IN_PROGRESS | WON | LOST.  Non-2xx replies (409 wrong room,
   400 bad input) carry the same JSON shape, so they render the same way.

   Re-uses from the other Pandemic scripts (all guarded):
     pandemic.js        timeLeft, timerInterval, updateTimer(), missionFailed(), missionCompleted()
     pandemic-sound.js  playCorrectSound(), playWrongSound(), playClickSound()
   ========================================================== */
(function () {
    'use strict';

    var API = '/api/pandemic/doctor';
    var state = null;        // last server reply
    var busy = false;        // a submit is in flight
    var finished = false;    // mission over (won or lost)
    var lastFocus = null;

    function $(id) { return document.getElementById(id); }
    function el(tag, cls, text) {
        var n = document.createElement(tag);
        if (cls) n.className = cls;
        if (text != null) n.textContent = text;
        return n;
    }
    function call(name) {
        if (typeof window[name] === 'function') { try { window[name](); } catch (e) { /* helper failed: ignore */ } }
    }

    /* ---------------- talking to Spring ---------------- */

    function request(path, options) {
        return fetch(API + path, options).then(function (res) {
            return res.json().then(function (data) {
                return { ok: res.ok, status: res.status, data: data };
            });
        });
    }

    function loadScenario() {
        return request('/scenario', { headers: { 'Accept': 'application/json' } })
            .then(function (r) {
                if (!r.ok) throw new Error('HTTP ' + r.status);
                banner('');
                applyState(r.data, true);
            })
            .catch(function (err) {
                console.error(err);
                banner('Unable to reach the Java backend. Check that the server is running, then reload.');
            });
    }

    function submit(caseId, answer) {
        if (busy || finished) return;
        busy = true;
        setOptionsDisabled(true);
        request('/action', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
            body: JSON.stringify({ caseId: caseId, answer: answer })
        }).then(function (r) {
            busy = false;
            handleAction(r, answer);
        }).catch(function (err) {
            console.error(err);
            busy = false;
            setOptionsDisabled(false);
            feedback('Unable to reach the Java backend. Try again.', 'wrong-feedback');
        });
    }

    /* ---------------- reacting to a reply ---------------- */

    function handleAction(r, answer) {
        var d = r.data;
        if (!d || !d.stats) {
            feedback('Unexpected server reply.', 'wrong-feedback');
            setOptionsDisabled(false);
            return;
        }

        // 400 / 409: nothing was graded, so no sound and no life lost; just show why.
        if (!r.ok) {
            applyState(d, false);
            if (d.status === 'IN_PROGRESS' && d.activeCase) {
                feedback(d.message || 'That action is not allowed right now.', 'wrong-feedback');
                setOptionsDisabled(false);
            } else {
                closeModal();
                endMission(d.status === 'WON');
            }
            return;
        }

        if (d.correct) call('playCorrectSound'); else call('playWrongSound');
        applyState(d, false);

        if (d.status === 'WON') {
            feedback('✓ ' + d.message, 'correct-feedback');
            markChoice(answer, true);
            setTimeout(function () { closeModal(); endMission(true); }, 1400);
        } else if (d.status === 'LOST') {
            feedback('❌ ' + d.message, 'wrong-feedback');
            markChoice(answer, false);
            setTimeout(function () { closeModal(); endMission(false); }, 1500);
        } else if (d.correct) {
            feedback('✓ ' + d.message, 'correct-feedback');
            markChoice(answer, true);
            setTimeout(closeModal, 1200);
        } else {
            markChoice(answer, false);
            showWrong(d);
        }
    }

    function endMission(won) {
        if (finished) return;
        finished = true;
        if (typeof timerInterval !== 'undefined' && timerInterval) clearInterval(timerInterval);
        call(won ? 'missionCompleted' : 'missionFailed');
    }

    /* ---------------- rendering ---------------- */

    function applyState(d, syncClock) {
        state = d;
        renderStats(d.stats);
        renderDoors(d.cases || []);
        syncTimer(d.stats.secondsLeft, syncClock);
    }

    function renderStats(s) {
        var lives = $('lives');
        if (lives) {
            var hearts = '';
            for (var i = 0; i < s.maxLives; i++) hearts += (i < s.lives ? '❤️ ' : '🖤 ');
            lives.innerHTML = hearts + '<br>LIVES: ' + s.lives;
        }
        setMeter('infection', s.infectionRate);
        setMeter('recovery', s.recoveryRate);
        var rooms = $('roomsCleared');
        if (rooms) rooms.textContent = s.solved + ' / ' + s.total;
    }

    function setMeter(name, pct) {
        var val = $(name + 'Value'), bar = $(name + 'Bar');
        if (val) val.textContent = pct + '%';
        if (bar) {
            bar.style.width = pct + '%';
            if (bar.parentNode) bar.parentNode.setAttribute('aria-valuenow', String(pct));
        }
    }

    function renderDoors(cases) {
        cases.forEach(function (c) {
            var card = document.querySelector('.door-card[data-case="' + c.id + '"]');
            if (!card) return;
            var btn = card.querySelector('.secondary-button');
            card.classList.remove('is-solved', 'is-active', 'is-locked');
            card.classList.add(c.state === 'SOLVED' ? 'is-solved' : c.state === 'ACTIVE' ? 'is-active' : 'is-locked');
            if (btn) {
                btn.disabled = c.state !== 'ACTIVE';
                btn.textContent = c.state === 'SOLVED' ? '✓ CLEARED' : c.state === 'ACTIVE' ? 'INVESTIGATE' : '🔒 LOCKED';
            }
        });
    }

    // The server is the clock. Only correct the local countdown when it has drifted.
    function syncTimer(secondsLeft, force) {
        if (typeof timeLeft === 'undefined') return;
        if (force || Math.abs(timeLeft - secondsLeft) > 2) {
            timeLeft = secondsLeft;
            if (typeof updateTimer === 'function') updateTimer();
        }
    }

    function banner(text) {
        var b = $('doctorBanner');
        if (!b) return;
        b.textContent = text;
        b.hidden = !text;
    }

    /* ---------------- puzzle modal ---------------- */

    function openCase(c) {
        if (finished || !c) return;
        lastFocus = document.activeElement;

        var modal = $('puzzleModal');
        if (!modal) {
            modal = el('div');
            modal.id = 'puzzleModal';
            document.body.appendChild(modal);
        }
        modal.textContent = '';
        modal.setAttribute('role', 'dialog');
        modal.setAttribute('aria-modal', 'true');
        modal.setAttribute('aria-label', c.room + ' puzzle');

        var box = el('div', 'puzzle-modal-box');
        var close = el('button', 'close-puzzle', '✕');
        close.type = 'button';
        close.setAttribute('aria-label', 'Close puzzle');
        close.addEventListener('click', closeModal);
        box.appendChild(close);

        var content = el('div', 'puzzle-content');
        content.appendChild(el('div', 'system-status', 'PUZZLE ACCESS: ' + c.room + ' - ' + c.difficulty));
        content.appendChild(el('h2', null, c.icon + ' QUESTION ' + c.step + ' — ' + c.difficulty));
        if (c.brief) content.appendChild(el('p', 'patient-brief', c.brief));

        if (c.data && c.data.length) {
            var note = el('div', 'puzzle-note');
            c.data.forEach(function (line) {
                var p = el('p');
                p.appendChild(el('strong', null, line));
                note.appendChild(p);
            });
            content.appendChild(note);
        }

        content.appendChild(el('p', null, c.prompt));

        var list = el('div', 'mcq-options');
        c.options.forEach(function (o) {
            var b = el('button', 'mcq-option', o.key + '. ' + o.text);
            b.type = 'button';
            b.dataset.key = o.key;
            b.addEventListener('click', function () { submit(c.id, o.key); });
            list.appendChild(b);
        });
        content.appendChild(list);

        var fb = el('div');
        fb.id = 'doctorPuzzleFeedback';
        fb.setAttribute('aria-live', 'polite');
        content.appendChild(fb);

        box.appendChild(content);
        modal.appendChild(box);
        modal.classList.add('active');

        var first = list.querySelector('button');
        if (first) first.focus();
    }

    function closeModal() {
        var modal = $('puzzleModal');
        if (modal) modal.classList.remove('active');
        if (lastFocus && lastFocus.focus && document.contains(lastFocus)) lastFocus.focus();
    }

    function setOptionsDisabled(flag) {
        document.querySelectorAll('#puzzleModal .mcq-option').forEach(function (b) { b.disabled = flag; });
    }

    function markChoice(key, good) {
        var b = document.querySelector('#puzzleModal .mcq-option[data-key="' + key + '"]');
        if (b) b.classList.add(good ? 'is-correct' : 'is-wrong');
    }

    function feedback(text, cls) {
        var f = $('doctorPuzzleFeedback');
        if (!f) return;
        f.textContent = text;
        f.className = cls || '';
    }

    function showWrong(d) {
        var f = $('doctorPuzzleFeedback');
        if (!f) return;
        f.textContent = '';
        f.className = '';
        var box = el('div', 'wrong-feedback');
        box.appendChild(el('div', null, '❌ ' + d.message));
        box.appendChild(el('div', null, 'You lost 1 life. ❤️ ' + d.stats.lives + (d.stats.lives === 1 ? ' LIFE' : ' LIVES') + ' left.'));
        var retry = el('button', 'primary-button retry-button', '↻ RETRY');
        retry.type = 'button';
        retry.addEventListener('click', function () {
            call('playClickSound');
            if (state && state.activeCase) openCase(state.activeCase);
        });
        box.appendChild(retry);
        f.appendChild(box);
    }

    /* ---------------- start-up ---------------- */

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('.door-card[data-case] .secondary-button').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var id = btn.closest('.door-card').dataset['case'];
                if (state && state.activeCase && state.activeCase.id === id) openCase(state.activeCase);
            });
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') closeModal();
        });
        loadScenario();
    });
})();
