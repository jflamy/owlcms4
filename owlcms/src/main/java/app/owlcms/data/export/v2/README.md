# Database Export Format V2

## Overview

This package provides an alternative JSON export format (V2) for competition data. The V2 format differs from the original format in several key ways:

### Key Differences

1. **Format Version Identifier**: V2 exports include a `formatVersion` field set to "2.0" to allow automatic detection during import.

2. **Group renamed to Session**: The `Group` entity is exported as `sessions` instead of `groups` in the JSON structure.

3. **Code/Name References**: Instead of using database IDs for relationships, V2 uses human-readable codes and names:
   - Athletes reference sessions by **session name** (`sessionName`) instead of group ID
   - Athletes and their participations reference categories by **category code** (`categoryCode`) instead of category ID
   - Sessions reference platforms by **platform name** (`platformName`) instead of platform ID
   - Timetable entries reference sessions by **session name** (`sessionName`)
   - Athletes reference teams by **team ID** (hashcode of team name) with a separate teams collection; the team name
     itself is also written as `teamName` (and as the deprecated `club`, still read by version 68)

4. **Numeric Lifts**: In the Athlete data, lift values are stored as **Integer** instead of String:
   - `null` represents an empty/not-yet-declared lift (instead of empty string)
   - Negative values represent failed lifts
   - This makes the data easier to process programmatically

### Null Value Handling

**On Export** (Domain → V2 JSON):

- Empty string lift values (`""`) in the database are converted to `null` in the JSON
- `V2Json.parseWeight()` returns `null` for empty or null strings
- In JSON, `null` lifts are written explicitly as `null`

**On Import** (V2 JSON → Domain):

- `null` values in JSON are converted back to empty strings (`""`) for storage in the database
- Missing fields are treated as `null` and converted to empty strings
- This preserves the existing database schema where lifts are stored as strings

**Example**:

```text
Database (V1):     snatch1Declaration = ""
Export (V2 JSON):  "snatch1Declaration": null  (or omitted)
Import (Database): snatch1Declaration = ""
```

## Usage

### Exporting Data

To export competition data in V2 format:

```java
CompetitionDataV2 exportData = new CompetitionDataV2();
InputStream jsonStream = exportData.exportData();
```

With UI notification:

```java
CompetitionDataV2 exportData = new CompetitionDataV2();
InputStream jsonStream = exportData.exportData(ui, notification);
```

### Importing Data

The system automatically detects the format version during import:

```java
FormatDetector.importData(inputStream);
```

This will:

1. Detect whether the file is V1 or V2 format
2. Route to the appropriate importer
3. Convert the data back to domain objects

### Manual Format Detection

You can also manually detect the format version:

```java
String version = FormatDetector.detectVersion(inputStream);
// Returns "1.0" or "2.0"
```

## Implementation Details

### Classes

- **CompetitionDataV2**: the export file; restores it in one transaction.
- **V2Json**: converts entities to and from their V2 JSON. Jackson writes every property that has both a getter and
  a setter, as V1 does, so a property added to an entity is exported and restored without further code.
- **V2Mixins**: the only differences from V1, as Jackson mixins:
  - database ids are not written; references are written as names or codes
  - nested objects (categories in age groups, participations in athletes) do not refer back to their parent
  - attempt weights are numbers; the team is a numeric id into the `teams` table
  - computed values that external readers use are written explicitly (`key`, `bestSnatch`, `bestCleanJerk`,
    `presumedBodyWeight`, `forcedAsCurrent`, the participations' scores and ranks, the categories' `code` and
    `categoryName`)
- **TeamDTO**: an entry of the `teams` table. There is no team entity: an athlete's team is a name. External readers
  rely on the ids; `TeamDTO.idCollisions` reports team names that share one, which the export logs as an error.

References by name or code (session, platform, category) are resolved with the import's own entity manager: the
entities they refer to were written earlier in the same transaction and are not committed yet. An unknown reference
is logged as a warning.

### Format Rules

- V2 carries every property that V1 round-trips (getter and setter), with database ids replaced by names or codes.
- External readers rely on the format: existing fields keep their names and value types; fields may be added, and
  readers ignore those they don't know.
- `V2FormatCoverageTest` checks that every property V1 round-trips is in V2 (renames and dropped ids are listed
  there); `V2FixtureRoundTripTest` checks that a fresh export keeps every field of the fixture and that export,
  reload and export give the same file.

### Format Detection

The `FormatDetector` class uses the following logic:

1. First checks for explicit `formatVersion` field
2. If not found, checks for V2-specific field (`sessions`)
3. If not found, checks for V1-specific field (`groups`)
4. Defaults to V1 if no indicators found

## Example JSON Structure

### V2 Format

```json
{
  "formatVersion": "2.0",
  "teams": [
    {
      "id": 12345678,
      "name": "Barbell Club"
    }
  ],
  "sessions": [
    {
      "name": "A",
      "platformName": "Platform 1",
      "weighInTime": "2025-01-15T08:00:00"
    }
  ],
  "athletes": [
    {
      "lastName": "Smith",
      "firstName": "John",
      "sessionName": "A",
      "categoryCode": "M81",
      "team": 12345678,
      "teamName": "Barbell Club",
      "snatch1Declaration": 100,
      "snatch1ActualLift": -100,
      "snatch2Declaration": 105,
      "snatch2ActualLift": 105
    }
  ]
}
```

### V1 Format (Original)

```json
{
  "groups": [
    {
      "id": 123456,
      "name": "A",
      "platform": 789012
    }
  ],
  "athletes": [
    {
      "id": 345678,
      "lastName": "Smith",
      "group": 123456,
      "category": 901234,
      "snatch1Declaration": "100",
      "snatch1ActualLift": "-100",
      "snatch2Declaration": "105",
      "snatch2ActualLift": "105"
    }
  ]
}
```

## Backward Compatibility

The original V1 export/import mechanism remains the **default** and is fully functional. The V2 format is completely separate and does not affect existing exports or imports.

Both formats can coexist, and the system will automatically choose the correct importer based on the format version detected.

## Benefits of V2 Format

1. **Human Readable**: Uses names/codes instead of database IDs, making exports easier to read and understand
2. **Portable**: References by name/code are more stable across different database instances
3. **Type Safe**: Numeric lifts are proper numbers, not strings, reducing parsing errors
4. **Clear Intent**: `null` vs. numeric value is clearer than empty string vs. string with number
5. **Easier Processing**: External tools can process numeric lift data without string parsing
6. **Version Control Friendly**: More semantic diffs when changes are made
7. **Normalized Teams**: Teams are exported as separate entities with referential integrity, making team data easier to query and manipulate

## Migration Notes

If you want to convert existing V1 exports to V2 format:

1. Import the V1 file using the standard import
2. Export using the V2 exporter

The data will be preserved, and you'll get the benefits of the new format.
