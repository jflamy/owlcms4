package app.owlcms.apputils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import java.io.IOException;

public class JpaJsonConverter implements AttributeConverter<Object, String> {
    private static final ObjectMapper om = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Object attribute) {
        try {
            return om.writeValueAsString(attribute);
        } catch (JsonProcessingException ex) {
            //log.error("Error while transforming Object to a text datatable column as json string", ex);
            return null;
        }
    }

    @Override
    public Object convertToEntityAttribute(String dbData) {
        try {
            return om.readValue(dbData, Object.class);
        } catch (IOException ex) {
            //log.error("IO exception while transforming json text column in Object property", ex);
            return null;
        }
    }
}