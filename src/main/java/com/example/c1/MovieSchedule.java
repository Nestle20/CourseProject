package com.example.c1;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MovieSchedule implements Serializable {
    private static final long serialVersionUID = 1L;

    private int movieId;
    private LocalDate plannedDate;
    private LocalDate completionDate;
    private List<ScheduleChange> changeHistory = new ArrayList<>();
    private boolean reminderSent = false;

    public MovieSchedule(int movieId, LocalDate plannedDate) {
        this.movieId = movieId;
        this.plannedDate = plannedDate;
    }

    // Getters and setters
    public int getMovieId() { return movieId; }
    public LocalDate getPlannedDate() { return plannedDate; }
    public LocalDate getCompletionDate() { return completionDate; }
    public List<ScheduleChange> getChangeHistory() { return changeHistory; }
    public boolean isReminderSent() { return reminderSent; }

    public void setPlannedDate(LocalDate newDate, String reason) {
        if (plannedDate != null) {
            changeHistory.add(new ScheduleChange(plannedDate, newDate, reason));
        }
        this.plannedDate = newDate;
        this.reminderSent = false;
    }

    public void markAsCompleted() {
        this.completionDate = LocalDate.now();
    }

    public void markAsCompleted(LocalDate date) {
        this.completionDate = date;
    }

    public void setReminderSent(boolean reminderSent) {
        this.reminderSent = reminderSent;
    }
}