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

        // Escalation management route (keeps existing behavior)
        [HttpGet]
        public IActionResult EscalationManagement()
        {
            return View(); // Views/Admin/EscalationManagement.cshtml
        }
    }
}
