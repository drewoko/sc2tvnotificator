package ee.drewoko.sc2tvnotificator.web;

import ee.drewoko.sc2tvnotificator.core.SessionRepository;
import org.apache.log4j.Logger;

import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;

/**
 * Created by Deniss Gubanov on 15/03/15.
 * Project: sc2tvnotificator
 * Package: ee.drewoko.sc2tvnotificator.web.Controllers
 */
@RestController
public class ListenController {

    private static final Logger logger = Logger.getLogger(ListenController.class);

    @Resource
    SessionRepository sessionRepository;

    @RequestMapping(value = "/set", method = RequestMethod.POST)
    public void setListen(
        @RequestBody SetRequest setRequest
    ) {

        logger.info("Tag set request: " + setRequest.getTags().toString());

        try {
            sessionRepository.setTagList(
                    setRequest.getSessionId(),
                    setRequest.getTags() == null ? new ArrayList<>() : setRequest.getTags()
            );
        } catch (NullPointerException ignored) {
        }

    }


}
