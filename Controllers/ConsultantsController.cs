using AcadenceWebApp.Models;
using Microsoft.AspNetCore.Mvc;
using System.Linq;
using System.Collections.Generic;

namespace AcadenceWebApp.Controllers
{
    public class ConsultantsController : Controller
    {
        private static readonly List<ConsultantDto> _seed = new()
        {
            new ConsultantDto { Name = "Alex Mokoena",   ActiveProjects = 5, Department = "Health",    Region = "GP" },
            new ConsultantDto { Name = "Lerato Dlamini",  ActiveProjects = 8, Department = "Education", Region = "WC" },
            new ConsultantDto { Name = "Thabo Nkosi",     ActiveProjects = 3, Department = "Health",    Region = "GP" },
            new ConsultantDto { Name = "Nomsa Khumalo",   ActiveProjects = 6, Department = "Finance",   Region = "KZN" },
            new ConsultantDto { Name = "Sipho Zulu",      ActiveProjects = 4, Department = "Education", Region = "GP" },
        };

        [HttpGet]
        public IActionResult Workload()
        {
            var vm = new ConsultantWorkloadViewModel
            {
                Consultants = _seed.OrderByDescending(c => c.ActiveProjects).ToList()
            };
            vm.TotalActiveProjects = vm.Consultants.Sum(c => c.ActiveProjects);
            vm.AverageActiveProjects = vm.Consultants.Any()
                ? vm.TotalActiveProjects / (double)vm.Consultants.Count
                : 0;
            vm.Busiest = vm.Consultants
                .OrderByDescending(c => c.ActiveProjects)
                .FirstOrDefault();

            // This will look for Views/Consultants/Workload.cshtml
            return View("~/Views/Admin/ConsultantWorkload.cshtml", vm);

        }
    }
}
