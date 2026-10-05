import test from 'node:test';
import assert from 'node:assert/strict';
import {SCENARIOS, BOARDS, THEMES, APPEARANCES, MEDAL_HIGHLIGHT_BOARDS, LIFT_RANK_SCENARIOS, buildFixture} from './fixtures.mjs';

test('all supported cases have complete production layout properties', () => {
  for (const scenario of Object.keys(SCENARIOS)) for (const board of Object.keys(BOARDS))
    for (const theme of THEMES) for (const appearance of APPEARANCES) {
      const fixture = buildFixture({scenario, board, theme, appearance});
      assert.equal(fixture.properties.showLiftRanks, LIFT_RANK_SCENARIOS.includes(scenario));
      assert.equal(fixture.properties.darkMode, appearance);
      assert.equal(fixture.properties.leaderFillerHeight, '--leaderFillerHeight: 0px');
      assert.equal(fixture.properties.leaderLines, theme === 'nogrid' ? 1 : 0);
      assert.equal(fixture.properties.resultLines, fixture.properties.athletes.length + 1);
      assert.equal(fixture.properties.showSinclair, false);
      assert.equal(fixture.properties.showSinclairRank, false);
      assert.equal(fixture.athleteRows.length, 2);
    }
});

test('mixed medal systems are separate registration categories, each ranked first', () => {
  for (const scenario of ['mixed', 'scores']) {
    const fixture = buildFixture({scenario});
    assert.equal(new Set(fixture.athleteRows.map(a => a.key)).size, 2);
    assert.equal(new Set(fixture.athleteRows.map(a => a.category)).size, 2);
    assert.deepEqual(fixture.athleteRows.map(a => a.medalRank), ['1', '1']);
    assert.equal(fixture.properties.athletes.filter(a => a.isSpacer).length, 2);
    const masters = fixture.athleteRows.find(a => a.category === 'W35Q 64');
    assert.equal(masters.categoryMinimumWeight, 0);
    assert.equal(masters.categoryMaximumWeight, 999);
  }
  assert.equal(buildFixture({scenario: 'mixed'}).athleteRows[0].medalScore, '');
  assert.equal(buildFixture({scenario: 'scores'}).properties.medalScoringName, 'Score');
});

test('score medals can differ from kg ranks; kg ranks are sent even when their columns are hidden', () => {
  const fixture = buildFixture({scenario: 'masters'});
  assert.deepEqual(fixture.athleteRows.map(a => a.medalRank), ['1', '2']);
  assert.deepEqual(fixture.athleteRows.map(a => a.totalRank), ['2', '1']);
  assert.deepEqual(fixture.athleteRows.map(a => a.cleanJerkRank), ['2', '1']);
  assert.equal(fixture.properties.showLiftRanks, false);
});

test('lift rank columns follow the medal policy: three-medal Total brings them, also in the mix', () => {
  assert.deepEqual([...LIFT_RANK_SCENARIOS].sort(), ['mixed', 'total']);
  assert.equal(buildFixture({scenario: 'total'}).properties.showLiftRanks, true);
  assert.equal(buildFixture({scenario: 'mixed'}).properties.showLiftRanks, true);
  assert.equal(buildFixture({scenario: 'masters'}).properties.showLiftRanks, false);
  assert.equal(buildFixture({scenario: 'scores'}).properties.showLiftRanks, false);
});

test('ranking scoreboards use the real ResultsRankingOrder renderer, with and without medals', () => {
  for (const [board, highlight] of [['ranking', false], ['ranking-medals', true]]) {
    const fixture = buildFixture({scenario: 'total', board});
    assert.equal(fixture.tag, 'results-template');
    assert.equal(fixture.rankingOrder, true);
    assert.equal(fixture.medalHighlight, highlight);
    assert.equal(fixture.properties.showCategoryHeaders, true);
    assert.deepEqual(fixture.athleteRows.map(athlete => athlete.startNumber), [2, 1]);
    assert.deepEqual(fixture.athleteRows.map(athlete => athlete.medalRank), ['1', '2']);
    assert.match(fixture.title, highlight ? /Ranking scoreboard — with medals/ : /Ranking scoreboard — without medals/);
  }
});

test('simple scoreboard uses the regular Results renderer, never ranking mode nor medals', () => {
  const fixture = buildFixture({scenario: 'total', board: 'simple'});
  assert.equal(fixture.tag, 'results-template');
  assert.equal(fixture.rankingOrder, false);
  assert.equal(fixture.medalHighlight, false);
  assert.equal(fixture.properties.showCategoryHeaders, false);
  assert.deepEqual(fixture.athleteRows.map(athlete => athlete.startNumber), [1, 2]);
  assert.deepEqual(fixture.athleteRows.map(athlete => athlete.medalRank), ['2', '1']);
  assert.match(fixture.title, /Simple scoreboard — without medals/);
});

test('medals board lists each category in medal-rank order', () => {
  const fixture = buildFixture({scenario: 'total', board: 'medals'});
  assert.deepEqual(fixture.athleteRows.map(athlete => athlete.medalRank), ['1', '2']);
  assert.deepEqual(fixture.properties.medalCategories[0].leaders.map(athlete => athlete.medalRank), ['1', '2']);
});

test('medal highlight is a board property: ranking-medals and medals only', () => {
  assert.deepEqual([...MEDAL_HIGHLIGHT_BOARDS].sort(), ['medals', 'ranking-medals']);
  for (const scenario of Object.keys(SCENARIOS)) for (const board of Object.keys(BOARDS))
    for (const theme of THEMES) {
      const fixture = buildFixture({scenario, board, theme});
      const highlights = fixture.athleteRows.map(athlete => athlete.medalHighlight);
      if (MEDAL_HIGHLIGHT_BOARDS.includes(board)) {
        assert.deepEqual(highlights, fixture.athleteRows.map(athlete => `medal${athlete.medalRank}`));
      } else {
        assert.deepEqual(highlights, ['', '']);
      }
    }
});

test('themes map to production normal (nogrid) and public stylesheets, defaulting to nogrid', () => {
  assert.deepEqual([...THEMES], ['nogrid', 'public']);
  assert.equal(buildFixture().options.theme, 'nogrid');
  assert.equal(buildFixture().properties.stylesDir, 'css/nogrid');
  assert.equal(buildFixture({theme: 'public'}).properties.stylesDir, 'css/public');
});

test('multi-category membership arrays do not rank an athlete in another category', () => {
  const fixture = buildFixture({scenario: 'mixed', board: 'multi'});
  assert.equal(fixture.properties.nbRanks, 2);
  assert.deepEqual(fixture.athleteRows.map(a => a.medalRanks), [['1', ''], ['', '1']]);
});

test('best-athlete score and rank are independent display flags', () => {
  for (const bestScore of [false, true]) for (const bestRank of [false, true]) {
    const fixture = buildFixture({bestScore, bestRank});
    assert.equal(fixture.properties.showSinclair, bestScore);
    assert.equal(fixture.properties.showSinclairRank, bestRank);
  }
});

test('body weight is its own column and never written into custom1', () => {
  for (const bodyWeight of [false, true]) {
    const fixture = buildFixture({bodyWeight});
    assert.equal(fixture.properties.showBodyWeight, bodyWeight);
    for (const athlete of fixture.athleteRows) {
      assert.equal(athlete.bodyWeight, '63.50');
      assert.equal(athlete.custom1, undefined);
    }
  }
  assert.equal(buildFixture().properties.t.BodyWeight, 'B.W.');
});

test('unknown scenarios and layout options fail explicitly', () => {
  assert.throws(() => buildFixture({scenario: 'unknown'}));
  assert.throws(() => buildFixture({board: 'resultsrankings-template'}));
  assert.throws(() => buildFixture({theme: 'unknown'}));
});
