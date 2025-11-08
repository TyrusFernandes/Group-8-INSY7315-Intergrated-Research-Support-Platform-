using AcadenceWebApp.Models;
using Google.Cloud.Firestore;
using Microsoft.AspNetCore.Mvc;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System.Collections.Generic;

namespace AcadenceWebApp.Controllers
{
    public class AdminController : Controller
    {
        private readonly FirestoreDb _firestore;

        // Constructor requires FirestoreDb to be injected via DI
        public AdminController(FirestoreDb firestore)
        {
            _firestore = firestore;
        }

        public IActionResult Dashboard()
        {
            return View();
        }

        public IActionResult StudentSatisfaction()
        {
            return View();
        }

        public IActionResult ConsultantWorkload()
        {
            return View();
        }

        public IActionResult ConsultantAssignment()
        {
            return View();
        }

        public IActionResult Notifications()
        {
            return View();
        }

        public IActionResult UploadResource()
        {
            return View();
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
    }
}
