# FORGESTACKK SYSTEM ARCHITECTURE & RULES

## 1. Tech Stack (STRICT COMPLIANCE)
- **Backend:** Java 17+, Spring Boot 3+, Hibernate/JPA, Maven.
- **Database:** MySQL. Strict relational mapping, foreign keys.
- **Frontend:** React (TypeScript) + TailwindCSS.
- **AI Integration:** AI connects to a CUSTOM internal server (`/api/v1/ai/...`). DO NOT import external AI SDKs (like OpenAI or Anthropic).

## 2. Coding Standards
- **No Hallucinations:** Use standard Spring Boot or React libraries only. Do not invent npm packages or Java dependencies.
- **Error Handling:** Implement `@ControllerAdvice` in Spring Boot. Use Error Boundaries and Toasts in React. Never fail silently.
- **Strict Typing:** Frontend TypeScript interfaces MUST mirror Backend Java DTOs perfectly. No `any` types.
- **Architecture:** Keep Java classes focused (Controller -> Service -> Repository). Abstract complex React logic into custom hooks.

## 3. UI/UX Design System
- **Colors:** Crisp White (Backgrounds), Deep Black (Text/Borders), Vibrant Orange (Primary Actions/Highlights). 
- **Vibe:** Professional, high-contrast, modern high-school aesthetic.