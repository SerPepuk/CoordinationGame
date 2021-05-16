class Node {
    private boolean status;
    private boolean newStatus;
    private int id;

    Node(int id) {
        this.id = id;
    }

    int getId() {
        return id;
    }

    boolean isActivated() {
        return status;
    }

    void setStatus(boolean status) {
        newStatus = status;
    }

    boolean statusHasNotChanged() {
        return status == newStatus;
    }

    void fixNewStatus() {
        status = newStatus;
    }
}
