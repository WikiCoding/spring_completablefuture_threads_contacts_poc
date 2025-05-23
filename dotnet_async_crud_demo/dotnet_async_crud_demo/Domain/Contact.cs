﻿using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace dotnet_async_crud_demo.Domain;

public class Contact
{
    [Key]
    [JsonPropertyName("id")]
    public int Id { get; set; }
    [JsonPropertyName("name")]
    public string Name { get; set; } = string.Empty;
    [JsonPropertyName("email")]
    public string Email { get; set; } = string.Empty;
}
