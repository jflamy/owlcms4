package app.owlcms.data.record;

import java.io.InputStream;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.utils.LoggerUtils;

public class RecordDefinitionReader {

    static void createRecords(Workbook workbook, Map<String, Category> templates,
            EnumSet<AgeDivision> ageDivisionOverride,
            String localizedName) {
    
        JPAService.runInTransaction(em -> {
            Sheet sheet = workbook.getSheetAt(1);
            Iterator<Row> rowIterator = sheet.rowIterator();
            int iRow = 0;
            while (rowIterator.hasNext()) {
                int iColumn = 0;
                Row row;
                if (iRow == 0) {
                    // process header
                    row = rowIterator.next();
                }
                row = rowIterator.next();
    
                AgeGroup ag = new AgeGroup();
    
                Iterator<Cell> cellIterator = row.cellIterator();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    switch (iColumn) {
                    case 0: {
                        String cellValue = cell.getStringCellValue();
                        String trim = cellValue.trim();
                        ag.setCode(trim);
                    }
                        break;
                    case 1:
                        break;
                    case 2: {
                        String cellValue = cell.getStringCellValue();
                        ag.setAgeDivision(AgeDivision.getAgeDivisionFromCode(cellValue));
                    }
                        break;
                    case 3: {
                        String cellValue = cell.getStringCellValue();
                        if (cellValue != null && !cellValue.trim().isEmpty()) {
                            ag.setGender(cellValue.contentEquals("F") ? Gender.F : Gender.M);
                        }
                    }
                        break;
                    case 4: {
                        long cellValue = Math.round(cell.getNumericCellValue());
                        ag.setMinAge(Math.toIntExact(cellValue));
                    }
                        break;
                    case 5: {
                        long cellValue = Math.round(cell.getNumericCellValue());
                        ag.setMaxAge(Math.toIntExact(cellValue));
                    }
                        break;
                    case 6: {
                        boolean explicitlyActive = cell.getBooleanCellValue();
                        // age division is active according to spreadsheet, unless we are given an explicit
                        // list of age divisions as override (e.g. to setup tests or demos)
                        boolean active = ageDivisionOverride == null ? explicitlyActive
                                : ageDivisionOverride.stream()
                                        .anyMatch((Predicate<AgeDivision>) (ad) -> ad.equals(ag.getAgeDivision()));
                        ag.setActive(active);
                    }
                        break;
                    default:
                        break;
                    }
  
                    iColumn++;
                }
                em.persist(ag);
                iRow++;
            }
            Competition comp = Competition.getCurrent();
            Competition comp2 = em.contains(comp) ? comp : em.merge(comp);
            comp2.setAgeGroupsFileName(localizedName);
    
            return null;
        });
    }

    static void doInsertRecords(EnumSet<AgeDivision> es, String localizedName) {
        InputStream localizedResourceAsStream = AgeGroupRepository.class.getResourceAsStream(localizedName);
        try (Workbook workbook = WorkbookFactory
                .create(localizedResourceAsStream)) {
            RecordRepository.logger.info("loading configuration file {}", localizedName);
            createRecords(workbook, null, es, localizedName);
            workbook.close();
        } catch (Exception e) {
            RecordRepository.logger.error("could not process ageGroup configuration\n{}", LoggerUtils.stackTrace(e));
        }
    }

}
