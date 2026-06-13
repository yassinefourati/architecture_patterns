package com.application.cqrs.command;

import java.util.UUID;

public record BookChanged(UUID bookId) {
	
}
