const fs = require('fs');
const path = require('path');

const IOC_URL = 'https://en.wikipedia.org/w/index.php?title=List_of_IOC_country_codes&action=render';
const COMPARISON_URL = 'https://en.wikipedia.org/w/index.php?title=Comparison_of_alphabetic_country_codes&action=render';
const ISO_RAW_URL = 'https://en.wikipedia.org/w/index.php?title=ISO_3166-1_alpha-3&action=raw';

const OUTPUT_PATH = path.join(__dirname, '..', 'shared', 'src', 'main', 'resources', 'mappings', 'country-code-map.json');

const IOC_NAME_OVERRIDES = {
  GBR: 'Great Britain',
  TPE: 'Chinese Taipei'
};

const MANUAL_ALIASES = {
  GBR: [
    'United Kingdom',
    'United Kingdom of Great Britain and Northern Ireland',
    'Great Britain and Northern Ireland'
  ],
  TPE: [
    'Taiwan',
    'Taiwan, Province of China',
    'Republic of China'
  ],
  CHN: ['People\'s Republic of China', 'China, People\'s Republic of'],
  HKG: ['Hong Kong, China'],
  MAC: ['Macau', 'Macao', 'Macau, China', 'Macao, China'],
  PRK: ['Democratic People\'s Republic of Korea', 'Korea, Democratic People\'s Republic of', 'DPRK', 'North Korea'],
  KOR: ['Republic of Korea', 'Korea, Republic of', 'South Korea'],
  COD: ['DR Congo', 'Congo DR', 'Congo, Democratic Republic of the'],
  CGO: ['Congo', 'Republic of the Congo'],
  ISV: ['United States Virgin Islands', 'U.S. Virgin Islands'],
  IVB: ['Virgin Islands (British)'],
  VIE: ['Vietnam', 'Viet Nam'],
  CIV: ['Ivory Coast', 'Cote d\'Ivoire', 'Côte d\'Ivoire'],
  CPV: ['Cape Verde', 'Cabo Verde'],
  CZE: ['Czech Republic', 'Czechia'],
  IRI: ['Iran', 'Iran, Islamic Republic of'],
  BOL: ['Bolivia', 'Bolivia, Plurinational State of'],
  VEN: ['Venezuela', 'Venezuela, Bolivarian Republic of'],
  LAO: ['Laos', 'Lao People\'s Democratic Republic'],
  FSM: ['Micronesia', 'Micronesia, Federated States of'],
  MDA: ['Moldova', 'Moldova, Republic of'],
  TLS: ['East Timor', 'Timor-Leste'],
  UAE: ['United Arab Emirates'],
  RSA: ['South Africa'],
  SUD: ['Sudan'],
  SAM: ['Samoa', 'Western Samoa'],
  SWZ: ['Swaziland', 'Eswatini']
};

const SUBDIVISION_ENTRIES = [
  { code: 'AB', country: 'Canada', kind: 'province', name: 'Alberta' },
  { code: 'BC', country: 'Canada', kind: 'province', name: 'British Columbia' },
  { code: 'MB', country: 'Canada', kind: 'province', name: 'Manitoba' },
  { code: 'NB', country: 'Canada', kind: 'province', name: 'New Brunswick' },
  { code: 'NL', country: 'Canada', kind: 'province', name: 'Newfoundland and Labrador' },
  { code: 'NT', country: 'Canada', kind: 'territory', name: 'Northwest Territories' },
  { code: 'NU', country: 'Canada', kind: 'territory', name: 'Nunavut' },
  { code: 'NS', country: 'Canada', kind: 'province', name: 'Nova Scotia' },
  { code: 'ON', country: 'Canada', kind: 'province', name: 'Ontario' },
  { code: 'PE', country: 'Canada', kind: 'province', name: 'Prince Edward Island' },
  { code: 'QC', country: 'Canada', kind: 'province', name: 'Quebec', aliases: ['Québec'] },
  { code: 'SK', country: 'Canada', kind: 'province', name: 'Saskatchewan' },
  { code: 'YT', country: 'Canada', kind: 'territory', name: 'Yukon', aliases: ['Yukon Territory'] },
  { code: 'AL', country: 'United States', kind: 'state', name: 'Alabama' },
  { code: 'AK', country: 'United States', kind: 'state', name: 'Alaska' },
  { code: 'AZ', country: 'United States', kind: 'state', name: 'Arizona' },
  { code: 'AR', country: 'United States', kind: 'state', name: 'Arkansas' },
  { code: 'CA', country: 'United States', kind: 'state', name: 'California' },
  { code: 'CO', country: 'United States', kind: 'state', name: 'Colorado' },
  { code: 'CT', country: 'United States', kind: 'state', name: 'Connecticut' },
  { code: 'DC', country: 'United States', kind: 'district', name: 'District of Columbia', aliases: ['Washington, DC', 'Washington DC'] },
  { code: 'DE', country: 'United States', kind: 'state', name: 'Delaware' },
  { code: 'FL', country: 'United States', kind: 'state', name: 'Florida' },
  { code: 'GA', country: 'United States', kind: 'state', name: 'Georgia', aliases: ['Georgia (US)', 'Georgia State', 'State of Georgia'] },
  { code: 'HI', country: 'United States', kind: 'state', name: 'Hawaii' },
  { code: 'ID', country: 'United States', kind: 'state', name: 'Idaho' },
  { code: 'IL', country: 'United States', kind: 'state', name: 'Illinois' },
  { code: 'IN', country: 'United States', kind: 'state', name: 'Indiana' },
  { code: 'IA', country: 'United States', kind: 'state', name: 'Iowa' },
  { code: 'KS', country: 'United States', kind: 'state', name: 'Kansas' },
  { code: 'KY', country: 'United States', kind: 'state', name: 'Kentucky' },
  { code: 'LA', country: 'United States', kind: 'state', name: 'Louisiana' },
  { code: 'ME', country: 'United States', kind: 'state', name: 'Maine' },
  { code: 'MD', country: 'United States', kind: 'state', name: 'Maryland' },
  { code: 'MA', country: 'United States', kind: 'state', name: 'Massachusetts' },
  { code: 'MI', country: 'United States', kind: 'state', name: 'Michigan' },
  { code: 'MN', country: 'United States', kind: 'state', name: 'Minnesota' },
  { code: 'MS', country: 'United States', kind: 'state', name: 'Mississippi' },
  { code: 'MO', country: 'United States', kind: 'state', name: 'Missouri' },
  { code: 'MT', country: 'United States', kind: 'state', name: 'Montana' },
  { code: 'NE', country: 'United States', kind: 'state', name: 'Nebraska' },
  { code: 'NV', country: 'United States', kind: 'state', name: 'Nevada' },
  { code: 'NH', country: 'United States', kind: 'state', name: 'New Hampshire' },
  { code: 'NJ', country: 'United States', kind: 'state', name: 'New Jersey' },
  { code: 'NM', country: 'United States', kind: 'state', name: 'New Mexico' },
  { code: 'NY', country: 'United States', kind: 'state', name: 'New York' },
  { code: 'NC', country: 'United States', kind: 'state', name: 'North Carolina' },
  { code: 'ND', country: 'United States', kind: 'state', name: 'North Dakota' },
  { code: 'OH', country: 'United States', kind: 'state', name: 'Ohio' },
  { code: 'OK', country: 'United States', kind: 'state', name: 'Oklahoma' },
  { code: 'OR', country: 'United States', kind: 'state', name: 'Oregon' },
  { code: 'PA', country: 'United States', kind: 'state', name: 'Pennsylvania' },
  { code: 'RI', country: 'United States', kind: 'state', name: 'Rhode Island' },
  { code: 'SC', country: 'United States', kind: 'state', name: 'South Carolina' },
  { code: 'SD', country: 'United States', kind: 'state', name: 'South Dakota' },
  { code: 'TN', country: 'United States', kind: 'state', name: 'Tennessee' },
  { code: 'TX', country: 'United States', kind: 'state', name: 'Texas' },
  { code: 'UT', country: 'United States', kind: 'state', name: 'Utah' },
  { code: 'VT', country: 'United States', kind: 'state', name: 'Vermont' },
  { code: 'VA', country: 'United States', kind: 'state', name: 'Virginia' },
  { code: 'WA', country: 'United States', kind: 'state', name: 'Washington' },
  { code: 'WV', country: 'United States', kind: 'state', name: 'West Virginia' },
  { code: 'WI', country: 'United States', kind: 'state', name: 'Wisconsin' },
  { code: 'WY', country: 'United States', kind: 'state', name: 'Wyoming' }
];

function decodeEntities(text) {
  if (!text) return '';

  const named = {
    amp: '&',
    apos: "'",
    nbsp: ' ',
    quot: '"',
    lt: '<',
    gt: '>',
    ndash: '-',
    mdash: '-',
    hellip: '...'
  };

  return text
    .replace(/&#(\d+);/g, (_, dec) => String.fromCodePoint(Number(dec)))
    .replace(/&#x([0-9a-fA-F]+);/g, (_, hex) => String.fromCodePoint(parseInt(hex, 16)))
    .replace(/&([a-zA-Z]+);/g, (match, name) => (name in named ? named[name] : match));
}

function stripHtml(html) {
  if (!html) return '';

  return decodeEntities(
    html
      .replace(/<!--([\s\S]*?)-->/g, ' ')
      .replace(/<sup[\s\S]*?<\/sup>/gi, ' ')
      .replace(/<style[\s\S]*?<\/style>/gi, ' ')
      .replace(/<script[\s\S]*?<\/script>/gi, ' ')
      .replace(/<br\s*\/?>/gi, ' / ')
      .replace(/<hr\s*\/?>/gi, ' / ')
      .replace(/<[^>]+>/g, ' ')
  )
    .replace(/[\u200b-\u200d\ufeff]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

function normalizeKey(value) {
  return stripHtml(value)
    .normalize('NFKD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/['’]/g, '')
    .replace(/&/g, ' and ')
    .replace(/\([^)]*\)/g, ' ')
    .replace(/[^a-zA-Z0-9]+/g, ' ')
    .trim()
    .toLowerCase();
}

function collapseRepeatedName(text) {
  const cleaned = stripHtml(text);
  const match = cleaned.match(/^(.+?)\s+\1$/);
  return match ? match[1] : cleaned;
}

function cleanCountryName(text) {
  return collapseRepeatedName(text)
    .replace(/^The\s+/i, 'The ')
    .replace(/^People's Republic of China$/i, 'China')
    .trim();
}

function findSection(html, startIdPattern, endIdPattern) {
  const startRegex = new RegExp(startIdPattern, 'i');
  const endRegex = new RegExp(endIdPattern, 'i');
  const startIndex = html.search(startRegex);
  if (startIndex === -1) {
    throw new Error(`Could not find section start: ${startIdPattern}`);
  }

  const sliced = html.slice(startIndex);
  const endIndex = sliced.search(endRegex);
  return endIndex === -1 ? sliced : sliced.slice(0, endIndex);
}

function extractFirstTable(sectionHtml) {
  const tableMatch = sectionHtml.match(/<table[\s\S]*?<\/table>/i);
  if (!tableMatch) {
    throw new Error('Could not find expected table in section');
  }
  return tableMatch[0];
}

function extractAllRows(tableHtml) {
  return Array.from(tableHtml.matchAll(/<tr[\s\S]*?<\/tr>/gi)).map(match => match[0]);
}

function extractCells(rowHtml) {
  return Array.from(rowHtml.matchAll(/<t[dh][^>]*>([\s\S]*?)<\/t[dh]>/gi)).map(match => match[1]);
}

function parseIocTable(html) {
  const section = findSection(html, 'id="Current_NOCs"', 'id="Current_NPCs"|id="Historic_NOCs');
  const table = extractFirstTable(section);
  const rows = extractAllRows(table);
  const result = new Map();

  for (const row of rows) {
    const cells = extractCells(row);
    if (cells.length < 2) continue;

    const code = stripHtml(cells[0]).replace(/[^A-Z0-9]/g, '');
    if (!/^[A-Z0-9]{3}$/.test(code)) continue;

    const rawName = cleanCountryName(cells[1]);
    const name = IOC_NAME_OVERRIDES[code] || rawName;
    if (!name) continue;

    result.set(code, name);
  }

  return result;
}

function parseComparisonTable(html) {
  const section = findSection(html, 'id="List"', 'id="Former_countries');
  const table = extractFirstTable(section);
  const rows = extractAllRows(table);
  const result = [];

  for (const row of rows) {
    const cells = extractCells(row);
    if (cells.length < 5) continue;

    const country = cleanCountryName(cells[1]);
    const ioc = stripHtml(cells[2]).replace(/[^A-Z0-9]/g, '');
    const iso = stripHtml(cells[4]).replace(/[^A-Z0-9]/g, '');

    if (!country || (!ioc && !iso)) continue;

    result.push({
      country,
      ioc: /^[A-Z0-9]{3}$/.test(ioc) ? ioc : null,
      iso: /^[A-Z0-9]{3}$/.test(iso) ? iso : null
    });
  }

  return result;
}

function parseIsoRaw(raw) {
  const startMarker = '===Officially assigned code elements===';
  const endMarker = '===User-assigned code elements===';
  const startIndex = raw.indexOf(startMarker);
  const endIndex = raw.indexOf(endMarker);

  if (startIndex === -1 || endIndex === -1 || endIndex <= startIndex) {
    throw new Error('Could not locate ISO code list in raw page');
  }

  const section = raw.slice(startIndex, endIndex);
  const isoNames = new Map();
  const itemRegex = /^\*\s*\{\{mono\|([A-Z]{3})\}\}[^\[]*\[\[([^\]]+)\]\]/gm;

  let match;
  while ((match = itemRegex.exec(section)) !== null) {
    const code = match[1];
    const wikiTarget = match[2];
    const name = cleanCountryName(wikiTarget.includes('|') ? wikiTarget.split('|').pop() : wikiTarget);
    isoNames.set(code, name);
  }

  return isoNames;
}

function addAlias(aliasSet, value) {
  const cleaned = cleanCountryName(value);
  if (!cleaned) return;
  aliasSet.add(cleaned);
}

function addLookupMapping(exactMap, normalizedMap, ambiguousMap, alias, code) {
  const cleanedAlias = cleanCountryName(alias);
  if (!cleanedAlias || !code) return;

  if (exactMap[cleanedAlias] && exactMap[cleanedAlias] !== code) {
    ambiguousMap[cleanedAlias] = Array.from(new Set([exactMap[cleanedAlias], code])).sort();
  } else if (!exactMap[cleanedAlias]) {
    exactMap[cleanedAlias] = code;
  }

  const normalizedAlias = normalizeKey(cleanedAlias);
  if (!normalizedAlias) return;

  if (normalizedMap[normalizedAlias] && normalizedMap[normalizedAlias] !== code) {
    ambiguousMap[cleanedAlias] = Array.from(new Set([normalizedMap[normalizedAlias], code])).sort();
  } else if (!normalizedMap[normalizedAlias]) {
    normalizedMap[normalizedAlias] = code;
  }
}

async function fetchText(url) {
  const response = await fetch(url, {
    headers: {
      'user-agent': 'GitHub-Copilot-country-code-map-generator/1.0'
    }
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch ${url}: ${response.status} ${response.statusText}`);
  }

  return response.text();
}

async function main() {
  const [iocHtml, comparisonHtml, isoRaw] = await Promise.all([
    fetchText(IOC_URL),
    fetchText(COMPARISON_URL),
    fetchText(ISO_RAW_URL)
  ]);

  const iocByCode = parseIocTable(iocHtml);
  const comparisonEntries = parseComparisonTable(comparisonHtml);
  const isoNamesByCode = parseIsoRaw(isoRaw);

  const entriesByIoc = new Map();

  for (const [iocCode, canonicalName] of iocByCode.entries()) {
    entriesByIoc.set(iocCode, {
      country: canonicalName,
      ioc: iocCode,
      isoAlpha3: null,
      aliases: new Set([canonicalName])
    });
  }

  for (const row of comparisonEntries) {
    if (!row.ioc) continue;
    const entry = entriesByIoc.get(row.ioc);
    if (!entry) continue;

    if (row.iso) {
      entry.isoAlpha3 = row.iso;
    }

    addAlias(entry.aliases, row.country);
  }

  for (const entry of entriesByIoc.values()) {
    if (entry.isoAlpha3 && isoNamesByCode.has(entry.isoAlpha3)) {
      addAlias(entry.aliases, isoNamesByCode.get(entry.isoAlpha3));
    }

    const extraAliases = MANUAL_ALIASES[entry.ioc] || [];
    for (const alias of extraAliases) {
      addAlias(entry.aliases, alias);
    }
  }

  const entries = Array.from(entriesByIoc.values())
    .map(entry => ({
      country: entry.country,
      ioc: entry.ioc,
      isoAlpha3: entry.isoAlpha3,
      aliases: Array.from(entry.aliases)
        .filter(alias => alias !== entry.country)
        .sort((a, b) => a.localeCompare(b))
    }))
    .sort((a, b) => a.country.localeCompare(b.country));

  const countryToIoc = {};
  const iocToCountry = {};
  const isoToCountry = {};
  const isoToIoc = {};
  const normalizedCountryToIoc = {};
  const flagAliasToCode = {};
  const normalizedFlagAliasToCode = {};
  const flagCodeToName = {};
  const subdivisionNameToCode = {};
  const normalizedSubdivisionNameToCode = {};
  const subdivisionCodeToName = {};
  const ambiguousCodes = {
    BRN: {
      iocCountry: 'Bahrain',
      isoCountry: 'Brunei',
      note: 'Current IOC code BRN means Bahrain, while current ISO alpha-3 code BRN means Brunei.'
    }
  };
  const ambiguousFlagAliases = {};

  for (const entry of entries) {
    iocToCountry[entry.ioc] = entry.country;
    flagCodeToName[entry.ioc] = entry.country;

    const allNames = [entry.country, ...entry.aliases];
    for (const name of allNames) {
      countryToIoc[name] = entry.ioc;
      normalizedCountryToIoc[normalizeKey(name)] = entry.ioc;
      addLookupMapping(flagAliasToCode, normalizedFlagAliasToCode, ambiguousFlagAliases, name, entry.ioc);
    }

    addLookupMapping(flagAliasToCode, normalizedFlagAliasToCode, ambiguousFlagAliases, entry.ioc, entry.ioc);

    if (entry.isoAlpha3) {
      isoToCountry[entry.isoAlpha3] = entry.country;
      isoToIoc[entry.isoAlpha3] = entry.ioc;
      addLookupMapping(flagAliasToCode, normalizedFlagAliasToCode, ambiguousFlagAliases, entry.isoAlpha3, entry.ioc);
    }
  }

  const subdivisions = SUBDIVISION_ENTRIES
    .map(entry => ({
      ...entry,
      aliases: Array.from(new Set(entry.aliases || [])).sort((a, b) => a.localeCompare(b))
    }))
    .sort((a, b) => a.name.localeCompare(b.name));

  for (const subdivision of subdivisions) {
    subdivisionCodeToName[subdivision.code] = subdivision.name;
    flagCodeToName[subdivision.code] = subdivision.name;

    const subdivisionNames = [subdivision.name, ...subdivision.aliases];
    for (const name of subdivisionNames) {
      subdivisionNameToCode[name] = subdivision.code;
      normalizedSubdivisionNameToCode[normalizeKey(name)] = subdivision.code;
      addLookupMapping(flagAliasToCode, normalizedFlagAliasToCode, ambiguousFlagAliases, name, subdivision.code);
    }

    addLookupMapping(flagAliasToCode, normalizedFlagAliasToCode, ambiguousFlagAliases, subdivision.code, subdivision.code);
  }

  const output = {
    metadata: {
      generatedAt: new Date().toISOString(),
      focus: 'IOC current country names and codes, with ISO alpha-3 crosswalks and North American subdivision flag aliases',
      sources: [IOC_URL.replace('&action=render', ''), COMPARISON_URL.replace('&action=render', ''), ISO_RAW_URL.replace('&action=raw', '')],
      notes: [
        'countryToIoc always returns the current IOC code for known country names and aliases.',
        'isoToIoc converts current ISO alpha-3 codes to the current IOC code for the same country where an IOC code exists.',
        'flagAliasToCode resolves display-team inputs to the actual short flag filename code.',
        'Some 3-letter codes are ambiguous across systems; see ambiguousCodes.',
        'Some human-readable names are ambiguous across countries and subdivisions; see ambiguousFlagAliases.'
      ]
    },
    ambiguousCodes,
    ambiguousFlagAliases,
    countryToIoc,
    normalizedCountryToIoc,
    iocToCountry,
    isoToCountry,
    isoToIoc,
    flagAliasToCode,
    normalizedFlagAliasToCode,
    flagCodeToName,
    subdivisionNameToCode,
    normalizedSubdivisionNameToCode,
    subdivisionCodeToName,
    entries,
    subdivisions
  };

  fs.mkdirSync(path.dirname(OUTPUT_PATH), { recursive: true });
  fs.writeFileSync(OUTPUT_PATH, JSON.stringify(output, null, 2) + '\n');

  console.log(`iocEntries=${entries.length}`);
  console.log(`countryToIoc=${Object.keys(countryToIoc).length}`);
  console.log(`isoToIoc=${Object.keys(isoToIoc).length}`);
  console.log(`subdivisionEntries=${subdivisions.length}`);
  console.log(`sample_CA=${JSON.stringify(subdivisions.find(entry => entry.code === 'CA'))}`);
  console.log(`sample_ON=${JSON.stringify(subdivisions.find(entry => entry.code === 'ON'))}`);
  console.log(`sample_TPE=${JSON.stringify(entries.find(entry => entry.ioc === 'TPE'))}`);
  console.log(`sample_GBR=${JSON.stringify(entries.find(entry => entry.ioc === 'GBR'))}`);
  console.log(`output=${OUTPUT_PATH}`);
}

main().catch(error => {
  console.error(error.stack || error.message || String(error));
  process.exitCode = 1;
});