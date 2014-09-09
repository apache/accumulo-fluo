/*
 * Copyright 2014 Fluo authors (see AUTHORS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fluo.core.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.fluo.api.data.Column;
import io.fluo.api.observer.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.fluo.core.util.UtilWaitThread;

/**
 *
 */
public class WorkerTask implements Runnable {

  // TODO max sleep time should probably be a function of the total number of threads in the system
  private static long MAX_SLEEP_TIME = 5 * 60 * 1000;

  private static Logger log = LoggerFactory.getLogger(WorkerTask.class);
  private final  Environment env;
  private final AtomicBoolean shutdownFlag;

  public WorkerTask(Environment env, AtomicBoolean shutdownFlag) {
    this.env = env;
    this.shutdownFlag = shutdownFlag;
  }

  @Override
  public void run() {
    // TODO handle worker dying
    Worker worker = null;
    try {
      worker = new Worker(env, new RandomTabletChooser(env));
    } catch (Exception e1) {
      log.error("Error creating worker", e1);
      throw new RuntimeException(e1);
    }

    long sleepTime = 0;

    Map<Column,Observer> colObservers = new HashMap<Column,Observer>();

    try {
      while (!shutdownFlag.get()) {
        long numProcessed = 0;
        try {
          startedProcessing();
          numProcessed = worker.processUpdates(colObservers);
        } catch (Exception e) {
          log.error("Error while processing updates", e);
        } finally {
          finishedProcessing(numProcessed);
        }

        if (numProcessed > 0) {
          sleepTime = 0;
        } else if (sleepTime == 0) {
          sleepTime = 100;
        } else if (sleepTime < MAX_SLEEP_TIME) {
          sleepTime = sleepTime + (long) (sleepTime * Math.random());
        }

        log.debug("thread id:" + Thread.currentThread().getId() + "  numProcessed:" + numProcessed + "  sleepTime:" + sleepTime);

        UtilWaitThread.sleep(sleepTime, shutdownFlag);
      }
    } catch (Exception e1) {
      log.error("Error in worker loop", e1);
    } finally {
      for (Observer observer : colObservers.values()) {
        try {
          observer.close();
        } catch (Exception e) {
          log.error("Failed to close observer", e);
        }
      }
    }
  }

  public void startedProcessing() {}

  public void finishedProcessing(long numProcessed) {}

}
