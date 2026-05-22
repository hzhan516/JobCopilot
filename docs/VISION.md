<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](VISION.md) | [简体中文](i18n/zh-Hans-CN/VISION.md) | [繁體中文](i18n/zh-Hant-TW/VISION.md)

# Vision: JobCopilot

## The Problem
Tailoring a resume for different job postings is a highly repetitive, error-prone, and time-consuming process. Existing job boards rely heavily on rigid keyword-matching (TF-IDF), causing qualified candidates to be filtered out due to synonym mismatches or rigid Applicant Tracking Systems (ATS). 

Conversely, using generic AI chatbots (like ChatGPT) to rewrite resumes lacks persistent state, cannot track application pipelines, and does not provide a structured way to manage document versions and historical iterations.

## Our Vision
**JobCopilot** is designed to fundamentally streamline the job-hunting process. We envision an open-source, AI-powered platform that acts as a personalized career co-pilot. 

It aims to automatically parse user-uploaded resumes, evaluate them against specific job descriptions using semantic vector matching, and provide an interactive AI assistant to iteratively optimize resume content. By combining secure document management, asynchronous AI processing, and personalized recommendations, the system saves users hours of manual tailoring while significantly increasing their interview chances.

## Core Capabilities
* **AI-Driven Resume Parsing & Structuring:** Converts messy PDFs/Word docs into strictly structured, ATS-friendly JSON data.
* **Semantic Job Matching:** Goes beyond keyword matching. It uses vector databases to understand the semantic capabilities of a candidate against market demands.
* **Iterative Optimization:** Provides an interactive AI chat interface to refine the resume, retaining the context of the user\'s historical experience.
* **Version Control:** Maintains distinct versions of a resume (Original, Converted, AI-Optimized) with full rollback capabilities.

## Architecture Philosophy
We believe that AI features shouldn\'t compromise system stability. Therefore, JobCopilot is built on a robust, enterprise-grade architecture:
* **Microservices & DDD:** The core backend follows Domain-Driven Design principles, ensuring clear boundaries between user management, document storage, and tracking.
* **Asynchronous AI Processing:** AI operations (LLM calls, document parsing) are heavy. We decouple them from the main backend using message queues (RabbitMQ), preventing API timeouts and ensuring smooth UX.
* **Privacy-Focused:** The architecture is designed to handle user data securely, keeping personal documents isolated and strictly bound to authorized accounts.
