# JSON to XML Mapping Utility

## Introduction

This document outlines the architectural changes and approach used to enhance the JSON to XML mapping utility. The primary goal is to eliminate hardcoded logic in the `JsonToXmlSteam` class and enable a more generic solution driven by CSV mapping rules.

## Problem Statement

The existing implementation of the `JsonToXmlSteam` class contained hardcoded logic for handling specific fields like `Status` and `FullName`. This approach was not scalable or maintainable for handling diverse JSON data structures.

## Proposed Solution

To address these issues, we introduced a `MappingRuleEvaluator` helper class. This class encapsulates the rule evaluation logic, making the `JsonToXmlSteam` class cleaner and more maintainable.

### Key Features of `MappingRuleEvaluator`

- **Rule Evaluation**: Evaluates conditions and determines XML element content based on CSV mapping rules.
- **JSON Context Management**: Manages JSON context for child mappings, ensuring correct data extraction.
- **Separation of Concerns**: Keeps the `JsonToXmlSteam` class focused on transformation logic, while `MappingRuleEvaluator` handles rule-specific logic.

## Integration with `JsonToXmlSteam`

The `MappingRuleEvaluator` integrates with the `JsonToXmlSteam` class by:

- **Processing CSV Mapping Rules**: Reads and processes mapping rules from a CSV file, determining how JSON fields map to XML elements.
- **Handling Conditional Logic**: Applies conditions specified in the CSV to determine when and how XML elements should be created.
- **Generating XML Output**: Constructs XML elements based on evaluated rules, ensuring accurate representation of JSON data.

## Benefits

- **Improved Readability**: The separation of rule evaluation logic enhances the readability of the `JsonToXmlSteam` class.
- **Enhanced Testability**: Isolating rule logic in `MappingRuleEvaluator` allows for targeted testing of rule evaluation.
- **Scalability**: The system can now handle diverse JSON structures and fields without additional hardcoded logic.

## Conclusion

The introduction of the `MappingRuleEvaluator` class represents a significant improvement in the architecture of the JSON to XML mapping utility. By leveraging CSV mapping rules, the utility is now more flexible, maintainable, and capable of handling a wide range of JSON data structures. 