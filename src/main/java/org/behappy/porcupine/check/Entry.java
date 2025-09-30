package org.behappy.porcupine.check;

import org.behappy.porcupine.model.CallEvent;
import org.behappy.porcupine.model.Event;
import org.behappy.porcupine.model.Operation;
import org.behappy.porcupine.model.ReturnEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public record Entry<T>(
        EntryKind kind,
        T value,
        int id,
        long time,
        int clientId) implements Comparable<Entry<T>> {

    static <T> List<Entry<T>> makeEntries(List<Operation<T>> history) {
        var entries = new ArrayList<Entry<T>>();
        var id = 0;
        for (var elem : history) {
            entries.add(new Entry<>(EntryKind.CALL, elem.input(), id, elem.callTime(), elem.clientId()));
            entries.add(new Entry<>(EntryKind.CALL, elem.output(), id, elem.returnTime(), elem.clientId()));
            id++;
        }
        entries.sort(Comparator.naturalOrder());
        return entries;
    }

    @Override
    public int compareTo(Entry<T> o) {
        if (time() != o.time()) {
            return time() < o.time() ? -1 : 1;
        }
        // if the timestamps are the same, we need to make sure we order calls
        // before returns
        return kind.compareTo(o.kind);
    }

    static <T> Node<T> makeLinkedEntries(List<Entry<T>> entries) {
        Node<T> root = null;
        var match = new HashMap<Integer, Node<T>>();
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry<T> elem = entries.get(i);
            Node<T> entry;
            if (elem.kind == EntryKind.RETURN) {
                entry = new Node<>(elem.value, null, elem.id);
                match.put(elem.id, entry);
            } else {
                entry = new Node<>(elem.value, match.get(elem.id), elem.id);
            }
            root = entry.insertBefore(root);
        }
        return root;
    }
}
