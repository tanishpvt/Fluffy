package com.example.fluffy.Notification;

public class Token {
    /*An FCM token, or much commonly known as a registrationToken
    * An ID issued by the GCM
    * connection servers to the clicent app that allows it to receive message*/

    String token;

    public Token(String token) {
        this.token = token;
    }

    public Token() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
