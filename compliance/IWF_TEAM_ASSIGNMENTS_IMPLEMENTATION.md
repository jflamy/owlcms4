# IWF Team Assignments Implementation Prompt

## Overview

Implement a timetable-based team assignment system for IWF-style competitions. This allows organizers to:
1. Assign technical officials to teams (1-4) and roles (REFEREE, JURY_MEMBER, MARSHAL, etc.)
2. Import/export a timetable that specifies which team works each session for each role category
3. Apply rotation logic to assign specific officials to specific session positions

---

## PART 1: Add Generic Roles to OfficialRole Enum

### File: `owlcms/src/main/java/app/owlcms/data/technicalofficial/OfficialRole.java`

**Task:** Add two new enum values at the beginning of the enum for generic team assignment roles.

**Current enum has values like:**
- JURY_PRESIDENT, JURY_A, JURY_B, JURY_C, JURY_D, JURY_RESERVE (specific jury positions)
- CENTER_REFEREE, LEFT_REFEREE, RIGHT_REFEREE, REFEREE_RESERVE (specific referee positions)
- MARSHAL1, MARSHAL2, TIMEKEEPER, ANNOUNCER, etc.

**Add these two new values at the TOP of the enum:**
```java
// Generic roles for team assignment (used with timetable)
REFEREE,        // Generic referee - rotates through CENTER, LEFT, RIGHT, RESERVE
JURY_MEMBER,    // Generic jury member - rotates through PRESIDENT, A, B, C, D, RESERVE
```

**Add a method to check if a role is a generic team role:**
```java
public boolean isGenericTeamRole() {
    return this == REFEREE || this == JURY_MEMBER;
}
```

**Add a method to get specific positions for a generic role:**
```java
public List<OfficialRole> getSpecificPositions() {
    switch (this) {
        case REFEREE:
            return List.of(CENTER_REFEREE, LEFT_REFEREE, RIGHT_REFEREE, REFEREE_RESERVE);
        case JURY_MEMBER:
            return List.of(JURY_PRESIDENT, JURY_A, JURY_B, JURY_C, JURY_D, JURY_RESERVE);
        default:
            return List.of(this); // Non-rotating roles return themselves
    }
}
```

---

## PART 2: Add officialRole Field to TechnicalOfficial Entity

### File: `owlcms/src/main/java/app/owlcms/data/technicalofficial/TechnicalOfficial.java`

**Task:** Add a new field `officialRole` to store the generic role for team assignment.

**Add field after existing fields (near `technicalOfficialTeam`):**
```java
@Enumerated(EnumType.STRING)
@Column(columnDefinition = "varchar(255)")
private OfficialRole officialRole;
```

**Add getter and setter:**
```java
public OfficialRole getOfficialRole() {
    return officialRole;
}

public void setOfficialRole(OfficialRole officialRole) {
    this.officialRole = officialRole;
}
```

---

## PART 3: Update TechnicalOfficialReader for Import

### File: `owlcms/src/main/java/app/owlcms/data/technicalofficial/TechnicalOfficialReader.java`

**Task:** Add support for importing the new `OfficialRole` column.

**Add constant at top with other column names:**
```java
private static final String OFFICIAL_ROLE = "OfficialRole";
```

**In `findColumnIndices` method, add index for OfficialRole column (follow existing pattern).**

**In `readRow` method, add parsing for OfficialRole:**
```java
String officialRoleStr = getStringValue(row, colIndices[OFFICIAL_ROLE_INDEX]);
if (officialRoleStr != null && !officialRoleStr.isEmpty()) {
    try {
        official.setOfficialRole(OfficialRole.valueOf(officialRoleStr.toUpperCase()));
    } catch (IllegalArgumentException e) {
        // Log warning, continue with null
    }
}
```

---

## PART 4: Create TechnicalOfficialsTimetable Entity

### File: `owlcms/src/main/java/app/owlcms/data/technicalofficial/TechnicalOfficialsTimetable.java` (NEW FILE)

**Task:** Create a new entity to store the timetable mapping sessions to teams by role.

```java
package app.owlcms.data.technicalofficial;

import java.io.Serializable;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import app.owlcms.data.group.Group;
import app.owlcms.utils.IdUtils;

/**
 * Represents a timetable entry mapping a session (Group) and role category to a team number.
 * 
 * For example: Session 1, REFEREE role → Team 1
 *              Session 1, JURY_MEMBER role → Team 2
 */
@SuppressWarnings("serial")
@Entity
@Cacheable
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = TechnicalOfficialsTimetable.class)
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
public class TechnicalOfficialsTimetable implements Serializable {

    @Id
    private Long id;

    @ManyToOne
    private Group group;

    @Enumerated(EnumType.STRING)
    private OfficialRole roleCategory;  // REFEREE, JURY_MEMBER, MARSHAL1, TIMEKEEPER, etc.

    private Integer teamNumber;  // 1-4

    public TechnicalOfficialsTimetable() {
        this.id = IdUtils.getTimeBasedId();
    }

    public TechnicalOfficialsTimetable(Group group, OfficialRole roleCategory, Integer teamNumber) {
        this();
        this.group = group;
        this.roleCategory = roleCategory;
        this.teamNumber = teamNumber;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }

    public OfficialRole getRoleCategory() { return roleCategory; }
    public void setRoleCategory(OfficialRole roleCategory) { this.roleCategory = roleCategory; }

    public Integer getTeamNumber() { return teamNumber; }
    public void setTeamNumber(Integer teamNumber) { this.teamNumber = teamNumber; }
}
```

---

## PART 5: Create TechnicalOfficialsTimetableRepository

### File: `owlcms/src/main/java/app/owlcms/data/technicalofficial/TechnicalOfficialsTimetableRepository.java` (NEW FILE)

**Task:** Create repository for CRUD operations on timetable entries.

```java
package app.owlcms.data.technicalofficial;

import java.util.List;

import javax.persistence.EntityManager;

import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;

public class TechnicalOfficialsTimetableRepository {

    public static List<TechnicalOfficialsTimetable> findAll() {
        return JPAService.runInTransaction(em -> {
            return em.createQuery("SELECT t FROM TechnicalOfficialsTimetable t", TechnicalOfficialsTimetable.class)
                    .getResultList();
        });
    }

    public static List<TechnicalOfficialsTimetable> findByGroup(Group group) {
        return JPAService.runInTransaction(em -> {
            return em.createQuery(
                    "SELECT t FROM TechnicalOfficialsTimetable t WHERE t.group = :group",
                    TechnicalOfficialsTimetable.class)
                    .setParameter("group", group)
                    .getResultList();
        });
    }

    public static TechnicalOfficialsTimetable findByGroupAndRole(Group group, OfficialRole roleCategory) {
        return JPAService.runInTransaction(em -> {
            List<TechnicalOfficialsTimetable> results = em.createQuery(
                    "SELECT t FROM TechnicalOfficialsTimetable t WHERE t.group = :group AND t.roleCategory = :role",
                    TechnicalOfficialsTimetable.class)
                    .setParameter("group", group)
                    .setParameter("role", roleCategory)
                    .getResultList();
            return results.isEmpty() ? null : results.get(0);
        });
    }

    public static TechnicalOfficialsTimetable save(TechnicalOfficialsTimetable entry) {
        return JPAService.runInTransaction(em -> em.merge(entry));
    }

    public static void delete(TechnicalOfficialsTimetable entry) {
        JPAService.runInTransaction(em -> {
            TechnicalOfficialsTimetable attached = em.find(TechnicalOfficialsTimetable.class, entry.getId());
            if (attached != null) {
                em.remove(attached);
            }
            return null;
        });
    }

    public static void deleteAll() {
        JPAService.runInTransaction(em -> {
            em.createQuery("DELETE FROM TechnicalOfficialsTimetable").executeUpdate();
            return null;
        });
    }

    public static void deleteAll(EntityManager em) {
        em.createQuery("DELETE FROM TechnicalOfficialsTimetable").executeUpdate();
    }
}
```

---

## PART 6: Register Entity in JPAService

### File: `owlcms/src/main/java/app/owlcms/data/jpa/JPAService.java`

**Task:** Add the new entity to the `entityClassNames()` method.

**Find the method `entityClassNames()` (around line 294) and add:**
```java
.add(TechnicalOfficialsTimetable.class.getName())
```

**Add import at top:**
```java
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetable;
```

---

## PART 7: Add to CompetitionData for V1 Export/Import

### File: `owlcms/src/main/java/app/owlcms/data/export/CompetitionData.java`

**Task:** Add timetable support to v1 database export/import.

**Add field:**
```java
private List<TechnicalOfficialsTimetable> timetable;
```

**Add getter with @JsonProperty:**
```java
@JsonProperty(index = 75)
public List<TechnicalOfficialsTimetable> getTimetable() {
    return timetable;
}
```

**Add setter:**
```java
public void setTimetable(List<TechnicalOfficialsTimetable> timetable) {
    this.timetable = timetable;
}
```

**In `fromDatabase()` method, add:**
```java
setTimetable(TechnicalOfficialsTimetableRepository.findAll());
```

**In `restore()` method, add (after technicalOfficials section):**
```java
if (updated.getTimetable() != null) {
    for (TechnicalOfficialsTimetable t : updated.getTimetable()) {
        em.merge(t);
    }
}
```

**In `removeAll()` method, add:**
```java
TechnicalOfficialsTimetableRepository.deleteAll(em);
```

**Add import:**
```java
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetable;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetableRepository;
```

---

## PART 8: Create Timetable CSV Import/Export Functions

### File: `owlcms/src/main/java/app/owlcms/data/technicalofficial/TimetableIO.java` (NEW FILE)

**Task:** Create utility class for importing/exporting timetable as CSV.

```java
package app.owlcms.data.technicalofficial;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;

/**
 * Import/Export timetable as CSV.
 * 
 * CSV Format:
 * Session,REFEREE,JURY_MEMBER,MARSHAL1,MARSHAL2,TIMEKEEPER,TECHNICAL_CONTROLLER1,TECHNICAL_CONTROLLER2,ANNOUNCER
 * Session 1,1,1,1,1,1,1,1,1
 * Session 2,2,1,1,1,1,1,1,1
 * ...
 */
public class TimetableIO {

    // Role categories that appear in timetable (order matters for CSV columns)
    public static final List<OfficialRole> TIMETABLE_ROLES = List.of(
        OfficialRole.REFEREE,
        OfficialRole.JURY_MEMBER,
        OfficialRole.MARSHAL1,
        OfficialRole.MARSHAL2,
        OfficialRole.TIMEKEEPER,
        OfficialRole.TECHNICAL_CONTROLLER1,
        OfficialRole.TECHNICAL_CONTROLLER2,
        OfficialRole.ANNOUNCER
    );

    /**
     * Export current timetable to CSV.
     * If no timetable exists, creates an empty template with all sessions and Team 1 defaults.
     */
    public static InputStream exportTimetable() {
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append("Session");
        for (OfficialRole role : TIMETABLE_ROLES) {
            sb.append(",").append(role.name());
        }
        sb.append("\n");

        // Get sessions sorted by weigh-in time
        List<Group> sessions = GroupRepository.findAll().stream()
            .sorted((a, b) -> {
                int cmp = ObjectUtils.compare(a.getWeighInTime(), b.getWeighInTime(), true);
                if (cmp != 0) return cmp;
                return ObjectUtils.compare(a.getPlatform(), b.getPlatform(), true);
            })
            .collect(Collectors.toList());

        // Build lookup map from existing timetable
        Map<String, Integer> lookup = new HashMap<>();  // "groupId-role" -> teamNumber
        for (TechnicalOfficialsTimetable entry : TechnicalOfficialsTimetableRepository.findAll()) {
            if (entry.getGroup() != null && entry.getRoleCategory() != null) {
                String key = entry.getGroup().getId() + "-" + entry.getRoleCategory().name();
                lookup.put(key, entry.getTeamNumber());
            }
        }

        // Output rows
        for (Group session : sessions) {
            sb.append(session.getName());
            for (OfficialRole role : TIMETABLE_ROLES) {
                String key = session.getId() + "-" + role.name();
                Integer team = lookup.getOrDefault(key, 1);  // Default to Team 1
                sb.append(",").append(team);
            }
            sb.append("\n");
        }

        return new ByteArrayInputStream(sb.toString().getBytes());
    }

    /**
     * Import timetable from CSV, replacing existing entries.
     */
    public static void importTimetable(InputStream is, StringBuilder errors) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        
        // Read header
        String headerLine = reader.readLine();
        if (headerLine == null) {
            errors.append("Empty file\n");
            return;
        }
        
        String[] headers = headerLine.split(",");
        if (headers.length < 2 || !headers[0].equalsIgnoreCase("Session")) {
            errors.append("Invalid header. Expected: Session,REFEREE,JURY_MEMBER,...\n");
            return;
        }

        // Map header columns to roles
        Map<Integer, OfficialRole> columnToRole = new HashMap<>();
        for (int i = 1; i < headers.length; i++) {
            try {
                OfficialRole role = OfficialRole.valueOf(headers[i].trim().toUpperCase());
                columnToRole.put(i, role);
            } catch (IllegalArgumentException e) {
                errors.append("Unknown role in column " + i + ": " + headers[i] + "\n");
            }
        }

        // Build session lookup by name
        Map<String, Group> sessionByName = GroupRepository.findAll().stream()
            .collect(Collectors.toMap(Group::getName, g -> g, (a, b) -> a));

        // Clear existing timetable
        TechnicalOfficialsTimetableRepository.deleteAll();

        // Read data rows
        String line;
        int lineNum = 1;
        while ((line = reader.readLine()) != null) {
            lineNum++;
            if (line.trim().isEmpty()) continue;
            
            String[] parts = line.split(",");
            if (parts.length < 2) continue;

            String sessionName = parts[0].trim();
            Group session = sessionByName.get(sessionName);
            if (session == null) {
                errors.append("Line " + lineNum + ": Unknown session '" + sessionName + "'\n");
                continue;
            }

            for (int i = 1; i < parts.length && i < headers.length; i++) {
                OfficialRole role = columnToRole.get(i);
                if (role == null) continue;

                try {
                    int teamNumber = Integer.parseInt(parts[i].trim());
                    if (teamNumber < 1 || teamNumber > 4) {
                        errors.append("Line " + lineNum + ": Team number must be 1-4, got " + teamNumber + "\n");
                        continue;
                    }
                    TechnicalOfficialsTimetable entry = new TechnicalOfficialsTimetable(session, role, teamNumber);
                    TechnicalOfficialsTimetableRepository.save(entry);
                } catch (NumberFormatException e) {
                    errors.append("Line " + lineNum + ": Invalid team number '" + parts[i] + "'\n");
                }
            }
        }
    }
}
```

---

## PART 9: Add UI Buttons to TechnicalOfficialContent Header

### File: `owlcms/src/main/java/app/owlcms/nui/preparation/TechnicalOfficialContent.java`

**Task:** Add "Export Timetable" and "Import Timetable" buttons to the top bar.

**Look at the existing `createTopBar()` method pattern. Add two new buttons:**

1. **Export Timetable Button** - Downloads CSV file
2. **Import Timetable Button** - Opens upload dialog

**Add to imports:**
```java
import app.owlcms.data.technicalofficial.TimetableIO;
```

**In `createTopBar()` method (or wherever the button bar is created), add:**

```java
// Export Timetable button
Button exportTimetableButton = new Button(Translator.translate("TechnicalOfficials.ExportTimetable"),
    new Icon(VaadinIcon.DOWNLOAD));
exportTimetableButton.addClickListener(e -> {
    try {
        InputStream is = TimetableIO.exportTimetable();
        // Use existing download pattern from the codebase
        // StreamResource with "timetable.csv" filename
    } catch (Exception ex) {
        // Show error notification
    }
});

// Import Timetable button  
Button importTimetableButton = new Button(Translator.translate("TechnicalOfficials.ImportTimetable"),
    new Icon(VaadinIcon.UPLOAD));
importTimetableButton.addClickListener(e -> {
    // Open TimetableUploadDialog (create similar to TechnicalOfficialsUploadDialog)
    new TimetableUploadDialog(getUI().orElse(null)).open();
});
```

**Add buttons to the button layout (follow existing pattern in the file).**

---

## PART 10: Create TimetableUploadDialog

### File: `owlcms/src/main/java/app/owlcms/nui/preparation/TimetableUploadDialog.java` (NEW FILE)

**Task:** Create upload dialog for timetable CSV import.

**Follow the pattern from `TechnicalOfficialsUploadDialog.java`:**

```java
package app.owlcms.nui.preparation;

import java.io.ByteArrayInputStream;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;

import app.owlcms.data.technicalofficial.TimetableIO;
import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")
public class TimetableUploadDialog extends Dialog {

    public TimetableUploadDialog(UI ui) {
        H3 title = new H3(Translator.translate("TechnicalOfficials.ImportTimetable"));

        TextArea errors = new TextArea(Translator.translate("Errors"));
        errors.setWidth("50em");
        errors.setHeight("10em");
        errors.setVisible(false);

        UploadHandler uploadHandler = UploadHandler.inMemory((metadata, data) -> {
            try (ByteArrayInputStream is = new ByteArrayInputStream(data)) {
                StringBuilder errorBuilder = new StringBuilder();
                TimetableIO.importTimetable(is, errorBuilder);
                
                if (errorBuilder.length() > 0) {
                    errors.setValue(errorBuilder.toString());
                    errors.setVisible(true);
                } else {
                    this.close();
                    // Show success notification
                }
                if (ui != null) ui.push();
            } catch (Exception e) {
                errors.setValue(e.getMessage());
                errors.setVisible(true);
                if (ui != null) ui.push();
            }
        });

        Upload upload = new Upload(uploadHandler);
        upload.setAcceptedFileTypes("text/csv", ".csv");
        upload.setWidth("40em");

        add(new VerticalLayout(title, upload, errors));
    }
}
```

---

## PART 11: Update TechnicalOfficialEditingFormFactory

### File: `owlcms/src/main/java/app/owlcms/nui/preparation/TechnicalOfficialEditingFormFactory.java`

**Task:** Add a new section at the bottom titled "IWF Team Assignments" with:
1. Team selection (ComboBox 1-4) - already exists as `technicalOfficialTeam`
2. OfficialRole selection (ComboBox with REFEREE, JURY_MEMBER, MARSHAL1, etc.)

**Add ComboBox for OfficialRole (after the existing teamComboBox):**

```java
// Official Role for team assignment
ComboBox<OfficialRole> officialRoleComboBox = new ComboBox<>(Translator.translate("TechnicalOfficials.OfficialRole"));
officialRoleComboBox.setItems(
    OfficialRole.REFEREE,
    OfficialRole.JURY_MEMBER,
    OfficialRole.MARSHAL1,
    OfficialRole.MARSHAL2,
    OfficialRole.TIMEKEEPER,
    OfficialRole.TECHNICAL_CONTROLLER1,
    OfficialRole.TECHNICAL_CONTROLLER2,
    OfficialRole.ANNOUNCER,
    OfficialRole.WEIGHIN1,
    OfficialRole.WEIGHIN2
);
officialRoleComboBox.setItemLabelGenerator(role -> Translator.translate("OfficialRole." + role.name()));
binder.forField(officialRoleComboBox).bind(TechnicalOfficial::getOfficialRole, TechnicalOfficial::setOfficialRole);
```

**Create a section layout (using the existing pattern in the file for sections):**

```java
// IWF Team Assignments section
H4 teamHeader = new H4(Translator.translate("TechnicalOfficials.IWFTeamAssignments"));
FormLayout teamLayout = new FormLayout();
teamLayout.add(teamComboBox, officialRoleComboBox);
teamLayout.setResponsiveSteps(new ResponsiveStep("0", 2));
```

**Add the section to the main form layout (at the bottom).**

---

## PART 12: Add Translation Keys

### File: `shared/src/main/resources/i18n/translation4.csv`

**Task:** Add new translation keys for the UI elements.

Add these rows (follow the CSV format with all language columns):

```
TechnicalOfficials.ExportTimetable,Export Timetable,...
TechnicalOfficials.ImportTimetable,Import Timetable,...
TechnicalOfficials.OfficialRole,Official Role,...
TechnicalOfficials.IWFTeamAssignments,IWF Team Assignments,...
OfficialRole.REFEREE,Referee,...
OfficialRole.JURY_MEMBER,Jury Member,...
OfficialRole.MARSHAL1,Marshal 1,...
OfficialRole.MARSHAL2,Marshal 2,...
OfficialRole.TIMEKEEPER,Timekeeper,...
OfficialRole.TECHNICAL_CONTROLLER1,Technical Controller 1,...
OfficialRole.TECHNICAL_CONTROLLER2,Technical Controller 2,...
OfficialRole.ANNOUNCER,Announcer,...
OfficialRole.WEIGHIN1,Weigh-in 1,...
OfficialRole.WEIGHIN2,Weigh-in 2,...
```

---

## PART 13: Add to CompetitionDataV2 for V2 Export/Import

### File: `owlcms/src/main/java/app/owlcms/data/export/v2/CompetitionDataV2.java`

**Task:** Add timetable support to v2 database export/import.

**Update `@JsonPropertyOrder` annotation to include timetable:**
```java
@JsonPropertyOrder({
	"formatVersion",
	"exportDate",
	"competition",
	"config",
	"ageGroups",
	"teams",
	"sessions",
	"athletes",
	"platforms",
	"records",
	"recordConfig",
	"technicalOfficials",
	"timetable"  // ADD THIS
})
```

**Add field (after technicalOfficials):**
```java
private List<TimetableEntryDTO> timetable;
```

**Add getter:**
```java
public List<TimetableEntryDTO> getTimetable() {
    return timetable;
}
```

**Add setter:**
```java
public void setTimetable(List<TimetableEntryDTO> timetable) {
    this.timetable = timetable;
}
```

**In `fromDatabase()` method, add (after setTechnicalOfficials):**
```java
// Convert timetable entries to DTOs (use session name instead of Group reference)
setTimetable(TechnicalOfficialsTimetableRepository.findAll().stream()
    .map(TimetableEntryDTO::fromEntity)
    .collect(Collectors.toList()));
```

**In `restore()` method, add (after technicalOfficials section):**
```java
if (updated.getTimetable() != null) {
    TechnicalOfficialsTimetableRepository.deleteAll(em);
    for (TimetableEntryDTO dto : updated.getTimetable()) {
        TechnicalOfficialsTimetable entry = dto.toEntity(em);
        if (entry != null) {
            em.merge(entry);
        }
    }
}
```

**Add import:**
```java
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetable;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetableRepository;
```

---

## PART 14: Create TimetableEntryDTO for V2 Format

### File: `owlcms/src/main/java/app/owlcms/data/export/v2/TimetableEntryDTO.java` (NEW FILE)

**Task:** Create DTO for timetable entries in v2 format (uses session name instead of Group entity reference).

```java
package app.owlcms.data.export.v2;

import javax.persistence.EntityManager;

import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.technicalofficial.OfficialRole;
import app.owlcms.data.technicalofficial.TechnicalOfficialsTimetable;

/**
 * DTO for timetable entries in V2 export format.
 * Uses session name string instead of Group entity reference.
 */
public class TimetableEntryDTO {
    
    private String sessionName;      // Group name (e.g., "Session 1", "M1")
    private String roleCategory;     // OfficialRole enum name (e.g., "REFEREE", "JURY_MEMBER")
    private Integer teamNumber;      // 1-4
    
    public TimetableEntryDTO() {}
    
    public TimetableEntryDTO(String sessionName, String roleCategory, Integer teamNumber) {
        this.sessionName = sessionName;
        this.roleCategory = roleCategory;
        this.teamNumber = teamNumber;
    }
    
    /**
     * Convert entity to DTO for export
     */
    public static TimetableEntryDTO fromEntity(TechnicalOfficialsTimetable entity) {
        if (entity == null) return null;
        return new TimetableEntryDTO(
            entity.getGroup() != null ? entity.getGroup().getName() : null,
            entity.getRoleCategory() != null ? entity.getRoleCategory().name() : null,
            entity.getTeamNumber()
        );
    }
    
    /**
     * Convert DTO to entity for import
     */
    public TechnicalOfficialsTimetable toEntity(EntityManager em) {
        if (sessionName == null || roleCategory == null) return null;
        
        // Look up group by name
        Group group = GroupRepository.findByName(sessionName);
        if (group == null) return null;
        
        // Parse role
        OfficialRole role;
        try {
            role = OfficialRole.valueOf(roleCategory);
        } catch (IllegalArgumentException e) {
            return null;
        }
        
        return new TechnicalOfficialsTimetable(group, role, teamNumber);
    }
    
    // Getters and setters
    public String getSessionName() { return sessionName; }
    public void setSessionName(String sessionName) { this.sessionName = sessionName; }
    
    public String getRoleCategory() { return roleCategory; }
    public void setRoleCategory(String roleCategory) { this.roleCategory = roleCategory; }
    
    public Integer getTeamNumber() { return teamNumber; }
    public void setTeamNumber(Integer teamNumber) { this.teamNumber = teamNumber; }
}
```

---

## PART 15: Update tracker-core to Read Timetable

### File: `tracker-core/src/protocol/parser-v2.js`

**Task:** Add parsing of timetable from v2 format database.

**In the `parseV2Database` function, add after technicalOfficials parsing (around line 63):**
```javascript
// Parse timetable (new - session/role to team mapping)
const timetable = db.timetable || [];
```

**Add timetable to the result object (around line 73):**
```javascript
const result = {
  formatVersion: '2.0',
  exportDate: db.exportDate || null,
  athletes,
  ageGroups: db.ageGroups || [],
  categories,
  fops,
  sessions,
  records,
  technicalOfficials,
  timetable,  // ADD THIS LINE
  platforms: db.platforms || [],
  teams: db.teams || [],
  // ... rest of result
};
```

---

### File: `tracker-core/src/competition-hub.js`

**Task:** Store and expose timetable data via hub methods.

**Add method to get timetable:**
```javascript
/**
 * Get the timetable mapping sessions and roles to teams
 * @returns {Array} Array of timetable entries: { sessionName, roleCategory, teamNumber }
 */
getTimetable() {
  return this.databaseState?.timetable || [];
}

/**
 * Get timetable entries for a specific session
 * @param {object} params
 * @param {string} params.sessionName - The session/group name
 * @returns {Array} Array of { roleCategory, teamNumber } for the session
 */
getTimetableForSession({ sessionName }) {
  const timetable = this.getTimetable();
  return timetable.filter(entry => entry.sessionName === sessionName);
}

/**
 * Get team number assigned to a session and role
 * @param {object} params
 * @param {string} params.sessionName - The session/group name
 * @param {string} params.roleCategory - The role category (REFEREE, JURY_MEMBER, etc.)
 * @returns {number|null} Team number (1-4) or null if not found
 */
getTeamForSessionRole({ sessionName, roleCategory }) {
  const timetable = this.getTimetable();
  const entry = timetable.find(e => 
    e.sessionName === sessionName && e.roleCategory === roleCategory
  );
  return entry?.teamNumber || null;
}

/**
 * Get technical officials assigned to a specific team and role
 * @param {object} params
 * @param {number} params.teamNumber - Team number (1-4)
 * @param {string} params.roleCategory - Role category (REFEREE, JURY_MEMBER, etc.)
 * @returns {Array} Array of technical officials on that team with that role
 */
getTeamOfficials({ teamNumber, roleCategory }) {
  const officials = this.databaseState?.technicalOfficials || [];
  return officials.filter(to => 
    to.technicalOfficialTeam === teamNumber && 
    to.officialRole === roleCategory
  );
}
```

---

### File: `tracker-core/src/index.js`

**Task:** Export the new timetable methods (if not already auto-exported via hub).

The hub singleton already exposes all methods. No changes needed if `competitionHub` is exported.

**Verify these exports exist:**
```javascript
export { competitionHub } from './competition-hub.js';
```

---

### File: `tracker-core/docs/API_REFERENCE.md`

**Task:** Document the new timetable methods.

**Add to "Hub Data Access Methods" section:**

```markdown
#### `getTimetable()`

Returns the full timetable mapping sessions and roles to teams.

**Returns:** `Array<Object>`

```javascript
const timetable = competitionHub.getTimetable();
// [
//   { sessionName: "Session 1", roleCategory: "REFEREE", teamNumber: 1 },
//   { sessionName: "Session 1", roleCategory: "JURY_MEMBER", teamNumber: 1 },
//   { sessionName: "Session 2", roleCategory: "REFEREE", teamNumber: 2 },
//   ...
// ]
```

---

#### `getTimetableForSession({ sessionName })`

Returns timetable entries for a specific session.

**Parameters:**
- `sessionName` (string) - Session/group name

**Returns:** `Array<Object>`

```javascript
const sessionRoles = competitionHub.getTimetableForSession({ sessionName: 'Session 1' });
// [
//   { roleCategory: "REFEREE", teamNumber: 1 },
//   { roleCategory: "JURY_MEMBER", teamNumber: 1 },
//   { roleCategory: "MARSHAL1", teamNumber: 1 },
//   ...
// ]
```

---

#### `getTeamForSessionRole({ sessionName, roleCategory })`

Returns the team number assigned to a session and role.

**Parameters:**
- `sessionName` (string) - Session/group name
- `roleCategory` (string) - Role category (REFEREE, JURY_MEMBER, MARSHAL1, etc.)

**Returns:** `number | null`

```javascript
const team = competitionHub.getTeamForSessionRole({
  sessionName: 'Session 1',
  roleCategory: 'REFEREE'
});
// 1
```

---

#### `getTeamOfficials({ teamNumber, roleCategory })`

Returns technical officials assigned to a specific team and role.

**Parameters:**
- `teamNumber` (number) - Team number (1-4)
- `roleCategory` (string) - Role category

**Returns:** `Array<Object>`

```javascript
const referees = competitionHub.getTeamOfficials({
  teamNumber: 1,
  roleCategory: 'REFEREE'
});
// [
//   { firstName: "John", lastName: "DOE", ... },
//   { firstName: "Jane", lastName: "SMITH", ... },
//   ...
// ]
```
```

---

## Validation Checklist

After implementing all parts, verify:

1. [ ] `OfficialRole.java` has REFEREE and JURY_MEMBER enum values
2. [ ] `TechnicalOfficial.java` has `officialRole` field with getter/setter
3. [ ] `TechnicalOfficialsTimetable.java` entity exists and compiles
4. [ ] `TechnicalOfficialsTimetableRepository.java` has all CRUD methods
5. [ ] `JPAService.entityClassNames()` includes `TechnicalOfficialsTimetable`
6. [ ] `CompetitionData.java` (v1) exports/imports timetable
7. [ ] `CompetitionDataV2.java` exports/imports timetable with `TimetableEntryDTO`
8. [ ] `TimetableEntryDTO.java` exists with fromEntity/toEntity methods
9. [ ] `TimetableIO.java` exports/imports CSV
10. [ ] `TechnicalOfficialContent.java` has Export/Import buttons
11. [ ] `TimetableUploadDialog.java` handles CSV upload
12. [ ] `TechnicalOfficialEditingFormFactory.java` has IWF Team Assignments section
13. [ ] Translation keys exist for all new UI strings
14. [ ] **tracker-core:** `parser-v2.js` parses `timetable` from database
15. [ ] **tracker-core:** `competition-hub.js` has `getTimetable()`, `getTimetableForSession()`, `getTeamForSessionRole()`, `getTeamOfficials()` methods
16. [ ] **tracker-core:** `API_REFERENCE.md` documents new timetable methods

**Note:** Hibernate handles schema migration automatically. No Flyway migration needed.

---

## Testing

### OWLCMS Testing

1. Start owlcms and navigate to Technical Officials page
2. Verify Export Timetable button downloads a CSV with all sessions
3. Modify the CSV (change team numbers)
4. Verify Import Timetable button uploads and stores the changes
5. Edit a technical official and verify Team and OfficialRole fields appear
6. Export the full database (v1) and verify timetable is included in JSON
7. Export the full database (v2) and verify timetable is included with session names
8. Import the database on a fresh instance and verify timetable is restored

### tracker-core Testing

1. Start owlcms-tracker connected to owlcms
2. Verify database contains `timetable` array:
   ```javascript
   const timetable = competitionHub.getTimetable();
   console.log('Timetable entries:', timetable.length);
   ```
3. Test session lookup:
   ```javascript
   const s1 = competitionHub.getTimetableForSession({ sessionName: 'Session 1' });
   console.log('Session 1 assignments:', s1);
   ```
4. Test team/role lookup:
   ```javascript
   const team = competitionHub.getTeamForSessionRole({
     sessionName: 'Session 1',
     roleCategory: 'REFEREE'
   });
   console.log('Referee team for Session 1:', team);
   ```
5. Test getting officials by team:
   ```javascript
   const refs = competitionHub.getTeamOfficials({
     teamNumber: 1,
     roleCategory: 'REFEREE'
   });
   console.log('Team 1 referees:', refs.map(r => r.lastName));
   ```
