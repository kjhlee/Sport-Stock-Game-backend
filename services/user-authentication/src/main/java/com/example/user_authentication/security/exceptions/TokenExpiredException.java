package com.example.user_authentication.security.exceptions;

public class TokenExpiredException extends RuntimeException{
    public TokenExpiredException(String message){
        super("Token is expired: " + message);
    }

}
