package com.example.demo.timetravel;

/** Request/response bodies for POST /api/timetravel/answer. */
public final class AnswerCheck {

    private AnswerCheck() {
    }

    public record Request(int scenario, TimePeriod period, int question, String answer) {
    }

    public record Result(boolean correct, String message) {
    }
}
