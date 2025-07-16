package model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Epic extends Task {
    private final List<Integer> subtaskIds = new ArrayList<>();

    public Epic(String name, String description) {
        super(name, description, Status.NEW, null, Duration.ZERO);
    }

    public void updateTimeFields(List<Subtask> subtasks) {
        if (subtasks == null || subtasks.isEmpty()) {
            setStartTime(null);
            setDuration(null);
            return;
        }

        LocalDateTime earliestStart = null;
        LocalDateTime latestEnd = null;
        Duration totalDuration = Duration.ZERO;

        for (Subtask subtask : subtasks) {
            if (subtask.getStartTime() != null && subtask.getDuration() != null) {
                LocalDateTime subStart = subtask.getStartTime();
                LocalDateTime subEnd = subtask.getEndTime();

                totalDuration = totalDuration.plus(subtask.getDuration());

                if (earliestStart == null || subStart.isBefore(earliestStart)) {
                    earliestStart = subStart;
                }
                if (latestEnd == null || subEnd.isAfter(latestEnd)) {
                    latestEnd = subEnd;
                }
            }
        }

        setStartTime(earliestStart);
        setDuration(totalDuration.isZero() ? null : totalDuration);
    }


    public List<Integer> getSubtaskIds() {
        return subtaskIds;
    }

    public void addSubtaskId(int subtaskId) {
        if (subtaskId == this.getId()) {
            throw new IllegalArgumentException("Эпик не может содержать сам себя в качестве подзадачи");
        }
        subtaskIds.add(subtaskId);
    }

    public void removeSubtaskId(int subtaskId) {
        subtaskIds.remove((Integer) subtaskId);
    }

    @Override
    public TaskType getType() {
        return TaskType.EPIC;
    }

    @Override
    public LocalDateTime getEndTime() {
        if (getStartTime() != null && getDuration() != null) {
            return getStartTime().plus(getDuration());
        }
        return null;
    }

    @Override
    public String toString() {
        return "Epic{" +
                "id=" + getId() +
                ", name='" + getName() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", status=" + getStatus() +
                ", subtaskIds=" + subtaskIds +
                '}';
    }
}
