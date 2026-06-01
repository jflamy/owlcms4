/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.records;

/**
 * Per-bundle impact row for the import preview dialog.
 * Each row summarises one (federation, recordName, ageGrp) combination found in the file.
 */
public class RecordImportImpactRow {

	private String recordFederation;
	private String recordName;
	private String ageGrp;
	private int importedCount;
	private int officialToReplace;
	private int provisionalToRemove;
	private int duplicateProvisionalToSkip;

	public RecordImportImpactRow(String recordFederation, String recordName, String ageGrp) {
		this.recordFederation = recordFederation;
		this.recordName = recordName;
		this.ageGrp = ageGrp;
	}

	public String getRecordFederation() {
		return recordFederation;
	}

	public String getRecordName() {
		return recordName;
	}

	public String getAgeGrp() {
		return ageGrp;
	}

	public int getImportedCount() {
		return importedCount;
	}

	public void incrementImportedCount() {
		this.importedCount++;
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
}
