package tika;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

public class TikaHW {

	List<String> keywords;
	PrintWriter logfile;
	int num_keywords, num_files, num_fileswithkeywords;
	Map<String,Integer> keyword_counts;
	Date timestamp;

	/**
	 * constructor
	 * DO NOT MODIFY
	 */
	public TikaHW() {
		keywords = new ArrayList<String>();
		num_keywords=0;
		num_files=0;
		num_fileswithkeywords=0;
		keyword_counts = new HashMap<String,Integer>();
		timestamp = new Date();
		try {
			logfile = new PrintWriter("log.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * destructor
	 * DO NOT MODIFY
	 */
	protected void finalize() throws Throwable {
		try {
			logfile.close();
	    } finally {
	        super.finalize();
	    }
	}

	/**
	 * main() function
	 * instantiate class and execute
	 * DO NOT MODIFY
	 */
	public static void main(String[] args) {
		TikaHW instance = new TikaHW();
		instance.run();
	}

	/**
	 * execute the program
	 * DO NOT MODIFY
	 */
	private void run() {

		// Open input file and read keywords
		try {
			BufferedReader keyword_reader = new BufferedReader(new FileReader("keywords.txt"));
			String str;
			while ((str = keyword_reader.readLine()) != null) {
				keywords.add(str);
				num_keywords++;
				keyword_counts.put(str, 0);
			}
			keyword_reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Open all pdf files, process each one
		File pdfdir = new File("./vault");
		File[] pdfs = pdfdir.listFiles(new PDFFilenameFilter());
		for (File pdf:pdfs) {
			num_files++;
			processfile(pdf);
		}

		// Print output file
		try {
			PrintWriter outfile = new PrintWriter("output.txt");
			outfile.print("Keyword(s) used: ");
			if (num_keywords>0) outfile.print(keywords.get(0));
			for (int i=1; i<num_keywords; i++) outfile.print(", "+keywords.get(i));
			outfile.println();
			outfile.println("No of files processed: " + num_files);
			outfile.println("No of files containing keyword(s): " + num_fileswithkeywords);
			outfile.println();
			outfile.println("No of occurrences of each keyword:");
			outfile.println("----------------------------------");
			for (int i=0; i<num_keywords; i++) {
				String keyword = keywords.get(i);
				outfile.println("\t"+keyword+": "+keyword_counts.get(keyword));
			}
			outfile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Process a single file
	 * 
	 * Here, you need to:
	 *  - use Tika to extract text contents from the file
	 *  - (optional) check OCR quality before proceeding
	 *  - search the extracted text for the given keywords
	 *  - update num_fileswithkeywords and keyword_counts as needed
	 *  - update log file as needed
	 * 
	 * @param f File to be processed
	 */
	private void processfile(File f) {

		/***** YOUR CODE GOES HERE *****/
		try
		{   
			int cnt=0,fcount = 0;
		    String str=null;
			FileInputStream ifStream= new FileInputStream(f);
			ContentHandler handler = new BodyContentHandler(100000000);
			Metadata mdata=new Metadata();
			PDFParser pdfpar=new PDFParser();
			pdfpar.parse(ifStream,handler,mdata,new ParseContext());
            String data=handler.toString();
            String folded_data=data.toLowerCase();             //Case Folding OF data
            String clean_data=folded_data.replaceAll("\\s+"," ");  //white spaces collapse
            for (String kword : keywords)                      //Iterating thru Keywords list
            {   cnt=0;
                str="\\b"+kword.toLowerCase()+"s?\\b";       //case folding of keywords
            	Pattern pattern=Pattern.compile(str);
            	Matcher matcher=pattern.matcher(clean_data);
            	while(matcher.find())
            	{   System.out.println("Found the text " +matcher.group()+" starting at " +matcher.start());
            		updatelog(kword,f.getName());
            		cnt++;
            		fcount++;
            	}
            	cnt+=keyword_counts.get(kword);
            	keyword_counts.put(kword,cnt);
            }
            if(fcount>0)
            num_fileswithkeywords++;   //increase the number of files with keywords
			
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
		
		
		
		// to update the log file with a search hit, use:
		// 	updatelog(keyword,f.getName());

	}

	/**
	 * Update the log file with search hit
	 * Appends a log entry with the system timestamp, keyword found, and filename of PDF file containing the keyword
	 * DO NOT MODIFY
	 */
	private void updatelog(String keyword, String filename) {
		timestamp.setTime(System.currentTimeMillis());
		logfile.println(timestamp + " -- \"" + keyword + "\" found in file \"" + filename +"\"");
		logfile.flush();
	}

	/**
	 * Filename filter that accepts only *.pdf
	 * DO NOT MODIFY 
	 */
	static class PDFFilenameFilter implements FilenameFilter {
		private Pattern p = Pattern.compile(".*\\.pdf",Pattern.CASE_INSENSITIVE);
		public boolean accept(File dir, String name) {
			Matcher m = p.matcher(name);
			return m.matches();
		}
	}
}