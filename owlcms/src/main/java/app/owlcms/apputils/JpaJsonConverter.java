/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.apputils;

import javax.persistence.AttributeConverter;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class JpaJsonConverter implements AttributeConverter<Object, String> {
	private static final ObjectMapper om = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(Object attribute) {
		try {
			return om.writeValueAsString(attribute);
		} catch (JacksonException ex) {
			// log.error("Error while transforming Object to a text datatable column as json string", ex);
			return null;
		}
	}

	@Override
	public Object convertToEntityAttribute(String dbData) {
		try {
			return om.readValue(dbData, Object.class);
		} catch (JacksonException ex) {
			// log.error("IO exception while transforming json text column in Object property", ex);
			return null;
		}
	}
}