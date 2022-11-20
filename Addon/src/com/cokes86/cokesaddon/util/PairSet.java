package com.cokes86.cokesaddon.util;

import org.jetbrains.annotations.NotNull;

public class PairSet<L, R> implements Cloneable {
    public static <L, R> PairSet<L, R> of(@NotNull L left, @NotNull R right) {
        return new PairSet<>(left, right);
    }

    private L left;

    public @NotNull L getLeft() {
        return this.left;
    }

    public void setLeft(@NotNull L left) {
        this.left = left;
    }

    private R right;

    public @NotNull R getRight() {
        return this.right;
    }

    public void setRight(@NotNull R right) {
        this.right = right;
    }

    public PairSet(@NotNull L left,@NotNull R right) {
        this.left = left;
        this.right = right;
    }
}
