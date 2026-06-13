package com.application.cqrs.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class BookCommandHandler {

	private final BookRepository repository;
	private final ApplicationEventPublisher publisher;

	public BookCommandHandler(BookRepository repository, ApplicationEventPublisher publisher) {
		this.repository = repository;
		this.publisher = publisher;
	}

	public UUID handle(BookCommand.CreateBook cmd) {
		Book book = new Book(cmd.title(), cmd.author(), cmd.price(), cmd.initialStock());
		repository.save(book);
		publisher.publishEvent(new BookChanged(book.getId()));
		return book.getId();
	}

	public void handle(BookCommand.ChangePrice cmd) {
		Book book = load(cmd.bookId());
		book.changePrice(cmd.newPrice());
		publisher.publishEvent(new BookChanged(book.getId()));
	}

	public void handle(BookCommand.AdjustStock cmd) {
		Book book = load(cmd.bookId());
		book.adjustStock(cmd.delta());
		publisher.publishEvent(new BookChanged(book.getId()));
	}

	private Book load(UUID id) {
		return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Book not found: " + id));
	}
}
