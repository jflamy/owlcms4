/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.LoggerFactory;

import app.owlcms.data.group.Group;
import app.owlcms.utils.DateTimeUtils;
import ch.qos.logback.classic.Logger;

/**
 * Used to read groups from spreadsheet and perform Excel-to-Java conversions
 *
 * @author Jean-François Lamy
 *
 */
public class RGroup {
	final Logger logger = (Logger) LoggerFactory.getLogger(RGroup.class);
	Group group = new Group();
	String groupName;
	String description;
	String jury1;
	String jury2;
	String jury3;
	String jury4;
	String jury5;
	String ref1;
	String ref2;
	String ref3;
	String marshall;
	String marshal2;
	String weighInTO1;
	String weighInTO2;
	String techController;
	String techController2;
	String announcer;
	String timekeeper;
	String reserve;
	String competitionTime;
	String weighinTime;
	String platform;

	public String getAnnouncer() {
		return this.announcer;
	}

	public String getCompetitionTime() {
		return this.competitionTime;
	}

	public String getDescription() {
		return this.description;
	}

	public Group getGroup() {
		return this.group;
	}

	public String getGroupName() {
		return this.groupName;
	}

	public String getJury1() {
		return this.jury1;
	}

	public String getJury2() {
		return this.jury2;
	}

	public String getJury3() {
		return this.jury3;
	}

	public String getJury4() {
		return this.jury4;
	}

	public String getJury5() {
		return this.jury5;
	}

	public String getMarshal2() {
		return this.marshal2;
	}

	public String getMarshall() {
		return this.marshall;
	}

	public String getPlatform() {
		return this.platform;
	}

	public String getRef1() {
		return this.ref1;
	}

	public String getRef2() {
		return this.ref2;
	}

	public String getRef3() {
		return this.ref3;
	}

	public String getReserve() {
		return this.reserve;
	}

	public String getTechController() {
		return this.techController;
	}

	public String getTechController2() {
		return this.techController2;
	}

	public String getTimekeeper() {
		return this.timekeeper;
	}

	public String getWeighinTime() {
		return this.weighinTime;
	}

	public String getWeighInTO1() {
		return this.weighInTO1;
	}

	public String getWeighInTO2() {
		return this.weighInTO2;
	}

	public void setAnnouncer(String announcer) {
		this.group.setAnnouncer(announcer);
		this.announcer = announcer;
	}

	public void setCompetitionTime(String cTime) {
		if (cTime == null || cTime.isBlank()) {
			this.competitionTime = "";
			return;
		}
		try {
			LocalDateTime parseExcelDateTime = DateTimeUtils.parseExcelFractionalDate(cTime);
			// brute force round to the closest minute due to rounding errors in Excel fractional date times.
			int seconds = parseExcelDateTime.getSecond();
			parseExcelDateTime = parseExcelDateTime.withSecond(0).withNano(0);
			if (seconds >= 30) {
				parseExcelDateTime = parseExcelDateTime.plusMinutes(1);
			}

			this.competitionTime = parseExcelDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE) + " "
			        + parseExcelDateTime.format(DateTimeFormatter.ISO_LOCAL_TIME);
			this.group.setCompetitionTime(parseExcelDateTime);
			this.logger.trace("is a numeric time {} {}", this.competitionTime, parseExcelDateTime);
		} catch (NumberFormatException e) {
			this.logger.trace("not a numeric time {}", cTime);
			this.group.setCompetitionTime(DateTimeUtils.parseISO8601DateTime(cTime));
			// if no Exception thrown, the time was correctly parsed
			this.competitionTime = cTime;
			return;
		}
	}

	public void setDescription(String description) {
		this.group.setDescription(description);
		this.description = description;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public void setGroupName(String groupName) {
		this.group.setName(groupName);
		this.groupName = groupName;
	}

	public void setJury1(String jury1) {
		this.group.setJury1(jury1);
		this.jury1 = jury1;
	}

	public void setJury2(String jury2) {
		this.group.setJury2(jury2);
		this.jury2 = jury2;
	}

	public void setJury3(String jury3) {
		this.group.setJury3(jury3);
		this.jury3 = jury3;
	}

	public void setJury4(String jury4) {
		this.group.setJury4(jury4);
		this.jury4 = jury4;
	}

	public void setJury5(String jury5) {
		this.group.setJury5(jury5);
		this.jury5 = jury5;
	}

	public void setMarshal2(String marshal2) {
		this.group.setMarshal2(marshal2);
		this.marshal2 = marshal2;
	}

	public void setMarshall(String marshall) {
		this.group.setMarshall(marshall);
		this.marshall = marshall;
	}

	public void setPlatform(String pName) {
		// the existing platforms are reused, processed as part of the upload loop
		// so we don't set the platform in the wrapped object.
		this.platform = pName;
	}

	public void setRef1(String ref1) {
		this.group.setReferee1(ref1);
		this.ref1 = ref1;
	}

	public void setRef2(String ref2) {
		this.group.setReferee2(ref2);
		this.ref2 = ref2;
	}

	public void setRef3(String ref3) {
		this.group.setReferee3(ref3);
		this.ref3 = ref3;
	}

	public void setReserve(String reserve) {
		this.group.setReserve(reserve);
		this.reserve = reserve;
	}

	public void setTechController(String techController) {
		this.group.setTechnicalController(techController);
		this.techController = techController;
	}

	public void setTechController2(String techController2) {
		this.group.setTechnicalController2(techController2);
		this.techController2 = techController2;
	}

	public void setTimekeeper(String timekeeper) {
		this.group.setTimeKeeper(timekeeper);
		this.timekeeper = timekeeper;
	}

	public void setWeighinTime(String wTime) {
		if (wTime == null || wTime.isBlank()) {
			this.weighinTime = "";
			return;
		}
		try {
			LocalDateTime parseExcelDateTime = DateTimeUtils.parseExcelFractionalDate(wTime);
			// brute force round to the closest minute due to rounding errors in Excel fractional date times.
			int seconds = parseExcelDateTime.getSecond();
			parseExcelDateTime = parseExcelDateTime.withSecond(0).withNano(0);
			if (seconds >= 30) {
				parseExcelDateTime = parseExcelDateTime.plusMinutes(1);
			}
			this.weighinTime = parseExcelDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE) + " "
			        + parseExcelDateTime.format(DateTimeFormatter.ISO_LOCAL_TIME);
			this.group.setWeighInTime(parseExcelDateTime);
			this.logger.trace("is a numeric time {} {}", this.weighinTime, parseExcelDateTime);
		} catch (NumberFormatException e) {
			this.group.setWeighInTime(DateTimeUtils.parseISO8601DateTime(wTime));
			this.logger.trace("not a numeric time {}", wTime);
			// if no Exception thrown, the time was correctly parsed
			this.competitionTime = wTime;
			return;
		}

	}

	public void setWeighInTO1(String weighInTO1) {
		this.group.setWeighIn1(weighInTO1);
		this.weighInTO1 = weighInTO1;
	}

	public void setWeighInTO2(String weighInTO2) {
		this.group.setWeighIn2(weighInTO2);
		this.weighInTO2 = weighInTO2;
	}

}
