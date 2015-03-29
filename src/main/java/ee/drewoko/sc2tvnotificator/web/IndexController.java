package ee.drewoko.sc2tvnotificator.web;

import org.apache.log4j.Logger;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by Deniss Gubanov on 15/03/15.
 * Project: sc2tvnotificator
 * Package: ee.drewoko.sc2tvnotificator.web.Controllers
 */
@Controller
public class IndexController {

    private static final Logger logger = Logger.getLogger(IndexController.class);

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String indexPage() {
        logger.info("Index Page Hit");
        return "index";
    }

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
