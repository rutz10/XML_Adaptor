package org.rutz;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributeLevelTransformation implements Serializable {

    public static final String INTEGER = "Integer";
    public static final String LONG = "Long";
    public static final String UTC = "UTC";
    public static final String DATE = "Date";
    public static final String STRING = "String";
    public static final String DOUBLE = "Double";

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeLevelTransformation.class);

    public static String transform(String sourceAttributeValue, Mapping mapping) throws Exception {
        String result;
        JexlContext context = new MapContext();
        if (null != sourceAttributeValue && null != mapping.getExprsn() && !"".equals(mapping.getExprsn())) {
            try {
                if (sourceAttributeValue == null) {
                    context.set("val", null);
                } else {
                    context.set("val", convertToDataTypeValue(sourceAttributeValue, mapping.getXmlType()));
                }
            } catch (DataTypeTransformationException e) {
                context.set("val", null);
                LOGGER.error("Error during setting jexl context for attrName"
                        + mapping.getJPath() + " Value " + sourceAttributeValue, e);
            }
        }
        try {
            result = ExpressionEvaluator.attrEval(mapping.getExprsn(), context, String.class);
        } catch (Exception e) {
            throw new AttributeLevelTransformationException("Transformation failed: "
                    + mapping.getJPath().split("\\.")[1] + " Val: " + sourceAttributeValue);
        }
        return result == null ? sourceAttributeValue : result;
    }

    public static Object convertToDataTypeValue(String value, String dataType) throws DataTypeTransformationException {
        try {
            if (null == value || "null".equalsIgnoreCase(value)) {
                return null;
            }
            if (dataType.equalsIgnoreCase(DOUBLE)) {
                return Double.parseDouble(value);
            } else if (dataType.equalsIgnoreCase(INTEGER)) {
                return Integer.parseInt(value);
            } else if (dataType.equalsIgnoreCase(LONG)) {
                return Long.valueOf(value);
            } else if (dataType.equalsIgnoreCase(DATE)) {
                SimpleDateFormat formatterDT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                formatterDT.setTimeZone(TimeZone.getDefault());
                return formatterDT.parse(value);
            } else {
                return value;
            }
        } catch (Exception e) {
            throw new DataTypeTransformationException("Datatype: " + dataType + " Value: " + value);
        }
    }
}