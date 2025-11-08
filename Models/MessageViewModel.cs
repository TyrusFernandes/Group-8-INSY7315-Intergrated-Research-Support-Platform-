namespace AcadenceWebApp.Models
{
    public class MessageViewModel
    {
        public string Id { get; set; }
        public string ChatRoomId { get; set; }
        public string SenderId { get; set; }
        public string Text { get; set; }
        public DateTime Timestamp { get; set; }

        public string ReceiverId { get; set; }
        public string ReceiverUsername { get; set; }
        public string SenderUsername { get; set; }


    }
}
