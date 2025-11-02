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
       
        private static readonly List<AcademicResourceDto> _store = new();

        private static readonly List<EscalationItemDto> _escalations = new()
        {
            new EscalationItemDto { StudentName = "Student A", Issue = "Clarification delay", Severity = "Medium", Status = "Open" },
            new EscalationItemDto { StudentName = "Student D", Issue = "Missed Deadline",     Severity = "High",   Status = "Open" },
        };

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

        // ================== Escalation Management ==================
        [HttpGet]
        public IActionResult EscalationManagement()
        {
            var vm = new EscalationConsoleViewModel
            {
                Items = _escalations
                    .OrderByDescending(e => e.CreatedAt)
                    .ToList()
            };
            return View(vm); // Views/Admin/EscalationManagement.cshtml
        }

        [HttpGet]
        public IActionResult EscalationDetails(string id)
        {
            var item = _escalations.FirstOrDefault(e => e.Id == id);
            if (item == null) return NotFound();

            return View("~/Views/Admin/EscalationDetails.cshtml", item);
        }

        // ================== Upload Academic Resource ==================
        [HttpGet]
        public IActionResult UploadResource()
        {
            var vm = new UploadResourceViewModel
            {
                Existing = _store.OrderByDescending(r => r.UploadedAt).ToList()
            };
            return View(vm); // Views/Admin/UploadResource.cshtml
        }

        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> UploadResource(UploadResourceViewModel vm)
        {
            vm.Existing = _store.OrderByDescending(r => r.UploadedAt).ToList();

            if (!ModelState.IsValid)
                return View(vm);

            if (vm.File == null || vm.File.Length == 0)
            {
                ModelState.AddModelError("File", "Please choose a file to upload.");
                return View(vm);
            }

        
            var webRoot = Path.Combine(Directory.GetCurrentDirectory(), "wwwroot");
            var resourcesPath = Path.Combine(webRoot, "resources");
            if (!Directory.Exists(resourcesPath))
                Directory.CreateDirectory(resourcesPath);

       
            var safeOriginal = Path.GetFileName(vm.File.FileName);
            var ext = Path.GetExtension(safeOriginal);
            var uniqueName = $"{Guid.NewGuid():N}{ext}";
            var savePath = Path.Combine(resourcesPath, uniqueName);

            using (var stream = new FileStream(savePath, FileMode.Create))
            {
                await vm.File.CopyToAsync(stream);
            }

            // Store metadata
            var dto = new AcademicResourceDto
            {
                Title = vm.Title ?? "",
                FileName = uniqueName,
                OriginalName = safeOriginal,
                Url = $"/resources/{uniqueName}",
                UploadedAt = DateTime.UtcNow
            };
            _store.Add(dto);

            TempData["UploadMsg"] = $"Uploaded “{dto.Title}”.";
            return RedirectToAction(nameof(UploadResource));
        }

        [HttpPost]
        [ValidateAntiForgeryToken]
        public IActionResult DeleteResource(string id)
        {
            var item = _store.FirstOrDefault(x => x.Id == id);
            if (item != null)
            {
                var webRoot = Path.Combine(Directory.GetCurrentDirectory(), "wwwroot");
                var filePath = Path.Combine(webRoot, "resources", item.FileName);
                if (System.IO.File.Exists(filePath))
                    System.IO.File.Delete(filePath);

                _store.Remove(item);
                TempData["UploadMsg"] = $"Deleted “{item.Title}”.";
            }

            return RedirectToAction(nameof(UploadResource));
        }
    }
}
