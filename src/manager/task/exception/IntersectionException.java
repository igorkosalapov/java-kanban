package manager.task.exception;

public class IntersectionException extends IllegalArgumentException {
    public IntersectionException() {
        super("Пересечение задач по времени");
    }
}
