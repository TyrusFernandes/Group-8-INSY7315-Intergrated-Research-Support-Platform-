namespace AcadenceWebApp.Models
{
    public class EscalationItemDto
    {
        public string Id { get; set; } = Guid.NewGuid().ToString();
        public string StudentName { get; set; } = "";
        public string Issue { get; set; } = "";        // e.g., "Missed Deadline"
        public string Severity { get; set; } = "High"; // Low/Medium/High
        public string Status { get; set; } = "Open";   // Open/In-Progress/Resolved
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    }
}

