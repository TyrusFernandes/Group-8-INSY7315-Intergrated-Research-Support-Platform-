using FirebaseAdmin.Auth;
using Microsoft.AspNetCore.Mvc;
using System.Threading.Tasks;
namespace AcadenceWebApp.Controllers
{
    public class ConsultantController : Controller
    {
        public IActionResult Dashboard()
        {
            return View();
        }
        public IActionResult AssignedTasks()
        {
            return View();
        }
        public IActionResult ResourceLibrary()
        {
            return View();
        }
        public async Task<IActionResult> MessagesAsync()
        {
            // 1. Get the current application user's ID (Replace with your actual ASP.NET Identity logic)
            string currentAppUserId = User.Identity.IsAuthenticated ? User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value : "anonymous_user_123";
            // --- ⚠️ CONFIGURATION: REPLACE THESE WITH YOUR ACTUAL VALUES ⚠️ ---
            // This is the CRITICAL Firebase SDK JSON CONFIGURATION. 
            // Get these values from your Firebase Project Settings -> Web App Setup.
            // It must be a valid JSON string.
            string firebaseConfigJson = @"{
           apiKey: ""AIzaSyB8fUMWYN29wYg0YOvhW5NBUzstzpHD7pY"",
           authDomain: ""acadence-40662.firebaseapp.com"",
           projectId: ""acadence-40662"",
           storageBucket: ""acadence-40662.firebasestorage.app"",
           messagingSenderId: ""907171020913"",
           appId: ""1:907171020913:web:46cb92eea7b9ed0d28dbd0""
        }";
            // Your Canvas Application ID (Used for Firestore paths)
            string canvasAppId = "YOUR_CANVAS_APP_ID";

            // 2. Generate the Firebase Custom Auth Token
            string firebaseCustomToken = await FirebaseAuth.DefaultInstance
                .CreateCustomTokenAsync(currentAppUserId);
            // 3. Pass the necessary variables to the view
            ViewBag.InitialAuthToken = firebaseCustomToken;
            ViewBag.AppId = canvasAppId;
            ViewBag.FirebaseConfig = firebaseConfigJson;
            return View();
        }
        public IActionResult MeetingCalendar()
        {
            return View();
        }
        public IActionResult ProgressReport()
        {
            return View();
        }
    }
}