package app.owlcms.data.group;

import org.apache.commons.lang3.ObjectUtils;

class BWCatInfo implements Comparable<BWCatInfo> {
	int maxWeight;
	String limitString;
	String subCat;

	public BWCatInfo(int maxWeight, String limitString, String subCat) {
		super();
		this.maxWeight = maxWeight;
		this.limitString = limitString;
		this.subCat = subCat != null && !subCat.isBlank() ? subCat : "A";
	}

	@Override
	public int compareTo(BWCatInfo o) {
		int compare = 0;
		compare = ObjectUtils.compare(this.maxWeight, o.maxWeight);
		if (compare != 0)
			return compare;
		String subCat2 = this.subCat;
		String subCat3 = o.subCat;
		compare = ObjectUtils.compare(subCat2, subCat3);
		return -compare; // A is better than B, listed after.
	}

	public String getFormattedString() {
		return limitString+subCat;
	}
	
	public String getKey() {
		return String.format("%03d%s", maxWeight, subCat);
	}
}
