using Microsoft.AspNetCore.Mvc;

namespace AcadenceWebApp.Controllers
{
    public class ConsultantController : Controller
    {
        public IActionResult Dashboard()
        {
            return View();
        }
    }
}
