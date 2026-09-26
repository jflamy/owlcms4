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

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import app.owlcms.apputils.JpaJsonConverter;
import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Logger;

@Cacheable
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordConfig {

	static Logger logger = (Logger) LoggerFactory.getLogger(RecordConfig.class);
	private static final int RECORD_ORDER_LENGTH = 512;
	private static final int RECORD_ORDER_SAFE_LENGTH = RECORD_ORDER_LENGTH - 16;
	private static RecordConfig current;

	public static RecordConfig getCurrent() {
		current = JPAService.runInTransaction(em -> em.find(RecordConfig.class, 1L));
		if (current == null) {
			current = new RecordConfig();
			setCurrent(current);
		}
		if (current.getRecordOrder() == null) {
			current.setRecordOrder(new ArrayList<>());
		}
		if (current.getShowAllCategoryRecords() == null) {
			current.setShowAllCategoryRecords(false);
		}
		if (current.getShowAllFederations() == null) {
			current.setShowAllFederations(false);
		}
		return current;
	}

	public static RecordConfig setCurrent(RecordConfig recordConfig) {
		RecordConfig merged = JPAService.runInTransaction(em -> {
			RecordConfig nc = em.merge(recordConfig);
			em.flush();
			return nc;
		});
		current = merged;
		return current;
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

	public RecordConfig() {
		this.recordOrder = new ArrayList<>();
	}

	public void addMissing(List<String> findAllRecordNames) {
		// get rid of items in list but not in database
		if (this.recordOrder != null && this.recordOrder.size() > 0) {
			ArrayList<String> extras = new ArrayList<>(this.recordOrder);
			extras.removeAll(findAllRecordNames);
			this.recordOrder.removeAll(extras);
			findAllRecordNames.removeAll(this.recordOrder);
		}
		if (this.recordOrder == null && findAllRecordNames != null && findAllRecordNames.size() > 0) {
			this.recordOrder = new ArrayList<>();
		}

		// new items in database but not in list, add them at the end to preserve
		// current sort order
		this.recordOrder.addAll(findAllRecordNames);
		setCurrent(this);
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

	@Transient
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

	@Transient
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