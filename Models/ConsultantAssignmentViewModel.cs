using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;

namespace AcadenceWebApp.Models
{
    public class ConsultantAssignmentViewModel
    {
        // Dropdown data
        public List<ServiceRequestDto> Requests { get; set; } = new();
        public List<ConsultantDto> Consultants { get; set; } = new();

        // Selected values
        [Required(ErrorMessage = "Please select a service request.")]
        public string? SelectedRequestId { get; set; }

        [Required(ErrorMessage = "Please select a consultant.")]
        public string? SelectedConsultantId { get; set; }
    }
}
