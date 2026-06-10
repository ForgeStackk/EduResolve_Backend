package com.forgeStackk.EduResolve.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

/**
 * channels accepts a plain string ("whatsapp") or a JSON array (["in-app","whatsapp"]).
 * The controller joins the list to a comma-separated string before persisting.
 */
public record BroadcastRequest(
        List<String> channels,
        UUID classId,
        Boolean targetStudents,
        Boolean targetParents,
        @NotBlank(message = "message is required")
        String message,
        Boolean isEmergency,
        String sentByName
) {
    public boolean targetStudentsOrFalse() { return targetStudents != null && targetStudents; }
    public boolean targetParentsOrFalse()  { return targetParents  != null && targetParents;  }
    public boolean isEmergencyOrFalse()    { return isEmergency    != null && isEmergency;    }
}
