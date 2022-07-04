package com.ibm.ei.utils;

import java.util.Date;

public class FakeDate extends Date {

  private final Date start;

  public FakeDate(long start) {
    final Date fakeNow = new Date();
    fakeNow.setTime(start);
    this.start = fakeNow;
  }

  @Override
  public synchronized long getTime() {
    start.setTime(start.getTime() + 1000);
    return start.getTime();
  }
}
