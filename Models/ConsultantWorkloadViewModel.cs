namespace AcadenceWebApp.Models
{
    public class ConsultantWorkloadViewModel
    {
        public List<ConsultantDto> Consultants { get; set; } = new();
    
        public int TotalConsultants => Consultants.Count;
        public int TotalActiveProjects { get; set; }
        public double AverageActiveProjects { get; set; }
        public ConsultantDto? Busiest { get; set; }
    }
}
