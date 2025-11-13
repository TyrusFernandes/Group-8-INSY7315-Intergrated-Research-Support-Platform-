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

        
        // === TOP STATS ===
        public int TotalStudentCases { get; set; }
        public int ActiveStudentCases { get; set; }
        public decimal PendingQuotes { get; set; }
        public int TasksOverdue { get; set; }

        // === UPCOMING DEADLINES ===
        public List<DeadlineItem> UpcomingDeadlines { get; set; } = new();

        // === ACTIVE TASKS ===
        public List<ActiveTaskItem> ActiveTasks { get; set; } = new();

        // === NOTIFICATIONS ===
        public List<NotificationItem> Notifications { get; set; } = new();
    }
    // -------------------- Deadline Items --------------------
    public class DeadlineItem
    {
        public string TaskId { get; set; }
        public string StudentName { get; set; }
        public string TaskTitle { get; set; }
        public string ReviewType { get; set; }
        public DateTime DueDate { get; set; }
    }

    // -------------------- Active Tasks --------------------
    public class ActiveTaskItem
    {
        public string TaskId { get; set; }
        public string StudentName { get; set; }
        public string TaskTitle { get; set; }
        public string ReviewType { get; set; }
        public DateTime DueDate { get; set; }
    }

    // -------------------- Notifications --------------------
    public class NotificationItem
    {
        public string NotificationId { get; set; }
        public string ThreadId { get; set; }
        public string Type { get; set; }
        public string Text { get; set; }

        public DateTime CreatedAt { get; set; }
    }
}