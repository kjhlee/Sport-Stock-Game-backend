package com.sportstock.scheduler.config;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

@Component
public class ProjectionSyncLock {

  private final AtomicBoolean locked = new AtomicBoolean(false);

  public boolean tryAcquire() {
    return locked.compareAndSet(false, true);
  }

  public void release() {
    locked.set(false);
  }
}
