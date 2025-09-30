package org.behappy.porcupine.model;

import org.behappy.porcupine.util.Function3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

/// A NondeterministicModel is a nondeterministic sequential specification of a
/// system.
///
/// For basics on models, see the documentation for [Model].  In contrast to
/// Model, NondeterministicModel has a step function that returns a set of
/// states, indicating all possible next states. It can be converted to a Model
/// using the [NondeterministicModel#toModel] function.
///
/// It may be helpful to look at this package's \[test code] for examples of how
/// to write and use nondeterministic models.
///
/// [test code](https://github.com/anishathalye/porcupine/blob/master/porcupine_test.go)
///
/// @param partition
///   Partition functions, such that a history is linearizable if and only
///   if each partition is linearizable. If left nil, this package will
///   skip partitioning.
/// @param init
///   Initial states of the system.
/// @param step
///   Step function for the system. Returns all possible next states for
///   the given state, input, and output. If the system cannot step with
///   the given state/input to produce the given output, this function
///   should return an empty slice.
/// @param equal
///   Equality on states. If left nil, this package will use == as a
///   fallback \([#shallowEqual]).
/// @param describeOperation
///   For visualization, describe an operation as a string. For example,
///   "Get('x') -> 'y'". Can be omitted if you're not producing
///   visualizations.
/// @param describeState
///   For visualization purposes, describe a state as a string. For
///   example, "{'x' -> 'y', 'z' -> 'w'}". Can be omitted if you're not
///   producing visualizations.
public record NondeterministicModel<S, I, O, T>(
        Function<List<Operation<T>>, List<List<Operation<T>>>> partition,
        Function<List<Event>, List<List<Event>>> partitionEvent,
        Supplier<List<S>> init,
        Function3<S, I, O, List<S>> step,
        BiPredicate<S, S> equal,
        BiFunction<I, O, String> describeOperation,
        Function<S, String> describeState
) {
    public Model<List<S>, I, O, T> toModel() {
        var self = this;
        var equal = Objects.requireNonNullElse(this.equal,
                NondeterministicModel::shallowEqual);
        var describeOperation = Objects.requireNonNullElse(this.describeOperation,
                NondeterministicModel::defaultDescribeOperation);
        var describeState = Objects.requireNonNullElse(this.describeState,
                NondeterministicModel::defaultDescribeState);
        return new Model<>() {
            @Override
            public List<List<Operation<T>>> partition(List<Operation<T>> history) {
                return self.partition.apply(history);
            }

            @Override
            public List<List<Event>> partitionEvent(List<Event> history) {
                return self.partitionEvent.apply(history);
            }

            /// we need this wrapper to convert a \[]interface{} to an interface{}
            @Override
            public List<S> init() {
                return merge(self.init.get(), self.equal);
            }

            @Override
            public Pair<Boolean, List<S>> step(List<S> states, I input, O output) {
                var allNextStates = new ArrayList<S>();
                for (var state : states) {
                    allNextStates.addAll(self.step.apply(state, input, output));
                }
                var uniqueNextStates = merge(allNextStates, self.equal);
                return Pair.of(!uniqueNextStates.isEmpty(), uniqueNextStates);
            }

            /// this operates on sets of states that have been merged, so we
            /// don't need to check inclusion in both directions
            @Override
            public boolean equal(List<S> state1, List<S> state2) {
                if (state1.size() != state2.size()) {
                    return false;
                }
                for (var s1 : state1) {
                    var found = false;
                    for (var s2 : state2) {
                        if (equal.test(s1, s2)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public String describeOperation(I input, O output) {
                return describeOperation.apply(input, output);
            }

            @Override
            public String describeState(List<S> states) {
                var descriptions = new ArrayList<String>();
                for (S state : states) {
                    descriptions.add(describeState.apply(state));
                }
                return "{" + String.join(", ", descriptions) + "}";
            }

        };
    }

    /// noPartition is a fallback partition function that partitions the history
    /// into a single partition containing all of the operations.
    static <T> List<List<Operation<T>>> noPartition(List<Operation<T>> history) {
        return List.of(history);
    }

    /// noPartitionEvent is a fallback partition function that partitions the
    /// history into a single partition containing all of the events.
    static List<List<Event>> noPartitionEvent(List<Event> history) {
        return List.of(history);
    }

    /// shallowEqual is a fallback equality function that compares two states using
    /// ==.
    static <S> boolean shallowEqual(S state1, S state2) {
        return Objects.equals(state1, state2);
    }

    static <I, O> String defaultDescribeOperation(I input, O output) {
        return input + " -> " + output;
    }

    static <S> String defaultDescribeState(S state) {
        return String.valueOf(state);
    }

    static <S> List<S> merge(List<S> states, BiPredicate<S, S> equal) {
        var uniqueStats = new ArrayList<S>();
        for (S state : states) {
            var unique = true;
            for (S us : uniqueStats) {
                if (equal.test(state, us)) {
                    unique = false;
                    break;
                }
            }
            if (unique) {
                uniqueStats.add(state);
            }
        }
        return uniqueStats;
    }
}
