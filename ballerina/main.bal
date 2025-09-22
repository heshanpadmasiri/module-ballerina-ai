import ai.observability;

public function main() {
    observability:initTracing("http://localhost:6006/v1/traces", "test-from-ballerina");
    observability:AgentSpan agentSpan = observability:createAgentSpan("agent");
    agentSpan.enter();
    agentSpan.setInput("hi");
    {
        observability:LLMSpan llmSpan = observability:createLLMSpan("llm", "gpt-4o-mini", "openai");
        llmSpan.enter();
        llmSpan.setInput("hi llm");
        map<json> toolInput = {
            "arg1": "value1",
            "arg2": "value2"
        };
        observability:ToolCallRequest toolCallRequest = {
            name: "tool",
            id: "123",
            argumentJson: toolInput
        };
        llmSpan.addToolCallRequests(toolCallRequest);
        {
            observability:ToolSpan toolSpan = observability:createToolSpan("tool");
            toolSpan.enter();
            toolSpan.setInput("hi tool");
            toolSpan.setOutput("bye tool");
            toolSpan.setStatus(observability:OK);
            toolSpan.exit();
        }
        observability:ToolCallResponse toolCallResponse = {
            name: "tool",
            id: "123",
            content: "bye tool"
        };
        llmSpan.addToolCallResponse(toolCallResponse);
        llmSpan.addIntermediateRequest("what is the output of the tool?");
        llmSpan.addIntermediateResponse("the output of the tool is great");
        llmSpan.setOutput("final llm output");
        llmSpan.setTokenCount(100, 50, 50);
        llmSpan.setStatus(observability:OK);
        llmSpan.exit();
    }
    agentSpan.setOutput("bye agent");
    agentSpan.setStatus(observability:OK);
    agentSpan.exit();
}
