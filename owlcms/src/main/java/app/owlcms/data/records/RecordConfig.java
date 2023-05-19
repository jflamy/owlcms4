package app.owlcms.data.records;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

	private static RecordConfig current;

	public static RecordConfig getCurrent() {
		current = JPAService.runInTransaction(em -> em.find(RecordConfig.class, 1L));
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
		if (recordOrder != null && recordOrder.size() > 0) {
			ArrayList<String> extras = new ArrayList<>(recordOrder);
			extras.removeAll(findAllRecordNames);
			recordOrder.removeAll(extras);
			findAllRecordNames.removeAll(recordOrder);
		}
		if (recordOrder == null && findAllRecordNames != null && findAllRecordNames.size() > 0) {
			recordOrder = new ArrayList<String>();
		}

		// new items in database but not in list, add them at the end to preserve
		// current sort order
		recordOrder.addAll(findAllRecordNames);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RecordConfig other = (RecordConfig) obj;
		return Objects.equals(id, other.id) && Objects.equals(recordOrder, other.recordOrder)
		        && Objects.equals(showAllCategoryRecords, other.showAllCategoryRecords)
		        && Objects.equals(showAllFederations, other.showAllFederations);
	}

	@Transient
	@JsonIgnore
	public List<RecordEvent> getLoadedFiles() {
		return RecordRepository.findAllLoadedRecords();
	}

	public ArrayList<String> getRecordOrder() {
		return recordOrder;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, recordOrder, showAllCategoryRecords, showAllFederations);
	}

	public Boolean getShowAllCategoryRecords() {
		return showAllCategoryRecords;
	}

	public Boolean getShowAllFederations() {
		return showAllFederations;
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
		showAllCategoryRecords = b;
	}

	public void setShowAllFederations(Boolean b) {
		showAllFederations = b;
	}

}