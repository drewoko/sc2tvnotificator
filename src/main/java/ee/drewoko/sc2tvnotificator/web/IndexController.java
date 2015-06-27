package ee.drewoko.sc2tvnotificator.web;

import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;

/**
 * Created by Deniss Gubanov on 15/03/15.
 * Project: sc2tvnotificator
 * Package: ee.drewoko.sc2tvnotificator.web.Controllers
 */
@Controller
public class IndexController {

    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer() {
        return (container -> {
            ErrorPage error401Page = new ErrorPage(HttpStatus.UNAUTHORIZED, "/ERROR.html");
            ErrorPage error404Page = new ErrorPage(HttpStatus.NOT_FOUND, "/ERROR.html");
            ErrorPage error500Page = new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/ERROR.html");

            container.addErrorPages(error401Page, error404Page, error500Page);
        });
    }

}
