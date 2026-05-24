package com.forgeStackk.EduResolve.dto.student.submission;

import java.util.List;

/** Safe teacher projection returned to students — never includes email, phone, or salary. */
public record TeacherPickerDto(
        Long         teacherUserId,
        String       name,
        List<String> subjects,
        List<String> classNames,
        boolean      teachesMyClass
) {}
