namespace AcadenceWebApp.Models
{
    public class RecentChatViewModel
    {
        public string ChatRoomId { get; set; }
        public string OtherUserId { get; set; }
        public string OtherUsername { get; set; }
        public string LastMessage { get; set; }
        public long Timestamp { get; set; } 
    }
}
