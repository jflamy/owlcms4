/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.owlcms.utils.DateTimeUtils;

/**
 * The birth dates of one registration spreadsheet. The day/month order is decided once for the whole column, by
 * evidence: a date with a part from 13 to 31 can only have that part as the day. Dates that give no evidence follow
 * the decision. See compliance/030-BIRTHDATE_LENIENT_PARSE.md.
 */
final class BirthDateColumn {

	enum Order {
		DMY, MDY
	}

	enum Kind {
		/** The year does not have 4 digits. */
		TWO_DIGIT_YEAR,
		/** Some dates can only be day-first and others only month-first. */
		CONFLICTING_ORDER,
		/** Not a date, or not an existing date in the decided order. */
		INVALID
	}

	/**
	 * @param address     cell address, or null for the whole column
	 * @param text        the cell text
	 * @param otherAddress for a conflict, the cell that proves the other order
	 * @param otherText   for a conflict, the text of that cell
	 */
	record Problem(Kind kind, String address, String text, String otherAddress, String otherText) {
	}

	private record Entry(String address, String text) {
	}

	private static final String DELIMITER = "[\\s/.\\-]+";
	private static final Pattern BARE_NUMBER = Pattern.compile("\\d+");
	private static final Pattern DELIMITED = Pattern
	        .compile("(\\d+)" + DELIMITER + "(\\d+)" + DELIMITER + "(\\d+)[\\s/.]*");

	private final Locale locale;
	private final List<Entry> entries = new ArrayList<>();
	private final List<Problem> problems = new ArrayList<>();
	private Order order;
	private boolean orderAssumed;
	private boolean resolved;

	BirthDateColumn(Locale locale) {
		this.locale = locale != null ? locale : Locale.ENGLISH;
	}

	void add(String address, String text) {
		if (text == null || text.isBlank()) {
			return;
		}
		this.entries.add(new Entry(address, text.trim()));
		this.resolved = false;
	}

	/** Votes on the order, then validates every date. */
	void resolve() {
		this.problems.clear();
		this.order = null;
		this.orderAssumed = false;

		Set<Entry> flagged = new HashSet<>();
		Entry dmyProof = null;
		Entry mdyProof = null;
		boolean needsOrder = false;
		for (Entry e : this.entries) {
			Parts p = parts(e.text());
			if (p == null) {
				continue;
			}
			if (!p.yearFirst() && p.yearDigits() != 4) {
				Kind kind = p.yearDigits() == 2 ? Kind.TWO_DIGIT_YEAR : Kind.INVALID;
				this.problems.add(new Problem(kind, e.address(), e.text(), null, null));
				flagged.add(e);
				continue;
			}
			if (p.yearFirst()) {
				continue;
			}
			if (isDayOnly(p.a()) && p.b() <= 12) {
				dmyProof = dmyProof != null ? dmyProof : e;
			} else if (isDayOnly(p.b()) && p.a() <= 12) {
				mdyProof = mdyProof != null ? mdyProof : e;
			} else if (p.a() <= 12 && p.b() <= 12 && p.a() != p.b()) {
				needsOrder = true;
			}
		}

		boolean conflict = dmyProof != null && mdyProof != null;
		if (conflict) {
			this.problems.add(new Problem(Kind.CONFLICTING_ORDER, dmyProof.address(), dmyProof.text(),
			        mdyProof.address(), mdyProof.text()));
		} else if (dmyProof != null) {
			this.order = Order.DMY;
		} else if (mdyProof != null) {
			this.order = Order.MDY;
		} else {
			this.order = localeOrder(this.locale);
			this.orderAssumed = needsOrder;
		}

		for (Entry e : this.entries) {
			if (flagged.contains(e) || BARE_NUMBER.matcher(e.text()).matches()) {
				continue;
			}
			Parts p = parts(e.text());
			boolean invalid = p == null || (!conflict && convert(p, this.order) == null);
			if (invalid) {
				this.problems.add(new Problem(Kind.INVALID, e.address(), e.text(), null, null));
			}
		}
		this.resolved = true;
	}

	boolean hasProblems() {
		ensureResolved();
		return !this.problems.isEmpty();
	}

	List<Problem> problems() {
		ensureResolved();
		return List.copyOf(this.problems);
	}

	/** The order used for the column; null if the votes conflict. */
	Order order() {
		ensureResolved();
		return this.order;
	}

	/** True when dates that depend on the order exist but no date proves it, so the locale order was assumed. */
	boolean isOrderAssumed() {
		ensureResolved();
		return this.orderAssumed;
	}

	/**
	 * @return an ISO date for a delimited date, otherwise the text unchanged (bare year, Excel serial, ISO). Only
	 *         meaningful when there are no problems.
	 */
	String normalize(String text) {
		ensureResolved();
		if (text == null) {
			return null;
		}
		Parts p = parts(text.trim());
		String converted = p != null ? convert(p, this.order) : null;
		return converted != null ? converted : text;
	}

	private void ensureResolved() {
		if (!this.resolved) {
			resolve();
		}
	}

	private record Parts(int a, int b, int c, boolean yearFirst, int yearDigits) {
	}

	/** Splits a delimited date; null for anything else, including bare numbers. */
	private static Parts parts(String text) {
		if (BARE_NUMBER.matcher(text).matches()) {
			return null;
		}
		Matcher m = DELIMITED.matcher(text);
		if (!m.matches()) {
			return null;
		}
		String t1 = m.group(1);
		String t2 = m.group(2);
		String t3 = m.group(3);
		if (t1.length() == 4) {
			return t2.length() <= 2 && t3.length() <= 2
			        ? new Parts(Integer.parseInt(t2), Integer.parseInt(t3), Integer.parseInt(t1), true, 4)
			        : null;
		}
		if (t1.length() > 2 || t2.length() > 2 || t3.length() > 4) {
			return null;
		}
		return new Parts(Integer.parseInt(t1), Integer.parseInt(t2), Integer.parseInt(t3), false, t3.length());
	}

	private static boolean isDayOnly(int v) {
		return v >= 13 && v <= 31;
	}

	private static Order localeOrder(Locale locale) {
		String pattern = DateTimeUtils.localizedShortDatePattern(locale);
		int d = pattern.indexOf('d');
		int m = pattern.indexOf('M');
		return d >= 0 && m >= 0 && m < d ? Order.MDY : Order.DMY;
	}

	/** @return the ISO date, or null if the date does not exist in this order. */
	private static String convert(Parts p, Order order) {
		if (!p.yearFirst() && p.yearDigits() != 4) {
			return null;
		}
		try {
			LocalDate date;
			if (p.yearFirst() || order == Order.MDY) {
				date = LocalDate.of(p.c(), p.a(), p.b());
			} else {
				date = LocalDate.of(p.c(), p.b(), p.a());
			}
			return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (DateTimeException e) {
			return null;
		}
	}
}
