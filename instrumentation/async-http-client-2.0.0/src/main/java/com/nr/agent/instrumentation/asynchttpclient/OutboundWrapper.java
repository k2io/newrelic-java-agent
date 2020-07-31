/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.asynchttpclient;

import java.util.Collections;

import org.asynchttpclient.Request;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;

/**
 * Wraps async http client's outbound request headers for CAT.
 */
public class OutboundWrapper implements OutboundHeaders {

    private final Request request;

    public OutboundWrapper(Request request) {
        this.request = request;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String name, String value) {
        request.getHeaders().set(name, Collections.singletonList(value));
    }
}
