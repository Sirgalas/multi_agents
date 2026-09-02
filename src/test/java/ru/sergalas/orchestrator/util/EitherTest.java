package ru.sergalas.orchestrator.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unit Tests: Either Monad Container")
class EitherTest {

    @Test
    @DisplayName("Left branch creation and retrieval should succeed")
    void testLeftCreationAndRetrieval() {
        String errorPayload = "Validation failed";
        Either<String, Integer> either = Either.left(errorPayload);

        assertThat(either.isLeft()).isTrue();
        assertThat(either.isRight()).isFalse();
        assertThat(either.getLeft()).isEqualTo(errorPayload);
    }

    @Test
    @DisplayName("Right branch creation and retrieval should succeed")
    void testRightCreationAndRetrieval() {
        Integer successPayload = 200;
        Either<String, Integer> either = Either.right(successPayload);

        assertThat(either.isRight()).isTrue();
        assertThat(either.isLeft()).isFalse();
        assertThat(either.getRight()).isEqualTo(successPayload);
    }

    @Test
    @DisplayName("Calling getRight on Left should throw NoSuchElementException")
    void testCallingGetRightOnLeftThrows() {
        Either<String, Integer> either = Either.left("Error");

        assertThatThrownBy(either::getRight)
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("getRight called on Left");
    }

    @Test
    @DisplayName("Calling getLeft on Right should throw NoSuchElementException")
    void testCallingGetLeftOnRightThrows() {
        Either<String, Integer> either = Either.right(42);

        assertThatThrownBy(either::getLeft)
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("getLeft called on Right");
    }

    @Test
    @DisplayName("Creating Left or Right with null should throw NullPointerException")
    void testNullGuards() {
        assertThatThrownBy(() -> Either.left(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Left value cannot be null");

        assertThatThrownBy(() -> Either.right(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Right value cannot be null");
    }

    @Test
    @DisplayName("Fold operation should map correctly according to underlying state")
    void testFold() {
        Either<String, Integer> leftEither = Either.left("Err");
        Either<String, Integer> rightEither = Either.right(100);

        String fromLeft = leftEither.fold(l -> "Mapped: " + l, r -> "Number: " + r);
        String fromRight = rightEither.fold(l -> "Mapped: " + l, r -> "Number: " + r);

        assertThat(fromLeft).isEqualTo("Mapped: Err");
        assertThat(fromRight).isEqualTo("Number: 100");
    }

    @Test
    @DisplayName("Accept consumers should trigger only the corresponding branch")
    void testAccept() {
        AtomicBoolean leftCalled = new AtomicBoolean(false);
        AtomicBoolean rightCalled = new AtomicBoolean(false);

        Either<String, Integer> either = Either.right(777);
        either.accept(l -> leftCalled.set(true), r -> rightCalled.set(true));

        assertThat(leftCalled.get()).isFalse();
        assertThat(rightCalled.get()).isTrue();
    }
}