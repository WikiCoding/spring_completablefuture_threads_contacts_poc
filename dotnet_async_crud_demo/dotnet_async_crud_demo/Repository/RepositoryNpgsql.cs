using dotnet_async_crud_demo.Domain;
using Npgsql;

namespace dotnet_async_crud_demo.Repository;

public class RepositoryNpgsql
{
    private readonly ILogger<RepositoryNpgsql> logger;
    private const string table = "Contacts";
    private readonly NpgsqlDataSource dataSource;

    public RepositoryNpgsql(ILogger<RepositoryNpgsql> logger, NpgsqlDataSource dataSource)
    {
        this.logger = logger;
        this.dataSource = dataSource;
    }

    public async Task<Contact> SaveAsync(Contact contact)
    {
        await using (var cmd = dataSource.CreateCommand($"INSERT INTO \"{table}\" (\"Name\", \"Email\") VALUES (@name, @email)"))
        {
            cmd.Parameters.AddWithValue("@name", contact.Name);
            cmd.Parameters.AddWithValue("@email", contact.Email);
            await cmd.ExecuteNonQueryAsync();
        }

        return contact;
    }

    public async Task<Contact?> FindByEmailAsync(string email)
    {
        await using var cmd = dataSource.CreateCommand($"SELECT \"Id\", \"Name\", \"Email\" FROM \"{table}\" WHERE \"Email\" = @email");

        cmd.Parameters.AddWithValue("@email", email);

        await using var reader = await cmd.ExecuteReaderAsync();

        if (await reader.ReadAsync())
        {
            return new Contact
            {
                Id = reader.GetInt32(0),
                Name = reader.GetString(1),
                Email = reader.GetString(2)
            };
        }

        return null;
    }
}
