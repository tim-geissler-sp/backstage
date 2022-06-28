/*
 * Copyright (c) 2022. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service.model;

import java.util.List;
import java.util.Objects;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties
public class AuditReportResponse {

    private Object completed;

    private Object completionStatus;

    private Long created;

    private String description;

    private String id;

    private Object launched;

    private String launcher;

    private List<Object> messages;

    private String name;

    private Object parentName;

    private Object progress;

    private List<Object> returns;

    private String type;

    public Object getCompleted() {
        return completed;
    }

    public void setCompleted(Object completed) {
        this.completed = completed;
    }

    public Object getCompletionStatus() {
        return completionStatus;
    }

    public void setCompletionStatus(Object completionStatus) {
        this.completionStatus = completionStatus;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Object getLaunched() {
        return launched;
    }

    public void setLaunched(Object launched) {
        this.launched = launched;
    }

    public String getLauncher() {
        return launcher;
    }

    public void setLauncher(String launcher) {
        this.launcher = launcher;
    }

    public List<Object> getMessages() {
        return messages;
    }

    public void setMessages(List<Object> messages) {
        this.messages = messages;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getParentName() {
        return parentName;
    }

    public void setParentName(Object parentName) {
        this.parentName = parentName;
    }

    public Object getProgress() {
        return progress;
    }

    public void setProgress(Object progress) {
        this.progress = progress;
    }

    public List<Object> getReturns() {
        return returns;
    }

    public void setReturns(List<Object> returns) {
        this.returns = returns;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditReportResponse that = (AuditReportResponse) o;
        return Objects.equals(completed, that.completed) && Objects.equals(completionStatus, that.completionStatus) && Objects.equals(created, that.created) && Objects.equals(description, that.description) && Objects.equals(id, that.id) && Objects.equals(launched, that.launched) && Objects.equals(launcher, that.launcher) && Objects.equals(messages, that.messages) && Objects.equals(name, that.name) && Objects.equals(parentName, that.parentName) && Objects.equals(progress, that.progress) && Objects.equals(returns, that.returns) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(completed, completionStatus, created, description, id, launched, launcher, messages, name, parentName, progress, returns, type);
    }

    @Override
    public String toString() {
        return "AuditReportResponse{" + "completed=" + completed + ", completionStatus=" + completionStatus + ", created=" + created + ", description='" + description + '\'' + ", id='" + id + '\'' + ", launched=" + launched + ", launcher='" + launcher + '\'' + ", messages=" + messages + ", name='" + name + '\'' + ", parentName=" + parentName + ", progress=" + progress + ", returns=" + returns + ", type='" + type + '\'' + '}';
    }
}
