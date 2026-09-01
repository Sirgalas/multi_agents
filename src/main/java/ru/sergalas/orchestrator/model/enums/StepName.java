package ru.sergalas.orchestrator.model.enums;

public enum StepName {
    ARCHITECT,
    WORKER,
    TESTER,
    HELPER;

    public StepName next() {
        return switch (this) {
            case ARCHITECT -> WORKER;
            case WORKER -> TESTER;
            case TESTER -> HELPER;
            case HELPER -> null;
        };
    }
}