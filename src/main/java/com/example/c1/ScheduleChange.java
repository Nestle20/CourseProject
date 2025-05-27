package com.example.c1;

import java.io.Serializable;
import java.time.LocalDate;

public class ScheduleChange implements Serializable {
    private static final long serialVersionUID = 1L;

    private final LocalDate oldDate;
    private final LocalDate newDate;
    private final String reason;
    private final LocalDate changeDate;

    public ScheduleChange(LocalDate oldDate, LocalDate newDate, String reason) {
        this(oldDate, newDate, reason, LocalDate.now());
    }

    public ScheduleChange(LocalDate oldDate, LocalDate newDate, String reason, LocalDate changeDate) {
        this.oldDate = oldDate;
        this.newDate = newDate;
        this.reason = reason;
        this.changeDate = changeDate;
    }

    // Getters
    public LocalDate getOldDate() { return oldDate; }
    public LocalDate getNewDate() { return newDate; }
    public String getReason() { return reason; }
    public LocalDate getChangeDate() { return changeDate; }
}