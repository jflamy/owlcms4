package app.owlcms.nui.preparation;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;
import app.owlcms.spreadsheet.JXLSWorkbookStreamSource;

public record KitElement(String id, String name, String extension, Path isp, int count,
        BiFunction<List<Athlete>, Group, JXLSWorkbookStreamSource> writerFactory,
        BiFunction<List<Athlete>, Group, Optional<Exception>> preCheck) {
}
