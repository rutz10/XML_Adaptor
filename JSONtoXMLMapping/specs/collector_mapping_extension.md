*   **`jPath`**:
    *   **For a Collector Mapping Row**: Typically empty. If used, it would apply to the collector element itself (e.g., for conditional creation of the entire collector block, an advanced scenario not commonly used).
    *   **For a Direct Child of a Collector (Structural Template Row)**: This mapping defines the repeating XML element (e.g., `<Branch>`). Its `jPath` is usually `.` (dot), signifying that this structure corresponds to one item from a source feed. The XML element defined by this template row will be created for each item in the `sourceFeedJPaths`.
    *   **For Descendants within the Repeating Structure (e.g., `<BranchID>` under `<Branch>`)**:
        *   `jPath="fieldName"`: Resolved against the current feed item (if the feed item is an object). E.g., if a feed item is `{"branchNameEU": "Data"}`, a child mapping with `jPath="branchNameEU"` under the structural template will get the value "Data". This resolution is direct field access.
        *   `jPath="path.to.field"`: Resolved against the current feed item. E.g. if a feed item is `{"details": {"code": "XYZ"}}`, a child mapping with `jPath="details.code"` will get "XYZ".
        *   `jPath="$.field.from.root"`: If the `jPath` starts with `$.`, it is resolved from the absolute root of the original input JSON, *not* relative to the current feed item. This allows pulling in global data if needed, though less common for fields within a repeating structure that usually sources from the feed item itself.
        *   `jPath="."`: Resolved as the feed item itself. Useful if the feed item is a primitive value that should directly populate this tag.
        *   If `jPath` is empty, the field is a candidate for derivation using `conditionJPath` and `defaultValue`.
*   **`parentXPath`**: Used as before to define the hierarchy. Children of a collector will have the collector's `xPath` as their `parentXPath`. The structural template will have the collector's `xPath` as its parent. Fields within the template will have the template's `xPath` as their parent.
*   **`conditionJPath` / `defaultValue`**: For derived fields within the repeating structure, `conditionJPath` is resolved relative to the current feed item (e.g., `conditionJPath="isActive"` or `conditionJPath="otherfullname"`). If the field specified in `conditionJPath` exists in the current feed item, the condition is met. The `defaultValue` is then used.

## 4. CSV Structure and Examples

Let's consider creating a `<Company>/<Branches>` structure, where each `<Branch>` can be sourced from `$.company.branchList[*]`, `$.company.mainBranch`, or a legacy `$.alternateBranchInfo`.

**`mappings.csv` Example:**

| jPath                 | xPath                             | isList | jsonType | xmlType | expression | namespace | parentXPath        | order | isCollector | sourceFeedJPaths                                      | conditionJPath | defaultValue   |
|-----------------------|-----------------------------------|--------|----------|---------|------------|-----------|--------------------|-------|-------------|-------------------------------------------------------|----------------|----------------|
|                       | Company                           | No     | object   | element |            |           |                    | 0     | No          |                                                       |                |                |
| $.company.name        | CompanyName                       | No     | string   | element |            |           | Company            | 10    | No          |                                                       |                |                |
|                       | Branches                          | No     |          | element |            |           | Company            | 20    | Yes         | `$.company.branchList[*];$.company.mainBranch;$.alternateBranchInfo` |                |                |
| .                     | Branch                            | No     | object   | element |            |           | Branches           | 10    | No          |                                                       |                |                |
| id                    | BranchID                          | No     | string   | element |            |           | Branch             | 10    | No          |                                                       |                |                |
| name                  | BranchName                        | No     | string   | element |            |           | Branch             | 20    | No          |                                                       |                |                |
| location.city         | City                              | No     | string   | element |            |           | Branch             | 30    | No          |                                                       |                |                |
| .                     | LegacyCode                        | No     | string   | element |            |           | Branch             | 35    | No          |                                                       |                |                | <!-- If feed item is primitive -->
|                       | Status                            | No     | string   | element |            |           | Branch             | 40    | No          |                                                       | isActive       | Active         |
|                       | Status                            | No     | string   | element |            |           | Branch             | 41    | No          |                                                       |                | Inactive       | <!-- Default if isActive not met -->
| type                  | BranchType                        | No     | string   | element |            |           | Branch             | 50    | No          |                                                       |                |                |
|                       | Region                            | No     | string   | element |            |           | Branch             | 60    | No          |                                                       | name           | DefaultRegion  | <!-- Example: derive if 'name' exists -->

**Explanation of the example with `otherfullname` scenario:**

Consider the `issue_tracker/issue.txt` case where an `Employee` element needs to be populated from the `$.company` object itself, using the `otherfullname` field:

JSON Snippet (from `$.company` as a feed item):
```json
{
  "gname": "TechCorp Global",
  "founded": 1998,
  "otherfullname": "Gamer",
  // ... other company fields ...
}
```

Relevant Mappings for `Employee` structure when `$.company` is the feed:

| jPath         | xPath    | ... | parentXPath | order | conditionJPath | defaultValue |
|---------------|----------|-----|-------------|-------|----------------|--------------|
| ` `             | Employees| ... | Organization| 30    |                |              | <!-- Collector Definition -->
| `.`           | Employee | ... | Employees   | 10    |                |              | <!-- Structural Template for Employee -->
| `id`          | EmployeeID | ... | Employee    | 10    |                |              | <!-- This would be MISSING for $.company feed -->
| `$.name`      | FullName | ... | Employee    | 20    |                |              | <!-- This would be MISSING for $.company feed -->
| ` `             | Status   | ... | Employee    | 40    | `otherfullname`| `Inactive`   |
| `otherfullname` | FullName | ... | Employee    | 50    |                |              |

*   When `$.company` is a feed item for the `Employees` collector, and `processCollectorItem` processes it using the `Employee` structural template:
    *   The `Status` mapping rule has `conditionJPath="otherfullname"`. Since the `$.company` feed item *has* an `otherfullname` field, this condition is met, and `<Status>Inactive</Status>` is generated.
    *   The `FullName` mapping rule has `jPath="otherfullname"`. This directly accesses the `otherfullname` field from the `$.company` feed item, generating `<FullName>Gamer</FullName>`.
    *   Mappings like `jPath="id"` (for `EmployeeID`) or `jPath="$.name"` (for a standard `FullName`) would correctly find no data if the `$.company` object doesn't have fields named `id` or `name` at its top level, and thus these tags would not be generated for *this specific feed item*.

This demonstrates how `jPath` and `conditionJPath` being resolved against the current feed item enable flexible data extraction and conditional logic within collector structures. 