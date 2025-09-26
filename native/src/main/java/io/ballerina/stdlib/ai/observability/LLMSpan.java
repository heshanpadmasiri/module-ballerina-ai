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
import io.opentelemetry.api.trace.SpanBuilder;

public class LLMSpan extends Span {

    private final String modelName;
    private final String providerName;
    private int inputIndex = 0;
    private int outputIndex = 0;

    public LLMSpan(String name, String modelName, String providerName) {
        super(name);
        this.modelName = modelName;
        this.providerName = providerName;

    }

    @Override
    public void init(OITracer tracer) {
        SpanBuilder sb = tracer.spanBuilder(name).setSpanKind(io.opentelemetry.api.trace.SpanKind.CLIENT)
                .setAttribute(Attributes.SPAN_KIND.key(), SpanKind.LLM.toString())
                .setAttribute(Attributes.MODEL_NAME.key(), modelName)
                .setAttribute(Attributes.MODEL_PROVIDER.key(), providerName);
        this.span = sb.startSpan();
    }

    @Override
    public void setInput(String input) {
        super.setInput(input);
        span.setAttribute("llm.input_messages.%d.message.role".formatted(inputIndex), "user");
        span.setAttribute("llm.input_messages.%d.message.content".formatted(inputIndex), input);
        inputIndex++;
    }

    @SuppressWarnings("unused")
    public void addToolCallInputs(ToolRequest... toolCalls) {
        String prefix = "llm.input_messages.%d.message.tool_calls".formatted(inputIndex);
        for (int i = 0; i < toolCalls.length; i++) {
            String perCallPrefix = "%s.%d.tool_call".formatted(prefix, i);
            // TODO:
//            setJsonAttribute("%s.function.arguments".formatted(perCallPrefix), toolCalls[i].argumentJson);
            span.setAttribute("%s.function.name".formatted(perCallPrefix), toolCalls[i].name);
            span.setAttribute("%s.id".formatted(perCallPrefix), toolCalls[i].id);
        }
        span.setAttribute("llm.input_messages.%d.message.role".formatted(inputIndex), "assistant");
        inputIndex++;
    }

    @SuppressWarnings("unused")
    public void addToolCallResponse(ToolResponse toolResponse) {
        String prefix = "llm.input_messages.%d.message".formatted(inputIndex);
        span.setAttribute("%s.content".formatted(prefix), toolResponse.content);
        span.setAttribute("%s.name".formatted(prefix), toolResponse.name);
        span.setAttribute("%s.tool_call_id".formatted(prefix), toolResponse.id);
        span.setAttribute("%s.role".formatted(prefix), "tool");
        inputIndex++;
    }

    @SuppressWarnings("unused")
    public void addIntermediateResponse(String content) {
        String prefix = "llm.input_messages.%d.message".formatted(inputIndex);
        span.setAttribute("%s.content".formatted(prefix), content);
        span.setAttribute("%s.role".formatted(prefix), "assistant");
        inputIndex++;
    }

    @SuppressWarnings("unused")
    public void addIntermediateRequest(String content) {
        String prefix = "llm.input_messages.%d.message".formatted(inputIndex);
        span.setAttribute("%s.content".formatted(prefix), content);
        span.setAttribute("%s.role".formatted(prefix), "user");
        inputIndex++;
    }

    @Override
    public void setOutput(String output) {
        super.setOutput(output);
        span.setAttribute("llm.output_messages.%d.message.role".formatted(outputIndex), "assistant");
        span.setAttribute("llm.output_messages.%d.message.content".formatted(outputIndex), output);
        outputIndex++;
    }

    @SuppressWarnings("unused")
    public void setTokenCount(int total, int prompt, int completion) {
        span.setAttribute("llm.token_count.total", total);
        span.setAttribute("llm.token_count.prompt", prompt);
        span.setAttribute("llm.token_count.completion", completion);
    }

    public record ToolRequest(String name, String argumentJson, String id) {

    }

    public record ToolResponse(String name, String content, String id) {

    }
}
