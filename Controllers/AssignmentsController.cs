using Microsoft.AspNetCore.Mvc;
using AcadenceWebApp.Models;
using System.Collections.Generic;
using System.Linq;

namespace AcadenceWebApp.Controllers
{
    public class AssignmentsController : Controller
    {
        // Demo data (swap for EF Core later)
        private static List<ServiceRequestDto> _requests = new()
        {
            new ServiceRequestDto { Id = "R1", StudentName = "Student A", Category = "Editing" },
            new ServiceRequestDto { Id = "R2", StudentName = "Student B", Category = "Consultation" },
            new ServiceRequestDto { Id = "R3", StudentName = "Student C", Category = "Coaching" },
        };

        private static List<ConsultantDto> _consultants = new()
        {
            new ConsultantDto { Id = "C1", Name = "Dr. Smith",   Department = "Education", Region = "GP",  ActiveProjects = 5 },
            new ConsultantDto { Id = "C2", Name = "Ms. Luthuli", Department = "Coaching",  Region = "WC",  ActiveProjects = 7 },
            new ConsultantDto { Id = "C3", Name = "Prof. Zwide", Department = "Research",  Region = "KZN", ActiveProjects = 4 },
        };

        [HttpGet]
        public IActionResult Assign()
        {
            // ensure there is at least some OPEN data for testing
            if (!_requests.Any(r => r.Status == "Open"))
            {
                _requests = new()
                {
                    new ServiceRequestDto { Id = "R1", StudentName = "Student A", Category = "Editing" },
                    new ServiceRequestDto { Id = "R2", StudentName = "Student B", Category = "Consultation" },
                    new ServiceRequestDto { Id = "R3", StudentName = "Student C", Category = "Coaching" },
                };
            }

            var vm = new ConsultantAssignmentViewModel
            {
                Requests = _requests.Where(r => r.Status == "Open").ToList(),
                Consultants = _consultants.OrderBy(c => c.Name).ToList()
            };

            // 👈 IMPORTANT: explicit path since your view is under Admin
            return View("~/Views/Admin/Assign.cshtml", vm);
        }

        [HttpPost]
        [ValidateAntiForgeryToken]
        public IActionResult Assign(ConsultantAssignmentViewModel vm)
        {
            if (!ModelState.IsValid)
            {
                vm.Requests = _requests.Where(r => r.Status == "Open").ToList();
                vm.Consultants = _consultants.OrderBy(c => c.Name).ToList();
                return View("~/Views/Admin/Assign.cshtml", vm);
            }

            var req = _requests.FirstOrDefault(r => r.Id == vm.SelectedRequestId);
            var con = _consultants.FirstOrDefault(c => c.Id == vm.SelectedConsultantId);

            if (req is null || con is null)
            {
                ModelState.AddModelError("", "Invalid request or consultant selection.");
                vm.Requests = _requests.Where(r => r.Status == "Open").ToList();
                vm.Consultants = _consultants.OrderBy(c => c.Name).ToList();
                return View("~/Views/Admin/Assign.cshtml", vm);
            }

            req.Status = "Assigned";
            con.ActiveProjects += 1;
            TempData["AssignmentMsg"] = $"Assigned {req.Title} to {con.Name}.";
            return RedirectToAction(nameof(Assign));
        }
    }
}
