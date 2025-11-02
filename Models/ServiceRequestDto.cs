using System;

namespace AcadenceWebApp.Models
{
    public class ServiceRequestDto
    {
        public string Id { get; set; } = Guid.NewGuid().ToString();
        public string StudentName { get; set; } = "";
        public string Category { get; set; } = "";  // e.g., "Coaching", "Editing", "Consultation"
        public string Title => $"{StudentName} - {Category}";
        public string Status { get; set; } = "Open"; // Open, Assigned, Closed
    }
}
