package com.ibm.ei.utils;

import java.util.Date;

public class FakeDate extends Date {

  private final Date start;
  private final long interval;

  public FakeDate(long start, long interval) {
    final Date fakeNow = new Date();
    fakeNow.setTime(start);
    this.interval = interval;
    this.start = fakeNow;
  }

  @Override
  public synchronized long getTime() {
    start.setTime(start.getTime() + interval);
    return start.getTime();
  }
}
