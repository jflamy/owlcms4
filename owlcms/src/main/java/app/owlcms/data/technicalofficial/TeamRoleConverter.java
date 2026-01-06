/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.technicalofficial;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * JPA converter for TeamRole that handles legacy database values.
 * Specifically handles "MARSHAL" (single L) which was used in earlier versions
 * before standardizing to "MARSHALL" (double L).
 */
@Converter(autoApply = false)
public class TeamRoleConverter implements AttributeConverter<TeamRole, String> {

    @Override
    public String convertToDatabaseColumn(TeamRole attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public TeamRole convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        // Handle legacy spelling with single L
        if ("MARSHAL".equalsIgnoreCase(dbData)) {
            return TeamRole.MARSHALL;
        }
        // Standard enum lookup
        try {
            return TeamRole.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
