# TypeScript Strict Typing & Configuration

- **Strict Mode**: Enforce 'strict': true across all tsconfig configurations. Disallow 'any' in favor of unknown or generics.
- **Discriminated Unions**: Model states, events, and API payloads with tagged union types for exhaustive type checking.
- **Contracts**: Share or synchronize DTO types directly with backend schemas (OpenAPI / Zod validation).
- **Type Utilities**: Leverage built-in utility types (Pick, Omit, Partial, Readonly, Record) to avoid type duplication.
- **Imports**: Use explicit 'import type { ... }' for type-only imports to aid bundler tree-shaking.
