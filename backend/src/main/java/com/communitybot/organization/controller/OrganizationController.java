package com.communitybot.organization.controller;

import com.communitybot.organization.dto.CreateOrgRequest;
import com.communitybot.organization.dto.OrgResponse;
import com.communitybot.organization.service.OrganizationService;
import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService orgService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrgResponse>> create(
            @Valid @RequestBody CreateOrgRequest request,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(orgService.create(request, userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrgResponse>>> listMine(@CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(orgService.listForUser(userId)));
    }

    @GetMapping("/{orgId}")
    public ResponseEntity<ApiResponse<OrgResponse>> getOne(
            @PathVariable UUID orgId,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(orgService.getById(orgId, userId)));
    }
}
