package com.koobe.tool.worker;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileDeleteStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.koobe.common.data.domain.Book;
import com.koobe.common.data.repository.BookRepository;
import com.koobe.common.storage.AmazonS3Storage;

public class ExtractFlipOrderWorker implements Runnable {
	
	protected Logger log = LoggerFactory.getLogger(getClass());
	
	private String guid;
	
	private Book book;
	
	private BookRepository bookRepository;
	
	private AmazonS3Storage storage;
	
	private String keyPrefix = "books-2x/epub/unzip";
	
	public ExtractFlipOrderWorker(AmazonS3Storage storage, Book book, BookRepository bookRepository) {
		this.guid = guid;
		this.storage = storage;
		this.book = book;
		this.bookRepository = bookRepository;
		this.guid = book.getGuid();
	}

	public void run() {
		
		log.info("[{}] process book order", guid);
		
		String opfKey = keyPrefix + "/" + guid.substring(0, 1) + 
				"/" + guid.substring(1, 2) + 
				"/" + guid.substring(2, 3) + 
				"/" + guid + ".epub/OEBPS/content.opf";
		
		File file = storage.getObject("koobecloudepub", opfKey);
		log.info("[{}] file got {}", guid, file.getAbsolutePath());
		
		try {
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			
			Document document = builder.parse(file);
			
			NodeList list = document.getElementsByTagName("order");
			
			if (list.getLength() > 0) {
				String value = list.item(0).getFirstChild().getNodeValue();
				if (value != null && (value.equals("right") || value.equals("left"))) {
					log.info("[{}] order value: {}", guid, value);
					book.setFlipOrder(value);
				} else {
					log.info("[{}][ERROR] not a valid order value {}", guid, value);
				}
			} else {
				log.info("[{}][ERROR] no order node found", guid);
			}
		} catch (Exception e) {
			log.error("[{}][ERROR] {}", e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				FileDeleteStrategy.FORCE.delete(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
			bookRepository.save(book);
		}
		
		log.info("[{}] process end", guid);
	}
}
