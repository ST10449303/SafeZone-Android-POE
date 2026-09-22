using Microsoft.EntityFrameworkCore;
using SafeZoneAPI.Models;

namespace SafeZoneAPI.Data
{
    public class SafeZoneDbContext : DbContext
    {
        public SafeZoneDbContext(DbContextOptions<SafeZoneDbContext> options)
            : base(options)
        {
        }

        public DbSet<SafetyAlert> SafetyAlerts { get; set; }
    }
}