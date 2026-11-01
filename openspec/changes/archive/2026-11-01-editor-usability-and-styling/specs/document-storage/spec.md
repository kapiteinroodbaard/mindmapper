# Spec Delta

## ADDED Requirements

### Requirement: Rename the document
Confirming a new name in the editor's rename dialog SHALL rename the document's file to the new name
with the `.mindmap` extension, keep editing that file, and update the recent-files entry. For a
never-saved document the name SHALL be kept and suggested as the file name at the first save. When the
storage location does not allow renaming, the file SHALL stay unchanged and the app SHALL show a message
suggesting "Save as".

#### Scenario: Rename a saved map
- **WHEN** the user renames the saved map "Plan" to "Plan 2026"
- **THEN** the file is renamed to "Plan 2026.mindmap", the top bar shows "Plan 2026", further saves go to that file and the recent files show the new name

#### Scenario: Rename a never-saved map
- **WHEN** the user renames a never-saved map to "Ideas" and then saves it
- **THEN** the save picker suggests "Ideas.mindmap"

#### Scenario: Location does not allow renaming
- **WHEN** the user renames a map that was opened read-only from another app
- **THEN** the file keeps its name and a message says that renaming is not possible here and suggests "Save as"
