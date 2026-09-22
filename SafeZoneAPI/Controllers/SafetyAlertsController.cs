using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SafeZoneAPI.Data;
using SafeZoneAPI.Models;

namespace SafeZoneAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class SafetyAlertsController : ControllerBase
    {
        private readonly SafeZoneDbContext _context;

        public SafetyAlertsController(SafeZoneDbContext context)
        {
            _context = context;
        }

        // =========================================================
        // GET: api/SafetyAlerts
        // Returns all active alerts
        // =========================================================

        [HttpGet]
        public async Task<ActionResult<IEnumerable<SafetyAlert>>> GetAlerts()
        {
            var alerts = await _context.SafetyAlerts
                .Where(a => a.IsActive)
                .OrderByDescending(a => a.CreatedAt)
                .ToListAsync();

            return Ok(alerts);
        }

        // =========================================================
        // GET: api/SafetyAlerts/1
        // Returns one alert
        // =========================================================

        [HttpGet("{id}")]
        public async Task<ActionResult<SafetyAlert>> GetAlert(int id)
        {
            var alert = await _context.SafetyAlerts
                .FirstOrDefaultAsync(a => a.Id == id);

            if (alert == null)
            {
                return NotFound(new
                {
                    message = "Safety alert not found."
                });
            }

            return Ok(alert);
        }

        // =========================================================
        // CAMPUS ALERTS
        // GET: api/SafetyAlerts/campus/Pretoria
        //
        // Returns only:
        // Audience = Campus
        // Campus = selected campus
        // IsActive = true
        // =========================================================

        [HttpGet("campus/{campus}")]
        public async Task<ActionResult<IEnumerable<SafetyAlert>>> GetAlertsByCampus(
            string campus)
        {
            var alerts = await _context.SafetyAlerts
                .Where(a =>
                    a.Audience == "Campus" &&
                    a.Campus == campus &&
                    a.IsActive)
                .OrderByDescending(a => a.CreatedAt)
                .ToListAsync();

            return Ok(alerts);
        }

        // =========================================================
        // PERSONAL ALERTS
        // GET: api/SafetyAlerts/location/Pretoria
        //
        // Returns only:
        // Audience = Personal
        // Location = selected location
        // IsActive = true
        // =========================================================

        [HttpGet("location/{location}")]
        public async Task<ActionResult<IEnumerable<SafetyAlert>>> GetAlertsByLocation(
            string location)
        {
            var alerts = await _context.SafetyAlerts
                .Where(a =>
                    a.Audience == "Personal" &&
                    a.Location == location &&
                    a.IsActive)
                .OrderByDescending(a => a.CreatedAt)
                .ToListAsync();

            return Ok(alerts);
        }

        // =========================================================
        // GET: api/SafetyAlerts/type/Emergency
        // Returns active alerts by type
        // =========================================================

        [HttpGet("type/{alertType}")]
        public async Task<ActionResult<IEnumerable<SafetyAlert>>> GetAlertsByType(
            string alertType)
        {
            var alerts = await _context.SafetyAlerts
                .Where(a =>
                    a.AlertType == alertType &&
                    a.IsActive)
                .OrderByDescending(a => a.CreatedAt)
                .ToListAsync();

            return Ok(alerts);
        }

        // =========================================================
        // POST: api/SafetyAlerts
        // Creates a new alert
        // =========================================================

        [HttpPost]
        public async Task<ActionResult<SafetyAlert>> CreateAlert(
            SafetyAlert alert)
        {
            if (alert.Audience != "Personal" &&
                alert.Audience != "Campus")
            {
                return BadRequest(new
                {
                    message = "Audience must be Personal or Campus."
                });
            }

            if (string.IsNullOrWhiteSpace(alert.Location))
            {
                return BadRequest(new
                {
                    message = "Location is required."
                });
            }

            if (alert.Audience == "Campus" &&
                string.IsNullOrWhiteSpace(alert.Campus))
            {
                return BadRequest(new
                {
                    message = "Campus is required for Campus alerts."
                });
            }

            if (alert.Audience == "Personal")
            {
                alert.Campus = string.Empty;
            }

            alert.CreatedAt = DateTime.Now;
            alert.IsActive = true;

            _context.SafetyAlerts.Add(alert);

            await _context.SaveChangesAsync();

            return CreatedAtAction(
                nameof(GetAlert),
                new { id = alert.Id },
                alert);
        }

        // =========================================================
        // PUT: api/SafetyAlerts/1
        // Updates an existing alert
        // =========================================================

        [HttpPut("{id}")]
        public async Task<IActionResult> UpdateAlert(
            int id,
            SafetyAlert updatedAlert)
        {
            var alert = await _context.SafetyAlerts
                .FirstOrDefaultAsync(a => a.Id == id);

            if (alert == null)
            {
                return NotFound(new
                {
                    message = "Safety alert not found."
                });
            }

            if (updatedAlert.Audience != "Personal" &&
                updatedAlert.Audience != "Campus")
            {
                return BadRequest(new
                {
                    message = "Audience must be Personal or Campus."
                });
            }

            if (string.IsNullOrWhiteSpace(updatedAlert.Location))
            {
                return BadRequest(new
                {
                    message = "Location is required."
                });
            }

            if (updatedAlert.Audience == "Campus" &&
                string.IsNullOrWhiteSpace(updatedAlert.Campus))
            {
                return BadRequest(new
                {
                    message = "Campus is required for Campus alerts."
                });
            }

            alert.Title = updatedAlert.Title;
            alert.Message = updatedAlert.Message;
            alert.Audience = updatedAlert.Audience;
            alert.Location = updatedAlert.Location;
            alert.Campus = updatedAlert.Audience == "Personal"
                ? string.Empty
                : updatedAlert.Campus;
            alert.AlertType = updatedAlert.AlertType;
            alert.IsActive = updatedAlert.IsActive;

            await _context.SaveChangesAsync();

            return Ok(alert);
        }

        // =========================================================
        // DELETE: api/SafetyAlerts/1
        // Deletes an alert
        // =========================================================

        [HttpDelete("{id}")]
        public async Task<IActionResult> DeleteAlert(int id)
        {
            var alert = await _context.SafetyAlerts
                .FirstOrDefaultAsync(a => a.Id == id);

            if (alert == null)
            {
                return NotFound(new
                {
                    message = "Safety alert not found."
                });
            }

            _context.SafetyAlerts.Remove(alert);

            await _context.SaveChangesAsync();

            return NoContent();
        }
    }
}