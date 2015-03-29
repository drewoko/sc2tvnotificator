package ee.drewoko.sc2tvnotificator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by Deniss Gubanov on 15/03/15.
 * Project: sc2tvnotificator
 * Package: ee.drewoko.sc2tvnotificator
 */
@SpringBootApplication
@ComponentScan
public class Bootstrap {

    public static void main(String[] args) {
        SpringApplication.run(Bootstrap.class, args);
    }

}
