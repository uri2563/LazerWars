package com.lazerwars2563.Class;

public class UserDetails {
    String UserName;
    String UserId;

    public UserDetails(String userName, String userId) {
        if(userName != null && userId != null) {
            UserName = userName;
            UserId = userId;
        }
    }

    public String getUserName() {
        return UserName;
    }

    public String getUserId() {
        return UserId;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }
}
