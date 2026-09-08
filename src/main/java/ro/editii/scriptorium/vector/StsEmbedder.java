package ro.editii.scriptorium.vector;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.web.client.RestTemplate;

@AllArgsConstructor @Getter @ToString
public class StsEmbedder implements Embedder {

    final String host;
    final int port;
    final String model;
    final int vectorDimension;
    final RestTemplate restTemplate;


    @Override
    public float[][] encode(String[] sentences) {
//        return this.stsClient.get_models_id_encode(this.modelName, sentences);
        final var url = getUrl(String.format("/api/models/%s/encode", model));
        return this.restTemplate.postForObject(url, sentences, float[][].class);
    }

    private String getUrl(String path) {
        if (!path.startsWith("/"))
            path = "/" + path;
        return String.format("http://%s:%d%s", this.host, this.port, path);
    }

    @Override
    public String modelName() {
        return this.model;
    }

    @Override
    public int vectorDimension() {
        return this.vectorDimension;
    }

    @Override
    public String describe() {
        return String.format("Sentence-Transformers (STS) encoder \"%s\" at %s:%d", this.model, this.host, this.port);
    }


}
