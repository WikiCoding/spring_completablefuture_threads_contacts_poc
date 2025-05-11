using dotnet_async_crud_demo.dtos;
using dotnet_async_crud_demo.Services;
using Microsoft.AspNetCore.Mvc;
using System.Diagnostics;

namespace dotnet_async_crud_demo.Controllers;

[ApiController]
[Route("api/v1/[controller]")]
public class ContactsController(ContactsService service, ILogger<ContactsController> logger) : ControllerBase
{
    [HttpPost]
    public async Task<IActionResult> saveContact([FromBody] AddContactRequest request)
    {
        logger.LogWarning("Controller endpoint running on thread {}", Thread.CurrentThread.ManagedThreadId);

        if (string.IsNullOrEmpty(request.name) || string.IsNullOrEmpty(request.email))
        {
            return BadRequest("Request has to be valid");
        }

        var contact = await service.SaveAsync(request.name, request.email);

        return CreatedAtAction(nameof(saveContact), contact);
    }

    [HttpGet("{email}")]
    public async Task<IActionResult> findByEmail([FromRoute(Name = "email")] string email)
    {
        var stopwatch = new Stopwatch();
        stopwatch.Start();

        logger.LogWarning("Controller endpoint running on thread {}", Thread.CurrentThread.ManagedThreadId);

        if (string.IsNullOrEmpty(email))
        {
            return BadRequest("Request has to be valid");
        }

        var contact = await service.FindByEmailAsync(email);

        if (contact is null)
        {
            return NotFound("Contact not found by email");
        }

        stopwatch.Stop();

        logger.LogWarning("Request took {}ms and now is on thread {}", stopwatch.ElapsedMilliseconds, Thread.CurrentThread.ManagedThreadId);

        return Ok(contact);
    }
}
