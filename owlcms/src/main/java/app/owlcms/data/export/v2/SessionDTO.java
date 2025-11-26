/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export.v2;

import java.time.LocalDateTime;

import javax.persistence.EntityManager;

import app.owlcms.data.group.Group;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;

/**
 * DTO for Group, renamed to Session in the V2 export format.
 * Uses platform name/code instead of ID reference.
 */
public class SessionDTO {
	
	private String name;
	private String platformName;  // Instead of platform ID
	private String description;
	private LocalDateTime weighInTime;
	private LocalDateTime competitionTime;
	private LocalDateTime firstSnatchTime;
	private LocalDateTime firstCJTime;
	private LocalDateTime lastSnatchDecisionTime;
	private LocalDateTime lastCJDecisionTime;
	private Integer cleanJerkBreakDuration;
	private Boolean done;
	private Boolean masters;
	
	// Officials
	private String announcer;
	private String marshall;
	private String marshal2;
	private String timeKeeper;
	private String technicalController;
	private String technicalController2;
	private String technicalController3;
	private String referee1;
	private String referee2;
	private String referee3;
	private String reserve;
	private String jury1;
	private String jury2;
	private String jury3;
	private String jury4;
	private String jury5;
	private String reserveJury;
	private String weighIn1;
	private String weighIn2;
	private String competitionDirector;
	private String competitionSecretary;
	private String competitionSecretary2;
	private String doctor;
	private String doctor2;
	private String doctor3;

	public SessionDTO() {
	}

	/**
	 * Convert from domain Group object to DTO
	 */
	public static SessionDTO fromGroup(Group group) {
		SessionDTO dto = new SessionDTO();
		dto.setName(group.getName());
		dto.setPlatformName(group.getPlatform() != null ? group.getPlatform().getName() : null);
		dto.setDescription(group.getDescription());
		dto.setWeighInTime(group.getWeighInTime());
		dto.setCompetitionTime(group.getCompetitionTime());
		dto.setFirstSnatchTime(group.getFirstSnatchTime());
		dto.setFirstCJTime(group.getFirstCJTime());
		dto.setLastSnatchDecisionTime(group.getLastSnatchDecisionTime());
		dto.setLastCJDecisionTime(group.getLastCJDecisionTime());
		dto.setCleanJerkBreakDuration(group.getCleanJerkBreakDuration());
		dto.setDone(group.isDone());
		dto.setMasters(group.getMasters());
		
		dto.setAnnouncer(group.getAnnouncer());
		dto.setMarshall(group.getMarshall());
		dto.setMarshal2(group.getMarshal2());
		dto.setTimeKeeper(group.getTimeKeeper());
		dto.setTechnicalController(group.getTechnicalController());
		dto.setTechnicalController2(group.getTechnicalController2());
		dto.setTechnicalController3(group.getTechnicalController3());
		dto.setReferee1(group.getReferee1());
		dto.setReferee2(group.getReferee2());
		dto.setReferee3(group.getReferee3());
		dto.setReserve(group.getReserve());
		dto.setJury1(group.getJury1());
		dto.setJury2(group.getJury2());
		dto.setJury3(group.getJury3());
		dto.setJury4(group.getJury4());
		dto.setJury5(group.getJury5());
		dto.setReserveJury(group.getReserveJury());
		dto.setWeighIn1(group.getWeighIn1());
		dto.setWeighIn2(group.getWeighIn2());
		dto.setCompetitionDirector(group.getCompetitionDirector());
		dto.setCompetitionSecretary(group.getCompetitionSecretary());
		dto.setCompetitionSecretary2(group.getCompetitionSecretary2());
		dto.setDoctor(group.getDoctor());
		dto.setDoctor2(group.getDoctor2());
		dto.setDoctor3(group.getDoctor3());
		
		return dto;
	}

	/**
	 * Convert from DTO back to domain Group object
	 */
	public Group toGroup(EntityManager em) {
		Group group = new Group();
		group.setName(this.name);
		
		// Resolve platform by name
		if (this.platformName != null) {
			Platform platform = PlatformRepository.findByName(this.platformName);
			group.setPlatform(platform);
		}
		
		group.setDescription(this.description);
		group.setWeighInTime(this.weighInTime);
		group.setCompetitionTime(this.competitionTime);
		group.setFirstSnatchTime(this.firstSnatchTime, null);
		group.setFirstCJTime(this.firstCJTime, null);
		group.setLastSnatchDecisionTime(this.lastSnatchDecisionTime, null, null);
		group.setLastCJDecisionTime(this.lastCJDecisionTime, null, null);
		group.setCleanJerkBreakDuration(this.cleanJerkBreakDuration);
		group.setDone(this.done != null ? this.done : false);
		group.setMasters(this.masters);
		
		group.setAnnouncer(this.announcer);
		group.setMarshall(this.marshall);
		group.setMarshal2(this.marshal2);
		group.setTimeKeeper(this.timeKeeper);
		group.setTechnicalController(this.technicalController);
		group.setTechnicalController2(this.technicalController2);
		group.setTechnicalController3(this.technicalController3);
		group.setReferee1(this.referee1);
		group.setReferee2(this.referee2);
		group.setReferee3(this.referee3);
		group.setReserve(this.reserve);
		group.setJury1(this.jury1);
		group.setJury2(this.jury2);
		group.setJury3(this.jury3);
		group.setJury4(this.jury4);
		group.setJury5(this.jury5);
		group.setReserveJury(this.reserveJury);
		group.setWeighIn1(this.weighIn1);
		group.setWeighIn2(this.weighIn2);
		group.setCompetitionDirector(this.competitionDirector);
		group.setCompetitionSecretary(this.competitionSecretary);
		group.setCompetitionSecretary2(this.competitionSecretary2);
		group.setDoctor(this.doctor);
		group.setDoctor2(this.doctor2);
		group.setDoctor3(this.doctor3);
		
		return group;
	}

	// Getters and setters
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPlatformName() {
		return platformName;
	}

	public void setPlatformName(String platformName) {
		this.platformName = platformName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDateTime getWeighInTime() {
		return weighInTime;
	}

	public void setWeighInTime(LocalDateTime weighInTime) {
		this.weighInTime = weighInTime;
	}

	public LocalDateTime getCompetitionTime() {
		return competitionTime;
	}

	public void setCompetitionTime(LocalDateTime competitionTime) {
		this.competitionTime = competitionTime;
	}

	public LocalDateTime getFirstSnatchTime() {
		return firstSnatchTime;
	}

	public void setFirstSnatchTime(LocalDateTime firstSnatchTime) {
		this.firstSnatchTime = firstSnatchTime;
	}

	public LocalDateTime getFirstCJTime() {
		return firstCJTime;
	}

	public void setFirstCJTime(LocalDateTime firstCJTime) {
		this.firstCJTime = firstCJTime;
	}

	public LocalDateTime getLastSnatchDecisionTime() {
		return lastSnatchDecisionTime;
	}

	public void setLastSnatchDecisionTime(LocalDateTime lastSnatchDecisionTime) {
		this.lastSnatchDecisionTime = lastSnatchDecisionTime;
	}

	public LocalDateTime getLastCJDecisionTime() {
		return lastCJDecisionTime;
	}

	public void setLastCJDecisionTime(LocalDateTime lastCJDecisionTime) {
		this.lastCJDecisionTime = lastCJDecisionTime;
	}

	public Integer getCleanJerkBreakDuration() {
		return cleanJerkBreakDuration;
	}

	public void setCleanJerkBreakDuration(Integer cleanJerkBreakDuration) {
		this.cleanJerkBreakDuration = cleanJerkBreakDuration;
	}

	public Boolean getDone() {
		return done;
	}

	public void setDone(Boolean done) {
		this.done = done;
	}

	public Boolean getMasters() {
		return masters;
	}

	public void setMasters(Boolean masters) {
		this.masters = masters;
	}

	public String getAnnouncer() {
		return announcer;
	}

	public void setAnnouncer(String announcer) {
		this.announcer = announcer;
	}

	public String getMarshall() {
		return marshall;
	}

	public void setMarshall(String marshall) {
		this.marshall = marshall;
	}

	public String getMarshal2() {
		return marshal2;
	}

	public void setMarshal2(String marshal2) {
		this.marshal2 = marshal2;
	}

	public String getTimeKeeper() {
		return timeKeeper;
	}

	public void setTimeKeeper(String timeKeeper) {
		this.timeKeeper = timeKeeper;
	}

	public String getTechnicalController() {
		return technicalController;
	}

	public void setTechnicalController(String technicalController) {
		this.technicalController = technicalController;
	}

	public String getTechnicalController2() {
		return technicalController2;
	}

	public void setTechnicalController2(String technicalController2) {
		this.technicalController2 = technicalController2;
	}

	public String getTechnicalController3() {
		return technicalController3;
	}

	public void setTechnicalController3(String technicalController3) {
		this.technicalController3 = technicalController3;
	}

	public String getReferee1() {
		return referee1;
	}

	public void setReferee1(String referee1) {
		this.referee1 = referee1;
	}

	public String getReferee2() {
		return referee2;
	}

	public void setReferee2(String referee2) {
		this.referee2 = referee2;
	}

	public String getReferee3() {
		return referee3;
	}

	public void setReferee3(String referee3) {
		this.referee3 = referee3;
	}

	public String getReserve() {
		return reserve;
	}

	public void setReserve(String reserve) {
		this.reserve = reserve;
	}

	public String getJury1() {
		return jury1;
	}

	public void setJury1(String jury1) {
		this.jury1 = jury1;
	}

	public String getJury2() {
		return jury2;
	}

	public void setJury2(String jury2) {
		this.jury2 = jury2;
	}

	public String getJury3() {
		return jury3;
	}

	public void setJury3(String jury3) {
		this.jury3 = jury3;
	}

	public String getJury4() {
		return jury4;
	}

	public void setJury4(String jury4) {
		this.jury4 = jury4;
	}

	public String getJury5() {
		return jury5;
	}

	public void setJury5(String jury5) {
		this.jury5 = jury5;
	}

	public String getReserveJury() {
		return reserveJury;
	}

	public void setReserveJury(String reserveJury) {
		this.reserveJury = reserveJury;
	}

	public String getWeighIn1() {
		return weighIn1;
	}

	public void setWeighIn1(String weighIn1) {
		this.weighIn1 = weighIn1;
	}

	public String getWeighIn2() {
		return weighIn2;
	}

	public void setWeighIn2(String weighIn2) {
		this.weighIn2 = weighIn2;
	}

	public String getCompetitionDirector() {
		return competitionDirector;
	}

	public void setCompetitionDirector(String competitionDirector) {
		this.competitionDirector = competitionDirector;
	}

	public String getCompetitionSecretary() {
		return competitionSecretary;
	}

	public void setCompetitionSecretary(String competitionSecretary) {
		this.competitionSecretary = competitionSecretary;
	}

	public String getCompetitionSecretary2() {
		return competitionSecretary2;
	}

	public void setCompetitionSecretary2(String competitionSecretary2) {
		this.competitionSecretary2 = competitionSecretary2;
	}

	public String getDoctor() {
		return doctor;
	}

	public void setDoctor(String doctor) {
		this.doctor = doctor;
	}

	public String getDoctor2() {
		return doctor2;
	}

	public void setDoctor2(String doctor2) {
		this.doctor2 = doctor2;
	}

	public String getDoctor3() {
		return doctor3;
	}

	public void setDoctor3(String doctor3) {
		this.doctor3 = doctor3;
	}
}
