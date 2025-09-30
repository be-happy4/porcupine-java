package org.behappy.porcupine.check;

import org.behappy.porcupine.model.Operation;
import org.behappy.porcupine.visualization.Annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

///
/// @param history for each partition, a list of entries
/// @param partialLinearizations for each partition, a set of histories \(list of ids)
/// PartialLinearizations returns partial linearizations found during the
/// linearizability check, as sets of operation IDs.
///
/// For each partition, it returns a set of possible linearization histories,
/// where each history is represented as a sequence of operation IDs. If the
/// history is linearizable, this will contain a complete linearization. If not
/// linearizable, it contains the maximal partial linearizations found.
public record LinearizationInfo<T>(
        List<List<Entry<T>>> history,
        List<List<List<Integer>>> partialLinearizations,
        List<Annotation> annotations) {
    public List<List<List<Operation<T>>>> partialLinearizationsOperations() {
        var result = new ArrayList<List<List<Operation<T>>>>(history.size());
        for (int p = 0; p < history.size(); p++) {
            var partition = history.get(p);
            // reconstruct operations based on entries
            var callMap = new HashMap<Integer, Entry<T>>();
            var retMap = new HashMap<Integer, Entry<T>>();
            for (var e : partition) {
                if (e.kind() == EntryKind.CALL) {
                    callMap.put(e.id(), e);
                } else {
                    retMap.put(e.id(), e);
                }
            }

            // 通过 id 组装 Operation
            var opMap = new HashMap<Integer, Operation<T>>();
            for (var callEntry : callMap.entrySet()) {
                int id = callEntry.getKey();
                var call = callEntry.getValue();
                var ret = retMap.get(id);
                if (ret == null) {
                    // this should never happen, because the LinearizationInfo
                    // object should always contain valid partial linearizations,
                    // where there is a return for every call
                    throw new RuntimeException("cannot find corresponding return for call");
                }
                opMap.put(id, new Operation<>(
                        call.clientId(),
                        call.value(),
                        call.time(),
                        ret.value(),
                        ret.time()
                ));
            }

            var partials = new ArrayList<List<Operation<T>>>(partialLinearizations.get(p).size());
            for (var linearization : partialLinearizations.get(p)) {
                var partialsi = new ArrayList<Operation<T>>();
                for (int id : linearization) {
                    var op = opMap.get(id);
                    if (op == null) {
                        // this should never happen, because the LinearizationInfo
                        // object should always contain valid partial
                        // linearizations, where every ID in the partial
                        // linearization is in the history
                        throw new RuntimeException("cannot find operation for given id in linearization");
                    }
                    partialsi.add(op);
                }
                partials.add(partialsi);
            }
            result.add(partials);
        }
        return result;
    }
}
