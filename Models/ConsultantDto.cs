namespace AcadenceWebApp.Models
{
    public class ConsultantDto
    {
        public string Id { get; set; } = Guid.NewGuid().ToString();
        public string Name { get; set; } = "";
        public int ActiveProjects { get; set; }
        public string Department { get; set; } = "General";
        public string Region { get; set; } = "N/A";
    }
}
