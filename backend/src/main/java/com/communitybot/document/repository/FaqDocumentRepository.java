package com.communitybot.document.repository;

import com.communitybot.document.domain.FaqDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FaqDocumentRepository extends JpaRepository<FaqDocument, UUID> {

    List<FaqDocument> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}
