# Hibernate ORM Best Practices & Performance

- **N+1 Problem**: Always eliminate N+1 select queries using @EntityGraph or JOIN FETCH on relationships.
- **Foreign Keys**: Ensure database indexes exist on all foreign key columns.
- **Projections**: Use immutable record/DTO projections for read-heavy operations instead of loading heavy managed entity graphs.
- **Bidirectional Associations**: Always provide helper methods on the owning entity (e.g., addItem(), removeItem()) to maintain in-memory consistency.
- **Cascading**: Never use CascadeType.ALL or CascadeType.REMOVE on @ManyToMany relationships.
- **Schema Management**: Enforce spring.jpa.hibernate.ddl-auto=validate in production; manage all schema evolutions strictly through Flyway migrations.
- **Batch Processing**: Configure batch fetching and batch inserts (hibernate.jdbc.batch_size=30) for bulk data operations.
