# Next.js Fullstack Architecture & Standards

- **App Router**: Structure applications around the app/ directory with layout, template, and route files.
- **Server Components**: Default to React Server Components (RSC) for zero client-side bundle overhead and fast data retrieval.
- **Client Boundaries**: Explicitly mark interactive or state-dependent components with 'use client' at the leaf component level.
- **Server Actions**: Use Server Actions for server-side mutations with automatic form handling and optimistic UI updates.
- **Optimization**: Utilize next/image, next/font, and next/link for optimal resource preloading and zero-CLS rendering.
- **Route Protection**: Implement middleware (middleware.ts) for centralized authentication checks and session validation.
