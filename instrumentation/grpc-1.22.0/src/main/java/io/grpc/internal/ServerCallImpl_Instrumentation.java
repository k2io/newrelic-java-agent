/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.grpc.internal;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.grpc.CompressorRegistry;
import io.grpc.Context;
import io.grpc.DecompressorRegistry;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCallListener_Instrumentation;
import io.grpc.Status;
import io.perfmark.Tag;

@Weave(originalName = "io.grpc.internal.ServerCallImpl")
final class ServerCallImpl_Instrumentation<ReqT, RespT> {
    @NewField
    Token token;

    /**
     * We use the constructor to capture the token created in the dispatcher transaction, which is
     * available on the supplied stream variable. This is later used to assign the token
     * to the listener when the newServerStreamListener method is called.
     */
    ServerCallImpl_Instrumentation(ServerStream_Instrumentation stream, MethodDescriptor<ReqT, RespT> method,
            Metadata inboundHeaders, Context.CancellableContext context,
            DecompressorRegistry decompressorRegistry, CompressorRegistry compressorRegistry,
            CallTracer serverCallTracer, Tag tag) {
        this.token = stream.token;
    }

    @Trace(async = true)
    ServerStreamListener newServerStreamListener(ServerCallListener_Instrumentation listener) {
        listener.token = this.token;

        if (token != null) {
            token.link();
        }

        return Weaver.callOriginal();
    }


    @Trace(async = true)
    public void sendMessage(RespT message) {
        // This helps ensure that csec agent will have a transaction available on the thread
        if (token != null) {
            token.link();
        }

        Weaver.callOriginal();
    }

    @Trace(async = true)
    public void close(Status status, Metadata trailers) {
        token = null;
        Weaver.callOriginal();
    }
}
