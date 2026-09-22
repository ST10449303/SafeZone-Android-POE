namespace SafeZoneAPI.Models
{
    public class SafetyAlert
    {
        public int Id { get; set; }

        public string Title { get; set; } = string.Empty;

        public string Message { get; set; } = string.Empty;

        public string Audience { get; set; } = string.Empty;

        public string Location { get; set; } = string.Empty;

        public string Campus { get; set; } = string.Empty;

        public string AlertType { get; set; } = string.Empty;

        public DateTime CreatedAt { get; set; }

        public bool IsActive { get; set; }
    }
}