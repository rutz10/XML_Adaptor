package org.rutz;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.rutz.Mapping;

public class AttributeLevelTransformation implements Serializable {

    public static final String INTEGER = "Integer";
    public static final String LONG = "Long";
    public static final String UTC = "UTC";
    public static final String DATE = "Date";
    public static final String STRING = "String";
    public static final String DOUBLE = "Double";

    private static final Logger logger = Logger.getLogger(AttributeLevelTransformation.class.getName());

    public static String transform(String sourceAttributeValue, Mapping mapping, JsonNode expressionContext) throws AttributeLevelTransformationException {
        String exprsn = mapping.getExprsn();
        if (exprsn != null && !exprsn.isEmpty()) {
            Object valueToUseInExpression = sourceAttributeValue;
            if (mapping.getXmlType() != null && sourceAttributeValue != null) {
                try {
                    valueToUseInExpression = convertToDataTypeValue(sourceAttributeValue, mapping.getXmlType());
                } catch (DataTypeTransformationException e) {
                    logger.log(Level.WARNING, "Data type conversion failed for value: " + sourceAttributeValue + " to type: " + mapping.getXmlType(), e);
                }
            }

            JexlContext jexlContext = new MapContext();
            jexlContext.set("val", valueToUseInExpression);
            // jexlContext.set("globalRoot", expressionContext); // Example if needed by functions

            try {
                Object result = ExpressionEvaluator.attrEval(exprsn, jexlContext, Object.class);
                return result != null ? String.valueOf(result) : null;
            } catch (Exception e) {
                throw new AttributeLevelTransformationException("Error evaluating JEXL expression: " + exprsn, e);
            }
        } else {
            return sourceAttributeValue;
        }
    }

    public static Object convertToDataTypeValue(String value, String dataType) throws DataTypeTransformationException {
        if (value == null) return null;
        try {
            switch (dataType.toLowerCase()) {
                case "double":
                    return Double.parseDouble(value);
                case "integer":
                    return Integer.parseInt(value);
                case "long":
                    return Long.parseLong(value);
                case "date":
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    return sdf.parse(value);
                default:
                    return value;
            }
        } catch (NumberFormatException | ParseException e) {
            throw new DataTypeTransformationException("Failed to convert value '" + value + "' to data type '" + dataType + "'", e);
        }
    }
}