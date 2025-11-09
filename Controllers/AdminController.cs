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

        public async Task<IActionResult> EscalationManagement()
        {
            
            var collectionRef = _firestore.Collection("flaggedChats");

            // 2. Fetch all reports ordered by latest timestamp
            QuerySnapshot snapshot = await collectionRef
                .OrderByDescending("timestamp")
                .GetSnapshotAsync();

            var reports = new List<FlaggedReportViewModel>();

            // 3. Map Firestore documents to the FlaggedReportViewModel
            foreach (DocumentSnapshot doc in snapshot.Documents)
            {
                if (doc.Exists)
                {
                    Dictionary<string, object> data = doc.ToDictionary();

                    DateTime timestamp = data.TryGetValue("timestamp", out object ts) && ts is Timestamp firestoreTs
                        ? firestoreTs.ToDateTime()
                        : DateTime.MinValue;

                    string conversationJson = "[]";
                    if (data.TryGetValue("conversationSnapshot", out object conversationObj))
                    {
                        try
                        {
                            conversationJson = JsonConvert.SerializeObject(conversationObj);
                        }
                        catch (Exception ex)
                        {
                            Console.WriteLine($"Error serializing conversation snapshot for doc {doc.Id}: {ex.Message}");
                        }
                    }

                    string flaggedByUserId = data.TryGetValue("flaggedByUserId", out object flaggedBy)
                        ? flaggedBy.ToString()
                        : "N/A";

                    // Fetch the username of the flagger from the users collection
                    string flaggedByUsername = "Unknown User";
                    if (!string.IsNullOrEmpty(flaggedByUserId) && flaggedByUserId != "N/A")
                    {
                        try
                        {
                            DocumentSnapshot userDoc = await _firestore
                                .Collection("users")
                                .Document(flaggedByUserId)
                                .GetSnapshotAsync();

                            if (userDoc.Exists && userDoc.ContainsField("username"))
                            {
                                flaggedByUsername = userDoc.GetValue<string>("username");
                            }
                        }
                        catch (Exception ex)
                        {
                            Console.WriteLine($"Error fetching username for user {flaggedByUserId}: {ex.Message}");
                        }
                    }

                    // 4. Create the ViewModel instance
                    reports.Add(new FlaggedReportViewModel
                    {
                        Id = doc.Id,
                        ChatId = data.TryGetValue("chatId", out object chatId) ? chatId.ToString() : "N/A",
                        FlaggedByUserId = flaggedByUserId,
                        FlaggedByUsername = flaggedByUsername, // ✅ new field
                        Status = data.TryGetValue("status", out object status) ? status.ToString() : "N/A",
                        Reason = data.TryGetValue("reason", out object reason) ? reason.ToString() : "Not specified",
                        Timestamp = timestamp,
                        ConversationJson = conversationJson
                    });
                }
            }

            // 5. Pass the list to the view
            return View(reports);
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
    }
}
