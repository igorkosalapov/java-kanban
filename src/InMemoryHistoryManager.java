import java.util.*;

public class InMemoryHistoryManager {
    private final Map<Integer, Node<Task>> historyMap = new HashMap<>();
    private Node<Task> head;
    private Node<Task> tail;
    private int size = 0;
    private static final int MAX_HISTORY_SIZE = 10;

    private static class Node<T> {
        T data;
        Node<T> next;
        Node<T> prev;

        Node(T data) {
            this.data = data;
        }
    }

    public void add(Task task) {
        if (task == null) return;

        removeNode(historyMap.get(task.getId()));

        Node<Task> newNode = new Node<>(task);
        addLast(newNode);
        historyMap.put(task.getId(), newNode);

        if (size > MAX_HISTORY_SIZE) {
            removeFirst();
        }
    }

    public List<Task> getHistory() {
        List<Task> result = new ArrayList<>();
        Node<Task> current = head;
        while (current != null) {
            result.add(current.data);
            current = current.next;
        }
        return result;
    }

    private void addLast(Node<Task> node) {
        if (head == null) {
            head = node;
        } else {
            tail.next = node;
            node.prev = tail;
        }
        tail = node;
        size++;
    }

    private void removeNode(Node<Task> node) {
        if (node == null) return;

        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
        }

        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            tail = node.prev;
        }

        size--;
    }

    private void removeFirst() {
        if (head != null) {
            historyMap.remove(head.data.getId());
            head = head.next;
            if (head != null) {
                head.prev = null;
            } else {
                tail = null;
            }
            size--;
        }
    }
}