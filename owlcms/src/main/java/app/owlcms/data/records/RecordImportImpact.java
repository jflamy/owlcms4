/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.records;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary of the impact a pending record file import will have on the database.
 * Computed in memory before any persistence occurs (preview step).
 */
public class RecordImportImpact {

	private int totalImported;
	private int officialImported;
	private int provisionalImported;
	private int officialToReplace;
	private int provisionalToRemove;
	private int duplicateProvisionalToSkip;
	private final List<RecordImportImpactRow> rows = new ArrayList<>();

	public RecordImportImpact() {
	}

	// ---- totals ----

	public int getTotalImported() {
		return totalImported;
	}

	public void setTotalImported(int totalImported) {
		this.totalImported = totalImported;
	}

	public int getOfficialImported() {
		return officialImported;
	}

	public void setOfficialImported(int officialImported) {
		this.officialImported = officialImported;
	}

	public int getProvisionalImported() {
		return provisionalImported;
	}

	public void setProvisionalImported(int provisionalImported) {
		this.provisionalImported = provisionalImported;
	}

	public int getOfficialToReplace() {
		return officialToReplace;
	}

	public void setOfficialToReplace(int officialToReplace) {
		this.officialToReplace = officialToReplace;
	}

	public int getProvisionalToRemove() {
		return provisionalToRemove;
	}

	public void setProvisionalToRemove(int provisionalToRemove) {
		this.provisionalToRemove = provisionalToRemove;
	}

	public int getDuplicateProvisionalToSkip() {
		return duplicateProvisionalToSkip;
	}

	public void setDuplicateProvisionalToSkip(int duplicateProvisionalToSkip) {
		this.duplicateProvisionalToSkip = duplicateProvisionalToSkip;
	}

	public List<RecordImportImpactRow> getRows() {
		return rows;
	}

	public void addRow(RecordImportImpactRow row) {
		this.rows.add(row);
	}
}
