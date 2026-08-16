package app.owlcms.nui.admin;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Logger;

final class BirthYearRepairService {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(BirthYearRepairService.class);

	private BirthYearRepairService() {
	}

	static BirthYearRepairPreview preview() {
		return JPAService.runInTransaction(em -> {
			List<BirthYearRepairRow> rows = em.createQuery(
			        "select a from Athlete a where a.fullBirthDate is not null",
			        Athlete.class)
			        .getResultList()
			        .stream()
			        .map(BirthYearRepairRow::new)
			        .sorted(Comparator
			                .comparing((BirthYearRepairRow row) -> sortKey(row.getLastName()))
			                .thenComparing(row -> sortKey(row.getFirstName()))
			                .thenComparing(BirthYearRepairRow::getId))
			        .toList();

			return new BirthYearRepairPreview(rows);
		});
	}

	static BirthYearRepairResult apply(Set<Long> selectedAthleteIds, String clientIp) {
		BirthYearRepairResult result = JPAService.runInTransaction(em -> {
			List<Athlete> athletes = em.createQuery(
			        "select a from Athlete a where a.fullBirthDate is not null",
			        Athlete.class)
			        .getResultList();

			int updatedCount = 0;
			int unselectedCount = 0;
			for (Athlete athlete : athletes) {
				if (!selectedAthleteIds.contains(athlete.getId())) {
					unselectedCount++;
					continue;
				}
				athlete.setFullBirthDate(repairedBirthDate(athlete.getFullBirthDate()));
				updatedCount++;
			}

			return new BirthYearRepairResult(updatedCount, unselectedCount);
		});
		logger./**/warn(
		        "Emergency birth-year repair applied: moved {} Athlete.fullBirthDate values to January 1 of the following year; unselected: {}; clientIp={}",
		        result.getUpdatedCount(), result.getUnselectedCount(), clientIp);
		return result;
	}

	static LocalDate repairedBirthDate(LocalDate currentBirthDate) {
		return LocalDate.of(currentBirthDate.getYear() + 1, 1, 1);
	}

	private static String sortKey(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}

	static final class BirthYearRepairPreview {
		private final List<BirthYearRepairRow> rows;

		BirthYearRepairPreview(List<BirthYearRepairRow> rows) {
			this.rows = rows;
		}

		List<BirthYearRepairRow> getRows() {
			return this.rows;
		}

		int getTotalCount() {
			return this.rows.size();
		}
	}

	static final class BirthYearRepairResult {
		private final int updatedCount;
		private final int unselectedCount;

		BirthYearRepairResult(int updatedCount, int unselectedCount) {
			this.updatedCount = updatedCount;
			this.unselectedCount = unselectedCount;
		}

		int getUpdatedCount() {
			return this.updatedCount;
		}

		int getUnselectedCount() {
			return this.unselectedCount;
		}
	}

	static final class BirthYearRepairRow {
		private final Long id;
		private final Integer lotNumber;
		private final String lastName;
		private final String firstName;
		private final LocalDate currentBirthDate;
		private final LocalDate repairedBirthDate;

		BirthYearRepairRow(Athlete athlete) {
			this.id = athlete.getId();
			this.lotNumber = athlete.getLotNumber();
			this.lastName = athlete.getLastName();
			this.firstName = athlete.getFirstName();
			this.currentBirthDate = athlete.getFullBirthDate();
			this.repairedBirthDate = BirthYearRepairService.repairedBirthDate(this.currentBirthDate);
		}

		Long getId() {
			return this.id;
		}

		Integer getLotNumber() {
			return this.lotNumber;
		}

		String getLastName() {
			return this.lastName;
		}

		String getFirstName() {
			return this.firstName;
		}

		LocalDate getCurrentBirthDate() {
			return this.currentBirthDate;
		}

		LocalDate getRepairedBirthDate() {
			return this.repairedBirthDate;
		}
	}
}