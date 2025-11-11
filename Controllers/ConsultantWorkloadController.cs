using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using AcadenceWebApp.Models;
using Google.Cloud.Firestore;

namespace AcadenceWebApp.Controllers
{
    [Route("ConsultantWorkload")]
    public class ConsultantWorkloadController : Controller
    {
        private readonly FirestoreDb _firestoreDb;
        private const string ProjectId = "acadence-40662";

        public ConsultantWorkloadController()
        {
            // Use same project id as other controllers to avoid mismatches
            _firestoreDb = FirestoreDb.Create(ProjectId);
        }

        [HttpGet]
        public async Task<IActionResult> Index(string searchTerm, string statusFilter,
            DateTime? deadlineFrom, DateTime? deadlineTo, string sortBy = "Name")
        {
            var viewModel = new ConsultantWorkloadViewModel
            {
                SearchTerm = searchTerm,
                StatusFilter = statusFilter,
                DeadlineFrom = deadlineFrom,
                DeadlineTo = deadlineTo,
                SortBy = sortBy
            };

            try
            {
                var consultants = await GetConsultantsWorkload();

                if (!string.IsNullOrWhiteSpace(searchTerm))
                {
                    var st = searchTerm.Trim();
                    consultants = consultants.Where(c =>
                        (!string.IsNullOrEmpty(c.ConsultantName) && c.ConsultantName.Contains(st, StringComparison.OrdinalIgnoreCase)) ||
                        (!string.IsNullOrEmpty(c.Email) && c.Email.Contains(st, StringComparison.OrdinalIgnoreCase))
                    ).ToList();
                }

                viewModel.Consultants = consultants;
            }
            catch (Exception ex)
            {
                ViewBag.Error = "Error loading consultant workload data.";
                Console.WriteLine(ex);
            }

            // Render the Admin view file explicitly so Index() does not try to find /Views/ConsultantWorkload/Index.cshtml
            return View("~/Views/Admin/ConsultantWorkload.cshtml", viewModel);
        }

        private async Task<List<ConsultantWorkload>> GetConsultantsWorkload()
        {
            var consultants = new List<ConsultantWorkload>();

            var consultantsRef = _firestoreDb.Collection("consultants");
            var snapshot = await consultantsRef.GetSnapshotAsync();

            foreach (var document in snapshot.Documents)
            {
                try
                {
                    var consultant = new ConsultantWorkload
                    {
                        ConsultantId = document.Id,
                        ConsultantName = document.ContainsField("name") ? document.GetValue<string>("name") : (document.ContainsField("displayName") ? document.GetValue<string>("displayName") : ""),
                        Email = document.ContainsField("email") ? document.GetValue<string>("email") : ""
                    };

                    // populate Projects if you have them (safe guards)
                    consultant.Projects = new List<ProjectDetail>();
                    var projectsRef = _firestoreDb.Collection("projects").WhereEqualTo("consultantId", consultant.ConsultantId);
                    var projectsSnapshot = await projectsRef.GetSnapshotAsync();
                    foreach (var projectDoc in projectsSnapshot.Documents)
                    {
                        try
                        {
                            var project = new ProjectDetail
                            {
                                ProjectId = projectDoc.Id,
                                ProjectName = projectDoc.ContainsField("name") ? projectDoc.GetValue<string>("name") : "",
                                Status = projectDoc.ContainsField("status") ? projectDoc.GetValue<string>("status") : "",
                                Deadline = projectDoc.ContainsField("deadline") ? projectDoc.GetValue<DateTime?>("deadline") : null,
                                TasksAssigned = projectDoc.ContainsField("tasksAssigned") ? projectDoc.GetValue<int>("tasksAssigned") : 0,
                                TasksCompleted = projectDoc.ContainsField("tasksCompleted") ? projectDoc.GetValue<int>("tasksCompleted") : 0
                            };
                            consultant.Projects.Add(project);
                        }
                        catch { }
                    }

                    // computed metrics (defensive)
                    consultant.ActiveProjects = consultant.Projects.Count(p => string.Equals(p.Status, "Active", StringComparison.OrdinalIgnoreCase));
                    consultant.TotalTasks = consultant.Projects.Sum(p => p.TasksAssigned);
                    consultant.CompletedTasks = consultant.Projects.Sum(p => p.TasksCompleted);
                    consultant.PendingTasks = consultant.TotalTasks - consultant.CompletedTasks;
                    consultant.NextDeadline = consultant.Projects.Where(p => p.Deadline.HasValue && p.Deadline > DateTime.UtcNow).OrderBy(p => p.Deadline).FirstOrDefault()?.Deadline;
                    consultant.WorkloadPercentage = consultant.TotalTasks > 0 ? Math.Round((double)consultant.PendingTasks / consultant.TotalTasks * 100, 1) : 0;

                    // ensure StudentRequests exists so Admin view can use it (left empty)
                    if (consultant.StudentRequests == null) consultant.StudentRequests = new List<StudentRequest>();

                    consultants.Add(consultant);
                }
                catch
                {
                    // ignore per-consultant parse errors
                }
            }

            return consultants;
        }
    }
}
