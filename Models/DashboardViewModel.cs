using System.Collections.Generic;

namespace AcadenceWebApp.Models
{
    public class DashboardViewModel
    {
        public int ResourcesCount { get; set; }
        public int SupportTicketsCount { get; set; }

        public int TotalFeedbackCount { get; set; }
        public double AverageRating { get; set; }

        // satisfaction breakdown for pie chart
        public int Satisfied { get; set; }
        public int Neutral { get; set; }
        public int Unsatisfied { get; set; }

        // new: number of assignment requests shown on Admin -> Assign page
        public int AssignRequestsCount { get; set; }

        public List<FeedbackItemDto> RecentFeedbacks { get; set; } = new List<FeedbackItemDto>();
        public List<ResourceItemDto> RecentResources { get; set; } = new List<ResourceItemDto>();
    }
}