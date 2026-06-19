package app.owlcms.nui.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.shared.AuthorizationDispatch;

@SuppressWarnings("serial")
@Route("admin/record-federation-report")
public class RecordFederationComparisonReport extends Composite<VerticalLayout>
        implements HasDynamicTitle, AuthorizationDispatch {

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		AuthorizationDispatch.super.beforeEnter(event);
		if (!OwlcmsSession.isAuthenticated()) {
			return;
		}
		String clientIp = AccessUtils.getClientIp();
		if (!AccessUtils.checkBackdoor(clientIp)) {
			throw new AccessDeniedException();
		}
	}

	public RecordFederationComparisonReport() {
		VerticalLayout content = getContent();
		content.setPadding(false);
		content.setSpacing(false);
		content.add(new Html(buildHtmlContent(buildReport())));
	}

	public static ReportData buildReport(Collection<RecordEvent> loadedRecords, Collection<Athlete> athletes) {
		List<BlankEligibilityAthlete> flaggedEligibilityAthletes = new ArrayList<>();

		Set<String> federations = new LinkedHashSet<>();
		for (RecordEvent record : loadedRecords) {
			if (record != null && record.getRecordFederation() != null && !record.getRecordFederation().isBlank()) {
				federations.add(record.getRecordFederation().trim());
			}
		}
		for (Athlete athlete : athletes) {
			if (athlete == null) {
				continue;
			}
			String federationCodes = athlete.getFederationCodes();
			if (federationCodes == null || federationCodes.isBlank()) {
				flaggedEligibilityAthletes.add(new BlankEligibilityAthlete(
				        describeAthlete(athlete),
				        federationCodes,
				        "Counted for all loaded federations because eligibility data is blank"));
				continue;
			}
			boolean containsEmptyFederationToken = false;
			for (String federation : splitFederationCodes(federationCodes)) {
				String trimmedFederation = federation.trim();
				federations.add(trimmedFederation);
				if (trimmedFederation.isEmpty()) {
					containsEmptyFederationToken = true;
				}
			}
			if (containsEmptyFederationToken) {
				flaggedEligibilityAthletes.add(new BlankEligibilityAthlete(
				        describeAthlete(athlete),
				        federationCodes,
				        "Malformed eligibility data contains an empty federation token and contributes to the empty federation row"));
			}
		}

		List<FederationParticipationSummary> summaries = new ArrayList<>();
		for (String federation : federations) {
			int loadedRecordsCount = 0;
			for (RecordEvent record : loadedRecords) {
				if (federation.equalsIgnoreCase(trimmed(record.getRecordFederation()))) {
					loadedRecordsCount++;
				}
			}
			int athleteCount = 0;
			for (Athlete athlete : athletes) {
				if (athlete == null) {
					continue;
				}
				String federationCodes = athlete.getFederationCodes();
				if (federationCodes == null || federationCodes.isBlank()) {
					athleteCount++;
					continue;
				}
				for (String code : splitFederationCodes(federationCodes)) {
					if (federation.equalsIgnoreCase(trimmed(code))) {
						athleteCount++;
						break;
					}
				}
			}
			summaries.add(new FederationParticipationSummary(
			        federation,
			        loadedRecordsCount,
			        athleteCount,
			        loadedRecordsCount == 0,
			        athleteCount == 0));
		}
		return new ReportData(summaries, flaggedEligibilityAthletes);
	}

	public static ReportData buildReport() {
		return buildReport(RecordRepository.findAllLoadedRecords(), AthleteRepository.findAll());
	}

	public static List<FederationParticipationSummary> buildSummary(Collection<RecordEvent> loadedRecords,
	        Collection<Athlete> athletes) {
		return buildReport(loadedRecords, athletes).getSummaries();
	}

	public static List<FederationParticipationSummary> buildSummary() {
		return buildReport().getSummaries();
	}

	private static List<String> splitFederationCodes(String federationCodes) {
		if (federationCodes == null || federationCodes.isBlank()) {
			return List.of();
		}
		String normalized = federationCodes.replaceAll("[ ]*,[ ]*", ",");
		return List.of(normalized.split("[;,]"));
	}

	private static String trimmed(String value) {
		return value == null ? null : value.trim();
	}

	private static String describeAthlete(Athlete athlete) {
		String fullName = athlete.getFullName();
		if (fullName != null && !fullName.isBlank()) {
			return fullName;
		}
		return "(unnamed athlete)";
	}

	public static String buildHtmlContent(ReportData report) {
		return buildHtmlContent(report.getSummaries(), report.getBlankEligibilityAthletes());
	}

	public static String buildHtmlContent(List<FederationParticipationSummary> summaries) {
		return buildHtmlContent(summaries, List.of());
	}

	private static String buildHtmlContent(List<FederationParticipationSummary> summaries,
	        List<BlankEligibilityAthlete> blankEligibilityAthletes) {
		StringBuilder html = new StringBuilder();
		html.append("<div class='record-federation-report'>");
		html.append("<style>.record-federation-report{font-family:Arial,sans-serif;margin:2rem;}"
		        + ".record-federation-report table{border-collapse:collapse;width:100%;max-width:900px;}"
		        + ".record-federation-report th,.record-federation-report td{border:1px solid #ccc;padding:0.5rem;text-align:left;}"
		        + ".record-federation-report th{background:#f3f3f3;}"
		        + ".record-federation-report tr.warning{background:#fff4f4;}"
		        + ".record-federation-report .flag{color:#b00020;font-weight:bold;}"
		        + ".record-federation-report .eligibility-code{font-family:monospace;white-space:pre-wrap;}"
		        + ".record-federation-report ul{padding-left:1.5rem;}</style>");
		html.append("<h1>Record Federation Comparison</h1>");
		if (summaries.isEmpty()) {
			html.append("<p>There are no loaded federations.</p>");
			appendBlankEligibilitySection(html, blankEligibilityAthletes);
			html.append("</div>");
			return html.toString();
		}
		html.append("<table><thead><tr><th>Federation</th><th>Loaded Records</th><th>Athlete Eligibility Count</th><th>Flags</th></tr></thead><tbody>");
		for (FederationParticipationSummary summary : summaries) {
			String rowClass = summary.isMissingAthletes() || summary.isMissingLoadedRecords() ? "warning" : "";
			String flags = Stream.of(
			        summary.isMissingLoadedRecords() ? "No loaded records" : null,
			        summary.isMissingAthletes() ? "No athletes" : null)
			        .filter(Objects::nonNull)
			        .collect(Collectors.joining(", "));
			if (flags.isBlank()) {
				flags = "-";
			}
			html.append("<tr class='")
			    .append(rowClass)
			    .append("'><td>")
			    .append(escapeHtml(summary.getFederation()))
			    .append("</td><td>")
			    .append(summary.getLoadedRecordsCount())
			    .append("</td><td>")
			    .append(summary.getAthleteCount())
			    .append("</td><td>")
			    .append(flags)
			    .append("</td></tr>");
		}
		html.append("</tbody></table>");
		appendBlankEligibilitySection(html, blankEligibilityAthletes);
		html.append("</div>");
		return html.toString();
	}

	private static void appendBlankEligibilitySection(StringBuilder html,
	        List<BlankEligibilityAthlete> blankEligibilityAthletes) {
		if (blankEligibilityAthletes.isEmpty()) {
			return;
		}
		html.append("<h2>Athletes With Blank Or Malformed Eligibility Data</h2>");
		html.append("<ul>");
		for (BlankEligibilityAthlete athlete : blankEligibilityAthletes) {
			html.append("<li>")
			    .append(escapeHtml(athlete.getAthleteName()))
			    .append(" - ")
			    .append(escapeHtml(athlete.getIssueDescription()))
			    .append(" - eligibility data <span class='eligibility-code'>&laquo;")
			    .append(escapeHtml(athlete.getDisplayEligibilityData()))
			    .append("&raquo;</span></li>");
		}
		html.append("</ul>");
	}

	public static String buildHtmlReport(List<FederationParticipationSummary> summaries) {
		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html>");
		html.append("<html><head><meta charset='utf-8'><title>Record Federation Comparison</title></head><body>");
		html.append(buildHtmlContent(summaries));
		html.append("</body></html>");
		return html.toString();
	}

	private static String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	@Override
	public String getPageTitle() {
		return "Record Federation Comparison";
	}

	public static class ReportData {
		private final List<FederationParticipationSummary> summaries;
		private final List<BlankEligibilityAthlete> blankEligibilityAthletes;

		public ReportData(List<FederationParticipationSummary> summaries,
		        List<BlankEligibilityAthlete> blankEligibilityAthletes) {
			this.summaries = summaries;
			this.blankEligibilityAthletes = blankEligibilityAthletes;
		}

		public List<FederationParticipationSummary> getSummaries() {
			return this.summaries;
		}

		public List<BlankEligibilityAthlete> getBlankEligibilityAthletes() {
			return this.blankEligibilityAthletes;
		}
	}

	public static class BlankEligibilityAthlete {
		private final String athleteName;
		private final String rawEligibilityData;
		private final String issueDescription;

		public BlankEligibilityAthlete(String athleteName, String rawEligibilityData, String issueDescription) {
			this.athleteName = athleteName;
			this.rawEligibilityData = rawEligibilityData;
			this.issueDescription = issueDescription;
		}

		public String getAthleteName() {
			return this.athleteName;
		}

		public String getRawEligibilityData() {
			return this.rawEligibilityData;
		}

		public String getIssueDescription() {
			return this.issueDescription;
		}

		public String getDisplayEligibilityData() {
			return this.rawEligibilityData == null ? "null" : this.rawEligibilityData;
		}
	}

	public static class FederationParticipationSummary {
		private final String federation;
		private final int loadedRecordsCount;
		private final int athleteCount;
		private final boolean missingLoadedRecords;
		private final boolean missingAthletes;

		public FederationParticipationSummary(String federation, int loadedRecordsCount, int athleteCount,
		        boolean missingLoadedRecords, boolean missingAthletes) {
			this.federation = federation;
			this.loadedRecordsCount = loadedRecordsCount;
			this.athleteCount = athleteCount;
			this.missingLoadedRecords = missingLoadedRecords;
			this.missingAthletes = missingAthletes;
		}

		public String getFederation() {
			return this.federation;
		}

		public int getLoadedRecordsCount() {
			return this.loadedRecordsCount;
		}

		public int getAthleteCount() {
			return this.athleteCount;
		}

		public boolean isMissingLoadedRecords() {
			return this.missingLoadedRecords;
		}

		public boolean isMissingAthletes() {
			return this.missingAthletes;
		}
	}
}