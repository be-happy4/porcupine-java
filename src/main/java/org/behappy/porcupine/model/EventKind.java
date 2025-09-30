package org.behappy.porcupine.model;

/// Interpreting the interval \[Call, Return\] as a closed interval is the only
/// reasonable approach for how we expect this library to be used. Otherwise, we
/// might have the following situation, when using a monotonic clock to get
/// timestamps \(where successive calls to the clock return values that are always
/// greater than _or equal to_ previously returned values\):
///
/// - Client 1 calls clock\(\), gets ts=1, invokes put\("x", "y"\)
/// - Client 2 calls clock\(), gets ts=2, invokes get\("x")
/// - Client 1 operation returns, calls clock\(), gets ts=2
/// - Client 2 operation returns "", calls clock\(), gets ts=3
///
/// These operations were concurrent, but if we interpret the intervals as
/// half-open, for example, Client 1's operation had interval \[1, 2) and Client
/// 2's operation had interval \[2, 3), so they are not concurrent operations, and
/// we'd say that this history is not linearizable, which is not correct. The
/// only sensible approach is to interpret the interval \[Call, Return] as a
/// closed interval.
/// An EventKind tags an [Event] as either a function call or a return.
public enum EventKind {
    CALL,
    RETURN,
}
