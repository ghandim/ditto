/*
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.ditto.services.connectivity.messaging.monitoring.logs;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.eclipse.ditto.model.base.exceptions.DittoRuntimeException;
import org.eclipse.ditto.model.connectivity.LogEntry;
import org.eclipse.ditto.services.connectivity.messaging.monitoring.ConnectionMonitor;
import org.eclipse.ditto.services.utils.akka.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link org.eclipse.ditto.services.connectivity.messaging.monitoring.logs.MuteableConnectionLogger}.
 * This implementation is not threadsafe since it ain't really of a big importance if a log message gets lost during activation of the logger.
 */
// TODO: should we also add the connection id to the logs?
@NotThreadSafe
final class DefaultMuteableConnectionLogger implements MuteableConnectionLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMuteableConnectionLogger.class);

    private final ConnectionLogger delegate;
    private boolean active;

    /**
     * Create a new mutable connection logger that is currently muted.
     * @param delegate the delegate to call while the logger is unmuted
     */
    DefaultMuteableConnectionLogger(final ConnectionLogger delegate) {
        this.delegate = delegate;
        this.active = false;
    }

    @Override
    public void mute() {
        LOGGER.debug("Muting the logger");
        active = false;
    }

    @Override
    public void unmute() {
        LOGGER.debug("Unmuting the logger");
        active = true;
    }

    @Override
    public boolean isMuted() {
        return !active;
    }

    @Override
    public void success(final ConnectionMonitor.InfoProvider infoProvider) {
        if (active) {
            delegate.success(infoProvider);
        } else {
            logTraceWithCorrelationId("Not logging success since logger is muted.", infoProvider);
        }
    }

    @Override
    public void success(final String correlationId, final Instant timestamp, final String message,
            @Nullable final String thingId) {
        if (active) {
            delegate.success(correlationId, timestamp, message, thingId);
        } else {
            logTraceWithCorrelationId("Not logging success since logger is muted.", correlationId);
        }
    }

    @Override
    public void failure(final ConnectionMonitor.InfoProvider infoProvider, @Nullable final DittoRuntimeException exception) {
        if (active) {
            delegate.failure(infoProvider, exception);
        } else {
            logTraceWithCorrelationId("Not logging failure since logger is muted.", infoProvider);
        }
    }

    @Override
    public void failure(final String correlationId, final Instant timestamp, final String message,
            @Nullable final String thingId) {
        if (active) {
            delegate.failure(correlationId, timestamp, message, thingId);
        } else {
            logTraceWithCorrelationId("Not logging failure since logger is muted.", correlationId);
        }
    }

    @Override
    public void exception(final ConnectionMonitor.InfoProvider infoProvider, @Nullable final Exception exception) {
        if (active) {
            delegate.exception(infoProvider, exception);
        } else {
            logTraceWithCorrelationId("Not logging exception since logger is muted.", infoProvider);
        }
    }

    @Override
    public void exception(final String correlationId, final Instant timestamp, final String message,
            @Nullable final String thingId) {
        if (active) {
            delegate.exception(correlationId, timestamp, message, thingId);
        } else {
            logTraceWithCorrelationId("Not logging exception since logger is muted.", correlationId);
        }
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Collection<LogEntry> getLogs() {
        if (active) {
            return delegate.getLogs();
        }
        LOGGER.trace("Returning empty logs since logger is muted.");
        return Collections.emptyList();
    }


    private void logTraceWithCorrelationId(final String message, final String correlationId, final Object... messageArguments) {
        if (LOGGER.isTraceEnabled()) {
            LogUtil.enhanceLogWithCorrelationId(correlationId);
            LOGGER.trace(message, messageArguments);
        }
    }

    private void logTraceWithCorrelationId(final String message, final ConnectionMonitor.InfoProvider infoProvider, final Object... messageArguments) {
        if (LOGGER.isTraceEnabled()) {
            LogUtil.enhanceLogWithCorrelationId(infoProvider.getCorrelationId());
            LOGGER.trace(message, messageArguments);
        }
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefaultMuteableConnectionLogger that = (DefaultMuteableConnectionLogger) o;
        return active == that.active &&
                Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate, active);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" +
                ", delegate=" + delegate +
                ", active=" + active +
                "]";
    }

}
