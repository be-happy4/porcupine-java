package org.behappy.porcupine.check;

import org.behappy.porcupine.model.CallEvent;
import org.behappy.porcupine.model.Event;
import org.behappy.porcupine.model.Model;
import org.behappy.porcupine.model.Operation;
import org.behappy.porcupine.model.Pair;
import org.behappy.porcupine.model.ReturnEvent;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.behappy.porcupine.check.CacheEntry.cacheContains;

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


    public static <S, I, O, T> Pair<Boolean, List<List<Integer>>> checkSingle(
            Model<S, I, O, T> model,
            List<Entry<T>> history,
            boolean computePartial,
            AtomicInteger kill
    ) {
        var entry = makeLinkedEntries(history);
        int n = entry.length() / 2;
        var linearized = new BitSet(n);
        Map<Integer, List<CacheEntry<S>>> cache = new HashMap<>();
        List<CallsEntry<T, S>> calls = new ArrayList<>();
        List<List<Integer>> longest = new ArrayList<>(Collections.nCopies(n, null));

        S state = model.init();
        var headEntry = entry.insertBefore(new Node<>(null, null, -1));

        while (headEntry.next != null) {
            if (kill.get() != 0) {
                return Pair.of(false, longest);
            }
            if (entry.match != null) {
                var matching = entry.match;
                Pair<Boolean, S> stepResult = model.step(state, entry.value, matching.value);
                boolean ok = stepResult.first();
                S newState = stepResult.second();
                if (ok) {
                    BitSet newLinearized = (BitSet) linearized.clone();
                    newLinearized.set(entry.id);
                    CacheEntry<S> newCacheEntry = new CacheEntry<>(newLinearized, newState);
                    if (!cacheContains(model, cache, newCacheEntry)) {
                        var hash = newLinearized.hashCode();
                        cache.computeIfAbsent(hash, k -> new ArrayList<>()).add(newCacheEntry);
                        calls.add(new CallsEntry<>(entry, state));
                        state = newState;
                        linearized.set(entry.id);
                        entry.lift();
                        entry = headEntry.next;
                    } else {
                        entry = entry.next;
                    }
                } else {
                    entry = entry.next;
                }
            } else {
                if (calls.isEmpty()) {
                    return Pair.of(false, longest);
                }
                if (computePartial) {
                    int callsLen = calls.size();
                    List<Integer> seq = null;
                    for (var v : calls) {
                        if (longest.get(v.entry().id) == null || callsLen > longest.get(v.entry().id).size()) {
                            if (seq == null) {
                                seq = new ArrayList<>();
                                for (var vv : calls) {
                                    seq.add(vv.entry().id);
                                }
                            }
                            longest.set(v.entry().id, seq);
                        }
                    }
                }
                var callsTop = calls.get(calls.size() - 1);
                entry = callsTop.entry;
                state = callsTop.state;
                linearized.clear(entry.id);
                calls.remove(calls.size() - 1);
                entry.unlift();
                entry = entry.next;
            }
        }
        List<Integer> seq = new ArrayList<>();
        for (var v : calls) {
            seq.add(v.entry().id);
        }
        for (int i = 0; i < n; i++) {
            longest.set(i, seq);
        }
        return Pair.of(true, longest);
    }
}
