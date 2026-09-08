package ro.editii.scriptorium.rest;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import ro.editii.scriptorium.dto.TeiDivDto;
import ro.editii.scriptorium.dto.TeiElemDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Textbase rest client
 */
public class RestApiClient {

    private final RestTemplate rt;
    String baseurl;

    public RestApiClient(String baseurl) {
        this.baseurl = baseurl;
        final List<HttpMessageConverter<?>> converters = new ArrayList<>();
        converters.add(new JacksonJsonHttpMessageConverter());
        this.rt = new RestTemplate(converters);
    }

    public TeiDivDto get_div_id(long id) {
        final String apiDivUrl = this.getApiDivsUrl();
        final String url = String.format("%s/%d", apiDivUrl, id);
        final TeiDivDto dto = this.rt.getForObject(url, TeiDivDto.class);
        return dto;
    }

    public TeiElemDto get_div_path(String path) {
        final String apiDivUrl = this.getApiDivsUrl();
        final String url = String.format("%s?path=%s", apiDivUrl, path);
        final TeiElemDto dto = this.rt.getForObject(url, TeiElemDto.class);
        return dto;
    }

    private String getApiDivsUrl() {
        return String.format("%s/api/divs", this.baseurl);
    }

}
