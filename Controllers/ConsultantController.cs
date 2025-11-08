using AcadenceWebApp.Models;
using FirebaseAdmin.Auth;
using Google.Cloud.Firestore;
using Google.Cloud.Firestore;
using Microsoft.AspNetCore.Mvc;
using System.Diagnostics;
using System.Security.Claims;
using System.Threading.Tasks;

namespace AcadenceWebApp.Controllers
{
    public class ConsultantController : Controller
    {
        private readonly FirestoreDb _firestore;

        public ConsultantController()
        {
            // Use your Firebase project ID and credentials
            _firestore = FirestoreDb.Create("acadence-40662");
        }


        public IActionResult Dashboard()
        {
            return View();
        }

        public IActionResult AssignedTasks()
        {
            return View();
        }

        public IActionResult ResourceLibrary()
        {
            return View();
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
            var recentChatsRef = _firestore.Collection("users").Document(currentUserId).Collection("recentChats");
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
                var usersRef = _firestore.Collection("users");
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
            var messagesRef = _firestore.Collection("chats").Document(chatRoomId).Collection("messages");
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

            var messagesRef = _firestore.Collection("chats").Document(chatRoomId).Collection("messages");

            var newMessage = new Dictionary<string, object>
        {
            { "senderId", currentUserId },
            { "text", request.Text },
            { "timestamp", Timestamp.GetCurrentTimestamp() }
        };

            await messagesRef.AddAsync(newMessage);

            // Update recent chats for both users
            var senderDoc = _firestore.Collection("users").Document(currentUserId)
                .Collection("recentChats").Document(request.ReceiverId);
            var receiverDoc = _firestore.Collection("users").Document(request.ReceiverId)
                .Collection("recentChats").Document(currentUserId);

            // Get usernames
            var senderSnapshot = await _firestore.Collection("users").Document(currentUserId).GetSnapshotAsync();
            var receiverSnapshot = await _firestore.Collection("users").Document(request.ReceiverId).GetSnapshotAsync();

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
