package org.behappy.porcupine.model;

import org.behappy.porcupine.check.Entry;
import org.behappy.porcupine.check.EntryKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// An Event is an element of a history, a function call event or a return
/// event.
///
/// This package supports two different representations of histories, as a
/// sequence of Event or \[Operation\]. In the Event representation, function
/// calls and returns are only relatively ordered and do not have absolute
/// timestamps.
///
/// The Id field is used to match a function call event with its corresponding
/// return event.
public sealed interface Event permits CallEvent, ReturnEvent {
    /// optional, unless you want a visualization; zero-indexed
    default int clientId() {
        return 0;
    }

    int id();

    static <T> List<Event> renumber(List<Event> events) {
        var e = new ArrayList<Event>();
        var m = new HashMap<Integer, Integer>(); // renumbering
        var id = 0;
        for (Event v : events) {
            var r = m.get(v.id());
            if (r != null) {
                e.add(of(v, r));
            } else {
                e.add(of(v, id));
                m.put(v.id(), id);
                id++;
            }
        }
        return e;
    }

    @SuppressWarnings("unchecked")
    static <T> Event of(Event old, int id) {
        return switch (old) {
            case CallEvent<?>(int clientId, Object value, _) ->
                    new CallEvent<>(clientId, (T) value, id);
            case ReturnEvent(int clientId, long ts, _) -> new ReturnEvent(clientId, ts, id);
        };
    }


    @SuppressWarnings("unchecked")
    public static <T> List<Entry<T>> convertEntries(List<Event> events) {
        List<Entry<T>> entries = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            var elem = events.get(i);
            var kind = elem instanceof ReturnEvent ? EntryKind.RETURN : EntryKind.CALL;
            var value = elem instanceof CallEvent<?> c ? (T) c.value() : null;
            // use index as "time"
            entries.add(new Entry<>(kind, value, elem.id(), i, elem.clientId()));
        }
        return entries;
    }
}
