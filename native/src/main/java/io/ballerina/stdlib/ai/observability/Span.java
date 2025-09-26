/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.stdlib.ai.observability;

import com.arize.instrumentation.OITracer;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Span {

    private static final AtomicInteger NEXT_SPAN_HANDLE_ID = new AtomicInteger();

    protected final int id;
    protected final String name;
    protected io.opentelemetry.api.trace.Span span = null;
    private Scope scope = null;

    private Span(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Span(String name) {
        this(NEXT_SPAN_HANDLE_ID.getAndIncrement(), name);
    }

    @SuppressWarnings("unused")
    public String spanContextVar() {
        return "span_context_" + id;
    }

    @SuppressWarnings("unused")
    public String spanVar() {
        return "span_" + id;
    }

    public abstract void init(OITracer tracer);

    @SuppressWarnings("unused")
    public void enter() {
        scope = span.makeCurrent();
    }

    @SuppressWarnings("unused")
    public void setAttribute(String key, String value) {
        span.setAttribute(key, value);
    }

    @SuppressWarnings("unused")
    public void setJsonAttribute(String key, String value) {
        span.setAttribute(key, value);
    }

    @SuppressWarnings("unused")
    public void setAttribute(String key, int value) {
        span.setAttribute(key, value);
    }

    @SuppressWarnings("unused")
    public void setInput(String input) {
        span.setAttribute("input.value", input);
        span.setAttribute("input.mime_type", "text/plain");
    }

    @SuppressWarnings("unused")
    public void setOutput(String output) {
        span.setAttribute("output.value", output);
        span.setAttribute("output.mime_type", "text/plain");
    }

    @SuppressWarnings("unused")
    public void setStatus(Status status) {
        span.setStatus(switch (status) {
            case OK -> StatusCode.OK;
            case ERROR -> StatusCode.ERROR;
        });
    }

    @SuppressWarnings("unused")
    public void exit() {
        span.end();
        scope.close();
    }

    // https://arize-ai.github.io/openinference/spec/semantic_conventions.html#span-kinds
    public enum SpanKind {
        LLM,
        EMBEDDING,
        CHAIN,
        RETRIEVER,
        RERANKER,
        TOOL,
        AGENT,
        GUARDRAIL,
        EVALUATOR;

        @SuppressWarnings("unused")
        public String toString() {
            return switch (this) {
                case LLM -> "LLM";
                case EMBEDDING -> "EMBEDDING";
                case CHAIN -> "CHAIN";
                case RETRIEVER -> "RETRIEVER";
                case RERANKER -> "RERANKER";
                case TOOL -> "TOOL";
                case AGENT -> "AGENT";
                case GUARDRAIL -> "GUARDRAIL";
                case EVALUATOR -> "EVALUATOR";
            };
        }

        public static SpanKind from(String spanKind) {
            return switch (spanKind.toUpperCase(Locale.ROOT)) {
                case "LLM" -> LLM;
                case "EMBEDDING" -> EMBEDDING;
                case "CHAIN" -> CHAIN;
                case "RETRIEVER" -> RETRIEVER;
                case "RERANKER" -> RERANKER;
                case "TOOL" -> TOOL;
                case "AGENT" -> AGENT;
                case "GUARDRAIL" -> GUARDRAIL;
                case "EVALUATOR" -> EVALUATOR;
                default -> throw new IllegalArgumentException("Unknown span kind: " + spanKind);
            };
        }
    }

    public enum Status {
        OK,
        ERROR;

        @SuppressWarnings("unused")
        public String toString() {
            return switch (this) {
                case OK -> "OK";
                case ERROR -> "ERROR";
            };
        }

        public static Status from(String status) {
            return switch (status.toUpperCase(Locale.ROOT)) {
                case "OK" -> OK;
                case "ERROR" -> ERROR;
                default -> throw new IllegalArgumentException("Unknown status: " + status);
            };
        }
    }
}
