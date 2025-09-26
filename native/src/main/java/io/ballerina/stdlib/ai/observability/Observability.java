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
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class Observability {

    static OITracer tracer;
    private static SdkTracerProvider tracerProvider;
    private static BatchSpanProcessor spanProcessor;

    private Observability() {

    }

    public static void initTracing(String phoenixEndpoint, String projectName) {
        // 1. Create resource with project name and service name
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), projectName,
                        AttributeKey.stringKey("service.version"), "1.0.0",
                        // Phoenix/OpenInference expects this specific attribute for project name
                        AttributeKey.stringKey("project.name"), projectName,
                        // Additional project name variants for compatibility
                        AttributeKey.stringKey("openinference.project.name"), projectName,
                        AttributeKey.stringKey("arize.project.name"), projectName
                )));

        // 2. Create OTLP exporter
        OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(phoenixEndpoint)
                .setTimeout(Duration.ofSeconds(30))
                .build();

        // 3. Create batch span processor with shorter intervals for testing
        spanProcessor = BatchSpanProcessor.builder(exporter)
                .setMaxExportBatchSize(1) // Export immediately for testing
                .setScheduleDelay(Duration.ofMillis(500)) // Short delay for testing
                .build();

        // 4. Build TracerProvider with resource
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(spanProcessor)
                .setResource(resource)
                .build();

        // 5. Register as global
        OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();

        // 6. Get OpenTelemetry tracer
        Tracer otelTracer = GlobalOpenTelemetry.getTracer(projectName);

        // 7. Wrap in OpenInference tracer
        tracer = new OITracer(otelTracer);
    }

    public static AgentSpan createAgentSpan(String name) {
      AgentSpan agentSpan = new AgentSpan("agent");
      agentSpan.init(Observability.tracer);
      return agentSpan;
    }

    public static EmbeddingSpan createEmbeddingSpan(String name) {
      EmbeddingSpan embeddingSpan = new EmbeddingSpan(name);
      embeddingSpan.init(Observability.tracer);
      return embeddingSpan;
    }

    public static ToolSpan createToolSpan(String name) {
      ToolSpan toolSpan = new ToolSpan(name);
      toolSpan.init(Observability.tracer);
      return toolSpan;
    }

    public static LLMSpan createLLMSpan(String name, String modelName, String providerName) {
      LLMSpan llmSpan = new LLMSpan(name, modelName, providerName);
      llmSpan.init(Observability.tracer);
      return llmSpan;
    }

    public static void addToolCallRequests(LLMSpan llmSpan, LLMSpan.ToolRequest[] toolRequests) {
      llmSpan.addToolCallInputs(toolRequests);
    }

    public static void addToolCallResponse(LLMSpan llmSpan, LLMSpan.ToolResponse toolResponse) {
      llmSpan.addToolCallResponse(toolResponse);
    }

    public static void addIntermediateResponse(LLMSpan llmSpan, String content) {
      llmSpan.addIntermediateResponse(content);
    }

    public static void addIntermediateRequest(LLMSpan llmSpan, String content) {
      llmSpan.addIntermediateRequest(content);
    }

    public static LLMSpan.ToolRequest createToolRequest(String name, String argumentJson, String id) {
      return new LLMSpan.ToolRequest(name, argumentJson, id);
    }

    public static LLMSpan.ToolResponse createToolResponse(String name, String content, String id) {
      return new LLMSpan.ToolResponse(name, content, id);
    }

    public static void setTokenCount(LLMSpan llmSpan, long total, long prompt, long completion) {
      llmSpan.setTokenCount((int) total, (int) prompt, (int) completion);
    }

    public static void enterSpan(Span span) {
      span.enter();
    }

    public static void exitSpan(Span span) {
      span.exit();
    }

    public static void setInput(Span span, String input) {
      span.setInput(input);
    }

    public static void setOutput(Span span, String output) {
      span.setOutput(output);
    }

    public static void setStatus(Span span, String status) {
      span.setStatus(Span.Status.from(status));
    }

    public static void flush() {
        if (spanProcessor != null) {
            spanProcessor.forceFlush().join(10, TimeUnit.SECONDS);
        }
    }

    public static void shutdown() {
        if (tracerProvider != null) {
            tracerProvider.shutdown().join(10, TimeUnit.SECONDS);
        }
    }
}
