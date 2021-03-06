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

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.Metadata;
import com.google.common.flogger.backend.TemplateContext;

import java.util.logging.Level;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Default implementation of LogData.
 */
public class DefaultLogData implements LogData {
	private final Level level = Level.INFO;
	/** The timestamp of the log statement that this context is associated with. */
	private final long timestampNanos;

	private final String loggerName;

	/** Additional metadata for this log statement (added via fluent API methods). */
	private Metadata metadata = null;
	/** The log site information for this log statement (set immediately prior to post-processing). */
	private LogSite logSite = null;
	/** The template context if formatting is required (set only after post-processing). */
	private TemplateContext templateContext = null;
	/** The log arguments (set only after post-processing). */
	private Object[] args = null;

	public DefaultLogData(long timestampNanos, String loggerName){
		this.timestampNanos = timestampNanos;
		this.loggerName = loggerName;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public void setLogSite(LogSite logSite) {
		this.logSite = logSite;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	@Override
	public Level getLevel() {
		return level;
	}

	@Override
	public long getTimestampMicros() {
		return NANOSECONDS.toMicros(timestampNanos);
	}

	@Override
	public long getTimestampNanos() {
		return timestampNanos;
	}

	@Override
	public String getLoggerName() {
		return loggerName;
	}

	@Override
	public LogSite getLogSite() {
		if (logSite == null) {
			throw new IllegalStateException("cannot request log site information prior to postProcess()");
		}
		return logSite;
	}

	public void setTemplateContext(TemplateContext templateContext) {
		this.templateContext = templateContext;
	}

	@Override
	public Metadata getMetadata() {
		return metadata != null ? metadata : Metadata.empty();
	}

	@Override
	public boolean wasForced() {
		// Check explicit TRUE here because findValue() can return null (which would fail unboxing).
		return metadata != null && Boolean.TRUE.equals(metadata.findValue(LogContext.Key.WAS_FORCED));

	}

	@Override
	public TemplateContext getTemplateContext() {
		return templateContext;
	}

	@Override
	public Object[] getArguments() {
		if (templateContext == null) {
			throw new IllegalStateException("cannot get arguments unless a template context exists");
		}
		return args;
	}

	@Override
	public Object getLiteralArgument() {
		if (templateContext != null) {
			throw new IllegalStateException("cannot get literal argument if a template context exists");
		}
		return args[0];
	}
}