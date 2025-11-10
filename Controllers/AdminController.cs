using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Http;
using AcadenceWebApp.Models;
using Google.Cloud.Firestore;
using Google.Cloud.Storage.V1;
using FirebaseAdmin.Auth;
using System.Diagnostics;
using System.Security.Claims;
using Newtonsoft.Json;

namespace AcadenceWebApp.Controllers
{
    public class AdminController : Controller
    {
        private readonly string _projectId = "acadence-40662";
        private readonly string _bucketName;
        private readonly FirestoreDb _firestore;
        private readonly StorageClient _storage;

        public AdminController()
        {
            // Firestore / Storage use GOOGLE_APPLICATION_CREDENTIALS env var (set elsewhere in app)
            _firestore = FirestoreDb.Create(_projectId);
            _storage = StorageClient.Create();

            // Use the exact bucket name shown in your console (from screenshots)
            _bucketName = "acadence-40662.firebasestorage.app";
        }

        // ================== Admin pages ==================
        
        // Replace the existing Dashboard() action with this implementation
        [HttpGet]
        public async Task<IActionResult> Dashboard()
        {
            var vm = new DashboardViewModel();

            try
            {
                // resources
                try
                {
                    var resourcesColl = _firestore.Collection("resources");
                    var resSnap = await resourcesColl.OrderByDescending("uploadedAt").GetSnapshotAsync();
                    vm.ResourcesCount = resSnap.Count;
                    var recentResources = new List<ResourceItemDto>();
                    foreach (var doc in resSnap.Documents.Take(6))
                    {
                        try
                        {
                            doc.TryGetValue("title", out string title);
                            doc.TryGetValue("originalName", out string originalName);
                            doc.TryGetValue("url", out string url);

                            DateTime uploadedAt = DateTime.UtcNow;
                            if (doc.TryGetValue("uploadedAt", out Google.Cloud.Firestore.Timestamp rawTs))
                                uploadedAt = rawTs.ToDateTime();

                            recentResources.Add(new ResourceItemDto
                            {
                                Id = doc.Id,
                                Title = title ?? "",
                                OriginalName = originalName ?? "",
                                Url = url ?? "",
                                UploadedAt = uploadedAt
                            });
                        }
                        catch { }
                    }
                    vm.RecentResources = recentResources;
                }
                catch { vm.ResourcesCount = 0; }

                // support tickets
                try
                {
                    var ticketsColl = _firestore.Collection("supportTickets");
                    var ticketsSnap = await ticketsColl.GetSnapshotAsync();
                    vm.SupportTicketsCount = ticketsSnap.Count;
                }
                catch { vm.SupportTicketsCount = 0; }

                // assignment requests count (for Assign page)
                try
                {
                    var reqSnap = await _firestore.Collection("requests").GetSnapshotAsync();
                    vm.AssignRequestsCount = reqSnap.Count;
                }
                catch { vm.AssignRequestsCount = 0; }

                // feedbacks & metrics
                try
                {
                    var fbColl = _firestore.Collection("feedbacks");
                    var fbSnap = await fbColl.OrderByDescending("timestamp").GetSnapshotAsync();

                    var items = new List<FeedbackItemDto>();
                    var uids = new HashSet<string>();

                    foreach (var doc in fbSnap.Documents)
                    {
                        if (!doc.Exists) continue;

                        doc.TryGetValue("comment", out string comment);
                        doc.TryGetValue("consultantUid", out string consultantUid);
                        doc.TryGetValue("studentUid", out string studentUid);
                        doc.TryGetValue("rating", out int rating);

                        DateTime ts = DateTime.UtcNow;
                        if (doc.TryGetValue("timestamp", out Google.Cloud.Firestore.Timestamp rawTs))
                            ts = rawTs.ToDateTime();

                        items.Add(new FeedbackItemDto
                        {
                            Id = doc.Id,
                            Comment = comment ?? "",
                            ConsultantUid = consultantUid ?? "",
                            StudentUid = studentUid ?? "",
                            Rating = rating,
                            Timestamp = ts
                        });

                        if (!string.IsNullOrEmpty(consultantUid)) uids.Add(consultantUid);
                        if (!string.IsNullOrEmpty(studentUid)) uids.Add(studentUid);
                    }

                    // Resolve names (best-effort)
                    var nameMap = new Dictionary<string, string>();
                    foreach (var uid in uids)
                    {
                        try
                        {
                            var uDoc = await _firestore.Collection("users").Document(uid).GetSnapshotAsync();
                            if (uDoc.Exists)
                            {
                                uDoc.TryGetValue("displayName", out string displayName);
                                uDoc.TryGetValue("username", out string username);
                                uDoc.TryGetValue("email", out string email);
                                nameMap[uid] = !string.IsNullOrEmpty(displayName) ? displayName
                                               : (!string.IsNullOrEmpty(username) ? username : (!string.IsNullOrEmpty(email) ? email : uid));
                            }
                            else
                            {
                                nameMap[uid] = uid;
                            }
                        }
                        catch
                        {
                            nameMap[uid] = uid;
                        }
                    }

                    foreach (var it in items)
                    {
                        it.StudentName = nameMap.ContainsKey(it.StudentUid) ? nameMap[it.StudentUid] : it.StudentUid;
                        it.ConsultantName = nameMap.ContainsKey(it.ConsultantUid) ? nameMap[it.ConsultantUid] : it.ConsultantUid;
                    }

                    vm.TotalFeedbackCount = items.Count;
                    vm.AverageRating = items.Count > 0 ? items.Average(x => (double)x.Rating) : 0;
                    vm.RecentFeedbacks = items.OrderByDescending(x => x.Timestamp).Take(6).ToList();

                    // breakdown for pie chart (same logic as StudentSatisfaction)
                    vm.Satisfied = items.Count(x => x.Rating >= 4);
                    vm.Neutral = items.Count(x => x.Rating == 3);
                    vm.Unsatisfied = items.Count(x => x.Rating <= 2);
                }
                catch { vm.TotalFeedbackCount = 0; vm.AverageRating = 0; vm.RecentFeedbacks = new List<FeedbackItemDto>(); vm.Satisfied = vm.Neutral = vm.Unsatisfied = 0; }
            }
            catch (Exception ex)
            {
                ViewBag.ErrorMessage = "Unable to load dashboard data: " + ex.Message;
            }

            return View(vm);
        }

        // Admin: Consultant Workload
        // Replaced implementation with the provided ConsultantWorkloadController logic
        [HttpGet]
        public async Task<IActionResult> ConsultantWorkload(string searchTerm, string statusFilter,
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
                // Fetch consultants from Firestore (uses helper below)
                var consultants = await GetConsultantsWorkload();

                // Apply filters
                if (!string.IsNullOrEmpty(searchTerm))
                {
                    consultants = consultants.Where(c =>
                        (!string.IsNullOrEmpty(c.ConsultantName) && c.ConsultantName.Contains(searchTerm, StringComparison.OrdinalIgnoreCase)) ||
                        (!string.IsNullOrEmpty(c.Email) && c.Email.Contains(searchTerm, StringComparison.OrdinalIgnoreCase))
                    ).ToList();
                }

                if (!string.IsNullOrEmpty(statusFilter))
                {
                    consultants = consultants.Where(c =>
                        c.Projects.Any(p => string.Equals(p.Status, statusFilter, StringComparison.OrdinalIgnoreCase))
                    ).ToList();
                }

                if (deadlineFrom.HasValue)
                {
                    consultants = consultants.Where(c =>
                        c.NextDeadline.HasValue && c.NextDeadline.Value.Date >= deadlineFrom.Value.Date
                    ).ToList();
                }

                if (deadlineTo.HasValue)
                {
                    consultants = consultants.Where(c =>
                        c.NextDeadline.HasValue && c.NextDeadline.Value.Date <= deadlineTo.Value.Date
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
                ViewBag.Error = "Error loading consultant workload data.";
                Console.WriteLine($"Error: {ex.Message}");
            }

            // Explicit admin view path kept
            return View("~/Views/Admin/ConsultantWorkload.cshtml", viewModel);
        }

        public IActionResult ConsultantAssignment() => View();

        public IActionResult Notifications() => View();

        
        public IActionResult FeedbackLink() => View();
        public IActionResult NotificationsLink() => View();
        // GET: show upload page and existing resources from Firestore (collection "resources")
        [HttpGet]
        public async Task<IActionResult> UploadResource()
        {
            var vm = new UploadResourceViewModel();

            try
            {
                var coll = _firestore.Collection("resources");
                var snap = await coll.GetSnapshotAsync();
                var list = new List<ResourceItemDto>();

                foreach (var doc in snap.Documents)
                {
                    try
                    {
                        var data = doc.ToDictionary();
                        var uploadedAt = DateTime.UtcNow;
                        if (data.ContainsKey("uploadedAt") && data["uploadedAt"] is Timestamp ts)
                        {
                            uploadedAt = ts.ToDateTime();
                        }

                        var item = new ResourceItemDto
                        {
                            Id = doc.Id,
                            Title = data.ContainsKey("title") ? data["title"]?.ToString() ?? "" : "",
                            OriginalName = data.ContainsKey("originalName") ? data["originalName"]?.ToString() ?? "" : "",
                            Url = data.ContainsKey("url") ? data["url"]?.ToString() ?? "" : "",
                            UploadedAt = uploadedAt
                        };
                        list.Add(item);
                    }
                    catch
                    {
                        // ignore per-document parse errors
                    }
                }

                vm.Existing = list.OrderByDescending(r => r.UploadedAt).ToList();
            }
            catch (Exception ex)
            {
                TempData["UploadMsg"] = "Unable to load resources: " + ex.Message;
            }

            return View(vm); // Views/Admin/UploadResource.cshtml
        }

        // POST: upload file to Firebase Storage and save metadata as a Firestore document (collection "resources")
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> UploadResource(UploadResourceViewModel model)
        {
            if (model == null || model.File == null || string.IsNullOrWhiteSpace(model.Title))
            {
                ModelState.AddModelError(string.Empty, "Please provide a file and a title.");
            }

            if (!ModelState.IsValid)
            {
                // re-render with existing list
                return await UploadResource();
            }

            // choose folder 'resources' in storage and a GUID prefixed filename to avoid collisions
            var objectName = $"resources/{Guid.NewGuid():N}_{Path.GetFileName(model.File.FileName)}";

            try
            {
                using var stream = model.File.OpenReadStream();

                // Upload and make publicly readable so the constructed URL works.
                var uploaded = await _storage.UploadObjectAsync(
                    _bucketName,
                    objectName,
                    model.File.ContentType ?? "application/octet-stream",
                    stream,
                    new UploadObjectOptions { PredefinedAcl = PredefinedObjectAcl.PublicRead }
                );

                // Public URL for storage.googleapis.com - works for public objects
                var downloadUrl = $"https://storage.googleapis.com/{_bucketName}/{Uri.EscapeDataString(objectName)}";

                // Save metadata to Firestore collection "resources"
                var doc = new Dictionary<string, object>
                {
                    ["title"] = model.Title,
                    ["originalName"] = model.File.FileName,
                    ["uploadedAt"] = Timestamp.FromDateTime(DateTime.UtcNow),
                    ["url"] = downloadUrl,
                    ["storagePath"] = objectName,
                    ["uploadedBy"] = User?.Identity?.Name ?? HttpContext.Session.GetString("UserEmail") ?? "admin"
                };

                await _firestore.Collection("resources").AddAsync(doc);

                TempData["UploadMsg"] = "Resource uploaded successfully.";
                return RedirectToAction(nameof(UploadResource));
            }
            catch (Exception ex)
            {
                ModelState.AddModelError(string.Empty, "Upload failed: " + ex.Message);
                // re-render with existing list
                return await UploadResource();
            }
        }

        // POST: delete resource by Firestore doc id (removes storage object and Firestore document)
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> DeleteResource(string id)
        {
            if (string.IsNullOrEmpty(id))
            {
                TempData["UploadMsg"] = "Invalid resource id.";
                return RedirectToAction(nameof(UploadResource));
            }

            try
            {
                var docRef = _firestore.Collection("resources").Document(id);
                var snap = await docRef.GetSnapshotAsync();
                if (snap.Exists)
                {
                    snap.TryGetValue("storagePath", out string storagePath);

                    if (!string.IsNullOrEmpty(storagePath))
                    {
                        try
                        {
                            _storage.DeleteObject(_bucketName, storagePath);
                        }
                        catch
                        {
                            // ignore deletion errors for storage but continue to delete doc
                        }
                    }

                    await docRef.DeleteAsync();
                    TempData["UploadMsg"] = "Resource deleted.";
                }
                else
                {
                    TempData["UploadMsg"] = "Resource not found.";
                }
            }
            catch (Exception ex)
            {
                TempData["UploadMsg"] = "Error deleting resource: " + ex.Message;
            }

            return RedirectToAction(nameof(UploadResource));
        }

        
        // GET: Teacher feedbacks overview
        [HttpGet]
        public async Task<IActionResult> Feedback()
        {
            var list = new List<FeedbackItemDto>();

            try
            {
                var coll = _firestore.Collection("feedbacks");
                var snap = await coll.OrderByDescending("timestamp").GetSnapshotAsync();

                // collect UIDs to resolve display names
                var uids = new HashSet<string>();
                foreach (var d in snap.Documents)
                {
                    if (!d.Exists) continue;
                    if (d.TryGetValue("studentUid", out string sUid) && !string.IsNullOrEmpty(sUid)) uids.Add(sUid);
                    if (d.TryGetValue("consultantUid", out string cUid) && !string.IsNullOrEmpty(cUid)) uids.Add(cUid);
                }

                // resolve names from users collection
                var nameMap = new Dictionary<string, string>();
                foreach (var uid in uids)
                {
                    try
                    {
                        var uDoc = await _firestore.Collection("users").Document(uid).GetSnapshotAsync();
                        if (uDoc.Exists)
                        {
                            uDoc.TryGetValue("displayName", out string displayName);
                            uDoc.TryGetValue("username", out string username);
                            uDoc.TryGetValue("email", out string email);
                            nameMap[uid] = !string.IsNullOrEmpty(displayName) ? displayName
                                           : !string.IsNullOrEmpty(username) ? username
                                           : (!string.IsNullOrEmpty(email) ? email : uid);
                        }
                        else
                        {
                            nameMap[uid] = uid;
                        }
                    }
                    catch
                    {
                        nameMap[uid] = uid;
                    }
                }

                // map feedback documents to DTOs
                foreach (var d in snap.Documents)
                {
                    if (!d.Exists) continue;
                    try
                    {
                        d.TryGetValue("comment", out string comment);
                        d.TryGetValue("consultantUid", out string consultantUid);
                        d.TryGetValue("studentUid", out string studentUid);
                        d.TryGetValue("rating", out int rating);

                        DateTime ts = DateTime.UtcNow;
                        if (d.TryGetValue("timestamp", out Google.Cloud.Firestore.Timestamp rawTs))
                        {
                            ts = rawTs.ToDateTime();
                        }

                        var item = new FeedbackItemDto
                        {
                            Id = d.Id,
                            Comment = comment ?? "",
                            ConsultantUid = consultantUid ?? "",
                            ConsultantName = nameMap.ContainsKey(consultantUid ?? "") ? nameMap[consultantUid ?? ""] : (consultantUid ?? ""),
                            StudentUid = studentUid ?? "",
                            StudentName = nameMap.ContainsKey(studentUid ?? "") ? nameMap[studentUid ?? ""] : (studentUid ?? ""),
                            Rating = rating,
                            Timestamp = ts
                        };

                        list.Add(item);
                    }
                    catch
                    {
                        // ignore per-doc parse errors
                    }
                }
            }
            catch (Exception ex)
            {
                ViewBag.ErrorMessage = "Unable to load feedbacks: " + ex.Message;
            }

            return View(list);
        }

        // GET: /Admin/EscalationManagement
        [HttpGet]
        public async Task<IActionResult> EscalationManagement()
        {
            var tickets = new List<SupportTicketDto>();
            try
            {
                var coll = _firestore.Collection("supportTickets");
                var snap = await coll.OrderByDescending("timestamp").GetSnapshotAsync();

                foreach (var doc in snap.Documents)
                {
                    if (!doc.Exists) continue;
                    try
                    {
                        doc.TryGetValue("initialMessage", out string initialMessage);
                        doc.TryGetValue("studentId", out string studentId);
                        doc.TryGetValue("studentName", out string studentName);
                        doc.TryGetValue("status", out string status);

                        DateTime ts = DateTime.UtcNow;
                        if (doc.TryGetValue("timestamp", out Timestamp rawTs))
                        {
                            ts = rawTs.ToDateTime();
                        }

                        tickets.Add(new SupportTicketDto
                        {
                            Id = doc.Id,
                            InitialMessage = initialMessage ?? "",
                            StudentId = studentId ?? "",
                            StudentName = studentName ?? "",
                            Status = string.IsNullOrEmpty(status) ? "pending" : status,
                            Timestamp = ts
                        });
                    }
                    catch
                    {
                        // ignore per-doc parse errors
                    }
                }
            }
            catch (Exception ex)
            {
                ViewBag.ErrorMessage = "Unable to load support tickets: " + ex.Message;
            }

            return View(tickets.OrderByDescending(t => t.Timestamp).ToList());
        }

        // POST: /Admin/UpdateTicketStatus
        [HttpPost]
        public async Task<IActionResult> UpdateTicketStatus([FromBody] TicketStatusUpdateDto dto)
        {
            if (dto == null || string.IsNullOrEmpty(dto.TicketId) || string.IsNullOrEmpty(dto.Status))
            {
                return Json(new ApiResponse { Success = false, Message = "Invalid payload" });
            }

            try
            {
                var docRef = _firestore.Collection("supportTickets").Document(dto.TicketId);
                var updates = new Dictionary<string, object>
                {
                    ["status"] = dto.Status,
                    ["updatedAt"] = Timestamp.FromDateTime(DateTime.UtcNow)
                };

                await docRef.UpdateAsync(updates);
                return Json(new ApiResponse { Success = true, Message = "Updated" });
            }
            catch (Exception ex)
            {
                return Json(new ApiResponse { Success = false, Message = "Error updating ticket status: " + ex.Message });
            }
        }

        // update the StudentSatisfaction action to load real feedback data
        [HttpGet]
        public async Task<IActionResult> StudentSatisfaction()
        {
            var vm = new StudentSatisfactionViewModel();

            try
            {
                var coll = _firestore.Collection("feedbacks");
                var snap = await coll.OrderByDescending("timestamp").GetSnapshotAsync();

                var items = new List<FeedbackItemDto>();
                var uids = new HashSet<string>();

                foreach (var doc in snap.Documents)
                {
                    if (!doc.Exists) continue;

                    doc.TryGetValue("comment", out string comment);
                    doc.TryGetValue("consultantUid", out string consultantUid);
                    doc.TryGetValue("studentUid", out string studentUid);
                    doc.TryGetValue("rating", out int rating);

                    Google.Cloud.Firestore.Timestamp rawTs = default;
                    DateTime ts = DateTime.UtcNow;
                    if (doc.TryGetValue("timestamp", out rawTs) && rawTs != null)
                    {
                        ts = rawTs.ToDateTime();
                    }

                    var item = new FeedbackItemDto
                    {
                        Id = doc.Id,
                        Comment = comment ?? "",
                        ConsultantUid = consultantUid ?? "",
                        StudentUid = studentUid ?? "",
                        Rating = rating,
                        Timestamp = ts
                    };

                    items.Add(item);

                    if (!string.IsNullOrEmpty(consultantUid)) uids.Add(consultantUid);
                    if (!string.IsNullOrEmpty(studentUid)) uids.Add(studentUid);
                }

                // resolve user display names
                var nameMap = new Dictionary<string, string>();
                foreach (var uid in uids)
                {
                    try
                    {
                        var uDoc = await _firestore.Collection("users").Document(uid).GetSnapshotAsync();
                        if (uDoc.Exists)
                        {
                            uDoc.TryGetValue("displayName", out string displayName);
                            uDoc.TryGetValue("username", out string username);
                            uDoc.TryGetValue("email", out string email);
                            nameMap[uid] = !string.IsNullOrEmpty(displayName) ? displayName
                                           : (!string.IsNullOrEmpty(username) ? username : (!string.IsNullOrEmpty(email) ? email : uid));
                        }
                        else
                        {
                            nameMap[uid] = uid;
                        }
                    }
                    catch
                    {
                        nameMap[uid] = uid;
                    }
                }

                // set resolved names
                foreach (var it in items)
                {
                    it.StudentName = nameMap.ContainsKey(it.StudentUid) ? nameMap[it.StudentUid] : it.StudentUid;
                    it.ConsultantName = nameMap.ContainsKey(it.ConsultantUid) ? nameMap[it.ConsultantUid] : it.ConsultantUid;
                }

                vm.TotalFeedbackCount = items.Count;
                vm.AverageRating = items.Count > 0 ? items.Average(x => (double)x.Rating) : 0;
                vm.Satisfied = items.Count(x => x.Rating >= 4);
                vm.Neutral = items.Count(x => x.Rating == 3);
                vm.Unsatisfied = items.Count(x => x.Rating <= 2);
                vm.RecentFeedbacks = items.OrderByDescending(x => x.Timestamp).Take(12).ToList();
            }
            catch (Exception ex)
            {
                ViewBag.ErrorMessage = "Unable to load student satisfaction: " + ex.Message;
            }

            return View(vm);
        }

        // --- Helper: fetch consultants + projects and compute metrics (simpler version requested) ---
        private async Task<List<ConsultantWorkload>> GetConsultantsWorkload()
        {
            var consultants = new List<ConsultantWorkload>();

            // Fetch from "consultants" collection
            var consultantsRef = _firestore.Collection("consultants");
            var snapshot = await consultantsRef.GetSnapshotAsync();

            foreach (var document in snapshot.Documents)
            {
                var consultant = new ConsultantWorkload
                {
                    ConsultantId = document.Id,
                    ConsultantName = document.ContainsField("name") ? document.GetValue<string>("name") : "",
                    Email = document.ContainsField("email") ? document.GetValue<string>("email") : ""
                };

                // Fetch projects for this consultant
                var projectsRef = _firestore.Collection("projects")
                    .WhereEqualTo("consultantId", consultant.ConsultantId);
                var projectsSnapshot = await projectsRef.GetSnapshotAsync();

                foreach (var projectDoc in projectsSnapshot.Documents)
                {
                    var project = new ProjectDetail
                    {
                        ProjectId = projectDoc.Id,
                        ProjectName = projectDoc.ContainsField("name") ? projectDoc.GetValue<string>("name") : "",
                        Status = projectDoc.ContainsField("status") ? projectDoc.GetValue<string>("status") : "",
                        Deadline = projectDoc.ContainsField("deadline")
                            ? projectDoc.GetValue<DateTime?>("deadline")
                            : null,
                        TasksAssigned = projectDoc.ContainsField("tasksAssigned") ? projectDoc.GetValue<int>("tasksAssigned") : 0,
                        TasksCompleted = projectDoc.ContainsField("tasksCompleted") ? projectDoc.GetValue<int>("tasksCompleted") : 0
                    };

                    consultant.Projects.Add(project);
                }

                // Calculate workload metrics
                consultant.ActiveProjects = consultant.Projects.Count(p => string.Equals(p.Status, "Active", StringComparison.OrdinalIgnoreCase));
                consultant.TotalTasks = consultant.Projects.Sum(p => p.TasksAssigned);
                consultant.CompletedTasks = consultant.Projects.Sum(p => p.TasksCompleted);
                consultant.PendingTasks = consultant.TotalTasks - consultant.CompletedTasks;
                consultant.NextDeadline = consultant.Projects
                    .Where(p => p.Deadline.HasValue && p.Deadline > DateTime.Now)
                    .OrderBy(p => p.Deadline)
                    .FirstOrDefault()?.Deadline;

                consultant.WorkloadPercentage = consultant.TotalTasks > 0
                    ? Math.Round((double)consultant.PendingTasks / consultant.TotalTasks * 100, 1)
                    : 0;

                consultants.Add(consultant);
            }

            return consultants;
        }
    }
}
