// Item 200 acceptance story "Junior and Senior Masters" (compliance/200-SCENARIO_OLD_AND_VERY_OLD.md).
//
// One source, two ways to run it. The file is an async function body: `page` is in scope and the last
// expression returned is {summary, failures, timeline, events, inventories}.
//   - Standalone: `node run-old-very-old.cjs` (launches Chrome, writes screenshots and REPORT.md here).
//   - Playwright tool (run_playwright_code): paste the body; the optional overrides below are then undefined.
//
// Viewport is 1400x900 on purpose (a documented exception to the open-browser-1920x1080 skill).
//
// Start from a freshly loaded masters fixture ("load JSON owlcms reset"); the script does not restart or restore
// the application. Run everything, or set ONLY = ['S3'] to run chosen scenarios (they depend on each other:
// a failed scenario makes the following ones NOT RUN).
//
// Long tool runs return a deferredResultId: poll with the same page id until the result arrives.

const ONLY = typeof ONLY_OVERRIDE !== 'undefined' ? ONLY_OVERRIDE : null;
const BASE = typeof BASE_OVERRIDE !== 'undefined' ? BASE_OVERRIDE : 'http://localhost:8080';
const SHOTS = typeof SHOTS_OVERRIDE !== 'undefined' ? SHOTS_OVERRIDE : '/tmp/200-scenario';
// standalone runner: audit log offsets at each event, to show the entries written between screenshots
const MARK = typeof MARK_OVERRIDE !== 'undefined' && MARK_OVERRIDE ? MARK_OVERRIDE : () => null;
const JUNIOR = 'Junior Masters';
const SENIOR = 'Senior Masters';
const GENDERS = [
	{ name: 'Men', letter: 'M', junior55: 'M55', junior60: 'M60', tmp55: 'VM55', tmp60: 'VM60', slip55: 58, slip60: 65, cats: [60, 65, 70, 75, 85, 95, 110] },
	{ name: 'Women', letter: 'F', junior55: 'W55', junior60: 'W60', tmp55: 'VW55', tmp60: 'VW60', cats: [49, 53, 57, 61, 69, 77, 86] },
];
const JUNIOR_GROUPS = ['30', '35', '40', '45', '50', '55', '60'];

const timeline = [];
// checks and screenshots in the order they happened, for the story page
const events = [];
const summary = [];
const inventories = {};

// ---------------------------------------------------------------------------------------------------------------
// helpers
// ---------------------------------------------------------------------------------------------------------------

/** Records a checkpoint; a failed one aborts the scenario. */
const check = (scn, step, expected, observed, ok) => {
	const entry = { scn, step, expected, observed: String(observed).slice(0, 300), result: ok ? 'PASS' : 'FAIL' };
	timeline.push(entry);
	events.push({ kind: 'check', ...entry, audit: MARK() });
	if (!ok) {
		throw new Error(`${scn} ${step}: expected ${expected}; observed ${String(observed).slice(0, 300)}`);
	}
};

/** Ready when no Vaadin client has a request in flight, then two animation frames for dialogs to render. */
const settle = async () => {
	await page.waitForFunction(() => Object.values(window.Vaadin.Flow.clients).every(c => !c.isActive()));
	await page.evaluate(() => new Promise(r => requestAnimationFrame(() => requestAnimationFrame(r))));
};

const shot = async (scn, name) => {
	// finish dialog fade-ins and fade-outs, else dialogs are captured half transparent
	await page.screenshot({ path: `${SHOTS}/${scn}-${name}.png`, animations: 'disabled' });
	events.push({ kind: 'shot', scn, name, file: `${scn}-${name}.png`, audit: MARK() });
};

/** Visible buttons and checkboxes, with their state: the "visible controls by state" inventory. */
const inventory = () => page.evaluate(() => {
	const vis = e => e.getClientRects().length > 0;
	const items = [];
	for (const b of document.querySelectorAll('vaadin-button')) {
		if (vis(b) && b.textContent.trim()) items.push(`button "${b.textContent.trim()}"${b.id ? ' #' + b.id : ''}${b.disabled ? ' disabled' : ''}`);
	}
	for (const c of document.querySelectorAll('vaadin-dialog vaadin-checkbox')) {
		if (vis(c)) items.push(`checkbox "${(c.textContent || c.label || '').trim()}" ${c.checked ? 'checked' : 'unchecked'}`);
	}
	return items.slice(0, 60);
});
const note = async (scn, step) => { inventories[`${scn} ${step}`] = await inventory(); };

/** Rendered rows of a grid as arrays of cell texts. which: 'first' or 'last' grid matching the scope. */
const gridRows = (scope = 'vaadin-grid', which = 'first') => page.evaluate(([scope, which]) => {
	const all = [...document.querySelectorAll(scope)];
	const g = which === 'last' ? all[all.length - 1] : all[0];
	if (!g) return [];
	const rows = [];
	for (const tr of g.shadowRoot.querySelectorAll('tbody tr')) {
		if (!tr.getClientRects().length) continue;
		rows.push([...tr.children].map(td => {
			const s = td.querySelector('slot');
			const el = s && g.querySelector(`[slot="${s.name}"]`);
			return el ? el.textContent.trim() : '';
		}));
	}
	return rows;
}, [scope, which]);

/** Centre of an element in the grid row whose cells include rowText (inner selector and text optional). */
const rowTarget = (scope, which, rowText, cellIdx, inner, innerText) => page.evaluate(([scope, which, rowText, cellIdx, inner, innerText]) => {
	const all = [...document.querySelectorAll(scope)];
	const g = which === 'last' ? all[all.length - 1] : all[0];
	for (const tr of g.shadowRoot.querySelectorAll('tbody tr')) {
		if (!tr.getClientRects().length) continue;
		const els = [...tr.children].map(td => { const s = td.querySelector('slot'); return s && g.querySelector(`[slot="${s.name}"]`); });
		if (!els.some(e => e && e.textContent.trim() === rowText)) continue;
		let el = els[cellIdx];
		if (el && inner) el = [...el.querySelectorAll(inner)].find(b => !innerText || b.textContent.includes(innerText));
		if (!el) return null;
		el.scrollIntoView({ block: 'center' });
		const r = el.getBoundingClientRect();
		return { x: r.x + r.width / 2, y: r.y + r.height / 2 };
	}
	return null;
}, [scope, which, rowText, cellIdx, inner, innerText]);

const clickRow = async (scope, which, rowText, cellIdx, inner, innerText) => {
	const p = await rowTarget(scope, which, rowText, cellIdx, inner, innerText);
	if (!p) throw new Error(`row target not found: ${rowText} / ${inner || 'cell'} ${innerText || ''}`);
	await page.mouse.click(p.x, p.y);
	await settle();
};

/** Real mouse click on a button of the dialog showing headingText (script clicks do not reach the server reliably). */
const dialogButton = async (heading, label) => {
	const p = await page.evaluate(([heading, label]) => {
		const h = [...document.querySelectorAll('*')].find(e => e.children.length === 0 && e.textContent.trim() === heading);
		if (!h) return null;
		const has = c => [...c.querySelectorAll('vaadin-button')].some(b => b.textContent.trim() === label);
		let c = h;
		while (c && !has(c)) c = c.parentElement;
		if (!c) return null;
		const b = [...c.querySelectorAll('vaadin-button')].find(b => b.textContent.trim() === label);
		const r = b.getBoundingClientRect();
		return { x: r.x + r.width / 2, y: r.y + r.height / 2 };
	}, [heading, label]);
	if (!p) throw new Error(`dialog button not found: "${heading}" / "${label}"`);
	await page.mouse.click(p.x, p.y);
	await settle();
};

/** Click an item of an open select or combo-box list. */
const pickItem = async (tag, text) => {
	await page.waitForFunction(([tag, text]) => [...document.querySelectorAll(tag)].some(i => i.textContent.trim() === text && i.getClientRects().length),
		[tag, text], { timeout: 5000 }).catch(() => {});
	const p = await page.evaluate(([tag, text]) => {
		const it = [...document.querySelectorAll(tag)].find(i => i.textContent.trim() === text && i.getClientRects().length);
		if (!it) return null;
		const r = it.getBoundingClientRect();
		return { x: r.x + r.width / 2, y: r.y + r.height / 2 };
	}, [tag, text]);
	if (!p) throw new Error(`list item not found: ${tag} "${text}"`);
	await page.mouse.click(p.x, p.y);
	await settle();
};

/** Inner text of the dialog that contains the element with the given id. */
const dialogText = id => page.evaluate(id => {
	const b = document.getElementById(id);
	if (!b) return null;
	let c = b;
	while (c && c.tagName !== 'VAADIN-DIALOG') c = c.parentElement;
	return (c || b.parentElement).innerText.replace(/\n+/g, ' | ');
}, id);

const formInput = label => page.locator('vaadin-form-item', { hasText: label }).locator('input').first();

/** Notifications last 3 s: wait for the previous one to go, so a screenshot shows only its own. */
const quiet = () => page.waitForFunction(() => ![...document.querySelectorAll('vaadin-notification-card')].some(c => c.getClientRects().length),
	null, { timeout: 8000 });

/** Saves run in the background: the dialog or editor closes when the change is committed and shown. */
const closed = async selector => {
	await page.waitForSelector(selector, { state: 'hidden', timeout: 20000 });
	await settle();
};

// age group page ------------------------------------------------------------------------------------------------

const openAgeGroups = async () => {
	await page.goto(`${BASE}/preparation/agegroup`);
	await page.waitForSelector('.agegroup-active');
	await settle();
};
const filterName = async text => {
	await page.locator('vaadin-text-field[placeholder="Name"] input').fill(text);
	await settle();
};
const agRows = async () => (await gridRows()).map(r => ({ code: r[1], champ: r[2], gender: r[5], min: r[6], max: r[7] }));
const findAg = async code => { await filterName(code); return (await agRows()).find(r => r.code === code) || null; };
const isActive = (code, letter) => page.evaluate(([code, letter]) => {
	const el = [...document.querySelectorAll(`.agegroup-active[data-agegroup="${code}_${letter}"]`)].find(e => e.getClientRects().length);
	return el ? el.checked : null;
}, [code, letter]);
const openEditor = async code => {
	await filterName(code);
	await clickRow('vaadin-grid', 'first', code, 1);
	await page.locator('#agegroup-editor-update').waitFor({ state: 'visible' });
};

/** Press Update in the editor and answer the gap and reassignment dialogs with Continue / Apply; returns the dialogs seen. */
const updateEditor = async tag => {
	await page.locator('#agegroup-editor-update').click();
	const seen = [];
	for (let i = 0; i < 3; i++) {
		const state = await page.waitForFunction(() => {
			const dialog = ['agegroup-gap-continue', 'agegroup-reassign-apply'].find(id => document.getElementById(id));
			if (dialog) return dialog;
			const update = document.getElementById('agegroup-editor-update');
			return !update || !update.getClientRects().length ? 'closed' : false;
		}, null, { timeout: 20000 }).then(h => h.jsonValue());
		if (state === 'closed') break;
		seen.push({ id: state, text: await dialogText(state) });
		await shot('S3', `${tag}-${state.replace('agegroup-', '')}`);
		await page.locator('#' + state).click();
		await settle();
		await page.waitForFunction(id => !document.getElementById(id), state, { timeout: 10000 }).catch(() => {});
	}
	await settle();
	return seen;
};

/** Delete from the editor; returns the delete dialog text, or null when the delete went through directly. */
const startDelete = async code => {
	await openEditor(code);
	const del = page.locator('#agegroup-editor-delete');
	if (await del.count()) await del.click(); else await page.getByRole('button', { name: 'Delete', exact: true }).first().click();
	await settle();
	await dialogButton(`Delete ${code}?`, 'Delete');
	const dialog = await page.waitForFunction(() => !!document.getElementById('agegroup-delete-apply'), null, { timeout: 8000 })
		.then(() => true).catch(() => false);
	await settle();
	return dialog ? await dialogText('agegroup-delete-apply') : null;
};

/** Toggle Active in the grid and answer the gap and reassignment dialogs with Continue / Apply; returns the dialogs seen. */
const activate = async (code, letter, tag) => {
	await openAgeGroups(); // a closing editor dialog can still swallow the click
	await filterName(code);
	await page.locator(`.agegroup-active[data-agegroup="${code}_${letter}"]:visible`).first().click({ force: true });
	await settle();
	const seen = [];
	for (let i = 0; i < 3; i++) {
		const id = await page.waitForFunction(() => ['agegroup-gap-continue', 'agegroup-reassign-apply']
			.find(i => document.getElementById(i)), null, { timeout: 4000 }).then(h => h.jsonValue()).catch(() => null);
		if (!id) break;
		seen.push({ id, text: await dialogText(id) });
		if (tag) await shot('S3', `${tag}-${id.replace('agegroup-', '')}`);
		await page.locator('#' + id).click();
		await settle();
		await page.waitForFunction(id => !document.getElementById(id), id, { timeout: 10000 }).catch(() => {});
	}
	if (!seen.length) {
		const diag = await page.evaluate(() => ({
			dialogs: document.querySelectorAll('vaadin-dialog').length,
			notes: [...document.querySelectorAll('vaadin-notification-card')].map(n => n.innerText.slice(0, 120)),
			row: [...document.querySelectorAll('.agegroup-active')].filter(e => e.getClientRects().length).map(e => e.getAttribute('data-agegroup') + ':' + e.checked),
		}));
		inventories[`activate ${code} without dialog`] = [JSON.stringify(diag)];
		await shot('S3', `${code}-no-dialog`);
	}
	return seen;
};

// championship page ---------------------------------------------------------------------------------------------

const openChampionships = async () => {
	await page.goto(`${BASE}/preparation/championships`);
	await page.waitForSelector('vaadin-grid');
	await settle();
	const hide = page.getByRole('checkbox', { name: 'Hide Empty Championships' });
	if (await hide.isChecked()) { await hide.click(); await settle(); }
};
const champNames = async () => (await gridRows()).map(r => r[1]);

/** Drag a championship row by its handle onto the upper edge of another row (HTML5 drag and drop, real mouse). */
const dragChampionshipAbove = async (name, target) => {
	const box = text => page.evaluate(text => {
		const g = document.querySelector('vaadin-grid');
		for (const tr of g.shadowRoot.querySelectorAll('tbody tr')) {
			if (!tr.getClientRects().length) continue;
			const texts = [...tr.children].map(td => {
				const s = td.querySelector('slot');
				const el = s && g.querySelector(`[slot="${s.name}"]`);
				return el ? el.textContent.trim() : '';
			});
			if (!texts.includes(text)) continue;
			tr.scrollIntoView({ block: 'center' });
			const r = tr.getBoundingClientRect();
			return { x: r.x, y: r.y, h: r.height };
		}
		return null;
	}, text);
	const from = await box(name);
	if (!from) throw new Error(`championship row not found: ${name}`);
	await page.mouse.move(from.x + 22, from.y + from.h / 2);
	await page.mouse.down();
	await page.mouse.move(from.x + 26, from.y + from.h / 2 - 6, { steps: 3 });
	const to = await box(target);
	if (!to) throw new Error(`championship row not found: ${target}`);
	await page.mouse.move(to.x + 200, to.y + to.h * 0.15, { steps: 20 });
	await page.mouse.up();
	await settle();
};

// athlete page --------------------------------------------------------------------------------------------------

const athleteCategories = async championship => {
	await page.goto(`${BASE}/preparation/athletes?ad=${encodeURIComponent(championship)}`);
	await page.waitForSelector('vaadin-grid');
	await settle();
	return (await gridRows()).flatMap(r => r.filter(c => /^(VM|VW|M|W)\d\d\b/.test(c)));
};
const isTemp = c => /^V[MW]\d\d\b/.test(c);

// ---------------------------------------------------------------------------------------------------------------
// scenarios
// ---------------------------------------------------------------------------------------------------------------

const scenarios = {
	// Preconditions; read only.
	async S0() {
		await page.setViewportSize({ width: 1400, height: 900 });
		await openAgeGroups();
		const vp = await page.evaluate(() => ({ w: window.innerWidth, h: window.innerHeight }));
		check('S0', 'viewport', '1400x900', `${vp.w}x${vp.h}`, vp.w === 1400 && vp.h === 900);
		const m55 = await findAg('M55');
		check('S0', 'fixture has M55 in Masters', 'M55 / Masters', JSON.stringify(m55), m55 && m55.champ === 'Masters');
		await openChampionships();
		const names = await champNames();
		check('S0', `no ${JUNIOR} or ${SENIOR} yet`, 'neither present', names.join(','), !names.includes(JUNIOR) && !names.includes(SENIOR));
		await shot('S0', 'initial');
	},

	// 1. Rename Masters to Senior Masters.
	async S1() {
		await openChampionships();
		await shot('S1', 'initial');
		await clickRow('vaadin-grid', 'first', 'Masters', 2, 'vaadin-button', 'Edit Medals');
		await page.locator('vaadin-text-field input').first().fill(SENIOR);
		await shot('S1', 'form');
		await page.getByRole('button', { name: 'Update', exact: true }).click();
		await settle();
		const names = await champNames();
		check('S1', 'championship renamed', `${SENIOR} listed, Masters gone`, names.join(','), names.includes(SENIOR) && !names.includes('Masters'));
		await shot('S1', 'renamed');
		await openAgeGroups();
		const m30 = await findAg('M30');
		check('S1', 'age groups follow the rename', `M30 in ${SENIOR}`, JSON.stringify(m30), m30 && m30.champ === SENIOR);
		await shot('S1', 'final');
	},

	// 2. Create Junior Masters and move M30-M60 / W30-W60 into it: Update applies the move, there is no confirmation.
	async S2() {
		await openChampionships();
		if (!(await champNames()).includes(JUNIOR)) {
			await page.getByRole('button', { name: 'Add', exact: true }).click();
			await settle();
			await page.locator('vaadin-text-field input').first().fill(JUNIOR);
			await page.locator('vaadin-form-item', { hasText: 'Type' }).locator('vaadin-combo-box').click();
			await settle();
			await pickItem('vaadin-combo-box-item', 'Masters - code includes gender');
			await shot('S2', 'add');
			await page.getByRole('button', { name: 'Update', exact: true }).click();
			await settle();
		}
		check('S2', `${JUNIOR} created`, `${JUNIOR} listed`, (await champNames()).join(','), (await champNames()).includes(JUNIOR));

		await openChampionships(); // a closing form can swallow the next click
		await clickRow('vaadin-grid', 'first', JUNIOR, 2, 'vaadin-button', 'Select Age Groups');
		await page.waitForSelector('vaadin-dialog vaadin-grid', { state: 'attached' });
		await settle();
		const wanted = GENDERS.flatMap(g => JUNIOR_GROUPS.map(n => g.letter === 'M' ? 'M' + n : 'W' + n));
		for (const code of wanted) await clickRow('vaadin-dialog vaadin-grid', 'last', code, 0, 'vaadin-checkbox');
		await shot('S2', 'selection');
		await page.getByRole('button', { name: 'Update', exact: true }).click();
		await page.waitForFunction(() => ![...document.querySelectorAll('vaadin-dialog vaadin-grid')].some(g => g.getClientRects().length));
		await settle();
		await shot('S2', 'moved');

		// the championship order is the order of the age group list: Marie puts Junior Masters first
		await dragChampionshipAbove(JUNIOR, SENIOR);
		const order = await champNames();
		check('S2', `${JUNIOR} dragged above ${SENIOR}`, `${JUNIOR} listed before ${SENIOR}`, order.join(','),
			order.includes(JUNIOR) && order.indexOf(JUNIOR) < order.indexOf(SENIOR));
		await shot('S2', 'reordered');
		await openAgeGroups();
		const m60 = await findAg('M60');
		check('S2', 'M60 moved', JUNIOR, JSON.stringify(m60), m60 && m60.champ === JUNIOR);
		const m65 = await findAg('M65');
		check('S2', 'M65 stayed', SENIOR, JSON.stringify(m65), m65 && m65.champ === SENIOR);
		await filterName('M6');
		await shot('S2', 'final');
	},

	// 3. The natural code is refused; V55 is created and activated (gap warning: no 60 yet), then V60 closes the gap.
	async S3() {
		for (const g of GENDERS) {
			await openAgeGroups();
			// 3a. refused
			await page.getByRole('button', { name: 'Add', exact: true }).first().click();
			await settle();
			await formInput(/Display Short/).fill(g.junior55);
			await formInput(/Championship/).fill(SENIOR);
			await formInput(/Championship/).press('Tab');
			await page.locator('vaadin-form-item', { hasText: 'Gender' }).locator('vaadin-select, vaadin-combo-box').click({ force: true });
			await settle();
			await pickItem('vaadin-select-item, vaadin-combo-box-item', g.name);
			await page.locator('#agegroup-editor-add').click();
			await settle();
			const refused = await page.evaluate(() => [...document.querySelectorAll('vaadin-select, vaadin-text-field')]
				.filter(f => f.invalid).map(f => f.errorMessage).join(' / '));
			check('S3', `${g.junior55} refused`, 'code and gender already used', refused, /already used/i.test(refused));
			await shot('S3', `${g.junior55}-refused`);
			await page.getByRole('button', { name: 'Cancel', exact: true }).first().click();
			await settle();

			// 3b. each created with the V prefix and activated at once. The men's groups are typed wrongly (55-58 and
			// 60-65): V55 leaves 59-64 uncovered, V60 then leaves 59 uncovered and overlaps M65 at 65. Both are corrected after.
			for (const [tmp, min, max] of [[g.tmp55, 55, 59], [g.tmp60, 60, 64]]) {
				const slip = tmp === g.tmp55 ? g.slip55 : g.slip60;
				if (await findAg(tmp)) continue; // re-run after a partial run
				await quiet();
				await page.getByRole('button', { name: 'Add', exact: true }).first().click();
				await settle();
				await formInput(/Display Short/).fill(tmp);
				await page.locator('vaadin-checkbox', { hasText: 'The code contains the genre' }).click({ force: true });
				await formInput(/Championship/).fill(SENIOR);
				await settle();
				await pickItem('vaadin-combo-box-item', SENIOR);
				await formInput(/Minimum Age/).fill(String(min));
				await formInput(/Maximum Age/).fill(String(slip || max));
				await page.locator('vaadin-form-item', { hasText: 'Gender' }).locator('vaadin-select, vaadin-combo-box').click({ force: true });
				await settle();
				await pickItem('vaadin-select-item, vaadin-combo-box-item', g.name);
				const maxField = page.locator('vaadin-text-field[placeholder*="max"] input');
				for (const w of g.cats) { await maxField.click(); await maxField.fill(String(w)); await maxField.press('Enter'); await settle(); }
				await page.locator('#agegroup-editor-add').click();
				await settle();
				const created = await findAg(tmp);
				check('S3', `${tmp} created`, `${SENIOR}, inactive`, JSON.stringify(created), created && created.champ === SENIOR);
				check('S3', `${tmp} inactive on creation`, 'unchecked', await isActive(tmp, g.letter), (await isActive(tmp, g.letter)) === false);
				await shot('S3', `${tmp}-created`);

				const seen = await activate(tmp, g.letter, tmp);
				const gapText = seen.filter(d => d.id === 'agegroup-gap-continue').map(d => d.text).join(' ');
				if (tmp === g.tmp55) {
					const from = (slip || max) + 1;
					check('S3', `${tmp} activation gap warning`, `gap dialog for ${from}-64`, JSON.stringify(seen.map(d => d.text)), new RegExp(`${from}-64`).test(gapText));
					await note('S3', `${tmp} activated`);
				} else if (slip) {
					check('S3', `${tmp} activation gap and overlap warning`, 'gap at 59 and overlap at 65', JSON.stringify(seen.map(d => d.text)),
						/aged 59\b/.test(gapText) && /overlap/i.test(gapText));
				} else {
					check('S3', `${tmp} activation has no gap`, 'no gap dialog', JSON.stringify(seen.map(d => d.id)), !seen.some(d => d.id === 'agegroup-gap-continue'));
				}
			}

			if (g.slip55) {
				// the check lists everything left to fix: after V55 only the overlap at 65 remains, after V60 nothing
				for (const [tmp, max] of [[g.tmp55, 59], [g.tmp60, 64]]) {
					await openAgeGroups();
					await openEditor(tmp);
					await formInput(/Maximum Age/).fill(String(max));
					await shot('S3', `${tmp}-fix-editor`);
					const fix = await updateEditor(`${tmp}-fix`);
					const fixText = fix.map(d => d.text).join(' ');
					const reported = tmp === g.tmp55 ? fix.length === 1 && /overlap/i.test(fixText) && !/aged 59\b/.test(fixText) : fix.length === 0;
					check('S3', `${tmp} corrected to max age ${max}`, tmp === g.tmp55 ? 'only the overlap at 65 still reported' : 'nothing left, no dialog',
						JSON.stringify(fix.map(d => d.text)), reported && (await findAg(tmp) || {}).max === String(max));
				}
			}
			await filterName(g.tmp55);
			const active55 = await isActive(g.tmp55, g.letter);
			await filterName(g.tmp60);
			const active60 = await isActive(g.tmp60, g.letter);
			check('S3', `${g.tmp55} and ${g.tmp60} active`, 'true, true', `${active55}, ${active60}`, active55 === true && active60 === true);
		}
		await filterName('V');
		await shot('S3', 'final');
	},

	// 4. Trimming Junior Masters from the middle: the gap warning, then Cancel.
	async S4() {
		for (const g of GENDERS) {
			await openAgeGroups();
			const text = await startDelete(g.junior55);
			// until the AgeGroup.AgeGap.DeleteWarning translation is imported the dialog shows the missing-key marker
			check('S4', `${g.junior55} delete dialog`, `gap 55-59 in ${JUNIOR}`, text, text && /Age Gap/.test(text)
				&& ((/55-59/.test(text) && text.includes(JUNIOR)) || /AgeGroup\.AgeGap\.DeleteWarning/.test(text)));
			await note('S4', `${g.junior55} dialog`);
			await shot('S4', `${g.junior55}-dialog`);
			await page.locator('#agegroup-delete-cancel').click();
			await settle();
			await openAgeGroups();
			check('S4', `${g.junior55} kept after Cancel`, 'still listed', JSON.stringify(await findAg(g.junior55)), (await findAg(g.junior55)) !== null);
			await shot('S4', `${g.junior55}-kept`);
		}
	},

	// 5. Trimming from the top: 60 first, then 55. No gap text; the athletes who leave Junior Masters are listed.
	async S5() {
		for (const g of GENDERS) {
			for (const code of [g.junior60, g.junior55]) {
				await openAgeGroups();
				const text = await startDelete(code);
				if (text) {
					check('S5', `${code} no gap text`, 'no age-gap wording', text, !/Age Gap/.test(text));
					const table = await gridRows('vaadin-dialog vaadin-grid', 'last');
					// athletes entered below their QT by exception (Ignore errors) are ejected by any reassignment, by design (spec section 2)
					const ofCode = table.filter(r => r[2].includes(code));
					const qtOnly = table.filter(r => !r[2].includes(code));
					check('S5', `${code} athletes listed`, `rows Removed or Moved, Current ${code}`, JSON.stringify(ofCode.slice(0, 3)),
						ofCode.length > 0 && ofCode.every(r => /removed|moved|DryRun\.Moves/i.test(r[1])));
					check('S5', `${code} other rows are QT ejections`, 'only "entry total" reasons', JSON.stringify(qtOnly.slice(0, 3)),
						qtOnly.every(r => /entry total|qualifying/i.test(r[1])));
					inventories[`S5 ${code} QT ejections`] = qtOnly.map(r => `${r[0]} | ${r[1]} | ${r[2]}`);
					await shot('S5', `${code}-dialog`);
					await page.locator('#agegroup-delete-apply').click();
					await closed('#agegroup-delete-apply');
				}
				await openAgeGroups();
				check('S5', `${code} deleted`, 'not listed', JSON.stringify(await findAg(code)), (await findAg(code)) === null);
				await filterName(g.letter === 'M' ? 'M' : 'W');
				await shot('S5', `${code}-deleted`);
			}
		}
		await openAgeGroups();
		const m50 = await findAg('M50');
		check('S5', `${JUNIOR} now ends at 50`, `M50 in ${JUNIOR}`, JSON.stringify(m50), m50 && m50.champ === JUNIOR);
		await shot('S5', 'final');
	},

	// 6. The 55+ athletes are not in Senior Masters until "Recompute all eligibilities".
	async S6() {
		const before = await athleteCategories(SENIOR);
		check('S6', 'before: no VM/VW participations', 'none', before.join(','), !before.some(isTemp));
		await shot('S6', 'before');
		await openAgeGroups();
		await page.locator('#agegroup-recompute-all').click();
		await page.waitForSelector('#agegroup-global-apply');
		await settle();
		const text = await dialogText('agegroup-global-apply');
		const table = await gridRows('vaadin-dialog vaadin-grid', 'last');
		check('S6', 'dry run lists additions', 'Added rows with VM55 / VM60 / VW55 / VW60 After', JSON.stringify(table.slice(0, 3)),
			table.some(r => /added/i.test(r[2]) && r.some(c => /V[MW](55|60)/.test(c))));
		await note('S6', 'dry run');
		await shot('S6', 'dry-run');
		await page.locator('#agegroup-global-apply').click();
		await closed('#agegroup-global-apply');
		const after = await athleteCategories(SENIOR);
		check('S6', 'after: VM/VW participations', 'at least one', after.join(','), after.some(isTemp));
		await shot('S6', 'final');
	},

	// 7. Rename V55 / V60 to M55 / M60 (and the women's); possible only because Junior Masters no longer has them.
	async S7() {
		for (const g of GENDERS) {
			for (const [tmp, final] of [[g.tmp55, g.junior55], [g.tmp60, g.junior60]]) {
				await openAgeGroups();
				const before = await findAg(tmp);
				await openEditor(tmp);
				await formInput(/Display Short/).fill(final);
				await shot('S7', `${tmp}-editor`);
				await page.locator('#agegroup-editor-update').click();
				// the editor closes when the rename is saved; a dialog instead is what the next check catches
				await page.waitForFunction(() => {
					const u = document.getElementById('agegroup-editor-update');
					return !u || !u.getClientRects().length
						|| ['agegroup-gap-continue', 'agegroup-reassign-apply', 'agegroup-move-confirm'].some(i => document.getElementById(i));
				}, null, { timeout: 20000 });
				await settle();
				const dialog = await page.evaluate(() => ['agegroup-gap-continue', 'agegroup-reassign-apply', 'agegroup-move-confirm'].find(i => document.getElementById(i)) || null);
				check('S7', `${tmp} rename has no dialog`, 'none', dialog, dialog === null);
				await openAgeGroups();
				const renamed = await findAg(final);
				check('S7', `${tmp} -> ${final}`, 'same championship and ages', JSON.stringify(renamed),
					renamed && renamed.champ === SENIOR && renamed.min === before.min && renamed.max === before.max);
				check('S7', `${tmp} gone`, 'not listed', JSON.stringify(await findAg(tmp)), (await findAg(tmp)) === null);
			}
		}
		await openAgeGroups();
		await filterName('M');
		await shot('S7', 'groups');
		const cats = await athleteCategories(SENIOR);
		check('S7', 'athlete categories follow the rename', 'M55/M60 or W55/W60, no VM/VW', cats.join(','),
			cats.some(c => /^(M|W)(55|60)\b/.test(c)) && !cats.some(isTemp));
		await shot('S7', 'final');
	},
};

// ---------------------------------------------------------------------------------------------------------------
// runner
// ---------------------------------------------------------------------------------------------------------------

let blocked = false;
for (const id of Object.keys(scenarios)) {
	if (ONLY && !ONLY.includes(id)) { summary.push({ scenario: id, result: 'NOT RUN', detail: 'not selected' }); continue; }
	if (blocked) { summary.push({ scenario: id, result: 'NOT RUN', detail: 'an earlier scenario failed' }); continue; }
	events.push({ kind: 'start', scn: id, audit: MARK() });
	try {
		await scenarios[id]();
		summary.push({ scenario: id, result: 'PASS' });
	} catch (e) {
		blocked = true;
		await shot(id, 'FAILURE').catch(() => {});
		summary.push({ scenario: id, result: 'FAIL', detail: e.message });
	}
}
return { summary, failures: timeline.filter(t => t.result === 'FAIL'), timeline, events, inventories };
