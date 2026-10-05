// Which ranking system(s) the championships of the displayed categories use.
// Medal highlighting is a board choice (see MEDAL_HIGHLIGHT_BOARDS), not a session choice.
export const SCENARIOS = Object.freeze({
  masters: 'W35Q — ranked by Q-Masters score',
  total: 'F 64 — ranked by Total',
  scores: 'W35Q + U15 — ranked by Q-Masters and GAMX-U',
  mixed: 'F 64 + W35Q — ranked by Total and Q-Masters'
});

// Snatch/C&J rank columns follow the medal policy: shown when at least one displayed
// championship awards lift medals. A score medal system forces a Total-only policy, so only
// the F 64 (Total, three medals) championship brings them; in the mix it brings them for all rows.
export const LIFT_RANK_SCENARIOS = Object.freeze(['total', 'mixed']);

// Scoreboard layouts under test. Each entry maps to a production renderer:
//   simple          Results (normal/public scoreboard) — never highlights medals
//   ranking         ResultsRankingOrder with showMedals=false (or auto, category in progress)
//   ranking-medals  ResultsRankingOrder with showMedals=true (or auto, category done)
//   multi           ResultsMulti — never highlights medals
//   medals          ResultsMedals — always highlights medals
export const BOARDS = Object.freeze({
  'simple': 'Simple scoreboard — without medals',
  'ranking': 'Ranking scoreboard — without medals',
  'ranking-medals': 'Ranking scoreboard — with medals',
  'multi': 'Multi-category scoreboard — without medals',
  'medals': 'Medals board — with medals'
});

const BOARD_RENDERERS = Object.freeze({
  'simple': {tag: 'results-template', rankingOrder: false, highlight: false},
  'ranking': {tag: 'results-template', rankingOrder: true, highlight: false},
  'ranking-medals': {tag: 'results-template', rankingOrder: true, highlight: true},
  'multi': {tag: 'resultsfull-template', rankingOrder: false, highlight: false},
  'medals': {tag: 'resultsmedals-template', rankingOrder: false, highlight: true}
});

// Production: normal scoreboards default to css/nogrid, public scoreboards use css/public.
export const THEMES = Object.freeze(['nogrid', 'public']);
export const THEME_LABELS = Object.freeze({
  nogrid: 'Normal scoreboard (nogrid)',
  public: 'Public scoreboard (public)'
});
export const MEDAL_HIGHLIGHT_BOARDS = Object.freeze(
  Object.keys(BOARD_RENDERERS).filter(board => BOARD_RENDERERS[board].highlight));
export const APPEARANCES = Object.freeze(['dark', 'light']);

export function buildFixture({
  scenario = 'masters',
  board = 'simple',
  theme = 'nogrid',
  appearance = 'dark',
  bestScore = false,
  bestRank = false,
  bodyWeight = false,
  categoryHeaders = false
} = {}) {
  if (!Object.hasOwn(SCENARIOS, scenario) || !Object.hasOwn(BOARDS, board)
      || !THEMES.includes(theme) || !APPEARANCES.includes(appearance)) {
    throw new Error('Unknown scoreboard fixture configuration');
  }
  const renderer = BOARD_RENDERERS[board];
  const separateCategories = scenario === 'mixed' || scenario === 'scores';
  const multi = board === 'multi' && separateCategories;
  const categories = scenario === 'mixed' ? ['F 64', 'W35Q 64']
    : scenario === 'scores' ? ['W35Q 64', 'U15 F 64']
    : scenario === 'masters' ? ['W35Q 64', 'W35Q 64'] : ['F 64', 'F 64'];
  const names = scenario === 'mixed' ? ['Total athlete', 'Q-Masters athlete']
    : scenario === 'scores' ? ['Q-Masters athlete', 'GAMX-U athlete']
    : scenario === 'masters' ? ['Q-Masters athlete A', 'Q-Masters athlete B']
    : ['Total athlete A', 'Total athlete B'];

  const athleteRows = names.map((fullName, index) => {
    const weightMedals = scenario === 'total' || (scenario === 'mixed' && index === 0);
    const medalRank = separateCategories ? '1' : scenario === 'total' ? String(2 - index) : String(index + 1);
    const snatchRank = separateCategories ? '1' : String(index + 1);
    const cleanJerkRank = scenario === 'masters' ? String(2 - index) : snatchRank;
    const ranks = value => multi ? [index === 0 ? value : '', index === 1 ? value : ''] : [value];
    return {
      key: String(index + 1),
      fullName,
      startNumber: index + 1,
      category: categories[index],
      teamName: 'Team',
      // production sends the formatted string (BaseResults.getBodyWeightValue, %.2f)
      bodyWeight: '63.50',
      yearOfBirth: categories[index].startsWith('W35Q') ? '1990'
        : categories[index].startsWith('U15') ? '2012' : '2001',
      categoryMinimumWeight: 0,
      categoryMaximumWeight: categories[index].startsWith('W35Q') ? 999 : 64,
      sattempts: ['80', '85', '90'].map(stringValue => ({stringValue})),
      cattempts: ['100', '105', '110'].map(stringValue => ({stringValue})),
      bestSnatch: '90',
      bestCleanJerk: '110',
      total: '200',
      totalRank: separateCategories ? '1' : weightMedals ? medalRank : String(2 - index),
      medalRank,
      medalScore: weightMedals ? '' : index === 0 ? '257.830' : '249.180',
      medalHighlight: renderer.highlight ? `medal${medalRank}` : '',
      snatchRank,
      cleanJerkRank,
      snatchRanks: ranks(snatchRank),
      cleanJerkRanks: ranks(cleanJerkRank),
      medalRanks: ranks(medalRank),
      sinclair: index === 0 ? '270.234' : '260.123',
      sinclairRank: medalRank
    };
  });
  // ResultsRankings and ResultsMedals both present each category in medal-rank order.
  if (renderer.rankingOrder || renderer.tag === 'resultsmedals-template') {
    athleteRows.sort((left, right) => Number(left.medalRank) - Number(right.medalRank));
  }

  const athletes = [];
  let previousCategory;
  for (const athlete of athleteRows) {
    if (athlete.category !== previousCategory) athletes.push({isSpacer: true});
    athletes.push(athlete);
    previousCategory = athlete.category;
  }
  const medalScoringName = scenario === 'scores' ? 'Score' : 'Q-Masters';
  const expected = `Total; ${scenario === 'total' ? 'no medal score' : medalScoringName}; `
    + (separateCategories ? 'one athlete per category, medal ranks 1 and 1' : 'medal ranks 1 and 2')
    + (scenario === 'mixed' ? '; F 64 score blank; W35Q range 0-999 kg' : '');
  return {
    options: {scenario, board, theme, appearance, bestScore, bestRank, bodyWeight, categoryHeaders},
    title: `Fixture: ${BOARDS[board]} — ${SCENARIOS[scenario]}`,
    tag: renderer.tag,
    rankingOrder: renderer.rankingOrder,
    medalHighlight: renderer.highlight,
    athleteRows,
    expected,
    properties: {
      stylesDir: `css/${theme}`,
      mode: 'CURRENT_ATHLETE',
      darkMode: appearance,
      teamWidthClass: 'narrowTeams',
      showTotal: true,
      showBest: true,
      showTotalRank: true,
      showLiftRanks: LIFT_RANK_SCENARIOS.includes(scenario),
      showMedalScore: scenario !== 'total',
      showSinclair: bestScore,
      showSinclairRank: bestRank,
      showBodyWeight: bodyWeight,
      showCategoryHeaders: renderer.rankingOrder || categoryHeaders,
      showRecords: false,
      showLeaders: false,
      leaderFillerHeight: '--leaderFillerHeight: 0px',
      leadersLineHeight: '--leaderLineHeight: min-content',
      // Mirrors BaseResults.setBottomSize(1) for a no-leaders display.
      leaderLines: theme === 'nogrid' ? 1 : 0,
      resultLines: athletes.length + 1,
      nbRanks: multi ? 2 : 1,
      ageGroups: multi ? categories : [categories[0]],
      competitionName: `Rendering fixture: ${SCENARIOS[scenario]}`,
      medalScoringName,
      scoringName: scenario === 'total' ? 'GAMX' : scenario === 'masters' ? 'GAMX-M' : 'Score',
      sizeOverride: '--tableFontSize:1.25rem;',
      t: {
        Start: 'Start', Name: 'Name', Category: 'Category', Birth: 'Birth', Team: 'Team',
        BodyWeight: 'B.W.',
        Snatch: 'Snatch', Clean_and_Jerk: 'Clean & Jerk', Total: 'Total', Rank: 'Rank',
        Best: 'Best', ScoringTitle: 'Score', Custom1: '', Custom2: ''
      },
      athletes,
      medalCategories: [...new Set(categories)].map(category => ({
        categoryName: category,
        rankingTitle: 'Rank',
        scoreScoringTitle: category.startsWith('W35Q') ? 'GAMX-M'
          : category.startsWith('U15') ? 'GAMX-U' : 'GAMX',
        scoreRankingTitle: 'Rank',
        leaders: athleteRows.filter(row => row.category === category),
        categoryDone: true
      }))
    }
  };
}
