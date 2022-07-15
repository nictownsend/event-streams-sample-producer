package com.ibm.ei.utils;

import java.time.Duration;
import java.util.Date;

public class FakeDate extends Date {

  private final Date start;
  private final Date end;
  private final Duration interval;

  public FakeDate(Date start, Date end, int numRecords) {
    this.start = Date.from(start.toInstant());
    this.end = Date.from(end.toInstant());
    this.interval =
        Duration.between(this.start.toInstant(), this.end.toInstant()).dividedBy(numRecords);
  }

  @Override
  public synchronized long getTime() {
    final long next = start.toInstant().plus(interval).toEpochMilli();
    start.setTime(next);
    return next;
  }
}
