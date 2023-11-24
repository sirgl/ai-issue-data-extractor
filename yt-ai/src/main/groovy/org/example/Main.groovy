package org.example

import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import util.CSVExtractor
import data.Issue




static void main(String[] args) {
    def apiKey = args[0]
    def data = CSVExtractor.extract("data.csv")
    OpenAiService service = new OpenAiService(apiKey);
    ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
            .builder()
            .messages(prompt(new Issue("asdas", "asd",
                    "Rename refactoring doesn't work in external worksheets with Android plugin enabled",
                    "\"Create a Kotlin project via New Project Wizard. Open an external worksheet (or create one inside project and move to an excluded directory. E.g.: `build/`):\n\n#### Project structure\n![](image.png){width=70%}\n\n#### build/external.ws.kts\n```kotlin\nfun foo() = 1\n\nval bar = 2\n\nfoo()\nbar\n```\n\n#### Problem description\nCheck that Android plugin is enabled:\n\n![](image1.png){width=30%}\n\nTry renaming a function `foo`, then a value `bar`. Note that `foo` is renamed successfully but `bar`'s usage is not renamed:\n\n![](image2.png){width=30%}\n\nNow disable Android plugin and repeat. Everything is renamed correctly.\n\n#### Extra information\n\n>IntelliJ IDEA 2023.2 (Community Edition)\n>Build #IC-232.8660.185, built on July 26, 2023\n>Kotlin: 232-1.9.0-IJ8660.185\n\nSeems like `com.android.tools.idea.lang.proguardR8.ProguardR8UseScopeEnlarger` adds some files to the scope, making it something like `union(LocalSearchScope(worksheetFile), some_android_files)`.\nAnd after that `com.intellij.refactoring.rename.RenameUtil#processUsages` modifies the useScope:\n```java\nif (!(useScope instanceof LocalSearchScope)) {\n    useScope = searchScope.intersectWith(useScope);\n}\n```\"")))
            .maxTokens(256)
            .model("gpt-4-vision-preview")
            .build()
    service.createChatCompletion(chatCompletionRequest).choices
            .each { println(it) }
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

Response format:
```
{
  "subsystem": "<refactoring name from the list of options or None when none applicable or if the issue is not related to refactorings >",
  "android_plugin_involved": <true/false>
}
```
"""),
    ]
}