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

final class BirthDateRepairService {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(BirthDateRepairService.class);

	private BirthDateRepairService() {
	}

	static BirthDateRepairPreview preview() {
		return JPAService.runInTransaction(em -> {
			List<BirthDateRepairRow> rows = em.createQuery(
			        "select a from Athlete a where a.fullBirthDate is not null",
			        Athlete.class)
			        .getResultList()
			        .stream()
			        .map(BirthDateRepairRow::new)
			        .sorted(Comparator
			                .comparing((BirthDateRepairRow row) -> sortKey(row.getLastName()))
			                .thenComparing(row -> sortKey(row.getFirstName()))
			                .thenComparing(BirthDateRepairRow::getId))
			        .toList();

			return new BirthDateRepairPreview(rows);
		});
	}

	static BirthDateRepairResult apply(Set<Long> skippedAthleteIds, String clientIp) {
		BirthDateRepairResult result = JPAService.runInTransaction(em -> {
			List<Athlete> athletes = em.createQuery(
			        "select a from Athlete a where a.fullBirthDate is not null",
			        Athlete.class)
			        .getResultList();

			int jan1Count = 0;
			int dec31Count = 0;
			int updatedCount = 0;
			int skippedCount = 0;
			for (Athlete athlete : athletes) {
				LocalDate currentBirthDate = athlete.getFullBirthDate();
				if (isJan1(currentBirthDate)) {
					jan1Count++;
				}
				if (isDec31(currentBirthDate)) {
					dec31Count++;
				}
				if (skippedAthleteIds.contains(athlete.getId())) {
					skippedCount++;
					continue;
				}
				athlete.setFullBirthDate(currentBirthDate.plusDays(1));
				updatedCount++;
			}

			return new BirthDateRepairResult(updatedCount, skippedCount, jan1Count, dec31Count);
		});
		logger.warn(
		        "Emergency birth-date repair applied: added one day to {} Athlete.fullBirthDate values; skipped: {}; Jan 1 before repair: {}; Dec 31 before repair: {}; clientIp={}",
		        result.getUpdatedCount(), result.getSkippedCount(), result.getJan1Count(), result.getDec31Count(), clientIp);
		return result;
	}

	private static String sortKey(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}

	private static boolean isJan1(LocalDate date) {
		return date.getMonthValue() == 1 && date.getDayOfMonth() == 1;
	}

	private static boolean isDec31(LocalDate date) {
		return date.getMonthValue() == 12 && date.getDayOfMonth() == 31;
	}

	static final class BirthDateRepairPreview {
		private final List<BirthDateRepairRow> rows;
		private final int jan1Count;
		private final int dec31Count;

		BirthDateRepairPreview(List<BirthDateRepairRow> rows) {
			this.rows = rows;
			this.jan1Count = (int) rows.stream().filter(BirthDateRepairRow::isJan1).count();
			this.dec31Count = (int) rows.stream().filter(BirthDateRepairRow::isDec31).count();
		}

		List<BirthDateRepairRow> getRows() {
			return this.rows;
		}

		int getTotalCount() {
			return this.rows.size();
		}

		int getJan1Count() {
			return this.jan1Count;
		}

		int getDec31Count() {
			return this.dec31Count;
		}
	}

	static final class BirthDateRepairResult {
		private final int updatedCount;
		private final int skippedCount;
		private final int jan1Count;
		private final int dec31Count;

		BirthDateRepairResult(int updatedCount, int skippedCount, int jan1Count, int dec31Count) {
			this.updatedCount = updatedCount;
			this.skippedCount = skippedCount;
			this.jan1Count = jan1Count;
			this.dec31Count = dec31Count;
		}

		int getUpdatedCount() {
			return this.updatedCount;
		}

		int getSkippedCount() {
			return this.skippedCount;
		}

		int getJan1Count() {
			return this.jan1Count;
		}

		int getDec31Count() {
			return this.dec31Count;
		}
	}

	static final class BirthDateRepairRow {
		private final Long id;
		private final Integer lotNumber;
		private final String lastName;
		private final String firstName;
		private final LocalDate currentBirthDate;
		private final LocalDate repairedBirthDate;

		BirthDateRepairRow(Athlete athlete) {
			this.id = athlete.getId();
			this.lotNumber = athlete.getLotNumber();
			this.lastName = athlete.getLastName();
			this.firstName = athlete.getFirstName();
			this.currentBirthDate = athlete.getFullBirthDate();
			this.repairedBirthDate = this.currentBirthDate.plusDays(1);
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

		boolean isJan1() {
			return BirthDateRepairService.isJan1(this.currentBirthDate);
		}

		boolean isDec31() {
			return BirthDateRepairService.isDec31(this.currentBirthDate);
		}
	}
}