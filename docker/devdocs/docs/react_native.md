# React Native Mobile Standards

- **Component Design**: Modular cross-platform components with platform-specific extensions (.ios.tsx, .android.tsx) when needed.
- **Navigation**: Use React Navigation with native stack navigator for native 60/120fps screen transitions.
- **Offline-First**: Cache network responses locally (MMKV / SQLite) to guarantee instant screen load and offline reliability.
- **Permissions**: Request runtime permissions gracefully with clear rationale before accessing device resources (camera, location, notifications).
- **Animations**: Execute smooth 60fps animations on the UI thread using React Native Reanimated and Gesture Handler.
