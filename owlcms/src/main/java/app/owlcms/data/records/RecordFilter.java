package app.owlcms.data.records;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class RecordFilter {

	@SuppressWarnings("unused")
	private static Logger logger = (Logger) LoggerFactory.getLogger(RecordFilter.class);

	public static JsonValue buildRecordJson(List<RecordEvent> displayedRecords, Set<RecordEvent> challengedRecords,
	        Integer snatchRequest, Integer cjRequest,
	        Integer totalRequest) {

		if (displayedRecords == null || displayedRecords.isEmpty()) {
			return Json.createNull();
		}

		Multimap<Integer, RecordEvent> recordsByAgeWeight = ArrayListMultimap.create();
		TreeMap<String, String> rowOrder = new TreeMap<>();
		for (RecordEvent re : displayedRecords) {
			// rows are ordered according to file name.
			rowOrder.put(re.getFileName(), re.getRecordName());
			// synthetic key to arrange records in correct column.
			recordsByAgeWeight.put((re.getGender().ordinal() * 100000000) + re.getAgeGrpLower() * 1000000
			        + re.getAgeGrpUpper() * 1000 + re.getBwCatUpper(), re);
		}

		// order columns in ascending age groups;
		List<Integer> columnOrder = recordsByAgeWeight.keySet().stream().sorted((e1, e2) -> Integer.compare(e1, e2))
		        .collect(Collectors.toList());

		@SuppressWarnings("unchecked")
		List<RecordEvent>[][] recordTable = new ArrayList[rowOrder.size()][columnOrder.size()];
		ArrayList<String> rowRecordNames = new ArrayList<>(rowOrder.values());

		for (int j1 = 0; j1 < columnOrder.size(); j1++) {
			Collection<RecordEvent> recordsForCurrentCategory = recordsByAgeWeight.get(columnOrder.get(j1));
			for (int i1 = 0; i1 < rowOrder.size(); i1++) {
				String curRowRecordName = rowRecordNames.get(i1);

				List<RecordEvent> recordFound = recordsForCurrentCategory.stream()
				        .filter(r -> r.getRecordName().contentEquals(curRowRecordName)).collect(Collectors.toList());

				// put them in snatch/cj/total order (not needed really), then largest record
				// first in case of multiple
				// records
				recordFound.sort(Comparator.comparing(RecordEvent::getRecordLift)
				        .thenComparing(Comparator.comparing(RecordEvent::getRecordValue).reversed()));

				// keep the largest record
				List<RecordEvent> maxRecordFound = new ArrayList<>();
				recordFound.stream().filter(r -> r.getRecordLift() == Ranking.SNATCH).findFirst()
				        .ifPresent(r -> maxRecordFound.add(r));
				recordFound.stream().filter(r -> r.getRecordLift() == Ranking.CLEANJERK).findFirst()
				        .ifPresent(r -> maxRecordFound.add(r));
				recordFound.stream().filter(r -> r.getRecordLift() == Ranking.TOTAL).findFirst()
				        .ifPresent(r -> maxRecordFound.add(r));
				recordTable[i1][j1] = maxRecordFound;
			}
		}

		JsonObject recordInfo = Json.createObject();
		JsonArray recordFederations = Json.createArray();
		JsonArray recordCategories = Json.createArray();

		int ix1 = 0;
		for (String s : rowRecordNames) {
			recordFederations.set(ix1++, s);
		}

		JsonArray columns = Json.createArray();
		for (int j = 0; j < recordTable[0].length; j++) {
			JsonObject column = Json.createObject();
			JsonArray columnCells = Json.createArray();
			for (int i = 0; i < recordTable.length; i++) {
				JsonObject cell = Json.createObject();
				cell.put(Ranking.SNATCH.name(), "\u00a0");
				cell.put(Ranking.CLEANJERK.name(), "\u00a0");
				cell.put(Ranking.TOTAL.name(), "\u00a0");
				for (RecordEvent rec : recordTable[i][j]) {
					if (recordCategories.length() <= j || recordCategories.get(j) == null) {
						String string = Translator.translate("Record.CategoryTitle", rec.getAgeGrp(),
						        rec.getBwCatString());
						recordCategories.set(j, string);
						column.put("cat", string);
					}
					Double recordValue = rec.getRecordValue();
					cell.put(rec.getRecordLift().name(), recordValue != null ? recordValue : 999.0D);

					if (challengedRecords.stream().anyMatch(cr -> cr.sameAs(rec))) {
						// logger.debug("rec found {}", rec);
						if (rec.getRecordLift() == Ranking.SNATCH && snatchRequest != null && recordValue != null
						        && snatchRequest > recordValue) {
							cell.put("snatchHighlight", "highlight");
						} else if (rec.getRecordLift() == Ranking.CLEANJERK && cjRequest != null && recordValue != null
						        && cjRequest > +recordValue) {
							cell.put("cjHighlight", "highlight");
						} else if (rec.getRecordLift() == Ranking.TOTAL && totalRequest != null && recordValue != null
						        && totalRequest > +recordValue) {
							cell.put("totalHighlight", "highlight");
						}
					} else {
						// logger.debug("rec {} not found in {}", rec, challengedRecords);
					}
				}
				columnCells.set(i, cell);
			}
			column.put("records", columnCells);
			columns.set(j, column);
		}

		recordInfo.put("recordNames", recordFederations);
		recordInfo.put("recordCategories", recordCategories);
		recordInfo.put("recordTable", columns);
		recordInfo.put("nbRecords", Json.create(recordTable[0].length + 1));

		return recordInfo;
	}

	public static List<RecordEvent> computeChallengedRecords(List<RecordEvent> eligibleRecords, Integer snatchRequest,
	        Integer cjRequest,
	        Integer totalRequest) {
		List<RecordEvent> challengedRecords = new ArrayList<>();
		challengedRecords
		        .addAll(eligibleRecords.stream()
		                .filter(rec -> rec.getRecordLift() == Ranking.SNATCH && snatchRequest != null
		                        && rec.getRecordValue() != null
		                        && snatchRequest > rec.getRecordValue())
		                .collect(Collectors.toList()));
		challengedRecords
		        .addAll(eligibleRecords.stream()
		                .filter(rec -> rec.getRecordLift() == Ranking.CLEANJERK && cjRequest != null
		                        && rec.getRecordValue() != null
		                        && cjRequest > rec.getRecordValue())
		                .collect(Collectors.toList()));
		challengedRecords
		        .addAll(eligibleRecords.stream()
		                .filter(rec -> rec.getRecordLift() == Ranking.TOTAL && totalRequest != null
		                        && rec.getRecordValue() != null
		                        && totalRequest > rec.getRecordValue())
		                .collect(Collectors.toList()));
		return challengedRecords;
	}

	public static List<RecordEvent> computeEligibleRecordsForAthlete(Athlete curAthlete) {

		List<RecordEvent> records = RecordRepository.findFiltered(curAthlete.getGender(), curAthlete.getAge(),
		        curAthlete.getBodyWeight(), null, null);
		logger.warn("records size {}  {}  {}  {}" ,curAthlete.getGender(), curAthlete.getAge(),
		        curAthlete.getBodyWeight(), records.size());

		// remove duplicates for each kind of record, keep largest
		Map<String, RecordEvent> cleanMap = records.stream().collect(
		        Collectors.toMap(
		                RecordEvent::getKey,
		                Function.identity(),
		                (r1, r2) -> r1.getRecordValue() > r2.getRecordValue() ? r1 : r2));

		Collection<RecordEvent> candidateRecords = cleanMap.values();

		// if a record is defined to apply to an age group that is active in the
		// competition, athlete must be eligible
		// in that age group.
//        Set<String> activeAgeGroupCodes = AgeGroupRepository.findActive().stream().map(a -> a.getCode())
//                .collect(Collectors.toSet());
//        Set<String> athleteAgeGroupCodes = curAthlete.getParticipations().stream()
//                .map(a -> a.getCategory().getAgeGroup().getCode()).collect(Collectors.toSet());
		Set<String> athleteFederations = curAthlete.getFederationCodes() != null
		        ? new HashSet<>(Arrays.asList(curAthlete.getFederationCodes().split("[,;]")))
		        : Set.of();
//        logger.debug(" *** athlete {} agegroups {} active {} federations {}", curAthlete.getShortName(), athleteAgeGroupCodes, activeAgeGroupCodes, athleteFederations);
		records = candidateRecords.stream()
		        .filter(c -> athleteFederations.isEmpty() ? true : athleteFederations.contains(c.getRecordFederation()))
//                .peek(c -> logger.debug("retained {}", c.getRecordFederation()))
//                .filter(c -> activeAgeGroupCodes
//                        .contains(c.getAgeGrp()) ? athleteAgeGroupCodes.contains(c.getAgeGrp())
//                                : true)
		        .collect(Collectors.toList());
		return records;
	}

}
