package ro.editii.scriptorium.rest

import groovy.util.logging.Log
import io.swagger.v3.oas.annotations.Hidden
import lombok.RequiredArgsConstructor
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.rest.core.annotation.RestResource
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import ro.editii.scriptorium.dao.AuthorRepository
import ro.editii.scriptorium.dao.TeiDivRepository
import ro.editii.scriptorium.dao.TeiFileRepository
import ro.editii.scriptorium.service.AdminService
import ro.editii.scriptorium.tei.TeiRepo

@Configuration
class GroovyShellConfig {

    @Bean
    GroovyShell groovyShell(
            ApplicationContext applicationContext,
            AdminService adminService,
            TeiRepo teiRepo,
            TeiDivRepository teiDivRepository,
            TeiFileRepository teiFileRepository,
            AuthorRepository authorRepository
    ) {
        final Binding binding = new Binding(
                'ctxt': applicationContext,
                'tei_repo': teiRepo,
                'div_repo': teiDivRepository,
                'file_repo': teiFileRepository,
                'auth_repo': authorRepository,
                'admin': adminService,
        )

        return new GroovyShell(binding)
    }

}

/**
 * this must be thoroughly protected, any command can be executed on it.
 * it should be admin-protected.
 *
 * this is quite dangerous, can execute arbitrary commands, will disable it
 * until introduction of security, login.
 */

//@Controller
@RequiredArgsConstructor
@Log
@Hidden
class GroovyShellRestController {

    final GroovyShell groovyShell

    @PostMapping(value = "/api/shell", consumes = ["text/plain"])
    @ResponseBody
    Object execute(@RequestBody final String script) {

        log.fine(String.format("will execute script [%s]", script))

        Object res = this.groovyShell.parse(script).run()

        log.fine(String.format("returning response of class %s : [%s]",
                res.class.canonicalName,
                res.toString()))

        res
    }

}
