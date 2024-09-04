/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSMedalSchedule extends JXLSWorkbookStreamSource {

	public class CategoryStat {
		private Category category;
		private Group session;

		CategoryStat(Category category, Group session) {
			this.category = category;
			this.session = session;
		}

		public Category getCategory() {
			return this.category;
		}

		public Group getSession() {
			return this.session;
		}

		public void setCategory(Category category) {
			this.category = category;
		}

		public void setSession(Group session) {
			this.session = session;
		}
	}

	public class SessionStats {
		TreeSet<Category> categories = new TreeSet<>();
		Group session;
		private Gender gender;

		SessionStats(Group session) {
			this.session = session;
		}

		List<Category> computeSessionCategories() {
			TreeSet<Category> sessionCategories = new TreeSet<>();
			for (Athlete a : session.getAthletes()) {
				if (this.gender != null && a.getGender() != this.gender) {
					continue;
				}
				sessionCategories.addAll(a.getEligibleCategories());
			}
			ArrayList<Category> returned = new ArrayList<Category>();
			returned.addAll(sessionCategories);
			return returned;
		}

		public String getCategoriesAsString() {
			return getCategories().stream().map(c -> c.toString()).distinct().collect(Collectors.joining(", "));
		}

		public void setCategoriesAsString(String unused) {
		}

		public TreeSet<Category> getCategories() {
			return categories;
		}

		public void setCategories(List<Category> newCategories) {
			this.categories = new TreeSet<Category>(newCategories);
		}

		public Group getSession() {
			return session;
		}

		public void setSession(Group session) {
			this.session = session;
		}

		public Gender getGender() {
			return gender;
		}

		public void setGender(Gender gender) {
			this.gender = gender;
		}

	}

	Logger logger = (Logger) LoggerFactory.getLogger(JXLSMedalSchedule.class);

	public JXLSMedalSchedule(Gender g, boolean excludeNotWeighed, UI ui) {
	}

	public JXLSMedalSchedule(UI ui) {
		this(null, false, ui);
	}

	@Override
	public boolean isEmptyOk() {
		return true;
	}

	@Override
	protected void setReportingInfo() {
		Set<String> alreadyMedaledCodes = new HashSet<>();
		TreeMap<Group, SessionStats> medalingPerSession = new TreeMap<>(Group.groupWeighinTimeComparator);
		TreeMap<Category, Group> categorySessions = new TreeMap<>();

		Competition competition = Competition.getCurrent();
		getReportingBeans().put("t", Translator.getMap());
		getReportingBeans().put("competition", competition);
		getReportingBeans().put("session", getGroup()); // legacy
		getReportingBeans().put("group", getGroup());

		getReportingBeans().put("groups", medalingPerSession.keySet());
		getReportingBeans().put("sessions", medalingPerSession.keySet());

		List<Group> sess = GroupRepository.findAll().stream().sorted(Group.groupWeighinTimeComparator.reversed())
		        .collect(Collectors.toList());
		for (Group session : sess) {
			SessionStats sessionStats = new SessionStats(session);
			List<Category> newCategories = sessionStats.computeSessionCategories();

			//List<String> ncCodes = newCategories.stream().map(c -> c.getCode()).toList();
			newCategories = newCategories.stream().filter(c -> !alreadyMedaledCodes.contains(c.getCode())).toList();

			sessionStats.setCategories(newCategories);
			alreadyMedaledCodes.addAll(newCategories.stream().map(c -> c.getCode()).toList());
			newCategories.stream().forEach(c -> {
				categorySessions.put(c, session);
			});

			// the key set will be in the expected ascending order
			medalingPerSession.put(session, sessionStats);
		}

		ArrayList<SessionStats> all = new ArrayList<SessionStats>();
		all.addAll(medalingPerSession.values());
		getReportingBeans().put("medalStatsPerSession", all);

		ArrayList<CategoryStat> categories = new ArrayList<CategoryStat>();
		categorySessions.forEach((Category category, Group session) -> {
			categories.add(new CategoryStat(category, session));
		});
		categories.sort((a, b) -> a.getCategory().compareTo(b.getCategory())) ;
		getReportingBeans().put("medalStatsPerCategory", categories);
	}

	// @Override
	// public InputStream getTemplate(Locale locale) throws IOException {
	// return getLocalizedTemplate("/templates/medals/MedalSchedule", ".xlsx", locale);
	// }

}
