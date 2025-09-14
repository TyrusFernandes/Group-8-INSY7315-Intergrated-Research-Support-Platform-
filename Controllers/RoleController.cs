// Controllers/RoleController.cs
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Http;

public class RoleController : Controller
{
    public IActionResult SetRole(string role)
    {
        HttpContext.Session.SetString("UserRole", role);

        if (role == "Admin")
            return RedirectToAction("Dashboard", "Admin");
        else if (role == "Consultant")
            return RedirectToAction("Dashboard", "Consultant");
        else
            return RedirectToAction("Index", "Home");
    }
}
