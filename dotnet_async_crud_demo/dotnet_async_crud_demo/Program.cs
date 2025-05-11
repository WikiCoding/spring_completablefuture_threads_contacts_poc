using dotnet_async_crud_demo.Repository;
using dotnet_async_crud_demo.Services;
using Microsoft.EntityFrameworkCore;
using Npgsql;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.

builder.Services.AddControllers();
// Learn more about configuring Swagger/OpenAPI at https://aka.ms/aspnetcore/swashbuckle
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();
builder.Services.AddDbContext<AppDbContext>(config => config.UseNpgsql(builder.Configuration.GetConnectionString("db")));
var dataSource = NpgsqlDataSource.Create(builder.Configuration.GetConnectionString("db")!);
builder.Services.AddSingleton(dataSource);
builder.Services.AddScoped<RepositoryNpgsql>();
builder.Services.AddScoped<ContactsService>();

builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAll", policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyMethod()
              .AllowAnyHeader();
    });
});

var app = builder.Build();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseCors("AllowAll");

app.UseAuthorization();

app.MapControllers();

app.Run();
