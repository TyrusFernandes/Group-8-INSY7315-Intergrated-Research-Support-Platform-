using System.ComponentModel.DataAnnotations;

namespace AcadenceWebApp.Models
{
    public class SettingsViewModel
    {
        // User Profile Information
        public string UserName { get; set; }
        public string Email { get; set; }
        public string ProfileImageUrl { get; set; }

        // Preferences
        public bool NotificationsEnabled { get; set; }
        public bool SoundsEnabled { get; set; }

        [Display(Name = "Language")]
        public string SelectedLanguage { get; set; }
        public List<string> AvailableLanguages { get; set; }

        [Display(Name = "Theme")]
        public string SelectedTheme { get; set; }
        public List<string> AvailableThemes { get; set; }

        // Account Settings
        public bool BiometricLoginEnabled { get; set; }

        public SettingsViewModel()
        {
            AvailableLanguages = new List<string> { "English", "Spanish", "French", "German", "Chinese" };
            AvailableThemes = new List<string> { "Light", "Dark", "Auto" };
        }
    }

    public class ChangePasswordViewModel
    {
        [Required]
        [DataType(DataType.Password)]
        [Display(Name = "Current Password")]
        public string CurrentPassword { get; set; }

        [Required]
        [StringLength(100, MinimumLength = 6)]
        [DataType(DataType.Password)]
        [Display(Name = "New Password")]
        public string NewPassword { get; set; }

        [Required]
        [DataType(DataType.Password)]
        [Display(Name = "Confirm New Password")]
        [Compare("NewPassword", ErrorMessage = "Passwords do not match.")]
        public string ConfirmPassword { get; set; }
    }
}