package com.joprelys.backend.lead.service;

import com.joprelys.backend.lead.dto.DemoRequestDto;
import java.util.UUID;

public record DemoRequestRegistered(UUID id, DemoRequestDto details) {}
