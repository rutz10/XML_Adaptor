//import org.junit.jupiter.api.Test;
//
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.CsvSource;
//import org.rutz.AttributeLevelTransformation;
//import org.rutz.AttributeLevelTransformationException;
//import org.rutz.DataTypeTransformationException;
//import org.rutz.Mapping;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class AttributeLevelTransformationTest {
//
////    @Test
////    void numericTransformation() throws Exception {
////        Mapping mapping = new Mapping();
////        mapping.setXmlType("Integer");
////        mapping.setExprsn("val * 2");
////
////        String result = AttributeLevelTransformation.transform("5", mapping);
////        assertEquals("10", result);
////    }
//
//    @ParameterizedTest
//    @CsvSource({
//            "123.45, Double, 123.45",
//            "9876543210, Long, 9876543210",
//            "test, String, test"
//    })
//    void dataTypeConversions(String input, String type, String expected) throws Exception {
//        Object result = AttributeLevelTransformation.convertToDataTypeValue(input, type);
//        assertEquals(expected, result.toString());
//    }
//
////    @Test
////    void nullValueHandling() throws Exception {
////        Mapping mapping = new Mapping();
////        mapping.setXmlType("String");
////        String result = AttributeLevelTransformation.transform(null, mapping);
////        assertNull(result);
////    }
//
//    @Test
//    void invalidDataTypeConversion() {
//        assertThrows(DataTypeTransformationException.class, () -> {
//            AttributeLevelTransformation.convertToDataTypeValue("not_a_number", "Integer");
//        });
//    }
//
//    @Test
//    void expressionErrorHandling() {
//        Mapping mapping = new Mapping();
//        mapping.setExprsn("val / 0");
//        mapping.setJPath("$.risk");
//
//        assertThrows(AttributeLevelTransformationException.class, () -> {
//            AttributeLevelTransformation.transform("10", mapping);
//        });
//    }
//}
