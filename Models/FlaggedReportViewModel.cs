namespace AcadenceWebApp.Models
{
    public class FlaggedReportViewModel
    {
        public string Id { get; set; }
        public string ChatId { get; set; }
        public string FlaggedByUserId { get; set; }
        public string FlaggedByUsername { get; set; }  
        public string Status { get; set; }
        public DateTime Timestamp { get; set; }
        public string Reason { get; set; }
        public string ConversationJson { get; set; }
    }
}
