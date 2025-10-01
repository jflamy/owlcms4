package app.owlcms.nui.preparation;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;
import app.owlcms.spreadsheet.JXLSWorkbookStreamSource;
import app.owlcms.utils.Resource;

/**
 * Mutable kit element used to describe a downloadable document item. The dialog
 * updates template-related fields (name, isp, extension, and template suppliers)
 * when the user selects a template. Keeping these fields mutable avoids recreating
 * KitElement instances and keeps identity stable for callers that hold references.
 */
public class KitElement {
        private final String id;
        private final PreCompetitionTemplate templateEnum;
        private String name;
        private String extension;
        private Path isp;
        private int count;
        private final BiFunction<List<Athlete>, Group, JXLSWorkbookStreamSource> writerFactory;
        private final BiFunction<List<Athlete>, Group, Optional<Exception>> scopePrecheck;
        private Supplier<String> processingMessageSupplier;
        private Supplier<List<Resource>> availableTemplatesSupplier;
        private Supplier<String> selectedTemplateSupplier;

        public KitElement(String id, PreCompetitionTemplate templateEnum, String name, String extension, Path isp, int count,
                        BiFunction<List<Athlete>, Group, JXLSWorkbookStreamSource> writerFactory,
                        BiFunction<List<Athlete>, Group, Optional<Exception>> scopePrecheck,
                        Supplier<String> processingMessageSupplier,
                        Supplier<List<Resource>> availableTemplatesSupplier,
                        Supplier<String> selectedTemplateSupplier) {
                this.id = id;
                this.templateEnum = templateEnum;
                this.name = name;
                this.extension = extension;
                this.isp = isp;
                this.count = count;
                this.writerFactory = writerFactory;
                this.scopePrecheck = scopePrecheck;
                this.processingMessageSupplier = processingMessageSupplier;
                this.availableTemplatesSupplier = availableTemplatesSupplier;
                this.selectedTemplateSupplier = selectedTemplateSupplier;
        }

        public String id() { return id; }
        public PreCompetitionTemplate templateEnum() { return templateEnum; }
        public String name() { return name; }
        public void setName(String name) { this.name = name; }
        public String extension() { return extension; }
        public void setExtension(String extension) { this.extension = extension; }
        public Path isp() { return isp; }
        public void setIsp(Path isp) { this.isp = isp; }
        public int count() { return count; }
        public void setCount(int count) { this.count = count; }
        public BiFunction<List<Athlete>, Group, JXLSWorkbookStreamSource> writerFactory() { return writerFactory; }
        public BiFunction<List<Athlete>, Group, Optional<Exception>> scopePrecheck() { return scopePrecheck; }
        public Supplier<String> processingMessageSupplier() { return processingMessageSupplier; }
        public void setProcessingMessageSupplier(Supplier<String> s) { this.processingMessageSupplier = s; }
        public Supplier<List<Resource>> availableTemplatesSupplier() { return availableTemplatesSupplier; }
        public void setAvailableTemplatesSupplier(Supplier<List<Resource>> s) { this.availableTemplatesSupplier = s; }
        public Supplier<String> selectedTemplateSupplier() { return selectedTemplateSupplier; }
        public void setSelectedTemplateSupplier(Supplier<String> s) { this.selectedTemplateSupplier = s; }

        @Override
        public String toString() {
                return "KitElement[" + id + ":" + name + "]";
        }
}
