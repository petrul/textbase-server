package ro.editii.scriptorium.vector

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/** Guards the model/collection pairing used by production semantic search. */
@SpringBootTest(
        classes = [VectorConfig.class, MilvusService.class],
        properties = [
            "embedder.host=zmeu.local",
            "embedder.port=11434",
            "milvus.host=mini.local",
            "milvus.port=20112",
            "milvus.collection=int_tb_paras_bge_m3",
        ])
class VectorConfigWiringTest {

    @Autowired Embedder embedder
    @Autowired MilvusCollection collection

    @Test
    void semanticSearchUsesTheBgeM3CollectionPair() {
        assert embedder.modelName() == "BGE_M3"
        assert embedder.vectorDimension() == MilvusCollection.DIM_1024
        assert collection.name == "int_tb_paras_bge_m3"
    }
}
