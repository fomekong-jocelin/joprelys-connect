package com.joprelys.backend.lead.service;

import com.joprelys.backend.lead.dto.DemoRequestDto;
import com.joprelys.backend.lead.dto.DemoRequestResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface RegisterDemoRequest {
    DemoRequestResponse registerDemoRequest(DemoRequestDto request, HttpServletRequest context);
}
