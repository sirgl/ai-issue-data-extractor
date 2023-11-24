package org.example

import com.theokanning.openai.completion.CompletionRequest
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import util.CSVExtractor


static void main(String[] args) {
    def apiKey = args[0]
    def data = CSVExtractor.extract("data.csv")
    OpenAiService service = new OpenAiService(apiKey);
    ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
            .builder()
            .messages([new ChatMessage("system", "")])
            .maxTokens(256)
            .model("gpt-3.5-turbo-0613")
            .build()
    service.createChatCompletion(chatCompletionRequest).choices
            .each { println(it) }
}

