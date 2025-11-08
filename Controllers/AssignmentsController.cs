using AcadenceWebApp.Models;
using Google.Cloud.Firestore;
using Microsoft.AspNetCore.Mvc;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace AcadenceWebApp.Controllers
{
    public class AssignmentsController : Controller
    {
        private readonly FirestoreDb _firestore;
        private readonly string _projectId = "acadence-40662";

        public AssignmentsController()
        {
            _firestore = FirestoreDb.Create(_projectId);
        }

        // GET: Admin/Assign
        [HttpGet]
        public async Task<IActionResult> Assign()
        {
            try
            {
                // Load all requests (admin view shows everything)
                var requestsSnap = await _firestore.Collection("requests").GetSnapshotAsync();
                var requests = new List<TaskViewModel>();

                foreach (var doc in requestsSnap.Documents)
                {
                    try
                    {
                        var fr = doc.ConvertTo<FirestoreRequest>();
                        var vm = new TaskViewModel
                        {
                            Id = doc.Id,
                            DocId = fr.DocId,
                            DocTitle = fr.DocTitle,
                            RequestedByUid = fr.RequestedByUid,
                            StudentName = fr.RequestedByUid, // will attempt to resolve on client if needed
                            AssignedToUid = fr.AssignedToUid,
                            AssignedToName = fr.AssignedToName,
                            ReviewType = fr.ReviewType ?? "",
                            ReviewDetails = fr.ReviewDetails ?? "",
                            Urgency = fr.Urgency ?? "",
                            Visibility = fr.Visibility ?? "",
                            Status = fr.Status ?? "Pending",
                            DueDate = fr.DueDate.ToDateTime(),
                            CreatedAt = fr.CreatedAt.ToDateTime(),
                            Price = fr.Price,
                            AdminApproved = fr.AdminApproved
                        };
                        requests.Add(vm);
                    }
                    catch
                    {
                        // ignore bad docs
                    }
                }

                // Load consultants (users collection where role == "consultant")
                var consultants = new List<ConsultantDto>();
                var q = _firestore.Collection("users").WhereEqualTo("role", "consultant");
                var consSnap = await q.GetSnapshotAsync();
                foreach (var doc in consSnap.Documents)
                {
                    try
                    {
                        var fu = doc.ConvertTo<FirestoreUser>();
                        consultants.Add(new ConsultantDto
                        {
                            Id = doc.Id,
                            Name = !string.IsNullOrEmpty(fu.DisplayName) ? fu.DisplayName : (!string.IsNullOrEmpty(fu.Username) ? fu.Username : fu.Email),
                            ActiveProjects = 0,
                            Department = "",
                            Region = "",
                            Email = fu.Email ?? "",
                            Username = fu.Username ?? ""
                        });
                    }
                    catch { }
                }

                var vmOut = new ConsultantAssignmentViewModel
                {
                    Requests = requests.OrderBy(r => r.DueDate).ToList(),
                    Consultants = consultants.OrderBy(c => c.Name).ToList()
                };

                // explicit path for admin view location
                return View("~/Views/Admin/Assign.cshtml", vmOut);
            }
            catch (Exception ex)
            {
                ViewBag.ErrorMessage = "Unable to load assignments: " + ex.Message;
                var emptyVm = new ConsultantAssignmentViewModel();
                return View("~/Views/Admin/Assign.cshtml", emptyVm);
            }
        }

        // POST: /Assignments/UpdateAssignment
        [HttpPost]
        public async Task<IActionResult> UpdateAssignment([FromBody] AssignmentUpdateDto dto)
        {
            if (dto == null || string.IsNullOrEmpty(dto.TaskId))
            {
                return Json(new ApiResponse { Success = false, Message = "Invalid payload" });
            }

            try
            {
                var docRef = _firestore.Collection("requests").Document(dto.TaskId);

                var updates = new Dictionary<string, object>();
                // set assigned fields (empty string if unassigned)
                updates["assignedToUid"] = dto.AssignedToUid ?? "";
                updates["assignedToName"] = dto.AssignedToName ?? "";
                // adminApproved must be set explicitly
                updates["adminApproved"] = dto.AdminApproved;

                await docRef.UpdateAsync(updates);

                return Json(new ApiResponse { Success = true, Message = "Updated" });
            }
            catch (Exception ex)
            {
                return Json(new ApiResponse { Success = false, Message = "Error updating assignment: " + ex.Message });
            }
        }
    }
}
