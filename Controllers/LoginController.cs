// FILE: Controllers/LoginController.cs
using AcadenceWebApp.Models;
using Google.Cloud.Firestore;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;
using System.Text.Json;
using System.Text;
using System.Net.Http.Headers;

namespace AcadenceWebApp.Controllers
{
    public class LoginController : Controller
    {
        private readonly FirestoreDb _firestore;
        private readonly string _apiKey;

        public LoginController()
        {
            // 🔹 Path to the JSON file (root of project)
            string credentialsPath = Path.Combine(Directory.GetCurrentDirectory(), "firebase-adminsdk.json");

            // 🔹 Set env variable so Firestore SDK picks it up
            Environment.SetEnvironmentVariable("GOOGLE_APPLICATION_CREDENTIALS", credentialsPath);

            // 🔹 Read JSON file
            var json = System.IO.File.ReadAllText(credentialsPath);
            var parsed = JsonDocument.Parse(json);

            // 🔹 Get project ID
            var projectId = parsed.RootElement.GetProperty("project_id").GetString();

            // 🔹 OPTIONAL: Read Web API key from custom field you must add to JSON
            if (!parsed.RootElement.TryGetProperty("api_key", out var apiKeyElement))
            {
                throw new Exception("Missing 'api_key' in firebase-adminsdk.json. Please add it manually.");
            }

            _apiKey = apiKeyElement.GetString();

            // 🔹 Initialize Firestore
            _firestore = FirestoreDb.Create(projectId);
        }


        [HttpGet]
        public IActionResult Login()
        {
            return View("~/Views/Home/Login.cshtml");
        }

        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Login(LoginViewModel model)
        {
            if (!ModelState.IsValid)
            {
                return View("~/Views/Home/Login.cshtml", model);
            }

            // Replaces _config["Firebase:ApiKey"]
            var client = new HttpClient();
            var payload = new
            {
                email = model.Email,
                password = model.Password,
                returnSecureToken = true
            };

            var content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");
            var response = await client.PostAsync(
                $"https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={_apiKey}",
                content
            );


            if (!response.IsSuccessStatusCode)
            {
                ModelState.AddModelError("", "Invalid email or password.");
                return View("~/Views/Home/Login.cshtml", model);
            }

            // Parse Firebase response
            var json = await response.Content.ReadAsStringAsync();
            using var doc = JsonDocument.Parse(json);
            var userId = doc.RootElement.GetProperty("localId").GetString();

            // Fetch user role from Firestore
            var userDoc = await _firestore.Collection("users").Document(userId).GetSnapshotAsync();

            if (!userDoc.Exists || !userDoc.TryGetValue("role", out string role))
            {
                ModelState.AddModelError("", "User role not found. Please contact admin.");
                return View("~/Views/Home/Login.cshtml", model);
            }

            // Create claims
            var claims = new List<Claim>
            {
                new Claim(ClaimTypes.Name, model.Email),
                new Claim(ClaimTypes.Email, model.Email),
                new Claim(ClaimTypes.Role, role)
            };

            var claimsIdentity = new ClaimsIdentity(claims, CookieAuthenticationDefaults.AuthenticationScheme);
            var claimsPrincipal = new ClaimsPrincipal(claimsIdentity);

            var authProperties = new AuthenticationProperties
            {
                IsPersistent = model.RememberMe,
                ExpiresUtc = DateTimeOffset.UtcNow.AddHours(24)
            };

            // Sign in with cookie auth
            await HttpContext.SignInAsync(
                CookieAuthenticationDefaults.AuthenticationScheme,
                claimsPrincipal,
                authProperties);

            HttpContext.Session.SetString("UserEmail", model.Email);
            HttpContext.Session.SetString("UserRole", role);

            // Redirect to correct dashboard
            return role.ToLower() == "admin"
                ? RedirectToAction("Dashboard", "Admin")
                : RedirectToAction("Dashboard", "Consultant");
        }

        [HttpGet]
        public async Task<IActionResult> Logout()
        {
            await HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
            HttpContext.Session.Clear();
            return RedirectToAction("Login");
        }
        [HttpGet]
        public IActionResult Register()
        {
            return View("~/Views/Home/Register.cshtml");
        }

    }
}
