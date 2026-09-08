package ro.editii.scriptorium.vector;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Import this into any test that needs Milvus/search behavior but should
 * not depend on a reachable Ollama/STS server - overrides the real,
 * @Primary qwen3EmbeddingEmbedder bean (see VectorConfig) with
 * FakeQwen3Embedder. Requires spring.main.allow-bean-definition-overriding=true
 * on the importing test, same as everywhere else TestConfig is used.
 */
@TestConfiguration
public class FakeEmbedderTestConfig {

    @Bean(name = "qwen3EmbeddingEmbedder")
    @Primary
    public Embedder qwen3EmbeddingEmbedder() {
        return new FakeQwen3Embedder();
    }

    // Neutralize VectorConfig's real @Primary BGE bean so the Qwen-shaped
    // fake remains compatible with these tests' existing collection names.
    @Bean(name = "bgeM3Embedder")
    public Embedder bgeM3Embedder() {
        return new FakeQwen3Embedder();
    }
}
