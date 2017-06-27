package edu.cmu.inmind.multiuser.controller.blackboard;

import edu.cmu.inmind.multiuser.common.Utils;

/**
 * Created by oscarr on 3/16/17.
 */
public class BlackboardEvent {
    private String status;
    private String id;
    private Object element;

    public BlackboardEvent(String status, String id, Object element) {
        this.status = status;
        this.element = element;
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getElement() {
        return element;
    }

    public void setElement(Object element) {
        this.element = element;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString(){
        return "[status: " + status + ", id: " + id + ", element: " + element + "]";
    }
}
