/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.spreadsheet.RAthlete;
import app.owlcms.spreadsheet.RCompetition;
import app.owlcms.spreadsheet.RGroup;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.sf.jxls.reader.ReaderBuilder;
import net.sf.jxls.reader.ReaderConfig;
import net.sf.jxls.reader.XLSReadMessage;
import net.sf.jxls.reader.XLSReadStatus;
import net.sf.jxls.reader.XLSReader;

@SuppressWarnings("serial")
public class RegistrationFileUploadDialog extends Dialog {

	final static Logger jxlsLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.reader.SimpleBlockReaderImpl");
	final static Logger logger = (Logger) LoggerFactory.getLogger(RegistrationFileUploadDialog.class);

	private static final String GROUPS_READER_SPEC = "/templates/registration/GroupsReader.xml";
	private static final String REGISTRATION_READER_SPEC = "/templates/registration/RegistrationReader.xml";
	static {
		jxlsLogger.setLevel(Level.ERROR);
	}

	static public void listGroups(String msg) {
//        JPAService.runInTransaction(em -> {
//            List<Group> oldGroups = GroupRepository.doFindAll(em);
//            logger.error("{} : {}", msg, oldGroups.size());
//            for (Group g : oldGroups) {
//                logger.error("{}, group {}, id={}, {}",msg,g.getName(),g.getId(), g.getPlatform());
//            }
//            em.flush();
//            return null;
//        });
	}

	private boolean keepParticipations;

	public RegistrationFileUploadDialog() {

		H5 label = new H5(Translator.translate("Upload.WarningWillReplaceAll"));
		label.getStyle().set("color", "red");

		MemoryBuffer buffer = new MemoryBuffer();
		Upload upload = new Upload(buffer);
		upload.setWidth("40em");
//        upload.setAcceptedFileTypes("application/vnd.ms-excel",
//                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

		TextArea ta = new TextArea(getTranslation("Errors"));
		ta.setHeight("20ex");
		ta.setWidth("80em");
		ta.setVisible(false);

		upload.addSucceededListener(event -> {
			processInput(event.getFileName(), buffer.getInputStream(), ta);
		});

		upload.addStartedListener(event -> {
			ta.clear();
			ta.setVisible(false);
		});

		H3 title = new H3(getTranslation("UploadRegistrationFile"));
		VerticalLayout vl = new VerticalLayout(title, label, upload, ta);
		add(vl);
	}

	public String cleanMessage(String localizedMessage) {
		localizedMessage = localizedMessage.replace("Can't read cell ", "");
		String cell = localizedMessage.substring(0, localizedMessage.indexOf(" "));
		String ss = "spreadsheet";
		int ix = localizedMessage.indexOf(ss) + ss.length();
		localizedMessage = localizedMessage.substring(ix);
		if (localizedMessage.trim().contentEquals("text")) {
			localizedMessage = "Empty or invalid.";
		}
		String cleanMessage = getTranslation("Cell") + " " + cell + ": " + localizedMessage;
		return cleanMessage;
	}

	private void appendErrors(TextArea ta, StringBuffer sb, XLSReadStatus status) {
		@SuppressWarnings("unchecked")
		List<XLSReadMessage> errors = status.getReadMessages();
		for (XLSReadMessage m : errors) {
			String cleanMessage = cleanMessage(m.getMessage());
			sb.append(cleanMessage);
			Exception e = m.getException();
			if (e != null) {
				Throwable cause = e.getCause();
				String causeMessage = cause != null ? cause.getLocalizedMessage() : e.getLocalizedMessage();
				// causeMessage = LoggerUtils.stackTrace(cause);
				causeMessage = causeMessage != null ? causeMessage : e.toString();
				if (causeMessage.contentEquals("text")) {
					causeMessage = "Empty or invalid.";
				}
				sb.append(causeMessage);
				logger.debug(cleanMessage + causeMessage);
			}
			sb.append(System.lineSeparator());
		}
		if (sb.length() > 0) {
			ta.setValue(sb.toString());
			ta.setVisible(true);
		}
	}

	private int processAthletes(InputStream inputStream, TextArea ta, boolean dryRun) {
		StringBuffer sb = new StringBuffer();
		try (InputStream xmlInputStream = ResourceWalker.getResourceAsStream(REGISTRATION_READER_SPEC)) {
			inputStream.reset();
			ReaderConfig readerConfig = ReaderConfig.getInstance();
			readerConfig.setUseDefaultValuesForPrimitiveTypes(true);
			readerConfig.setSkipErrors(true);
			XLSReader reader = ReaderBuilder.buildFromXML(xmlInputStream);

			try (InputStream xlsInputStream = inputStream) {
				RCompetition c = new RCompetition();
				RCompetition.resetActiveCategories();
				RCompetition.resetActiveGroups();
				RCompetition.resetAthleteToEligibles();
				RCompetition.resetAthleteToTeams();

				List<RAthlete> athletes = new ArrayList<>();

				Map<String, Object> beans = new HashMap<>();
				beans.put("competition", c);
				beans.put("athletes", athletes);

				XLSReadStatus status = reader.read(inputStream, beans);

				// we created a batch of new athletes. the ones that have exact matches for a
				// category have had their eligibility and team memberships set in during the
				// reader processing.
				keepParticipations = beans.values().stream()
				        .filter(r -> ((RAthlete) r).getAthlete().getEligibleCategories() != null).findFirst()
				        .isPresent();

				logger.info(getTranslation("DataRead") + " " + athletes.size() + " athletes");
				if (dryRun) {
					return athletes.size();
				}

				if (athletes.size() > 0) {
					updateAthletes(sb, c, athletes);
					appendErrors(ta, sb, status);
				} else {
					ta.setValue(Translator.translate("NoAthletes"));
				}
				return athletes.size();
			} catch (InvalidFormatException | IOException e) {
				LoggerUtils.stackTrace(e);
				LoggerUtils.logError(logger, e);
			}
		} catch (IOException | SAXException e1) {
			LoggerUtils.stackTrace(e1);
			LoggerUtils.logError(logger, e1);
		}
		return 0;
	}

	private int processGroups(InputStream inputStream, TextArea ta, boolean dryRun) {
		StringBuffer sb = new StringBuffer();
		try (InputStream xmlInputStream = ResourceWalker.getResourceAsStream(GROUPS_READER_SPEC)) {
			inputStream.reset();
			ReaderConfig readerConfig = ReaderConfig.getInstance();
			readerConfig.setUseDefaultValuesForPrimitiveTypes(true);
			readerConfig.setSkipErrors(true);
			XLSReader reader = ReaderBuilder.buildFromXML(xmlInputStream);

			try (InputStream xlsInputStream = inputStream) {
				List<RGroup> groups = new ArrayList<>();

				Map<String, Object> beans = new HashMap<>();
				beans.put("groups", groups);

				// logger.info(getTranslation("ReadingData_"));
				XLSReadStatus status = reader.read(inputStream, beans);
				logger.info("Read {} groups.", groups.size());
				if (!dryRun) {
					updatePlatformsAndGroups(groups);
				}

				appendErrors(ta, sb, status);
				return groups.size();
			} catch (InvalidFormatException | IOException e) {
				LoggerUtils.logError(logger, e);
			}
		} catch (IOException | SAXException e1) {
			LoggerUtils.logError(logger, e1);
		}
		return 0;
	}

	private void processInput(String fileName, InputStream inputStream, TextArea ta) {
		// clear athletes to be able to clear groups
		resetAthletes();
		listGroups("after reset athletes");

		// dry run to count groups
		int nbGroups = processGroups(inputStream, ta, true);
		listGroups("after processGroups dryRun");
		if (nbGroups > 0) {
			// new format, reset groups from spreadsheet
			resetGroups();
			processGroups(inputStream, ta, false);
			listGroups("after processGroups real");
		}

		// process athletes now that groups have been adjusted
		processAthletes(inputStream, ta, false);
		if (!keepParticipations) {
			AthleteRepository.resetParticipations();
		}
		listGroups("after processAthletes real");
		return;
	}

	private void resetAthletes() {
		// delete all athletes and groups (naive version).
		JPAService.runInTransaction(em -> {
			List<Athlete> athletes = AthleteRepository.doFindAll(em);
			for (Athlete a : athletes) {
				em.remove(a);
			}
			em.flush();
			return null;
		});
	}

	private void resetGroups() {
		// delete all athletes and groups (naive version).
		JPAService.runInTransaction(em -> {
			List<Group> oldGroups = GroupRepository.doFindAll(em);
			for (Group g : oldGroups) {
				em.remove(g);
			}
			em.flush();
			return null;
		});
	}

	private void updateAthletes(StringBuffer sb, RCompetition c, List<RAthlete> athletes) {
//      for (Category c1: athlete.getEligibleCategories()) {
//      em.merge(c1);
//  }
		JPAService.runInTransaction(em -> {
			Competition curC = Competition.getCurrent();
			try {
				Competition rCompetition = c.getCompetition();
				// save some properties from current database that do not appear on spreadheet
				rCompetition.setEnforce20kgRule(curC.isEnforce20kgRule());
				rCompetition.setUseBirthYear(curC.isUseBirthYear());
				rCompetition.setMasters(curC.isMasters());

				// update the current competition with the new properties read from spreadsheet
				BeanUtils.copyProperties(curC, rCompetition);
				// update in database and set current to result of JPA merging.
				Competition.setCurrent(em.merge(curC));

				// Create the new athletes.
				athletes.stream().forEach(r -> {
					Athlete athlete = r.getAthlete();
					em.merge(athlete);
				});
				em.flush();
			} catch (IllegalAccessException | InvocationTargetException | RuntimeException e) {
				LoggerUtils.stackTrace(e);
				sb.append(e.getLocalizedMessage());
			}

			return null;
		});

		JPAService.runInTransaction(em -> {
			AthleteRepository.findAll().stream().forEach(a2 -> {
				LinkedHashSet<Category> eligibles = (LinkedHashSet<Category>) RCompetition
				        .getAthleteToEligibles()
				        .get(a2.getId());
				LinkedHashSet<Category> teams = (LinkedHashSet<Category>) RCompetition
				        .getAthleteToTeams()
				        .get(a2.getId());
				if (eligibles != null) {
					Category first = eligibles.stream().findFirst().orElse(null);
					a2.setCategory(first);
					a2.setEligibleCategories(eligibles);
					List<Participation> participations2 = a2.getParticipations();
					for (Participation p : participations2) {
						if (teams.contains(p.getCategory())) {
							p.setTeamMember(true);
						} else {
							logger.info("Excluding {} as team member for {}", a2.getShortName(),
							        p.getCategory().getComputedCode());
							p.setTeamMember(false);
						}
					}
					em.merge(a2);
				}
			});
			em.flush();
			return null;
		});
	}

	private void updatePlatformsAndGroups(List<RGroup> groups) {
		Set<String> futurePlatforms = groups.stream().map(RGroup::getPlatform).filter(p -> (p != null && !p.isBlank()))
		        .collect(Collectors.toSet());

		String defaultPlatformName = OwlcmsFactory.getDefaultFOP().getName();
		if (futurePlatforms.isEmpty()) {
			// keep the current default if no group is linked to a platform.
			futurePlatforms.add(defaultPlatformName);
		}
		logger.debug("to be kept if present: {}", futurePlatforms);

		PlatformRepository.deleteUnusedPlatforms(futurePlatforms);
		PlatformRepository.createMissingPlatforms(groups);

		// recompute the available platforms, unregister the existing FOPs, etc.
		OwlcmsFactory.initDefaultFOP();
		String newDefault = OwlcmsFactory.getDefaultFOP().getName();

		JPAService.runInTransaction(em -> {
			groups.stream().forEach(g -> {
				String platformName = g.getPlatform();
				Group group = g.getGroup();
				if (platformName == null || platformName.isBlank()) {
					platformName = newDefault;
				}
				logger.info("setting platform '{}' for group {}", platformName, g.getGroupName());
				Platform op = PlatformRepository.findByName(platformName);
				group.setPlatform(op);
				em.merge(group);
			});
			em.flush();
			return null;
		});

		groups.stream().forEach(g -> {
			logger.debug("group {} weighIn {} competition {}", g.getGroup(), g.getWeighinTime(),
			        g.getCompetitionTime());
		});
	}

}
