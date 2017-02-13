package com.penghaonan.homemonitorclient.cmd.db;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class CmdLog {

    public final static int CONTENT_TYPE_TEXT = 1;
    public final static int CONTENT_TYPE_PIC = 2;
    public final static int CONTENT_TYPE_SOUND = 3;

    public final static int LOG_TYPE_REQUEST = 1;
    public final static int LOG_TYPE_RESPONSE = 2;

    @Id
    private Long id;

    private String server;

    private String content;

    private Integer contentType;

    private Integer logType;

    private Long time;

    @Generated(hash = 770898684)
    public CmdLog(Long id, String server, String content, Integer contentType,
            Integer logType, Long time) {
        this.id = id;
        this.server = server;
        this.content = content;
        this.contentType = contentType;
        this.logType = logType;
        this.time = time;
    }

    @Generated(hash = 1463196788)
    public CmdLog() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServer() {
        return this.server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getContentType() {
        return this.contentType;
    }

    public void setContentType(Integer contentType) {
        this.contentType = contentType;
    }

    public Integer getLogType() {
        return this.logType;
    }

    public void setLogType(Integer logType) {
        this.logType = logType;
    }

    public Long getTime() {
        return this.time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}
