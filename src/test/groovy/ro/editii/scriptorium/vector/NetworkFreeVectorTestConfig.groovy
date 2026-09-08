package ro.editii.scriptorium.vector

import io.milvus.client.MilvusServiceClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

/**
 * Prevents unrelated Spring component/integration tests from opening a gRPC
 * channel or probing Milvus during ApplicationReadyEvent. An accidental
 * vector operation fails locally through an unstubbed mock rather than
 * silently reaching a developer's shared integration environment.
 */
@TestConfiguration
class NetworkFreeVectorTestConfig {

    @Bean(name = "milvusClient")
    @Primary
    MilvusServiceClient milvusClient() {
        return mock(MilvusServiceClient)
    }

    @Bean(name = "milvusService")
    @Primary
    MilvusService milvusService() {
        final service = mock(MilvusService)
        when(service.has(anyString())).thenReturn(false)
        return service
    }
}
