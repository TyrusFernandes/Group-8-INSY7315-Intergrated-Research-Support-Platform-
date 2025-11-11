using System;

namespace AcadenceWebApp.Models
{
    public class FeedbackItemDto
    {
        public string Id { get; set; } = Guid.NewGuid().ToString();
        public string Comment { get; set; } = "";
        public string ConsultantUid { get; set; } = "";
        public string ConsultantName { get; set; } = "";
        public string StudentUid { get; set; } = "";
        public string StudentName { get; set; } = "";
        public int Rating { get; set; } = 0;
        public DateTime Timestamp { get; set; } = DateTime.UtcNow;
    }
}