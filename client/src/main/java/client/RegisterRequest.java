package client;

public class RegisterRequest {
    public String username;
    public String password;
    public String email;

    public RegisterRequest(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
}
