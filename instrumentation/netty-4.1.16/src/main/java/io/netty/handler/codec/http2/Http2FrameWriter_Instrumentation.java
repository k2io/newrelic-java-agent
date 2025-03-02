/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.netty.handler.codec.http2;

import com.agent.instrumentation.netty4116.NettyUtil;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext_Instrumentation;
import io.netty.channel.ChannelPromise;

@Weave(type = MatchType.Interface, originalName = "io.netty.handler.codec.http2.Http2FrameWriter")
public class Http2FrameWriter_Instrumentation {

    // Process HTTP/2 response headers and end txn
    public ChannelFuture writeHeaders(ChannelHandlerContext_Instrumentation ctx, int streamId, Http2Headers headers, int padding, boolean endStream,
            ChannelPromise promise) {

        boolean expired = NettyUtil.processResponse(headers, ctx.pipeline().token);
        if (expired) {
            ctx.pipeline().token = null;
        }

        return Weaver.callOriginal();
    }

    // Process HTTP/2 response headers and end txn
    public ChannelFuture writeHeaders(ChannelHandlerContext_Instrumentation ctx, int streamId, Http2Headers headers, int streamDependency, short weight,
            boolean exclusive, int padding, boolean endStream, ChannelPromise promise) {

        boolean expired = NettyUtil.processResponse(headers, ctx.pipeline().token);
        if (expired) {
            ctx.pipeline().token = null;
        }

        return Weaver.callOriginal();
    }
}
