/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.web.reactive;

import com.newrelic.agent.bridge.Token;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.spring.reactive.Util;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import reactor.core.publisher.Mono;

@Weave(type = MatchType.Interface, originalName = "org.springframework.web.reactive.function.server.ServerResponse")
public abstract class ServerResponse_Instrumentation {

    public Mono<Void> writeTo(ServerWebExchange exchange, ServerResponse.Context context) {
        final Token token = (Token) exchange.getAttribute(Util.NR_TOKEN);
        if (token != null) {
            final PathPattern pathPattern = exchange.getAttribute("org.springframework.web.reactive.function.server.RouterFunctions.matchingPattern");
            String txnName = exchange.getAttribute(Util.NR_TXN_NAME);
            if (pathPattern != null) {
                // If the pattern string provided by Spring is available we should use it
                txnName = pathPattern.getPatternString();
            }

            final String methodName = " (" + exchange.getRequest().getMethod() + ")";
            if (txnName != null && statusCode().value() != 404) {
                final String txnNameWithMethod = removeTrailingSlash(txnName) + methodName;
                token.getTransaction()
                        .setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true, "Spring", txnNameWithMethod);
            } else {
                token.getTransaction()
                        .setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, true, "Spring", "Unknown Route" + methodName);
            }
        }

        return Weaver.callOriginal();
    }

    private String removeTrailingSlash(String txnName) {
        if (txnName.endsWith("/")) {
            return txnName.substring(0, txnName.length() - 1);
        }
        return txnName;
    }

    public abstract HttpStatus statusCode();
}
