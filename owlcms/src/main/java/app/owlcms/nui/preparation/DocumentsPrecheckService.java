package app.owlcms.nui.preparation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
                    if (e instanceof TemplateMissingException || "NoTemplate".equals(e.getMessage()) || "NoTemplates".equals(e.getMessage())) {
                        result.missing++;
                        continue;
                    }
                    String s = e.getMessage() == null ? Translator.translate("Download.failed") : e.getMessage();
                    dialog.reportPrecheckErrors(List.of(new Exception(s)));
                    throw new StopProcessingException(s, e);
                } else {
                    result.present.add(ke);
                }
            } catch (Throwable t) {
                LoggerUtils.logError(logger, t, true);
                String s = t.getMessage() == null ? Translator.translate("Download.failed") : t.getMessage();
                dialog.reportPrecheckErrors(List.of(new Exception(s)));
                throw new StopProcessingException(s, t);
            }
        }
        return result;
    }

    /**
     * Run element prechecks and return present list; if all elements are missing templates, throw AtLeastOneTemplateRequiredException (this is the set-level behavior).
     */
    public List<KitElement> runSetPrecheckOrThrow(List<KitElement> elements, Group g, List<Athlete> athletes, DocumentDownloadDialog dialog) {
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
}
