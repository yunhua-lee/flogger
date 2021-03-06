/*
 * Copyright (C) 2020 The Flogger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.flogger;

import com.google.common.flogger.util.Checks;
import com.google.errorprone.annotations.CheckReturnValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * EventAggregator is used to log many same type events. The typical scenario is api request and response.
 * <p>
 * If we log each api request and response, the log data will be too large. But if we do not log each api request and
 * response, when there are some errors, we can not confirm if we get the request or return the response.
 * <p>
 * EventAggregator can aggregate many same type events and log the key information within one log.
 * For example:
 * requestId=1053:200 | requestId=1054:200 | requestId=1055:500 | requestId=1056:200 | requestId=1057:200 |
 * requestId=1063:200 | requestId=1064:200 | requestId=1065:200 | requestId=1066:404 | requestId=1067:200 |
 * <p>
 * The best practice is to use {@link EventAggregator} to log brief information for all api request and response,
 * and use {@link FluentLogger} to log detailed information for error requests and responses.
 */
@CheckReturnValue
public class EventAggregator extends AggregatedLogContext<FluentAggregatedLogger, EventAggregator> {

  /**
   * Use LinkedBlockingQueue to store events.
   * <p>
   * Two reasons for using LinkedBlockingQueue:
   * 1. Thread-safe: many threads will use the same {@link EventAggregator} to log same type events.
   * 2. Async log: logging aggregated events is a time-consuming action. It's better to use separate thread to do it.
   */
  protected final BlockingQueue<EventAggregator.EventPair> eventList;

  /**
   * Instantiates a new Event aggregator.
   *
   * @param name    the name
   * @param logger  the logger (see {@link FluentAggregatedLogger}).
   * @param logSite the log site (see {@link LogSite}).
   * @param pool    the executor service pool used to periodically log data.
   */
  EventAggregator(String name, FluentAggregatedLogger logger, LogSite logSite,
                  ScheduledExecutorService pool, int capacity) {
    super(name, logger, logSite, pool);
    eventList = new LinkedBlockingQueue<EventPair>(capacity);
  }

  @Override
  protected EventAggregator self() {
    return this;
  }

  /**
   * Add event to {@link EventAggregator}.
   *
   * @param event   the event
   * @param content the content
   */
  public void add(String event, String content) {
    //try 3 times
    int i = 0;
    while (i++ < 3) {
      try {
        if (eventList.offer(new EventPair(event, content), 1, TimeUnit.MILLISECONDS)) {
          if (shouldFlushByNumber()) {
            asyncFlush(getNumberWindow());
          }

          break;
        } else {
          //If BlockingQueue is full, just immediately flush
          asyncFlush(0);
          Thread.sleep(1);
        }
      } catch (InterruptedException e) {
        if (i == 2) {
          //Do not log anything, just print stacktrace
          e.printStackTrace();
        }
      }
      ;
    }
    ;
  }

  @Override
  public boolean shouldFlushByNumber() {
    return eventList.size() >= getNumberWindow();
  }

  @Override
  public int haveData() {
    return eventList.size();
  }

  @Override
  public String message(int count) {
    Checks.checkArgument(count >= 0, "count should be >=0");

    List<EventPair> eventBuffer = new ArrayList<EventPair>();
    if (count == 0) {
      eventList.drainTo(eventBuffer);
    } else {
      eventList.drainTo(eventBuffer, count);
    }

    return formatMessage(eventBuffer);
  }

  private String formatMessage(List<EventPair> eventBuffer) {
    int bufferSize = eventBuffer.size();
    StringBuilder builder = new StringBuilder();

    builder.append(name).append("\n");
    for (int i = 0; i < bufferSize; i++) {
      builder.append(eventBuffer.get(i).getKey()).append(":")
        .append(eventBuffer.get(i).getValue()).append(" | ");
      if ((i + 1) % 10 == 0) {
        builder.append("\n");
      }
    }
    if (bufferSize % 10 != 0) {
      builder.append("\n");
    }

    builder.append("\ntotal: ").append(bufferSize);

    return builder.toString();
  }

  /**
   * Simple Event pair.
   */
  static final class EventPair {
    private final String key;
    private final String value;

    /**
     * Instantiates a new Event pair.
     *
     * @param key   the key
     * @param value the value
     */
    public EventPair(String key, String value) {
      this.key = key;
      this.value = value;
    }

    /**
     * Gets key.
     *
     * @return the key
     */
    public String getKey() {
      return key;
    }

    /**
     * Gets value.
     *
     * @return the value
     */
    public String getValue() {
      return value;
    }
  }
}
