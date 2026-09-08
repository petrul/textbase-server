package ro.editii.scriptorium.search.content;

import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;


@RequiredArgsConstructor
public class UrlContentResolver implements ContentResolver {

    final RestTemplate restTemplate;

    @Override
    public String resolve(String id) {
        assert id != null;
        try {
            return restTemplate.getForObject(id + ".txt", String.class);
        } catch (Exception e) {
            return "could not resolve: " + id;
        }

    }
}
