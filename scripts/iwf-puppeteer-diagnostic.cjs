#!/usr/bin/env node

const fs = require('node:fs/promises');
const path = require('node:path');
const process = require('node:process');
const { createRequire } = require('node:module');

const upstreamDirectory = path.resolve(process.argv[2] || 'upstream');
const outputDirectory = path.resolve(process.argv[3] || 'iwf-puppeteer-diagnostic');
const upstreamRequire = createRequire(path.join(upstreamDirectory, 'package.json'));
const puppeteer = upstreamRequire('puppeteer');

const browserConfiguration = {
  headless: true,
  args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage'],
};
const userAgent = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) '
  + 'AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36';
const viewport = { width: 1280, height: 800 };
const targets = [
  {
    name: 'results',
    url: 'https://iwf.sport/results/results-by-events/',
    expectedSelectors: ['select[name="event_year"]', 'a.card', 'div.single__event__filter'],
  },
  {
    name: 'records',
    url: 'https://iwf.sport/results/world-records/',
    expectedSelectors: ['#ranking_curprog', '[name="ranking_agegroup"]', '#ranking_gender'],
  },
];
const challengeSelectors = [
  '#challenge-running',
  '#challenge-form',
  '.cf-challenge',
  'iframe[src*="challenges.cloudflare.com"]',
  'iframe[src*="recaptcha"]',
  '.g-recaptcha',
];

async function matchingSelectors(page, selectors) {
  const matches = [];
  for (const selector of selectors) {
    if (await page.$(selector)) matches.push(selector);
  }
  return matches;
}

async function probe(browser, target) {
  const page = await browser.newPage();
  const startedAt = Date.now();
  let response = null;
  let navigationError = null;

  await page.setUserAgent(userAgent);
  await page.setViewport(viewport);

  try {
    response = await page.goto(target.url, {
      waitUntil: 'domcontentloaded',
      timeout: 30_000,
    });
  } catch (error) {
    navigationError = error.message;
  }

  await new Promise((resolve) => setTimeout(resolve, 3_000));

  const title = await page.title();
  const bodyText = (await page.$eval('body', (body) => body.innerText).catch(() => '')).slice(0, 2_000);
  const expectedMatches = await matchingSelectors(page, target.expectedSelectors);
  const challengeMatches = await matchingSelectors(page, challengeSelectors);
  const challengeText = /just a moment|verify you are human|checking your browser|cloudflare|captcha/i.test(
    `${title}\n${bodyText}`,
  );
  const headers = response?.headers() || {};
  const finalUrl = page.url();
  const browserSignals = await page.evaluate(() => ({
    userAgent: navigator.userAgent,
    platform: navigator.platform,
    webdriver: navigator.webdriver,
    languages: navigator.languages,
  }));

  await page.screenshot({ path: path.join(outputDirectory, `${target.name}.png`), fullPage: true });
  await fs.writeFile(path.join(outputDirectory, `${target.name}.html`), await page.content(), 'utf8');
  await page.close();

  return {
    name: target.name,
    requestedUrl: target.url,
    finalUrl,
    status: response?.status() ?? null,
    title,
    elapsedMs: Date.now() - startedAt,
    navigationError,
    expectedMatches,
    challengeMatches,
    challengeText,
    passed: expectedMatches.length > 0 && challengeMatches.length === 0 && !challengeText,
    responseHeaders: {
      server: headers.server || null,
      cfRay: headers['cf-ray'] || null,
      cfCacheStatus: headers['cf-cache-status'] || null,
      contentType: headers['content-type'] || null,
    },
    browserSignals,
  };
}

async function main() {
  await fs.mkdir(outputDirectory, { recursive: true });
  const browser = await puppeteer.launch(browserConfiguration);

  try {
    const browserVersion = await browser.version();
    const results = [];
    for (const target of targets) results.push(await probe(browser, target));

    const report = {
      generatedAt: new Date().toISOString(),
      runner: {
        platform: process.platform,
        architecture: process.arch,
        node: process.version,
      },
      puppeteerVersion: upstreamRequire('puppeteer/package.json').version,
      browserVersion,
      configuredUserAgent: userAgent,
      configuredViewport: viewport,
      configuredLaunchOptions: browserConfiguration,
      results,
    };

    await fs.writeFile(path.join(outputDirectory, 'report.json'), JSON.stringify(report, null, 2), 'utf8');
    console.log(JSON.stringify(report, null, 2));
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});