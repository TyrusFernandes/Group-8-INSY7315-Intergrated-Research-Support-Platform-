using System;

namespace AcadenceWebApp.Models
{
    public class ServiceRequestDto
    {
        public string Id { get; set; } = Guid.NewGuid().ToString();

        // Field names used by the Admin/Assign view
        public string Title { get; set; } = "";
        public string StudentName { get; set; } = "";
        public string Category { get; set; } = "";

        // Status and timestamps
        public string Status { get; set; } = "Open";
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
        public DateTime? DueDate { get; set; }

        // Optional extra fields if referenced elsewhere
        public string Details { get; set; } = "";
        public int Price { get; set; } = 0;
    }
}