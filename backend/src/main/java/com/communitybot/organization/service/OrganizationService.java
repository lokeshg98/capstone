package com.communitybot.organization.service;

import com.communitybot.auth.domain.User;
import com.communitybot.auth.service.UserService;
import com.communitybot.organization.domain.OrgMember;
import com.communitybot.organization.domain.OrgRole;
import com.communitybot.organization.domain.Organization;
import com.communitybot.organization.dto.CreateOrgRequest;
import com.communitybot.organization.dto.OrgResponse;
import com.communitybot.organization.repository.OrgMemberRepository;
import com.communitybot.organization.repository.OrganizationRepository;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import com.communitybot.shared.util.SlugGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository orgRepository;
    private final OrgMemberRepository    memberRepository;
    private final UserService            userService;

    /**
     * Creates a new organisation and immediately adds the creator as OWNER.
     * Slug is auto-generated from the name and guaranteed to be unique.
     */
    @Transactional
    public OrgResponse create(CreateOrgRequest request, UUID creatorId) {
        User owner = userService.getOrThrow(creatorId);
        String slug = uniqueSlug(request.name());

        Organization org = orgRepository.save(
                Organization.builder()
                        .name(request.name())
                        .slug(slug)
                        .owner(owner)
                        .build()
        );

        memberRepository.save(
                OrgMember.builder()
                        .organization(org)
                        .user(owner)
                        .role(OrgRole.OWNER)
                        .build()
        );

        return OrgResponse.from(org, OrgRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<OrgResponse> listForUser(UUID userId) {
        return orgRepository.findAllByMemberUserId(userId).stream()
                .map(org -> {
                    OrgRole role = memberRepository
                            .findByOrganizationIdAndUserId(org.getId(), userId)
                            .map(OrgMember::getRole)
                            .orElse(OrgRole.MEMBER);
                    return OrgResponse.from(org, role);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public OrgResponse getById(UUID orgId, UUID requesterId) {
        Organization org = getOrThrow(orgId);
        requireMembership(orgId, requesterId);
        OrgRole role = memberRepository
                .findByOrganizationIdAndUserId(orgId, requesterId)
                .map(OrgMember::getRole)
                .orElse(OrgRole.MEMBER);
        return OrgResponse.from(org, role);
    }

    // -------------------------------------------------------------------------
    // Helpers used by other services

    public Organization getOrThrow(UUID orgId) {
        return orgRepository.findById(orgId)
                .orElseThrow(() -> new AppException(ErrorCode.ORG_NOT_FOUND));
    }

    public void requireMembership(UUID orgId, UUID userId) {
        if (!memberRepository.existsByOrganizationIdAndUserId(orgId, userId)) {
            throw new AppException(ErrorCode.ORG_ACCESS_DENIED);
        }
    }

    // -------------------------------------------------------------------------

    private String uniqueSlug(String name) {
        String base = SlugGenerator.from(name);
        if (!orgRepository.existsBySlug(base)) return base;

        // Append a short numeric suffix until the slug is free
        for (int i = 2; i <= 99; i++) {
            String candidate = base + "-" + i;
            if (!orgRepository.existsBySlug(candidate)) return candidate;
        }
        throw new AppException(ErrorCode.ORG_SLUG_TAKEN);
    }
}
