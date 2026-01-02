const API_URL = "http://" + window.location.hostname + ":8081/api";

function checkAuth(requiredRole) {
    const token = localStorage.getItem("token");
    const role = localStorage.getItem("role");

    if (!token) {
        window.location.href = "index.html";
        return;
    }

    if (requiredRole && role !== requiredRole) {
        alert("Unauthorized access");
        // Redirect to appropriate dashboard based on actual role
        if (role === 'admin') window.location.href = "admin.html";
        else if (role === 'employee') window.location.href = "employee.html";
        else if (role === 'student') window.location.href = "student.html";
        else {
            logout();
        }
    }

    // Set user info in UI if element exists
    const roleDisplay = document.getElementById("user-role-display");
    if (roleDisplay) {
        roleDisplay.textContent = role.charAt(0).toUpperCase() + role.slice(1);
    }
}

function logout() {
    localStorage.clear();
    window.location.href = "index.html";
}

function showSection(sectionId) {
    // Hide all sections first
    document.querySelectorAll('.dashboard-section').forEach(el => el.classList.add('hidden'));
    // Show the requested section
    const section = document.getElementById(sectionId);
    if (section) {
        section.classList.remove('hidden');
    }
}
