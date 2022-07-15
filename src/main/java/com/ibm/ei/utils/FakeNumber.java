package com.ibm.ei.utils;

public class FakeNumber {

  private final Number start;
  private final Number end;
  private final Number increment;
  private Number next;

  public FakeNumber(Number start, Number end, Number increment) {
    this.start = start;
    this.end = end;
    this.next = start;
    this.increment = increment;
  }

  public Number next() {
    if (next == end) {
      next = start;
      return next;
    }

    next = next.doubleValue() + increment.doubleValue();
    return next;
  }
}
