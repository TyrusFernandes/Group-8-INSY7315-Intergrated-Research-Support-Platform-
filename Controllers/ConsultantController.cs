using AcadenceWebApp.Models;
using FirebaseAdmin.Auth;
using Google.Cloud.Firestore;
using Google.Cloud.Firestore;
using AcadenceWebApp.Models;
using Google.Cloud.Firestore;
using Microsoft.AspNetCore.Mvc;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Claims;
using System.Diagnostics;
using System.Security.Claims;
using System.Threading.Tasks;

namespace AcadenceWebApp.Controllers
{
    public class ConsultantController : Controller
    {
        private readonly FirestoreDb _firestoreDb;
        private readonly string _projectId = "acadence-40662";

        public ConsultantController()
        {
            _firestoreDb = FirestoreDb.Create(_projectId);
        }

        public IActionResult Dashboard()
        {
            return View();
        }

        // GET: /Consultant/AssignedTasks
        [HttpGet]
        public async Task<IActionResult> AssignedTasks()
        {
            // Resolve Firebase UID from claims or session
            string currentUserUid = null;
            if (User?.Identity?.IsAuthenticated == true)
            {
                currentUserUid =
                    User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                    ?? User.FindFirst("user_id")?.Value
                    ?? User.FindFirst("sub")?.Value;
            }

            if (string.IsNullOrEmpty(currentUserUid))
            {
                currentUserUid = HttpContext.Session.GetString("UserUid");
            }

            ViewBag.CurrentUserId = currentUserUid ?? string.Empty;

            if (string.IsNullOrEmpty(currentUserUid))
            {
                ViewBag.ErrorMessage = "Error: No current user UID found in claims. Check which claim holds the Firebase UID.";
                return View(Enumerable.Empty<TaskViewModel>());
            }

            try
            {
                var requestsRef = _firestoreDb.Collection("requests");

                // only return requests assigned to this consultant AND approved by admin
                var query = requestsRef
                    .WhereEqualTo("assignedToUid", currentUserUid)
                    .WhereEqualTo("adminApproved", true);

                var snap = await query.GetSnapshotAsync();

                var tasks = new List<TaskViewModel>();

                foreach (var doc in snap.Documents)
                {
                    try
                    {
                        var fr = doc.ConvertTo<FirestoreRequest>();

                        // Resolve student display name (try doc by UID, then query by email, then by username)
                        string studentName = fr.RequestedByUid ?? "(unknown)";
                        if (!string.IsNullOrEmpty(fr.RequestedByUid))
                        {
                            // 1) Try direct document lookup (users/{uid})
                            var studentDoc = await _firestoreDb.Collection("users").Document(fr.RequestedByUid).GetSnapshotAsync();
                            if (studentDoc.Exists)
                            {
                                var fu = studentDoc.ConvertTo<FirestoreUser>();
                                studentName = !string.IsNullOrEmpty(fu.DisplayName) ? fu.DisplayName
                                            : !string.IsNullOrEmpty(fu.Username) ? fu.Username
                                            : !string.IsNullOrEmpty(fu.Email) ? fu.Email
                                            : fr.RequestedByUid;
                            }
                            else
                            {
                                // 2) Query users where email == requestedByUid
                                var qByEmail = _firestoreDb.Collection("users").WhereEqualTo("email", fr.RequestedByUid).Limit(1);
                                var qSnap = await qByEmail.GetSnapshotAsync();
                                if (qSnap.Documents.Count > 0)
                                {
                                    var fu = qSnap.Documents[0].ConvertTo<FirestoreUser>();
                                    studentName = !string.IsNullOrEmpty(fu.DisplayName) ? fu.DisplayName
                                                : !string.IsNullOrEmpty(fu.Username) ? fu.Username
                                                : !string.IsNullOrEmpty(fu.Email) ? fu.Email
                                                : fr.RequestedByUid;
                                }
                                else
                                {
                                    // 3) Query users where username == requestedByUid
                                    var qByUsername = _firestoreDb.Collection("users").WhereEqualTo("username", fr.RequestedByUid).Limit(1);
                                    var qSnap2 = await qByUsername.GetSnapshotAsync();
                                    if (qSnap2.Documents.Count > 0)
                                    {
                                        var fu = qSnap2.Documents[0].ConvertTo<FirestoreUser>();
                                        studentName = !string.IsNullOrEmpty(fu.DisplayName) ? fu.DisplayName
                                                    : !string.IsNullOrEmpty(fu.Username) ? fu.Username
                                                    : !string.IsNullOrEmpty(fu.Email) ? fu.Email
                                                    : fr.RequestedByUid;
                                    }
                                    // else fallback remains UID
                                }
                            }
                        }

                        // Map to view model
                        var model = new TaskViewModel
                        {
                            Id = doc.Id,
                            DocId = fr.DocId,
                            DocTitle = fr.DocTitle,
                            RequestedByUid = fr.RequestedByUid,
                            StudentName = studentName,
                            AssignedToUid = fr.AssignedToUid,
                            AssignedToName = fr.AssignedToName,
                            ReviewType = fr.ReviewType ?? "",         // ensure reviewType is mapped
                            ReviewDetails = fr.ReviewDetails ?? "",
                            Urgency = fr.Urgency ?? "",
                            Visibility = fr.Visibility ?? "",
                            Status = fr.Status ?? "Pending",
                            DueDate = fr.DueDate.ToDateTime(),
                            CreatedAt = fr.CreatedAt.ToDateTime(),
                            Price = fr.Price,
                            AdminApproved = fr.AdminApproved
                        };

                        tasks.Add(model);
                    }
                    catch
                    {
                        // ignore mapping errors per-document but continue
                    }
                }

                // sort by due date ascending
                var ordered = tasks.OrderBy(t => t.DueDate).ToList();
                return View(ordered);
            }
            catch (Exception ex)
            {
                ViewBag.ErrorMessage = "Unable to load assigned tasks: " + ex.Message;
                return View(Enumerable.Empty<TaskViewModel>());
            }
        }

        // POST: /Consultant/UpdateTaskStatus
        [HttpPost]
        public async Task<IActionResult> UpdateTaskStatus([FromBody] TaskStatusUpdateDto dto)
        {
            if (dto == null || string.IsNullOrEmpty(dto.TaskId) || string.IsNullOrEmpty(dto.Status))
            {
                return Json(new ApiResponse { Success = false, Message = "Invalid request" });
            }

            try
            {
                var docRef = _firestoreDb.Collection("requests").Document(dto.TaskId);
                await docRef.UpdateAsync("status", dto.Status);
                return Json(new ApiResponse { Success = true, Message = "Status updated" });
            }
            catch (Exception ex)
            {
                return Json(new ApiResponse { Success = false, Message = "Error updating status: " + ex.Message });
            }
        }

        // modified ResourceLibrary action to load resources from Firestore and return model
        [HttpGet]
        public async Task<IActionResult> ResourceLibrary()
        {
            var vm = new UploadResourceViewModel();

            try
            {
                var coll = _firestoreDb.Collection("resources");
                var snap = await coll.GetSnapshotAsync();
                var list = new List<ResourceItemDto>();

                foreach (var doc in snap.Documents)
                {
                    try
                    {
                        var data = doc.ToDictionary();

                        DateTime uploadedAt = DateTime.UtcNow;
                        if (data.TryGetValue("uploadedAt", out var tsObj) && tsObj is Timestamp ts)
                            uploadedAt = ts.ToDateTime();

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
                        // ignore parse errors for individual docs
                    }
                }

                vm.Existing = list.OrderByDescending(r => r.UploadedAt).ToList();
            }
            catch (Exception ex)
            {
                ViewBag.ErrorMessage = "Unable to load resources: " + ex.Message;
            }

            return View(vm);
        }

        [HttpGet]
        public async Task<IActionResult> MessagesAsync()
        {
            string currentUserId = User.Identity?.Name;
            if (string.IsNullOrEmpty(currentUserId))
            {
                return Unauthorized("User not logged in");
            }

            // Load recent chats for sidebar
            var recentChatsRef = _firestoreDb.Collection("users").Document(currentUserId).Collection("recentChats");
            var snapshot = await recentChatsRef.OrderByDescending("timestamp").GetSnapshotAsync();

            var recentChats = new List<RecentChatViewModel>();
            foreach (var doc in snapshot.Documents)
            {
                var data = doc.ToDictionary();
                recentChats.Add(new RecentChatViewModel
                {
                    ChatRoomId = data.ContainsKey("chatId") ? data["chatId"].ToString() : "",
                    OtherUserId = data.ContainsKey("otherUserId") ? data["otherUserId"].ToString() : "",
                    OtherUsername = data.ContainsKey("otherUsername") ? data["otherUsername"].ToString() : "",
                    LastMessage = data.ContainsKey("lastMessage") ? data["lastMessage"].ToString() : "",
                    Timestamp = data.ContainsKey("timestamp") ? ((Timestamp)data["timestamp"]).ToDateTime() : DateTime.UtcNow
                });
            }

            return View(recentChats.OrderByDescending(c => c.Timestamp).ToList());
        }
        
        // Resolve a username to UID
        [HttpGet]
        public async Task<IActionResult> GetUserIdByUsername(string username)
        {
            if (string.IsNullOrWhiteSpace(username))
                return BadRequest(new { error = "Username required" });

            Debug.WriteLine($"Searching for username: {username}");
            try
            {
                // Query Firestore users collection by "username" field
                var usersRef = _firestoreDb.Collection("users");
                var query = usersRef.WhereEqualTo("username", username);
                var snapshot = await query.GetSnapshotAsync();

                if (snapshot.Count == 0)
                    return NotFound(new { error = "User not found" });

                // Should only be one match
                var userDoc = snapshot.Documents.First();
                var userId = userDoc.Id; // Firestore document ID is the user's UID

                return Ok(new { userId });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { error = ex.Message });
            }
        }

        // Fetch chat messages for a chat room
        [HttpGet]
        public async Task<IActionResult> ChatRoom(string chatRoomId)
        {
            var messagesRef = _firestoreDb.Collection("chats").Document(chatRoomId).Collection("messages");
            var snapshot = await messagesRef.OrderBy("timestamp").GetSnapshotAsync();

            var messages = snapshot.Documents.Select(d => new
            {
                senderId = d.GetValue<string>("senderId"),
                text = d.GetValue<string>("text"),
                timestamp = ((Timestamp)d.GetValue<Timestamp>("timestamp")).ToDateTime()
            }).ToList();

            return Json(messages);
        }

        // Send a message
        [HttpPost]
        public async Task<IActionResult> SendMessage([FromBody] SendMessageRequest request)
        {
            string currentUserId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(currentUserId))
                return Unauthorized("User not logged in");

            if (string.IsNullOrEmpty(request.ReceiverId) || string.IsNullOrEmpty(request.Text))
                return BadRequest("Receiver or text missing");

            // Determine deterministic chat room ID
            string chatRoomId = string.Compare(currentUserId, request.ReceiverId) < 0
                ? currentUserId + "_" + request.ReceiverId
                : request.ReceiverId + "_" + currentUserId;

            var messagesRef = _firestoreDb.Collection("chats").Document(chatRoomId).Collection("messages");

            var newMessage = new Dictionary<string, object>
        {
            { "senderId", currentUserId },
            { "text", request.Text },
            { "timestamp", Timestamp.GetCurrentTimestamp() }
        };

            await messagesRef.AddAsync(newMessage);

            // Update recent chats for both users
            var senderDoc = _firestoreDb.Collection("users").Document(currentUserId)
                .Collection("recentChats").Document(request.ReceiverId);
            var receiverDoc = _firestoreDb.Collection("users").Document(request.ReceiverId)
                .Collection("recentChats").Document(currentUserId);

            // Get usernames
            var senderSnapshot = await _firestoreDb.Collection("users").Document(currentUserId).GetSnapshotAsync();
            var receiverSnapshot = await _firestoreDb.Collection("users").Document(request.ReceiverId).GetSnapshotAsync();

            string senderUsername = senderSnapshot.GetValue<string>("username") ?? "You";
            string receiverUsername = receiverSnapshot.GetValue<string>("username") ?? "Unknown";

            var timestamp = Timestamp.GetCurrentTimestamp();
            var senderRecent = new Dictionary<string, object>
        {
            { "chatId", chatRoomId },
            { "otherUserId", request.ReceiverId },
            { "otherUsername", receiverUsername },
            { "lastMessage", request.Text },
            { "timestamp", timestamp }
        };

            var receiverRecent = new Dictionary<string, object>
        {
            { "chatId", chatRoomId },
            { "otherUserId", currentUserId },
            { "otherUsername", senderUsername },
            { "lastMessage", request.Text },
            { "timestamp", timestamp }
        };

            await senderDoc.SetAsync(senderRecent, SetOptions.MergeAll);
            await receiverDoc.SetAsync(receiverRecent, SetOptions.MergeAll);

            return Json(new { chatRoomId });
        }


        public IActionResult MeetingCalendar()
        {
            return View();
        }

        public IActionResult ProgressReport()
        {
            return View();
        }
    }
}