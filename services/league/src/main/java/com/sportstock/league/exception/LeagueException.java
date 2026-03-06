package com.sportstock.league.exception;

public class LeagueException extends RuntimeException {

    public LeagueException(String message) {
        super(message);
    }

    public LeagueException(String message, Throwable cause) {
        super(message, cause);
    }
}
