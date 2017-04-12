package com.sample.service;

import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.MDC;

public class HystrixContextCallable<T> implements Callable<T> {

  private final Callable<T> callable;
  private final Map<String, String> parentMDC;

  public HystrixContextCallable(Callable<T> callable) {
    this.callable = callable;
    this.parentMDC = MDC.getCopyOfContextMap();
  }

  @Override
  public T call() throws Exception {
    MDC.setContextMap(parentMDC);
    return callable.call();
  }
}
