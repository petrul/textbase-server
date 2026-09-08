package ro.editii.scriptorium.web

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.PlatformTransactionManager
import ro.editii.scriptorium.RunStuffOnStartup
import ro.editii.scriptorium.VersionProperties
import ro.editii.scriptorium.cache.CacheConf
import ro.editii.scriptorium.cache.DiskCache
import ro.editii.scriptorium.cache.DiskCaches
import ro.editii.scriptorium.dao.AuthorRepository
import ro.editii.scriptorium.dao.RelocationRepository
import ro.editii.scriptorium.dao.TeiDivRepository
import ro.editii.scriptorium.dao.TeiFileRepository
import ro.editii.scriptorium.rest.GroovyShellConfig
import ro.editii.scriptorium.rest.GroovyShellRestController
import ro.editii.scriptorium.service.AdminService
import ro.editii.scriptorium.service.DivService
import ro.editii.scriptorium.tei.TeiRepo

import javax.sql.DataSource

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@TestPropertySource(properties = ["spring.main.allow-bean-definition-overriding=true"])
@WebMvcTest
@ContextConfiguration(classes = [MappingTestConfig.class, VersionProperties.class])
class MappingTest {

    @Autowired MockMvc mockMvc
    @MockitoBean GroovyShellConfig groovyShellConfig
    @MockitoBean GroovyShellRestController groovyShellRestController
    @MockitoBean AuthorRepository authorRepository
    @MockitoBean TeiFileRepository teiFileRepository
    @MockitoBean TeiDivRepository teiDivRepository
    @MockitoBean EntityManager entityManager
    @MockitoBean AdminService adminService
    @MockitoBean DataSource dataSource
    @MockitoBean TeiRepo teiRepo
    @MockitoBean RelocationRepository relocationRepository
    @MockitoBean @Qualifier(CacheConf.CACHE_TOC) DiskCache cacheToc
    @MockitoBean @Qualifier(CacheConf.CACHE_NODE) DiskCache cacheNode
    @MockitoBean @Qualifier(CacheConf.CACHE_BINARY_OBJECT) DiskCache cacheBinObj
    @MockitoBean PlatformTransactionManager platformTransactionManager
    @MockitoBean DiskCaches diskCaches
    @MockitoBean DivService divService
    @MockitoBean DivController divController

    @Test
    void testMapping() {

        // author page
        this.mockMvc
                .perform(get("/alecsandri"))
                .andExpect(MockMvcResultMatchers.handler().handlerType(DivController.class))
                .andExpect(MockMvcResultMatchers.handler().methodName("get_authorId"))

        // opus TOC page
        this.mockMvc
                .perform(get("/alecsandri/scrieri"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.handler().handlerType(DivController.class))
                .andExpect(MockMvcResultMatchers.handler().methodName("catchAllDivDispatcher"))

        // chapter page
        this.mockMvc
                .perform(get("/alecsandri/scrieri/poem"))
                .andExpect(MockMvcResultMatchers.handler().handlerType(DivController.class))
                .andExpect(MockMvcResultMatchers.handler().methodName("catchAllDivDispatcher"))

        // binary object (not page)
        this.mockMvc
                .perform(get("/alecsandri/scrieri/_binary/12334"))
                .andExpect(MockMvcResultMatchers.handler().handlerType(DivController.class))
                .andExpect(MockMvcResultMatchers.handler().methodName("catchAllDivDispatcher"));

        this.mockMvc
                .perform(get("/alecsandri/scrieri/some_volume/_binary/12334"))
                .andExpect(MockMvcResultMatchers.handler().handlerType(DivController.class))
                .andExpect(MockMvcResultMatchers.handler().methodName("catchAllDivDispatcher"));

        this.mockMvc
                .perform(get("/alecsandri/scrieri/some_volume/some_poem/_binary/12334"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.handler().handlerType(DivController.class))
                .andExpect(MockMvcResultMatchers.handler().methodName("catchAllDivDispatcher"));

        this.mockMvc
                .perform(get("/alecsandri/scrieri/some_volume/some_subvolume/some_poem/_binary/abcdef"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.handler().handlerType(DivController.class))
                .andExpect(MockMvcResultMatchers.handler().methodName("catchAllDivDispatcher"));

    }
}

@TestConfiguration
@Import(RunStuffOnStartup.class)
class MappingTestConfig {

    @Bean @Primary
    CommandLineRunner printJdbcUrlCLR(DataSource dataSource) {
        return (args) -> { /* NOOP */ }
    }

}