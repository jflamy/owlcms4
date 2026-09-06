import { css, html, LitElement, nothing } from 'lit';

class TeamScheduleEditor extends LitElement {
  static properties = {
    title: { type: String },
    sessionLabel: { type: String },
    clearLabel: { type: String },
    copyLabel: { type: String },
    pasteLabel: { type: String },
    saveLabel: { type: String },
    cancelLabel: { type: String },
    rolesJson: { type: String },
    roleLabelsJson: { type: String },
    scheduleJson: { type: String }
  };

  static styles = css`
    :host {
      display: block;
      box-sizing: border-box;
      height: 100%;
      padding: 1rem;
      color: var(--lumo-body-text-color);
    }

    .editor {
      display: flex;
      flex-direction: column;
      height: 100%;
      min-height: 22rem;
      gap: 1rem;
    }

    .toolbar {
      display: flex;
      align-items: center;
      gap: .5rem;
    }

    h1 {
      margin: 0 auto 0 0;
      font-size: var(--lumo-font-size-xl);
      font-weight: 600;
    }

    button {
      min-width: 5rem;
      min-height: 2.25rem;
      padding: 0 .75rem;
      border: 1px solid var(--lumo-contrast-20pct);
      border-radius: var(--lumo-border-radius-m);
      background: var(--lumo-base-color);
      color: var(--lumo-body-text-color);
      font: inherit;
      cursor: pointer;
    }

    button.primary {
      border-color: var(--lumo-primary-color);
      background: var(--lumo-primary-color);
      color: var(--lumo-primary-contrast-color);
    }

    button:disabled {
      cursor: default;
      opacity: .55;
    }

    .grid-wrap {
      overflow: auto;
      border: 1px solid var(--lumo-contrast-20pct);
      border-radius: var(--lumo-border-radius-m);
      background: var(--lumo-base-color);
    }

    table {
      border-collapse: collapse;
      width: 100%;
      min-width: 57rem;
      table-layout: fixed;
    }

    th,
    td {
      border-right: 1px solid var(--lumo-contrast-10pct);
      border-bottom: 1px solid var(--lumo-contrast-10pct);
      padding: 0;
    }

    th {
      position: sticky;
      top: 0;
      z-index: 1;
      min-width: 8rem;
      padding: .55rem .75rem;
      background: var(--lumo-contrast-5pct);
      text-align: left;
      font-weight: 600;
      user-select: none;
    }

    th.role-header {
      box-sizing: border-box;
      width: 9.44%;
      min-width: 0;
      max-width: none;
      padding-left: .25rem;
      padding-right: .25rem;
      cursor: grab;
      touch-action: none;
      text-align: center;
      white-space: normal;
      overflow-wrap: anywhere;
    }

    th.role-header.dragging {
      cursor: grabbing;
    }

    th.drag-over {
      box-shadow: inset 3px 0 var(--lumo-primary-color);
    }

    th.session,
    td.session {
      box-sizing: border-box;
      width: 15%;
      min-width: 0;
      position: sticky;
      left: 0;
      z-index: 2;
      min-width: 9rem;
      background: var(--lumo-base-color);
    }

    th.session {
      z-index: 3;
      background: var(--lumo-contrast-5pct);
    }

    td.session {
      padding: .4rem .75rem;
      font-weight: 600;
      white-space: nowrap;
    }

    input {
      box-sizing: border-box;
      width: 100%;
      min-width: 0;
      height: 2.5rem;
      border: 0;
      padding: .25rem .5rem;
      background: transparent;
      color: inherit;
      font: inherit;
      text-align: center;
    }

    input:focus {
      outline: 2px solid var(--lumo-primary-color);
      outline-offset: -2px;
      background: var(--lumo-primary-color-10pct);
    }
  `;

  constructor() {
    super();
    this.roles = [];
    this.roleLabels = {};
    this.rows = [];
    this.original = { roles: [], rows: [] };
    this.draggedRole = null;
    this.pointerTargetRole = null;
    this.pointerStartX = null;
    this.pointerDragging = false;
    this.dirty = false;
    this.boundPointerMove = event => this.movePointerDrag(event);
    this.boundPointerUp = () => this.finishPointerDrag();
  }

  updated(changedProperties) {
    if (changedProperties.has('rolesJson') || changedProperties.has('roleLabelsJson') || changedProperties.has('scheduleJson')) {
      this.roles = this.parseJson(this.rolesJson, []);
      this.roleLabels = this.parseJson(this.roleLabelsJson, {});
      this.rows = this.parseJson(this.scheduleJson, []);
      this.original = this.snapshot();
      this.dirty = false;
      this.requestUpdate();
    }
  }

  parseJson(value, fallback) {
    try {
      return value ? JSON.parse(value) : fallback;
    } catch (_) {
      return fallback;
    }
  }

  snapshot() {
    return JSON.parse(JSON.stringify({ roles: this.roles, rows: this.rows }));
  }

  updateCell(rowIndex, role, value) {
    this.rows[rowIndex].cells[role] = value;
    this.dirty = true;
    this.requestUpdate();
  }

  selectCell(event) {
    event.target.select();
  }

  clearTable() {
    this.rows.forEach(row => {
      this.roles.forEach(role => {
        row.cells[role] = '';
      });
    });
    this.dirty = true;
    this.requestUpdate();
  }

  paste(event, startRow, startColumn) {
    const clipboard = event.clipboardData?.getData('text/plain');
    if (!clipboard || !clipboard.includes('\t') && !clipboard.includes('\n')) {
      return;
    }
    event.preventDefault();
    const cells = this.clipboardCells(clipboard);
    this.applyPastedCells(cells, startRow, startColumn);
  }

  async copyToClipboard() {
    const headers = [this.sessionLabel, ...this.roles.map(role => this.roleLabels[role] ?? role)];
    const rows = this.rows.map(row => [row.session, ...this.roles.map(role => row.cells[role] ?? '')]);
    try {
      await navigator.clipboard.writeText([headers, ...rows].map(row => row.join('\t')).join('\n'));
    } catch (_) {
      // Browsers deny clipboard access outside a trusted user gesture.
    }
  }

  async pasteFromClipboard() {
    try {
      const clipboard = await navigator.clipboard.readText();
      if (clipboard) {
        this.applyPastedCells(this.clipboardCells(clipboard), 0, 0);
      }
    } catch (_) {
      // Browsers deny clipboard access outside a trusted user gesture.
    }
  }

  applyPastedCells(cells, startRow, startColumn) {
    const dataRows = this.isSessionHeader(cells[0]?.[0]) ? cells.slice(1) : cells;
    if (this.applyRowsBySession(dataRows)) {
      return;
    }
    dataRows.forEach((pastedRow, rowOffset) => {
      const row = this.rows[startRow + rowOffset];
      if (!row) return;
      pastedRow.forEach((value, columnOffset) => {
        const role = this.roles[startColumn + columnOffset];
        if (role) row.cells[role] = value;
      });
    });
    this.dirty = true;
    this.requestUpdate();
  }

  clipboardCells(clipboard) {
    return clipboard.replace(/\r/g, '').split('\n')
      .filter((row, index, all) => row !== '' || index < all.length - 1)
      .map(row => row.split('\t').map(value => value.trim()));
  }

  applyRowsBySession(dataRows) {
    if (!dataRows.length) {
      return false;
    }
    const rowsBySession = new Map(this.rows.map(row => [row.session, row]));
    if (!dataRows.every(values => rowsBySession.has(values[0]))) {
      return false;
    }
    dataRows.forEach(values => {
      const row = rowsBySession.get(values[0]);
      this.roles.forEach((role, index) => {
        row.cells[role] = values[index + 1] ?? '';
      });
    });
    this.dirty = true;
    this.requestUpdate();
    return true;
  }

  isSessionHeader(value) {
    return value === this.sessionLabel || value.toLowerCase() === 'session';
  }

  moveFocus(event, rowIndex, columnIndex) {
    const moves = {
      ArrowUp: [-1, 0],
      ArrowDown: [1, 0],
      ArrowLeft: [0, -1],
      ArrowRight: [0, 1]
    };
    const move = moves[event.key];
    if (!move) return;
    const nextRow = rowIndex + move[0];
    const nextColumn = columnIndex + move[1];
    if (nextRow < 0 || nextRow >= this.rows.length || nextColumn < 0 || nextColumn >= this.roles.length) return;
    event.preventDefault();
    this.shadowRoot.querySelector(`input[data-row="${nextRow}"][data-column="${nextColumn}"]`)?.focus();
  }

  startPointerDrag(event, role) {
    if (event.button !== 0) return;
    this.draggedRole = role;
    this.pointerStartX = event.clientX;
    this.pointerTargetRole = null;
    this.pointerDragging = false;
    window.addEventListener('pointermove', this.boundPointerMove);
    window.addEventListener('pointerup', this.boundPointerUp, { once: true });
  }

  movePointerDrag(event) {
    if (!this.draggedRole) return;
    if (Math.abs(event.clientX - this.pointerStartX) > 4) {
      this.pointerDragging = true;
      this.shadowRoot.querySelector(`th[data-role="${this.draggedRole}"]`)?.classList.add('dragging');
    }
    if (!this.pointerDragging) return;
    const headers = [...this.shadowRoot.querySelectorAll('th[data-role]')];
    const target = headers.find(header => {
      const bounds = header.getBoundingClientRect();
      return event.clientX >= bounds.left && event.clientX <= bounds.right;
    });
    this.shadowRoot.querySelectorAll('th.drag-over').forEach(header => header.classList.remove('drag-over'));
    this.pointerTargetRole = target?.dataset.role ?? null;
    if (target && this.pointerTargetRole !== this.draggedRole) {
      target.classList.add('drag-over');
    }
  }

  finishPointerDrag() {
    window.removeEventListener('pointermove', this.boundPointerMove);
    this.shadowRoot.querySelectorAll('th.drag-over').forEach(header => header.classList.remove('drag-over'));
    this.shadowRoot.querySelectorAll('th.dragging').forEach(header => header.classList.remove('dragging'));
    const draggedRole = this.draggedRole;
    const targetRole = this.pointerTargetRole;
    this.draggedRole = null;
    this.pointerTargetRole = null;
    this.pointerStartX = null;
    if (!this.pointerDragging || !targetRole) return;
    this.reorderColumns(draggedRole, targetRole);
  }

  reorderColumns(draggedRole, targetRole) {
    const sourceIndex = this.roles.indexOf(draggedRole);
    const targetIndex = this.roles.indexOf(targetRole);
    if (sourceIndex < 0 || targetIndex < 0 || sourceIndex === targetIndex) return;
    const roles = [...this.roles];
    roles.splice(sourceIndex, 1);
    roles.splice(targetIndex, 0, draggedRole);
    this.roles = roles;
    this.draggedRole = null;
    this.dirty = true;
    this.requestUpdate();
  }

  disconnectedCallback() {
    window.removeEventListener('pointermove', this.boundPointerMove);
    super.disconnectedCallback();
  }

  cancel() {
    const original = JSON.parse(JSON.stringify(this.original));
    this.roles = original.roles;
    this.rows = original.rows;
    this.dirty = false;
    this.requestUpdate();
  }

  async save() {
    const rows = this.rows.map(row => ({ ...row, id: row.id == null ? null : String(row.id) }));
    await this.$server.saveSchedule(JSON.stringify({ columns: this.roles, rows }));
    this.original = this.snapshot();
    this.dirty = false;
    this.requestUpdate();
  }

  render() {
    return html`
      <main class="editor">
        <header class="toolbar">
          <h1>${this.title}</h1>
          <button @click=${this.clearTable}>${this.clearLabel}</button>
          <button @click=${this.copyToClipboard}>${this.copyLabel}</button>
          <button @click=${this.pasteFromClipboard}>${this.pasteLabel}</button>
          <button @click=${this.cancel} ?disabled=${!this.dirty}>${this.cancelLabel}</button>
          <button class="primary" @click=${this.save} ?disabled=${!this.dirty}>${this.saveLabel}</button>
        </header>
        <div class="grid-wrap">
          <table>
            <thead>
              <tr>
                <th class="session">${this.sessionLabel}</th>
                ${this.roles.map(role => html`
                  <th class="role-header" data-role=${role}
                      @pointerdown=${event => this.startPointerDrag(event, role)}>
                    ${this.roleLabels[role] ?? role}
                  </th>
                `)}
              </tr>
            </thead>
            <tbody>
              ${this.rows.map((row, rowIndex) => html`
                <tr>
                  <td class="session">${row.session}</td>
                  ${this.roles.map((role, columnIndex) => html`
                    <td>
                      <input .value=${row.cells[role] ?? ''}
                             inputmode="numeric"
                             data-row=${rowIndex}
                             data-column=${columnIndex}
                             @input=${event => this.updateCell(rowIndex, role, event.target.value)}
                             @focus=${this.selectCell}
                             @paste=${event => this.paste(event, rowIndex, columnIndex)}
                             @keydown=${event => this.moveFocus(event, rowIndex, columnIndex)}>
                    </td>
                  `)}
                </tr>
              `)}
            </tbody>
          </table>
        </div>
      </main>
    `;
  }
}

customElements.define('team-schedule-editor', TeamScheduleEditor);