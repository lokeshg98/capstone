package com.communitybot.publicsite.repository;

import com.communitybot.publicsite.domain.NewsletterSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, UUID> {
    boolean existsByEmailIgnoreCase(String email);
}
