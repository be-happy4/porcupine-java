package org.behappy.porcupine.check;

import org.behappy.porcupine.model.Model;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

record CacheEntry<S>(BitSet linearized, S state) {

    static <S> boolean cacheContains(Model<S, ?, ?, ?> model, Map<Integer, List<CacheEntry<S>>> cache, CacheEntry<S> entry) {
        List<CacheEntry<S>> list = cache.getOrDefault(entry.linearized.hashCode(), List.of());
        for (CacheEntry<S> elem : list) {
            if (entry.linearized.equals(elem.linearized) && model.equal(entry.state, elem.state)) {
                return true;
            }
        }
        return false;
    }
}