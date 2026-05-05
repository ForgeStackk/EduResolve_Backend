package com.forgeStackk.EduResolve.dto;

import com.forgeStackk.EduResolve.entity.Complaint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintStatusUpdate {
    private Complaint.Status status;
}
