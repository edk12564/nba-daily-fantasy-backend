package com.picknroll.demo.services;

public record RosterWriteOutcome(Status status, Integer totalPrice) {

    public enum Status {
        SAVED,
        DELETED,
        OVER_CAP,
        NOT_CHANGED,
        NO_GAMES,
        LOCKED
    }

    public static RosterWriteOutcome of(Status status) {
        return new RosterWriteOutcome(status, null);
    }

    public static RosterWriteOutcome overCap(int totalPrice) {
        return new RosterWriteOutcome(Status.OVER_CAP, totalPrice);
    }
}
