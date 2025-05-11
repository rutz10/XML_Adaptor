# JSON to XML Utility Extension: Collector Mappings

## 1. Introduction

This document outlines the specification for an extension to the JSON to XML utility. The primary goal is to enable the creation of repeating XML structures where the data for each instance can be sourced from multiple, potentially heterogeneous, locations within the input JSON. This is achieved through a new concept called "Collector Mappings."

This extension will allow for more complex transformations, such as merging data from a JSON array and individual JSON fields into a common repeating XML element. It also integrates with the existing derived fields functionality (`conditionJPath`, `defaultValue`).

## 2. Core Problem

The existing utility maps a single JPath to a single XPath, or a JPath representing an array to a repeating XML element. However, it lacks a straightforward way to:

1.  Define a repeating XML structure (e.g., `<Branch>`) that can be instantiated multiple times.
2.  Source the data for these instances from different JPaths. For example, some `<Branch>` instances might come from a JSON array `$.branches[*]`, while another `<Branch>` instance might come from a single JSON field `$.otherJpathBranch`.
3.  Ensure that derived fields within this repeating structure (e.g., `<BranchDerivedEU>`) are correctly processed based on conditions relative to the data of the *current* instance.

## 3. Proposed Solution: Collector Mappings

We introduce "Collector Mappings" to address this. A collector mapping defines an XML element that acts as a container. Instead of having its own direct data source, it specifies multiple "source feeds" (JPaths) from the input JSON. For each item found through these feeds, it instantiates a defined child XML structure.

### 3.1. Architectural Principles

*   **Centralized Repeating Structure Definition**: The XML structure of the repeating element (e.g., `<Branch>` and all its children) is defined once in the `mappings.csv`.
*   **Multiple Data Feeds**: A parent "collector" mapping specifies multiple JPaths. Each JPath acts as a "feed" that can produce data for one or more instances of the repeating structure.
*   **Contextual Data Resolution**: JPaths within the repeating structure's definition are resolved relative to the specific JSON data item provided by the currently active "feed."
*   **Heterogeneous Feeds**: Feeds can be JSON arrays (each item producing one instance) or single JSON objects/values (producing one instance).
*   **Integrated Derived Fields**: The existing derived field logic (`conditionJPath`, `defaultValue`) applies within the context of each item from a feed.

### 3.2. Changes to `mappings.csv` Structure

Two new columns will be added to `mappings.csv`:

*   **`isCollector` (Boolean)**:
    *   Value: `Yes` or `No` (or empty, treated as `No`).
    *   If `Yes`, this mapping defines a collector element. Its child mappings in the hierarchy define the structure to be repeated for each item sourced from its feeds.
*   **`sourceFeedJPaths` (String)**:
    *   Used only if `isCollector` is `Yes`.
    *   A semicolon-separated list of JPaths (e.g., `$.branches[*];$.allOtherBranches.primaryBranch;$.legacyBranchData`).
    *   Each JPath in this list is a "source feed." The collector will iterate through these feeds in the order specified.

### 3.3. Behavior of Existing Columns with Collectors

*   **`jPath`**:
    *   **For a Collector Mapping Row**: Typically empty or can be used for conditional creation of the collector itself (advanced). For its primary role, the data comes from `sourceFeedJPaths`.
    *   **For a Direct Child of a Collector (Structural Template Row)**: This mapping defines the repeating XML element (e.g., `<Branch>`). Its `jPath` is usually `.` (dot), signifying that this structure corresponds to one item from a source feed.
    *   **For Descendants within the Repeating Structure (e.g., `<BranchEU>` under `<Branch>`)**:
        *   `jPath="fieldName"`: Resolved against the current feed item (if the feed item is an object). E.g., if feed item is `{"branchNameEU": "Data"}`, `jPath="branchNameEU"` gets "Data".
        *   `jPath="."`: Resolved as the feed item itself. Useful if the feed item is a primitive value that should directly populate this tag.
        *   If `jPath` is empty, the field is a candidate for derivation using `conditionJPath` and `defaultValue`.
*   **`parentXPath`**: Used as before to define the hierarchy. Children of a collector will have the collector's `xPath` as their `parentXPath`. The structural template will have the collector's `xPath` as its parent. Fields within the template will have the template's `xPath` as their parent.
*   **`conditionJPath` / `defaultValue`**: For derived fields within the repeating structure, `conditionJPath` is resolved relative to the current feed item.

## 4. CSV Structure and Examples

Let's consider creating a `<Company>/<Branches>` structure, where each `<Branch>` can be sourced from `$.company.branchList[*]`, `$.company.mainBranch`, or a legacy `$.alternateBranchInfo`.

**`mappings.csv` Example:**

| jPath                 | xPath                             | isList | jsonType | xmlType | expression | namespace | parentXPath        | order | isCollector | sourceFeedJPaths                                      | conditionJPath | defaultValue   |
| :-------------------- | :-------------------------------- | :----- | :------- | :------ | :--------- | :-------- | :----------------- | :---- | :---------- | :---------------------------------------------------- | :------------- | :------------- |
| `$.company.name`      | `Company/CompanyName`             | No     | string   | element |            |           | `Company`          | 10    | No          |                                                       |                |                |
|                       | `Company/Branches`                | No     |          | element |            |           | `Company`          | 20    | **Yes**     | `$.company.branchList[*];$.company.mainBranch;$.alternateBranchInfo` |                |                |
| `.`                   | `Company/Branches/Branch`         | No     | object   | element |            |           | `Company/Branches` | 10    | No          |                                                       |                |                |
| `id`                  | `Company/Branches/Branch/BranchID`| No     | string   | element |            |           | `.../Branch`       | 10    | No          |                                                       |                |                |
| `name`                | `Company/Branches/Branch/BranchName`| No     | string   | element |            |           | `.../Branch`       | 20    | No          |                                                       |                |                |
| `location.city`       | `Company/Branches/Branch/City`    | No     | string   | element |            |           | `.../Branch`       | 30    | No          |                                                       |                |                |
| `.`                   | `Company/Branches/Branch/LegacyCode`| No     | string   | element |            |           | `.../Branch`       | 35    | No          |                                                       |                |                |
|                       | `Company/Branches/Branch/Status`  | No     | string   | element |            |           | `.../Branch`       | 40    | No          |                                                       | `isActive`     | `Active`       |
|                       | `Company/Branches/Branch/Status`  | No     | string   | element |            |           | `.../Branch`       | 41    | No          |                                                       |                | `Inactive`     |
| `type`                | `Company/Branches/Branch/BranchType`| No     | string   | element |            |           | `.../Branch`       | 50    | No          |                                                       |                |                |
|                       | `Company/Branches/Branch/Region`  | No     | string   | element |            |           | `.../Branch`       | 60    | No          |                                                       | `name`         | `DefaultRegion`|

**Explanation:**

1.  **`Company/Branches` (Collector)**:
    *   `isCollector=Yes`.
    *   `sourceFeedJPaths` lists three sources:
        1.  `$.company.branchList[*]`: An array of branch objects.
        2.  `$.company.mainBranch`: A single branch object.
        3.  `$.alternateBranchInfo`: A single primitive value (e.g., a legacy code).
2.  **`Company/Branches/Branch` (Structural Template)**:
    *   `jPath="."`: Indicates this structure is instantiated for each item from the feeds.
    *   Its children (`BranchID`, `BranchName`, etc.) define the content of each `<Branch>` element.
3.  **Fields within `<Branch>`**:
    *   `BranchID` (`jPath="id"`): Takes `id` field from feed item if it's an object.
    *   `BranchName` (`jPath="name"`): Takes `name` field from feed item if it's an object.
    *   `City` (`jPath="location.city"`): Takes nested field.
    *   `LegacyCode` (`jPath="."`): If the feed item is a primitive (like from `$.alternateBranchInfo`), this tag will take that primitive value. This mapping needs to be ordered carefully or have conditions if `id`/`name` etc. might also exist. A common strategy is to have specific mappings for object fields and a general `jPath="."` for primitives, potentially with different target XPaths or careful ordering if the XPath is the same. For simplicity here, we assume it's for the primitive feed.
    *   `Status`: A derived field. If `isActive` field exists in the feed item, Status is "Active". Otherwise (if a second mapping for Status with no `conditionJPath` is provided and ordered after), it defaults to "Inactive".
    *   `Region`: Derived. If `name` field exists in the feed item, Region is "DefaultRegion".

## 5. Example Scenarios & Expected Output

**Input JSON 1:**
```json
{
  "company": {
    "name": "Global Corp",
    "branchList": [
      { "id": "B100", "name": "North Hub", "location": { "city": "New York" }, "isActive": true, "type": "Regional" },
      { "id": "B101", "name": "West Wing", "location": { "city": "London" }, "isActive": false, "type": "Local" }
    ],
    "mainBranch": { "id": "B200", "name": "HQ", "location": { "city": "Paris" }, "type": "Central" },
    "alternateBranchInfo": "LEGACY001"
  }
}
```

**Expected XML Output 1 (based on CSV above):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Company>
    <CompanyName>Global Corp</CompanyName>
    <Branches>
        <Branch>
            <BranchID>B100</BranchID>
            <BranchName>North Hub</BranchName>
            <City>New York</City>
            <!-- LegacyCode would not be populated as feed item is object -->
            <Status>Active</Status>
            <BranchType>Regional</BranchType>
            <Region>DefaultRegion</Region>
        </Branch>
        <Branch>
            <BranchID>B101</BranchID>
            <BranchName>West Wing</BranchName>
            <City>London</City>
            <!-- LegacyCode would not be populated as feed item is object -->
            <Status>Inactive</Status>
            <BranchType>Local</BranchType>
            <Region>DefaultRegion</Region>
        </Branch>
        <Branch>
            <BranchID>B200</BranchID>
            <BranchName>HQ</BranchName>
            <City>Paris</City>
            <!-- LegacyCode would not be populated as feed item is object -->
            <Status>Inactive</Status> <!-- Assuming mainBranch has no isActive, so default rule for Status applies -->
            <BranchType>Central</BranchType>
            <Region>DefaultRegion</Region>
        </Branch>
        <Branch>
            <!-- id, name, city would not be populated as feed item is primitive -->
            <LegacyCode>LEGACY001</LegacyCode>
            <Status>Inactive</Status> <!-- Assuming primitive feed item has no isActive -->
            <!-- BranchType would not be populated -->
            <!-- Region would not be populated as primitive feed item has no 'name' -->
        </Branch>
    </Branches>
</Company>
```

**Input JSON 2 (Only `alternateBranchInfo`):**
```json
{
  "company": {
    "name": "Solo Corp",
    "alternateBranchInfo": "LEGACY002"
  }
}
```

**Expected XML Output 2:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Company>
    <CompanyName>Solo Corp</CompanyName>
    <Branches>
        <Branch>
            <LegacyCode>LEGACY002</LegacyCode>
            <Status>Inactive</Status>
        </Branch>
    </Branches>
</Company>
```

**Handling Overlapping jPaths for same xPath (e.g. `BranchEU` from object vs. primitive):**

If the same XML tag (e.g. `Company/Branches/Branch/DataPoint`) needs to be populated from `feedItem.fieldA` if the feed item is an object, or from the `feedItem` itself if it's a primitive, two mapping rows would be needed for the *same* `xPath`:

| jPath    | xPath                                 | ... | parentXPath  | order | Notes                                      |
| :------- | :------------------------------------ | :-- | :----------- | :---- | :----------------------------------------- |
| `fieldA` | `Company/Branches/Branch/DataPoint`   | ... | `.../Branch` | 10    | For object feed items                      |
| `.`      | `Company/Branches/Branch/DataPoint`   | ... | `.../Branch` | 20    | For primitive feed items (processed if 1st fails) |

The processing logic would try the mapping with order 10. If `jPath="fieldA"` fails to resolve (e.g., because the feed item is a primitive, or `fieldA` doesn't exist on the object), it would then try the mapping with order 20 for the *same target XPath*.

## 6. Key Implementation Considerations (High-Level)

*   **`Mapping.java`**: Add `isCollector (boolean)` and `sourceFeedJPaths (List<String>)`.
*   **`MappingGenerator.java`**: Parse new CSV columns. Split `sourceFeedJPaths` string into a list.
*   **`JsonToXmlSteam.java` (Core Logic Changes)**:
    *   Modify the main transformation loop to identify collector mappings.
    *   When a collector is encountered:
        *   Iterate through its `sourceFeedJPaths`.
        *   For each JPath, retrieve the corresponding `JsonNode` (feed data).
        *   If feed data is an array, iterate through its items. Each item becomes a "current feed item."
        *   If feed data is a single node, it becomes the "current feed item."
        *   For each "current feed item":
            *   Process the *children* of the collector mapping (which define the repeating structure, e.g., the `<Branch>` mapping).
            *   The "current feed item" serves as the JSON context for resolving JPaths and `conditionJPath`s within this repeating structure.
    *   JPath resolution for fields within the repeating structure must be relative to the "current feed item."
    *   The `conditionJPath` evaluation cache should be scoped to the "current feed item" context for efficiency.
*   **Handling `jPath="."` vs. `jPath="fieldName"`**: The system needs to correctly interpret these against the "current feed item," especially distinguishing between a feed item that *is* a primitive and one that *has* fields.

## 7. Open Questions / Future Considerations

*   **Deeply Nested Collectors**: Are collectors allowed within the structural template of another collector? (Assume yes, standard recursion should handle).
*   **Error Handling**: Enhanced logging for feed resolution failures or unhandled feed item types.
*   **Performance**: For extremely numerous feeds or very large arrays from feeds, monitor performance.

This specification provides a foundational plan for the "Collector Mappings" extension. 