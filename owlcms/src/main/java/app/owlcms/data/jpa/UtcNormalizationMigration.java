package app.owlcms.data.jpa;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Transient;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;
import app.owlcms.data.config.Config;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.records.RecordEvent;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;

public class UtcNormalizationMigration {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(UtcNormalizationMigration.class);

    public static void normalizeAllToUtc(EntityManager em) {
        if (Config.getCurrent().isLocalDateTimeUtcNormalized()) {
            return; // Already normalized
        }

        // Athlete entity
        List<Athlete> athletes = em.createQuery("SELECT a FROM Athlete a", Athlete.class).getResultList();
        int athleteCount = 0;
        for (Athlete a : athletes) {
            normalizeDates(a);
            em.merge(a);
            athleteCount++;
        }
        logger.warn("UtcNormalizationMigration: converted {} Athlete entities", athleteCount);

        // Group entity (table: CompetitionGroup, class: Group)
        List<Group> groups = em.createQuery("SELECT g FROM CompetitionGroup g", app.owlcms.data.group.Group.class).getResultList();
        int groupCount = 0;
        for (Group g : groups) {
            normalizeDates(g);
            em.merge(g);
            groupCount++;
        }
        logger.warn("UtcNormalizationMigration: converted {} CompetitionGroup entities", groupCount);

        // Competition entity
        List<Competition> competitions = em.createQuery("SELECT c FROM Competition c", Competition.class).getResultList();
        int competitionCount = 0;
        for (Competition c : competitions) {
            normalizeDates(c);
            em.merge(c);
            competitionCount++;
        }
        logger.warn("UtcNormalizationMigration: converted {} Competition entities", competitionCount);

        // RecordEvent entity
        List<RecordEvent> recordEvents = em.createQuery("SELECT r FROM RecordEvent r", RecordEvent.class).getResultList();
        int recordEventCount = 0;
        for (RecordEvent r : recordEvents) {
            normalizeDates(r);
            em.merge(r);
            recordEventCount++;
        }
        logger.warn("UtcNormalizationMigration: converted {} RecordEvent entities", recordEventCount);

        // Set the flag in Config
        Config config = Config.getCurrent();
        config.setLocalDateTimeUtcNormalized(true);
        em.merge(config);
        logger.warn("UtcNormalizationMigration: normalization flag set in Config");
    }

    private static void normalizeDates(Object entity) {
        for (Field field : entity.getClass().getDeclaredFields()) {
            try {
                // Ignore fields marked as JPA @Transient
                if (field.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(entity);
                if (value instanceof LocalDateTime) {
                    field.set(entity, toUtc((LocalDateTime) value));
                } else if (value instanceof LocalDate) {
                    field.set(entity, toUtc((LocalDate) value));
                }
            } catch (Exception e) {
                logger.warn("UtcNormalizationMigration: error normalizing field {} in {}: {}", field.getName(), entity.getClass().getSimpleName(), e.toString());
            }
        }
    }

    private static LocalDateTime toUtc(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private static LocalDate toUtc(LocalDate ld) {
        if (ld == null) return null;
        return ld.atStartOfDay(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDate();
    }
}
