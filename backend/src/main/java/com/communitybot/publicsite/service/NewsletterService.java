package com.communitybot.publicsite.service;

import com.communitybot.publicsite.domain.NewsletterSubscriber;
import com.communitybot.publicsite.repository.NewsletterSubscriberRepository;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NewsletterService {

    private final NewsletterSubscriberRepository repository;

    @Transactional
    public void subscribe(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
        if (email.isBlank() || !email.contains("@") || email.length() > 320) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Please enter a valid email address.");
        }
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new AppException(ErrorCode.NEWSLETTER_ALREADY_SUBSCRIBED);
        }
        repository.save(NewsletterSubscriber.builder()
                .email(email)
                .subscribedAt(Instant.now())
                .build());
    }
}
