using Microsoft.EntityFrameworkCore;
using SafeZoneAPI.Data;

var builder = WebApplication.CreateBuilder(args);

// =========================================================
// DATABASE CONNECTION
// =========================================================
//
// Priority:
// 1. Azure App Service SQL connection string
// 2. Azure App Service application setting
// 3. Normal ASP.NET Core connection string
//
// Azure App Service exposes a SQL connection string named
// SafeZoneConnection as:
// SQLAZURECONNSTR_SafeZoneConnection
//
// This allows the deployed application to use Azure SQL,
// while local development can continue using localhost.
//

var azureConnectionString =
    Environment.GetEnvironmentVariable(
        "SQLAZURECONNSTR_SafeZoneConnection");

var appSettingConnectionString =
    Environment.GetEnvironmentVariable(
        "SafeZoneConnection");

var configurationConnectionString =
    builder.Configuration.GetConnectionString(
        "SafeZoneConnection");

var connectionString =
    azureConnectionString
    ?? appSettingConnectionString
    ?? configurationConnectionString;

if (string.IsNullOrWhiteSpace(connectionString))
{
    throw new InvalidOperationException(
        "SafeZoneConnection was not found. " +
        "Please configure the Azure SQL connection string " +
        "in Azure App Service.");
}

// =========================================================
// DATABASE
// =========================================================

builder.Services.AddDbContext<SafeZoneDbContext>(options =>
    options.UseSqlServer(connectionString));

// =========================================================
// SERVICES
// =========================================================

builder.Services.AddControllers();

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// =========================================================
// BUILD APPLICATION
// =========================================================

var app = builder.Build();

// =========================================================
// SWAGGER
// =========================================================

app.UseSwagger();
app.UseSwaggerUI();

// =========================================================
// ROOT PAGE
// =========================================================

app.MapGet("/", () => Results.Redirect("/swagger"));

// =========================================================
// HTTPS
// =========================================================

app.UseHttpsRedirection();

// =========================================================
// AUTHORIZATION
// =========================================================

app.UseAuthorization();

// =========================================================
// API CONTROLLERS
// =========================================================

app.MapControllers();

// =========================================================
// START APPLICATION
// =========================================================

app.Run();