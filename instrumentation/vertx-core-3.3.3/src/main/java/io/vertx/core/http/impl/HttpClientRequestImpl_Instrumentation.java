/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.http.impl;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.OutboundWrapper;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;

import static com.nr.vertx.instrumentation.VertxCoreUtil.END;
import static com.nr.vertx.instrumentation.VertxCoreUtil.VERTX_CLIENT;

@Weave(originalName = "io.vertx.core.http.impl.HttpClientRequestImpl")
public abstract class HttpClientRequestImpl_Instrumentation extends HttpClientRequestBase_Instrumentation {

    public void end(Buffer chunk) {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            segment = NewRelic.getAgent().getTransaction().startSegment(VERTX_CLIENT, END);
            segment.addOutboundRequestHeaders(new OutboundWrapper(headers()));
        }
        Weaver.callOriginal();
    }

    public void end() {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            segment = NewRelic.getAgent().getTransaction().startSegment(VERTX_CLIENT, END);
            segment.addOutboundRequestHeaders(new OutboundWrapper(headers()));
        }
        Weaver.callOriginal();
    }

    public abstract MultiMap headers();
}
