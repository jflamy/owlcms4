<!-- markdownlint-disable -->
# V2 Update Message Format - Implementation Complete

## Status: ✅ COMPLETE

Implementation of V2 update message format for v2Export feature flag.

## Changes Made

### File: WebSocketEventForwarder.java

**Location:** `c:\Users\lamyj\git\owlcms4\owlcms\src\main\java\app\owlcms\monitors\WebSocketEventForwarder.java`

#### 1. Imports Added (Lines 66-67)
```java
import app.owlcms.data.export.AthleteDTO;
import app.owlcms.data.export.TeamDTO;
```

#### 2. Modified createUpdate() Method (Lines 1264-1330)

Added v2Export feature flag check:
```java
boolean useV2Format = Config.getCurrent().featureSwitch("v2Export");
```

Each FOP gets its own trio of session structures (`sessionAthletes`, `startOrderKeys`, `liftingOrderKeys`). If three platforms are running, the update stream carries three independent sets keyed by their `fopName`s. The tracker still maintains a single shared database cache underneath, and every session map simply layers its live state on top of that common athlete store.

**V2 Format Branch** (when v2Export enabled):
- `startOrderKeys`: List<Integer> - Athlete keys in start order (display order)
- `liftingOrderKeys`: List<Integer> - Athlete keys in lifting order
- `sessionAthletes`: List<AthleteDTO> - Full athlete data for all athletes in current session
- `currentAthleteKey`: Integer - Key of athlete currently lifting
- `nextAthleteKey`: Integer - Key of next athlete
- `previousAthleteKey`: Integer - Key of previous athlete who lifted (if exists)

**V1 Format Branch** (backward compatible):
- `groupAthletes`: Full athlete objects as JSON (existing behavior)
- `liftingOrderAthletes`: Full athlete objects as JSON (existing behavior)

#### 3. New Helper Method: getAthleteKeys() (Line 1356)
```java
private List<Integer> getAthleteKeys(List<Athlete> athletes)
```
Extracts athlete keys from a list of athletes for V2 format.

#### 4. New Helper Method: exportSessionAthletes() (Line 1370)
```java
private List<AthleteDTO> exportSessionAthletes(List<Athlete> athletes)
```
**Purpose:** Converts session athletes to V2 DTO format with team mapping

**Process:**
1. Builds TeamDTO map from unique team names in athlete list
2. Converts each athlete using AthleteDTO.fromAthlete(athlete, teamMap)
3. Returns List<AthleteDTO> in V2 export format

**Team ID Generation:** Uses `teamName.hashCode()` for consistent team IDs

## V2 Update Message Structure

### JSON Example (Partial)
```json
{
  "type": "update",
  "payload": {
    "uiEvent": "LiftingOrderUpdated",
    "fopName": "Platform A",
    
    // V2 Format Fields (when v2Export enabled)
    "startOrderKeys": [12345, 67890, 11223],
    "liftingOrderKeys": [67890, 12345, 11223],
    "sessionAthletes": [
      {
        "key": 12345,
        "fullName": "GARCIA, Steven",
        "team": "Barbell Club",
        "categoryName": "M 79",
        "snatch1Declaration": 71,
        "snatch1Change1": null,
        "snatch1ActualLift": null,
        // ... all other AthleteDTO fields
      },
      // ... more athletes
    ],
    "currentAthleteKey": 67890,
    "nextAthleteKey": 12345,
    "previousAthleteKey": null
  }
}
```

### Key Benefits

1. **Efficient References**: Orders use keys instead of full objects (smaller payload)
2. **Self-Contained**: sessionAthletes provides all data needed for display
3. **No Database Dependency**: Tracker doesn't need full database for every update
4. **Consistent Format**: AthleteDTO matches database export format
5. **Backward Compatible**: V1 format preserved when v2Export disabled

## Data Flow

```
OWLCMS Lifting Order Update
         ↓
WebSocketEventForwarder.createUpdate(UIEvent)
         ↓
Check Config.featureSwitch("v2Export")
         ↓
    [V2 Enabled]
         ↓
exportSessionAthletes(displayOrder)
    • Build TeamDTO map (team name → TeamDTO with ID)
    • Convert each Athlete → AthleteDTO
    • Return List<AthleteDTO>
         ↓
getAthleteKeys(displayOrder) → startOrderKeys
getAthleteKeys(liftingOrder) → liftingOrderKeys
         ↓
Extract current/next/previous athlete keys
         ↓
Build update payload with:
    • startOrderKeys (order of all session athletes)
    • liftingOrderKeys (current lifting sequence)
    • sessionAthletes (full AthleteDTO data)
    • currentAthleteKey, nextAthleteKey, previousAthleteKey
         ↓
Send WebSocket message type="update"
```

## Tracker Integration Plan

### 1. Parse V2 Update Message
```javascript
if (payload.sessionAthletes) {
  // V2 format detected
  const athleteMap = new Map();
  payload.sessionAthletes.forEach(athlete => {
    athleteMap.set(athlete.key, athlete);
  });
  
  // Build display using startOrderKeys
  const displayOrder = payload.startOrderKeys.map(key => athleteMap.get(key));
  
  // Build lifting order using liftingOrderKeys
  const liftingOrder = payload.liftingOrderKeys.map(key => athleteMap.get(key));
  
  // Highlight current/next athletes
  const current = athleteMap.get(payload.currentAthleteKey);
  const next = athleteMap.get(payload.nextAthleteKey);
}
```

### 2. Benefits for Tracker
- **No Full Database**: Update messages are self-contained
- **Efficient Parsing**: Build Map<key, athlete> once, lookup by key
- **Consistent Format**: Same AthleteDTO structure as database export
- **Easy Highlighting**: Current/next/previous keys identify special athletes

## Testing Checklist

- [ ] Enable v2Export feature switch in OWLCMS config
- [ ] Start OWLCMS with tracker connected
- [ ] Trigger lifting order update (athlete lifts, weight change)
- [ ] Verify update message contains:
  - [ ] startOrderKeys array
  - [ ] liftingOrderKeys array
  - [ ] sessionAthletes array with full AthleteDTO objects
  - [ ] currentAthleteKey
  - [ ] nextAthleteKey
  - [ ] previousAthleteKey (if previous athlete exists)
- [ ] Verify athlete keys in *OrderKeys match keys in sessionAthletes
- [ ] Verify AthleteDTO format matches database export
- [ ] Verify V1 format still works when v2Export disabled

## Related Files

- `WebSocketEventForwarder.java` - Update message creation
- `AthleteDTO.java` - V2 athlete export format
- `TeamDTO.java` - Team reference objects
- `CompetitionDataV2.java` - Database export using same DTOs
- `Config.java` - Feature switch configuration

## Implementation Date

2025-01-14

## Compilation Status

✅ No errors - Implementation complete and ready for testing
