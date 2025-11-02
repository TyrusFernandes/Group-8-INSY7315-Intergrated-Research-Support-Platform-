using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Http;

namespace AcadenceWebApp.Models
{
    public class UploadResourceViewModel
    {
        [Required(ErrorMessage = "Please select a file.")]
        public IFormFile? File { get; set; }

        [Required(ErrorMessage = "Enter a resource title.")]
        [StringLength(120)]
        public string? Title { get; set; }

        public List<AcademicResourceDto> Existing { get; set; } = new();
    }
}
