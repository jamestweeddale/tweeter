package com.tweeddale.jtweeter.contentstrategy;

import com.tweeddale.jtweeter.services.FortuneService;
import org.apache.logging.log4j.LogManager;
import twitter4j.StatusUpdate;

import java.util.logging.Logger;

/**
 * Created by James on 7/18/2015.
 */
public class FortuneStrategy implements ContentFetchStrategy{
    
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(FortuneStrategy.class);

    public StatusUpdate getTweetableStatus() {
        return new StatusUpdate(FortuneService.getFortune());
    }


}
