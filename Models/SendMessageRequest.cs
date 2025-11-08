namespace AcadenceWebApp.Models
{
    public class SendMessageRequest
    {
        public string ReceiverId { get; set; }
        public string ReceiverName { get; set; }
        public string Text { get; set; }
    }
}
