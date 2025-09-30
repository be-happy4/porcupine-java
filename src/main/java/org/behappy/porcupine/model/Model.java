package org.behappy.porcupine.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

/// A Model is a sequential specification of a system.
///
/// Note: models in this package are expected to be purely functional. That is,
/// the model Step function should not modify the given state \(or input or
/// output\), but return a new state.
///
/// Only the Init, Step, and Equal functions are necessary to specify if you
/// just want to test histories for linearizability.
///
/// Implementing the partition functions can greatly improve performance. If
/// you're implementing the partition function, the model Init and Step
/// functions can be per-partition. For example, if your specification is for a
/// key-value store and you partition by key, then the per-partition state
/// representation can just be a single value rather than a map.
///
/// Implementing DescribeOperation and DescribeState will produce nicer
/// visualizations.
///
/// It may be helpful to look at this package's \[test code\] for examples of how
/// to write models, including models that include partition functions.
///
/// [test code](https://github.com/anishathalye/porcupine/blob/master/porcupine_test.go)
public interface Model<S, I, O, T> {
    /// Partition functions, such that a history is linearizable if and only
    /// if each partition is linearizable. If left nil, this package will
    /// skip partitioning.
    List<List<Operation<T>>> partition(List<Operation<T>> history);

    List<List<Event>> partitionEvent(List<Event> history);

    /// Initial states of the system.
    S init();

    /// Step function for the system. Returns all possible next states for
    /// the given state, input, and output. If the system cannot step with
    /// the given state/input to produce the given output, this function
    /// should return an empty slice.
    Pair<Boolean, S> step(S state, I input, O output);

    /// Equality on states. If left nil, this package will use == as a
    /// fallback \(\[ShallowEqual]).
    boolean equal(S state1, S state2);

    /// For visualization, describe an operation as a string. For example,
    /// "Get\('x') -> 'y'". Can be omitted if you're not producing
    /// visualizations.
    String describeOperation(I input, O output);

    /// For visualization purposes, describe a state as a string. For
    /// example, "{'x' -> 'y', 'z' -> 'w'}". Can be omitted if you're not
    /// producing visualizations.
    String describeState(S state);

}
