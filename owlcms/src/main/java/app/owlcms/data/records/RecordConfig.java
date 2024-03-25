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