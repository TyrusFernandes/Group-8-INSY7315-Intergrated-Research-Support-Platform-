using Microsoft.AspNetCore.Mvc;

public class TaskController : Controller
{
    public IActionResult Index()
    {
        var role = HttpContext.Session.GetString("UserRole");

        if (role == "Admin")
            return View("Admin");
        else if (role == "Consultant")
            return View("Consultant");
        else
            return RedirectToAction("Login", "Account"); // fallback
    }
}
