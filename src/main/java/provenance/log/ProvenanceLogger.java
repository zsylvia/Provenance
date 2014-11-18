package provenance.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import provenance.model.Calling;
import provenance.model.Constructor;
import provenance.model.Method;
import provenance.model.Model;

public class ProvenanceLogger {
	private static BufferedWriter bufferedWriter;
	private static ArrayBlockingQueue<String> logQueue;
	private static final ProvenanceLogger logger = new ProvenanceLogger();
	
	
	private ProvenanceLogger() {
		try {
			logQueue = new ArrayBlockingQueue<String>(50000, true);
			String fileName = "provenance-capture-" + new Timestamp(new Date().getTime()) + ".log";
			File file = new File(System.getProperty("java.io.tmpdir") + "/" + fileName.replace(" ", "-"));
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			bufferedWriter = new BufferedWriter(fw);
			write();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void log(String message, Class clazz, int hashCode) {
		logQueue.offer(new Timestamp(new Date().getTime()) + "\t" + clazz.getSimpleName() + "(" + hashCode + "): " + message);
	}
	
	public static void log(String message, Class clazz) {
		logQueue.offer(new Timestamp(new Date().getTime()) + "\t" + clazz.getSimpleName() + ": " + message);
	}
	
	public static void log(Model m) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			if(m instanceof Constructor) {
				logQueue.offer(mapper.writeValueAsString((Constructor)m) + ",");
			} else if(m instanceof Method) {
				logQueue.offer(mapper.writeValueAsString((Method)m) + ",");
			} else if(m instanceof Calling) {
				logQueue.offer(mapper.writeValueAsString((Calling)m) + ",");
			}
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void write() {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(new Runnable() {
			public void run() {
				try {
					while(true) {
						bufferedWriter.newLine();
						bufferedWriter.write(logQueue.take());
						bufferedWriter.flush();
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
