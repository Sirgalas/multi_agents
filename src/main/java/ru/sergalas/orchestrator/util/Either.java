package ru.sergalas.orchestrator.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Either<L, R> {
    private final L left;
    private final R right;
    private final boolean isRight;

    private Either(L left, R right, boolean isRight) {
        this.left = left;
        this.right = right;
        this.isRight = isRight;
    }

    public static <L, R> Either<L, R> left(L left) {
        Objects.requireNonNull(left, "Left value cannot be null");
        return new Either<>(left, null, false);
    }

    public static <L, R> Either<L, R> right(R right) {
        Objects.requireNonNull(right, "Right value cannot be null");
        return new Either<>(null, right, true);
    }

    public boolean isLeft() {
        return !isRight;
    }

    public boolean isRight() {
        return isRight;
    }

    public L getLeft() {
        if (isRight) {
            throw new NoSuchElementException("getLeft called on Right");
        }
        return left;
    }

    public R getRight() {
        if (!isRight) {
            throw new NoSuchElementException("getRight called on Left");
        }
        return right;
    }

    public <T> T fold(Function<? super L, ? extends T> leftOp, Function<? super R, ? extends T> rightOp) {
        return isRight ? rightOp.apply(right) : leftOp.apply(left);
    }

    public void accept(Consumer<? super L> leftConsumer, Consumer<? super R> rightConsumer) {
        if (isRight) {
            rightConsumer.accept(right);
        } else {
            leftConsumer.accept(left);
        }
    }
}