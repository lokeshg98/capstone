The goal is to build a custom platform in the style of slack/discord, and set up an AI moderator that automates community engagement, content moderation, and information dissemination on a messaging platform

The Core Features (Minimum Required) are as follows:

Community Platform
    Build a simplified chat interface mimicking Slack/Discord (channels, threads, user profiles)
    Implement real-time messaging with message history
    Create basic user authentication and role system (admin, moderator, member)

Bot Capabilities
    Answer common questions using RAG over FAQ documents and community guidelines
    Detect inappropriate content (profanity, spam, harassment) and flag/hide messages
    Summarize long threads (10+ messages) into digestible overviews
    Welcome new members with personalized onboarding messages based on their profile/interests
    Schedule automated posts (e.g., "Weekly Digest every Friday 5pm")

Intelligence Layer
    Build FAQ knowledge base (minimum 20 Q&A pairs covering community rules, technical help, resources)
    Implement content classification for moderation
    Generate context-aware responses (acknowledge previous messages in thread)
