# Database ZIP Double-Encoding Bug - Fixed

## Problem Summary

When OWLCMS sent database updates after plate configuration changes or session switches, the second database ZIP contained **double-encoded JSON** that failed to parse in the tracker.

**Symptoms:**
- First database ZIP (at startup): ✅ Valid JSON, loads 39 athletes successfully
- Second database ZIP (after session switch): ❌ Double-encoded JSON string, 0 athletes parsed
- Team scoreboard continued working because it used cached data from first successful load
- Tracker logs showed "Processing database with 0 athletes" for subsequent database ZIPs

**Sample Evidence:**
```bash
# First database ZIP (16:10:08) - CORRECT:
$ head -c 200 samples/2025-12-20T16-10-08-359-DATABASE_ZIP.json
{
  "formatVersion" : "2.0",
  "competition" : {
    "competitionName" : "Régionaux 2024-2025",
...

# Second database ZIP (16:12:00) - DOUBLE-ENCODED:
$ head -c 200 samples/2025-12-20T16-12-00-401-DATABASE_ZIP.json | cat -A
"{\r\n  \"formatVersion\" : \"2.0\",\r\n  \"competition\" : {\r\n    \"competitionName\" : \"R\u00E9gionaux 2024-2025\",\r\n ...
```

Note the leading `"` and escaped `\r\n` in the second sample - this is a JSON-encoded string containing JSON.

## Root Cause

**File:** `WebSocketEventForwarder.java`  
**Method:** `sendDatabaseZip()` (line 2250)

```java
// ❌ BUG: Double-encoding
String jsonDatabase = export.json();               // Returns JSON string
byte[] databaseZipBytes = DatabaseZipHelper.createDatabaseZipBytes(
    JSON_MAPPER.valueToTree(jsonDatabase)          // Treats string as plain value
);
```

**What happened:**
1. `export.json()` returns a **JSON string** (already serialized)
2. `JSON_MAPPER.valueToTree(jsonDatabase)` treats that JSON string as a **plain string value**
3. Creates a JSON tree where the root node is a **string literal** containing JSON
4. `DatabaseZipHelper.createDatabaseZipBytes()` serializes this tree
5. **Result:** JSON-encoded string `"{\r\n  \"formatVersion\" ..."}` instead of object `{ "formatVersion": ... }`

## Solution

Changed `sendDatabaseZip()` to use `export.structure()` (parsed object) instead of `export.json()` (JSON string):

```java
// ✅ FIXED: Use parsed object
byte[] databaseZipBytes = DatabaseZipHelper.createDatabaseZipBytes(
    export.structure()                             // Parsed Map<String, Object>
);
```

**Why this works:**
- `export.structure()` returns `Map<String, Object>` (parsed Java object)
- `DatabaseZipHelper.createDatabaseZipBytes(Object)` serializes the object **once**
- **Result:** Valid JSON `{ "formatVersion": "2.0", ... }` as expected

## Verification

**Comparison with working code:**

`sendFullCompetitionData()` (line 2146) - which worked correctly for initial connection:
```java
Map<String, Object> payload = new LinkedHashMap<>();
payload.put("databaseChecksum", export.checksum());
payload.put("database", export.structure());       // ✅ Correct: uses parsed object
```

## Testing Plan

1. **Start OWLCMS** with tracker URL configured: `ws://localhost:8096/ws`
2. **Load first session** - verify tracker receives valid database (39 athletes)
3. **Switch to different session** - verify tracker receives valid database ZIP (not double-encoded)
4. **Check tracker logs:**
   - Should show "Processing database with 39 athletes" (or actual count)
   - Should NOT show "Processing database with 0 athletes"
5. **Verify team scoreboard** shows athletes from new session correctly

## Files Changed

- `owlcms/src/main/java/app/owlcms/monitors/WebSocketEventForwarder.java`
  - Line 2272: Changed `JSON_MAPPER.valueToTree(jsonDatabase)` → `export.structure()`
  - Added comment explaining why parsed object is used instead of JSON string
  - Added local variable in logging block to calculate compression ratio

## Impact

- **Fixes:** Session switch database parsing failure
- **Fixes:** Plate configuration change database updates
- **Maintains:** Initial connection database send (already working)
- **Maintains:** Compression efficiency (70-80% reduction via ZIP)

## Related Issues

- Initial tracker implementation worked with first database send because it used `sendFullCompetitionData()` which correctly used `export.structure()`
- Second database sends used `sendDatabaseZip()` (for efficiency) which had the double-encoding bug
- Team scoreboard continued working because it cached data from first successful load

---

**Status:** Fixed  
**Date:** 2025-12-20  
**Commit Message:** Fix double-encoding bug in sendDatabaseZip() - use export.structure() instead of JSON string
