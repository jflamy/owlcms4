// Text of the "Junior and Senior Masters" tutorial (index.html). The runner merges it with the screenshots of the run, in
// the order they were taken. Captions narrate what Marie does and what the screen shows; edit them with the
// scenario. The test report (old-very-old/REPORT.md) uses the same captions.

const title = 'Junior Masters and Senior Masters';

const lead = 'Splitting a Masters championship in two: renaming championships, moving and deleting age groups, '
	+ 'and keeping athletes in the right categories.';

const intro = `
<p>Marie is the registrar of a regional masters federation. Next season the competition splits in two: <strong>Junior Masters</strong> for lifters under 55 and <strong>Senior Masters</strong> for lifters 55 and over, each with its own medals and team ranking. She has last year's Masters database and decides to reuse it.</p>
<p>The database is a real masters competition reduced to 120 athletes: 10 per gender in each 5-year slice from 35 to 64, in the 2026 weight categories. It has one masters championship, <strong>Masters</strong>, with the age groups M30 to M90 and W30 to W90, all active. Nobody is in the 30 groups or in the groups of 65 and over. Ages are inclusive: M55 covers 55 to 59, M60 covers 60 to 64.</p>
<p>Following Marie, this tutorial shows:</p>
<ul>
  <li>how a championship rename carries its age groups along;</li>
  <li>how age groups move from one championship to another;</li>
  <li>why an age group code can be refused, and how new age groups start inactive;</li>
  <li>the age gap warning, when activating or deleting leaves a hole in the ages of a championship;</li>
  <li>what happens to the athletes when an age group is deleted;</li>
  <li>why the athletes have to be recomputed into a new championship, and the dry run that comes first;</li>
  <li>renaming an age group code once it is free;</li>
  <li>what each of these changes leaves in the audit log.</li>
</ul>
<p>Marie makes mistakes along the way; the tutorial shows how the program catches them or shows her what she is about to do.</p>`;

const steps = {
	S0: {
		title: 'Where Marie starts',
		intro: `<p>Before changing anything, Marie looks at the championships of the competition.</p>`,
		takeaway: '',
	},
	S1: {
		title: '1. Renaming Masters to “Senior Masters”',
		intro: `<p>Rather than create Senior Masters from scratch, Marie renames Masters: its age groups 55 and over are exactly what Senior Masters needs. She goes to <em>Preparation › Edit Age Groups › Edit Championships</em>.</p>`,
		takeaway: `<div class="takeaway"><p>A championship rename carries its age groups and their athletes along: nobody loses a participation. The audit log records one <code>championship.rename</code> entry, Masters to Senior Masters, listing the age groups renamed.</p></div>`,
	},
	S2: {
		title: '2. Creating “Junior Masters” and moving the younger age groups',
		intro: `<p>Junior Masters needs the age groups under 55. Marie creates the championship, then moves the age groups into it with <em>Select Age Groups</em>. A new championship has no athletes yet, so it stays hidden until <em>Hide Empty Championships</em> is unticked.</p>`,
		takeaway: `<div class="mistake"><p>Marie read “under 55” loosely: M55, M60, W55 and W60 are now in Junior Masters. Senior Masters will need them.</p></div>
<div class="takeaway"><p>Moving an age group only changes its championship: the athletes move with it, and nobody is reassigned. The audit log records one <code>agegroup.change</code> on <code>championship</code> per age group moved. The age group list follows the order of the championships: Junior Masters, added last, would come at the bottom, which is why Marie drags it above Senior Masters.</p></div>`,
	},
	S3: {
		title: '3. Creating the age groups Senior Masters is missing',
		intro: `<p>“Senior Masters should start at 55, and there is nothing there.” Marie adds the age group for 55 to 59 to Senior Masters, then the one for 60 to 64, men and women, with the same weight categories as the 65 group. Creating an age group raises no warning: it starts inactive, and inactive age groups do not count towards the ages a championship covers. The warning comes when she activates one, so she activates each age group right after creating it. For the men she slips twice: she types 58 instead of 59 as the maximum age of the 55 group, and 65 instead of 64 for the 60 group.</p>`,
		takeaway: `<div class="takeaway"><p>An age group code and gender must be unique across all championships: the M55 in Junior Masters blocks a new M55 in Senior Masters. New age groups start inactive. The age gap warning comes from activating an age group (or editing or deleting one), never from creating it, because inactive age groups do not count. No other page shows which ages a championship covers, so activating is where Marie sees it: activating the men's 55-58 group leaves 59 to 64 uncovered, because the 60 group does not exist yet (the women's 55-59 group leaves 60 to 64); activating the men's 60-65 group leaves 59 uncovered and overlaps M65 at age 65, and the dialog lists both. Overlaps are always reported when an age group is activated, as a warning like a gap, with Continue and Cancel. Marie continues, then corrects the two ages to 59 and 64. Each time the program checks, it lists all the gaps and all the overlaps of the championship, so she always sees what is left to fix: after correcting the 55 group the overlap at 65 is still reported, and after correcting the 60 group nothing is left and the save raises no warning. The audit log records an <code>agegroup.add</code> for each age group created, and an <code>agegroup.change</code> on <code>active</code> for each activation.</p></div>`,
	},
	S4: {
		title: '4. Trimming Junior Masters from the middle: the mistake',
		intro: `<p>Junior Masters still has M55, M60, W55 and W60, which Senior Masters now covers. Marie decides to delete them and starts with the first she sees, <strong>M55</strong>: she opens it, presses Delete and confirms <em>Delete M55?</em>.</p>`,
		takeaway: `<div class="takeaway"><p>Before deleting, the program shows the hole the delete would open in Junior Masters (55 to 59, with M60 above it) together with the athletes who would leave Junior Masters. Cancel changes nothing, and nothing is written to the audit log.</p></div>`,
	},
	S5: {
		title: '5. Trimming Junior Masters from the top',
		intro: `<p>Marie starts again from the top: M60, then M55, which is then the top, then W60 and W55.</p>`,
		takeaway: `<div class="takeaway"><p>Deleting the highest age group opens no hole, so there is no age gap warning, only the list of the athletes who leave Junior Masters. After Delete, the age group is gone and its athletes remain registered. Junior Masters now covers 30 to 54 for both genders, with no hole. The audit log records an <code>agegroup.delete</code> with the full definition of the age group, followed by an <code>athlete.change</code> for each athlete whose categories changed, in the log of the athlete's platform.</p></div>`,
	},
	S6: {
		title: '6. Putting the athletes of 55 and over into Senior Masters',
		intro: `<p>Marie checks the athletes registered in Senior Masters. The lifters of 55 to 64 are missing: deleting an age group reassigns athletes only inside its own championship, so they left Junior Masters without entering Senior Masters.</p>`,
		takeaway: `<div class="takeaway"><p>Changes to one age group never add athletes to another championship; <em>Recompute All Eligibilities</em> does, after a dry run that shows every change. Athletes under 55 are untouched. The audit log records an <code>agegroup.recompute-all</code> with the number of athletes changed, followed by their <code>athlete.change</code> entries.</p></div>`,
	},
	S7: {
		title: '7. Giving the new age groups their proper names',
		intro: `<p><code>VM55</code> and <code>VM60</code> were only there to avoid the clash. Now that Junior Masters no longer has M55 and M60, Marie renames them, and the women's as well.</p>`,
		takeaway: `<div class="takeaway"><p>A code change is saved directly, with no dialog. The age group keeps its athletes, and the category codes follow (<code>VM55 85</code> becomes <code>M55 85</code>). Senior Masters now has M55, M60, M65 and up; Junior Masters has M30 to M50. The audit log records an <code>agegroup.change</code> on <code>code</code> and one on <code>categories</code> for each rename.</p></div>`,
	},
};

/** What Marie does and what the screenshot shows, from its name. */
const captions = [
	[/^S0-initial$/, () => 'The championships page as Marie finds it: Masters is there, and there is no Junior Masters or Senior Masters yet.'],
	[/^S1-initial$/, () => 'Marie unticks Hide Empty Championships so that every championship is listed. Masters is the one she will rename.'],
	[/^S1-form$/, () => 'She presses Edit Medals and Team Rules on the Masters row and types Senior Masters as the name. Update is next.'],
	[/^S1-renamed$/, () => 'After Update, Senior Masters is listed where Masters was.'],
	[/^S1-final$/, () => 'On the age groups page, filtering on M30 shows that the age group now belongs to Senior Masters.'],
	[/^S2-add$/, () => 'Marie presses Add and fills in the new championship: name Junior Masters, type “Masters - code includes gender”.'],
	[/^S2-selection$/, () => 'On the Junior Masters row she presses Select Age Groups and ticks M30 to M60 and W30 to W60. The 55 and 60 groups are ticked too: that is her mistake.'],
	[/^S2-moved$/, () => 'Update moves the age groups at once, with no confirmation: the dialog closes and Junior Masters is listed with its age groups.'],
	[/^S2-reordered$/, () => 'Marie drags the handle of the Junior Masters row (the three bars at the left) onto the upper edge of the Senior Masters row. Junior Masters is now listed first.'],
	[/^S2-final$/, () => 'Filtering the age groups on M6 shows the result: M60 is in Junior Masters, M65 and up stayed in Senior Masters.'],
	[/^S3-(M|W)55-refused$/, m => `She adds ${m[1]}55 to Senior Masters. The code and gender fields are refused: the combination is already used, by the ${m[1]}55 now in Junior Masters. She cancels.`],
	[/^S3-(V[MW]55)-created$/, m => `She settles on a prefix. ${m[1]} is created in Senior Masters${m[1] === 'VM55' ? ', with 58 instead of 59 as the maximum age' : ''}. No warning appears: Active is unticked, and inactive age groups do not count towards the ages Senior Masters covers.`],
	[/^S3-(V[MW]60)-created$/, m => `Next she creates ${m[1]} the same way, in Senior Masters, inactive${m[1] === 'VM60' ? ', but types 65 instead of 64 as the maximum age' : ''}.`],
	[/^S3-VM60-gap-continue$/, () => 'She ticks Active on VM60. The dialog now lists two problems: Senior Masters has no active age group for men aged 59 (the 55 group stops at 58), and VM60 overlaps M65 at age 65. Both are allowed, so Marie presses Continue and then corrects the two ages.'],
	[/^S3-VM55-fix-editor$/, () => 'She opens VM55 and corrects the maximum age to 59. Update is next.'],
	[/^S3-VM55-fix-gap-continue$/, () => 'The gap at 59 is gone, but the program still lists what is left to fix: VM60 overlaps M65 at age 65. She presses Continue and corrects VM60 next.'],
	[/^S3-VM60-fix-editor$/, () => 'Then VM60: the maximum age becomes 64. Nothing is left to report, so saving raises no warning.'],
	[/^S3-(V[MW]55)-gap-continue$/, m => `She ticks Active on ${m[1]} right away, and now the program checks the ages of Senior Masters. It warns that Senior Masters has no active age group for ages ${m[1] === 'VM55' ? '59 to 64 (the 55 group stops at 58)' : '60 to 64'}: there is no 60 group yet. She is about to create it and presses Continue.`],
	[/^S3-(V[MW]\d\d)-reassign-apply$/, m => `Ticking Active on ${m[1]} asks to reassign the athletes of Senior Masters; she applies.`],
	[/^S3-(V[MW]60)-no-dialog$/, m => `She ticks Active on ${m[1]}. The gap is closed and nothing overlaps, so no warning appears, and ${m[1]} is active.`],
	[/^S3-final$/, () => 'Filtering on V shows the four new age groups of Senior Masters, all active.'],
	[/^S4-([MW]55)-dialog$/, m => `She opens ${m[1]} in Junior Masters and presses Delete. One dialog shows the hole the delete would open, ages 55 to 59 in Junior Masters, and below it the athletes who would leave Junior Masters. Seeing the hole, she presses Cancel.`],
	[/^S4-([MW]55)-kept$/, m => `Nothing changed: ${m[1]} is still in Junior Masters.`],
	[/^S5-([MW]\d\d)-dialog$/, m => `She deletes ${m[1]}, the highest age group left in Junior Masters. No hole is opened, so the dialog only lists the athletes who leave Junior Masters, “Removed (age)”. Lifters entered below their qualifying total can be listed too: any reassignment removes them. She presses Delete.`],
	[/^S5-([MW])(\d\d)-deleted$/, m => `${m[1]}${m[2]} is gone; Junior Masters' ${m[1] === 'M' ? 'men' : 'women'} age groups now end at ${m[1]}${Number(m[2]) - 5}.`],
	[/^S5-final$/, () => 'With the 55 and 60 groups gone for both genders, M50 is the top of Junior Masters: it covers 30 to 54, with no hole.'],
	[/^S6-before$/, () => 'The athletes page filtered on Senior Masters: none of the 55 to 64 lifters is there.'],
	[/^S6-dry-run$/, () => 'On the age groups page Marie presses Recompute All Eligibilities. Before changing anything, the program lists what would happen: the 55 to 64 lifters are added to Senior Masters in VM55, VM60, VW55 or VW60. She applies.'],
	[/^S6-final$/, () => 'The athletes page filtered on Senior Masters now lists them, in the VM and VW categories.'],
	[/^S7-(V([MW])(\d\d))-editor$/, m => `She opens ${m[1]} and changes its code to ${m[2]}${m[3]}. Update is next.`],
	[/^S7-groups$/, () => 'Filtering the age groups on M: M55, M60 and up in Senior Masters; M30 to M50 in Junior Masters.'],
	[/^S7-final$/, () => 'On the athletes page filtered on Senior Masters, the categories followed the rename: M55, M60, W55 and W60; no VM or VW is left.'],
	[/-FAILURE$/, () => 'The screen when the automated run failed.'],
];

const caption = file => {
	const name = file.replace(/\.png$/, '');
	for (const [re, text] of captions) {
		const m = name.match(re);
		if (m) return text(m);
	}
	return '';
};

const howMade = `
<p>The screenshots were taken by an automated run of the steps above (<code>old-very-old.scenario.js</code>, run with <code>node run-old-very-old.cjs</code> against a server loaded with <code>oldVeryOldMasters_2026.json</code>), in a 1400×900 browser. The run checks every step on the page itself; its checks, screenshots and audit entries are in the <a href="old-very-old/REPORT.md">test report</a>. This page is regenerated from <code>old-very-old.story.cjs</code> at each run.</p>`;

module.exports = { title, lead, intro, steps, caption, howMade };
