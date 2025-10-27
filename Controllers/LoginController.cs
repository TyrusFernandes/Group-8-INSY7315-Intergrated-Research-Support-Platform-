using AcadenceWebApp.Models;
using FirebaseAdmin.Auth;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;

namespace AcadenceWebApp.Controllers
{
    public class LoginController : Controller
    {
        // Hardcoded admin credentials
        private const string ADMIN_EMAIL = "admin@acadence.com";
        private const string ADMIN_PASSWORD = "Admin@123";

        // GET: Account/Login
        [HttpGet]
        public IActionResult Login()
        {
            return View("~/Views/Home/Login.cshtml");
        }

        // POST: Account/Login
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Login(LoginViewModel model)
        {
            if (!ModelState.IsValid)
            {
                return View("~/Views/Home/Login.cshtml", model);
            }

            // Check hardcoded admin credentials
            if (model.Email.Trim().Equals(ADMIN_EMAIL, StringComparison.OrdinalIgnoreCase) &&
                model.Password == ADMIN_PASSWORD)
            {
                // Create claims for the user
                var claims = new List<Claim>
                {
                    new Claim(ClaimTypes.Name, model.Email),
                    new Claim(ClaimTypes.Email, model.Email),
                    new Claim(ClaimTypes.Role, "Admin"),
                    new Claim("IsAdmin", "true")
                };

                var claimsIdentity = new ClaimsIdentity(claims, CookieAuthenticationDefaults.AuthenticationScheme);
                var claimsPrincipal = new ClaimsPrincipal(claimsIdentity);

                var authProperties = new AuthenticationProperties
                {
                    IsPersistent = model.RememberMe,
                    ExpiresUtc = DateTimeOffset.UtcNow.AddHours(24)
                };

                // Sign in the user
                await HttpContext.SignInAsync(
                    CookieAuthenticationDefaults.AuthenticationScheme,
                    claimsPrincipal,
                    authProperties);

                // Store additional info in session
                HttpContext.Session.SetString("UserEmail", model.Email);
                HttpContext.Session.SetString("IsAdmin", "true");

                // Redirect to admin dashboard (update as needed)
                return RedirectToAction("Dashboard", "Admin");
            }
            else
            {
                ModelState.AddModelError("", "Invalid email or password.");
                return View("~/Views/Home/Login.cshtml", model);
            }
        }

        // GET: Account/Logout
        [HttpGet]
        public async Task<IActionResult> Logout()
        {
            await HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
            HttpContext.Session.Clear();
            return RedirectToAction("Login");
        }

        // GET: Account/ForgotPassword
        [HttpGet]
        public IActionResult ForgotPassword()
        {
            return View("~/Views/Home/ForgotPassword.cshtml");
        }

        // GET: Account/Register
        [HttpGet]
        public IActionResult Register()
        {
            return View("~/Views/Home/Register.cshtml");
        }
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Register(RegisterViewModel model)
        {
            if (!ModelState.IsValid)
                return View("~/Views/Home/Register.cshtml", model);

            var claims = new List<Claim>
                {
                    new Claim(ClaimTypes.Name, model.Username),
                    new Claim(ClaimTypes.Email, model.Email),
                    new Claim(ClaimTypes.Role, "Consultant")
                };

            var claimsIdentity = new ClaimsIdentity(claims, CookieAuthenticationDefaults.AuthenticationScheme);
            var claimsPrincipal = new ClaimsPrincipal(claimsIdentity);

            await HttpContext.SignInAsync(
                CookieAuthenticationDefaults.AuthenticationScheme,
                claimsPrincipal,
                new AuthenticationProperties { IsPersistent = true });

            HttpContext.Session.SetString("UserEmail", model.Email);
            HttpContext.Session.SetString("IsAdmin", "false");

            return RedirectToAction("Dashboard", "Consultant");
        }
    }
}


