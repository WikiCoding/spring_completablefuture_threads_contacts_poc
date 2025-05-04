package com.wikicoding.asynccrudcompletablefuture.service;

import com.wikicoding.asynccrudcompletablefuture.domain.Contact;
import com.wikicoding.asynccrudcompletablefuture.repository.ContactsRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@AllArgsConstructor
@Slf4j
public class ContactsService {
    private final ContactsRepository contactsRepository;

    @Async
    @Transactional
    public CompletableFuture<Contact> saveAsync(String name, String email) {
        log.warn("Service running save on thread {}", Thread.currentThread().getName());
        Contact contact = new Contact(name, email);
        Contact saved = contactsRepository.save(contact);

        try {
            Thread.sleep(10_000);
        } catch (InterruptedException ex) {
            log.error(ex.getMessage());
        }

        CompletableFuture<Contact> contactCompletableFuture = CompletableFuture.completedFuture(saved);

        log.warn("I'm just logging after releasing thread {}, which is currently blocked!", Thread.currentThread().getName());

        return contactCompletableFuture;
    }

    @Async
    public CompletableFuture<Optional<Contact>> findByEmailAsync(String email) {
        log.warn("Service running the find on thread {}", Thread.currentThread().getName());
        return contactsRepository.findByEmail(email);
    }

    @Async
    public CompletableFuture<Void> deleteByEmailAsync(String email) {
        return findByEmailAsync(email) // thenCompose is making everything async and flattening the CompletableFuture<CompletableFuture<Void>>
                .thenCompose(optContact -> {
                    log.warn("Service running on the find on thread {}", Thread.currentThread().getName());
                    if (optContact.isEmpty()) {
                        CompletableFuture<Void> failed = new CompletableFuture<>();
                        failed.completeExceptionally(new EntityNotFoundException("Not found"));
                        return failed;
                    }

                    return CompletableFuture.runAsync(() -> {
                        log.warn("Service running the delete on thread {}", Thread.currentThread().getName());
                        contactsRepository.delete(optContact.get());
                    });
                });
    }
}
