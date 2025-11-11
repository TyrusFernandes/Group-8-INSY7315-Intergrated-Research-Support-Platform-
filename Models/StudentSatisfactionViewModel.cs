using System.Collections.Generic;

namespace AcadenceWebApp.Models
{
    public class StudentSatisfactionViewModel
    {
        public int Satisfied { get; set; }
        public int Neutral { get; set; }
        public int Unsatisfied { get; set; } = 0;

        // additional metrics
        public int TotalFeedbackCount { get; set; }
        public double AverageRating { get; set; }

        // recent feedback items to display
        public List<FeedbackItemDto> RecentFeedbacks { get; set; } = new List<FeedbackItemDto>();
    }
}
