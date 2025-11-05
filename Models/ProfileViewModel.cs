using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Http;

namespace AcadenceWebApp.Models
{
    public class ProfileViewModel
    {
        public string UserId { get; set; }

        [Required(ErrorMessage = "Full name is required")]
        [Display(Name = "Full Name")]
        public string FullName { get; set; }

        [Required(ErrorMessage = "Username is required")]
        [Display(Name = "Username")]
        public string Username { get; set; }

        [Required(ErrorMessage = "Email is required")]
        [EmailAddress(ErrorMessage = "Invalid email address")]
        [Display(Name = "Email")]
        public string Email { get; set; }

        [Phone(ErrorMessage = "Invalid phone number")]
        [Display(Name = "Phone Number")]
        public string PhoneNumber { get; set; }

        [Display(Name = "Field of Study")]
        public string Field { get; set; }

        [Display(Name = "Profile Photo")]
        public string ProfilePhotoUrl { get; set; }

        public IFormFile ProfilePhoto { get; set; }
    }
}
