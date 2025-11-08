using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;

namespace AcadenceWebApp.Models
{
    public class ConsultantAssignmentViewModel
    {
        // All requests (mapped from Firestore 'requests')
        public List<TaskViewModel> Requests { get; set; } = new List<TaskViewModel>();

        // All consultants for the dropdown
        public List<ConsultantDto> Consultants { get; set; } = new List<ConsultantDto>();

        // Selected values
        [Required(ErrorMessage = "Please select a service request.")]
        public string? SelectedRequestId { get; set; }

        [Required(ErrorMessage = "Please select a consultant.")]
        public string? SelectedConsultantId { get; set; }
    }
}
