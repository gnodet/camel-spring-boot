# Camel LangChain4j Spring Boot Starter

This Spring Boot starter provides auto-configuration for the Camel LangChain4j Chat component.

## Features

- Auto-configuration for LangChain4j ChatLanguageModel and StreamingChatLanguageModel beans
- Property-based configuration under `camel.component.langchain4j.*` prefix
- Bean reference support using `#beanName` syntax
- Compatible with Java 17 and Spring Boot 3.x

## Maven Dependency

```xml
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-langchain4j-spring-boot</artifactId>
    <version>${camel-version}</version>
</dependency>
```

## Configuration Properties

All properties are under the `camel.component.langchain4j` prefix:

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | Boolean | true | Enable/disable auto-configuration |
| `chat-model` | ChatLanguageModel | - | Chat Language Model bean reference |
| `streaming-chat-model` | StreamingChatLanguageModel | - | Streaming Chat Language Model bean reference |
| `lazy-start-producer` | Boolean | false | Start producer lazily |
| `autowired-enabled` | Boolean | true | Enable autowiring |

## Usage Example

### application.properties

```properties
# Enable the component
camel.component.langchain4j.enabled=true

# Reference a ChatLanguageModel bean
camel.component.langchain4j.chat-model=#myChatModel

# Reference a StreamingChatLanguageModel bean
camel.component.langchain4j.streaming-chat-model=#myStreamingChatModel
```

### Java Configuration

```java
@Configuration
public class LangChain4jConfig {
    
    @Bean
    public ChatLanguageModel myChatModel() {
        return OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4")
            .build();
    }
    
    @Bean
    public StreamingChatLanguageModel myStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4")
            .build();
    }
}
```

### Camel Route

```java
@Component
public class MyRoute extends RouteBuilder {
    @Override
    public void configure() {
        from("direct:chat")
            .to("langchain-chat:myChat");
    }
}
```

## Building

To build only this module:

```bash
./mvnw clean install -pl :camel-langchain4j-spring-boot -am
```

## License

Apache License 2.0

