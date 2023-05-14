package app.owlcms.data.records;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;

import app.owlcms.apputils.JpaJsonConverter;
import app.owlcms.data.jpa.JPAService;

@Entity
public class RecordConfig {
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

	public void setRecordOrder(List<String> asList) {
		this.recordOrder = new ArrayList<String>(asList);
	}

	public static RecordConfig getCurrent() {
		return JPAService.runInTransaction(em -> em.find(RecordConfig.class, 1l));
	}

}