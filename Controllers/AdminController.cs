using Microsoft.AspNetCore.Mvc;

namespace AcadenceWebApp.Controllers
{
    public class AdminController : Controller
    {
        public IActionResult Dashboard()
        {
            return View();
        }

        public IActionResult StudentSatisfaction()
        {
            return View();
        }

        public IActionResult ConsultantWorkload()
        {
            return View();
        }

        public IActionResult ConsultantAssignment()
        {
            return View();
        }

        public IActionResult Notifications()
        {
            return View();
        }

        public IActionResult UploadResource()
        {
            return View();
        }

        public IActionResult EscalationManagement()
        {
            return View();
        }
    }
}
