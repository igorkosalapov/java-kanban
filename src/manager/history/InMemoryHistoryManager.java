package manager.history;

import model.Task;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class InMemoryHistoryManager implements HistoryManager {
    private Node head;
    private Node tail;
    private final Map<Integer, Node> nodes = new HashMap<>();

    @Override
    public void add(Task task) {
        if (task == null) return;

        remove(task.getId());

        linkLast(task);
    }

    private void linkLast(Task task) {
        Node newNode = new Node();
        newNode.setValue(task);
        newNode.setPrev(tail);

        if (tail != null) {
            tail.setNext(newNode);
        } else {
            head = newNode;
        }

        tail = newNode;
        nodes.put(task.getId(), newNode);
    }

    @Override
    public void remove(int id) {
        Node node = nodes.get(id);
        removeNode(node);
    }

    @Override
    public List<Task> getHistory() {
        List<Task> result = new ArrayList<>();
        Node current = head;

        while (current != null) {
            result.add(current.getValue());
            current = current.getNext();
        }

        return result;
    }

    private void removeNode(Node node) {
        if (node == null) return;

        Node prev = node.getPrev();
        Node next = node.getNext();

        if (prev != null) {
            prev.setNext(next);
        } else {
            head = next;
        }

        if (next != null) {
            next.setPrev(prev);
        } else {
            tail = prev;
        }

        nodes.remove(node.getValue().getId());
    }

    private static class Node {
        private Task value;
        private Node prev;
        private Node next;

        public Node getNext() {
            return next;
        }

        public Node getPrev() {
            return prev;
        }

        public Task getValue() {
            return value;
        }

        public void setValue(Task value) {
            this.value = value;
        }

        public void setPrev(Node prev) {
            this.prev = prev;
        }

        public void setNext(Node next) {
            this.next = next;
        }
    }
}