import {SCENARIOS, BOARDS, THEMES, APPEARANCES} from './fixtures.mjs';
import {renderFixture, readControls, setControls} from './app.mjs';

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function visible(element) {
  assert(element, 'Expected rendered cell is missing');
  const style = getComputedStyle(element);
  return style.visibility !== 'hidden' && style.display !== 'none';
}

function texts(elements) {
  return [...elements].map(element => element.textContent.trim()).filter(Boolean);
}

export async function checkCase(options) {
  const fixture = await renderFixture(options);
  await new Promise(resolve => setTimeout(resolve, 0));
  assert(innerWidth === 1920 && innerHeight === 1080, 'Set the Playwright viewport to 1920x1080 first');
  const host = document.getElementById('fixture').firstElementChild;
  const root = host.shadowRoot;
  assert(document.title === fixture.title, 'Incorrect browser tab title');
  assert(host.hasAttribute('ranking-order') === fixture.rankingOrder, 'Incorrect regular/ranking-order host mode');
  assert(host.showCategoryHeaders === fixture.properties.showCategoryHeaders,
    'Incorrect category-header mode for regular/ranking scoreboard');
  assert(root.querySelector('.wrapper').classList.contains(fixture.options.appearance), 'Missing dark/light wrapper flag');
  const rows = [...root.querySelectorAll('tr.athlete')];
  assert(rows.length === fixture.athleteRows.length, 'Unexpected athlete row count');
  for (let index = 0; index < rows.length; index++) {
    const row = rows[index];
    const athlete = fixture.athleteRows[index];
    assert(row.querySelector('.category').textContent.trim() === athlete.category, 'Registration categories differ');
    assert(row.querySelector('.total').textContent.trim() === athlete.total, 'Kg total differs');
    const liftRanks = texts(row.querySelectorAll('td.rank'));
    assert(JSON.stringify(liftRanks) === JSON.stringify([athlete.snatchRank, athlete.cleanJerkRank]), 'Incorrect kg lift ranks');
    assert([...row.querySelectorAll('td.rank')].every(cell => visible(cell) === fixture.properties.showLiftRanks),
      fixture.properties.showLiftRanks ? 'Kg lift rank is hidden' : 'Lift rank shown although no displayed championship awards lift medals');
    assert(JSON.stringify(texts(row.querySelectorAll('td.totalRank'))) === JSON.stringify([athlete.medalRank]), 'Incorrect medal rank');
    assert([...row.querySelectorAll('td.totalRank')].every(visible), 'Medal rank is hidden');
    const highlighted = [...row.querySelectorAll('td.totalRank')]
      .some(cell => [...cell.classList].some(name => /^medal\d$/.test(name)));
    assert(highlighted === fixture.medalHighlight,
      highlighted ? 'Without-medals board must not highlight medals' : 'With-medals board lost medal highlight');
    const scoreCells = row.querySelectorAll('td.medalScore');
    assert(scoreCells.length === 1, 'Expected one shared medal score cell per athlete');
    assert(visible(scoreCells[0]) === fixture.properties.showMedalScore, 'Incorrect medal-score column visibility');
    assert(scoreCells[0].textContent.trim() === athlete.medalScore, 'Incorrect medal score or nonblank Total-medal score');
    assert(visible(row.querySelector('td.sinclair')) === fixture.properties.showSinclair, 'Incorrect best-athlete score visibility');
    assert(visible(row.querySelector('td.sinclairRank')) === fixture.properties.showSinclairRank, 'Incorrect best-athlete rank visibility');
    assert(row.querySelector('td.sinclairRank').textContent.trim() === athlete.sinclairRank, 'Incorrect best-athlete rank');
    const bodyWeightCell = row.querySelector('td.bodyWeight');
    assert(visible(bodyWeightCell) === fixture.properties.showBodyWeight, 'Incorrect body-weight column visibility');
    assert(bodyWeightCell.textContent.trim() === athlete.bodyWeight, 'Incorrect body weight');
    assert(row.querySelector('td.custom1').textContent.trim() === '', 'Body weight must not be written into custom1');
    assert(!visible(row.querySelector('td.custom1')), 'custom1 stays CSS-controlled and hidden by default');
    const height = row.querySelector('td.name').getBoundingClientRect().height;
    assert(height > 0 && height <= 80, `Stretched or collapsed athlete row: ${height}px`);
    const categoryCell = row.querySelector('td.category').getBoundingClientRect();
    const rankCell = row.querySelector('td.totalRank').getBoundingClientRect();
    assert(Math.abs(categoryCell.y - rankCell.y) <= 2, 'Athlete cells wrapped into different grid rows');
  }
  if (fixture.properties.showMedalScore) {
    const header = root.querySelector('th.medalScore');
    assert(header.textContent.trim() === fixture.properties.medalScoringName, 'Incorrect single-system or mixed-system score heading');
  }
  const spacers = [...root.querySelectorAll('td.vspacer, th.vspacer')];
  assert(spacers.length > 0, 'Vertical spacer cells are missing');
  for (const spacer of spacers) {
    const style = getComputedStyle(spacer);
    assert(['borderTopWidth', 'borderRightWidth', 'borderBottomWidth', 'borderLeftWidth']
      .every(side => parseFloat(style[side]) === 0), 'A vertical spacer has visible borders');
  }
  if (fixture.options.theme === 'nogrid') {
    assert(fixture.properties.leaderLines === 1, 'Missing no-grid footer-row count');
    assert(fixture.properties.leaderFillerHeight === '--leaderFillerHeight: 0px', 'Unexpected leaders filler');
  }
  const categories = [...new Set(fixture.athleteRows.map(row => row.category))];
  if (fixture.options.board === 'medals' && categories.length > 1) {
    assert(root.querySelectorAll('.category').length >= fixture.athleteRows.length, 'Missing medal category sections');
  } else if (categories.length > 1) {
    const boundaries = root.querySelectorAll('td.spacer, td.categoryGroupHeader');
    assert(boundaries.length === categories.length, 'Missing category-boundary spacer/header');
  }
  return {scenario: fixture.options.scenario, board: fixture.options.board,
    theme: fixture.options.theme, appearance: fixture.options.appearance,
    bestScore: fixture.options.bestScore, bestRank: fixture.options.bestRank,
    bodyWeight: fixture.options.bodyWeight, passed: true};
}

export async function runMatrix({scenarios = Object.keys(SCENARIOS), boards = Object.keys(BOARDS),
  themes = THEMES, appearances = APPEARANCES,
  bestColumns = [[false, false], [true, false], [false, true], [true, true]],
  bodyWeights = [false, true]} = {}) {
  const original = readControls();
  const results = [];
  try {
    for (const scenario of scenarios) for (const board of boards)
      for (const theme of themes) for (const appearance of appearances)
        for (const [bestScore, bestRank] of bestColumns) for (const bodyWeight of bodyWeights) {
          const options = {scenario, board, theme, appearance, bestScore, bestRank, bodyWeight, categoryHeaders: false};
          try {
            results.push(await checkCase(options));
          } catch (error) {
            throw new Error(`${JSON.stringify(options)}: ${error.message}`, {cause: error});
          }
        }
  } finally {
    setControls(original);
    await window.updateFixture();
  }
  return {passed: results.length, failed: 0, results};
}
