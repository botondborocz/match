interface LoginResponse {
    token: string;
  }
  
  // Ensure this matches your Kotlin 'LoginRequest' data class exactly
  interface LoginRequest {
    email: string;
    password: ""; 
  }