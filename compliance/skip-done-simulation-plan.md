<!-- markdownlint-disable -->
# Implementation Plan: "Skip Done Groups" Simulation Mode

## Goal

Add a startup flag to the competition simulator so it can:
1. **Skip** sessions already marked `Group.isDone()`.
2. **Defer** the fake weigh-in + declarations to the moment each group actually starts (instead of a massive up-front pass), so existing results are NOT wiped.

This supports testing "mid-competition addition" of athletes: already-finished sessions keep their results, and only the not-done sessions get simulated.

## Hard Rules (read first)

- Do NOT run `mvn` or any build. After editing Java, only validate with the IDE error checker (`get_errors`). The workspace uses a JBR runtime; if classpath looks stale, ask the user to reload the window.
- Do NOT use fully-qualified class names in new code. Add an `import` and use the short name.
- Format all shell commands for bash.
- Do NOT commit or push. Leave that to the human.
- Use `logger.warn(...)` for any temporary debug logging.
- Keep changes minimal and scoped to the files listed below. Do not refactor unrelated code.

## Files in scope

1. `owlcms/src/main/java/app/owlcms/endpoints/SimulationServlet.java`
2. `owlcms/src/main/java/app/owlcms/simulation/CompetitionSimulator.java`
3. `owlcms/src/main/java/app/owlcms/simulation/FOPSimulator.java`

---

## Step 1 — Read the current code

Before editing, read these methods so the edits match exactly:
- `SimulationServlet.processRequest(...)` and `SimulationServlet.startSimulation()`.
- `CompetitionSimulator.runSimulation()`, `CompetitionSimulator.clearLifts()`, `CompetitionSimulator.weighIn(Group)`, and the constructor.
- `FOPSimulator` constructor and `FOPSimulator.startNextGroup(List<Group>)`.

Key facts already verified (do not re-investigate, just rely on them):
- `CompetitionSimulator.runSimulation()` currently calls `clearLifts()` unconditionally (wipes ALL athletes' lifts). This MUST be skipped in skip-done mode.
- `FieldOfPlay.loadGroup(...)` filters athletes with `AthleteRepository.findAllByGroupAndWeighIn(group, true)` — i.e. **weighed-in only**. So deferred weigh-in must happen BEFORE the `SwitchGroup` event is posted, or the group loads empty.
- In `FOPSimulator.startNextGroup`, the order today is: post `SwitchGroup` → `testAssignStartNumbers` → post `StartLifting`. The new weigh-in must run at the very top, before `SwitchGroup`.
- `Group.isDone()` is a persisted boolean; `Group.doDone()` only flips it when every weighed-in athlete is done.

---

## Step 2 — Thread a `skipDone` flag from the servlet

In `SimulationServlet.java`:

1. In `processRequest(...)`, when the action is `start`, read the checkbox parameter (an HTML checkbox sends its value only when ticked):
   ```java
   boolean skipDone = "on".equalsIgnoreCase(request.getParameter("skipDone"))
           || "true".equalsIgnoreCase(request.getParameter("skipDone"));
   ```
2. Change `startSimulation()` to accept `boolean skipDone` and pass it into the simulator:
   ```java
   new CompetitionSimulator(skipDone).runSimulation();
   ```
   Update the call site in `processRequest` accordingly: `message = startSimulation(skipDone);`.
3. In `writePage(...)`, add a labeled checkbox **inside the existing `<form>`**, before the Start button, so the flag is a real UI control on the launch page. The checkbox name must be `skipDone`; when ticked the browser submits `skipDone=on` with the form (and thus with the `Start` button click):
   ```java
   pw.println("<form method='post' action=''>");
   // add the checkbox before the buttons:
   pw.println("<p><label><input type='checkbox' name='skipDone' value='on'> "
           + "Skip sessions already done (defer weigh-in, keep existing results)</label></p>");
   pw.println("<button type='submit' name='action' value='start'>Start</button>");
   pw.println("<button type='submit' name='action' value='stop'>Stop</button>");
   pw.println("</form>");
   ```
   Keep the Stop button as-is; the `skipDone` checkbox only affects Start.
4. After a start, reflect the chosen mode in the status line so the operator gets feedback, e.g. include the mode in the `message` returned by `startSimulation(...)` ("Simulation start requested (skip done)." vs "Simulation start requested.").

> Note: `startSimulation()` and `simulationThread` are `static`. Keep them static; just add the parameter.
> Do NOT introduce a Vaadin view; the simulation launch page is this servlet-rendered HTML page.

---

## Step 3 — Add the flag to `CompetitionSimulator`

In `CompetitionSimulator.java`:

1. Add a field and constructor overload (keep the existing no-arg constructor for compatibility):
   ```java
   private final boolean skipDone;

   public CompetitionSimulator() {
       this(false);
   }

   public CompetitionSimulator(boolean skipDone) {
       this.skipDone = skipDone;
   }
   ```

2. In `runSimulation()`:
   - **Skip done groups** when building the group list `gs`. After the existing sort, filter:
     ```java
     if (this.skipDone) {
         gs = gs.stream().filter(g -> !g.isDone()).collect(Collectors.toList());
     }
     ```
   - **Do not clear lifts** in skip-done mode:
     ```java
     if (!this.skipDone) {
         clearLifts();
     }
     ```
   - **Defer weigh-in**: in skip-done mode, do NOT call `weighIn(g)` in the up-front loop. The emptiness test must then use the registration query instead of the weighed-in query. Replace the per-group logic so that:
     - Normal mode (unchanged): `weighIn(g)`, then `as = findAllByGroupAndWeighIn(g, true)`, skip if empty.
     - Skip-done mode: `as = AthleteRepository.findAllByGroupAndWeighIn(g, null)` (registration; no weigh-in requirement). Skip the group only if `as.size() == 0`. Do NOT weigh in here.
   - The platform-bucketing logic below stays the same; it only depends on `as.size() > 0` and `g.getPlatform()`.

3. Make `weighIn(Group)` reusable from `FOPSimulator`:
   - Change its visibility from `private` to package-private (drop `private`) OR add a `static` helper. Simplest: change signature to
     ```java
     List<Athlete> weighIn(Group g) { ... }   // package-private, same package as FOPSimulator
     ```
   - `FOPSimulator` is in the same package (`app.owlcms.simulation`), so package-private is sufficient. Do not make it public.

> Do NOT change the seeded-`Random` behavior inside `weighIn`. Leave the body as-is.

---

## Step 4 — Defer weigh-in into `FOPSimulator.startNextGroup`

In `FOPSimulator.java`:

1. Add a `skipDone` field and pass it through the constructor:
   ```java
   private final boolean skipDone;

   public FOPSimulator(FieldOfPlay f, List<Group> groups, boolean skipDone) {
       this.fop = f;
       this.groups = groups;
       this.skipDone = skipDone;
   }
   ```
   Keep a 2-arg constructor delegating with `false` if any other caller exists; otherwise update the single call site in `CompetitionSimulator.runSimulation()` to pass `this.skipDone`.

2. The simulator needs access to the weigh-in routine. Pass a reference to the owning `CompetitionSimulator` OR move `weighIn` to a `static` utility. **Preferred minimal approach**: give `FOPSimulator` the `CompetitionSimulator` instance, OR make `weighIn` `static` in `CompetitionSimulator` and call `CompetitionSimulator.weighIn(g)`. Choose `static` only if `weighIn` does not use instance state.
   - `weighIn` uses `this.r` (the seeded Random) for declarations. If made static, replace `this.r` with a local `Random` consistent with current behavior, or keep it instance-based and pass the `CompetitionSimulator` reference. **Pick the instance-reference approach** to avoid changing RNG semantics:
     ```java
     // CompetitionSimulator.runSimulation(), when constructing simulators:
     FOPSimulator fopSimulator = new FOPSimulator(f, groupsByPlatform.get(p), this.skipDone);
     fopSimulator.setSimulator(this);   // add a setter + field
     ```

3. In `startNextGroup(List<Group> curGs)`, at the very top of the `if (curGs != null && curGs.size() > 0)` block, BEFORE posting `SwitchGroup`:
   ```java
   Group g = curGs.get(0);
   if (this.skipDone && this.simulator != null) {
       this.simulator.weighIn(g);   // deferred fake weigh-in + declarations
   }
   ```
   Leave the rest (`SwitchGroup`, `testAssignStartNumbers`, `StartLifting`) unchanged. Because `loadGroup` re-queries weighed-in athletes after `SwitchGroup`, the deferred weigh-in is now visible.

> Ordering is critical: weigh-in must precede `this.fop.fopEventPost(new FOPEvent.SwitchGroup(g, this))`.

---

## Step 5 — Validate (no build)

1. Run the error checker on all three edited files:
   - `SimulationServlet.java`
   - `CompetitionSimulator.java`
   - `FOPSimulator.java`
   Confirm "No errors found" for each. Fix any compile errors before proceeding.

2. Confirm no fully-qualified class names were introduced; confirm needed imports are present:
   - `java.util.stream.Collectors` already imported in `CompetitionSimulator`.
   - `Athlete`, `Group`, `AthleteRepository` already imported where used.

3. Do NOT attempt to run the simulation or start the server. Report completion to the human with a short summary and the exact manual test instructions below.

---

## Manual test instructions (for the human, not the agent)

1. Start owlcms with an existing competition where some sessions are already done.
2. Open the `/simulation` page, tick the "Skip sessions already done" checkbox, and press **Start**.
3. Expect:
   - Done sessions are not re-run and keep their results.
   - Not-done sessions get weighed in at the moment they start, then lift.
   - Category rankings blend finished + newly simulated athletes.

---

## Acceptance criteria

- [ ] `skipDone` flag exposed as a labeled checkbox inside the `/simulation` page form, named `skipDone`.
- [ ] Ticking the checkbox and pressing Start runs the simulation in skip-done mode; leaving it unticked runs normal mode.
- [ ] The status/message on the page reflects which mode was started.
- [ ] In skip-done mode, `clearLifts()` is NOT called.
- [ ] In skip-done mode, `g.isDone()` groups are excluded from the run.
- [ ] In skip-done mode, weigh-in happens inside `startNextGroup` before `SwitchGroup`, not up front.
- [ ] Normal (no-flag) simulation behavior is byte-for-byte unchanged (same code path, same `clearLifts`, same up-front weigh-in).
- [ ] All three files report no compile errors.
- [ ] No build run, no commit, no push.

## Out of scope (do not do)

- No Vaadin view. The only UI is the checkbox added to the existing `/simulation` servlet page.
- No changes to `FieldOfPlay`, `Group`, `Athlete`, or `AthleteRepository`.
- No new tests required (there is no existing simulator test harness); do not add Vaadin-based tests.
- No documentation files beyond this plan.
