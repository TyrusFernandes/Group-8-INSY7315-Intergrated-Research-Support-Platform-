using System;

namespace AcadenceWebApp.Models
{
    public class EscalationItemDto
    {
        public string Id { get; set; } = Guid.NewGuid().ToString();
        public string StudentName { get; set; } = "";
        public string Issue { get; set; } = "";
        public string Severity { get; set; } = "Medium"; // Low/Medium/High
        public string Status { get; set; } = "Open";      // Open/In Progress/Closed
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        // Optional fields
        public string Details { get; set; } = "";
        public string ReportedBy { get; set; } = "";
    }
}