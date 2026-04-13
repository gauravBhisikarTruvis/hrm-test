# HRM Tool - Drools Rule Enforcement

This tool is a Core Java application that fetches employee data from a local JSON mock and applies business rules using the Drools Rule Engine.

## Business Rule
- **No work force below the age of 18**: Identifies any employee who is under 18 years of age based on their `date_of_birth`.

## Project Structure
- `com.hrm.Main`: Entry point of the application.
- `com.hrm.rules.RuleEngine`: Dedicated module for managing Drools rules.
- `com.hrm.util.DataFetcher`: Utility for JSON parsing and age calculation logic.
- `src/main/resources/rules/age-check.drl`: The business rule defined in Drools language.

## Prerequisites
- Java 17 or higher
- Gradle (use the included wrapper)

## How to Run
To execute the tool and see the results on the console, run the following command from the `hrm-tool` directory:

```bash
./gradlew run
```

## Results
The tool will output:
1. Progress of data fetching and rule engine initialization.
2. Individual "Rule hit" messages for each violation.
3. A summary table showing all violating Employee IDs.
