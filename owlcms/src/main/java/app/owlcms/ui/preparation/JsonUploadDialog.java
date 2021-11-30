/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.preparation;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import app.owlcms.data.export.CompetitionData;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;
import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")
public class JsonUploadDialog extends Dialog {

    final static Logger logger = (Logger) LoggerFactory.getLogger(JsonUploadDialog.class);
    private UI ui;

    public JsonUploadDialog(UI ui) {
        this.ui = ui;

        H5 label = new H5(Translator.translate("ExportDatabase.WarningWillReplaceAll"));
        label.getStyle().set("color", "red");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setWidth("40em");

        TextArea ta = new TextArea(getTranslation("Errors"));
        ta.setHeight("20ex");
        ta.setWidth("80em");
        ta.setVisible(false);

        upload.addSucceededListener(event -> {
            try {
                processInput(event.getFileName(), buffer.getInputStream(), ta);
            } catch (IOException e) {
                ta.setValue(LoggerUtils./**/stackTrace(e));
            }
        });

        upload.addStartedListener(event -> {
            ta.clear();
            ta.setVisible(false);
        });

        H3 title = new H3(getTranslation("ExportDatabase.UploadJson"));
        VerticalLayout vl = new VerticalLayout(title, label, upload, ta);
        add(vl);
    }

    private void processInput(String fileName, InputStream inputStream, TextArea ta)
            throws StreamReadException, DatabindException, IOException {
        try {
            new CompetitionData().restore(inputStream);
            ui.getPage().reload();
        } catch (Throwable e1) {
            ta.setValue(LoggerUtils.exceptionMessage(e1));
        }
    }


//    private void resetAthletes() {
//        // delete all athletes and groups (naive version).
//        JPAService.runInTransaction(em -> {
//            List<Athlete> athletes = AthleteRepository.doFindAll(em);
//            for (Athlete a : athletes) {
//                em.remove(a);
//            }
//            em.flush();
//            return null;
//        });
//    }
//
//    private void resetGroups() {
//        // delete all groups (naive version).
//        JPAService.runInTransaction(em -> {
//            List<Group> oldGroups = GroupRepository.doFindAll(em);
//            for (Group g : oldGroups) {
//                em.remove(g);
//            }
//            em.flush();
//            return null;
//        });
//    }

//    private void updateAthletes() {
//        JPAService.runInTransaction(em -> {
//
//            Competition curC = Competition.getCurrent();
//            try {
//                Competition rCompetition = c.getCompetition();
//                // save some properties from current database that do not appear on spreadheet
//                rCompetition.setEnforce20kgRule(curC.isEnforce20kgRule());
//                rCompetition.setUseBirthYear(curC.isUseBirthYear());
//                rCompetition.setMasters(curC.isMasters());
//
//                // update the current competition with the new properties read from spreadsheet
//                BeanUtils.copyProperties(curC, rCompetition);
//                // update in database and set current to result of JPA merging.
//                Competition.setCurrent(em.merge(curC));
//
//                // update the athletes with the values read; create if not present.
//                // because the athletes in the file have got no Id, this will create
//                // new athletes if the file is reloaded.
//                athletes.stream().forEach(r -> {
//                    Athlete athlete = r.getAthlete();
//                    em.merge(athlete);
//                });
//                em.flush();
//            } catch (IllegalAccessException | InvocationTargetException | RuntimeException e) {
//                sb.append(e.getLocalizedMessage());
//            }
//
//            return null;
//        });
//    }

}
