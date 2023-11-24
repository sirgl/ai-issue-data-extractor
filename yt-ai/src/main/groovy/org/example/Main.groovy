package org.example

import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import groovy.json.JsonSlurper
import util.CSVExtractor
import data.Issue

import java.time.Duration


static void main(String[] args) {
    def apiKey = args[0]
    def data = CSVExtractor.extract("data.csv")
    OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(30))

    def sb = new StringBuilder()
    for (final def issue in data.take(10)) {
        try {
            def result = handleSingle(issue, service)
            sb.append("${result.issue.id()},${result.response.subsystem()},${result.response.androidPluginInvolved()}\n")
        } catch (def e) {
            e.printStackTrace()
        }
    }
    def file = new File("out.csv")
    file << sb.toString()
}

Result handleSingle(Issue issue, OpenAiService service) {
    ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
            .builder()
            .messages(prompt(issue))
            .maxTokens(256)
            .model("gpt-4-vision-preview")
            .build()
    def rawResponse = service.createChatCompletion(chatCompletionRequest).choices
            .collect { it.message.content }
            .join("")
    if (rawResponse.substring(0, 7) == "```json") {
        rawResponse = rawResponse[7..-4]
    }
    def response = new JsonSlurper().parseText(rawResponse) as Response
    return new Result(issue: issue, response: response)
}

static List<ChatMessage> prompt(Issue issue) {
    def systemPrompt = """
You are autonomous agent helping IntelliJ Support Engineer.
You don't have access to the internet or human.
You may only emit JSON in form specified in the prompt.

The list of available refactorings in Intellij idea:
```
Make Static
Migrate
Move and copy refactorings
Pull members up, push members down
Rename refactorings
Replace inheritance with delegation
Safe delete
Type migration
Use interface where possible
Introduction to refactoring
Remove middleman
Wrap return value
Convert Raw Types to Generics refactoring
Replace constructor with builder
Replace constructor with factory method
Change Signature for Java
Convert anonymous to inner
Convert to Instance Method
Encapsulate Fields
Extract constant
Extract field
Extract interface
Extract method
Extract superclass
Extract/Introduce variable
Extract Parameter
Extract into class refactorings
Extract trait in Scala
Replace Conditional Logic with Strategy Pattern
Find and replace code duplicates
Invert Boolean
```
"""

    return [
            new ChatMessage("system", systemPrompt),
            new ChatMessage("user", """
The summary of the issue:
```
${issue.summary()}
```
The issue is:
```
${issue.description()}
```

Response format (without ticks and markdown, raw json, must be immediately parseable):
{
  "subsystem": "<refactoring name from the list of options or None when none applicable or if the issue is not related to refactorings >",
  "androidPluginInvolved": <true/false>
}
"""),
    ]
}

record Response(String subsystem, boolean androidPluginInvolved) {}


class Result {
    Issue issue
    Response response
}