/*
 * Copyright (C) 2012 The Flogger Authors.
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

import static com.google.common.flogger.util.Checks.checkNotNull;

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.LoggingException;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.logging.Level;

/**
 * Base class for the fluent logger API. This class is a factory for instances of a fluent logging
 * API, used to build log statements via method chaining.
 *
 * @param <API> the logging API provided by this logger.
 */
@CheckReturnValue
public abstract class AbstractMessageLogger<API extends LoggingApi<API>> extends AbstractLogger{

  /**
   * Constructs a new logger for the specified backend.
   *
   * @param backend the logger backend which ultimately writes the log statements out.
   */
  protected AbstractMessageLogger(LoggerBackend backend) {
    super(backend);
  }

  // ---- PUBLIC API ----

  /**
   * Returns a fluent logging API appropriate for the specified log level.
   * <p>
   * If a logger implementation determines that logging is definitely disabled at this point then
   * this method is expected to return a "no-op" implementation of that logging API, which will
   * result in all further calls made for the log statement to being silently ignored.
   * <p>
   * A simple implementation of this method in a concrete subclass might look like:
   * <pre>{@code
   * boolean isLoggable = isLoggable(level);
   * boolean isForced = Platform.shouldForceLogging(getName(), level, isLoggable);
   * return (isLoggable | isForced) ? new SubContext(level, isForced) : NO_OP;
   * }</pre>
   * where {@code NO_OP} is a singleton, no-op instance of the logging API whose methods do nothing
   * and just {@code return noOp()}.
   */
  public abstract API at(Level level);

  /** A convenience method for at({@link Level#SEVERE}). */
  public final API atSevere() {
    return at(Level.SEVERE);
  }

  /** A convenience method for at({@link Level#WARNING}). */
  public final API atWarning() {
    return at(Level.WARNING);
  }

  /** A convenience method for at({@link Level#INFO}). */
  public final API atInfo() {
    return at(Level.INFO);
  }

  /** A convenience method for at({@link Level#CONFIG}). */
  public final API atConfig() {
    return at(Level.CONFIG);
  }

  /** A convenience method for at({@link Level#FINE}). */
  public final API atFine() {
    return at(Level.FINE);
  }

  /** A convenience method for at({@link Level#FINER}). */
  public final API atFiner() {
    return at(Level.FINER);
  }

  /** A convenience method for at({@link Level#FINEST}). */
  public final API atFinest() {
    return at(Level.FINEST);
  }

  protected final boolean isLoggable(Level level) {
    return getBackend().isLoggable(level);
  }
}
