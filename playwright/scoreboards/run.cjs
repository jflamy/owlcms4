#!/usr/bin/env node
// Runs the scoreboard rendering matrix in headless Chrome over the DevTools protocol.
// No extra packages: uses the fixture server, Node's built-in WebSocket, and an installed Chrome.
//
//   node playwright/scoreboards/run.cjs                       # full matrix
//   node playwright/scoreboards/run.cjs --boards simple,medals --scenarios total
//   node playwright/scoreboards/run.cjs --chrome "/path/to/Chrome"
//
// Axes: --scenarios, --boards, --themes, --appearances (comma lists), --best (e.g. 00,10,01,11).
// Exit code 0 when every rendered case passes, 1 otherwise.

const {spawn} = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');
const {start, server} = require('./server.cjs');

function argument(name) {
  const index = process.argv.indexOf(`--${name}`);
  return index >= 0 ? process.argv[index + 1] : undefined;
}
function list(name) {
  const value = argument(name);
  return value ? value.split(',').map(s => s.trim()).filter(Boolean) : undefined;
}

function findChrome() {
  const candidates = [argument('chrome'), process.env.CHROME_PATH,
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    '/Applications/Chromium.app/Contents/MacOS/Chromium',
    '/usr/bin/google-chrome', '/usr/bin/google-chrome-stable', '/usr/bin/chromium', '/usr/bin/chromium-browser',
    'C:/Program Files/Google/Chrome/Application/chrome.exe',
    'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe'];
  const found = candidates.find(c => c && fs.existsSync(c));
  if (!found) throw new Error('Chrome not found; pass --chrome <path> or set CHROME_PATH');
  return found;
}

function launchChrome(executable, profile) {
  const chrome = spawn(executable, [
    '--headless=new', '--remote-debugging-port=0', `--user-data-dir=${profile}`,
    '--no-first-run', '--no-default-browser-check', '--disable-extensions', '--disable-gpu',
    '--window-size=1920,1080', 'about:blank'
  ], {stdio: ['ignore', 'ignore', 'pipe']});
  return new Promise((resolve, reject) => {
    let stderr = '';
    chrome.stderr.on('data', chunk => {
      stderr += chunk;
      const match = stderr.match(/DevTools listening on (ws:\/\/\S+)/);
      if (match) resolve({chrome, browserWs: match[1]});
    });
    chrome.on('exit', code => reject(new Error(`Chrome exited (${code}) before DevTools was ready:\n${stderr}`)));
    setTimeout(() => reject(new Error(`Chrome DevTools did not start:\n${stderr}`)), 20000).unref();
  });
}

class Cdp {
  constructor(ws) {
    this.ws = ws;
    this.id = 0;
    this.pending = new Map();
    ws.addEventListener('message', event => {
      const message = JSON.parse(event.data);
      if (message.id && this.pending.has(message.id)) {
        const {resolve, reject} = this.pending.get(message.id);
        this.pending.delete(message.id);
        message.error ? reject(new Error(`${message.error.message} ${message.error.data ?? ''}`)) : resolve(message.result);
      }
    });
  }
  static connect(url) {
    return new Promise((resolve, reject) => {
      const ws = new WebSocket(url);
      ws.addEventListener('open', () => resolve(new Cdp(ws)));
      ws.addEventListener('error', () => reject(new Error(`Cannot connect to ${url}`)));
    });
  }
  send(method, params = {}) {
    const id = ++this.id;
    this.ws.send(JSON.stringify({id, method, params}));
    return new Promise((resolve, reject) => this.pending.set(id, {resolve, reject}));
  }
  async evaluate(expression) {
    const {result, exceptionDetails} = await this.send('Runtime.evaluate',
      {expression, awaitPromise: true, returnByValue: true});
    if (exceptionDetails) {
      throw new Error(exceptionDetails.exception?.description ?? exceptionDetails.text);
    }
    return result.value;
  }
}

async function main() {
  const options = {
    scenarios: list('scenarios'), boards: list('boards'), themes: list('themes'),
    appearances: list('appearances'),
    bestColumns: list('best')?.map(pair => [pair[0] === '1', pair[1] === '1'])
  };
  for (const key of Object.keys(options)) if (options[key] === undefined) delete options[key];

  const baseUrl = await start(0);
  const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'scoreboard-fixture-chrome-'));
  let chrome;
  try {
    const launched = await launchChrome(findChrome(), profile);
    chrome = launched.chrome;
    const browser = await Cdp.connect(launched.browserWs);
    const {targetId} = await browser.send('Target.createTarget', {url: 'about:blank'});
    const pageWs = launched.browserWs.replace(/\/devtools\/browser\/.*$/, `/devtools/page/${targetId}`);
    const page = await Cdp.connect(pageWs);
    await page.send('Page.enable');
    await page.send('Runtime.enable');
    await page.send('Emulation.setDeviceMetricsOverride',
      {width: 1920, height: 1080, deviceScaleFactor: 1, mobile: false});
    await page.send('Page.navigate', {url: `${baseUrl}?scenario=total&board=simple`});
    await page.evaluate(`new Promise((resolve, reject) => {
      const started = Date.now();
      (function poll() {
        if (window.fixtureReady === true) return resolve();
        if (Date.now() - started > 30000) return reject(new Error('Fixture did not become ready'));
        setTimeout(poll, 50);
      })();
    })`);
    const summary = await page.evaluate(`import('/fixture/checks.mjs')
      .then(({runMatrix}) => runMatrix(${JSON.stringify(options)}))
      .then(result => ({passed: result.passed, failed: result.failed}))
      .catch(error => ({error: error.message}))`);
    if (summary.error) {
      console.error(`FAIL ${summary.error}`);
      process.exitCode = 1;
    } else {
      console.log(`Scoreboard rendering matrix: ${summary.passed} passed, ${summary.failed} failed`);
      if (summary.failed) process.exitCode = 1;
    }
  } finally {
    if (chrome && chrome.exitCode === null) {
      await new Promise(resolve => { chrome.once('exit', resolve); chrome.kill(); });
    }
    server.close();
    fs.rmSync(profile, {recursive: true, force: true, maxRetries: 5});
  }
}

main().catch(error => {
  console.error(error.message);
  process.exitCode = 1;
});
