package app.owlcms.nui.preparation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;
import app.owlcms.i18n.Translator;
import app.owlcms.servlet.StopProcessingException;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import java.io.FileNotFoundException;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper service that centralizes kit-element precheck orchestration.
 * This keeps DocumentsContent focused on UI wiring and selection while
 * the precheck logic lives here for reuse by the dialog.
 */
public class DocumentsPrecheckService {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(DocumentsPrecheckService.class);

    public static class PrecheckResult {
        public List<KitElement> present = new ArrayList<>();
        public int missing = 0;
    }

    /**
     * Evaluate per-element prechecks. Does not throw AtLeastOneTemplateRequiredException.
     * It reports other non-template failures via dialog.reportPrecheckErrors and throws StopProcessingException for fatal precheck failures.
     */
    public PrecheckResult evaluatePrechecks(List<KitElement> elements, Group g, List<Athlete> athletes, DocumentDownloadDialog dialog) {
        PrecheckResult result = new PrecheckResult();
        for (KitElement ke : elements) {
            try {
                Optional<Exception> pre = ke.preCheck().apply(athletes, g);
                if (pre != null && pre.isPresent()) {
                    Exception e = pre.get();
                    // Template missing is a recoverable per-element condition for set-level logic
                    if (e instanceof TemplateMissingException) {
                        result.missing++;
                        continue;
                    }
                    // Non-template failures: report the original exception and abort without altering the message
                    Exception reportEx = (e instanceof Exception) ? e : new Exception(e);
                    dialog.reportPrecheckErrors(List.of(reportEx));
                    throw new StopProcessingException(e.getMessage(), e);
                } else {
                    result.present.add(ke);
                }
            } catch (Throwable t) {
                LoggerUtils.logError(logger, t, true);
                // Report original throwable as Exception to the dialog and rethrow wrapped so the original message/cause are preserved
                Exception reportEx = (t instanceof Exception) ? (Exception) t : new Exception(t);
                dialog.reportPrecheckErrors(List.of(reportEx));
                throw new StopProcessingException(t.getMessage(), t);
            }
        }
        return result;
    }

    /**
     * Run element prechecks and return present list; if all elements are missing templates, throw AtLeastOneTemplateRequiredException (this is the set-level behavior).
     */
    public List<KitElement> runSetPrecheckOrThrow(List<KitElement> elements, Group g, List<Athlete> athletes, DocumentDownloadDialog dialog) {
        // Require a selected session for multi-element document sets (scope precheck)
        if (g == null) {
            // Show the NoSession message and prevent download
            dialog.displayPrecheckErrors(List.of(new Exception("NoSession")));
            throw new StopProcessingException("NoSession", new RuntimeException("No session selected for document set"));
        }

        PrecheckResult r = evaluatePrechecks(elements, g, athletes, dialog);
        if (r.missing > 0 && r.present.isEmpty()) {
            String s = Translator.translate("Documents.NoTemplate");
            dialog.displayPrecheckErrors(List.of(new Exception(s)));
            throw new AtLeastOneTemplateRequiredException();
        }
        return r.present.isEmpty() ? elements : r.present;
    }

    /**
     * Run each KitElement.preCheck and return the filtered list of elements that are present. On non-template precheck failure, the method will add an error
     * paragraph to the dialog, disable the associated download control and throw StopProcessingException. If all elements are missing templates, behaves similarly and throws.
     */
    public List<KitElement> filterElementsByPrecheckOrThrow(List<KitElement> elements, Group g, List<Athlete> athletes, DocumentDownloadDialog dialog) {
        PrecheckResult r = evaluatePrechecks(elements, g, athletes, dialog);
        if (r.missing > 0 && r.present.isEmpty()) {
            String s = Translator.translate("Documents.NoTemplate");
            dialog.displayPrecheckErrors(List.of(new Exception(s)));
            throw new StopProcessingException("NoTemplate", new TemplateMissingException("NoTemplate"));
        }
        return r.present.isEmpty() ? elements : r.present;
    }

    /**
     * Validate template selection and that the template resource exists. Returns Optional.empty() when OK or Optional.of(TemplateMissingException) when missing.
     */
    public Optional<Exception> checkTemplateSelectedAndExists(PreCompetitionTemplates templateEnum) {
        try {
            String selected = templateEnum.templateFileNameSupplier.get();
            if (selected == null || selected.isBlank()) {
                return Optional.of(new TemplateMissingException("NoTemplate"));
            }
            String resourceFolder = templateEnum.folder + "/";
            String templatePath = resourceFolder + selected;
            try {
                ResourceWalker.getFileOrResourcePath(templatePath);
            } catch (FileNotFoundException fnfe) {
                return Optional.of(new TemplateMissingException("NoTemplate", fnfe));
            }
            return Optional.empty();
        } catch (Throwable t) {
            return Optional.of(new TemplateMissingException("NoTemplate", t));
        }
    }

    /**
     * Shared logic for default prechecks. If allowNoSelection is false, a missing group (g==null) results in a NoSession exception; otherwise group may be null.
     */
    public Optional<Exception> runDefaultPrecheck(PreCompetitionTemplates templateEnum, List<Athlete> a, Group g, boolean allowNoSelection) {
        Optional<Exception> tpl = checkTemplateSelectedAndExists(templateEnum);
        if (tpl.isPresent()) {
            return tpl;
        }
        try {
            if (!allowNoSelection && g == null) {
                return Optional.of(new Exception("NoSession"));
            }

            int incomingCount = a == null ? 0 : a.size();
            String sampleIds = "";
            if (a != null && !a.isEmpty()) {
                sampleIds = a.stream().limit(10).map(ath -> String.valueOf(ath.getId())).collect(java.util.stream.Collectors.joining(","));
            }
            String groupInfo = (g == null) ? "<no-group>" : (g.getId() + ":" + g.getName());
            Optional<Exception> outcome = Optional.empty();

            if (g != null) {
                if (incomingCount == 0) {
                    outcome = Optional.of(new StopProcessingException("NoAthletes", new RuntimeException(Translator.translate("NoAthletes"))));
                }
            }

            String resultText = outcome.isEmpty() ? "OK" : (outcome.get().getMessage() == null ? outcome.get().toString() : outcome.get().getMessage());
            logger.warn("preCheck %s for template=%s received: incomingCount=%d, sampleIds=[%s], group=%s, resolvedCount=%d, outcome=%s",
                    allowNoSelection ? "allow-no-selection" : "default",
                    templateEnum.name(), incomingCount, sampleIds, groupInfo, incomingCount, resultText);
            return outcome;
        } catch (Throwable t) {
            LoggerUtils.logError(logger, t, true);
            logger.warn("preCheck %s for template=%s threw exception: %s", allowNoSelection ? "allow-no-selection" : "default", templateEnum.name(),
                    t.toString());
            return Optional.of(new Exception(t));
        }
    }

    /**
     * Canonical set-level template selection precheck. This centralizes the
     * logic used by the UI (dialog) and by service-level invocation so there
     * that both paths behave identically when deciding whether at least one
     * template is required for a multi-element kit.
     *
     * Returns Optional.empty() when OK, Optional.of(AtLeastOneTemplateRequiredException)
     * when a multi-element kit has none selected, or Optional.of(new Exception("NoTemplate"))
     * when a single-element kit has no selection.
     */
    public Optional<Exception> runTemplateSetPrecheck(List<KitElement> kitElements) {
        if (kitElements == null || kitElements.isEmpty()) return Optional.empty();
        try {
            boolean anyMapped = false;
            boolean anySelected = false;

            // Debugging: log the kit contents and supplier values so we can
            // understand what the dialog sees at runtime when performing the
            // template precheck. This is temporary and can be removed after
            // diagnosis.
            logger.warn("runTemplateSetPrecheck: kitElements.size={}", kitElements.size());

            for (KitElement ke : kitElements) {
                if (ke == null) {
                    logger.warn("runTemplateSetPrecheck: encountered null KitElement");
                    continue;
                }
                Supplier<String> selSupplier = ke.selectedTemplateSupplier();
                if (selSupplier == null) {
                    logger.warn("runTemplateSetPrecheck: element id='{}' has no selectedTemplateSupplier", ke.id());
                    continue; // not a mapped element
                }
                anyMapped = true;
                String selected = selSupplier.get();
                logger.warn("runTemplateSetPrecheck: element id='{}' selected='{}'", ke.id(), selected);
                if (selected != null && !selected.isBlank()) {
                    anySelected = true;
                    break;
                }
            }

            if (!anyMapped) return Optional.empty();

            if (!anySelected) {
                if (kitElements.size() == 1) {
                    return Optional.of(new Exception("NoTemplate"));
                } else {
                    return Optional.of(new AtLeastOneTemplateRequiredException());
                }
            }

            return Optional.empty();
        } catch (Throwable t) {
            return Optional.of(new Exception(t));
        }
    }
}
