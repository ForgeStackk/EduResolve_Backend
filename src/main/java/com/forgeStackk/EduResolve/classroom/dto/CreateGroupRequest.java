package com.forgeStackk.EduResolve.classroom.dto;

import java.util.List;

public record CreateGroupRequest(String name, String description, List<Long> memberUserIds) {}
