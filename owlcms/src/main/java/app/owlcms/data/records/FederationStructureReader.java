/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.records;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.LoggerFactory;

import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * Read federation structure from an Excel file
 *
 * Example: qc < ca < panam < iwf but also ca < commonwealth < iwf and qc < francophonie At a competition, qc can break
 * all these records (if included in the records file). but usa would not.
 *
 * @author Jean-François Lamy
 *
 */
public class FederationStructureReader {

    private final static Logger logger = (Logger) LoggerFactory.getLogger(FederationStructureReader.class);

    private Map<String, Set<String>> membership = new HashMap<>();

    private Map<String, Set<String>> eligibility = new HashMap<>();

    public void readMembership(Workbook workbook, String localizedName) {

        String childFederation = "";
        String parentFederation = "";

        for (Sheet sheet : workbook) {
            int iRow = 0;

            processsheet: for (Row row : sheet) {
                if (row.getRowNum() != iRow) {
                    // gap in rows - empty row, exit.
                    break processsheet;
                }

                if (iRow == 0) {
                    iRow = 1;
                    continue;
                }

                int iColumn = 0;
                for (Cell cell : row) {
                    switch (iColumn) {
                    case 0: {
                        String cellValue = cell.getStringCellValue();
                        String trim = cellValue.trim();
                        if (trim.isEmpty()) {
                            break processsheet;
                        }
                        parentFederation = trim;
                        break;
                    }
                    case 1: {
                        String cellValue = cell.getStringCellValue();
                        cellValue = cellValue != null ? cellValue.trim() : cellValue;
                        childFederation = cellValue;
                        break;
                    }
                    }

                    iColumn++;
                }

                Set<String> childMemberOf = membership.get(childFederation);
                if (childMemberOf == null) {
                    childMemberOf = new HashSet<>();
                    membership.put(childFederation, childMemberOf);
                }
                childMemberOf.add(parentFederation);

                iRow++;
            }
        }
    }

    private void transitiveClosure(String newAncestorFederation, String childFederation, Collection<String> childAncestors) {
        childAncestors.add(newAncestorFederation);
        Set<String> parentMembers = membership.get(newAncestorFederation);
        if (parentMembers == null || parentMembers.isEmpty()) {
            return;
        }
        
        for (String parentMember : parentMembers) {
            transitiveClosure(parentMember, childFederation, childAncestors);
        }
    }

    public Map<String, Set<String>> buildStructure(Workbook workbook, String uri) throws Exception {
        try {
            readMembership(workbook, uri);

            for (Entry<String, Set<String>> e : membership.entrySet()) {
                String childFederation = e.getKey();
                Set<String> childParents = e.getValue();
                
                Set<String> childEligibility = new LinkedHashSet<>();
                childEligibility.add(childFederation); // athlete from federation F is eligible to beat F records.
                childEligibility.addAll(childParents);

                for (String parentFederation : childParents) {
                    transitiveClosure(parentFederation, childFederation, childEligibility);
                }
                eligibility.put(childFederation, childEligibility);
            }
            return eligibility;
        } catch (Exception e) {
            RecordRepository.logger.error("could not process federation structure\n{}",
                    LoggerUtils./**/stackTrace(e));
            throw e;
        }
    }

}
