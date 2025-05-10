# CSV Mapping Format Specification

This document provides detailed specifications for creating mapping CSV files for the JSON to XML transformation utility, including guidelines for programmatic generation of these files.

## CSV Format Overview

The mapping CSV file defines how JSON data should be transformed into XML. Each row in the CSV represents a single mapping rule for creating XML elements from JSON data.

## Column Definitions

| Column Name | Description | Required | Example |
|-------------|-------------|:--------:|---------|
| `jPath` | JSON path to extract data from. Can be a simple field name for direct access within the current JSON context. | Yes* | `$.company.name` or `active` |
| `xPath` | Local XML element name. Does *not* need to be unique globally, only unique within its `parentXPath` combined with `order` or conditional logic. | Yes | `Name`, `Status` |
| `isList` | Whether the JSON path points to a list/array | Yes | `No` or `Yes` |
| `jsonType` | Type of JSON data (string, number, boolean, object) | No | `string` |
| `xmlType` | Type of XML element to create (element, attribute) | Yes | `element` |
| `expression` | Expression to transform data (optional) | No | |
| `namespace` | XML namespace for the element (optional) | No | |
| `parentXPath` | The `xPath` of the parent element, establishing hierarchy. Parent `xPath` values used this way should uniquely identify structural mapping rules. | No** | `CompanyInfo`, `Employee` |
| `order` | Order of the element within its parent. Used to sequence siblings and differentiate rules with the same `xPath` under the same parent. | Yes | `10`, `20` |
| `isCollector` | Whether this mapping collects data from multiple sources | Yes | `Yes` or `No` |
| `sourceFeedJPaths` | Semicolon-separated list of JPaths to collect data from | No*** | `$.company.mainDepartments[*];$.company.satelliteOffices.departments[*]` |
| `conditionJPath` | JPath (or simple field name in current context) to check for existence (for derived fields) | No | `active` or `otherfullname` |
| `defaultValue` | Value to use if the `conditionJPath` condition is met | No**** | `Active`, `Inactive` |

\* `jPath` is required except for some collector structural mappings and some derived fields where only `conditionJPath` is used.  
\** `parentXPath` is required for all mappings except root-level elements (those directly under the conceptual "Root").  
\*** `sourceFeedJPaths` is required for collector mappings (when `isCollector` is `Yes`).  
\**** `defaultValue` is required for derived fields (when `conditionJPath` is specified and `jPath` is empty).

## Mapping Types

### 1. Basic Element Mapping
Maps a JSON field directly to an XML element.

```csv
jPath,xPath,isList,jsonType,xmlType,expression,namespace,parentXPath,order,isCollector,sourceFeedJPaths,conditionJPath,defaultValue
$.company.name,Name,No,string,element,,,CompanyInfo,10,No,,,
```

### 2. Collector Mapping
Collects data from multiple JSON paths into a single XML structure.

```csv
jPath,xPath,isList,jsonType,xmlType,expression,namespace,parentXPath,order,isCollector,sourceFeedJPaths,conditionJPath,defaultValue
,Departments,No,,element,,,Organization,20,Yes,"$.company.mainDepartments[*];$.company.satelliteOffices.departments[*]",,
.,Department,No,object,element,,,Departments,10,No,,,
```

### 3. Derived Field Mapping
Creates XML elements based on conditions in the JSON data. Multiple rules for the same `xPath` under the same parent can exist if their conditions or `jPath` differ, or if `order` differentiates them.

```csv
// Example 1: Status based on 'active' field in Department context
jPath,xPath,isList,jsonType,xmlType,expression,namespace,parentXPath,order,isCollector,sourceFeedJPaths,conditionJPath,defaultValue
,Status,No,string,element,,,Department,40,No,,active,Active

// Example 2: Status and FullName for Employee based on 'otherfullname' field in a specific JSON context (e.g., company object)
// These rules are distinct from other "Status" or "FullName" rules due to their parentXPath, conditionJPath, and jPath.
,Status,No,string,string,,,Employee,40,No,,otherfullname,Inactive
otherfullname,FullName,No,string,string,,,Employee,50,No,,,
```

## Key Rules and Requirements

### Path Syntax

1.  **JSON Paths (`jPath`, `conditionJPath`, `sourceFeedJPaths`)**:
    *   Root-level paths typically start with `$` (e.g., `$.company.name`).
    *   Relative paths for nested elements (e.g., `name`, `address.city`) or direct field names (e.g., `active`, `otherfullname`) are resolved against the current JSON context node.
    *   Use `[*]` for arrays/lists (e.g., `$.people[*].name`).
    *   Special character `.` in `jPath` indicates the current context node itself.

2.  **XML Paths (`xPath` and `parentXPath`)**:
    *   `xPath`: Use local element names (e.g., `Name`, `Status`). This value does *not* need to be globally unique. Its uniqueness is determined by its `parentXPath` and its role (differentiated by `order` or conditions).
    *   `parentXPath`: References the `xPath` of a *structural parent mapping rule*. These parent `xPath` values (like "Organization", "CompanyInfo", "Employee") should be unique identifiers for those structural rules within the mapping configuration to ensure correct hierarchy.

### Hierarchical Structure

1.  **Parent-Child Relationships**:
    *   Every mapping rule (except root-level ones) must reference its parent in `parentXPath`.
    *   The `parentXPath` must correspond to the `xPath` of an existing structural mapping rule.

2.  **Element Order and Uniqueness**:
    *   The `order` field determines the sequence of sibling elements in the XML output.
    *   Multiple mapping rules can have the same `xPath` (local name) under the same `parentXPath` if they are differentiated by their `jPath`, `conditionJPath`, or `order`. This allows for conditional logic (if-else like behavior) or mapping different source fields to elements with the same name based on context.

## Programmatic CSV Generation Guidelines

When generating mapping CSV files programmatically, follow these guidelines to ensure compatibility and correctness:

### Validation Rules

1.  **Required Fields**:
    *   Ensure all required fields are filled based on the mapping type (see column definitions).
    *   Validate that `isList` and `isCollector` are only "Yes" or "No".

2.  **Path Correctness**:
    *   Validate JSON paths follow correct syntax. Field names used as `jPath` or `conditionJPath` for direct access should exist in the expected JSON contexts.
    *   Ensure XML element names in `xPath` are valid XML names.

3.  **Hierarchy and Parent Resolution**:
    *   Ensure that `parentXPath` values correctly reference the `xPath` of intended structural parent mapping rules. These parent `xPath`s should be unique identifiers for those structural rules.
    *   It's good practice to define parent structural rules before their children in the CSV, or ensure your generation logic can handle out-of-order definitions if your parsing builds a temporary structure first (like the iterative approach in `MappingGenerator`).

4.  **Uniqueness and Differentiation of Rules**:
    *   For structural elements that will serve as parents (i.e., their `xPath` will be used in other rules' `parentXPath`), their `xPath` values must be unique across the CSV.
    *   For leaf elements (elements that don't act as parents), their `xPath` (local name) does not need to be unique globally. Multiple leaf rules can share the same `xPath` if they:
        *   Belong to different parents (different `parentXPath`).
        *   Belong to the same parent but are differentiated by their `jPath` (mapping different source data).
        *   Belong to the same parent but are differentiated by their `conditionJPath` and `defaultValue` (conditional mapping).
        *   Belong to the same parent and map to the same conceptual field but need to appear multiple times (less common, but `order` would differentiate instance, though this usually means a list mapping is more appropriate).
    *   Use the `order` field consistently to define the sequence of sibling elements, especially when multiple rules might produce elements under the same parent.

5.  **Collector and Derived Field Logic**:
    *   For collector mappings, ensure `sourceFeedJPaths` are correct and the structural template child mapping (often with `jPath="."`) is defined.
    *   For derived fields, ensure `conditionJPath` and `defaultValue` are correctly specified. The `conditionJPath` will be evaluated in the context of the current JSON node being processed for that mapping rule.

### Code Example for Programmatic Generation

(The existing Java example for `MappingCsvGenerator` is a good starting point. The key is to ensure the `List<MappingDefinition>` passed to it contains rules constructed with the above principles in mind, especially regarding `parentXPath` resolution and how `xPath` is used.)

Consider adding a pre-validation step in your programmatic generator to check for:
*   Unresolvable `parentXPath` references.
*   Duplicate `xPath` values for rules intended to be unique structural parents.

This helps in creating large, complex mapping files more reliably.

## Testing Generated Mappings

After generating a mapping CSV file, verify it with these tests:

1. **Structure Validation**: Parse the CSV and validate hierarchical structure
2. **Transformation Test**: Test with sample JSON data to verify output
3. **Edge Cases**: Test with empty values, arrays, and special characters

## Best Practices

1. **Naming Conventions**: Use consistent naming for both JSON paths and XML elements
2. **Organization**: Group related mappings together in the CSV
3. **Documentation**: Add comments in adjacent documentation (cannot be directly included in CSV)
4. **Version Control**: Track changes to mapping files in source control
5. **Validation**: Validate CSV files before use in production

## Example Complete Mapping

```csv
jPath,xPath,isList,jsonType,xmlType,expression,namespace,parentXPath,order,isCollector,sourceFeedJPaths,conditionJPath,defaultValue
$,Organization,No,object,element,,,,0,No,,,
,CompanyInfo,No,object,element,,,Organization,10,No,,,
$.company.name,Name,No,string,element,,,CompanyInfo,10,No,,,
$.company.founded,YearFounded,No,number,element,,,CompanyInfo,20,No,,,
$.company.public,IsPublic,No,boolean,element,,,CompanyInfo,30,No,,,
,Departments,No,,element,,,Organization,20,Yes,"$.company.mainDepartments[*];$.company.satelliteOffices.departments[*]",,
.,Department,No,object,element,,,Departments,10,No,,,
id,DepartmentID,No,string,element,,,Department,10,No,,,
name,DepartmentName,No,string,element,,,Department,20,No,,,
headCount,HeadCount,No,number,element,,,Department,30,No,,,
,Status,No,string,element,,,Department,40,No,,active,Active
,Status,No,string,element,,,Department,41,No,,,Inactive
``` 