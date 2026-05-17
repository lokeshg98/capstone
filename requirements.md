I need to build the following project. these are the high level guidelines. please guide about slack functionality, scalability missing in following and must be considered. also draw the target architecture diagrams. the component layers and abstractions. technology for each component and pros and cons. i'll use java as backend programming language. suggest the database, Frontend. i would prefer open source. deployment options so that small number of users say 20 spread globally can access it.roject: Slack/Discord Community Manager Bot

Goal Build an AI moderator that automates community engagement, content moderation, and information dissemination on a messaging platform.

Core Features (Minimum Required)

Community Platform



* Build a simplified chat interface mimicking Slack/Discord (channels, threads, user profiles)

* Implement real-time messaging with message history

* Create basic user authentication and role system (admin, moderator, member)

Bot Capabilities



* Answer common questions using RAG over FAQ documents and community guidelines

* Detect inappropriate content (profanity, spam, harassment) and flag/hide messages

* Summarize long threads (10+ messages) into digestible overviews

* Welcome new members with personalized onboarding messages based on their profile/interests

* Schedule automated posts (e.g., "Weekly Digest every Friday 5pm")

Intelligence Layer



* Build FAQ knowledge base (minimum 20 Q&A pairs covering community rules, technical help, resources)

* Implement content classification for moderation

* Generate context-aware responses (acknowledge previous messages in thread)

Presentation Guidelines (20 minutes) Explain your choices: Why your platform stack (React+Node, Django+WebSockets, etc.)? How does your moderation system balance false positives? Why specific summarization approach? Discuss challenges like maintaining conversation context across threads, avoiding over-moderation, or handling edge cases in content detection. Demo the bot responding to questions, moderating content, and summarizing an active discussion. please ask questions if unsure