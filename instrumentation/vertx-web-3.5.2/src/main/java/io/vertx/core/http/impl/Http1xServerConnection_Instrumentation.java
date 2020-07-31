/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.vertx.core.http.impl;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.vertx.instrumentation.VertxUtil;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

@Weave(originalName = "io.vertx.core.http.impl.Http1xServerConnection")
public class Http1xServerConnection_Instrumentation {

    private Handler<HttpServerRequest> requestHandler = Weaver.callOriginal();

    synchronized void requestHandlers(HttpHandlers handlers) {
        Weaver.callOriginal();

        // Wrap the request handler so we can start the transaction and start tracking child threads
        requestHandler = VertxUtil.wrapRequestHandler(requestHandler);
    }

}
