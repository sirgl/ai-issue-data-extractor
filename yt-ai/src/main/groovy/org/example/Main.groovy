package org.example

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingRegistry
import com.knuddels.jtokkit.api.ModelType
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.embedding.Embedding
import com.theokanning.openai.embedding.EmbeddingRequest
import com.theokanning.openai.embedding.EmbeddingResult
import com.theokanning.openai.service.OpenAiService
import groovy.json.JsonSlurper
import io.pinecone.PineconeClient
import io.pinecone.PineconeClientConfig
import io.pinecone.PineconeConnection
import io.pinecone.proto.UpsertRequest
import io.pinecone.proto.UpsertResponse
import io.pinecone.proto.Vector
import util.CSVExtractor
import data.Issue

import java.time.Duration


static void main(String[] args) {
    def apiKey = args[0]
    def pinecodeKey = args[1]
    def data = CSVExtractor.extract("data.csv").parallelStream().filter {countTokens(getTextForIssue(it)) < 8192 }.toList()
    OpenAiService openAiService = new OpenAiService(apiKey, Duration.ofSeconds(30))

    PineconeClientConfig config = new PineconeClientConfig()
            .withApiKey(pinecodeKey)
            .withEnvironment("us-east4-gcp")
            .withProjectName("463dbdd")
            .withServerSideTimeoutSec(10);


    PineconeClient client = new PineconeClient(config)
    def pineconeConnection = client.connect("hackathon-2023")


    indexIssues(data, openAiService, pineconeConnection)
}

private static void indexIssues(List<Issue> issues, OpenAiService openAiService, PineconeConnection pineconeConnection) {
    def rawIssues = issues.collect { getTextForIssue(it) }

    def embeddings = openAiService.createEmbeddings(new EmbeddingRequest("text-embedding-ada-002", rawIssues, "user"))

    def issuesWithEmbeddings = embeddings.data.withIndex().collect {
        def vectorOfFloats = it.v1.embedding.collect { it as Float }

        def index = it.v2
        def vector = Vector.newBuilder()
                .addAllValues(vectorOfFloats)
                .setId(issues[index].id())
                .build()
        new IssueWithEmbedding(embedding: vector, issue: issues[index])
    }

    def chunks = issuesWithEmbeddings.collate(30)
    for (final def chunk in chunks) {
        UpsertRequest hybridRequest = UpsertRequest.newBuilder()
                .setNamespace("")
                .addAllVectors(chunk.collect { it.embedding })
                .build()
        def upsertResponse = pineconeConnection.blockingStub.upsert(hybridRequest)
        println upsertResponse.toString()
    }


}

private static String getTextForIssue(Issue it) {
    "${it.summary()}\n${it.description()}" as String
}

class IssueWithEmbedding {
    Vector embedding
    Issue issue
}


static int countTokens(String text) {
    EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    Encoding encoding = registry.getEncodingForModel(ModelType.TEXT_EMBEDDING_ADA_002)
    List<Integer> encoded = encoding.encode(text);
    return encoded.size()
}