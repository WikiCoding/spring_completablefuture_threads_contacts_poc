package com.wikicoding.webfluxasyncdemo.controllers;


import com.wikicoding.webfluxasyncdemo.dto.AddContactRequest;
import com.wikicoding.webfluxasyncdemo.service.ContactsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/contacts")
@AllArgsConstructor
@Slf4j
public class ContactsController {
    private final ContactsService contactsService;

    @PostMapping
    public ResponseEntity<Object> createContact(@RequestBody AddContactRequest request) {
        log.warn("Controller endpoint running on thread {}", Thread.currentThread().getName());
        if (request.getName().trim().isEmpty() || request.getEmail().isEmpty())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request has to be valid");

        return ResponseEntity.status(HttpStatus.CREATED).body(contactsService.saveAsync(request.getName(), request.getEmail()));
    }

    @GetMapping("/{email}")
    public Mono<ResponseEntity<Object>> findByEmail(@PathVariable(name = "email") String email) {
        log.warn("Controller endpoint running on thread {}", Thread.currentThread().getName());

        if (email.trim().isEmpty())
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email has to be valid"));

        return contactsService.findByEmailAsync(email)
                .map(contact -> ResponseEntity.ok((Object) contact))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
