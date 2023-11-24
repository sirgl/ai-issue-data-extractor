package org.example

import com.theokanning.openai.OpenAiService
import com.theokanning.openai.completion.CompletionRequest


static void main(String[] args) {
    def apiKey = args[0]
    OpenAiService service = new OpenAiService(apiKey);
    CompletionRequest completionRequest = CompletionRequest.builder()
            .prompt("Somebody once told me the world is gonna roll me")
//            .model("ada")
            .echo(true)
            .build();
    service.createCompletion("gpt-4", completionRequest).getChoices().forEach(System.out::println);
}

