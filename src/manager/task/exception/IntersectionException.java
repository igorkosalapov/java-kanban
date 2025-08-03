package manager.task.exception;

public class IntersectionException extends RuntimeException {
    public IntersectionException() {
        super("Пересечение задач по времени");
    }
}
