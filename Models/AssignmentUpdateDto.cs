namespace AcadenceWebApp.Models
{
    public class AssignmentUpdateDto
    {
        public string TaskId { get; set; } = "";
        public string? AssignedToUid { get; set; }
        public string? AssignedToName { get; set; }
        public bool AdminApproved { get; set; }
    }
}