//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.DisplayName;
//import org.rutz.PreProcess;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
//class PreProcessTest {
//
//    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
//
//    @Test
//    @DisplayName("Should remove null fields from a JSON object")
//    void testRemoveNullFields() throws Exception {
//        // Arrange
//        String inputJson = "{\"name\":\"John\",\"age\":null,\"city\":null,\"address\":{\"street\":null,\"country\":\"USA\"}}";
//
//        // Act
//        String cleanedJson = PreProcess.cleanGraphQLResponse(inputJson);
//
//        // Assert
//        assertThat(cleanedJson)
//                .contains("\"name\":\"John\"")
//                .contains("\"country\":\"USA\"")
//                .doesNotContain("\"age\":null")
//                .doesNotContain("\"city\":null")
//                .doesNotContain("\"street\":null");
//    }
//
//    @Test
//    @DisplayName("Should remove empty strings from a JSON object")
//    void testRemoveEmptyStrings() throws Exception {
//        // Arrange
//        String inputJson = "{\"name\":\"John\",\"email\":\"\",\"phone\":\"   \",\"address\":{\"city\":\"\"}}";
//
//        // Act
//        String cleanedJson = PreProcess.cleanGraphQLResponse(inputJson);
//
//        // Assert
//        assertThat(cleanedJson)
//                .contains("\"name\":\"John\"")
//                .doesNotContain("\"email\":\"\"")
//                .doesNotContain("\"phone\":\"   \"")
//                .doesNotContain("\"city\":\"\"");
//    }
//
//    @Test
//    @DisplayName("Should remove empty arrays from a JSON object")
//    void testRemoveEmptyArrays() throws Exception {
//        // Arrange
//        String inputJson = "{\"name\":\"John\",\"hobbies\":[],\"skills\":[],\"address\":{\"tags\":[]}}";
//
//        // Act
//        String cleanedJson = PreProcess.cleanGraphQLResponse(inputJson);
//
//        // Assert
//        assertThat(cleanedJson)
//                .contains("\"name\":\"John\"")
//                .doesNotContain("\"hobbies\":[]")
//                .doesNotContain("\"skills\":[]")
//                .doesNotContain("\"tags\":[]");
//    }
//
//    @Test
//    @DisplayName("Should remove empty nested objects")
//    void testRemoveEmptyNestedObjects() throws Exception {
//        // Arrange
//        String inputJson = "{\"name\":\"John\",\"address\":{},\"contact\":{\"email\":\"\"}}";
//
//        // Act
//        String cleanedJson = PreProcess.cleanGraphQLResponse(inputJson);
//
//        // Assert
//        assertThat(cleanedJson)
//                .contains("\"name\":\"John\"")
//                .doesNotContain("\"address\":{}")
//                .doesNotContain("\"contact\":{}");
//    }
//
//    @Test
//    @DisplayName("Should handle arrays with mixed content")
//    void testCleanArrayWithMixedContent() throws Exception {
//        // Arrange
//        String inputJson = "{\"users\":[" +
//                "{\"name\":\"John\",\"age\":null}," +
//                "{\"name\":\"\",\"age\":30}," +
//                "{\"name\":\"Alice\",\"skills\":[]}" +
//                "]}";
//
//        // Act
//        String cleanedJson = PreProcess.cleanGraphQLResponse(inputJson);
//
//        // Assert
//        assertThat(cleanedJson)
//                .contains("\"name\":\"Alice\"")
//                .contains("\"age\":30")
//                .doesNotContain("\"name\":\"\"")
//                .doesNotContain("\"age\":null")
//                .doesNotContain("\"skills\":[]");
//    }
//
//    @Test
//    @DisplayName("Should handle deeply nested JSON structures")
//    void testDeepNestedJsonCleaning() throws Exception {
//        // Arrange
//        String inputJson = "{" +
//                "\"user\":{" +
//                "\"profile\":{" +
//                "\"details\":{" +
//                "\"name\":\"John\"," +
//                "\"email\":\"\"," +
//                "\"address\":null" +
//                "}" +
//                "}" +
//                "}" +
//                "}";
//
//        // Act
//        String cleanedJson = PreProcess.cleanGraphQLResponse(inputJson);
//
//        // Assert
//        assertThat(cleanedJson)
//                .contains("\"name\":\"John\"")
//                .doesNotContain("\"email\":\"\"")
//                .doesNotContain("\"address\":null");
//    }
//
//    @Test
//    @DisplayName("Should handle null input gracefully")
//    void testNullInput() throws Exception {
//        // Arrange
//        String inputJson = "null";
//
//        // Act & Assert
//        assertThrows(JsonProcessingException.class,
//                () -> PreProcess.cleanGraphQLResponse(inputJson));
//    }
//
//    @Test
//    @DisplayName("Should handle invalid JSON input")
//    void testInvalidJsonInput() {
//        // Arrange
//        String invalidJson = "{invalid json}";
//
//        // Act & Assert
//        assertThrows(JsonProcessingException.class,
//                () -> PreProcess.cleanGraphQLResponse(invalidJson));
//    }
//}