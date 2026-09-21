# React Guidelines, Hooks & Best Practices

- **Component Architecture**: Pure functional components with TypeScript interfaces for props.
- **Custom Hooks**: Extract stateful logic, business rules, and external subscriptions into dedicated custom hooks (useFeatureData, useDebounce).
- **Performance**: Use useMemo and useCallback judiciously for expensive calculations or passing callbacks to optimized children; avoid premature optimization.
- **State Management**: Keep state as close to where it is used as possible; use React Context or lightweight stores (Zustand) for global cross-cutting state.
- **Effect Cleanup**: Maintain exhaustive dependency arrays in useEffect and properly clean up timers, event listeners, and abort controllers.
- **Error Boundaries**: Wrap major component subtrees in Error Boundaries with user-friendly recovery UI.
