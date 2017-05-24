package server;

/**
 * Authentication data
 */
class Authentication {

    private String userName;
    private int userId;

    public Authentication(String name, int uid) {
        this.userName = name;
        this.userId = uid;
    }

    protected String getUserName() {
        return userName;
    }

    protected int getUserId() {
        return userId;
    }
}
