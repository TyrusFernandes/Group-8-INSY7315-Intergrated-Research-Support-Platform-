using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Http;
using AcadenceWebApp.Models;

namespace AcadenceWebApp.Controllers
{
    public class AdminController : Controller
    {

        // ================== Admin pages ==================
        public IActionResult Dashboard() => View();

        // Student Satisfaction Overview (returns a model to the view)
        public IActionResult StudentSatisfaction()
        {
            
            var vm = new StudentSatisfactionViewModel
            {
                Satisfied = 72,
                Neutral = 18,
                Unsatisfied = 10
            };
            return View(vm); // Views/Admin/StudentSatisfaction.cshtml
        }

   
        public IActionResult ConsultantWorkload() => View();

        public IActionResult ConsultantAssignment() => View();

        public IActionResult Notifications() => View();

    }
}
