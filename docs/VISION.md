<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](VISION.md) | [简体中文](i18n/zh-Hans-CN/VISION.md) | [繁體中文](i18n/zh-Hant-TW/VISION.md)

# Vision: JobCopilot

## The Problem

Tailoring a resume for each job posting is tedious. Job boards use keyword matching (TF-IDF), so qualified candidates get filtered out when their resume uses synonyms the system doesn't recognize. 

Generic AI chatbots (ChatGPT, etc.) can rewrite resumes but have no memory: they can't track your application pipeline, keep document versions, or maintain historical edits.

## Our Vision

**JobCopilot** is an open-source, AI-powered platform that acts as a persistent career assistant. 

It parses uploaded resumes, evaluates them against job descriptions using semantic vector matching, and provides an interactive AI assistant to iteratively optimize content. The goal: cut hours of manual tailoring and improve interview rates.

## Core Capabilities

* **Resume Parsing:** Converts PDF/Word into structured, ATS-friendly JSON.
* **Semantic Job Matching:** Uses vector search (not keyword matching) to understand candidate capability against job requirements.
* **Iterative Optimization:** Interactive chat interface to refine resumes, retaining context of historical experience.
* **Version Control:** Maintains resume versions (Original, Converted, AI-Optimized) with rollback.

## Architecture Philosophy

AI features should not make the system fragile. JobCopilot is built accordingly:

* **Microservices & DDD:** The backend follows Domain-Driven Design. Boundaries between user management, document storage, and tracking are explicit.
* **Asynchronous AI Processing:** LLM calls and document parsing are heavy. They run through RabbitMQ, decoupled from the main backend, so API timeouts don't affect the user experience.
* **Privacy-First:** User documents are isolated and bound to authorized accounts only.
