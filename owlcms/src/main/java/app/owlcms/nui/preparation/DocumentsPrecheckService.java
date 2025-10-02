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
 * Helper service that centralizes kit-element scope precheck orchestration.
 * This keeps DocumentsContent focused on UI wiring and selection while
 * the scope precheck logic lives here for reuse by the dialog.
 */
public class DocumentsPrecheckService {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(DocumentsPrecheckService.class);

    public static class PrecheckResult {
        public List<KitElement> present = new ArrayList<>();
        public int missing = 0;
    }

    /**
     * Evaluate per-element scope prechecks. Does not throw AtLeastOneTemplateRequiredException.
     * It reports other non-template failures via dialog.reportPrecheckErrors and throws StopProcessingException for fatal scope precheck failures.
     * Elements without a template selected are skipped (counted as missing).
     */
    public PrecheckResult evaluateScopePrechecks(List<KitElement> elements, Group g, List<Athlete> athletes, DocumentDownloadDialog dialog) {
        PrecheckResult result = new PrecheckResult();
        for (KitElement ke : elements) {
            try {
                // Check if template is selected first - skip scope precheck if no template
                Supplier<String> selectedTemplateSupplier = ke.selectedTemplateSupplier();
                boolean hasTemplate = false;
                if (selectedTemplateSupplier != null) {
                    String selected = selectedTemplateSupplier.get();
                    hasTemplate = (selected != null && !selected.isBlank());
                }
                
                if (!hasTemplate) {
                    // No template selected for this element - skip it
                    result.missing++;
                    continue;
                }
                
                // Template exists, now run the scope precheck
                Optional<Exception> scopeCheck = ke.scopePrecheck().apply(athletes, g);
                if (scopeCheck != null && scopeCheck.isPresent()) {
                    Exception e = scopeCheck.get();
                    // Template missing is a recoverable per-element condition for set-level logic
                    if (e instanceof TemplateException) {
                        result.missing++;
                        continue;
                    }
                } else {
                    result.present.add(ke);
                }
            } catch (Throwable t) {
                if (!(t instanceof ScopeException) && !(t instanceof TemplateException)) {
                    LoggerUtils.logError(logger, t, true);
                }
                // Report original throwable as Exception to the dialog and rethrow wrapped so the original message/cause are preserved
                Exception reportEx = (t instanceof Exception) ? (Exception) t : new Exception(t);
                dialog.reportPrecheckErrors(List.of(reportEx));
                throw new StopProcessingException(t.getMessage(), t);
            }
        }
        return result;
    }

    /**
     * Run element scope prechecks and return present list; if all elements are missing templates, throw AtLeastOneTemplateRequiredException (this is the set-level behavior).
     * Each element's individual scope precheck determines whether a session is required. Elements without templates are skipped.
     */
    public List<KitElement> runSetScopePrecheckOrThrow(List<KitElement> elements, Group g, List<Athlete> athletes, DocumentDownloadDialog dialog) throws AtLeastOneTemplateRequiredException {
        // Run individual element prechecks - each will determine if it needs a session
        // Elements without templates will be skipped by evaluateScopePrechecks
        PrecheckResult r = evaluateScopePrechecks(elements, g, athletes, dialog);
        
        if (r.missing > 0 && r.present.isEmpty()) {
            String s = Translator.translate("Documents.NoTemplate");
            dialog.displayPrecheckErrors(List.of(new Exception(s)));
            throw new AtLeastOneTemplateRequiredException();
        }
        return r.present.isEmpty() ? elements : r.present;
    }

    /**
     * Run each KitElement.scopePrecheck and return the filtered list of elements that are present. On non-template scope precheck failure, the method will add an error
     * paragraph to the dialog, disable the associated download control and throw StopProcessingException. If all elements are missing templates, behaves similarly and throws.
     */
    public List<KitElement> filterElementsByScopePrecheckOrThrow(List<KitElement> elements, Group g, List<Athlete> athletes, DocumentDownloadDialog dialog) {
        PrecheckResult r = evaluateScopePrechecks(elements, g, athletes, dialog);
        if (r.missing > 0 && r.present.isEmpty()) {
            String s = Translator.translate("Documents.NoTemplate");
            dialog.displayPrecheckErrors(List.of(new Exception(s)));
            throw new StopProcessingException("NoTemplate", new NoTemplateException("NoTemplate"));
        }
        return r.present.isEmpty() ? elements : r.present;
    }

    /**
     * Validate template selection and that the template resource exists. Returns Optional.empty() when OK or Optional.of(NoTemplateException) when missing.
     */
    public Optional<Exception> checkTemplateSelectedAndExists(PreCompetitionTemplate templateEnum) {
        try {
            String selected = templateEnum.templateFileNameSupplier.get();
            if (selected == null || selected.isBlank()) {
                return Optional.of(new NoTemplateException("NoTemplate"));
            }
            String resourceFolder = templateEnum.folder + "/";
            String templatePath = resourceFolder + selected;
            try {
                ResourceWalker.getFileOrResourcePath(templatePath);
            } catch (FileNotFoundException fnfe) {
                return Optional.of(new NoTemplateException("NoTemplate", fnfe));
            }
            return Optional.empty();
        } catch (Throwable t) {
            return Optional.of(new NoTemplateException("NoTemplate", t));
        }
    }

    /**
     * Shared logic for default scope prechecks. If allowNoSelection is false, a missing group (g==null) results in a NoSession exception; otherwise group may be null.
     */
    public Optional<Exception> runDefaultScopePrecheck(PreCompetitionTemplate templateEnum, List<Athlete> a, Group g, boolean allowNoSelection) {
        try {
            if (!allowNoSelection && g == null) {
                return Optional.of(new NoSessionException());
            }

            int incomingCount = a == null ? 0 : a.size();
            Optional<Exception> outcome = Optional.empty();

            if (g != null) {
                if (incomingCount == 0) {
                    outcome = Optional.of(new StopProcessingException("NoAthletes", new RuntimeException(Translator.translate("NoAthletes"))));
                }
            }

            // logging removed
            return outcome;
        } catch (Throwable t) {
            LoggerUtils.logError(logger, t, true);
        // logger removed
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
            // logger removed

            for (KitElement ke : kitElements) {
                if (ke == null) {
                    // logger removed
                    continue;
                }
                Supplier<String> selSupplier = ke.selectedTemplateSupplier();
                if (selSupplier == null) {
                    // logger removed
                    continue; // not a mapped element
                }
                anyMapped = true;
                String selected = selSupplier.get();
                // logger removed
                if (selected != null && !selected.isBlank()) {
                    anySelected = true;
                    break;
                }
            }

            if (!anyMapped) return Optional.empty();

            if (!anySelected) {
                if (kitElements.size() == 1) {
                    return Optional.of(new NoTemplateException());
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
