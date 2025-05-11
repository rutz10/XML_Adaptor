# JSON to XML Mapping Utility

## Overview
This utility transforms JSON data into XML format based on a set of mapping rules defined in a CSV file. It supports straightforward field-to-element mapping, handling of JSON arrays, type conversion, and more advanced features like Collector Mappings and Derived Fields.

**Recent Enhancements & Fixes:**
*   **Improved Mapping Generation:** The system now more robustly handles complex CSV mapping configurations, correctly parsing and applying all defined rules, especially in scenarios where multiple rules might define the same local XML element name (`xPath`) under different parent elements or for different conditional purposes. This has resolved previous issues related to specific field mappings (like `otherfullname`) not being applied correctly in certain collector contexts.
*   **Enhanced Contextual Field Access:** Resolution of `jPath` and `conditionJPath` (especially simple field names) within the current JSON processing context (including items from collector feeds) has been made more precise.
*   **Improved Rule Evaluation Logic**: The `MappingRuleEvaluator` class has been significantly enhanced to ensure that XML elements are only created when appropriate. This involves a more explicit decision-making process at the end of the `evaluateRule` method, which categorizes rules as:
  * **Purely Structural**: These rules have neither a `jPath` nor a `conditionJPath` and always proceed to create their XML tag, allowing children to be processed.
  * **jPath-driven**: These rules have a `jPath` and will only proceed if the `jPath` successfully resolves to a non-empty value, or if a `defaultValue` (triggered by a `conditionJPath`) explicitly provides a value. If neither condition is met, the rule is skipped.
  * **Condition-driven (no jPath)**: These rules have a `conditionJPath` but no `jPath`. They proceed if the `conditionJPath` is met, with the value being the `defaultValue` if specified.

* **Enhanced Logging**: Detailed logging has been added to `MappingRuleEvaluator` to trace the evaluation process, including decisions made for each rule based on its `jPath`, `conditionJPath`, and `defaultValue`. This helps in debugging and understanding why certain XML elements are created or skipped.

* **Handling of Empty Values**: The logic now treats empty strings from a `jPath`-resolved value node as 'no value provided', allowing the final decision logic to correctly skip rules that don't yield usable data.

These changes ensure that the transformation process is more robust and that XML output accurately reflects the intended structure and data from the JSON input.

For a detailed explanation of recent fixes related to specific complex mapping scenarios, please see `specs/issue_resolution_otherfullname.md`.

## Features
*   **CSV-Based Mapping**: Define transformations using a simple CSV file format.
*   **Hierarchical XML**: Create complex, nested XML structures.
*   **JSON Path (jPath) Support**: Use JSONPath expressions to extract data from JSON.
*   **Collector Mappings**: Gather data from multiple JSON paths into unified XML structures.
*   **Derived Fields**: Generate XML elements based on conditions in the JSON data.
*   **Declarative Mapping**: Define transformations through CSV files, no coding required.
*   **Support for Complex Structures**: Handle nested objects, arrays, and various data types.

---

## Workflow
1. **Input**: The application receives a JSON file and a mapping CSV file (`mappings.csv`).
2. **Mapping**: The CSV defines how JSON fields map to XML elements/attributes, including data types and structure.
3. **Processing**: The main logic reads the JSON, applies the mapping, and writes the output as an XML file (`output.xml`).
4. **Output**: The resulting XML file reflects the structure and data as defined by the mapping.

---

## Mapping CSV Structure
The mapping CSV (`mappings.csv`) contains the following columns:
- **jPath**: JSONPath expression to locate data in the JSON input.
- **xPath**: Local XML element name. Does *not* need to be unique globally, only unique within its `parentXPath` combined with `order` or conditional logic.
- **isList**: Whether the JSON path points to a list/array.
- **jsonType**: Type of JSON data (string, number, boolean, object).
- **xmlType**: Type of XML element to create (element, attribute).
- **expression**: Expression to transform data (optional).
- **namespace**: XML namespace for the element (optional).
- **parentXPath**: The `xPath` of the parent element, establishing hierarchy. Parent `xPath` values used this way should uniquely identify structural mapping rules.
- **order**: Order of the element within its parent. Used to sequence siblings and differentiate rules with the same `xPath` under the same parent.
- **isCollector**: Whether this mapping collects data from multiple sources.
- **sourceFeedJPaths**: Semicolon-separated list of JPaths to collect data from.
- **conditionJPath**: JPath (or simple field name in current context) to check for existence (for derived fields).
- **defaultValue**: Value to use if the `conditionJPath` condition is met.

### Collector Mapping Details

- **`jPath`**:
  - **For a Collector Mapping Row**: Typically empty. If used, it would apply to the collector element itself (e.g., for conditional creation of the entire collector block, an advanced scenario not commonly used).
  - **For a Direct Child of a Collector (Structural Template Row)**: This mapping defines the repeating XML element (e.g., `<Branch>`). Its `jPath` is usually `.` (dot), signifying that this structure corresponds to one item from a source feed. The XML element defined by this template row will be created for each item in the `sourceFeedJPaths`.
  - **For Descendants within the Repeating Structure**:
    - `jPath="fieldName"`: Resolved against the current feed item (if the feed item is an object).
    - `jPath="path.to.field"`: Resolved against the current feed item.
    - `jPath="$.field.from.root"`: Resolved from the absolute root of the original input JSON.
    - `jPath="."`: Resolved as the feed item itself.
    - If `jPath` is empty, the field is a candidate for derivation using `conditionJPath` and `defaultValue`.

- **`parentXPath`**: Used to define the hierarchy. Children of a collector will have the collector's `xPath` as their `parentXPath`. The structural template will have the collector's `xPath` as its parent. Fields within the template will have the template's `xPath` as their parent.

- **`conditionJPath` / `defaultValue`**: For derived fields within the repeating structure, `conditionJPath` is resolved relative to the current feed item. If the field specified in `conditionJPath` exists in the current feed item, the condition is met. The `defaultValue` is then used.

### Example Collector Mapping

Consider creating a `<Company>/<Branches>` structure, where each `<Branch>` can be sourced from `$.company.branchList[*]`, `$.company.mainBranch`, or a legacy `$.alternateBranchInfo`.

```csv
| jPath                 | xPath                             | isList | jsonType | xmlType | expression | namespace | parentXPath        | order | isCollector | sourceFeedJPaths                                      | conditionJPath | defaultValue   |
|-----------------------|-----------------------------------|--------|----------|---------|------------|-----------|--------------------|-------|-------------|-------------------------------------------------------|----------------|----------------|
|                       | Company                           | No     | object   | element |            |           |                    | 0     | No          |                                                       |                |                |
| $.company.name        | CompanyName                       | No     | string   | element |            |           | Company            | 10    | No          |                                                       |                |                |
|                       | Branches                          | No     |          | element |            |           | Company            | 20    | Yes         | `$.company.branchList[*];$.company.mainBranch;$.alternateBranchInfo` |                |                |
| .                     | Branch                            | No     | object   | element |            |           | Branches           | 10    | No          |                                                       |                |                |
| id                    | BranchID                          | No     | string   | element |            |           | Branch             | 10    | No          |                                                       |                |                |
| name                  | BranchName                        | No     | string   | element |            |           | Branch             | 20    | No          |                                                       |                |                |
| location.city         | City                              | No     | string   | element |            |           | Branch             | 30    | No          |                                                       |                |                |
| .                     | LegacyCode                        | No     | string   | element |            |           | Branch             | 35    | No          |                                                       |                |                |
|                       | Status                            | No     | string   | element |            |           | Branch             | 40    | No          |                                                       | isActive       | Active         |
|                       | Status                            | No     | string   | element |            |           | Branch             | 41    | No          |                                                       |                | Inactive       |
| type                  | BranchType                        | No     | string   | element |            |           | Branch             | 50    | No          |                                                       |                |                |
|                       | Region                            | No     | string   | element |            |           | Branch             | 60    | No          |                                                       | name           | DefaultRegion  |
```

This example demonstrates how `jPath` and `conditionJPath` being resolved against the current feed item enable flexible data extraction and conditional logic within collector structures.

---

## Main Components

### 1. `JsonToXmlSteam.java`
This is the core class responsible for orchestrating the JSON to XML transformation using StAX (Streaming API for XML) for efficient writing.

#### Key Methods:
- **`transformJsonToXml(String jsonString, List<Mapping> mappings, String outputFilePath)`**: The main public entry point. Takes the JSON string, the list of root-level `Mapping` objects (already sorted by `MappingGenerator`), and the output file path. It parses the JSON, initializes an `XMLStreamWriter`, **sorts the top-level `mappings` list** using `Comparator.comparingInt(Mapping::getOrder)` (to ensure top-level elements respect the order), and then iterates through this sorted list. For each mapping, it finds the corresponding JSON data using `convertJsonPathToJsonPointer` and Jackson's `at()`, and calls `writeXmlElement` for non-missing nodes. Writes the XML start and end document tags.
- **`writeXmlElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping)`**: Handles the writing of a single XML element based on the mapping's `xPath`. It splits the `xPath` by '/' to handle potential nesting, writes the necessary parent start elements, calls `processElement` to handle the actual node content, and then writes the corresponding end elements. The order of sibling elements written by recursive calls from `processChildMappings` is determined by the pre-sorted `childMappings` list within the `Mapping` object.
- **`processElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName)`**: Determines the type of the current `jsonNode` (array, object, or simple value) and delegates the processing to the appropriate method (`processArrayElement`, `processObjectElement`, or `processValueNode`).
- **`processArrayElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName)`**: Iterates through items in a JSON array (`jsonNode`). For each item, it writes a start element (`elementName`). If the item is a simple value, it transforms it using `AttributeLevelTransformation.transform` and writes it as character data. If the item is complex (object/array), it recursively calls `processChildMappings`. Finally, it writes the end element.
- **`processObjectElement(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName)`**: Writes the start element (`elementName`) for a JSON object. If the mapping has child mappings defined (`mapping.getChildMappings()`), it calls `processChildMappings` to handle the object's nested structure based on those child mappings. Writes the end element.
- **`processValueNode(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping, String elementName)`**: Handles simple JSON values (string, number, boolean, null). It writes the start element (`elementName`), transforms the value using `AttributeLevelTransformation.transform`, writes the transformed value as character data, and writes the end element.
- **`processChildMappings(XMLStreamWriter writer, JsonNode jsonNode, Mapping mapping)`**: Iterates through the `childMappings` associated with the current `mapping`. For each child mapping, it finds the corresponding nested JSON node within the current `jsonNode` context using `convertJsonPathToJsonPointer` and Jackson's `at()`, and then recursively calls `writeXmlElement` to process that child mapping and node. Skips mappings where the `xPath` contains '@' (assumed to be attributes, which are not explicitly handled in the current code).
- **`convertJsonPathToJsonPointer(String jsonPath)`**: A static utility method to convert a standard JSONPath expression (like `$.path.to.node` or `$.array[*]`) into the JSON Pointer format (like `/path/to/node` or `/array/`) required by Jackson's `JsonNode.at()` method for efficient node lookup. Handles the root path `$` as well.

#### Example Flow:
1. `transformJsonToXml` parses JSON and gets mappings.
2. It iterates mappings, finds JSON nodes via `convertJsonPathToJsonPointer`.
3. For each mapping/node, `writeXmlElement` is called.
4. `writeXmlElement` handles nesting and calls `processElement`.
5. `processElement` delegates to `processArrayElement`, `processObjectElement`, or `processValueNode` based on JSON type.
6. `processArrayElement` and `processObjectElement` may call `processChildMappings` for nested structures.
7. `processChildMappings` recursively calls `writeXmlElement` for sub-mappings.
8. `processValueNode` and `processArrayElement` (for simple items) use `AttributeLevelTransformation.transform` before writing text.

### 2. `Mapping.java`
A Plain Old Java Object (POJO) representing a single mapping rule read from the CSV file.
- **Fields**: Stores `jPath` (source JSONPath), `xPath` (target XML path), `isList` (boolean flag), `jsonType`, `xmlType`, `exprsn` (JEXL expression), `namespace`, and `parentXPath` (for building hierarchy).
- **`childMappings`**: A `List<Mapping>` to hold nested mappings, allowing for the representation of hierarchical structures defined in the CSV via the `parentXPath` column.
- **Methods**: Standard getters, setters, and an `addChildMapping` convenience method used by `MappingGenerator`.

### 3. `AttributeLevelTransformation.java`
Handles the transformation and type conversion of individual JSON values before they are written to XML.
- **`transform(String sourceAttributeValue, Mapping mapping)`**: The primary method called by `JsonToXmlSteam`. It takes the original string value and the corresponding `Mapping`. If the mapping contains an expression (`exprsn`), it first attempts to convert the `sourceAttributeValue` to the target `xmlType` using `convertToDataTypeValue`. It then sets up a JEXL context (`MapContext`) with the potentially converted value available as the variable `val`. Finally, it invokes `ExpressionEvaluator.attrEval` to execute the expression. Returns the expression result or the original value if no expression exists. Includes error handling for conversion and evaluation.
- **`convertToDataTypeValue(String value, String dataType)`**: Attempts to parse the input `value` string into the specified `dataType` (supports "Double", "Integer", "Long", "Date"). Returns the converted object or the original string if the type is not recognized or conversion fails (throwing `DataTypeTransformationException`).

### 4. `ExpressionEvaluator.java`
Manages the evaluation of JEXL (Java Expression Language) expressions defined in the `exprsn` column of the mapping CSV.
- **Initialization**: Statically initializes a shared, configured Apache Commons `JexlEngine`. The engine is set up with caching, strict mode (errors on undefined variables/functions), and registers custom functions from `TransformerExpressionFunctions` under the `fmfcn` namespace.
- **`evaluate(String expression, JexlContext context)`**: A general method to evaluate a given JEXL expression string within a provided context.
- **`attrEval(String expression, JexlContext context, Class<T> returnType)`**: A specialized version used by `AttributeLevelTransformation` that evaluates the expression and casts the result to the specified `returnType`.
- **`buildJexlContext(Map<String, Object> variables)`**: Utility method to create a `JexlContext` (specifically a `MapContext`) from a map of variable names and values.

### 5. `TransformerExpressionFunctions.java`
Contains custom static methods that can be invoked within JEXL expressions using the `fmfcn` namespace (e.g., `fmfcn:convertUTCToEST(val)`).
- **`addFive(int val)`**: Example function (adds 1000, name is misleading).
- **`convertUTCToEST(String utcDateString)`**: Converts an ISO UTC date/time string to the "America/New_York" time zone and returns the full local date/time string.
- **`convertUTCToESTDateOnly(String utcDateString)`**: Converts an ISO UTC date/time string to the "America/New_York" time zone and returns only the local date string (YYYY-MM-DD).

### 6. `JsonUtils.java`
Provides utility functions for JSON manipulation using the Jackson library.
- **`copyObjectToNewContainer(...)`**: A specific utility function designed to find objects within a specified array in a JSON structure (matching based on a hardcoded "departmentName" field), copy them, and place the copies into a new array added to the root of the JSON structure. Its parameters and implementation suggest a very tailored use case.

### 7. `MappingGenerator.java`
Responsible for reading the `mappings.csv` file and building the hierarchical structure of `Mapping` objects.
- **`readMappingsFromCsv(String filePath)`**: Reads the CSV file using OpenCSV, skipping the header. It uses a `Map<String, Mapping>` (`mappingRegistry`) to keep track of created mappings by their `xPath`. It initializes a `virtualRoot` mapping to act as the parent for all top-level mappings. Calls `processMappingsIteratively` to build the hierarchy, then calls `sortMappingsRecursively` to sort it based on the `order` field. Finally, it returns the sorted list of direct children of the `virtualRoot`.
- **`processMappingsIteratively(List<String[]> rows, Map<String, Mapping> mappingRegistry, Mapping virtualRoot)`**: Processes the CSV rows iteratively. In each pass, it attempts to create `Mapping` objects for rows whose `parentXPath` is either empty (top-level) or already exists in the `mappingRegistry`. It reads the `order` column (defaulting to 0 if invalid/missing) and passes it to the `Mapping` constructor. This iterative approach correctly builds the hierarchy even if parent rows appear after child rows in the CSV. Newly created mappings are added to their parent's `childMappings` list and registered. The loop continues until a full pass adds no new mappings.
- **`sortMappingsRecursively(Mapping mapping)`**: After the hierarchy is built by `processMappingsIteratively`, this method recursively traverses the mapping tree (depth-first) and sorts the `childMappings` list within each `Mapping` object using `Comparator.comparingInt(Mapping::getOrder)`. This ensures siblings are ordered correctly at all levels.
- **`printMappingHierarchy(Mapping mapping, int level)`**: A recursive helper method for debugging, printing the constructed mapping hierarchy (after sorting) to the console.

*(Note: `MappingGenerator1.java` appears to be a variant incorporating Spring's `ResourcePatternResolver` but might have issues in its current implementation regarding file handling.)*

### 8. `MappingRuleEvaluator.java`
A helper class introduced to encapsulate rule evaluation logic, making the `JsonToXmlSteam` class cleaner and more maintainable.
- **Purpose**: Evaluates conditions, determines XML element content, and manages JSON context for child mappings.
- **Integration**: Works with `JsonToXmlSteam` to process CSV mapping rules, handle conditional logic, and generate XML output.
- **Benefits**: Improves readability, enhances testability, and increases scalability by handling diverse JSON structures and fields without additional hardcoded logic.

### Custom Exceptions
- **`AttributeLevelTransformationException.java`**: Custom checked exception thrown by `AttributeLevelTransformation` on errors during expression evaluation.
- **`DataTypeTransformationException.java`**: Custom checked exception thrown by `AttributeLevelTransformation.convertToDataTypeValue` on data type parsing errors.

---

## Example Usage (Conceptual)
1. Prepare your input JSON data (e.g., in a file or string).
2. Define the transformation rules in `mappings.csv` located in `src/main/resources/`. Ensure `parentXPath` correctly links hierarchical elements and use the `order` column to specify sequence.
3. Load the mappings using `MappingGenerator`:
   ```java
   List<Mapping> mappings = MappingGenerator.readMappingsFromCsv("src/main/resources/mappings.csv");
   ```
4. Load the JSON data:
   ```java
   String jsonString = new String(Files.readAllBytes(Paths.get("src/main/resources/data.json")));
   // Or have the JSON string from another source
   ```
5. Execute the transformation using `JsonToXmlSteam`:
   ```java
   JsonToXmlSteam.transformJsonToXml(jsonString, mappings, "output.xml");
   ```
6. The resulting XML will be written to `output.xml` in the project's root directory.

---

## Detailed Example with Nesting and Ordering

Let's illustrate with a concrete example.

**Input JSON (`src/main/resources/example.json`)**

```json
{
  "transactionId": "TXN123",
  "timestamp": "2024-01-15T10:30:00Z",
  "customer": {
    "id": "CUST001",
    "name": "Alice Smith",
    "address": {
      "street": "123 Main St",
      "city": "Anytown",
      "zip": "12345"
    }
  },
  "items": [
    {
      "sku": "SKU001",
      "description": "Product A",
      "quantity": 2,
      "price": 10.50
    },
    {
      "sku": "SKU002",
      "description": "Product B",
      "quantity": 1,
      "price": 25.00
    }
  ],
  "summary": {
    "totalItems": 3,
    "totalAmount": 46.00
  }
}
```

**Mapping CSV (`src/main/resources/mappings.csv`)**

```csv
jPath,xPath,isList,jsonType,xmlType,expression,namespace,parentXPath,order,isCollector,sourceFeedJPaths,conditionJPath,defaultValue
$.transactionId,Order/Header/TransactionID,No,string,element,,,Order/Header,10,No,,,
$.timestamp,Order/Header/OrderDate,No,string,element,fmfcn:convertUTCToESTDateOnly(val),,Order/Header,20,No,,,
$.customer.id,Order/Customer/CustomerID,No,string,element,,,Order/Customer,10,No,,,
$.customer.name,Order/Customer/CustomerName,No,string,element,,,Order/Customer,20,No,,,
$.customer.address.street,Order/Customer/Address/StreetLine,No,string,element,,,Order/Customer/Address,10,No,,,
$.customer.address.city,Order/Customer/Address/City,No,string,element,,,Order/Customer/Address,20,No,,,
$.customer.address.zip,Order/Customer/Address/PostalCode,No,string,element,,,Order/Customer/Address,30,No,,,
$.items[*],Order/OrderLines/LineItem,Yes,object,element,,,Order/OrderLines,10,Yes,$.company.branchList[*];$.company.mainBranch;$.alternateBranchInfo,,
$.sku,Order/OrderLines/LineItem/SKU,No,string,element,,,Order/OrderLines/LineItem,10,No,,,
$.description,Order/OrderLines/LineItem/Description,No,string,element,,,Order/OrderLines/LineItem,20,No,,,
$.quantity,Order/OrderLines/LineItem/Qty,No,number,element,,,Order/OrderLines/LineItem,30,No,,,
$.price,Order/OrderLines/LineItem/UnitPrice,No,number,element,,,Order/OrderLines/LineItem,40,No,,,
$.summary.totalItems,Order/Summary/ItemCount,No,number,element,,,Order/Summary,10,No,,,
$.summary.totalAmount,Order/Summary/TotalValue,No,number,element,,,Order/Summary,20,No,,,
$,Order,No,object,element,,,,0,No,,,
$.customer,Order/Customer,No,object,element,,,Order,20,No,,,
$.items,Order/OrderLines,No,array,element,,,Order,30,No,,,
$.summary,Order/Summary,No,object,element,,,Order,40,No,,,
$.customer.address,Order/Customer/Address,No,object,element,,,Order/Customer,30,No,,,
$.*,Order/Header,No,object,element,,,Order,10,No,,,
```

**Explanation of `mappings.csv`:**

*   **Hierarchy**: `parentXPath` is used to define the structure (e.g., `Order/Header/TransactionID` has `Order/Header` as its parent).
*   **Ordering**:
    *   Under `Order`, `Header` comes first (order 10), then `Customer` (20), `OrderLines` (30), and `Summary` (40).
    *   Under `Order/Header`, `TransactionID` comes first (10), then `OrderDate` (20).
    *   Under `Order/Customer`, `CustomerID` (10), `CustomerName` (20), then `Address` (30).
    *   Under `Order/Customer/Address`, `StreetLine` (10), `City` (20), `PostalCode` (30).
    *   Under `Order/OrderLines/LineItem`, `SKU` (10), `Description` (20), `Qty` (30), `UnitPrice` (40).
*   **Lists**: `$.items[*]` maps to `Order/OrderLines/LineItem` with `isList=Yes`. The subsequent mappings for `sku`, `description`, etc., have `Order/OrderLines/LineItem` as their parent, defining the structure *within* each `LineItem`.
*   **Expressions**: `OrderDate` uses `fmfcn:convertUTCToESTDateOnly(val)` to format the timestamp.
*   **Container Elements**: Rows like `$,Order,...` and `$.customer,Order/Customer,...` define the container elements themselves. Their `jPath` points to the corresponding JSON object/array, and `parentXPath` links them into the hierarchy.

**Expected Output XML (`output.xml`)**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Order>
    <Header>
        <TransactionID>TXN123</TransactionID>
        <OrderDate>2024-01-15</OrderDate>
    </Header>
    <Customer>
        <CustomerID>CUST001</CustomerID>
        <CustomerName>Alice Smith</CustomerName>
        <Address>
            <StreetLine>123 Main St</StreetLine>
            <City>Anytown</City>
            <PostalCode>12345</PostalCode>
        </Address>
    </Customer>
    <OrderLines>
        <LineItem>
            <SKU>SKU001</SKU>
            <Description>Product A</Description>
            <Qty>2</Qty>
            <UnitPrice>10.50</UnitPrice>
        </LineItem>
        <LineItem>
            <SKU>SKU002</SKU>
            <Description>Product B</Description>
            <Qty>1</Qty>
            <UnitPrice>25.00</UnitPrice>
        </LineItem>
    </OrderLines>
    <Summary>
        <ItemCount>3</ItemCount>
        <TotalValue>46.00</TotalValue>
    </Summary>
</Order>
```

This example demonstrates how `parentXPath` creates the nested structure and how the `order` column dictates the sequence of sibling elements at each level (e.g., `Header`, `Customer`, `OrderLines`, `Summary` appear in that specific order directly under `Order`).

---

## Testing
Unit tests are provided in the `src/test/java/` directory for key components, including:
- `JsonToXmlStreamTest.java`
- `JsonUtilsTest.java`
- `AttributeLevelTransformationTest.java`

---

## Extending the Utility
- To add new transformation logic, extend `AttributeLevelTransformation` or `ExpressionEvaluator`.
- To support new mapping features, update `Mapping.java` and the relevant processing logic in `JsonToXmlSteam.java`.

---

## File Overview
- **`src/main/java/org/rutz/JsonToXmlSteam.java`**: Core StAX-based transformation engine.
- **`src/main/java/org/rutz/Mapping.java`**: Represents a single mapping rule (POJO).
- **`src/main/java/org/rutz/MappingGenerator.java`**: Reads CSV and builds the `Mapping` hierarchy.
- **`src/main/java/org/rutz/AttributeLevelTransformation.java`**: Handles value conversion and prepares context for expressions.
- **`src/main/java/org/rutz/ExpressionEvaluator.java`**: Evaluates JEXL expressions.
- **`src/main/java/org/rutz/TransformerExpressionFunctions.java`**: Defines custom functions for JEXL.
- **`src/main/java/org/rutz/JsonUtils.java`**: JSON utility functions (currently specific).
- **`src/main/java/org/rutz/MappingRuleEvaluator.java`**: Evaluates mapping rules, integrates with `JsonToXmlSteam` for processing CSV mapping rules and generating XML output.
- **`src/main/resources/mappings.csv`**: Defines the JSON-to-XML mapping rules.
- **`src/main/resources/*.json`**: Example input JSON files.
- **`output.xml`**: Default output file name.

---

## Logging
Logs are written to the `logs/` directory for debugging and traceability.

---

## Contact
For questions or contributions, please refer to the project maintainer or open an issue.