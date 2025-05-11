using dotnet_async_crud_demo.Domain;
using Microsoft.EntityFrameworkCore;

namespace dotnet_async_crud_demo.Repository;

public class AppDbContext(DbContextOptions<AppDbContext> options) : DbContext(options)
{
    public DbSet<Contact> Contacts { get; set; }
}
