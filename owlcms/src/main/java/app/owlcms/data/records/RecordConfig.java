package app.owlcms.data.records;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonSetter;

import app.owlcms.apputils.JpaJsonConverter;
import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Logger;

@Entity
public class RecordConfig {

	Logger logger = (Logger) LoggerFactory.getLogger(RecordConfig.class);

	public RecordConfig() {
		this.recordOrder = new ArrayList<String>();
	}

	public RecordConfig(List<String> asList) {
		this.recordOrder = new ArrayList<String>(asList);
	}

	@Id
	@Column(name = "ID")
	public Long getId() {
		return 1l;
	}

	public void setId(Long id) {
	}

	@Convert(converter = JpaJsonConverter.class)
	private ArrayList<String> recordOrder;

	public ArrayList<String> getRecordOrder() {
		return recordOrder;
	}

	public void setRecordOrder(ArrayList<String> recordOrder) {
		this.recordOrder = recordOrder;
	}

	@JsonSetter
	public void setRecordOrder(List<String> asList) {
		this.recordOrder = new ArrayList<String>(asList);
	}

	public static RecordConfig getCurrent() {
		return JPAService.runInTransaction(em -> em.find(RecordConfig.class, 1l));
	}

	public void addMissing(List<String> findAllRecordNames) {
		// get rid of items in list but not in database
		ArrayList<String> extras = new ArrayList<>(recordOrder);
		extras.removeAll(findAllRecordNames);
		recordOrder.removeAll(extras);
		
		// new items in database but not in list, add them at the end to preserve current sort order
		findAllRecordNames.removeAll(recordOrder);
		recordOrder.addAll(findAllRecordNames);
	}

}