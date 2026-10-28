/* ==========================================================
   SPACECRAFT mission UI. Talks to the Java game logic (REST).
   The server decides what is correct; this file only renders.
   ========================================================== */
(function () {
  'use strict';

  const root = document.getElementById('mission');
  if (!root) return;
  const api = root.dataset.api;

  const $ = function (id) { return document.getElementById(id); };
  const el = {
    briefing: $('screen-briefing'), play: $('screen-play'), end: $('screen-end'),
    lives: $('lives'), count: $('challenge-count'), bar: $('progress-bar'),
    title: $('challenge-title'), clues: $('clue-list'), question: $('challenge-question'),
    options: $('options'), submit: $('btn-submit'), hintBtn: $('btn-hint'),
    hint: $('hint-panel'), hintText: $('hint-text'),
    challenge: $('challenge-card'), feedback: $('feedback'),
    fbTitle: $('feedback-title'), fbText: $('feedback-text'), fbBtn: $('btn-feedback'),
    error: $('error-box'),
    endPanel: $('end-panel'), endIcon: $('end-icon'), endTitle: $('end-title'),
    endSub: $('end-sub'), endText: $('end-text')
  };

  let selected = null;
  let pending = null;     // response held back while the player reads feedback
  let busy = false;

  function show(which) {
    ['briefing', 'play', 'end'].forEach(function (k) { el[k].classList.toggle('hidden', k !== which); });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  async function call(path, method, body) {
    const res = await fetch(api + path, {
      method: method,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined
    });
    if (!res.ok) throw new Error('Request failed (' + res.status + ')');
    return res.json();
  }

  function showError(msg) {
    el.error.textContent = msg;
    el.error.classList.remove('hidden');
  }

  /* ---------- rendering ---------- */
  function renderLives(lives, max) {
    el.lives.innerHTML = '';
    for (let i = 0; i < max; i++) {
      const s = document.createElement('span');
      s.className = 'life' + (i >= lives ? ' is-lost' : '');
      s.textContent = '❤️';
      s.setAttribute('aria-hidden', 'true');
      el.lives.appendChild(s);
    }
    el.lives.setAttribute('aria-label', lives + ' of ' + max + ' lives left');
  }

  function renderChallenge(r) {
    const c = r.challenge;
    selected = null;
    el.submit.disabled = true;
    el.count.textContent = r.challengeNumber + ' / ' + r.totalChallenges;
    el.bar.style.width = ((r.challengeNumber - 1) / r.totalChallenges * 100) + '%';
    el.title.textContent = c.title;
    el.question.textContent = c.question;
    el.hintText.textContent = c.hint;
    el.hint.classList.add('hidden');
    el.hintBtn.disabled = false;

    el.clues.innerHTML = '';
    c.clues.forEach(function (t) {
      const li = document.createElement('li');
      li.textContent = t;
      el.clues.appendChild(li);
    });

    el.options.innerHTML = '';
    c.options.forEach(function (text, i) {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'option';
      b.setAttribute('role', 'radio');
      b.setAttribute('aria-checked', 'false');
      b.innerHTML = '<span class="option-key">' + String.fromCharCode(65 + i) + '</span><span></span>';
      b.lastChild.textContent = text;
      b.addEventListener('click', function () { choose(i); });
      el.options.appendChild(b);
    });

    el.challenge.classList.remove('hidden');
    el.feedback.classList.add('hidden');
  }

  function choose(i) {
    if (busy) return;
    selected = i;
    Array.prototype.forEach.call(el.options.children, function (b, idx) {
      b.setAttribute('aria-checked', String(idx === i));
    });
    el.submit.disabled = false;
  }

  function showEnd(r) {
    const ok = r.status === 'COMPLETED';
    el.bar.style.width = ok ? '100%' : el.bar.style.width;
    el.endPanel.className = 'card mission-end stack-3 fade-up ' + (ok ? 'is-success' : 'is-failure');
    el.endIcon.textContent = ok ? '✓' : '✕';
    el.endTitle.textContent = ok ? 'Mission Accomplished' : 'Mission Failed';
    el.endSub.textContent = ok ? 'Objective Completed' : 'Objective Failed';
    el.endText.textContent = ok ? root.dataset.winText : root.dataset.loseText;
    show('end');
    window.DER && window.DER.sfx(ok ? 'success' : 'failure');
  }

  function showFeedback(correct, message, btnLabel) {
    el.feedback.className = 'feedback stack-2 fade-up ' + (correct ? 'is-correct' : 'is-wrong');
    el.fbTitle.textContent = (correct ? '✓ Correct' : '✕ Incorrect');
    el.fbText.textContent = message;
    el.fbBtn.textContent = btnLabel;
    el.challenge.classList.add('hidden');
    el.feedback.classList.remove('hidden');
    el.fbBtn.focus();
  }

  /* ---------- actions ---------- */
  async function start() {
    if (busy) return;
    busy = true;
    el.error.classList.add('hidden');
    try {
      const r = await call('/start', 'POST');
      renderLives(r.lives, r.maxLives);
      renderChallenge(r);
      show('play');
    } catch (e) { showError('Could not start the mission. ' + e.message); }
    busy = false;
  }

  async function submit() {
    if (busy || selected === null) return;
    busy = true;
    el.submit.disabled = true;
    el.error.classList.add('hidden');
    try {
      const r = await call('/answer', 'POST', { choice: selected });
      renderLives(r.lives, r.maxLives);
      if (r.correct) {
        window.DER && window.DER.sfx('correct');
        pending = r;
        showFeedback(true, r.message, 'Continue');
      } else {
        window.DER && window.DER.sfx('wrong');
        el.challenge.classList.add('shake');
        if (r.status === 'FAILED') {
          pending = r;
          showFeedback(false, 'No lives remaining.', 'Continue');
        } else {
          pending = null;
          showFeedback(false, 'That wasn\'t the right solution. ' + r.lives + (r.lives === 1 ? ' life' : ' lives') + ' left. Try again.', 'Try Again');
        }
      }
    } catch (e) { showError('Could not submit your answer. ' + e.message); el.submit.disabled = false; }
    busy = false;
  }

  function onFeedbackButton() {
    el.challenge.classList.remove('shake');
    if (pending && pending.status !== 'IN_PROGRESS') { showEnd(pending); pending = null; return; }
    if (pending) { renderChallenge(pending); pending = null; return; }
    /* wrong answer: same challenge again */
    el.feedback.classList.add('hidden');
    el.challenge.classList.remove('hidden');
    selected = null;
    el.submit.disabled = true;
    Array.prototype.forEach.call(el.options.children, function (b) { b.setAttribute('aria-checked', 'false'); });
  }

  $('btn-start').addEventListener('click', start);
  $('btn-restart').addEventListener('click', start);
  el.submit.addEventListener('click', submit);
  el.fbBtn.addEventListener('click', onFeedbackButton);
  el.hintBtn.addEventListener('click', function () {
    el.hint.classList.remove('hidden');
    el.hintBtn.disabled = true;
  });

  /* keyboard: 1-4 choose, Enter submits */
  document.addEventListener('keydown', function (e) {
    if (el.play.classList.contains('hidden') || el.challenge.classList.contains('hidden')) return;
    const n = parseInt(e.key, 10);
    if (n >= 1 && n <= el.options.children.length) choose(n - 1);
    else if (e.key === 'Enter' && selected !== null && document.activeElement.tagName !== 'BUTTON') submit();
  });
})();
