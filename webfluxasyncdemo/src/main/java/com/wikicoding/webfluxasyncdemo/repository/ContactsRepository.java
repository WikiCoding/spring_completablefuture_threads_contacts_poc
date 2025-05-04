package com.wikicoding.webfluxasyncdemo.repository;

import com.wikicoding.webfluxasyncdemo.domain.Contact;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ContactsRepository extends ReactiveCrudRepository<Contact, Integer> {
    Mono<Contact> findByEmail(String email);
}
