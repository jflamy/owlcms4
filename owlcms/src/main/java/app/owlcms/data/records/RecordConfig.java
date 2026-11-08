/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.records;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import app.owlcms.apputils.JpaJsonConverter;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.JpaTransactions;
import app.owlcms.data.jpa.SingletonSaves;
import app.owlcms.data.jpa.StaleSingletonException;
import app.owlcms.data.jpa.StaleSingletonException.Kind;
import ch.qos.logback.classic.Logger;

@Cacheable
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordConfig {

	static Logger logger = (Logger) LoggerFactory.getLogger(RecordConfig.class);
	private static final int RECORD_ORDER_LENGTH = 512;
	private static final int RECORD_ORDER_SAFE_LENGTH = RECORD_ORDER_LENGTH - 16;

	/** Read at each call (second-level cache allowed); creates the default row when there is none. */
	public static RecordConfig getCurrent() {
		RecordConfig rc = JPAService.runInTransaction(em -> em.find(RecordConfig.class, 1L));
		if (rc == null) {
			rc = createIfAbsent();
		}
		return withDefaults(rc);
	}

	private static RecordConfig withDefaults(RecordConfig rc) {
		if (rc.getRecordOrder() == null) {
			rc.setRecordOrder(new ArrayList<>());
		}
		if (rc.getShowAllCategoryRecords() == null) {
			rc.setShowAllCategoryRecords(false);
		}
		if (rc.getShowAllFederations() == null) {
			rc.setShowAllFederations(false);
		}
		return rc;
	}

	/** A fresh copy for a form, read from the database; it carries the stamp {@link #saveEdited} compares with the row. */
	public static RecordConfig loadForEdit() {
		RecordConfig rc = JpaTransactions.inTransaction(em -> SingletonSaves.findFresh(em, RecordConfig.class, 1L));
		if (rc == null) {
			rc = createIfAbsent();
		}
		return withDefaults(rc);
	}

	/**
	 * Saves a form's copy, unless the stored record settings were saved after the copy was loaded.
	 *
	 * @throws StaleSingletonException when they were saved since; nothing is written
	 */
	public static RecordConfig saveEdited(RecordConfig draft, AuditOrigin origin) {
		synchronized (SingletonSaves.LOCK) {
			RecordConfig merged = JpaTransactions.inTransaction(em -> {
				RecordConfig row = SingletonSaves.findFresh(em, RecordConfig.class, draft.id);
				if (row == null || !Objects.equals(row.getLastSavedAt(), draft.getLastSavedAt())) {
					throw new StaleSingletonException(Kind.RECORD_CONFIG, row != null ? row.getLastSavedBy() : null);
				}
				draft.stamp(origin, row.getLastSavedAt());
				return em.merge(draft);
			});
			return merged;
		}
	}

	/** Immediate change (no form): loads the stored row under the save lock, applies the change and saves. */
	public static RecordConfig apply(Consumer<RecordConfig> change, AuditOrigin origin) {
		synchronized (SingletonSaves.LOCK) {
			RecordConfig saved = JpaTransactions.inTransaction(em -> {
				RecordConfig row = SingletonSaves.findFresh(em, RecordConfig.class, 1L);
				if (row == null) {
					row = new RecordConfig();
					change.accept(row);
					row.stamp(origin, null);
					return em.merge(row);
				}
				change.accept(row);
				row.stamp(origin, row.getLastSavedAt());
				return row;
			});
			return saved;
		}
	}

	private static RecordConfig createIfAbsent() {
		synchronized (SingletonSaves.LOCK) {
			return JpaTransactions.inTransaction(em -> {
				RecordConfig row = SingletonSaves.findFresh(em, RecordConfig.class, 1L);
				if (row != null) {
					return row;
				}
				RecordConfig fresh = new RecordConfig();
				fresh.stamp(AuditOrigin.system("record settings created"), null);
				return em.merge(fresh);
			});
		}
	}

	/**
	 * Hibernate schema update never alters existing columns, so databases created with the
	 * former 255 default must be widened explicitly. Idempotent; works on H2 2.x and PostgreSQL.
	 */
	public static void widenRecordOrderColumn() {
		try {
			JPAService.runInTransaction(em -> {
				List<?> lengths = em.createNativeQuery(
				        "SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS"
				                + " WHERE LOWER(TABLE_NAME) = 'recordconfig' AND LOWER(COLUMN_NAME) = 'recordorder'")
				        .getResultList();
				boolean tooShort = lengths.stream()
				        .anyMatch(l -> l instanceof Number n && n.longValue() < RECORD_ORDER_LENGTH);
				if (tooShort) {
					em.createNativeQuery("ALTER TABLE RecordConfig ALTER COLUMN recordOrder SET DATA TYPE VARCHAR("
					        + RECORD_ORDER_LENGTH + ")").executeUpdate();
					logger.info("Widened RecordConfig.recordOrder column to {} characters", RECORD_ORDER_LENGTH);
				}
				return null;
			});
		} catch (Exception e) {
			logger.error("Could not widen RecordConfig.recordOrder column: {}", e.toString());
		}
	}

	@Column(length = RECORD_ORDER_LENGTH)
	@Convert(converter = JpaJsonConverter.class)
	private ArrayList<String> recordOrder;
	@Column(columnDefinition = "boolean default false")
	private Boolean showAllCategoryRecords;
	@Column(columnDefinition = "boolean default false")
	private Boolean showAllFederations;
	@Id
	Long id = 1L; // is a singleton. if we ever create a new one it should merge.
	/** Stale-save check: time of the last save of this row (epoch ms). Never exported. */
	@JsonIgnore
	private Long lastSavedAt;
	/** Stale-save check: who made the last save of this row. Never exported. */
	@JsonIgnore
	private String lastSavedBy;

	public RecordConfig() {
		this.recordOrder = new ArrayList<>();
	}

	/**
	 * Keeps the display order in line with the record names in the database: names no longer present are dropped,
	 * new names are added at the end so the current order is preserved. Saves only when the order changes.
	 *
	 * @return the saved settings when the order changed, otherwise this instance
	 */
	public RecordConfig addMissing(List<String> findAllRecordNames) {
		if (Objects.equals(mergedOrder(this.recordOrder, findAllRecordNames), this.recordOrder)) {
			return this;
		}
		return apply(rc -> rc.setRecordOrder(mergedOrder(rc.getRecordOrder(), findAllRecordNames)),
		        AuditOrigin.system("record order update"));
	}

	static ArrayList<String> mergedOrder(List<String> order, List<String> names) {
		List<String> available = names != null ? names : List.of();
		ArrayList<String> merged = new ArrayList<>();
		if (order != null) {
			for (String name : order) {
				if (available.contains(name) && !merged.contains(name)) {
					merged.add(name);
				}
			}
		}
		for (String name : available) {
			if (!merged.contains(name)) {
				merged.add(name);
			}
		}
		return merged;
	}

	/** The singleton row's id; not exported. */
	@JsonIgnore
	public Long getId() {
		return this.id;
	}

	/** When the stored row was last saved (epoch milliseconds); null if never saved since this check exists. */
	@JsonIgnore
	public Long getLastSavedAt() {
		return this.lastSavedAt;
	}

	/** Who made the last save of the stored row. */
	@JsonIgnore
	public String getLastSavedBy() {
		return this.lastSavedBy;
	}

	/**
	 * Marks this instance as saved now. Persistence use only (repository, import).
	 *
	 * @param previous the stamp of the row being replaced, null if none
	 */
	public void stamp(AuditOrigin origin, Long previous) {
		this.lastSavedAt = SingletonSaves.nextStamp(previous);
		this.lastSavedBy = SingletonSaves.savedByLabel(origin);
	}

	public static boolean normalizeImportedRecordNames(List<RecordEvent> records, RecordConfig recordConfig) {
		if (records == null || records.isEmpty()) {
			return false;
		}

		Set<String> recordNames = new LinkedHashSet<>();
		Set<String> federations = new LinkedHashSet<>();
		for (RecordEvent record : records) {
			if (record.getRecordName() != null && !record.getRecordName().isBlank()) {
				recordNames.add(record.getRecordName());
			}
			if (record.getRecordFederation() != null && !record.getRecordFederation().isBlank()) {
				federations.add(record.getRecordFederation());
			}
		}

		String serializedRecordNames = new JpaJsonConverter()
		        .convertToDatabaseColumn(new ArrayList<>(recordNames));
		if (federations.isEmpty() || serializedRecordNames == null
		        || serializedRecordNames.length() <= RECORD_ORDER_SAFE_LENGTH) {
			return false;
		}

		for (RecordEvent record : records) {
			if (record.getRecordFederation() != null && !record.getRecordFederation().isBlank()) {
				record.setRecordName(record.getRecordFederation());
			}
		}
		if (recordConfig != null) {
			recordConfig.setRecordOrder(new ArrayList<>(federations));
		}
		logger./**/warn("Normalized imported record order from {} characters and {} names to {} federation names",
		        serializedRecordNames.length(), recordNames.size(), federations.size());
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		RecordConfig other = (RecordConfig) obj;
		return Objects.equals(this.id, other.id) && Objects.equals(this.recordOrder, other.recordOrder)
		        && Objects.equals(this.showAllCategoryRecords, other.showAllCategoryRecords)
		        && Objects.equals(this.showAllFederations, other.showAllFederations);
	}

	@JsonIgnore
	public List<RecordEvent> getLoadedFiles() {
		return RecordRepository.findAllLoadedRecords();
	}

	public ArrayList<String> getRecordOrder() {
		return this.recordOrder;
	}

	public Boolean getShowAllCategoryRecords() {
		return this.showAllCategoryRecords;
	}

	public Boolean getShowAllFederations() {
		return this.showAllFederations;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id, this.recordOrder, this.showAllCategoryRecords, this.showAllFederations);
	}

	@JsonIgnore
	public void setLoadedFiles(List<RecordEvent> ignored) {
	}

	public void setRecordOrder(ArrayList<String> recordOrder) {
		this.recordOrder = recordOrder;
	}

	@JsonSetter
	public void setRecordOrder(List<String> asList) {
		this.recordOrder = new ArrayList<>(asList);
	}

	public void setShowAllCategoryRecords(Boolean b) {
		this.showAllCategoryRecords = b;
	}

	public void setShowAllFederations(Boolean b) {
		this.showAllFederations = b;
	}

}