/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 *
 * @author nityata
 */
@WebServlet(urlPatterns = {"/searchServletNew"})
public class searchServletNew extends HttpServlet {

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    static boolean create = true;
    static String indexPath = "index";
    static String docsPath = "trial.txt";
    static IndexWriter writer = null;
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Serach Results</title>");            
            out.println("</head>");
            out.println("<body>");
            //out.println("<h1>Servlet searchServletNew at " + request.getContextPath() + "</h1>");
            out.println("<h1>You are searching for:"+request.getParameter("searchString")+"</h1>");
            Long start = System.currentTimeMillis();
            prepareIndex(response,request.getParameter("searchString"));
            Long end = System.currentTimeMillis();
            Long elapsedTime = end - start;
            out.println("Time taken : " + elapsedTime/1000 + " seconds" );
            out.println("</body>");
            out.println("</html>");
        } 
        catch(Exception e){
            out.println(e.getMessage());
        }
        
        //finally {            
          //  out.close();
        //}
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
         
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
    void prepareIndex(HttpServletResponse response, String query) throws Exception
  {
                   PrintWriter out = response.getWriter();
                   ServletContext context = getServletContext();
                   //out.println(context.getRealPath("/"));
                   //return;
		   final File docDir = new File(context.getRealPath("/")+docsPath);
		   if (!docDir.exists() || !docDir.canRead()) {
		     out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
		     return;
		   }
                   //return;
		   
		   
		   try {
		     // out.println("Indexing to directory '" + indexPath + "'...");
		
		     Directory dir = FSDirectory.open(new File(indexPath));
		     Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
		     IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_40, analyzer);
		
		     if (create) {
		       // Create a new index in the directory, removing any
		       // previously indexed documents:
		       iwc.setOpenMode(OpenMode.CREATE);
		     } else {
		       // Add new documents to an existing index:
		       iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		     }
		
		     // Optional: for better indexing performance, if you
		     // are indexing many documents, increase the RAM
		     // buffer.  But if you do this, increase the max heap
		     // size to the JVM (eg add -Xmx512m or -Xmx1g):
		     //
		     // iwc.setRAMBufferSizeMB(256.0);
		
		     writer = new IndexWriter(dir, iwc);
		     //indexDocs(writer, docDir);
		     indexFile(response);

		     //System.out.println("Here");
		     writer.close();
		     
		     
		     searchIndex(dir,response,query);
		     
		 		   }catch (IOException e) {
		     out.println(" caught a " + e.getClass() +
		      "\n with message: " + e.getMessage());
		   }
		
	}
    static void searchIndex(Directory ind,HttpServletResponse response,String searchQuery) throws Exception
	{
            PrintWriter out = response.getWriter();
            String field = "contents";
	    String queries = null;
	    int repeat = 0;
	    boolean raw = false; // turn true for document scoring based on query.
	    String queryString = null;
	    int hitsPerPage = 10;
		
		
		
            IndexReader reader = DirectoryReader.open(ind);
	    IndexSearcher searcher = new IndexSearcher(reader);
	    Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
	    QueryParser parser = new QueryParser(Version.LUCENE_40, "contents", analyzer);
	    String line = searchQuery;

	      if (line == null || line.length() == -1) {
	       return;
	      }

	      line = line.trim();
	      if (line.length() == 0) {
	        return;
	      }
	      
	      Query query = parser.parse(line);
              //out.println("Searching for: " + query.toString(field));
	            
	      if (repeat > 0) {                           // repeat & time as benchmark
	        Date start = new Date();
	        for (int i = 0; i < repeat; i++) {
	          searcher.search(query, null, 100);
	        }
	        Date end = new Date();
	        out.println("Time: "+(end.getTime()-start.getTime())+"ms");
	      }

	      doPagingSearch(response, searcher, query, hitsPerPage, raw, queries == null && queryString == null);

	      if (queryString != null) {
	        return;
	      }
	    
	    reader.close();
		
		
		
	}
    public static void doPagingSearch(HttpServletResponse response,IndexSearcher searcher, Query query, 
           int hitsPerPage, boolean raw, boolean interactive) throws IOException {

//Collect enough docs to show 5 pages
                PrintWriter out = response.getWriter();
		TopDocs results = searcher.search(query, 5 * hitsPerPage);
		ScoreDoc[] hits = results.scoreDocs;
		
		int numTotalHits = results.totalHits;
		out.println(numTotalHits + " total matching documents"+"<br/>");
		
		int start = 0;
		int end = Math.min(numTotalHits, hitsPerPage);
		if (end > hits.length) {
		out.println("Only results 1 - " + hits.length +" of " + numTotalHits + " total matching documents collected.");
		}
		hits = searcher.search(query, numTotalHits).scoreDocs;
		
		
		end = Math.min(hits.length, start + hitsPerPage);
		
		for (int i = start; i < end; i++) {
		if (raw) {                              // output raw format
		out.println("doc="+hits[i].doc+" score="+hits[i].score);
		continue;
		}
		
		Document doc = searcher.doc(hits[i].doc);
		String path = doc.get("isbn");
		if (path != null) {
		out.println((i+1) + ". " + "<a target=\"_blank\" href="+path+">"+path+"</a>");
		String title = doc.get("contents");
		if (title != null) {
		out.println("   Contents: " + doc.get("contents"));
                out.println("<br/>");
		}
		} else {
		out.println((i+1) + ". " + "No path for this document");
                out.println("<br/>");
		}
		out.println("<br/>");
		}
   }
    static void indexDocs(File file,String u,HttpServletResponse response)
		    throws IOException {
                     PrintWriter out = response.getWriter();
		    // do not try to index files that cannot be read
		    if (file.canRead()) {
		      if (file.isDirectory()) {
		        String[] files = file.list();
		        // an IO error could occur
		        if (files != null) {
		          for (int i = 0; i < files.length; i++) {
		            indexDocs(new File(file, files[i]),u,response);
		          }
		        }
		      } else {

		        FileInputStream fis;
		        try {
		          fis = new FileInputStream(file);
		        } catch (FileNotFoundException fnfe) {
		          // at least on windows, some temporary files raise this exception with an "access denied" message
		          // checking if the file can be read doesn't help
		          return;
		        }

		        try {

		          // make a new, empty document
		          Document doc = new Document();

		          // Add the path of the file as a field named "path".  Use a
		          // field that is indexed (i.e. searchable), but don't tokenize 
		          // the field into separate words and don't index term frequency
		          // or positional information:
		          Field pathField = new StringField("isbn", u, Field.Store.YES);
		          doc.add(pathField);

		          // Add the last modified date of the file a field named "modified".
		          // Use a LongField that is indexed (i.e. efficiently filterable with
		          // NumericRangeFilter).  This indexes to milli-second resolution, which
		          // is often too fine.  You could instead create a number based on
		          // year/month/day/hour/minutes/seconds, down the resolution you require.
		          // For example the long value 2011021714 would mean
		          // February 17, 2011, 2-3 PM.
		          doc.add(new LongField("modified", file.lastModified(), Field.Store.NO));

		          // Add the contents of the file to a field named "contents".  Specify a Reader,
		          // so that the text of the file is tokenized and indexed, but not stored.
		          // Note that FileReader expects the file to be in UTF-8 encoding.
		          // If that's not the case searching for special characters will fail.
		          Field f = new TextField("contents", new BufferedReader(new InputStreamReader(fis, "UTF-8")),Field.Store.NO);
		          doc.add(f);

		          if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
		            // New index, so we just add the document (no old document can be there):
		            //out.println("adding " + file);
		            writer.addDocument(doc);
		          } else {
		            // Existing index (an old copy of this document may have been indexed) so 
		            // we use updateDocument instead to replace the old one matching the exact 
		            // path, if present:
		            out.println("updating " + file);
		            writer.updateDocument(new Term("path", file.getPath()), doc);
		          }
		          
		        } finally {
		          fis.close();
		        }
		      }
		    }
		  }
                  public void indexFile(HttpServletResponse response) throws IOException
		{
                        PrintWriter out = response.getWriter();
			BufferedReader br = null;
			try
			{
                                ServletContext context = getServletContext();
				br = new BufferedReader(new FileReader(new File(context.getRealPath("/")+"trial.txt")));
				String S= " ";
				do{				
					S=br.readLine();
					//System.out.println("Line read : " + S);
					Matcher m1=Pattern.compile("(.*)(url:)(.*)(\\$\\$content:)(.*)").matcher(S);
					int len = 0;
					while(m1.find())
					{
							String S1=m1.group(5).toString();
							if(!S1.trim().equals(""))
							{
								File f = new File(context.getRealPath("/")+"content1.txt");
								org.apache.commons.io.FileUtils.writeStringToFile(f, S1);
								//out.println("URI : " + m1.group(3).toString());
								indexDocs(f,m1.group(3).toString(),response);
							}
					}
				}while(S.length()!=0);
			}
			catch(Exception ex)
			{
				ex.getMessage();
			}
			finally
			{
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
}
