package edu.sjsu.cmpe.library;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import java.net.URL;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.fusesource.stomp.jms.message.StompJmsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.views.ViewBundle;

import edu.sjsu.cmpe.library.api.resources.BookResource;
import edu.sjsu.cmpe.library.api.resources.RootResource;
import edu.sjsu.cmpe.library.config.LibraryServiceConfiguration;
import edu.sjsu.cmpe.library.repository.BookRepository;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;
import edu.sjsu.cmpe.library.ui.resources.HomeResource;
import edu.sjsu.cmpe.library.domain.Book;
import edu.sjsu.cmpe.library.dto.BookDto;
import edu.sjsu.cmpe.library.dto.BooksDto;
import edu.sjsu.cmpe.library.dto.LinkDto;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class LibraryService extends Service<LibraryServiceConfiguration> {

    private final Logger log = LoggerFactory.getLogger(getClass()	);
    private static String qName;
    private static String tName;
    private static String userName;
    private static String passwordName;
    private static String hostName;
    private static String portName;
    
    public static BookRepositoryInterface bookRepository;
    
    public static void main(String[] args) throws Exception {
	new LibraryService().run(args);
	int numThreads = 1;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    
    Runnable backgroundTask = new Runnable() {
    	 
	    @Override
	    public void run() {
	    	try{
	       	
	    	String user =  LibraryService.userName;//"admin";
	    	String password =  LibraryService.passwordName;//"password";
	    	String host =  LibraryService.hostName;//"54.215.210.214";
	    	int port = Integer.parseInt(LibraryService.portName);
	    	String destination = tName;

	    	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
	    	factory.setBrokerURI("tcp://" + host + ":" + port);
	        
	    	Connection connection = factory.createConnection(user, password);
	    	connection.start();
	    		
	    	Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	    	
	    	Destination dest = new StompJmsDestination(destination);        

	    	MessageConsumer consumer = session.createConsumer(dest);	    	
	    	
	    	System.currentTimeMillis();
	    	System.out.println("Waiting for messages...");
	    	
	    	while(true) {
	    	    Message msg = consumer.receive();	    	    
	    	    if( msg instanceof  TextMessage ) {
	    		String body = ((TextMessage) msg).getText();
	    		
	    		if( "SHUTDOWN".equals(body)) {
	    			System.out.println("Exiting of while loop");
	    		    break;
	    		}
	    		System.out.println("Received message = " + body);

	    		//Check if hashmap has the book
	    	    
	    	    String[] token = body.split("\"");
	    	    Long isbn = Long.parseLong(token[0].replaceAll(":",""));
	    	    Book book =  bookRepository.getBookByISBN(isbn);	    	     	    
	    	   
	    	    if(book==null){
	    	    	System.out.println("Hashmap does not contain this book");
	    	    	Book newBook= new Book();
	    	    	newBook.setIsbn(isbn);
	    	    	newBook.setCategory(token[3]);	    	    	
	    	    	newBook.setCoverimage(new URL(token[5]));	    	
	    	    	newBook.setStatus("available");
	    	    	newBook.setTitle(token[1]);
	    	        bookRepository.addBook(newBook);	
	    	        System.out.println("New Book Added::"+newBook.getTitle());
	    	       
	    	    }
	    	    else if(book.getStatus().contains("lost"))
	    	    {
	    	    	System.out.println("Book :"+isbn+" Status::"+ book.getStatus());
	    	    	book.setStatus("available");
	    	    	bookRepository.updateBook(isbn, book);
	    	    	System.out.println("Status updated");
	    	    }
	    	       
	    	    } 
	    	    
	    	}
	    	
	    	connection.close();}catch(Exception e){System.out.println("Exception::"+e.getMessage());}
	    }

	};	
	System.out.println("About to submit the background task");
	executor.execute(backgroundTask);
	System.out.println("Submitted the background task");
	executor.shutdown();
	}

    @Override
    public void initialize(Bootstrap<LibraryServiceConfiguration> bootstrap) {
	bootstrap.setName("library-service");
	bootstrap.addBundle(new ViewBundle());
	bootstrap.addBundle(new AssetsBundle());
    }

    @Override
    public void run(LibraryServiceConfiguration configuration,
	    Environment environment) throws Exception {
    	
	// This is how you pull the configurations from library_x_config.yml
	String queueName = configuration.getStompQueueName();
	String topicName = configuration.getStompTopicName();
	LibraryService.userName= configuration.getapolloUser();
	LibraryService.passwordName= configuration.getapolloPassword();
	LibraryService.hostName =  configuration.getapolloHost();
	LibraryService.portName= configuration.getapolloPort();
	
	log.debug(LibraryService.userName+" "+LibraryService.passwordName+" "+LibraryService.hostName+" "+LibraryService.portName);
	log.debug("Queue name is {}. Topic name is {}", queueName,
		topicName);
	/** Root API */
	environment.addResource(RootResource.class);
	/** Books APIs */
	bookRepository = new BookRepository();
	environment.addResource(new BookResource(bookRepository));

	/** UI Resources */
	environment.addResource(new HomeResource(bookRepository));
	
	LibraryService.setqName(queueName);
	LibraryService.settName(topicName);
	System.out.println("QueueName:"+queueName+"  TopicName"+topicName);	
    }

	public static String getqName() {
		return qName;
	}

	public static void setqName(String qName) {
		LibraryService.qName = qName;
	}

	public static String gettName() {
		return tName;
	}

	public static void settName(String tName) {
		LibraryService.tName = tName;
	}
	
	public static String getUname()
	{
		return userName;
	}
	
	public static String getPassword()
	{
		return passwordName;
	}
	public static String getHost()
	{
		return hostName;
	}
	public static String getPort()
	{
		return portName;
	}
}
