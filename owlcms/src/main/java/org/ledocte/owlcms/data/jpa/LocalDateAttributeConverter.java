/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Affero GNU License amended with the
 * Commons Clause.
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.data.jpa;

import java.sql.Date;
import java.time.LocalDate;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * The Class LocalDateAttributeConverter.
 */
@Converter(autoApply = true)
public class LocalDateAttributeConverter implements AttributeConverter<LocalDate, Date> {
	
    /* (non-Javadoc)
     * @see javax.persistence.AttributeConverter#convertToDatabaseColumn(java.lang.Object)
     */
    @Override
    public Date convertToDatabaseColumn(LocalDate locDate) {
    	return (locDate == null ? null : Date.valueOf(locDate));
    }

    /* (non-Javadoc)
     * @see javax.persistence.AttributeConverter#convertToEntityAttribute(java.lang.Object)
     */
    @Override
    public LocalDate convertToEntityAttribute(Date sqlDate) {
    	return (sqlDate == null ? null : sqlDate.toLocalDate());
    }
}