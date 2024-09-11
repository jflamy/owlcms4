package app.owlcms.data.export;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadFeature;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Read athlete information from a json export, updating the lifting data for a list of sessions
 * 
 * Used when the sessions were recorded off line.
 * 
 * @author jf@jflamy.dev
 */
public class AthleteSessionDataReader {

	static Logger logger = (Logger) LoggerFactory.getLogger(AthleteSessionDataReader.class);

	public static void importAthletes(InputStream is, List<Group> sessions) throws IOException {
		List<Long> sessionIds = sessions.stream().map(g -> g.getId()).toList();
		logger.info("importing sessions {}", sessions);
		doImportAthletes(is, sessionIds);
	}

	private static void doImportAthletes(InputStream is, List<Long> sessionIds) throws IOException, JsonParseException {
		JsonFactory factory = JsonFactory.builder()
		        .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
		        .build();
		List<Athlete> athletes = new ArrayList<>();
		List<Athlete> jsonAthletes;
		String[] attributesToRead = {
		        "id",
		        "lotNumber",
		        "group",
		        
		        "bodyWeight",

		        "snatch1AutomaticProgression",
		        "snatch1Declaration",
		        "snatch1Change1",
		        "snatch1Change2",
		        "snatch1LiftTime",
		        "snatch1ActualLift",

		        "snatch2AutomaticProgression",
		        "snatch2Declaration",
		        "snatch2Change1",
		        "snatch2Change2",
		        "snatch2LiftTime",
		        "snatch2ActualLift",

		        "snatch3AutomaticProgression",
		        "snatch3Declaration",
		        "snatch3Change1",
		        "snatch3Change2",
		        "snatch3LiftTime",
		        "snatch3ActualLift",

		        "cleanJerk1AutomaticProgression",
		        "cleanJerk1Declaration",
		        "cleanJerk1Change1",
		        "cleanJerk1Change2",
		        "cleanJerk1LiftTime",
		        "cleanJerk1ActualLift",

		        "cleanJerk2AutomaticProgression",
		        "cleanJerk2Declaration",
		        "cleanJerk2Change1",
		        "cleanJerk2Change2",
		        "cleanJerk2LiftTime",
		        "cleanJerk2ActualLift",

		        "cleanJerk3AutomaticProgression",
		        "cleanJerk3Declaration",
		        "cleanJerk3Change1",
		        "cleanJerk3Change2",
		        "cleanJerk3LiftTime",
		        "cleanJerk3ActualLift",
		};
		try (JsonParser parser = factory.createParser(is)) {
			jsonAthletes = readAthletes(parser, athletes, attributesToRead, sessionIds);
		}
		updateAthletes(jsonAthletes, attributesToRead);
	}

	private static void updateAthletes(List<Athlete> jsonAthletes, String[] attributesToRead) {
		// Find existing athlete with the same id and lot
		JPAService.runInTransaction(em -> {
			for (Athlete jsonAthlete : jsonAthletes) {
				Long id = jsonAthlete.getId();
				Athlete existingAthlete = AthleteRepository.findById(id);
				if (existingAthlete != null) {
					copyAttributes(jsonAthlete, existingAthlete, attributesToRead);
					em.merge(existingAthlete);
				} else {
					logger.error("did not find athlete {}", id);
				}
			}
			return null;
		});
	}

	private static List<Athlete> readAthletes(JsonParser parser,
	        List<Athlete> athletes, String[] attributesToRead,
	        List<Long> sessionIds) throws IOException {
		List<String> attributes = Arrays.asList(attributesToRead);
		while (!parser.isClosed()) {
			JsonToken token = parser.nextToken();

			// skip tokens until top-level athlete field name.
			if (JsonToken.FIELD_NAME.equals(token) && "athletes".equals(parser.currentName())) {
				token = parser.nextToken(); // Move to start of array of athletes

				while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
					Athlete jsonAthlete = new Athlete();
					boolean keep = false;

					// each object is an athlete
					while ((token = parser.nextToken()) != JsonToken.END_OBJECT) {

						// process each field.
						String fieldName = parser.currentName();
						// value
						token = parser.nextToken();
						if (token == JsonToken.START_ARRAY || token == JsonToken.START_OBJECT) {
							// complex attribute value, we ignore them.
							parser.skipChildren();
						} else {
							// simple attribute value, check the ones we care about.
							if (attributes.contains(fieldName)) {
								if (fieldName.equals("group") && sessionIds.contains(parser.getLongValue())) {
									keep = true;
								}
								boolean validating = Athlete.isSkipValidationsDuringImport();
								Level level = jsonAthlete.getLogger().getLevel();
								try {
									Athlete.setSkipValidationsDuringImport(true);
									jsonAthlete.getLogger().setLevel(Level.ERROR);
									PropertyDescriptor pd = new PropertyDescriptor(fieldName, Athlete.class);
									Method setter = pd.getWriteMethod();
									Class<?> fieldType = pd.getPropertyType();
									if (fieldType == Long.class) {
										setter.invoke(jsonAthlete, parser.getLongValue());
									} else if (fieldType == int.class) {
										setter.invoke(jsonAthlete, parser.getIntValue());
									} else if (fieldType == String.class) {
										setter.invoke(jsonAthlete, parser.getValueAsString());
									} else if (fieldType == Double.class) {
										setter.invoke(jsonAthlete, parser.getDoubleValue());
									} else if (fieldType == Float.class) {
										setter.invoke(jsonAthlete, parser.getFloatValue());
									}
								} catch (Exception e) {
									LoggerUtils.logError(logger, e);
								} finally {
									Athlete.setSkipValidationsDuringImport(validating);
									jsonAthlete.getLogger().setLevel(level);
								}
							}
						}
					}
					if (keep) {
						logger.debug("adding athlete {}", jsonAthlete.getId());
						athletes.add(jsonAthlete);
					}
				}
			}
		}
		return athletes;
	}

	private static void copyAttributes(Athlete source, Athlete target, String[] attributesToRead) {
		boolean validating = Athlete.isSkipValidationsDuringImport();
		try {
			logger.info("importing results for {} {} (session {})", target.getFullName(), target.getId(), target.getGroup());
			Athlete.setSkipValidationsDuringImport(true);
			for (String attribute : attributesToRead) {
				if (attribute.equals("id") || attribute.equals("group")) {
					continue;
				}
				try {
					PropertyDescriptor pd = new PropertyDescriptor(attribute, Athlete.class);
					Method getter = pd.getReadMethod();
					Method setter = pd.getWriteMethod();
					Object value = getter.invoke(source);
					setter.invoke(target, value);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
			Athlete.setSkipValidationsDuringImport(validating);
		}
	}
}
