package com.forgeStackk.EduResolve.controller;

import com.forgeStackk.EduResolve.entity.teacher.ClassRoom;
import com.forgeStackk.EduResolve.repository.teacher.ClassRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/classes")
@CrossOrigin(origins = "*")
public class AdminClassController {

    @Autowired private ClassRoomRepository classRepo;

    @GetMapping
    public List<Map<String, Object>> list() {
        return classRepo.findAll().stream().map(c -> Map.<String, Object>of(
                "id",        c.getSeqId(),
                "classId",   c.getClassId().toString(),
                "className", c.getClassName(),
                "section",   c.getSection(),
                "schoolName", c.getSchoolName() != null ? c.getSchoolName() : ""
        )).toList();
    }
}
