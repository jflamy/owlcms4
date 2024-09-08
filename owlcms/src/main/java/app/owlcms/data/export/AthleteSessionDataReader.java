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

	public static void importAthletes(InputStream is, List<Group> groups) throws IOException {
		List<Long> groupIds = groups.stream().map(g -> g.getId()).toList();
		doImportAthletes(is, groupIds);
	}

	private static void doImportAthletes(InputStream is, List<Long> groupIds) throws IOException, JsonParseException {
		JsonFactory factory = JsonFactory.builder()
		        .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
		        .build();
		List<Athlete> athletes = new ArrayList<>();
		List<Athlete> jsonAthletes;
		String[] attributesToRead = {
		        "id",
		        "lot",
		        "group",
		        
		        "snatch1AutomaticProgression",
		        "snatch1Declaration",
		        "snatch1Change1",
		        "snatch1Change2",
		        "snatch1LiftTime",
		        "snatch1ActualLift",
		        
		        "snatch2AutomaticProgression",
		        "snatch2Declaration",
		        "snatch2Change2",
		        "snatch2Change2",
		        "snatch2LiftTime",
		        "snatch2ActualLift",
		        
		        "snatch3AutomaticProgression",
		        "snatch3Declaration",
		        "snatch3Change3",
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
		        "cleanJerk2Change2",
		        "cleanJerk2Change2",
		        "cleanJerk2LiftTime",
		        "cleanJerk2ActualLift",
		        
		        "cleanJerk3AutomaticProgression",
		        "cleanJerk3Declaration",
		        "cleanJerk3Change3",
		        "cleanJerk3Change2",
		        "cleanJerk3LiftTime",
		        "cleanJerk3ActualLift",
		};
		try (JsonParser parser = factory.createParser(is)) {
			jsonAthletes = readAthletes(parser, athletes, attributesToRead, groupIds);
		}
		updateAthletes(jsonAthletes, attributesToRead);
	}

	private static void updateAthletes(List<Athlete> jsonAthletes, String[] attributesToRead) {
		// Find existing athlete with the same id and lot
		JPAService.runInTransaction(em -> {
			for (Athlete jsonAthlete : jsonAthletes) {
				Athlete existingAthlete = AthleteRepository.findById(jsonAthlete.getId());
				if (existingAthlete != null) {
					copyAttributes(jsonAthlete, existingAthlete, attributesToRead);
					em.merge(existingAthlete);
				}
			}
			return null;
		});
	}

	private static List<Athlete> readAthletes(JsonParser parser, List<Athlete> athletes, String[] attributesToRead, List<Long> groupIds) throws IOException {
		List<String> attributes = Arrays.asList(attributesToRead);
		while (!parser.isClosed()) {
			JsonToken token = parser.nextToken();

			// get tokens until top-level athlete field name.
			if (JsonToken.FIELD_NAME.equals(token) && "athletes".equals(parser.currentName())) {
				token = parser.nextToken(); // Move to start of array of athletes

				while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
					logger.warn("---");
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
								if (fieldName.equals("group") && groupIds.contains(parser.getLongValue())) {
									keep = true;
								}
								try {
									PropertyDescriptor pd = new PropertyDescriptor(fieldName, Athlete.class);
									Method setter = pd.getWriteMethod();
									Class<?> fieldType = pd.getPropertyType();
									if (fieldType == Long.class) {
										setter.invoke(jsonAthlete, parser.getLongValue());
									} else if (fieldType == int.class) {
										setter.invoke(jsonAthlete, parser.getIntValue());
									} else if (fieldType == String.class) {
										setter.invoke(jsonAthlete, parser.getValueAsString());
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					}
					if (keep) {
						athletes.add(jsonAthlete);
					}
				}
			}
		}
		return athletes;
	}

	private static void copyAttributes(Athlete source, Athlete target, String[] attributesToRead) {
		for (String attribute : attributesToRead) {
			if (attribute.equals("id")) {
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
	}
}
