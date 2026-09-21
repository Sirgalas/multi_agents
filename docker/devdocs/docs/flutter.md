# Flutter Framework & UI Widget Guidelines

- **Architecture**: Clean Architecture or Feature-First structure (Presentation, Domain, Data layers).
- **State Management**: BLoC / Cubit or Riverpod for unidirectional data flow and clear decoupling of UI from business rules.
- **Widget Performance**: Maximize the use of const constructors on widgets to eliminate redundant widget tree rebuilds.
- **Repository Pattern**: Abstract all network calls and local cache storage behind strongly typed repository interfaces.
- **Responsive Layout**: Use LayoutBuilder, MediaQuery, and adaptive widgets to ensure seamless rendering across phones, tablets, and web.
