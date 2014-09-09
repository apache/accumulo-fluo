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
package io.fluo.core.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.fluo.api.client.Loader;
import io.fluo.api.client.LoaderExecutor;
import io.fluo.api.config.FluoConfiguration;
import io.fluo.core.impl.Environment;
import io.fluo.core.impl.LoadTask;

/**
 * Implementation of LoaderExecutor
 */
public class LoaderExecutorImpl implements LoaderExecutor {
  private ExecutorService executor;
  private Semaphore semaphore;

  private AtomicReference<Exception> exceptionRef = new AtomicReference<Exception>(null);
  private Environment env;

  /**
   *
   * @param props
   *          To programmatically initialize use {@link io.fluo.api.config.FluoConfiguration}
   * @throws Exception
   */

  public LoaderExecutorImpl(FluoConfiguration config) {
    this(config, config.getLoaderThreads(), config.getLoaderQueueSize());
  }

  private LoaderExecutorImpl(FluoConfiguration config, int numThreads, int queueSize) {
    if (numThreads == 0 && queueSize == 0) {
      this.env = new Environment(config);
      return;
    }

    if (numThreads <= 0) {
      throw new IllegalArgumentException("numThreads must be positivie OR numThreads and queueSize must both be 0");
    }

    if (queueSize < 0) {
      throw new IllegalArgumentException("queueSize must be non-negative OR numThreads and queueSize must both be 0");
    }

    this.env = new Environment(config);
    this.semaphore = new Semaphore(numThreads + queueSize);
    this.executor = Executors.newFixedThreadPool(numThreads);
  }

  // TODO exception handling model
  @Override
  public void execute(Loader loader) {

    if (executor == null) {
      new LoadTask(loader, env).run();
    } else {
      if (exceptionRef.get() != null) {
        throw new RuntimeException(exceptionRef.get());
      }

      final Runnable lt = new LoadTask(loader, env);

      try {
        semaphore.acquire();
      } catch (InterruptedException e1) {
        throw new RuntimeException(e1);
      }

      Runnable eht = new Runnable() {

        @Override
        public void run() {
          try {
            lt.run();
          } catch (Exception e) {
            exceptionRef.compareAndSet(null, e);
          } finally {
            semaphore.release();
          }
        }
      };

      try {
        executor.execute(eht);
      } catch (RejectedExecutionException rje) {
        semaphore.release();
        throw rje;
      }
    }
  }

  @Override
  public void close() {
    if (executor != null) {
      executor.shutdown();
      while (!executor.isTerminated()) {
        try {
          executor.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }

    env.getSharedResources().close();

    if (exceptionRef.get() != null)
      throw new RuntimeException(exceptionRef.get());
  }

}
