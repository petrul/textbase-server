package ro.editii.scriptorium.rest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import ro.editii.scriptorium.Util;
import ro.editii.scriptorium.dao.AuthorRepository;
import ro.editii.scriptorium.service.DivService;

import java.util.Arrays;

@Log @RequiredArgsConstructor
@RestController
@CrossOrigin
@RequestMapping("/api")
public class RepoRestController {

    final AuthorRepository authorRepository;
    final DivService divService;
    final Environment environment;

    // sorry about this, cannot distinguish between the prod proxy and the dev ionic proxy
    private String devOrProd(String path, UriComponentsBuilder uriComponentsBuilder) {
        if (!path.startsWith("/"))
            path = "/" + path;
        if (Arrays.stream(this.environment.getActiveProfiles())
                .anyMatch(it -> it.equals("dev"))) { // sorry
            return  "http://localhost:8080" + path;
        } else
            return uriComponentsBuilder.path(path).build().toUriString();
    }

    String getResource(String strid, UriComponentsBuilder uriComponentsBuilder, HttpServletRequest httpServletRequest) {
        final String resName = strid;
        final ClassPathResource resource = new ClassPathResource("/static" + resName);
        if (resource.exists()) {
            UriComponentsBuilder ucb = Util.cloneUriComponentBuilder(uriComponentsBuilder, httpServletRequest);
            return this.devOrProd(resName, ucb);
        }
        return null;
    }

}
