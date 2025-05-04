package com.wikicoding.webfluxasyncdemo.service;

import com.wikicoding.webfluxasyncdemo.domain.Contact;
import com.wikicoding.webfluxasyncdemo.repository.ContactsRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@Slf4j
public class ContactsService {
    private final ContactsRepository contactsRepository;

    @Transactional
    public Mono<Contact> saveAsync(String name, String email) {
        log.warn("Service running save on thread {}", Thread.currentThread().getName());
        Contact contact = new Contact(name, email);
        return contactsRepository.save(contact);
    }

    public Mono<Contact> findByEmailAsync(String email) {
        log.warn("Service running the find on thread {}", Thread.currentThread().getName());
        return contactsRepository.findByEmail(email);
    }
}
