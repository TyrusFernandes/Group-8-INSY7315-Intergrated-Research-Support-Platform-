using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using AcadenceWebApp.Models;
using AcadenceWebApp.Models;
using Google.Cloud.Firestore;
using Google.Cloud.Storage.V1;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;

namespace AcadenceWebApp.Controllers
{
    [Authorize]
    public class ProfileController : Controller
    {
        private readonly FirestoreDb _firestoreDb;
        private readonly string _bucketName = "acadence-40662.appspot.com"; 
        private readonly string _projectId = "acadence-40662"; 

        public ProfileController()
        {
            // Initialize Firestore - Firebase is already initialized in Program.cs
            _firestoreDb = FirestoreDb.Create(_projectId);
        }

        // GET: Profile/Index
        [HttpGet]
        public async Task<IActionResult> Index()
        {
            try
            {
                var userEmail = User.Identity.Name;

                // Get user document from Firestore
                var userDoc = await _firestoreDb.Collection("users").Document(userEmail).GetSnapshotAsync();

                var model = new ProfileViewModel();

                if (userDoc.Exists)
                {
                    var data = userDoc.ToDictionary();
                    model.UserId = userEmail;
                    model.Email = userEmail;
                    model.FullName = data.ContainsKey("fullName") ? data["fullName"].ToString() : "";
                    model.Username = data.ContainsKey("username") ? data["username"].ToString() : "";
                    model.PhoneNumber = data.ContainsKey("phoneNumber") ? data["phoneNumber"].ToString() : "";
                    model.Field = data.ContainsKey("field") ? data["field"].ToString() : "";
                    model.ProfilePhotoUrl = data.ContainsKey("profilePhotoUrl") ? data["profilePhotoUrl"].ToString() : "/images/default-avatar.png";
                }
                else
                {
                    // New user, populate with basic info
                    model.UserId = userEmail;
                    model.Email = userEmail;
                    model.ProfilePhotoUrl = "/images/default-avatar.png";
                }

                return View(model);
            }
            catch (Exception ex)
            {
                TempData["Error"] = "Error loading profile: " + ex.Message;
                return View(new ProfileViewModel());
            }
        }

        // POST: Profile/Index
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Index(ProfileViewModel model)
        {
            if (!ModelState.IsValid)
            {
                return View(model);
            }

            try
            {
                var userEmail = User.Identity.Name;

                // Upload profile photo if provided
                string photoUrl = model.ProfilePhotoUrl;
                if (model.ProfilePhoto != null && model.ProfilePhoto.Length > 0)
                {
                    photoUrl = await UploadProfilePhotoAsync(model.ProfilePhoto, userEmail);
                }

                // Prepare data for Firestore
                var userData = new Dictionary<string, object>
                {
                    { "fullName", model.FullName ?? "" },
                    { "username", model.Username ?? "" },
                    { "email", model.Email ?? "" },
                    { "phoneNumber", model.PhoneNumber ?? "" },
                    { "field", model.Field ?? "" },
                    { "profilePhotoUrl", photoUrl ?? "/images/default-avatar.png" },
                    { "updatedAt", DateTime.UtcNow }
                };

                // Save to Firestore
                await _firestoreDb.Collection("users").Document(userEmail).SetAsync(userData, SetOptions.MergeAll);

                TempData["Success"] = "Profile updated successfully!";
                return RedirectToAction("Index");
            }
            catch (Exception ex)
            {
                TempData["Error"] = "Error saving profile: " + ex.Message;
                return View(model);
            }
        }

        private async Task<string> UploadProfilePhotoAsync(IFormFile file, string userId)
        {
            try
            {
                var storage = StorageClient.Create();
                var fileName = $"profile-photos/{userId}/{Guid.NewGuid()}{Path.GetExtension(file.FileName)}";

                using (var memoryStream = new MemoryStream())
                {
                    await file.CopyToAsync(memoryStream);
                    memoryStream.Position = 0;

                    await storage.UploadObjectAsync(
                        _bucketName,
                        fileName,
                        file.ContentType,
                        memoryStream
                    );
                }

                // Return public URL
                return $"https://storage.googleapis.com/{_bucketName}/{fileName}";
            }
            catch (Exception ex)
            {
                throw new Exception("Error uploading photo: " + ex.Message);
            }
        }
    }
}
