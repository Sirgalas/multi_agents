# PostgreSQL Best Practices & Schema Guidelines

- **Primary Keys**: Standardize on BIGSERIAL / BIGINT GENERATED ALWAYS AS IDENTITY or sequential UUIDv7 for high-insert tables.
- **Semi-Structured Data**: Use JSONB with GIN indexing for polymorphic or dynamic attributes; avoid overusing instead of relational normalization.
- **Date & Time**: Always use TIMESTAMP WITH TIME ZONE (TIMESTAMPTZ), serialize timestamps in UTC ISO-8601 format.
- **Migrations**: Versioned schema changes strictly via Flyway scripts (V...__description.sql). Never alter applied migrations.
- **Indexing**: B-Tree for equality and range filters, GIN for JSONB and text search, Partial indexes for status-filtered queries (WHERE is_active = true).
- **Transactions**: Default isolation level Read Committed. Keep transactions short to prevent lock contention.
