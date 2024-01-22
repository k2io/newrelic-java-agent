/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.jetty.server;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.eclipse.jetty.util.Callback;

import java.nio.ByteBuffer;

@Weave(originalName = "org.eclipse.jetty.server.Response", type = MatchType.Interface)
public abstract class Response_Instrumentation {
    public void write(boolean last, ByteBuffer content, Callback callback) {
        if (!isCommitted()) {
            NewRelic.getAgent().getTransaction().addOutboundResponseHeaders();
        }
        Weaver.callOriginal();
    }

    public abstract boolean isCommitted();
}
