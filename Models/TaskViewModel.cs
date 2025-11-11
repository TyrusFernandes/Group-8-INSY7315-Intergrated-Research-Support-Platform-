using System;
using System.ComponentModel.DataAnnotations;
using Google.Cloud.Firestore;

namespace AcadenceWebApp.Models
{
    /// <summary>
    /// ViewModel for displaying consultant assigned tasks
    /// Maps to Firestore 'requests' collection
    /// </summary>
    public class TaskViewModel
    {
        // Firestore document ID
        public string Id { get; set; }

        [Display(Name = "Document ID")]
        public string DocId { get; set; }

        [Display(Name = "Document Title")]
        public string DocTitle { get; set; }

        [Display(Name = "Student ID")]
        public string RequestedByUid { get; set; }

        [Display(Name = "Student Name")]
        public string StudentName { get; set; } // Loaded from users collection

        [Display(Name = "Consultant ID")]
        public string AssignedToUid { get; set; }

        [Display(Name = "Consultant Name")]
        public string AssignedToName { get; set; }

        [Display(Name = "Review Type")]
        public string ReviewType { get; set; }  // <== added for view

        [Display(Name = "Review Details")]
        public string ReviewDetails { get; set; }  // <== added for view

        [Display(Name = "Urgency")]
        public string Urgency { get; set; }

        [Display(Name = "Visibility")]
        public string Visibility { get; set; }

        [Display(Name = "Status")]
        public string Status { get; set; }

        [Display(Name = "Due Date")]
        public DateTime DueDate { get; set; }

        [Display(Name = "Created Date")]
        public DateTime CreatedAt { get; set; }

        [Display(Name = "Price")]
        public int Price { get; set; }

        [Display(Name = "Admin Approved")]
        public bool AdminApproved { get; set; }

        [Display(Name = "Consultant Approved")]
        public bool ConsultantApproved { get; set; }

        // Computed property for priority based on urgency
        public string Priority => Urgency switch
        {
            "Critical" => "Critical",
            "High" => "High",
            _ => "Normal",
        
        };

        // Formatted deadline for display
        public string DeadlineFormatted => DueDate.ToString("dd MMM yyyy");
    }

    /// <summary>
    /// Firestore document model for 'requests' collection
    /// </summary>
    [FirestoreData]
    public class FirestoreRequest
    {
        [FirestoreProperty("docId")]
        public string DocId { get; set; }

        [FirestoreProperty("docTitle")]
        public string DocTitle { get; set; }

        [FirestoreProperty("assignedToUid")]
        public string AssignedToUid { get; set; }

        [FirestoreProperty("assignedToName")]
        public string AssignedToName { get; set; }

        [FirestoreProperty("reviewType")]
        public string ReviewType { get; set; }

        [FirestoreProperty("urgency")]
        public string Urgency { get; set; }

        [FirestoreProperty("visibility")]
        public string Visibility { get; set; }

        [FirestoreProperty("reviewDetails")]
        public string ReviewDetails { get; set; }

        [FirestoreProperty("dueDate")]
        public Timestamp DueDate { get; set; }

        [FirestoreProperty("requestedByUid")]
        public string RequestedByUid { get; set; }

        [FirestoreProperty("status")]
        public string Status { get; set; }

        [FirestoreProperty("createdAt")]
        public Timestamp CreatedAt { get; set; }

        [FirestoreProperty("price")]
        public int Price { get; set; }

        [FirestoreProperty("adminApproved")]
        public bool AdminApproved { get; set; }
    }

    [FirestoreData]
    public class FirestoreUser
    {
        [FirestoreProperty("displayName")]
        public string DisplayName { get; set; }

        [FirestoreProperty("username")]
        public string Username { get; set; }

        [FirestoreProperty("role")]
        public string Role { get; set; }

        [FirestoreProperty("email")]
        public string Email { get; set; }
    }

    public class TaskStatusUpdateDto
    {
        [Required]
        public string TaskId { get; set; }

        [Required]
        public string Status { get; set; }
    }

    public class ApiResponse
    {
        public bool Success { get; set; }
        public string Message { get; set; }
        public object Data { get; set; }
    }
}
