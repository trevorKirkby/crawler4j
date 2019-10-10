package edu.uci.ics.crawler4j;

import java.io.IOException;
import java.util.Arrays;

/**
 * MyCrawler
 * @author trevor
 *
 * Courtesy of the quickstart guide on github.
 */

import java.util.Set;
import java.util.regex.Pattern;
//import java.util.StringTokenizer;

//import org.iq80.leveldb.WriteBatch;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import static org.fusesource.leveldbjni.JniDBFactory.*;

public class MyCrawler extends WebCrawler {

    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpg|jpeg|png|tif|tiff|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf|rm|smil|wmv|swf|wma|zip|rar|gz))$", Pattern.CASE_INSENSITIVE);
    private long startVisit = 0;
    private long endVisit = 0;
    public CrawlData mydata = new CrawlData();
    
    /**
     * This function is called by controller to get the local data of this crawler when job is
     * finished
     * From github example for communicating with threads
     */
    @Override
    public Object getMyLocalData() {
        return mydata;
    }
    
    /*
     * Credits to https://stackoverflow.com/questions/767759/occurrences-of-substring-in-a-string
     */
    public static int count(String str, String target) {
    	int lastIndex = 0;
    	int count = 0;
    	while(lastIndex != -1){
    	    lastIndex = str.indexOf(target,lastIndex);
    	    if(lastIndex != -1){
    	        count ++;
    	        lastIndex += target.length();
    	    }
    	}
    	return count;
    }
    
    /**
     * This method receives two parameters. The first parameter is the page
     * in which we have discovered this new url and the second parameter is
     * the new url. You should implement this function to specify whether
     * the given url should be crawled or not (based on your crawling logic).
     * In this example, we are instructing the crawler to ignore urls that
     * have css, js, git, ... extensions and to only accept urls that start
     * with "https://www.ics.uci.edu/". In this case, we didn't need the
     * referringPage parameter to make the decision.
     */
     @Override
     public boolean shouldVisit(Page referringPage, WebURL url) {
         String href = url.getURL().toLowerCase();
         return !FILTERS.matcher(href).matches() && href.startsWith("http://djp3.westmont.edu/gutenberg");
     }

     /**
      * This function is called when a page is fetched and ready
      * to be processed by your program.
     * @throws IOException 
      */
     @Override
     public void visit(Page page) throws IOException {
    	 System.out.println(myId + " : opened page at : " + startVisit);
         String url = page.getWebURL().getURL();
         System.out.println(myId + " : URL : " + url);
         mydata.visited.add(url);

         if (page.getParseData() instanceof HtmlParseData) {
        	 startVisit = System.currentTimeMillis();
        	 System.out.println(myId + " : TOTAL LATENCY BETWEEN VISITS : " + (startVisit - endVisit));
             HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
             String text = htmlParseData.getText();
             String html = htmlParseData.getHtml();
             Set<WebURL> links = htmlParseData.getOutgoingUrls();
             //System.out.println(myId + " : parsed HTML at : " + System.currentTimeMillis());
             if (count(text, "banksoopy brickle") > 0) {
            	 mydata.soopybrickles.add(url);
             }
             //System.out.println(myId + " : counted brickles at : " + System.currentTimeMillis());
             
             //StringTokenizer words = new StringTokenizer(text);
             //text.replaceAll(mydata.stopwords, ""); //Batch removal of stop words.
             String[] words = text.trim().split("\\s+"); //Hopefully faster than using a tokenizer, though not as good at intelligently handling punctuation.
             //System.out.println(myId + " : split text at : " + System.currentTimeMillis());
             //while (words.hasMoreTokens()) {
             long getTime = 0;
             long setTime = 0;
             long procTime = 0;
             long iterTime = 0;
             //WriteBatch batch = myController.getConfig().db.createWriteBatch();
             //try {
	             long startIter = System.currentTimeMillis();
	             for (String word : words) {
	            	 if (mydata.stopwords.contains(word)) {continue;}
	            	 //String word = words.nextToken();
	            	 //if (!mydata.stopwords.contains(word)) {
	            	 String value;
	        		 try {
	        			 long startGet = System.currentTimeMillis();
	        			 String lookup = asString(myController.getConfig().db.get(bytes(word)));
	        			 getTime += System.currentTimeMillis() - startGet;
	        			 
	        			 long startProc = System.currentTimeMillis();
	        			 String[] values = lookup.split("`");
	        			 values = Arrays.copyOf(values, values.length + 1);
	        			 values[values.length-1] = url;
	        			 Integer counter = Integer.parseInt(values[0]);
	        			 counter += count(text, word);
	        			 values[0] = counter.toString();
	        			 value = String.join("`", values);
	        			 procTime += System.currentTimeMillis() - startProc;
	        		 } catch (Exception e) {
	        			 long startProc = System.currentTimeMillis();
	        			 Integer counter = count(text, word);
	        			 String[] values = {counter.toString(), url};
	            		 value = String.join("`", values);
	            		 procTime += System.currentTimeMillis() - startProc;
	        		 }
	        		 long startSet = System.currentTimeMillis();
	        		 myController.getConfig().db.put(bytes(word), bytes(value));
	        		 //batch.put(bytes(word), bytes(value));
	        		 setTime += System.currentTimeMillis() - startSet;
	             }
	             iterTime = System.currentTimeMillis() - startIter;

	             //long startSet = System.currentTimeMillis();
	             //myController.getConfig().db.write(batch);
	             //setTime += System.currentTimeMillis() - startSet;
	         //} finally {
	        	 // Make sure you close the batch to avoid resource leaks.
			//	batch.close();
			//	System.out.println("BATCH CLOSED SUCCESSFULLY");
	         //}
             System.out.println(myId + " : GET TIME : " + getTime);
             System.out.println(myId + " : SET TIME : " + setTime);
             System.out.println(myId + " : PROC TIME : " + procTime);
             System.out.println(myId + " : ITER TIME : " + iterTime);
             //System.exit(1);
             
             //System.out.println(myId + " : saved to database at : " + System.currentTimeMillis());

             System.out.println("Text length: " + text.length());
             System.out.println("Html length: " + html.length());
             System.out.println("Number of outgoing links: " + links.size());
         }
         System.out.println(myId + " : closed page : " + System.currentTimeMillis());
         endVisit = System.currentTimeMillis();
         System.out.println(myId + " : TOTAL VISIT TIME : " + (endVisit - startVisit));
    }
}
