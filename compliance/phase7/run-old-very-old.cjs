// Standalone runner for the "Junior and Senior Masters" acceptance story (item 200, Phase 7).
//
//   cd compliance/phase7 && npm install && node run-old-very-old.cjs
//
// Needs a running server loaded with the masters fixture ("load JSON owlcms reset"), and Google Chrome installed
// (playwright-core drives it, no browser download). The run changes application data and does not restore it:
// RESTART=1 first restarts the server with the fixture (restart-server.sh, stopping this worktree's server).
//
// Environment: OWLCMS_URL (default http://localhost:8080), ONLY=S0,S1 to run chosen scenarios, HEADED=1 to watch,
// OWLCMS_AUDIT_DIR (default ../../owlcms/logs/audit) to show the audit entries written during the run, RENDER=1 to
// rebuild the pages from old-very-old/run.json without running (after editing the story text).
// Output: index.html (the tutorial, with every screenshot in order), old-very-old/REPORT.md (the test report: checks,
// screenshots and audit entries in order), old-very-old/*.png and run.json. Exit code 1 when a scenario fails.

const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');
const { chromium } = require('playwright-core');
const story = require('./old-very-old.story.cjs');

const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;
const BASE = process.env.OWLCMS_URL || 'http://localhost:8080';
const ONLY = process.env.ONLY ? process.env.ONLY.split(',').map(s => s.trim()) : null;
const OUT = path.join(__dirname, 'old-very-old');
const AUDIT_DIR = process.env.OWLCMS_AUDIT_DIR || path.join(__dirname, '..', '..', 'owlcms', 'logs', 'audit');

const cell = s => String(s ?? '').replace(/\|/g, '\\|').replace(/\s+/g, ' ').trim();
const esc = s => String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

// audit log -----------------------------------------------------------------------------------------------------

const auditFiles = () => {
	try {
		return fs.readdirSync(AUDIT_DIR).filter(f => /_plain_.*\.log$/.test(f));
	} catch {
		return [];
	}
};

/** Current size of each readable audit log. */
const auditMark = () => Object.fromEntries(auditFiles().map(f => [f, fs.statSync(path.join(AUDIT_DIR, f)).size]));

/** Audit lines appended between two marks, prefixed with their stream (competition or platform). */
const auditBetween = (from, to) => {
	const lines = [];
	if (!from || !to) return lines;
	for (const [f, end] of Object.entries(to)) {
		const start = from[f] || 0;
		if (end <= start) continue;
		const buf = Buffer.alloc(end - start);
		const fd = fs.openSync(path.join(AUDIT_DIR, f), 'r');
		try {
			fs.readSync(fd, buf, 0, buf.length, start);
		} finally {
			fs.closeSync(fd);
		}
		const stream = f.replace(/_plain_.*$/, '');
		for (const l of buf.toString('utf8').split('\n')) if (l.trim()) lines.push(`${stream} | ${l}`);
	}
	return lines;
};

// run data ------------------------------------------------------------------------------------------------------

/** Each event gets the audit lines written since the previous event: an event is recorded after its action. */
const attachAudit = (events, endMark) => {
	const marks = events.map(e => e.audit);
	events.forEach((e, i) => {
		e.auditLines = i === 0 ? [] : auditBetween(marks[i - 1], marks[i]);
		delete e.audit;
	});
	if (events.length) {
		const last = events[events.length - 1];
		last.auditLines = last.auditLines.concat(auditBetween(marks[marks.length - 1], endMark));
	}
};

/** "agegroup.delete ×1, athlete.change ×36" */
const auditSummary = lines => {
	const counts = {};
	for (const l of lines) {
		const action = (l.split(' | ')[4] || '?').trim();
		counts[action] = (counts[action] || 0) + 1;
	}
	return Object.entries(counts).map(([a, n]) => `${a} ×${n}`).join(', ');
};

const stepTitle = id => (story.steps[id] || {}).title || id;
const shotName = file => file.replace(/\.png$/, '');

// tutorial (index.html) -----------------------------------------------------------------------------------------

function tutorialPage(data) {
	const { ctx, summary, events } = data;
	const stamp = encodeURIComponent(ctx.date);
	const out = [];
	let takeaway = null;
	const closeSection = () => {
		if (takeaway === null) return;
		out.push(takeaway, '</section>');
		takeaway = null;
	};
	for (const e of events) {
		if (e.kind === 'start') {
			closeSection();
			const s = story.steps[e.scn] || { title: e.scn, intro: '', takeaway: '' };
			const sum = summary.find(x => x.scenario === e.scn) || {};
			out.push(`<section id="${e.scn}"><h2>${esc(s.title)}</h2>`, s.intro);
			if (sum.result === 'FAIL') out.push(`<div class="failure"><p>The automated run failed here: ${esc(sum.detail)}</p></div>`);
			takeaway = s.takeaway || '';
		} else if (e.kind === 'shot') {
			const src = `old-very-old/${e.file}`;
			out.push(`<figure id="${esc(shotName(e.file))}"><figcaption>${esc(story.caption(e.file))}</figcaption>`
				+ `<a href="${src}"><img src="${src}?t=${stamp}" width="1400" height="900" loading="lazy" alt="${esc(shotName(e.file))}"></a></figure>`);
		}
		if (e.auditLines && e.auditLines.length) {
			out.push(`<details class="audit"><summary>Audit log: ${esc(auditSummary(e.auditLines))}</summary><pre>${esc(e.auditLines.join('\n'))}</pre></details>`);
		}
	}
	closeSection();

	const ran = summary.filter(s => s.result !== 'NOT RUN');
	const passed = ran.filter(s => s.result === 'PASS').length;
	const runNote = `Screenshots from the automated run of ${esc(ctx.date)}: ${passed} of ${ran.length} steps passed.`
		+ ` See the <a href="old-very-old/REPORT.md">test report</a>.`
		+ (ctx.auditDir ? '' : ' The audit log was not readable during the run, so its entries are not shown.');
	return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(story.title)}: a tutorial</title>
<style>
  :root { --ink: #1d2733; --muted: #5b6b7b; --line: #d9e0e7; --accent: #1b5fb4; --ok: #1e7b46; --warn: #a15c00; --bad: #b3261e; --bg: #f6f8fa; }
  * { box-sizing: border-box; }
  body { margin: 0; font: 17px/1.6 -apple-system, "Segoe UI", Helvetica, Arial, sans-serif; color: var(--ink); background: #fff; }
  header { background: var(--bg); border-bottom: 1px solid var(--line); padding: 2rem 1rem 1.5rem; }
  main, header > div { max-width: 1440px; margin: 0 auto; padding: 0 1rem; }
  p, ul, .takeaway, .mistake, .failure { max-width: 52rem; }
  h1 { margin: 0 0 .25rem; font-size: 2rem; }
  h2 { margin-top: 3.5rem; padding-bottom: .3rem; border-bottom: 1px solid var(--line); }
  .lead { font-size: 1.15rem; margin: 0 0 .6rem; }
  .muted { color: var(--muted); font-size: .9rem; }
  nav ol { columns: 2; max-width: 52rem; padding-left: 1.2rem; }
  nav a, a { color: var(--accent); }
  .takeaway, .mistake, .failure { margin: 1.2rem 0; padding: .6rem .9rem; border-left: 4px solid var(--ok); background: #f0f8f3; border-radius: 0 6px 6px 0; }
  .takeaway::before { content: "What to remember"; display: block; font-size: .75rem; letter-spacing: .06em; text-transform: uppercase; color: var(--ok); font-weight: 600; }
  .mistake { border-color: var(--warn); background: #fff7ea; }
  .mistake::before { content: "Marie's mistake"; display: block; font-size: .75rem; letter-spacing: .06em; text-transform: uppercase; color: var(--warn); font-weight: 600; }
  .failure { border-color: var(--bad); background: #fdecea; }
  .takeaway p, .mistake p, .failure p { margin: .3rem 0; }
  code { background: var(--bg); border: 1px solid var(--line); border-radius: 4px; padding: 0 .3em; font-size: .88em; }
  figure { margin: 2rem 0; }
  figcaption { margin-bottom: .6rem; max-width: 52rem; }
  figure img { display: block; width: 100%; max-width: 1400px; height: auto; border: 1px solid #9aa7b4; box-shadow: 0 2px 8px rgba(0,0,0,.12); }
  details.audit { margin: -1rem 0 2rem; max-width: 1400px; }
  details.audit summary { cursor: pointer; color: var(--accent); font-size: .9rem; }
  details.audit pre { background: var(--bg); border: 1px solid var(--line); border-radius: 6px; padding: .6rem .8rem; overflow-x: auto; font-size: .75rem; }
  footer { margin: 3rem 0 2rem; color: var(--muted); font-size: .9rem; }
</style>
</head>
<body>
<header>
  <div>
    <h1>${esc(story.title)}</h1>
    <p class="lead">${esc(story.lead)}</p>
    <p class="muted">${runNote}</p>
    <nav><ol>${ran.map(s => `<li><a href="#${s.scenario}">${esc(stepTitle(s.scenario))}</a></li>`).join('')}</ol></nav>
  </div>
</header>
<main>
${story.intro}
${out.join('\n')}
<h2 id="how">How these screenshots were made</h2>
${story.howMade}
<footer>Compliance item 200, Phase 7. Story: <code>compliance/200-SCENARIO_OLD_AND_VERY_OLD.md</code>; specification: <code>compliance/200-AGEGROUP_EDIT_CONFIRMATION.md</code>.</footer>
</main>
</body>
</html>
`;
}

// test report (old-very-old/REPORT.md) --------------------------------------------------------------------------

function reportMd(data) {
	const { ctx, summary, timeline, events, inventories } = data;
	const firstFailure = id => (timeline.find(t => t.scn === id && t.result === 'FAIL') || {}).step || '';
	const lines = [];
	lines.push(`# ${story.title}: test report`, '');
	lines.push('## Run context', '');
	lines.push(`- Target: ${ctx.base}`, `- Date: ${ctx.date}`, `- Browser: ${ctx.browser}`);
	lines.push(`- Viewport: ${ctx.viewport} (1400x900 on purpose)`);
	lines.push(`- Readiness signal: no Vaadin client request in flight, then two animation frames`);
	lines.push(`- Audit log: ${ctx.auditDir ? '`' + ctx.auditDir + '`, entries shown where they were written' : 'not readable, entries not shown'}`);
	lines.push(`- Rows run: ${summary.filter(s => s.result === 'PASS' || s.result === 'FAIL').map(s => s.scenario).join(', ') || 'none'}`);
	lines.push(`- Rows not run: ${summary.filter(s => s.result === 'NOT RUN').map(s => s.scenario).join(', ') || 'none'}`);
	if (ctx.pageErrors.length) lines.push(`- Browser errors: ${ctx.pageErrors.length} (see Differences)`);
	lines.push(`- Tutorial built from this run: [index.html](../index.html)`);
	lines.push('', '## Summary', '', '| Scenario | Step | Result | First failing checkpoint |', '| --- | --- | --- | --- |');
	for (const s of summary) {
		lines.push(`| [${s.scenario}](#${s.scenario.toLowerCase()}) | ${cell(stepTitle(s.scenario))} | ${s.result} | ${cell(firstFailure(s.scenario) || s.detail)} |`);
	}

	lines.push('', '## Timeline', '', 'Checks, screenshots and audit log entries in the order they happened.');
	let checks = [];
	const flushChecks = () => {
		if (!checks.length) return;
		lines.push('', '| Check | Expected | Observed | Result |', '| --- | --- | --- | --- |');
		for (const c of checks) lines.push(`| ${cell(c.step)} | ${cell(c.expected)} | ${cell(c.observed)} | ${c.result} |`);
		checks = [];
	};
	for (const e of events) {
		if (e.kind === 'start') {
			flushChecks();
			const sum = summary.find(x => x.scenario === e.scn) || {};
			lines.push('', `### ${e.scn}`, '', `${stepTitle(e.scn)}: **${sum.result || 'NOT RUN'}**${sum.result === 'FAIL' ? ' (' + cell(sum.detail) + ')' : ''}`);
		} else if (e.kind === 'check') {
			checks.push(e);
		} else if (e.kind === 'shot') {
			flushChecks();
			lines.push('', `**${shotName(e.file)}**: ${story.caption(e.file)}`, '', `![${shotName(e.file)}](${e.file})`);
		}
		if (e.auditLines && e.auditLines.length) {
			flushChecks();
			lines.push('', `<details><summary>Audit log: ${auditSummary(e.auditLines)}</summary>`, '', '```text', ...e.auditLines, '```', '', '</details>');
		}
	}
	flushChecks();

	lines.push('', '## Visible controls by state', '');
	for (const [key, items] of Object.entries(inventories)) {
		lines.push(`### ${key}`, '', ...items.map(i => `- ${i}`), '');
	}
	lines.push('## Differences', '');
	const failed = summary.filter(s => s.result === 'FAIL');
	if (!failed.length && !ctx.pageErrors.length) lines.push('None.');
	for (const s of failed) lines.push(`- ${s.scenario}: ${cell(s.detail)}`);
	for (const e of ctx.pageErrors) lines.push(`- browser: ${cell(e)}`);
	lines.push('', '## Cleanup', '', 'None performed. The run changed application data (championships, age groups, participations);',
		'restore by running again with RESTART=1, or by restarting the "load JSON owlcms reset" launch with the masters fixture.', '');
	return lines.join('\n');
}

const RUN_DATA = path.join(OUT, 'run.json');
const writePages = data => {
	fs.writeFileSync(path.join(OUT, 'REPORT.md'), reportMd(data));
	fs.writeFileSync(path.join(__dirname, 'index.html'), tutorialPage(data));
	console.log(`Report: ${path.join(OUT, 'REPORT.md')}`);
	console.log(`Tutorial: ${path.join(__dirname, 'index.html')}`);
};

(async () => {
	if (process.env.RENDER) {
		writePages(JSON.parse(fs.readFileSync(RUN_DATA, 'utf8')));
		return;
	}
	if (process.env.RESTART) {
		const restart = spawnSync(path.join(__dirname, 'restart-server.sh'), { stdio: 'inherit' });
		if (restart.status !== 0) {
			console.error('BLOCKED: the server restart with the fixture failed.');
			process.exit(2);
		}
	}
	try {
		await fetch(BASE, { signal: AbortSignal.timeout(5000) });
	} catch (e) {
		console.error(`BLOCKED: ${BASE} is not reachable (${e.message}). Start the server with the masters fixture first.`);
		process.exit(2);
	}
	fs.mkdirSync(OUT, { recursive: true });
	if (!process.env.KEEP) for (const f of fs.readdirSync(OUT)) if (f.endsWith('.png')) fs.unlinkSync(path.join(OUT, f));

	const browser = await chromium.launch({ channel: 'chrome', headless: !process.env.HEADED });
	const context = await browser.newContext({ viewport: { width: 1400, height: 900 }, locale: 'en-US' });
	const page = await context.newPage();
	const pageErrors = [];
	page.on('pageerror', e => pageErrors.push(`pageerror: ${e.message}`));
	page.on('console', m => { if (m.type() === 'error' && !/WebSocket|push/i.test(m.text())) pageErrors.push(`console: ${m.text()}`); });

	const body = fs.readFileSync(path.join(__dirname, 'old-very-old.scenario.js'), 'utf8');
	const run = new AsyncFunction('page', 'ONLY_OVERRIDE', 'BASE_OVERRIDE', 'SHOTS_OVERRIDE', 'MARK_OVERRIDE', body);
	const result = await run(page, ONLY, BASE, OUT, auditMark);
	// the last entries of a save can land after its screen updated
	await new Promise(r => setTimeout(r, 1000));
	const endMark = auditMark();

	const vp = await page.evaluate(() => `${window.innerWidth}x${window.innerHeight}`);
	const ctx = {
		base: BASE, date: new Date().toISOString(), browser: `Chrome ${browser.version()}`, viewport: vp, pageErrors,
		auditDir: Object.keys(endMark).length ? AUDIT_DIR : null,
	};
	await browser.close();
	attachAudit(result.events, endMark);
	const data = { ctx, summary: result.summary, timeline: result.timeline, events: result.events, inventories: result.inventories };
	fs.writeFileSync(RUN_DATA, JSON.stringify(data, null, 1));

	for (const s of result.summary) console.log(`${s.scenario}: ${s.result}${s.detail ? ' - ' + s.detail : ''}`);
	writePages(data);
	process.exit(result.summary.some(s => s.result === 'FAIL') ? 1 : 0);
})().catch(e => { console.error(e); process.exit(2); });
