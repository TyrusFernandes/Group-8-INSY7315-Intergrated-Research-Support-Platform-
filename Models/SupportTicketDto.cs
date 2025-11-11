using System;

namespace AcadenceWebApp.Models
{
    public class SupportTicketDto
    {
        public string Id { get; set; } = Guid.NewGuid().ToString();
        public string InitialMessage { get; set; } = "";
        public string StudentId { get; set; } = "";
        public string StudentName { get; set; } = "";
        public string Status { get; set; } = "pending";
        public DateTime Timestamp { get; set; } = DateTime.UtcNow;
    }
}