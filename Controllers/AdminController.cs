using Microsoft.AspNetCore.Mvc;

namespace AcadenceWebApp.Controllers
{
    public class AdminController : Controller
    {
        public IActionResult Dashboard()
        {
            return View();
        }

        // You can add more views later, like:
        // public IActionResult TaskList() { return View(); }
    }
}
