import '/components/Results.js';
import '/components/ResultsMulti.js';
import '/components/ResultsMedals.js';
import {SCENARIOS, BOARDS, THEMES, THEME_LABELS, APPEARANCES, buildFixture} from './fixtures.mjs';

function populate(id, values) {
  const select = document.getElementById(id);
  for (const [value, label] of Object.entries(values)) select.add(new Option(label, value));
}
populate('scenario', SCENARIOS);
populate('component', BOARDS);
populate('theme', Object.fromEntries(THEMES.map(value => [value, THEME_LABELS[value]])));
populate('appearance', Object.fromEntries(APPEARANCES.map(value => [value, value])));

let revision = 0;
window.fixtureReady = false;

function stylesheetReady(link) {
  if (link.sheet) return Promise.resolve();
  return new Promise((resolve, reject) => {
    link.addEventListener('load', resolve, {once: true});
    link.addEventListener('error', () => reject(new Error(`Stylesheet failed: ${link.href}`)), {once: true});
  });
}

// requestAnimationFrame never fires in a background tab; fall back to a timer so
// fixtureReady still becomes true on tabs that are not currently visible.
function nextPaint() {
  return new Promise(resolve => {
    const timer = setTimeout(resolve, 100);
    requestAnimationFrame(() => { clearTimeout(timer); resolve(); });
  });
}

export async function renderFixture(options) {
  const current = ++revision;
  const fixture = buildFixture(options);
  window.fixtureReady = false;
  const element = document.createElement(fixture.tag);
  if (fixture.rankingOrder) element.setAttribute('ranking-order', 'true');
  Object.assign(element, fixture.properties);
  document.getElementById('fixture').replaceChildren(element);
  await element.updateComplete;
  await Promise.all([...element.shadowRoot.querySelectorAll('link')].map(stylesheetReady));
  await nextPaint();
  if (current === revision) {
    document.title = fixture.title;
    document.getElementById('expected').textContent = `Expected: ${fixture.expected}.`;
    document.getElementById('status').textContent = '';
    window.fixtureState = fixture;
    window.fixtureReady = true;
  }
  return fixture;
}

export function readControls() {
  return {
    scenario: document.getElementById('scenario').value,
    board: document.getElementById('component').value,
    theme: document.getElementById('theme').value,
    appearance: document.getElementById('appearance').value,
    bestScore: document.getElementById('best-score').checked,
    bestRank: document.getElementById('best-rank').checked,
    categoryHeaders: document.getElementById('category-headers').checked
  };
}

export function setControls(options) {
  const ids = {scenario: 'scenario', board: 'component', theme: 'theme', appearance: 'appearance'};
  for (const [key, id] of Object.entries(ids)) document.getElementById(id).value = options[key];
  for (const [key, id] of [['bestScore', 'best-score'], ['bestRank', 'best-rank'],
    ['categoryHeaders', 'category-headers']]) {
    document.getElementById(id).checked = options[key] === true;
  }
}

window.updateFixture = () => renderFixture(readControls());
window.renderFixture = renderFixture;
window.setFixtureControls = setControls;
document.querySelectorAll('#controls select, #controls input').forEach(control =>
  control.addEventListener('change', () => window.updateFixture().catch(reportError)));

window.setFixtureControlsVisible = visible => {
  document.getElementById('controls').style.display = visible ? 'flex' : 'none';
  document.getElementById('show-controls').style.display = visible ? 'none' : 'block';
};
document.getElementById('hide-controls').addEventListener('click', () => window.setFixtureControlsVisible(false));
document.getElementById('show-controls').addEventListener('click', () => window.setFixtureControlsVisible(true));

function reportError(error) {
  window.fixtureReady = false;
  document.getElementById('status').textContent = error.message;
  console.error(error);
}

const params = new URLSearchParams(location.search);
setControls({
  scenario: params.get('scenario') || 'masters',
  board: params.get('board') || 'simple',
  theme: params.get('theme') || 'nogrid',
  appearance: params.get('appearance') || 'dark',
  bestScore: params.get('bestScore') === 'true',
  bestRank: params.get('bestRank') === 'true',
  categoryHeaders: params.get('categoryHeaders') === 'true'
});
try {
  await window.updateFixture();
} catch (error) {
  reportError(error);
}
