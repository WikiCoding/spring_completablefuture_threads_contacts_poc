package com.wikicoding.asynccrudcompletablefuture.repository;

import com.wikicoding.asynccrudcompletablefuture.domain.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Repository
public interface ContactsRepository extends JpaRepository<Contact, Integer> {
//    Optional<Contact> findByEmail(String email);
    @Async
    CompletableFuture<Optional<Contact>> findByEmail(String email);
}
