using System;
using System.Collections.Generic;

namespace AcadenceWebApp.Models
{
    public class ConsultantWorkloadViewModel
    {
        public List<ConsultantWorkload> Consultants { get; set; } = new List<ConsultantWorkload>();

        // Filter / sorting inputs used by controller and view
        public string SearchTerm { get; set; }
        public string StatusFilter { get; set; }
        public DateTime? DeadlineFrom { get; set; }
        public DateTime? DeadlineTo { get; set; }
        public string SortBy { get; set; } = "Name";
    }

    public class ConsultantWorkload
    {
        public string ConsultantId { get; set; }
        public string ConsultantName { get; set; }
        public string Email { get; set; }

        // Keep Projects for backward compatibility with the admin view
        public List<ProjectDetail> Projects { get; set; } = new List<ProjectDetail>();

        // New: workload items (student requests)
        public List<StudentRequest> StudentRequests { get; set; } = new List<StudentRequest>();

        // Computed metrics (optional)
        public int ActiveProjects { get; set; }
        public int TotalTasks { get; set; }
        public int CompletedTasks { get; set; }
        public int PendingTasks { get; set; }
        public DateTime? NextDeadline { get; set; }
        public double WorkloadPercentage { get; set; }
    }

    public class ProjectDetail
    {
        public string ProjectId { get; set; }
        public string ProjectName { get; set; }
        public string Status { get; set; }
        public DateTime? Deadline { get; set; }
        public int TasksAssigned { get; set; }
        public int TasksCompleted { get; set; }
    }

    public class StudentRequest
    {
        public string RequestId { get; set; }
        public string StudentName { get; set; }
        public string RequestTitle { get; set; }
        public string Status { get; set; }
        public DateTime? RequestDate { get; set; }
    }
}
