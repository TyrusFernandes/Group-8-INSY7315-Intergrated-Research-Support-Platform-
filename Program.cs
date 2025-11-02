using FirebaseAdmin;
using Google.Apis.Auth.OAuth2;

var builder = WebApplication.CreateBuilder(args);

// ----- Services -----
builder.Services.AddControllersWithViews();
builder.Services.AddDistributedMemoryCache();
builder.Services.AddSession(); // if you use TempData/Session

var app = builder.Build();

// ----- Pipeline -----
if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Home/Error");
    app.UseHsts();
}

app.UseHttpsRedirection();
app.UseStaticFiles();

app.UseRouting();

app.UseSession();
// app.UseAuthentication(); // uncomment if you add auth
app.UseAuthorization();

// ----- Firebase Admin (init once) -----
if (FirebaseApp.DefaultInstance == null)
{
    FirebaseApp.Create(new AppOptions
    {
        Credential = GoogleCredential.FromFile("firebase-adminsdk.json")
    });
    Console.WriteLine("Firebase initialized successfully!");
}

// ----- Routes -----
// Consultant Workload page
app.MapControllerRoute(
    name: "consultants_workload",
    pattern: "consultants/workload",
    defaults: new { controller = "Consultants", action = "Workload" }
);

// Consultant Assignment Panel page
app.MapControllerRoute(
    name: "assignments_assign",
    pattern: "assignments/assign",
    defaults: new { controller = "Assignments", action = "Assign" }
);

// Default MVC route (Home/Index if no controller/action specified)
app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Home}/{action=Index}/{id?}"
);

app.Run();
