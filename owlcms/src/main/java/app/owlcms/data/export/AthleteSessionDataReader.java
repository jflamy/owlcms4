package app.owlcms.data.export;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;

/**
 * Read athlete information from a json export, updating the lifting
 * data for a list of sessions
 * 
 * Used when the sessions were recorded off line.
 * 
 * @author jf@jflamy.dev
 */
public class AthleteSessionDataReader {
	static String[] attributesToRead = {
	        "id",
	        "lot",
	        "group",
	        "snatch1ActualLift",
	        "snatch1Change1",
	        "snatch1Change2",
	        "snatch1Declaration",
	        "snatch1LiftTime",
	        "snatch2ActualLift",
	        "snatch2Change1",
	        "snatch2Change2",
	        "snatch2Declaration",
	        "snatch2LiftTime",
	        "snatch3ActualLift",
	        "snatch3Change1",
	        "snatch3Change2",
	        "snatch3Declaration",
	        "snatch3LiftTime",
	        "cleanJerk1ActualLift",
	        "cleanJerk1Change1",
	        "cleanJerk1Change2",
	        "cleanJerk1Declaration",
	        "cleanJerk1LiftTime",
	        "cleanJerk2ActualLift",
	        "cleanJerk2Change1",
	        "cleanJerk2Change2",
	        "cleanJerk2Declaration",
	        "cleanJerk2LiftTime",
	        "cleanJerk3ActualLift",
	        "cleanJerk3Change1",
	        "cleanJerk3Change2",
	        "cleanJerk3Declaration",
	        "cleanJerk3LiftTime"
	};
	
	public static void importAthletes(List<Group> groups) throws IOException {
		JsonFactory factory = new JsonFactory();
		List<Athlete> athletes = new ArrayList<>();
		List<Long> groupIds = groups.stream().map(g -> g.getId()).toList();


		List<Athlete> jsonAthletes;
		try (JsonParser parser = factory.createParser(new File("athletes.json"))) {
			jsonAthletes = readAthletes(parser, athletes, attributesToRead, groupIds);
		}
		updateAthletes(jsonAthletes);
	}

	private static void updateAthletes(List<Athlete> jsonAthletes) {
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
		while (!parser.isClosed()) {
			JsonToken token = parser.nextToken();
			if (JsonToken.FIELD_NAME.equals(token) && "athletes".equals(parser.currentName())) {
				parser.nextToken(); // Move to start of array
				while (parser.nextToken() != JsonToken.END_ARRAY) {
					Athlete jsonAthlete = new Athlete();
					boolean keep = false;
					
					while (parser.nextToken() != JsonToken.END_OBJECT) {
						String fieldName = parser.currentName();
						parser.nextToken(); // Move to value

						for (String attribute : attributesToRead) {
							if (attribute.equals("group") && groupIds.contains(parser.getLongValue())) {
								keep = true;
							}
							if (attribute.equals(fieldName)) {
								try {
									PropertyDescriptor pd = new PropertyDescriptor(fieldName, Athlete.class);
									Method setter = pd.getWriteMethod();
									Class<?> fieldType = pd.getPropertyType();

									if (fieldType == Long.class) {
										setter.invoke(jsonAthlete, parser.getLongValue());
									} if (fieldType == int.class) {
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
			// Comment out the part below if the athletes array is at the top level of the file
			/*
			 * else if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) { findAthletesArray(parser, athletes, attributesToRead); }
			 */
		}
		return athletes;
	}

	private static void copyAttributes(Athlete source, Athlete target, String[] attributesToRead) {
		for (String attribute : attributesToRead) {
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
