using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using AcadenceWebApp.Models;
using FirebaseAdmin;
using FirebaseAdmin.Auth;
using Google.Cloud.Firestore;

namespace AcadenceWebApp.Controllers
{
    [Route("ConsultantWorkload")]
    public class ConsultantWorkloadController : Controller
    {
        private readonly FirestoreDb _firestoreDb;

        public ConsultantWorkloadController()
        {
            // TODO: Initialize Firebase in Program.cs or Startup.cs
            // This assumes Firebase is already initialized
            // Add your Firebase project ID here
            _firestoreDb = FirestoreDb.Create("your-firebase-project-id");
        }

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
                // Fetch consultants from Firebase
                var consultants = await GetConsultantsWorkload();

                // Apply filters
                if (!string.IsNullOrEmpty(searchTerm))
                {
                    consultants = consultants.Where(c =>
                        c.ConsultantName.Contains(searchTerm, StringComparison.OrdinalIgnoreCase) ||
                        c.Email.Contains(searchTerm, StringComparison.OrdinalIgnoreCase)
                    ).ToList();
                }

                if (!string.IsNullOrEmpty(statusFilter))
                {
                    consultants = consultants.Where(c =>
                        c.Projects.Any(p => p.Status == statusFilter)
                    ).ToList();
                }

                if (deadlineFrom.HasValue)
                {
                    consultants = consultants.Where(c =>
                        c.NextDeadline >= deadlineFrom.Value
                    ).ToList();
                }

                if (deadlineTo.HasValue)
                {
                    consultants = consultants.Where(c =>
                        c.NextDeadline <= deadlineTo.Value
                    ).ToList();
                }

                // Apply sorting
                consultants = sortBy switch
                {
                    "Workload" => consultants.OrderByDescending(c => c.WorkloadPercentage).ToList(),
                    "Projects" => consultants.OrderByDescending(c => c.ActiveProjects).ToList(),
                    "Deadline" => consultants.OrderBy(c => c.NextDeadline ?? DateTime.MaxValue).ToList(),
                    _ => consultants.OrderBy(c => c.ConsultantName).ToList()
                };

                viewModel.Consultants = consultants;
            }
            catch (Exception ex)
            {
                // Log error and show empty list
                ViewBag.Error = "Error loading consultant workload data.";
                Console.WriteLine($"Error: {ex.Message}");
            }

            return View(viewModel);
        }

        private async Task<List<ConsultantWorkload>> GetConsultantsWorkload()
        {
            var consultants = new List<ConsultantWorkload>();

            // TODO: Update these collection names to match your Firebase structure
            // Example: Fetch from "consultants" collection
            var consultantsRef = _firestoreDb.Collection("consultants");
            var snapshot = await consultantsRef.GetSnapshotAsync();

            foreach (var document in snapshot.Documents)
            {
                var consultant = new ConsultantWorkload
                {
                    ConsultantId = document.Id,
                    ConsultantName = document.GetValue<string>("name"),
                    Email = document.GetValue<string>("email")
                };

                // Fetch projects for this consultant
                // TODO: Update to match your Firebase structure
                var projectsRef = _firestoreDb.Collection("projects")
                    .WhereEqualTo("consultantId", consultant.ConsultantId);
                var projectsSnapshot = await projectsRef.GetSnapshotAsync();

                foreach (var projectDoc in projectsSnapshot.Documents)
                {
                    var project = new ProjectDetail
                    {
                        ProjectId = projectDoc.Id,
                        ProjectName = projectDoc.GetValue<string>("name"),
                        Status = projectDoc.GetValue<string>("status"),
                        Deadline = projectDoc.ContainsField("deadline")
                            ? projectDoc.GetValue<DateTime?>("deadline")
                            : null,
                        TasksAssigned = projectDoc.GetValue<int>("tasksAssigned"),
                        TasksCompleted = projectDoc.GetValue<int>("tasksCompleted")
                    };

                    consultant.Projects.Add(project);
                }

                // Calculate workload metrics
                consultant.ActiveProjects = consultant.Projects.Count(p => p.Status == "Active");
                consultant.TotalTasks = consultant.Projects.Sum(p => p.TasksAssigned);
                consultant.CompletedTasks = consultant.Projects.Sum(p => p.TasksCompleted);
                consultant.PendingTasks = consultant.TotalTasks - consultant.CompletedTasks;
                consultant.NextDeadline = consultant.Projects
                    .Where(p => p.Deadline.HasValue && p.Deadline > DateTime.Now)
                    .OrderBy(p => p.Deadline)
                    .FirstOrDefault()?.Deadline;

                // Calculate workload percentage (you can adjust this logic)
                consultant.WorkloadPercentage = consultant.TotalTasks > 0
                    ? Math.Round((double)consultant.PendingTasks / consultant.TotalTasks * 100, 1)
                    : 0;

                consultants.Add(consultant);
            }

            return consultants;
        }
    }
}
