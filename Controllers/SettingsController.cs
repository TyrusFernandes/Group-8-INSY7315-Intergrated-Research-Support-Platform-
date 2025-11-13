using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;
using AcadenceWebApp.Models;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;

namespace AcadenceWebApp.Controllers
{
    [Authorize]
    public class SettingsController : Controller
    {
        // No UserManager/SignInManager injected to avoid requiring ASP.NET Core Identity registration.
        public SettingsController()
        {
        }

        // GET: Settings/Index
        public IActionResult Index()
        {
            // Read user info from claims issued at login (LoginController sets ClaimTypes.Email, ClaimTypes.Name, NameIdentifier, etc.)
            var email = User.FindFirstValue(ClaimTypes.Email) ?? string.Empty;
            var userName = User.FindFirstValue(ClaimTypes.Name) ?? User.Identity?.Name ?? string.Empty;

            // Read language from cookie (store codes 'en' or 'af'), map to display name
            var langCode = Request.Cookies["AppLanguage"] ?? HttpContext.Session.GetString("AppLanguageCode") ?? "en";
            var displayLang = langCode == "af" ? "Afrikaans" : "English";

            var model = new SettingsViewModel
            {
                UserName = userName,
                Email = email,
                ProfileImageUrl = "/images/default-avatar.png", // change if you persist user images
                NotificationsEnabled = true,
                SoundsEnabled = true,
                SelectedLanguage = displayLang,
                AvailableLanguages = new List<string> { "English", "Afrikaans" },
                SelectedTheme = "Light",
                AvailableThemes = new List<string> { "Light", "Dark" },
                BiometricLoginEnabled = false
            };

            return View(model);
        }

        // POST: Settings/SetLanguage
        [HttpPost]
        [ValidateAntiForgeryToken]
        public IActionResult SetLanguage(string language)
        {
            if (string.IsNullOrEmpty(language))
            {
                TempData["SuccessMessage"] = "No language selected.";
                return RedirectToAction("Index");
            }

            // Normalize input to codes we use
            var code = language.Equals("Afrikaans", StringComparison.OrdinalIgnoreCase) ? "af" : "en";

            // Set cookie (1 year)
            Response.Cookies.Append("AppLanguage", code, new CookieOptions
            {
                Expires = DateTimeOffset.UtcNow.AddYears(1),
                HttpOnly = false,
                IsEssential = true
            });

            // Also store in server session for immediate access
            HttpContext.Session.SetString("AppLanguage", language);
            HttpContext.Session.SetString("AppLanguageCode", code);

            TempData["SuccessMessage"] = $"Language set to {language}.";
            return RedirectToAction("Index");
        }

        // POST: Settings/UpdatePreferences
        [HttpPost]
        [ValidateAntiForgeryToken]
        public IActionResult UpdatePreferences(SettingsViewModel model)
        {
            // Since there's no Identity store configured here, persist preferences in your own store (DB / Firestore) if needed.
            TempData["SuccessMessage"] = "Preferences updated successfully!";
            return RedirectToAction("Index");
        }

        // GET: Settings/ChangePassword
        public IActionResult ChangePassword()
        {
            // If you use Firebase, implement change via Firebase REST (requires idToken) or instruct user to use Forgot Password flow.
            return View();
        }

        // POST: Settings/ChangePassword
        [HttpPost]
        [ValidateAntiForgeryToken]
        public IActionResult ChangePassword(ChangePasswordViewModel model)
        {
            if (!ModelState.IsValid)
            {
                return View(model);
            }

            // If you rely on Firebase Authentication (or another external provider), you cannot use UserManager here.
            // Implement provider-specific password change (e.g. Firebase accounts:update with idToken), or ask user to use Forgot Password.
            ModelState.AddModelError(string.Empty, "Password change must be performed via the authentication provider (e.g. use Forgot Password).");
            return View(model);
        }

        // POST: Settings/ToggleBiometric
        [HttpPost]
        [ValidateAntiForgeryToken]
        public IActionResult ToggleBiometric(bool enabled)
        {
            // Save biometric preference in your own store if required.
            return Json(new { success = true });
        }

        // GET: Settings/Support
        public IActionResult Support()
        {
            return View();
        }

        // GET: Settings/PrivacyPolicy
        public IActionResult PrivacyPolicy()
        {
            return View();
        }

        // POST: Settings/Logout
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Logout()
        {
            // Sign out cookie authentication (you configured cookie auth in Program.cs)
            await HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
            HttpContext.Session.Clear();

            // Redirect to the welcome page (Home/Index)
            return RedirectToAction("Index", "Home");
        }
    }
}