﻿using dotnet_async_crud_demo.Domain;
using dotnet_async_crud_demo.Repository;
using Microsoft.EntityFrameworkCore;

namespace dotnet_async_crud_demo.Services;

public class ContactsService(AppDbContext appDbContext, RepositoryNpgsql repositoryNpgsql, ILogger<ContactsService> logger)
{
    public async Task<Contact> SaveAsync(string name, string email)
    {
        logger.LogWarning("Service running save on thread {}", Thread.CurrentThread.ManagedThreadId);
        var contact = new Contact
        {
            Name = name,
            Email = email
        };

        //appDbContext.Contacts.Add(contact);
        //await appDbContext.SaveChangesAsync();
        await repositoryNpgsql.SaveAsync(contact);

        return contact;
    }

    public async Task<Contact?> FindByEmailAsync(string email)
    {
        logger.LogWarning("Service running the find on thread {}", Thread.CurrentThread.ManagedThreadId);
        //var contact = await appDbContext.Contacts.FirstOrDefaultAsync(contact => contact.Email == email);
        var contact = await repositoryNpgsql.FindByEmailAsync(email);
        logger.LogWarning("Service running the find on thread {}", Thread.CurrentThread.ManagedThreadId);
        return contact;
    }
}
