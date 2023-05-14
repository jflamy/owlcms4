package app.owlcms.data.records;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
	        Integer totalRequest,
	        Athlete a) {

		Integer bestSnatch = a.getPersonalBestSnatch();
		Integer bestCleanJerk = a.getPersonalBestCleanJerk();
		Integer bestTotal = a.getPersonalBestTotal();
		boolean personalRecords = bestSnatch != null || bestCleanJerk != null || bestTotal != null;
		if (!personalRecords && (displayedRecords == null || displayedRecords.isEmpty())) {
			return Json.createNull();
		}

		Multimap<Integer, RecordEvent> recordsByAgeWeight = ArrayListMultimap.create();
		TreeMap<String, String> rowOrder = new TreeMap<>();
		for (RecordEvent re : displayedRecords) {
			// rows are ordered according to configuration
			String order = getRowOrder(re.getRecordName(), re.getFileName());
			rowOrder.put(order , re.getRecordName());
			// synthetic key to arrange records in correct column.
			recordsByAgeWeight.put((re.getGender().ordinal() * 100000000) + re.getAgeGrpLower() * 1000000
			        + re.getAgeGrpUpper() * 1000 + re.getBwCatUpper(), re);
		}

		// order columns left to right in ascending age groups;
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
		int categoryRecordsLength = 0;
		if (recordTable.length > 0) {
			categoryRecordsLength = recordTable[0].length;
			for (int j = 0; j < categoryRecordsLength; j++) {
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
							} else if (rec.getRecordLift() == Ranking.CLEANJERK && cjRequest != null
							        && recordValue != null
							        && cjRequest > +recordValue) {
								cell.put("cjHighlight", "highlight");
							} else if (rec.getRecordLift() == Ranking.TOTAL && totalRequest != null
							        && recordValue != null
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
				column.put("recordClass", "recordBox");
				columns.set(j, column);
			}
		}

		int personalRecordsLength = 0;

		if (personalRecords) {
			personalRecordsLength = 1;
			JsonObject column = Json.createObject();
			JsonArray columnCells = Json.createArray();
			JsonObject cell = Json.createObject();
			String string = Translator.translate("Record.PersonalBest");
			column.put("cat", string);

			cell.put(Ranking.SNATCH.name(), bestSnatch != null ? bestSnatch.toString() : "\u00a0");

			cell.put(Ranking.CLEANJERK.name(), bestCleanJerk != null ? bestCleanJerk.toString() : "\u00a0");
			cell.put(Ranking.TOTAL.name(), bestTotal != null ? bestTotal.toString() : "\u00a0");
			columnCells.set(0, cell);
			column.put("records", columnCells);
			columns.set(categoryRecordsLength, column);
			column.put("recordClass", "recordBoxPersonal");
		}

		recordInfo.put("recordNames", recordFederations);
		recordInfo.put("recordCategories", recordCategories);
		recordInfo.put("recordTable", columns);
		recordInfo.put("nbRecords", Json.create(categoryRecordsLength + 1 + personalRecordsLength));

		return recordInfo;
	}

	private static String getRowOrder(String recordName, String fileName) {
		// FIXME change to something from record config.
		return fileName;
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
		//logger.debug("records size {} {} {} {}", curAthlete.getGender(), curAthlete.getAge(), curAthlete.getBodyWeight(),records);

		// remove duplicates for each kind of record, keep largest
		Map<String, RecordEvent> cleanMap = new HashMap<>();
		for (RecordEvent r : records) {
			RecordEvent curMax = cleanMap.get(r.getKey());
			if (curMax == null || r.getRecordValue() > curMax.getRecordValue()) {
				//logger.debug("updating {} {}", r.getKey(), r.getRecordValue());
				cleanMap.put(r.getKey(), r);
			} else {
				//logger.debug("DISCARDING {} {}", r.getKey(), r.getRecordValue());
			}
		}

		Collection<RecordEvent> candidateRecords = cleanMap.values();
		//logger.debug("candidate records {}", candidateRecords);
		String federationCodes = curAthlete.getFederationCodes();
		Set<String> athleteFederations = (federationCodes == null || federationCodes.isBlank())
		        ? Set.of()
		        : new HashSet<>(Arrays.asList(federationCodes.split("[,;]")));
		//logger.debug(" *** athlete {} agegroups {} federations {}", curAthlete.getShortName(), curAthlete.getEligibleCategories(), athleteFederations);
		records = candidateRecords.stream()
		        .filter(c -> athleteFederations.isEmpty() ? true : athleteFederations.contains(c.getRecordFederation()))
		        .collect(Collectors.toList());
		//logger.debug("retained records {}", records);
		return records;
	}

}
