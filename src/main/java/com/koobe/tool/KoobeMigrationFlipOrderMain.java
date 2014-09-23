package com.koobe.tool;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.koobe.common.core.KoobeApplication;
import com.koobe.common.data.KoobeDataService;
import com.koobe.common.data.domain.Book;
import com.koobe.common.data.repository.BookRepository;
import com.koobe.common.storage.AmazonS3Storage;
import com.koobe.common.storage.KoobeStorageService;
import com.koobe.tool.worker.ExtractFlipOrderWorker;

public class KoobeMigrationFlipOrderMain {
	
	protected static Logger log = LoggerFactory.getLogger(KoobeMigrationFlipOrderMain.class);
	
	static KoobeApplication koobeApplication;
	static KoobeDataService koobeDataService;
	static JdbcTemplate jdbcTemplate;
	static AmazonS3Storage storage;
	
	static ExecutorService executor;
	
	static BookRepository bookRepository;
	
	static {
		koobeApplication = KoobeApplication.getInstance();
		koobeDataService = (KoobeDataService) koobeApplication.getService(KoobeDataService.class);
		
		jdbcTemplate = koobeDataService.getJdbcTemplate();
		KoobeStorageService storageService = (KoobeStorageService) koobeApplication.getService(KoobeStorageService.class);
		storage = storageService.getAmazonS3Storage();
		
		bookRepository = (BookRepository) koobeDataService.getRepository(BookRepository.class);
	}

	public static void main(String[] args) {
		
		executor = Executors.newFixedThreadPool(3, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r);
				thread.setDaemon(true);
				return thread;
			}
		});

		List<Book> bookList = bookRepository.getByImported(1);
		log.info("Book list size: {}", bookList.size());
		for (Book book : bookList) {
			ExtractFlipOrderWorker worker = new ExtractFlipOrderWorker(storage, book, bookRepository);
			executor.submit(worker);
		}
		
		bookList = bookRepository.getByImported(2);
		log.info("Book list size: {}", bookList.size());
		for (Book book : bookList) {
			ExtractFlipOrderWorker worker = new ExtractFlipOrderWorker(storage, book, bookRepository);
			executor.submit(worker);
		}

//		ExtractFlipOrderWorker worker = new ExtractFlipOrderWorker(storage, new Book(), bookRepository);
//		executor.submit(worker);
		
		try {
			Thread.sleep(999999999);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
