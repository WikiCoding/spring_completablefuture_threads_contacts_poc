package com.wikicoding.asynccrudcompletablefuture.controllers;

import com.wikicoding.asynccrudcompletablefuture.domain.Contact;
import com.wikicoding.asynccrudcompletablefuture.dto.AddContactRequest;
import com.wikicoding.asynccrudcompletablefuture.service.ContactsService;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/contacts")
@AllArgsConstructor
@Slf4j
public class ContactsController {
    private final ContactsService contactsService;

    @PostMapping
    public CompletableFuture<ResponseEntity<Object>> createContact(@RequestBody AddContactRequest request) {
        log.warn("Controller endpoint running on thread {}", Thread.currentThread().getName());
        if (request.getName().trim().isEmpty() || request.getEmail().isEmpty())
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request has to be valid"));

        return contactsService.saveAsync(request.getName(), request.getEmail())
                .thenApply(savedContact -> {
                    log.info("Saved contact!");
                    return ResponseEntity.status(HttpStatus.CREATED).body(savedContact);
                });
    }

    @GetMapping("/{email}")
    public CompletableFuture<ResponseEntity<Object>> findByEmail(@PathVariable(name = "email") String email) {
        log.warn("Controller endpoint running on thread {}", Thread.currentThread().getName());

        if (email.trim().isEmpty())
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email has to be valid"));

        return contactsService.findByEmailAsync(email)
                .thenApply(optionalContact ->
                        optionalContact.isEmpty() ?
                                ResponseEntity.notFound().build() :
                                ResponseEntity.ok(optionalContact.get())
                );
    }

    @DeleteMapping("/{email}")
    public CompletableFuture<ResponseEntity<Object>> deleteByEmail(@PathVariable(name = "email") String email) {
        log.warn("Controller endpoint running on thread {}", Thread.currentThread().getName());

        if (email.trim().isEmpty())
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email has to be valid"));

        return contactsService.deleteByEmailAsync(email)
                .thenApply(aVoid -> ResponseEntity.noContent().build())
                .exceptionally(ex -> {
                    if (ex.getCause() instanceof EntityNotFoundException) {
                        return ResponseEntity.notFound().build();
                    }

                    log.info("Deleted contact");
                    return ResponseEntity.badRequest().build();
                });
    }
}
