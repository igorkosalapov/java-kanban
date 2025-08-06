package manager.task.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException() {
        super("Ресурс не найден");
    }

    public NotFoundException(String message) {
        super(message);
    }
}
