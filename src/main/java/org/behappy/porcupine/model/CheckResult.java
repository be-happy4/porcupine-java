package org.behappy.porcupine.model;

/// A CheckResult is the result of a linearizability check.
///
/// Checking for linearizability is decidable, but it is an NP-hard problem, so
/// the checker might take a long time. If a timeout is not given, functions in
/// this package will always return Ok or Illegal, but if a timeout is supplied,
/// then some functions may return Unknown. Depending on the use case, you can
/// interpret an Unknown result as Ok (i.e., the tool didn't find a
/// linearizability violation within the given timeout).
public enum CheckResult {
    /// timed out
    Unknown,
    Ok,
    Illegal,
}
