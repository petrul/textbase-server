package ro.editii.scriptorium.rest


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.client.RestTemplate
import ro.editii.scriptorium.TestConfig

import static ro.editii.scriptorium.GTestUtil.p

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = [TestConfig.class ])
class AdminRestControllerTest {

    @LocalServerPort
    int  localServerPort;

    @Autowired
    RestTemplate restTemplate;

//    @Test
    void version() {
        p this.restTemplate.getForEntity(url("/api/admin/version"), String.class)
    }

    String url(String path) {
        return "http://localhost:${this.localServerPort}${path}"
    }
}