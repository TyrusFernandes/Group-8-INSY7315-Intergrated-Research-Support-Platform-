namespace AcadenceWebApp.Models
{
    public class ChatMessageModel
    {
        public string SenderId { get; set; }
        public string SenderUsername { get; set; }
        public string Text { get; set; }
        public long Timestamp { get; set; }
    }
}
