/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.jpa;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import ch.qos.logback.classic.Logger;

/** Copies legacy full birth dates into the canonical textual birth-date field. */
public final class BirthDateTextMigration {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(BirthDateTextMigration.class);

	private BirthDateTextMigration() {
	}

	public static void migrate(EntityManager em) {
		List<LegacyBirthDate> legacyBirthDates = new ArrayList<>();
		Session session = em.unwrap(Session.class);
		session.doWork(connection -> readLegacyBirthDates(connection, legacyBirthDates));

		int migratedCount = 0;
		for (LegacyBirthDate legacyBirthDate : legacyBirthDates) {
			Athlete athlete = em.find(Athlete.class, legacyBirthDate.athleteId);
			if (athlete != null && athlete.getIsoBirthDate() == null) {
				athlete.setIsoBirthDate(legacyBirthDate.birthDate.toString());
				migratedCount++;
			}
		}

		logger.info("BirthDateTextMigration: migrated {} Athlete birth dates", migratedCount);
	}

	private static boolean hasLegacyBirthDateColumn(Connection connection) throws SQLException {
		DatabaseMetaData metadata = connection.getMetaData();
		try (ResultSet columns = metadata.getColumns(null, null, "%", "%")) {
			while (columns.next()) {
				String tableName = columns.getString("TABLE_NAME");
				String columnName = columns.getString("COLUMN_NAME");
				if ("Athlete".equalsIgnoreCase(tableName) && "fullBirthDate".equalsIgnoreCase(columnName)) {
					return true;
				}
			}
		}
		return false;
	}

	private static void readLegacyBirthDates(Connection connection, List<LegacyBirthDate> legacyBirthDates)
			throws SQLException {
		if (!hasLegacyBirthDateColumn(connection)) {
			return;
		}

		String sql = "SELECT id, fullBirthDate FROM Athlete "
				+ "WHERE isoBirthDate IS NULL AND fullBirthDate IS NOT NULL";
		try (PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet resultSet = statement.executeQuery()) {
			while (resultSet.next()) {
				java.sql.Date legacyBirthDate = resultSet.getDate("fullBirthDate");
				if (legacyBirthDate != null) {
					legacyBirthDates.add(new LegacyBirthDate(resultSet.getLong("id"), legacyBirthDate.toLocalDate()));
				}
			}
		}
	}

	private static final class LegacyBirthDate {
		private final Long athleteId;
		private final LocalDate birthDate;

		private LegacyBirthDate(Long athleteId, LocalDate birthDate) {
			this.athleteId = athleteId;
			this.birthDate = birthDate;
		}
	}
}