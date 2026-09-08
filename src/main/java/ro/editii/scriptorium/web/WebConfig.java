package ro.editii.scriptorium.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * maybe this will help me serve html by default unless
     * somebody actually specifies xml
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.TEXT_HTML);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // default page for ionic apps.
        registry.addViewController("/admin").setViewName("forward:/admin/index.html");
        registry.addViewController("/app").setViewName("forward:/app/index.html");
    }


    /**
     * hopefully work better with proxy -- not sure though
     * vroiam să te știm necăjit să nu ne mai blestemi.
     * nici tu n-ai vrut unirea cu basarabia
     */
//    @Bean
//    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
//        ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
//        FilterRegistrationBean<ForwardedHeaderFilter> registration = new FilterRegistrationBean<>(filter);
//        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
//        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
//        registration.setUrlPatterns(List.of("/**"));
//        return registration;
//    }

    /**
     * make spring boot give url control to angular routing
     * https://keepgrowing.in/java/springboot/make-spring-boot-surrender-routing-control-to-angular/
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // springfox swagger ui
//        registry.addResourceHandler("swagger-ui.html")
//                .addResourceLocations("classpath:/META-INF/resources/");
//
//        registry.addResourceHandler("/webjars/**")
//                .addResourceLocations("classpath:/META-INF/resources/webjars/");

        registry.addResourceHandler("/app/**")
                .addResourceLocations("classpath:/static/app/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        return requestedResource.exists() && requestedResource.isReadable() ? requestedResource
                                : new ClassPathResource("/static/app/index.html");
                    }
                });

        registry.addResourceHandler("/admin/**")
                .addResourceLocations("classpath:/static/admin/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        return requestedResource.exists() && requestedResource.isReadable() ? requestedResource
                                : new ClassPathResource("/static/admin/index.html");
                    }
                });

    }
}
