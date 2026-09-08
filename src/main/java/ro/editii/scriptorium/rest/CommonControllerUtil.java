package ro.editii.scriptorium.rest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.util.UriComponentsBuilder;
import ro.editii.scriptorium.Util;

import java.util.Arrays;

@RequiredArgsConstructor
public class CommonControllerUtil {

    final Environment environment;

    protected String devOrProd(String path, UriComponentsBuilder uriComponentsBuilder) {
        if (!path.startsWith("/"))
            path = "/" + path;
        if (Arrays.stream(this.environment.getActiveProfiles())
                .anyMatch(it -> it.equals("dev"))) { // sorry
            return  "http://localhost:8080" + path;
        } else
            return uriComponentsBuilder.path(path).build().toUriString();
    }

    protected String getAuthorImage(String strid, UriComponentsBuilder uriComponentsBuilder, HttpServletRequest httpServletRequest) {
        final String resName = String.format("/img/authors/%s.jpg", strid);
        return this.getResource(resName, uriComponentsBuilder, httpServletRequest);
    }

    protected String getResource(String strid, UriComponentsBuilder uriComponentsBuilder, HttpServletRequest httpServletRequest) {
        final String resName = strid;
        final ClassPathResource resource = new ClassPathResource("/static" + resName);
        if (resource.exists()) {
            UriComponentsBuilder ucb = Util.cloneUriComponentBuilder(uriComponentsBuilder, httpServletRequest);
            return this.devOrProd(resName, ucb);
        }
        return null;
    }

    String getAuthorThumb(String strid, UriComponentsBuilder uriComponentsBuilder, HttpServletRequest httpServletRequest) {
        final String resName = String.format("/img/authors/thumbs/200px/%s.webp", strid);
        return this.getResource(resName, uriComponentsBuilder, httpServletRequest);
    }
}
