using System;

namespace AcadenceWebApp.Models
{
    public class AcademicResourceDto
    {
        public string Id { get; set; } = Guid.NewGuid().ToString();
        public string Title { get; set; } = "";
        public string FileName { get; set; } = "";
        public string OriginalName { get; set; } = "";
        public string Url { get; set; } = "";
        public DateTime UploadedAt { get; set; } = DateTime.UtcNow;
    }
}
