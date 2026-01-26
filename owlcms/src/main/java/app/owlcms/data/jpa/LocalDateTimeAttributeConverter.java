/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.jpa;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import app.owlcms.data.config.Config;

@Converter(autoApply = true)
public class LocalDateTimeAttributeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(LocalDateTime attribute) {
        if (attribute == null) return null;
        if (Config.getCurrent().isLocalDateTimeUtcNormalized()) {
            // Store as UTC
            return Timestamp.from(attribute.toInstant(ZoneOffset.UTC));
        } else {
            // Store as system default
            return Timestamp.valueOf(attribute);
        }
    }

    @Override
    public LocalDateTime convertToEntityAttribute(Timestamp dbData) {
        if (dbData == null) return null;
        if (Config.getCurrent().isLocalDateTimeUtcNormalized()) {
            // Read as UTC
            return dbData.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
        } else {
            // Read as system default
            return dbData.toLocalDateTime();
        }
    }
}
