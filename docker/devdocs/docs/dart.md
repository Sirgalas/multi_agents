# Dart Language Conventions & Idiomatic Patterns

- **Null Safety**: Strict null-safety enforced. Avoid force unwrapping (!) unless guaranteed non-null by previous guards.
- **Naming Conventions**: lowerCamelCase for identifiers, functions, and variables; UpperCamelCase for classes, mixins, and enums.
- **Asynchronous Code**: Clean async/await flow with comprehensive error handling via try/catch and typed exceptions.
- **Data Models**: Use immutable data classes with value equality (via freezed or Dart 3 records).
- **Extensions**: Use extension methods to augment existing classes with utility methods cleanly.
