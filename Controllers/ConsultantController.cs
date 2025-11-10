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
                            AdminApproved = fr.AdminApproved,
                            ConsultantApproved = doc.TryGetValue("consultantApproved", out bool cApproved) ? cApproved : false,
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

        public async Task<IActionResult> MessagesAsync()
        {
            string currentUserId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(currentUserId))
                return Unauthorized("User not logged in");

            var recentChatsRef = _firestoreDb
                .Collection("users")
                .Document(currentUserId)
                .Collection("recentChats")
                .OrderByDescending("timestamp");

            var snapshot = await recentChatsRef.GetSnapshotAsync();
            var recentChats = new List<RecentChatViewModel>();

            foreach (var doc in snapshot.Documents)
            {
                var data = doc.ToDictionary();

                long ts = 0L;
                if (data.TryGetValue("timestamp", out object tsObj) && tsObj != null)
                {
                    switch (tsObj)
                    {
                        case long l: ts = l; break;
                        case int i: ts = i; break;
                        case double d: ts = Convert.ToInt64(d); break;
                        case string s when long.TryParse(s, out long parsed): ts = parsed; break;
                        case Google.Cloud.Firestore.Timestamp fts:
                            ts = new DateTimeOffset(fts.ToDateTime()).ToUnixTimeMilliseconds();
                            break;
                        default:
                            // leave ts = 0
                            break;
                    }
                }

                recentChats.Add(new RecentChatViewModel
                {
                    ChatRoomId = data.ContainsKey("chatId") ? data["chatId"]?.ToString() ?? "" : "",
                    OtherUserId = data.ContainsKey("otherUserId") ? data["otherUserId"]?.ToString() ?? "" : "",
                    OtherUsername = data.ContainsKey("otherUsername") ? data["otherUsername"]?.ToString() ?? "Unknown User" : "Unknown User",
                    LastMessage = data.ContainsKey("lastMessage") ? data["lastMessage"]?.ToString() ?? "No messages yet" : "No messages yet",
                    Timestamp = ts
                });
            }

            var sorted = recentChats.OrderByDescending(c => c.Timestamp).ToList();
            return View(sorted);
        }

        // Resolve a username to UID
        [HttpGet]
        public async Task<IActionResult> GetUserIdByUsername(string username)
        {
            if (string.IsNullOrWhiteSpace(username))
                return BadRequest(new { error = "Username required" });

            // Normalize & log what the controller received
            username = username.Trim();
            System.Diagnostics.Debug.WriteLine($"GetUserIdByUsername called. Searching for username: '{username}'");

            try
            {
                var usersRef = _firestoreDb.Collection("users");

                // 1) Exact match (fast, indexed)
                var exactQuery = usersRef.WhereEqualTo("username", username);
                var exactSnapshot = await exactQuery.GetSnapshotAsync();
                if (exactSnapshot.Count > 0)
                {
                    var userDoc = exactSnapshot.Documents.First();
                    return Ok(new { userId = userDoc.Id });
                }

                // 2) Fallback: case-insensitive search (read a reasonable limit then match in-memory)
                //    This avoids complicated indexing changes and will find "Tyrus1" vs "tyrus1".
                //    Limit to e.g. 1000 docs — adjust if you have a huge users collection.
                var allSnapshot = await usersRef.Limit(1000).GetSnapshotAsync();
                var caseInsensitiveMatch = allSnapshot.Documents
                    .FirstOrDefault(d =>
                    {
                        if (!d.ContainsField("username")) return false;
                        var v = d.GetValue<string>("username");
                        return !string.IsNullOrEmpty(v) && string.Equals(v, username, StringComparison.OrdinalIgnoreCase);
                    });

                if (caseInsensitiveMatch != null)
                    return Ok(new { userId = caseInsensitiveMatch.Id });

                // 3) Fallback: maybe the user typed an email — try matching 'email' field (case-insensitive)
                var emailMatch = allSnapshot.Documents
                    .FirstOrDefault(d =>
                    {
                        if (!d.ContainsField("email")) return false;
                        var e = d.GetValue<string>("email");
                        return !string.IsNullOrEmpty(e) && string.Equals(e, username, StringComparison.OrdinalIgnoreCase);
                    });

                if (emailMatch != null)
                    return Ok(new { userId = emailMatch.Id });

                // 4) Not found
                return NotFound(new { error = "User not found", searched = username });
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"GetUserIdByUsername error: {ex}");
                return StatusCode(500, new { error = "Internal server error", detail = ex.Message });
            }
        }

        // Fetch chat messages for a chat room
        public async Task<IActionResult> ChatRoom(string chatRoomId)
        {
            try
            {
                var messagesRef = _firestoreDb.Collection("chats")
                    .Document(chatRoomId)
                    .Collection("messages")
                    .OrderBy("timestamp");

                var snapshot = await messagesRef.GetSnapshotAsync();
                var messages = new List<object>();

                foreach (var doc in snapshot.Documents)
                {
                    var data = doc.ToDictionary();

                    messages.Add(new
                    {
                        senderId = data.ContainsKey("senderId") ? data["senderId"]?.ToString() : "",
                        text = data.ContainsKey("text") ? data["text"]?.ToString() : "",
                        timestamp = data.ContainsKey("timestamp")
                            ? Convert.ToInt64(data["timestamp"])
                            : 0L
                    });
                }

                return Json(messages);
            }
            catch (Exception ex)
            {
                return BadRequest(new { error = ex.Message });
            }
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

            // Create new document and include its own ID in the data
            var newMessageRef = messagesRef.Document();
            var messageId = newMessageRef.Id;

            var newMessage = new Dictionary<string, object>
    {
        { "id", messageId }, // ✅ message stores its own ID
        { "senderId", currentUserId },
        { "text", request.Text },
        { "timestamp", DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() }
    };

            await newMessageRef.SetAsync(newMessage);

            // Update recent chats for both users
            var senderDoc = _firestoreDb.Collection("users").Document(currentUserId)
                .Collection("recentChats").Document(request.ReceiverId);
            var receiverDoc = _firestoreDb.Collection("users").Document(request.ReceiverId)
                .Collection("recentChats").Document(currentUserId);

            var senderSnapshot = await _firestoreDb.Collection("users").Document(currentUserId).GetSnapshotAsync();
            var receiverSnapshot = await _firestoreDb.Collection("users").Document(request.ReceiverId).GetSnapshotAsync();

            string senderUsername = senderSnapshot.ContainsField("username")
                ? senderSnapshot.GetValue<string>("username") : "You";

            string receiverUsername = receiverSnapshot.ContainsField("username")
                ? receiverSnapshot.GetValue<string>("username") : "Unknown";

            var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

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

            return Json(new { chatRoomId, messageId });
        }


        public IActionResult MeetingCalendar()
        {
            return View();
        }

        public IActionResult ProgressReport()
        {
            return View();
        }

        // Add this method inside the existing ConsultantController class
        [HttpGet]
        public async Task<IActionResult> GetCalendarEvents()
        {
            // Resolve current user UID (same approach as AssignedTasks)
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

            if (string.IsNullOrEmpty(currentUserUid))
            {
                return Json(new List<object>());
            }

            try
            {
                var requestsRef = _firestoreDb.Collection("requests");
                var query = requestsRef
                    .WhereEqualTo("assignedToUid", currentUserUid)
                    .WhereEqualTo("adminApproved", true);

                var snap = await query.GetSnapshotAsync();
                var events = new List<object>();

                foreach (var doc in snap.Documents)
                {
                    try
                    {
                        var fr = doc.ConvertTo<FirestoreRequest>();

                        // Skip if no due date
                        if (fr.DueDate == null) continue;

                        var due = fr.DueDate.ToDateTime();

                        // Use docTitle (or fallback) as the main label; include reviewType in extendedProps
                        var docTitle = !string.IsNullOrEmpty(fr.DocTitle) ? fr.DocTitle : (!string.IsNullOrEmpty(fr.ReviewType) ? fr.ReviewType : "Request");
                        var reviewType = fr.ReviewType ?? "";

                        // Render as an all-day, single-day event (use date-only start so it does NOT span days)
                        var startDate = due.Date.ToString("yyyy-MM-dd");

                        events.Add(new
                        {
                            id = doc.Id,
                            title = docTitle,
                            start = startDate,
                            allDay = true,
                            extendedProps = new
                            {
                                status = fr.Status ?? "Pending",
                                priority = fr.Urgency ?? "Normal",
                                reviewType = reviewType,
                                docTitle = fr.DocTitle ?? "",
                                requestedBy = fr.RequestedByUid ?? ""
                            }
                        });
                    }
                    catch
                    {
                        // ignore mapping errors per document
                    }
                }

                return Json(events);
            }
            catch
            {
                return Json(new List<object>());
            }
        }

        // POST: /Consultant/UpdateConsultantApproval
        [HttpPost]
        public async Task<IActionResult> UpdateConsultantApproval([FromBody] ConsultantApprovalDto dto)
        {
            if (dto == null || string.IsNullOrEmpty(dto.TaskId))
            {
                return Json(new ApiResponse { Success = false, Message = "Invalid payload" });
            }

            try
            {
                var docRef = _firestoreDb.Collection("requests").Document(dto.TaskId);
                var updates = new Dictionary<string, object>
                {
                    ["consultantApproved"] = dto.ConsultantApproved
                };

                await docRef.UpdateAsync(updates);
                return Json(new ApiResponse { Success = true, Message = "Updated" });
            }
            catch (Exception ex)
            {
                return Json(new ApiResponse { Success = false, Message = "Error updating approval: " + ex.Message });
            }
        }
    }
}