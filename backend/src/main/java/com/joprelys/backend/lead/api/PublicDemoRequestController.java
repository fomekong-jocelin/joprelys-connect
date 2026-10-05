package com.joprelys.backend.lead.api;

import com.joprelys.backend.lead.dto.DemoRequestDto;
import com.joprelys.backend.lead.dto.DemoRequestResponse;
import com.joprelys.backend.lead.service.RegisterDemoRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/demo-requests")
public class PublicDemoRequestController {

    private final RegisterDemoRequest demoRequestService;

    public PublicDemoRequestController(RegisterDemoRequest demoRequestService) {
        this.demoRequestService = demoRequestService;
    }

    @PostMapping
    public ResponseEntity<DemoRequestResponse> createDemoRequest(
            @Valid @RequestBody DemoRequestDto dto,
            HttpServletRequest request
    ) {
        DemoRequestResponse response = demoRequestService.registerDemoRequest(dto, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
