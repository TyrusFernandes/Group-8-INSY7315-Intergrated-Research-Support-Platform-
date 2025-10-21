using Microsoft.AspNetCore.Mvc;

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

        public IActionResult Messages()
        {
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
