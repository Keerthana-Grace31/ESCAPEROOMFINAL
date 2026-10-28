/* Time Travel quiz: the server owns the puzzles and the answers. */
(function () {
  'use strict';

  var body = document.body;
  var scenario = parseInt(body.dataset.scenario, 10);
  var api = body.dataset.api;
  var soundBase = body.dataset.soundBase || '/sounds';
  var MAX_LIVES = 3;

  var lives, period, question, total;
  var $ = function (id) { return document.getElementById(id); };

  function sfx(name) {
    if (window.BGM && window.BGM.sfx) { window.BGM.sfx(name); return; }   // levels + toggle: bgm.js
    try {
      var a = new Audio(soundBase + '/common/' + name + '.mp3');
      var p = a.play();
      if (p && p.catch) p.catch(function () { /* autoplay blocked */ });
    } catch (e) { /* ignore */ }
  }

  function show(id, on) { $(id).hidden = !on; }
  function feedback(text, cls) {
    var f = $('feedback');
    f.textContent = text || '';
    f.className = 'quiz-feedback' + (cls ? ' ' + cls : '');
  }

  function renderLives() { $('lives').textContent = String(lives); }

  function loadPuzzle() {
    var url = api + '/puzzle?scenario=' + scenario + '&period=' + period + '&question=' + question;
    return fetch(url).then(function (r) {
      if (!r.ok) throw new Error('Could not load puzzle');
      return r.json();
    }).then(function (p) {
      total = p.total;
      $('puzzleText').textContent = p.question;
      $('progress').textContent = p.number + ' / ' + p.total;
      $('answerInput').value = '';
      $('answerInput').focus();
      feedback('');
    }).catch(function (e) { feedback(e.message, 'bad'); });
  }

  function start(chosen) {
    period = chosen; question = 1; lives = MAX_LIVES;
    $('era').textContent = period;
    renderLives();
    show('periodPicker', false); show('endCard', false); show('puzzleCard', true);
    loadPuzzle();
  }

  function end(won) {
    show('puzzleCard', false); show('endCard', true);
    $('endTitle').textContent = won ? 'TIMELINE STABILISED' : 'TIMELINE COLLAPSED';
    $('endText').textContent = won
      ? 'You solved every puzzle in the ' + period + '. Try another era!'
      : 'You ran out of lives in the ' + period + '. Try again.';
    sfx(won ? 'success' : 'failure');
  }

  function submit() {
    var answer = $('answerInput').value.trim();
    if (!answer) { feedback('Enter an answer first.', 'hint'); return; }
    fetch(api + '/answer', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ scenario: scenario, period: period, question: question, answer: answer })
    }).then(function (r) {
      if (!r.ok) throw new Error('Could not check answer');
      return r.json();
    }).then(function (res) {
      if (res.correct) {
        sfx('correct');
        if (question >= total) { end(true); return; }
        question++;
        loadPuzzle().then(function () { feedback(res.message, 'ok'); });
      } else {
        sfx('wrong');
        lives--; renderLives();
        if (lives <= 0) { end(false); } else { feedback(res.message + ' Lives left: ' + lives, 'bad'); }
      }
    }).catch(function (e) { feedback(e.message, 'bad'); });
  }

  function hint() {
    var url = api + '/puzzle?scenario=' + scenario + '&period=' + period + '&question=' + question;
    fetch(url).then(function (r) { return r.json(); })
      .then(function (p) { feedback('Hint: ' + p.hint, 'hint'); });
  }

  document.querySelectorAll('[data-period]').forEach(function (b) {
    b.addEventListener('click', function () { sfx('click'); start(b.dataset.period); });
  });
  $('submitBtn').addEventListener('click', function () { sfx('click'); submit(); });
  $('hintBtn').addEventListener('click', function () { sfx('click'); hint(); });
  $('answerInput').addEventListener('keydown', function (e) { if (e.key === 'Enter') submit(); });
  $('againBtn').addEventListener('click', function () {
    sfx('click'); show('endCard', false); show('periodPicker', true); $('era').textContent = '\u2014'; $('progress').textContent = '\u2014';
  });
})();
