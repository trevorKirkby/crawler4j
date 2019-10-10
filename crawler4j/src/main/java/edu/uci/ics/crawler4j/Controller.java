package edu.uci.ics.crawler4j;

import java.util.List;

/**
 * Controller
 * @author trevor
 *
 * Courtesy of the quickstart guide on github.
 */

import java.util.HashSet;
import java.util.regex.Pattern;

//From documentation at https://github.com/fusesource/leveldbjni
import org.iq80.leveldb.*;
import static org.fusesource.leveldbjni.JniDBFactory.*;
import java.io.*;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class Controller {
	
	private final static Pattern terms = Pattern.compile("(foolishness|deity|assassination)", Pattern.CASE_INSENSITIVE);
	
    public static void main(String[] args) throws Exception {
        String crawlStorageFolder = "./data/crawl/root";
        int numberOfCrawlers = 30;

        //From documentation at https://github.com/fusesource/leveldbjni
        Options options = new Options();
        options.createIfMissing(true);
        DB db = factory.open(new File("./data/crawldb"), options);
        try {
	        CrawlConfig config = new CrawlConfig();
	        //config.setPolitenessDelay(100);
	        //config.setMaxPagesToFetch(200);
	        config.setMaxDownloadSize(Integer.MAX_VALUE);
	        config.setResumableCrawling(true);
	        config.setUserAgentString("Westmont IR Trevor Kirkby Emily Peterson: Team minecraftvillagernoise");
	        config.setCrawlStorageFolder(crawlStorageFolder);
	        config.db = db;
	        
	        // Instantiate the controller for this crawl.
	        PageFetcher pageFetcher = new PageFetcher(config);
	        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
	        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
	        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);
	
	        // For each crawl, you need to add some seed urls. These are the first
	        // URLs that are fetched and then the crawler starts following links
	        // which are found in these pages
	        controller.addSeed("http://djp3.westmont.edu/gutenberg/index.php");
	    	
	    	// The factory which creates instances of crawlers.
	        CrawlController.WebCrawlerFactory<MyCrawler> factory = MyCrawler::new;
	        
	        //Credits to https://www.techiedelight.com/measure-elapsed-time-execution-time-java/ for how to measure elapsed time
	        long startTime = System.currentTimeMillis();
	        
	        // Start the crawl. This is a blocking operation, meaning that your code
	        // will reach the line after this only when crawling is finished.
	        controller.start(factory, numberOfCrawlers);
	        
	        long endTime = System.currentTimeMillis();
	        long timeElapsed = endTime - startTime;
			System.out.println("Execution time in milliseconds: " + timeElapsed);
			
			HashSet<String> soopybrickles = new HashSet<String>();
			HashSet<String> uniquepages = new HashSet<String>();
			List<Object> crawlersLocalData = controller.getCrawlersLocalData();
	        for (Object localData : crawlersLocalData) {
	            CrawlData stat = (CrawlData) localData;
	            soopybrickles.addAll(stat.soopybrickles);
	            uniquepages.addAll(stat.visited);
	     	}
	        System.out.println("Soopybrickles : " + soopybrickles);
	        System.out.println("Unique Pages : " + uniquepages.size());
	        
	       //From documentation at https://github.com/fusesource/leveldbjni
	        DBIterator iterator = db.iterator();
	        try {
	          int numwords = 0;
	          for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
	            String key = asString(iterator.peekNext().getKey());
	            String value = asString(iterator.peekNext().getValue());
	            if (terms.matcher(key).matches()) {
	            	System.out.println(key+" = "+value);
	            }
	            numwords ++;
	          }
	          System.out.println("Unique Words : " + numwords);
	        } finally {
	          // Make sure you close the iterator to avoid resource leaks.
	          iterator.close();
	        }
        } finally {
        	// Make sure you close the db to shutdown the database and avoid resource leaks.
        	db.close();
        }
    }
}
