package com.communitybot.organization.controller;

import com.communitybot.organization.dto.CreateOrgRequest;
import com.communitybot.organization.dto.DeleteOrgRequest;
import com.communitybot.organization.dto.JoinOrgRequest;
import com.communitybot.organization.dto.OrgResponse;
import com.communitybot.organization.service.OrganizationService;
import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.security.CurrentUser;
import com.communitybot.workspace.service.WorkspaceService;
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
    private final WorkspaceService    workspaceService;

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

    /** Join an existing organisation by slug (dev/demo self-service). */
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<OrgResponse>> join(
            @Valid @RequestBody JoinOrgRequest request,
            @CurrentUser UUID userId
    ) {
        OrgResponse org = orgService.joinBySlug(request.slug(), userId);
        workspaceService.joinAllInOrg(org.id(), userId);
        return ResponseEntity.ok(ApiResponse.ok(org));
    }

    @GetMapping("/{orgId}")
    public ResponseEntity<ApiResponse<OrgResponse>> getOne(
            @PathVariable UUID orgId,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(orgService.getById(orgId, userId)));
    }

    /** Deletes an organisation permanently. Requires matching slug confirmation; owner only. */
    @PostMapping("/{orgId}/delete")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID orgId,
            @Valid @RequestBody DeleteOrgRequest request,
            @CurrentUser UUID userId
    ) {
        orgService.delete(orgId, userId, request.confirmSlug());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
