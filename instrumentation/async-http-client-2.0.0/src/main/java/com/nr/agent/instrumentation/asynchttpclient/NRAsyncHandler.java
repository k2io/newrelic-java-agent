/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.asynchttpclient;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.HttpResponseHeaders;
import org.asynchttpclient.HttpResponseStatus;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

/**
 * Weaves org.asynchttpclient.AsyncHandler. We use the originalName because we need to reference the State enum, so we
 * need this weave class NOT to shadow the original class.
 */
@Weave(type = MatchType.Interface, originalName = "org.asynchttpclient.AsyncHandler")
public class NRAsyncHandler<T> {

    /**
     * Ning allows termination of response processing after reading only the status line. After much deliberation, we
     * decided to hold open the connection and read the headers <i>even though the customer requested it be closed</i>
     * so we can pick up CAT information if present. We see very little risk with this approach, though it slightly
     * changes the way the program behaves. We believe not reading the headers will result in support tickets regarding
     * missing CAT information, so we decided to read the headers against the delegate's wishes.
     */
    @NewField
    private final AtomicBoolean userAbortedOnStatusReceived = new AtomicBoolean(false);
    @NewField
    public Segment segment;
    @NewField
    public URI uri;
    @NewField
    private InboundWrapper inboundHeaders;

    public AsyncHandler.State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
        AsyncHandler.State userState = Weaver.callOriginal();
        if (userState == AsyncHandler.State.ABORT) {
            userAbortedOnStatusReceived.set(true);
            return AsyncHandler.State.CONTINUE;
        }
        return userState;
    }

    public void onThrowable(Throwable t) {
        if(segment != null) {
            segment.reportAsExternal(GenericParameters
                    .library("AsyncHttpClient")
                    .uri(uri)
                    .procedure("onThrowable")
                    .build());
            // This used to be segment.finish(t), but the agent doesn't report this throwable.
            segment.end();
            segment = null;
            uri = null;
        }
        Weaver.callOriginal();
    }

    public AsyncHandler.State onHeadersReceived(HttpResponseHeaders headers) throws Exception {
        if (!headers.isTrailling()) {
            if(segment != null) {
                inboundHeaders = new InboundWrapper(headers);
            }
            if (userAbortedOnStatusReceived.get()) {
                return AsyncHandler.State.ABORT;
            }
        }
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    public T onCompleted() throws Exception {
        if (segment != null) {
            // This keeps the transaction alive after "segment.end()" just in case there are any completion handlers
            segment.getTransaction().getToken().linkAndExpire();

            segment.reportAsExternal(HttpParameters
                    .library("AsyncHttpClient")
                    .uri(uri)
                    .procedure("onCompleted")
                    .inboundHeaders(inboundHeaders)
                    .build());
            //This used to be segment.finish(t), but the agent doesn't automatically report t.
            segment.end();
            segment = null;
            uri = null;
        }

        return Weaver.callOriginal();
    }
}
