package com.tweeddale.tweeter.contentstrategy;

import com.tweeddale.tweeter.services.DictionaryService;
import com.tweeddale.tweeter.services.ImageSearchService;
import com.tweeddale.tweeter.util.ConfigWrapper;
import com.tweeddale.tweeter.util.RemoteFileGrabber;
import org.apache.logging.log4j.LogManager;
import twitter4j.StatusUpdate;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by James on 7/4/2015.
 *
 * Uses injected dictionary and imageSearch service to fetch random words and query for a related image. The first accessible
 * related image is downloaded and attached to a StatusMessage along with the random words.
 *
 */
public class RandomWordsImageStrategy implements ContentFetchStrategy {

    protected static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(RandomWordsImageStrategy.class);

    private DictionaryService dictionaryService;
    private ImageSearchService imageSearchService;
    private final int MAX_NUM_SEARCH_RETRIES = 3;
    private final int MAX_NUM_IMAGE_FETCH_RETRIES = 10;

    public RandomWordsImageStrategy(DictionaryService dictionaryService, ImageSearchService imageSearchService, int numWords, boolean useWordOfTheDay) {
        this.dictionaryService = dictionaryService;
        this.imageSearchService = imageSearchService;
        this.numWords = numWords;
        this.useWordOfTheDay = useWordOfTheDay;
    }

    private int numWords = 0;
    private boolean useWordOfTheDay = false;

    //begin constructors
    public RandomWordsImageStrategy(){
    }

    public RandomWordsImageStrategy(int numWords){
        this();
        this.numWords = numWords;
    }

    //begin getters and setters
    public void setNumWords(int numWords) {
        this.numWords = numWords;
    }

    public int getNumWords() {
        return numWords;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    public void setImageSearchService(ImageSearchService imageSearchService) {
        this.imageSearchService = imageSearchService;
    }

    public boolean isUseWordOfTheDay() {
        return useWordOfTheDay;
    }

    public void setUseWordOfTheDay(boolean useWordOfTheDay) {
        this.useWordOfTheDay = useWordOfTheDay;
    }


    /**
     * Fetches random words and word of the day if applicable. Queries for and downloads related image then attaches it to
     * status update.
     *
     * @return  StatusUpdate composed of three random words with an attached related image ready for tweeting
     */
    public StatusUpdate getTweetableStatus() {

        String wordOfTheDay = "";
        String randomWords = "";
        String tweetableString = "";
        StatusUpdate status = null;
        RemoteFileGrabber remoteFileGrabber= new RemoteFileGrabber();
        File newFile = null;

        try {

            //word of the day, if turned on
            if(useWordOfTheDay){
                wordOfTheDay = dictionaryService.getWordOfTheDay();
                logger.debug("Applied word of the day: "+ wordOfTheDay);
            }

            //get random words and search for related image until we get one
            List<String> imgUrls = new ArrayList<String>();
            int numTries = 0;
            while(imgUrls.isEmpty() && (numTries < MAX_NUM_SEARCH_RETRIES)){
                randomWords =  this.fetchRandomWords();
                imgUrls = imageSearchService.search(randomWords);
                numTries++;
            }

            if(imgUrls != null && imgUrls.size() > 0) {
                //pick a random image from the results result and try to download it.
                //Try to download images until one works
                int numResults = imgUrls.size();
                int i = 0;
                int selectedResultIdx = 0;

                while (newFile == null && (i < MAX_NUM_IMAGE_FETCH_RETRIES)) {
                    try {
                        selectedResultIdx = new Random().nextInt(numResults);
                        newFile = remoteFileGrabber.getFile(new URL(imgUrls.get(selectedResultIdx)));
                    } catch (Exception e) {
                        logger.warn("Exception occurred while fetching image. Retrying.");
                    }

                    i++;
                }
            }else{
                logger.warn("Using default no-results-image");
                newFile = new File(ConfigWrapper.getConfig().getString("no-results-image"));
            }

            tweetableString = wordOfTheDay + " " + randomWords;

            //create Tweet message/status
            status = new StatusUpdate(tweetableString);
            status.setMedia(newFile);

        }catch(Exception e){
            e.printStackTrace();
        }

        return status;
    }


    /**
     * Fetches the set number of random words using the dictionary service. If word of the day is activated then it
     * substitutes it for one of the random words. Stores all fetched words in a space-separated string.
     *
     * @return String containing words fetched from dictionary service, space-separated
     */
    private String fetchRandomWords(){
        String wordString = "";
        int numWordsToFetch = this.numWords;

        if(this.numWords == 0)
            numWordsToFetch = ConfigWrapper.getConfig().getInt("num-words");

        if(this.useWordOfTheDay)  numWordsToFetch--;

        try {
            List<String> words = dictionaryService.getRandomWords(numWordsToFetch);

            for (String word : words) {
                wordString += word + " ";
            }

            wordString = wordString.trim();

        }catch (Exception e){
            e.printStackTrace();
        }

        return wordString;
    }
}
